package com.zfh.iot.modules.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zfh.iot.modules.system.entity.SysPermission;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Set;

@Mapper
public interface SysPermissionMapper extends BaseMapper<SysPermission> {
    
    @Select("SELECT * FROM sys_permission WHERE status = 1 ORDER BY sort_order")
    List<SysPermission> selectAllPermissions();
    
    @Select("SELECT perm_code FROM sys_permission WHERE status = 1")
    Set<String> selectAllPermCodes();
    
    @Select("SELECT DISTINCT p.* FROM sys_permission p " +
            "INNER JOIN sys_role_permission rp ON p.id = rp.perm_id " +
            "INNER JOIN sys_user_role ur ON rp.role_id = ur.role_id " +
            "WHERE ur.user_id = #{userId} AND p.status = 1 " +
            "ORDER BY p.sort_order")
    List<SysPermission> selectPermissionsByUserId(@Param("userId") Long userId);
    
    @Select("SELECT DISTINCT p.id FROM sys_permission p " +
            "INNER JOIN sys_role_permission rp ON p.id = rp.perm_id " +
            "INNER JOIN sys_user_role ur ON rp.role_id = ur.role_id " +
            "WHERE ur.user_id = #{userId} AND p.status = 1")
    Set<Long> selectPermissionIdsByUserId(@Param("userId") Long userId);
}
