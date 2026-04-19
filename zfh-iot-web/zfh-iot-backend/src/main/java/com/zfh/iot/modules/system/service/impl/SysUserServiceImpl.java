package com.zfh.iot.modules.system.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zfh.iot.modules.auth.dto.UserInfoVO;
import com.zfh.iot.modules.system.entity.SysTenant;
import com.zfh.iot.modules.system.entity.SysUser;
import com.zfh.iot.modules.system.mapper.SysPermissionMapper;
import com.zfh.iot.modules.system.mapper.SysTenantMapper;
import com.zfh.iot.modules.system.mapper.SysUserMapper;
import com.zfh.iot.modules.system.service.SysUserService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;

@Service
public class SysUserServiceImpl extends ServiceImpl<SysUserMapper, SysUser> implements SysUserService {

    @Autowired
    private SysPermissionMapper permissionMapper;

    @Autowired
    private SysTenantMapper tenantMapper;

    @Override
    public SysUser getByUsername(String username) {
        return baseMapper.selectByUsername(username);
    }

    @Override
    @Cacheable(value = "user:permissions", key = "#userId")
    public Set<String> getUserPermissions(Long userId) {
        SysUser user = getById(userId);
        if (user != null && "admin".equals(user.getUsername())) {
            return permissionMapper.selectAllPermCodes();
        }
        return baseMapper.selectUserPermissions(userId);
    }

    @Override
    @Cacheable(value = "user:roles", key = "#userId")
    public Set<String> getUserRoles(Long userId) {
        SysUser user = getById(userId);
        if (user != null && "admin".equals(user.getUsername())) {
            Set<String> roles = new HashSet<>();
            roles.add("admin");
            return roles;
        }
        return baseMapper.selectUserRoles(userId);
    }

    @Override
    public UserInfoVO getUserInfoWithPermissions(Long userId) {
        SysUser user = getById(userId);
        if (user == null) {
            return null;
        }
        
        UserInfoVO userInfo = new UserInfoVO();
        BeanUtils.copyProperties(user, userInfo);
        
        if (user.getTenantId() != null) {
            SysTenant tenant = tenantMapper.selectById(user.getTenantId());
            if (tenant != null) {
                userInfo.setTenantName(tenant.getTenantName());
            }
        }
        
        Set<String> roles;
        Set<String> permissions;
        
        if ("admin".equals(user.getUsername())) {
            roles = new HashSet<>();
            roles.add("admin");
            permissions = permissionMapper.selectAllPermCodes();
        } else {
            roles = baseMapper.selectUserRoles(userId);
            permissions = baseMapper.selectUserPermissions(userId);
        }
        
        userInfo.setPermissions(permissions);
        userInfo.setRoles(roles);
        
        return userInfo;
    }
}
