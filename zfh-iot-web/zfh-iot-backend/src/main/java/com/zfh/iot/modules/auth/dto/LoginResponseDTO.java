package com.zfh.iot.modules.auth.dto;

import lombok.Data;

@Data
public class LoginResponseDTO {
    private String token;
    private String refreshToken;
    private Long expiresIn;
    private UserInfoVO user;
}
