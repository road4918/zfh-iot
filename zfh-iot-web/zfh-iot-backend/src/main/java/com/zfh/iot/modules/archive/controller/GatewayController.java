package com.zfh.iot.modules.archive.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zfh.iot.common.enums.GatewayProtocolEnum;
import com.zfh.iot.common.exception.BusinessException;
import com.zfh.iot.common.result.PageResult;
import com.zfh.iot.common.result.Result;
import com.zfh.iot.common.utils.PageUtils;
import com.zfh.iot.modules.archive.entity.IotGateway;
import com.zfh.iot.modules.archive.service.IotGatewayService;
import com.zfh.iot.modules.auth.utils.JwtUtils;
import lombok.RequiredArgsConstructor;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.UUID;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/archive/gateways")
@RequiredArgsConstructor
public class GatewayController {

    private final IotGatewayService gatewayService;
    private final JwtUtils jwtUtils;

    private static final Integer USER_TYPE_PLATFORM = 0;
    private static final Integer USER_TYPE_TENANT = 1;
    private static final Pattern HEX_8_PATTERN = Pattern.compile("^[0-9a-fA-F]{8}$");
    private static final Pattern ALPHANUM_32_PATTERN = Pattern.compile("^[a-zA-Z0-9]{1,32}$");

    private void validateCommAddr(String commAddr, String protocolCode) {
        if (commAddr == null || commAddr.isEmpty()) {
            throw new BusinessException("通讯地址不能为空");
        }
        GatewayProtocolEnum protocol = GatewayProtocolEnum.getByCode(protocolCode);
        if (protocol == null) {
            throw new BusinessException("协议类型无效");
        }
        if (protocol == GatewayProtocolEnum.DLT645_2007) {
            if (!HEX_8_PATTERN.matcher(commAddr).matches()) {
                throw new BusinessException("DL/T645-2007协议通讯地址必须为8位16进制数字");
            }
        } else if (protocol == GatewayProtocolEnum.MQTT_ENERGY) {
            if (!ALPHANUM_32_PATTERN.matcher(commAddr).matches()) {
                throw new BusinessException("MQTT协议通讯地址只能包含英文和数字，最长32位");
            }
        }
        LambdaQueryWrapper<IotGateway> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(IotGateway::getCommAddr, commAddr);
        long count = gatewayService.count(wrapper);
        if (count > 0) {
            throw new BusinessException("通讯地址已存在，请更换");
        }
    }

    private void validateCommAddrForUpdate(String commAddr, String protocolCode, Long excludeId) {
        if (commAddr == null || commAddr.isEmpty()) {
            throw new BusinessException("通讯地址不能为空");
        }
        GatewayProtocolEnum protocol = GatewayProtocolEnum.getByCode(protocolCode);
        if (protocol == null) {
            throw new BusinessException("协议类型无效");
        }
        if (protocol == GatewayProtocolEnum.DLT645_2007) {
            if (!HEX_8_PATTERN.matcher(commAddr).matches()) {
                throw new BusinessException("DL/T645-2007协议通讯地址必须为8位16进制数字");
            }
        } else if (protocol == GatewayProtocolEnum.MQTT_ENERGY) {
            if (!ALPHANUM_32_PATTERN.matcher(commAddr).matches()) {
                throw new BusinessException("MQTT协议通讯地址只能包含英文和数字，最长32位");
            }
        }
        LambdaQueryWrapper<IotGateway> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(IotGateway::getCommAddr, commAddr).ne(IotGateway::getId, excludeId);
        long count = gatewayService.count(wrapper);
        if (count > 0) {
            throw new BusinessException("通讯地址已存在，请更换");
        }
    }

    private Long getCurrentUserId(HttpServletRequest request) {
        String token = request.getHeader("Authorization").substring(7);
        return jwtUtils.getUserIdFromToken(token);
    }

    private Long getCurrentTenantId(HttpServletRequest request) {
        String token = request.getHeader("Authorization").substring(7);
        return jwtUtils.getTenantIdFromToken(token);
    }

    private Integer getCurrentUserType(HttpServletRequest request) {
        String token = request.getHeader("Authorization").substring(7);
        return jwtUtils.getUserTypeFromToken(token);
    }

