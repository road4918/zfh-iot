package com.zfh.iot.modules.system.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zfh.iot.modules.system.entity.SysPermission;
import com.zfh.iot.modules.system.mapper.SysPermissionMapper;
import com.zfh.iot.modules.system.service.SysPermissionService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class SysPermissionServiceImpl extends ServiceImpl<SysPermissionMapper, SysPermission> implements SysPermissionService {

    @Override
    public List<SysPermission> getPermissionTree() {
        List<SysPermission> allPermissions = baseMapper.selectAllPermissions();
        return buildTree(allPermissions);
    }

    private List<SysPermission> buildTree(List<SysPermission> permissions) {
        Map<Long, SysPermission> map = permissions.stream()
                .collect(Collectors.toMap(SysPermission::getId, p -> p));
        
        List<SysPermission> roots = new ArrayList<>();
        
        for (SysPermission perm : permissions) {
            if (perm.getParentId() == null || perm.getParentId() == 0) {
                roots.add(perm);
            } else {
                SysPermission parent = map.get(perm.getParentId());
                if (parent != null) {
                    if (parent.getChildren() == null) {
                        parent.setChildren(new ArrayList<>());
                    }
                    parent.getChildren().add(perm);
                }
            }
        }
        
        return roots;
    }
}
