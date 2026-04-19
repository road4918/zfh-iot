package com.zfh.iot.modules.system.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zfh.iot.common.exception.BusinessException;
import com.zfh.iot.common.result.PageResult;
import com.zfh.iot.common.result.Result;
import com.zfh.iot.common.utils.PageUtils;
import com.zfh.iot.common.utils.PasswordUtils;
import com.zfh.iot.modules.auth.utils.JwtUtils;
import com.zfh.iot.modules.system.dto.ChangePasswordDTO;
import com.zfh.iot.modules.system.dto.UserDTO;
import com.zfh.iot.modules.system.entity.SysUser;
import com.zfh.iot.modules.system.entity.SysUserRole;
import com.zfh.iot.modules.system.service.SysUserRoleService;
import com.zfh.iot.modules.system.service.SysUserService;
import lombok.RequiredArgsConstructor;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.apache.shiro.crypto.hash.SimpleHash;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 用户管理控制器
 * 支持平台用户和租户用户两种类型
 * 平台用户(userType=0): 可以管理所有租户的用户
 * 租户用户(userType=1): 只能管理自己租户下的用户
 */
@RestController
@RequestMapping("/system/users")
@RequiredArgsConstructor
public class UserController {

    private final SysUserService userService;
    private final SysUserRoleService userRoleService;
    private final JwtUtils jwtUtils;

    // 用户类型常量
    private static final Integer USER_TYPE_PLATFORM = 0;  // 平台用户
    private static final Integer USER_TYPE_TENANT = 1;    // 租户用户

    /**
     * 从请求中获取当前用户ID
     */
    private Long getCurrentUserId(HttpServletRequest request) {
        String token = request.getHeader("Authorization").substring(7);
        return jwtUtils.getUserIdFromToken(token);
    }

    /**
     * 从请求中获取当前用户类型
     */
    private Integer getCurrentUserType(HttpServletRequest request) {
        String token = request.getHeader("Authorization").substring(7);
        return jwtUtils.getUserTypeFromToken(token);
    }

    /**
     * 从请求中获取当前用户租户ID
     */
    private Long getCurrentTenantId(HttpServletRequest request) {
        String token = request.getHeader("Authorization").substring(7);
        return jwtUtils.getTenantIdFromToken(token);
    }

    /**
     * 检查是否是超级管理员
     */
    private boolean isAdmin(Long userId) {
        SysUser user = userService.getById(userId);
        return user != null && "admin".equals(user.getUsername());
    }

    /**
     * 检查当前用户是否有权限操作目标用户
     * 平台用户可以操作所有用户（除了admin）
     * 租户用户只能操作自己租户下的用户
     */
    private void checkUserPermission(Long targetUserId, Long currentUserId, Integer currentUserType) {
        if (isAdmin(currentUserId)) {
            return;
        }
        
        // 平台用户可以操作所有用户
        if (USER_TYPE_PLATFORM.equals(currentUserType)) {
            return;
        }
        
        // 租户用户只能操作自己租户下的用户
        SysUser targetUser = userService.getById(targetUserId);
        SysUser currentUser = userService.getById(currentUserId);
        
        if (targetUser == null || currentUser == null) {
            throw new BusinessException("用户不存在");
        }
        
        if (!targetUser.getTenantId().equals(currentUser.getTenantId())) {
            throw new BusinessException("只能操作同租户下的用户");
        }
    }

    /**
     * 获取用户列表
     * 平台用户：可以看到所有用户
     * 租户用户：只能看到自己租户下的用户
     */
    @GetMapping
    @RequiresPermissions("user:list")
    public Result<PageResult<SysUser>> list(
            @RequestParam(defaultValue = "1") Long page,
            @RequestParam(defaultValue = "20") Long size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer userType,
            @RequestParam(required = false) Long tenantId,
            HttpServletRequest request) {
        
        Long currentUserId = getCurrentUserId(request);
        Integer currentUserType = getCurrentUserType(request);
        Long currentTenantId = getCurrentTenantId(request);
        
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        
        // 租户用户只能看到自己租户下的用户
        if (USER_TYPE_TENANT.equals(currentUserType)) {
            wrapper.eq(SysUser::getTenantId, currentTenantId);
        } else if (tenantId != null) {
            // 平台用户可以根据tenantId筛选
            wrapper.eq(SysUser::getTenantId, tenantId);
        }
        
        // 根据用户类型筛选
        if (userType != null) {
            wrapper.eq(SysUser::getUserType, userType);
        }
        
        // 关键字搜索
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.and(w -> w.like(SysUser::getUsername, keyword)
                   .or()
                   .like(SysUser::getRealName, keyword));
        }
        
