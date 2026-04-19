package com.zfh.iot.modules.archive.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zfh.iot.common.exception.BusinessException;
import com.zfh.iot.common.result.PageResult;
import com.zfh.iot.common.result.Result;
import com.zfh.iot.common.utils.PageUtils;
import com.zfh.iot.modules.archive.entity.IotMeter;
import com.zfh.iot.modules.archive.service.IotMeterService;
import com.zfh.iot.modules.auth.utils.JwtUtils;
import lombok.RequiredArgsConstructor;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/archive/meters")
@RequiredArgsConstructor
public class MeterController {

    private final IotMeterService meterService;
    private final JwtUtils jwtUtils;

    private static final Integer USER_TYPE_TENANT = 1;

    private Long getCurrentTenantId(HttpServletRequest request) {
        String token = request.getHeader("Authorization").substring(7);
        return jwtUtils.getTenantIdFromToken(token);
    }

    private Integer getCurrentUserType(HttpServletRequest request) {
        String token = request.getHeader("Authorization").substring(7);
        return jwtUtils.getUserTypeFromToken(token);
    }

    @GetMapping
    @RequiresPermissions("meter:list")
    public Result<PageResult<IotMeter>> list(
            @RequestParam(defaultValue = "1") Long page,
            @RequestParam(defaultValue = "20") Long size,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Integer meterType,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long tenantId,
            @RequestParam(required = false) Long gatewayId,
            HttpServletRequest request) {

        Long currentTenantId = getCurrentTenantId(request);
        Integer currentUserType = getCurrentUserType(request);

        LambdaQueryWrapper<IotMeter> wrapper = new LambdaQueryWrapper<>();

        if (USER_TYPE_TENANT.equals(currentUserType)) {
            wrapper.eq(IotMeter::getTenantId, currentTenantId);
        } else if (tenantId != null) {
            wrapper.eq(IotMeter::getTenantId, tenantId);
        }

        if (status != null) {
            wrapper.eq(IotMeter::getStatus, status);
        }
        if (meterType != null) {
            wrapper.eq(IotMeter::getMeterType, meterType);
        }
        if (gatewayId != null) {
            wrapper.eq(IotMeter::getGatewayId, gatewayId);
        }
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.and(w -> w.like(IotMeter::getMeterNo, keyword)
                    .or()
                    .like(IotMeter::getMeterName, keyword));
        }

        wrapper.orderByDesc(IotMeter::getCreateTime);

