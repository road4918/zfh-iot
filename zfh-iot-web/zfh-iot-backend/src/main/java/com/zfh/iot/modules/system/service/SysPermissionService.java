package com.zfh.iot.modules.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zfh.iot.modules.system.entity.SysPermission;

import java.util.List;

public interface SysPermissionService extends IService<SysPermission> {
    List<SysPermission> getPermissionTree();
}
