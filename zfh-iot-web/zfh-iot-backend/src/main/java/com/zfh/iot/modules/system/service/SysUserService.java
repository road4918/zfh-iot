package com.zfh.iot.modules.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zfh.iot.modules.auth.dto.UserInfoVO;
import com.zfh.iot.modules.system.entity.SysUser;

import java.util.Set;

public interface SysUserService extends IService<SysUser> {
    
    SysUser getByUsername(String username);
    
    Set<String> getUserPermissions(Long userId);
    
    Set<String> getUserRoles(Long userId);
    
    UserInfoVO getUserInfoWithPermissions(Long userId);
}