        Page<IotMeter> result = meterService.page(PageUtils.getPage(page, size), wrapper);
        return Result.success(PageUtils.convert(result));
    }

    @GetMapping("/{id}")
    @RequiresPermissions("meter:list")
    public Result<IotMeter> getById(@PathVariable Long id, HttpServletRequest request) {
        Long currentTenantId = getCurrentTenantId(request);
        Integer currentUserType = getCurrentUserType(request);

        IotMeter meter = meterService.getById(id);
        if (meter == null) {
            throw new BusinessException("表计不存在");
        }

        if (USER_TYPE_TENANT.equals(currentUserType) && !meter.getTenantId().equals(currentTenantId)) {
            throw new BusinessException("无权访问该表计");
        }

        return Result.success(meter);
    }

    @PostMapping
    @RequiresPermissions("meter:create")
    public Result<Void> create(@RequestBody IotMeter meter, HttpServletRequest request) {
        Long currentTenantId = getCurrentTenantId(request);
        Integer currentUserType = getCurrentUserType(request);

        if (USER_TYPE_TENANT.equals(currentUserType)) {
            meter.setTenantId(currentTenantId);
        } else {
            if (meter.getTenantId() == null) {
                throw new BusinessException("租户信息缺失");
            }
        }

        meter.setStatus(0);
        meterService.save(meter);
        return Result.success();
    }

    @PutMapping("/{id}")
    @RequiresPermissions("meter:update")
    public Result<Void> update(@PathVariable Long id, @RequestBody IotMeter meter, HttpServletRequest request) {
        Long currentTenantId = getCurrentTenantId(request);
        Integer currentUserType = getCurrentUserType(request);

        IotMeter existing = meterService.getById(id);
        if (existing == null) {
            throw new BusinessException("表计不存在");
        }

        if (USER_TYPE_TENANT.equals(currentUserType) && !existing.getTenantId().equals(currentTenantId)) {
            throw new BusinessException("无权修改该表计");
        }

        meter.setId(id);
        if (USER_TYPE_TENANT.equals(currentUserType)) {
            meter.setTenantId(currentTenantId);
        }

        meterService.updateById(meter);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @RequiresPermissions("meter:delete")
    public Result<Void> delete(@PathVariable Long id, HttpServletRequest request) {
        Long currentTenantId = getCurrentTenantId(request);
        Integer currentUserType = getCurrentUserType(request);

        IotMeter existing = meterService.getById(id);
        if (existing == null) {
            throw new BusinessException("表计不存在");
        }

        if (USER_TYPE_TENANT.equals(currentUserType) && !existing.getTenantId().equals(currentTenantId)) {
            throw new BusinessException("无权删除该表计");
        }

        meterService.removeById(id);
        return Result.success();
    }

    @PutMapping("/{id}/bind-gateway")
    @RequiresPermissions("meter:update")
    public Result<Void> bindGateway(@PathVariable Long id, @RequestParam Long gatewayId, HttpServletRequest request) {
        Long currentTenantId = getCurrentTenantId(request);
        Integer currentUserType = getCurrentUserType(request);

        IotMeter existing = meterService.getById(id);
        if (existing == null) {
            throw new BusinessException("表计不存在");
        }

        if (USER_TYPE_TENANT.equals(currentUserType) && !existing.getTenantId().equals(currentTenantId)) {
            throw new BusinessException("无权操作该表计");
        }

        IotMeter meter = new IotMeter();
        meter.setId(id);
        meter.setGatewayId(gatewayId);
        meterService.updateById(meter);
        return Result.success();
    }

    @PutMapping("/{id}/unbind-gateway")
    @RequiresPermissions("meter:update")
    public Result<Void> unbindGateway(@PathVariable Long id, HttpServletRequest request) {
        Long currentTenantId = getCurrentTenantId(request);
        Integer currentUserType = getCurrentUserType(request);

        IotMeter existing = meterService.getById(id);
        if (existing == null) {
            throw new BusinessException("表计不存在");
        }

        if (USER_TYPE_TENANT.equals(currentUserType) && !existing.getTenantId().equals(currentTenantId)) {
            throw new BusinessException("无权操作该表计");
        }

        IotMeter meter = new IotMeter();
        meter.setId(id);
        meter.setGatewayId(null);
        meterService.updateById(meter);
        return Result.success();
    }

    @PutMapping("/{id}/bind-group")
    @RequiresPermissions("meter:update")
    public Result<Void> bindGroup(@PathVariable Long id, @RequestParam Long groupId, HttpServletRequest request) {
        Long currentTenantId = getCurrentTenantId(request);
        Integer currentUserType = getCurrentUserType(request);

        IotMeter existing = meterService.getById(id);
        if (existing == null) {
            throw new BusinessException("表计不存在");
        }

        if (USER_TYPE_TENANT.equals(currentUserType) && !existing.getTenantId().equals(currentTenantId)) {
            throw new BusinessException("无权操作该表计");
        }

        IotMeter meter = new IotMeter();
        meter.setId(id);
        meter.setGroupId(groupId);
        meterService.updateById(meter);
        return Result.success();
    }

    @PutMapping("/{id}/unbind-group")
    @RequiresPermissions("meter:update")
    public Result<Void> unbindGroup(@PathVariable Long id, HttpServletRequest request) {
        Long currentTenantId = getCurrentTenantId(request);
        Integer currentUserType = getCurrentUserType(request);

        IotMeter existing = meterService.getById(id);
        if (existing == null) {
            throw new BusinessException("表计不存在");
        }

        if (USER_TYPE_TENANT.equals(currentUserType) && !existing.getTenantId().equals(currentTenantId)) {
            throw new BusinessException("无权操作该表计");
        }

        IotMeter meter = new IotMeter();
        meter.setId(id);
        meter.setGroupId(null);
        meterService.updateById(meter);
        return Result.success();
    }
}