    @GetMapping
    @RequiresPermissions("gateway:list")
    public Result<PageResult<IotGateway>> list(
            @RequestParam(defaultValue = "1") Long page,
            @RequestParam(defaultValue = "20") Long size,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long tenantId,
            HttpServletRequest request) {

        Long currentTenantId = getCurrentTenantId(request);
        Integer currentUserType = getCurrentUserType(request);

        LambdaQueryWrapper<IotGateway> wrapper = new LambdaQueryWrapper<>();

        if (USER_TYPE_TENANT.equals(currentUserType)) {
            wrapper.eq(IotGateway::getTenantId, currentTenantId);
        } else if (tenantId != null) {
            wrapper.eq(IotGateway::getTenantId, tenantId);
        }

        if (status != null) {
            wrapper.eq(IotGateway::getStatus, status);
        }
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.and(w -> w.like(IotGateway::getGatewayNo, keyword)
                    .or()
                    .like(IotGateway::getGatewayName, keyword));
        }

        wrapper.orderByDesc(IotGateway::getCreateTime);

        Page<IotGateway> result = gatewayService.page(PageUtils.getPage(page, size), wrapper);
        return Result.success(PageUtils.convert(result));
    }

    @GetMapping("/{id}")
    @RequiresPermissions("gateway:list")
    public Result<IotGateway> getById(@PathVariable Long id, HttpServletRequest request) {
        Long currentTenantId = getCurrentTenantId(request);
        Integer currentUserType = getCurrentUserType(request);

        IotGateway gateway = gatewayService.getById(id);
        if (gateway == null) {
            throw new BusinessException("网关不存在");
        }

        if (USER_TYPE_TENANT.equals(currentUserType) && !gateway.getTenantId().equals(currentTenantId)) {
            throw new BusinessException("无权访问该网关");
        }

        return Result.success(gateway);
    }

    @PostMapping
    @RequiresPermissions("gateway:create")
    public Result<Void> create(@RequestBody IotGateway gateway, HttpServletRequest request) {
        Long currentTenantId = getCurrentTenantId(request);
        Integer currentUserType = getCurrentUserType(request);

        if (USER_TYPE_TENANT.equals(currentUserType)) {
            gateway.setTenantId(currentTenantId);
        } else {
            if (gateway.getTenantId() == null) {
                throw new BusinessException("请选择所属租户");
            }
        }

        gateway.setStatus(0);
        gateway.setGatewayNo(UUID.randomUUID().toString().replace("-", ""));
        validateCommAddr(gateway.getCommAddr(), gateway.getProtocolCode());
        gatewayService.save(gateway);
        return Result.success();
    }

    @PutMapping("/{id}")
    @RequiresPermissions("gateway:update")
    public Result<Void> update(@PathVariable Long id, @RequestBody IotGateway gateway, HttpServletRequest request) {
        Long currentTenantId = getCurrentTenantId(request);
        Integer currentUserType = getCurrentUserType(request);

        IotGateway existing = gatewayService.getById(id);
        if (existing == null) {
            throw new BusinessException("网关不存在");
        }

        if (USER_TYPE_TENANT.equals(currentUserType) && !existing.getTenantId().equals(currentTenantId)) {
            throw new BusinessException("无权修改该网关");
        }

        gateway.setId(id);

        if (USER_TYPE_TENANT.equals(currentUserType)) {
            gateway.setTenantId(currentTenantId);
        }

        validateCommAddrForUpdate(gateway.getCommAddr(), gateway.getProtocolCode(), id);
        gatewayService.updateById(gateway);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @RequiresPermissions("gateway:delete")
    public Result<Void> delete(@PathVariable Long id, HttpServletRequest request) {
        Long currentTenantId = getCurrentTenantId(request);
        Integer currentUserType = getCurrentUserType(request);

        IotGateway existing = gatewayService.getById(id);
        if (existing == null) {
            throw new BusinessException("网关不存在");
        }

        if (USER_TYPE_TENANT.equals(currentUserType) && !existing.getTenantId().equals(currentTenantId)) {
            throw new BusinessException("无权删除该网关");
        }

        gatewayService.removeById(id);
        return Result.success();
    }
}
