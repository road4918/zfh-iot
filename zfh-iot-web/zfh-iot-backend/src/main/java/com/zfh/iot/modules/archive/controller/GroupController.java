package com.zfh.iot.modules.archive.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zfh.iot.common.exception.BusinessException;
import com.zfh.iot.common.result.Result;
import com.zfh.iot.modules.archive.entity.IotGroup;
import com.zfh.iot.modules.archive.entity.IotMeter;
import com.zfh.iot.modules.archive.service.IotGroupService;
import com.zfh.iot.modules.archive.service.IotMeterService;
import com.zfh.iot.modules.auth.utils.JwtUtils;
import lombok.RequiredArgsConstructor;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/archive/groups")
@RequiredArgsConstructor
public class GroupController {

    private final IotGroupService groupService;
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

    @GetMapping("/tree")
    @RequiresPermissions("group:list")
    public Result<List<IotGroup>> getTree(
            @RequestParam(required = false) Long tenantId,
            HttpServletRequest request) {
        Long currentTenantId = getCurrentTenantId(request);
        Integer currentUserType = getCurrentUserType(request);

        LambdaQueryWrapper<IotGroup> wrapper = new LambdaQueryWrapper<>();
        if (USER_TYPE_TENANT.equals(currentUserType)) {
            wrapper.eq(IotGroup::getTenantId, currentTenantId);
        } else if (tenantId != null) {
            wrapper.eq(IotGroup::getTenantId, tenantId);
        }
        wrapper.orderByAsc(IotGroup::getSortOrder);

        List<IotGroup> allGroups = groupService.list(wrapper);
        List<IotGroup> tree = buildTree(allGroups, 0L);
        return Result.success(tree);
    }

    private List<IotGroup> buildTree(List<IotGroup> groups, Long parentId) {
        return groups.stream()
                .filter(g -> g.getParentId().equals(parentId))
                .peek(g -> g.setChildren(buildTree(groups, g.getId())))
                .collect(Collectors.toList());
    }

    @GetMapping
    @RequiresPermissions("group:list")
    public Result<List<IotGroup>> list(
            @RequestParam(required = false) Long tenantId,
            HttpServletRequest request) {
        Long currentTenantId = getCurrentTenantId(request);
        Integer currentUserType = getCurrentUserType(request);

        LambdaQueryWrapper<IotGroup> wrapper = new LambdaQueryWrapper<>();
        if (USER_TYPE_TENANT.equals(currentUserType)) {
            wrapper.eq(IotGroup::getTenantId, currentTenantId);
        } else if (tenantId != null) {
            wrapper.eq(IotGroup::getTenantId, tenantId);
        }

        return Result.success(groupService.list(wrapper));
    }

    @PostMapping
    @RequiresPermissions("group:create")
    public Result<Void> create(@RequestBody IotGroup group, HttpServletRequest request) {
        Long currentTenantId = getCurrentTenantId(request);
        Integer currentUserType = getCurrentUserType(request);

        if (USER_TYPE_TENANT.equals(currentUserType)) {
            group.setTenantId(currentTenantId);
        } else {
            if (group.getTenantId() == null) {
                group.setTenantId(currentTenantId);
            }
        }

        groupService.save(group);
        return Result.success();
    }

