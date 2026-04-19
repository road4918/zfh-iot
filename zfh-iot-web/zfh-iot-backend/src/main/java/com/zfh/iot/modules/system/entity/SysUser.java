package com.zfh.iot.modules.system.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_user")
public class SysUser {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    @TableField(value = "tenant_id", insertStrategy = FieldStrategy.IGNORED)
    private Long tenantId;
    
    private String username;
    private String password;
    private String realName;
    private String phone;
    private String email;
    private String avatar;
    private Integer userType;
    private Integer status;
    
    @TableLogic
    private Integer deleted;
    
    private Long createdBy;
    
    private LocalDateTime lastLoginTime;
    private String lastLoginIp;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
