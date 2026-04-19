package com.zfh.iot.modules.system.controller;

import com.zfh.iot.common.result.Result;
import com.zfh.iot.modules.auth.utils.JwtUtils;
import com.zfh.iot.modules.system.entity.SysPermission;
import com.zfh.iot.modules.system.entity.SysUser;
import com.zfh.iot.modules.system.mapper.SysPermissionMapper;
import com.zfh.iot.modules.system.service.SysPermissionService;
import com.zfh.iot.modules.system.service.SysUserService;
import lombok.RequiredArgsConstructor;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/system/permissions")
@RequiredArgsConstructor
public class PermissionController {

    private final SysPermissionService permissionService;
    private final SysPermissionMapper permissionMapper;
    private final SysUserService userService;
    private final JwtUtils jwtUtils;

    @GetMapping("/tree")
    @RequiresPermissions("role:list")
    public Result<List<SysPermission>> getPermissionTree(HttpServletRequest request) {
        String token = request.getHeader("Authorization").substring(7);
        Long userId = jwtUtils.getUserIdFromToken(token);
        
        SysUser user = userService.getById(userId);
        List<SysPermission> permissions;
        
        if (user != null && "admin".equals(user.getUsername())) {
            permissions = permissionService.getPermissionTree();
        } else {
            List<SysPermission> userPermissions = permissionMapper.selectPermissionsByUserId(userId);
            permissions = buildTree(userPermissions);
        }
        
        return Result.success(permissions);
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
