package com.zfh.iot.modules.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@TableName("sys_permission")
public class SysPermission {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long parentId;
    private String permCode;
    private String permName;
    private Integer permType;
    private String path;
    private String component;
    private String icon;
    private Integer sortOrder;
    private Integer status;
    private LocalDateTime createTime;
    
    @TableField(exist = false)
    private List<SysPermission> children;
}
