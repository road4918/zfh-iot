package com.zfh.iot.modules.system.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zfh.iot.modules.system.entity.SysRole;
import com.zfh.iot.modules.system.mapper.SysRoleMapper;
import com.zfh.iot.modules.system.service.SysRoleService;
import org.springframework.stereotype.Service;

@Service
public class SysRoleServiceImpl extends ServiceImpl<SysRoleMapper, SysRole> implements SysRoleService {
}
