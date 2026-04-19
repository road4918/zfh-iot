package com.zfh.iot.modules.system.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zfh.iot.common.exception.BusinessException;
import com.zfh.iot.common.result.PageResult;
import com.zfh.iot.common.result.Result;
import com.zfh.iot.common.utils.PageUtils;
import com.zfh.iot.modules.auth.utils.JwtUtils;
import com.zfh.iot.modules.system.dto.RoleDTO;
import com.zfh.iot.modules.system.entity.SysRole;
import com.zfh.iot.modules.system.entity.SysRolePermission;
import com.zfh.iot.modules.system.entity.SysUser;
import com.zfh.iot.modules.system.entity.SysUserRole;
import com.zfh.iot.modules.system.mapper.SysPermissionMapper;
import com.zfh.iot.modules.system.service.SysRolePermissionService;
import com.zfh.iot.modules.system.service.SysRoleService;
import com.zfh.iot.modules.system.service.SysUserRoleService;
import com.zfh.iot.modules.system.service.SysUserService;
import lombok.RequiredArgsConstructor;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/system/roles")
@RequiredArgsConstructor
public class RoleController {

    private final SysRoleService roleService;
    private final SysRolePermissionService rolePermissionService;
    private final SysUserRoleService userRoleService;
    private final SysUserService sysUserService;
    private final SysPermissionMapper permissionMapper;
    private final JwtUtils jwtUtils;

    private static final Integer USER_TYPE_PLATFORM = 0;
    private static final Integer USER_TYPE_TENANT = 1;
    private static final Integer ROLE_TYPE_PLATFORM = 0;
    private static final Integer ROLE_TYPE_TENANT = 1;

    private Long getCurrentUserId(HttpServletRequest request) {
        String token = request.getHeader("Authorization").substring(7);
        return jwtUtils.getUserIdFromToken(token);
    }

    private Integer getCurrentUserType(HttpServletRequest request) {
        String token = request.getHeader("Authorization").substring(7);
        return jwtUtils.getUserTypeFromToken(token);
    }

    private Long getCurrentTenantId(HttpServletRequest request) {
        String token = request.getHeader("Authorization").substring(7);
        return jwtUtils.getTenantIdFromToken(token);
    }

    private boolean isAdmin(Long userId) {
        SysUser user = sysUserService.getById(userId);
        return user != null && "admin".equals(user.getUsername());
    }

    private boolean isPlatformUser(Integer userType) {
        return USER_TYPE_PLATFORM.equals(userType);
    }

    private boolean isTenantUser(Integer userType) {
        return USER_TYPE_TENANT.equals(userType);
    }

    private boolean isPlatformRole(Integer roleType) {
        return ROLE_TYPE_PLATFORM.equals(roleType);
    }

    private boolean isTenantRole(Integer roleType) {
        return ROLE_TYPE_TENANT.equals(roleType);
    }

    private void validateRoleAccess(SysRole targetRole, Long currentUserId, Integer currentUserType) {
        if (isAdmin(currentUserId)) {
            return;
        }

        if (isPlatformUser(currentUserType)) {
            return;
        }

        SysUser currentUser = sysUserService.getById(currentUserId);
        if (targetRole == null || currentUser == null) {
            throw new BusinessException("角色不存在");
        }

        if (!targetRole.getTenantId().equals(currentUser.getTenantId())) {
            throw new BusinessException("只能操作同租户下的角色");
        }

        if (isPlatformRole(targetRole.getRoleType())) {
            throw new BusinessException("租户用户不能操作平台角色");
        }
    }

    @GetMapping
    @RequiresPermissions("role:list")
    public Result<PageResult<SysRole>> list(
            @RequestParam(defaultValue = "1") Long page,
            @RequestParam(defaultValue = "20") Long size,
            @RequestParam(required = false) Integer roleType,
            @RequestParam(required = false) Long tenantId,
            HttpServletRequest request) {

        Long currentUserId = getCurrentUserId(request);
        Integer currentUserType = getCurrentUserType(request);
        Long currentTenantId = getCurrentTenantId(request);

        LambdaQueryWrapper<SysRole> wrapper = new LambdaQueryWrapper<>();

        if (isTenantUser(currentUserType)) {
            wrapper.eq(SysRole::getTenantId, currentTenantId);
            wrapper.eq(SysRole::getRoleType, ROLE_TYPE_TENANT);
        } else {
            if (tenantId != null) {
                wrapper.eq(SysRole::getTenantId, tenantId);
            }
            if (roleType != null) {
                wrapper.eq(SysRole::getRoleType, roleType);
            }
        }

        if (!isAdmin(currentUserId)) {
            wrapper.ne(SysRole::getRoleCode, "admin");
        }

        Page<SysRole> result = roleService.page(PageUtils.getPage(page, size), wrapper);
        return Result.success(PageUtils.convert(result));
    }

    @GetMapping("/{id}")
    @RequiresPermissions("role:list")
    public Result<SysRole> getById(@PathVariable Long id, HttpServletRequest request) {
        Long currentUserId = getCurrentUserId(request);
        Integer currentUserType = getCurrentUserType(request);

        SysRole role = roleService.getById(id);
        if (role == null) {
            throw new BusinessException("角色不存在");
        }

        validateRoleAccess(role, currentUserId, currentUserType);

        return Result.success(role);
    }

