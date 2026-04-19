package com.zfh.iot.modules.system.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RoleDTO {
    private Long id;
    
    @NotNull(message = "角色类型不能为空")
    private Integer roleType;
    
    private Long tenantId;
    
    @NotBlank(message = "角色编码不能为空")
    private String roleCode;
    
    @NotBlank(message = "角色名称不能为空")
    private String roleName;
    
    private String description;
    private Integer status;
}
