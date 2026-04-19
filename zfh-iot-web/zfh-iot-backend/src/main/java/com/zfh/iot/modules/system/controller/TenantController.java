package com.zfh.iot.modules.system.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zfh.iot.common.exception.BusinessException;
import com.zfh.iot.common.result.PageResult;
import com.zfh.iot.common.result.Result;
import com.zfh.iot.common.utils.PageUtils;
import com.zfh.iot.modules.system.dto.TenantDTO;
import com.zfh.iot.modules.system.entity.SysTenant;
import com.zfh.iot.modules.system.entity.SysUser;
import com.zfh.iot.modules.system.service.SysTenantService;
import com.zfh.iot.modules.system.service.SysUserService;
import lombok.RequiredArgsConstructor;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/system/tenants")
@RequiredArgsConstructor
public class TenantController {

    private final SysTenantService tenantService;
    private final SysUserService userService;

    @GetMapping
    @RequiresPermissions("tenant:list")
    public Result<PageResult<SysTenant>> list(
            @RequestParam(defaultValue = "1") Long page,
            @RequestParam(defaultValue = "20") Long size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status) {
        
        LambdaQueryWrapper<SysTenant> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.like(SysTenant::getTenantName, keyword)
                   .or()
                   .like(SysTenant::getTenantCode, keyword);
        }
        if (status != null) {
            wrapper.eq(SysTenant::getStatus, status);
        }
        wrapper.orderByDesc(SysTenant::getCreateTime);
        
        Page<SysTenant> result = tenantService.page(PageUtils.getPage(page, size), wrapper);
        return Result.success(PageUtils.convert(result));
    }

    @GetMapping("/{id}")
    @RequiresPermissions("tenant:list")
    public Result<SysTenant> getById(@PathVariable Long id) {
        return Result.success(tenantService.getById(id));
    }

    @PostMapping
    @RequiresPermissions("tenant:create")
    public Result<Void> create(@RequestBody TenantDTO dto) {
        SysTenant tenant = new SysTenant();
        BeanUtils.copyProperties(dto, tenant);
        tenantService.save(tenant);
        return Result.success();
    }

    @PutMapping("/{id}")
    @RequiresPermissions("tenant:update")
    public Result<Void> update(@PathVariable Long id, @RequestBody TenantDTO dto) {
        SysTenant tenant = new SysTenant();
        BeanUtils.copyProperties(dto, tenant);
        tenant.setId(id);
        tenantService.updateById(tenant);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @RequiresPermissions("tenant:delete")
    public Result<Void> delete(@PathVariable Long id) {
        Long userCount = userService.count(
            new LambdaQueryWrapper<SysUser>().eq(SysUser::getTenantId, id)
        );
        if (userCount > 0) {
            throw new BusinessException("该租户下存在" + userCount + "个用户，不能删除");
        }
        
        tenantService.removeById(id);
        return Result.success();
    }

    @PutMapping("/{id}/status")
    @RequiresPermissions("tenant:update")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        SysTenant tenant = new SysTenant();
        tenant.setId(id);
        tenant.setStatus(status);
        tenantService.updateById(tenant);
        return Result.success();
    }
}
