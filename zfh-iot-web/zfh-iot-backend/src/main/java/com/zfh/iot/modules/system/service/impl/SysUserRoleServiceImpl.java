package com.zfh.iot.modules.system.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zfh.iot.modules.system.entity.SysUserRole;
import com.zfh.iot.modules.system.mapper.SysUserRoleMapper;
import com.zfh.iot.modules.system.service.SysUserRoleService;
import org.springframework.stereotype.Service;

@Service
public class SysUserRoleServiceImpl extends ServiceImpl<SysUserRoleMapper, SysUserRole> implements SysUserRoleService {
}