        // 排除admin用户（除了admin自己）
        if (!isAdmin(currentUserId)) {
            wrapper.ne(SysUser::getUsername, "admin");
        }
        
        wrapper.orderByDesc(SysUser::getCreateTime);
        
        Page<SysUser> result = userService.page(PageUtils.getPage(page, size), wrapper);
        return Result.success(PageUtils.convert(result));
    }

    /**
     * 获取用户详情
     */
    @GetMapping("/{id}")
    @RequiresPermissions("user:list")
    public Result<SysUser> getById(@PathVariable Long id, HttpServletRequest request) {
        Long currentUserId = getCurrentUserId(request);
        Integer currentUserType = getCurrentUserType(request);
        
        SysUser user = userService.getById(id);
        
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        
        checkUserPermission(id, currentUserId, currentUserType);
        
        return Result.success(user);
    }

    /**
     * 创建用户
     * 租户类型用户必须选择tenantId
     * 平台类型用户不需要tenantId
     */
    @PostMapping
    @RequiresPermissions("user:create")
    public Result<Map<String, String>> create(@RequestBody UserDTO dto, HttpServletRequest request) {
        // 校验用户名唯一性
        if (userService.getByUsername(dto.getUsername()) != null) {
            throw new BusinessException("用户名已存在");
        }
        
        Long currentUserId = getCurrentUserId(request);
        Integer currentUserType = getCurrentUserType(request);
        Long currentTenantId = getCurrentTenantId(request);
        
        // 校验用户类型
        if (dto.getUserType() == null) {
            throw new BusinessException("用户类型不能为空");
        }
        
        // 如果是租户用户类型，必须选择租户
        if (USER_TYPE_TENANT.equals(dto.getUserType())) {
            if (dto.getTenantId() == null) {
                throw new BusinessException("租户用户必须选择所属租户");
            }
            // 租户用户只能在自己租户下创建用户
            if (USER_TYPE_TENANT.equals(currentUserType) && !dto.getTenantId().equals(currentTenantId)) {
                throw new BusinessException("只能在自己租户下创建用户");
            }
        } else if (USER_TYPE_PLATFORM.equals(dto.getUserType())) {
            // 平台用户不需要tenantId，设置为null
            dto.setTenantId(null);
            // 只有平台用户才能创建平台用户
            if (USER_TYPE_TENANT.equals(currentUserType)) {
                throw new BusinessException("租户用户不能创建平台用户");
            }
        }
        
        SysUser user = new SysUser();
        BeanUtils.copyProperties(dto, user);
        user.setTenantId(dto.getTenantId());
        user.setCreatedBy(currentUserId);
        
        // 生成随机密码
        String plainPassword = PasswordUtils.generateRandomPassword(8);
        String encryptedPassword = new SimpleHash("SHA-256", plainPassword, null, 1).toHex();
        user.setPassword(encryptedPassword);
        
        userService.save(user);
        
        // 保存用户角色关系
        saveUserRoles(user.getId(), dto.getRoleIds());
        
        Map<String, String> result = new HashMap<>();
        result.put("password", plainPassword);
        return Result.success(result);
    }

    /**
     * 更新用户
     */
    @PutMapping("/{id}")
    @RequiresPermissions("user:update")
    public Result<Void> update(@PathVariable Long id, @RequestBody UserDTO dto, HttpServletRequest request) {
        SysUser existingUser = userService.getById(id);
        if (existingUser != null && "admin".equals(existingUser.getUsername())) {
            throw new BusinessException("系统管理员账号不允许修改");
        }
        
        Long currentUserId = getCurrentUserId(request);
        Integer currentUserType = getCurrentUserType(request);
        
        checkUserPermission(id, currentUserId, currentUserType);
        
        // 校验用户类型变更
        if (dto.getUserType() != null) {
            if (USER_TYPE_TENANT.equals(dto.getUserType()) && dto.getTenantId() == null) {
                throw new BusinessException("租户用户必须选择所属租户");
            }
            // 租户用户不能将用户修改为平台用户
            if (USER_TYPE_TENANT.equals(currentUserType) && USER_TYPE_PLATFORM.equals(dto.getUserType())) {
                throw new BusinessException("租户用户不能创建平台用户");
            }
        }
        
        SysUser user = new SysUser();
        BeanUtils.copyProperties(dto, user);
        user.setId(id);
        user.setPassword(null); // 不更新密码
        user.setUsername(null); // 不更新用户名
        
        userService.updateById(user);
        
        // 更新用户角色关系
        userRoleService.remove(new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, id));
        saveUserRoles(id, dto.getRoleIds());
        
        return Result.success();
    }

    /**
     * 删除用户
     */
    @DeleteMapping("/{id}")
    @RequiresPermissions("user:delete")
    public Result<Void> delete(@PathVariable Long id, HttpServletRequest request) {
        SysUser existingUser = userService.getById(id);
        if (existingUser != null && "admin".equals(existingUser.getUsername())) {
            throw new BusinessException("系统管理员账号不允许删除");
        }
        
        Long currentUserId = getCurrentUserId(request);
        Integer currentUserType = getCurrentUserType(request);
        
        checkUserPermission(id, currentUserId, currentUserType);
        
        userService.removeById(id);
        return Result.success();
    }

    /**
     * 重置密码
     */
    @PutMapping("/{id}/reset-password")
    @RequiresPermissions("user:reset-password")
    public Result<Map<String, String>> resetPassword(@PathVariable Long id, HttpServletRequest request) {
        SysUser existingUser = userService.getById(id);
        if (existingUser != null && "admin".equals(existingUser.getUsername())) {
            throw new BusinessException("系统管理员账号不允许重置密码");
        }
        
        Long currentUserId = getCurrentUserId(request);
        Integer currentUserType = getCurrentUserType(request);
        
        checkUserPermission(id, currentUserId, currentUserType);
        
        String plainPassword = PasswordUtils.generateRandomPassword(8);
        String encryptedPassword = new SimpleHash("SHA-256", plainPassword, null, 1).toHex();
        
        SysUser user = new SysUser();
        user.setId(id);
        user.setPassword(encryptedPassword);
        userService.updateById(user);
        
        Map<String, String> result = new HashMap<>();
        result.put("password", plainPassword);
        return Result.success(result);
    }

    /**
     * 当前用户修改密码
     */
    @PutMapping("/change-password")
    public Result<Void> changePassword(@Valid @RequestBody ChangePasswordDTO dto, HttpServletRequest request) {
        Long currentUserId = getCurrentUserId(request);
        SysUser currentUser = userService.getById(currentUserId);

        if (currentUser == null) {
            throw new BusinessException("用户不存在");
        }

        String oldEncryptedPassword = new SimpleHash("SHA-256", dto.getOldPassword(), null, 1).toHex();
        if (!oldEncryptedPassword.equals(currentUser.getPassword())) {
            throw new BusinessException("旧密码不正确");
        }

        if (!dto.getNewPassword().equals(dto.getConfirmPassword())) {
            throw new BusinessException("两次输入的新密码不一致");
        }

        if (dto.getOldPassword().equals(dto.getNewPassword())) {
            throw new BusinessException("新密码不能与旧密码相同");
        }

        PasswordUtils.validateComplexPassword(dto.getNewPassword());

        SysUser user = new SysUser();
        user.setId(currentUserId);
        user.setPassword(new SimpleHash("SHA-256", dto.getNewPassword(), null, 1).toHex());
        userService.updateById(user);

        return Result.success();
    }

    /**
     * 获取用户角色
     */
    @GetMapping("/{id}/roles")
    @RequiresPermissions("user:list")
    public Result<List<Long>> getUserRoles(@PathVariable Long id, HttpServletRequest request) {
        Long currentUserId = getCurrentUserId(request);
        Integer currentUserType = getCurrentUserType(request);
        
        checkUserPermission(id, currentUserId, currentUserType);
        
        List<SysUserRole> list = userRoleService.list(
            new LambdaQueryWrapper<SysUserRole>()
                .eq(SysUserRole::getUserId, id)
        );
        return Result.success(list.stream()
                .map(SysUserRole::getRoleId)
                .collect(Collectors.toList()));
    }

    /**
     * 保存用户角色关系
     */
    private void saveUserRoles(Long userId, List<Long> roleIds) {
        if (roleIds != null && !roleIds.isEmpty()) {
            List<SysUserRole> userRoles = roleIds.stream()
                    .map(roleId -> {
                        SysUserRole userRole = new SysUserRole();
                        userRole.setUserId(userId);
                        userRole.setRoleId(roleId);
                        return userRole;
                    })
                    .collect(Collectors.toList());
            userRoleService.saveBatch(userRoles);
        }
    }
}