    @PostMapping
    @RequiresPermissions("role:create")
    public Result<Void> create(@RequestBody RoleDTO dto, HttpServletRequest request) {
        Long currentUserId = getCurrentUserId(request);
        Integer currentUserType = getCurrentUserType(request);
        Long currentTenantId = getCurrentTenantId(request);

        if (dto.getRoleType() == null) {
            throw new BusinessException("角色类型不能为空");
        }

        if (isTenantRole(dto.getRoleType())) {
            if (dto.getTenantId() == null) {
                throw new BusinessException("租户角色必须选择所属租户");
            }
            if (isTenantUser(currentUserType) && !dto.getTenantId().equals(currentTenantId)) {
                throw new BusinessException("只能在自己租户下创建角色");
            }
        } else if (isPlatformRole(dto.getRoleType())) {
            dto.setTenantId(null);
            if (isTenantUser(currentUserType)) {
                throw new BusinessException("租户用户不能创建平台角色");
            }
        }

        SysRole role = new SysRole();
        BeanUtils.copyProperties(dto, role);
        role.setTenantId(dto.getTenantId());
        role.setCreatedBy(currentUserId);
        roleService.save(role);
        return Result.success();
    }

    @PutMapping("/{id}")
    @RequiresPermissions("role:update")
    public Result<Void> update(@PathVariable Long id, @RequestBody RoleDTO dto, HttpServletRequest request) {
        SysRole existingRole = roleService.getById(id);
        if (existingRole != null && "admin".equals(existingRole.getRoleCode())) {
            throw new BusinessException("系统管理员角色不允许修改");
        }

        Long currentUserId = getCurrentUserId(request);
        Integer currentUserType = getCurrentUserType(request);

        validateRoleAccess(existingRole, currentUserId, currentUserType);

        if (dto.getRoleType() != null) {
            if (isTenantRole(dto.getRoleType()) && dto.getTenantId() == null) {
                throw new BusinessException("租户角色必须选择所属租户");
            }
            if (isTenantUser(currentUserType) && isPlatformRole(dto.getRoleType())) {
                throw new BusinessException("租户用户不能修改平台角色");
            }
        }

        SysRole role = new SysRole();
        BeanUtils.copyProperties(dto, role);
        role.setId(id);
        roleService.updateById(role);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @RequiresPermissions("role:delete")
    public Result<Void> delete(@PathVariable Long id, HttpServletRequest request) {
        SysRole existingRole = roleService.getById(id);
        if (existingRole != null && "admin".equals(existingRole.getRoleCode())) {
            throw new BusinessException("系统管理员角色不允许删除");
        }

        Long currentUserId = getCurrentUserId(request);
        Integer currentUserType = getCurrentUserType(request);

        validateRoleAccess(existingRole, currentUserId, currentUserType);

        Long userCount = userRoleService.count(
            new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getRoleId, id)
        );
        if (userCount > 0) {
            throw new BusinessException("该角色已被" + userCount + "个用户使用，不能删除");
        }

        roleService.removeById(id);
        return Result.success();
    }

    @GetMapping("/{id}/permissions")
    @RequiresPermissions("role:list")
    public Result<List<Long>> getRolePermissions(@PathVariable Long id, HttpServletRequest request) {
        Long currentUserId = getCurrentUserId(request);
        Integer currentUserType = getCurrentUserType(request);

        SysRole role = roleService.getById(id);
        validateRoleAccess(role, currentUserId, currentUserType);

        List<SysRolePermission> list = rolePermissionService.list(
            new LambdaQueryWrapper<SysRolePermission>()
                .eq(SysRolePermission::getRoleId, id)
        );
        return Result.success(list.stream()
                .map(SysRolePermission::getPermId)
                .collect(Collectors.toList()));
    }

    @PutMapping("/{id}/permissions")
    @RequiresPermissions("role:update")
    public Result<Void> assignPermissions(@PathVariable Long id, @RequestBody List<Long> permIds, HttpServletRequest request) {
        SysRole existingRole = roleService.getById(id);
        if (existingRole != null && "admin".equals(existingRole.getRoleCode())) {
            throw new BusinessException("系统管理员角色不允许修改权限");
        }

        Long currentUserId = getCurrentUserId(request);
        Integer currentUserType = getCurrentUserType(request);

        validateRoleAccess(existingRole, currentUserId, currentUserType);

        if (!isAdmin(currentUserId) && permIds != null && !permIds.isEmpty()) {
            Set<Long> userPermissionIds = permissionMapper.selectPermissionIdsByUserId(currentUserId);

            for (Long permId : permIds) {
                if (!userPermissionIds.contains(permId)) {
                    throw new BusinessException("只能分配自己拥有的权限");
                }
            }
        }

        rolePermissionService.remove(
            new LambdaQueryWrapper<SysRolePermission>()
                .eq(SysRolePermission::getRoleId, id)
        );

        if (permIds != null && !permIds.isEmpty()) {
            List<SysRolePermission> rolePerms = permIds.stream()
                    .map(permId -> {
                        SysRolePermission rp = new SysRolePermission();
                        rp.setRoleId(id);
                        rp.setPermId(permId);
                        return rp;
                    })
                    .collect(Collectors.toList());
            rolePermissionService.saveBatch(rolePerms);
        }

        return Result.success();
    }
}
