package com.zfh.iot.modules.system.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class UserDTO {
    private Long id;
    
    @NotNull(message = "用户类型不能为空")
    private Integer userType;
    
    private Long tenantId;
    
    @NotBlank(message = "用户名不能为空")
    private String username;
    
    private String password;
    
    private String realName;
    private String phone;
    private String email;
    private String avatar;
    private Integer status;
    
    @NotNull(message = "角色不能为空")
    private List<Long> roleIds;
}
