package com.zfh.iot.modules.auth.dto;

import javax.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginDTO {
    @NotBlank(message = "Username cannot be empty")
    private String username;
    
    @NotBlank(message = "Password cannot be empty")
    private String password;
    
    private String captcha;
    private String captchaKey;
}
