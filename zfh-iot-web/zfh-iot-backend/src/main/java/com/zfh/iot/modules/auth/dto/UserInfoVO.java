package com.zfh.iot.modules.auth.dto;

import lombok.Data;

import java.util.Set;

@Data
public class UserInfoVO {
    private Long id;
    private String username;
    private String realName;
    private String avatar;
    private String email;
    private String phone;
    private Integer status;
    private Integer userType;
    private Long tenantId;
    private String tenantName;
    private Set<String> permissions;
    private Set<String> roles;
}