    @PutMapping("/{id}")
    @RequiresPermissions("group:update")
    public Result<Void> update(@PathVariable Long id, @RequestBody IotGroup group, HttpServletRequest request) {
        Long currentTenantId = getCurrentTenantId(request);
        Integer currentUserType = getCurrentUserType(request);

        IotGroup existing = groupService.getById(id);
        if (existing == null) {
            throw new BusinessException("群组不存在");
        }

        if (USER_TYPE_TENANT.equals(currentUserType) && !existing.getTenantId().equals(currentTenantId)) {
            throw new BusinessException("无权修改该群组");
        }

        group.setId(id);
        if (USER_TYPE_TENANT.equals(currentUserType)) {
            group.setTenantId(currentTenantId);
        }

        groupService.updateById(group);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @RequiresPermissions("group:delete")
    public Result<Void> delete(@PathVariable Long id, HttpServletRequest request) {
        Long currentTenantId = getCurrentTenantId(request);
        Integer currentUserType = getCurrentUserType(request);

        IotGroup existing = groupService.getById(id);
        if (existing == null) {
            throw new BusinessException("群组不存在");
        }

        if (USER_TYPE_TENANT.equals(currentUserType) && !existing.getTenantId().equals(currentTenantId)) {
            throw new BusinessException("无权删除该群组");
        }

        List<IotGroup> children = groupService.list(
            new LambdaQueryWrapper<IotGroup>().eq(IotGroup::getParentId, id)
        );
        if (!children.isEmpty()) {
            throw new BusinessException("该群组下存在子群组，请先删除子群组");
        }

        List<IotMeter> boundMeters = meterService.list(
            new LambdaQueryWrapper<IotMeter>().eq(IotMeter::getGroupId, id)
        );
        for (IotMeter m : boundMeters) {
            IotMeter update = new IotMeter();
            update.setId(m.getId());
            update.setGroupId(null);
            meterService.updateById(update);
        }

        groupService.removeById(id);
        return Result.success();
    }

    @PostMapping("/{id}/meters")
    @RequiresPermissions("group:update")
    public Result<Void> addMeters(@PathVariable Long id, @RequestBody List<Long> meterIds, HttpServletRequest request) {
        Long currentTenantId = getCurrentTenantId(request);
        Integer currentUserType = getCurrentUserType(request);

        IotGroup existing = groupService.getById(id);
        if (existing == null) {
            throw new BusinessException("群组不存在");
        }

        if (USER_TYPE_TENANT.equals(currentUserType) && !existing.getTenantId().equals(currentTenantId)) {
            throw new BusinessException("无权操作该群组");
        }

        List<IotMeter> meters = meterService.listByIds(meterIds);
        for (IotMeter meter : meters) {
            if (meter.getGroupId() != null && !meter.getGroupId().equals(id)) {
                throw new BusinessException("表计 " + meter.getMeterName() + " 已绑定其他群组，请先解绑");
            }
        }

        for (Long meterId : meterIds) {
            IotMeter update = new IotMeter();
            update.setId(meterId);
            update.setGroupId(id);
            meterService.updateById(update);
        }
        return Result.success();
    }

    @DeleteMapping("/{id}/meters/{meterId}")
    @RequiresPermissions("group:update")
    public Result<Void> removeMeter(@PathVariable Long id, @PathVariable Long meterId, HttpServletRequest request) {
        Long currentTenantId = getCurrentTenantId(request);
        Integer currentUserType = getCurrentUserType(request);

        IotGroup existing = groupService.getById(id);
        if (existing == null) {
            throw new BusinessException("群组不存在");
        }

        if (USER_TYPE_TENANT.equals(currentUserType) && !existing.getTenantId().equals(currentTenantId)) {
            throw new BusinessException("无权操作该群组");
        }

        IotMeter meter = meterService.getById(meterId);
        if (meter == null || !id.equals(meter.getGroupId())) {
            throw new BusinessException("该表计不属于此群组");
        }

        IotMeter update = new IotMeter();
        update.setId(meterId);
        update.setGroupId(null);
        meterService.updateById(update);
        return Result.success();
    }

    @GetMapping("/{id}/meters")
    @RequiresPermissions("group:list")
    public Result<List<IotMeter>> getGroupMeters(@PathVariable Long id, HttpServletRequest request) {
        Long currentTenantId = getCurrentTenantId(request);
        Integer currentUserType = getCurrentUserType(request);

        IotGroup existing = groupService.getById(id);
        if (existing == null) {
            throw new BusinessException("群组不存在");
        }

        if (USER_TYPE_TENANT.equals(currentUserType) && !existing.getTenantId().equals(currentTenantId)) {
            throw new BusinessException("无权访问该群组");
        }

        List<IotMeter> meters = meterService.list(
            new LambdaQueryWrapper<IotMeter>().eq(IotMeter::getGroupId, id)
        );
        return Result.success(meters);
    }
}
