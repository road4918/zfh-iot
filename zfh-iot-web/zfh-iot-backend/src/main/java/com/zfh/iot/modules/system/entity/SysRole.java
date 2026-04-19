package com.zfh.iot.modules.system.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_role")
public class SysRole {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    @TableField(value = "tenant_id", insertStrategy = FieldStrategy.IGNORED)
    private Long tenantId;
    
    private String roleCode;
    private String roleName;
    private String description;
    private Integer roleType;
    private Integer status;
    
    @TableLogic
    private Integer deleted;
    
    private Long createdBy;
    
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
