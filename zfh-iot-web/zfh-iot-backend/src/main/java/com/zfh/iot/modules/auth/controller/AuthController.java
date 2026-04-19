package com.zfh.iot.modules.auth.controller;

import com.zfh.iot.common.exception.BusinessException;
import com.zfh.iot.common.result.Result;
import com.zfh.iot.modules.auth.dto.LoginDTO;
import com.zfh.iot.modules.auth.dto.LoginResponseDTO;
import com.zfh.iot.modules.auth.utils.JwtUtils;
import com.zfh.iot.modules.auth.dto.UserInfoVO;
import com.zfh.iot.modules.system.entity.SysUser;
import com.zfh.iot.modules.system.service.SysUserService;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.apache.shiro.crypto.hash.SimpleHash;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final SysUserService sysUserService;
    private final JwtUtils jwtUtils;

    @PostMapping("/login")
    public Result<LoginResponseDTO> login(@Valid @RequestBody LoginDTO loginDTO) {
        SysUser user = sysUserService.getByUsername(loginDTO.getUsername());
        if (user == null) {
            throw new BusinessException("Username or password incorrect");
        }

        String encryptedPassword = new SimpleHash("SHA-256", loginDTO.getPassword(), null, 1).toHex();
        if (!encryptedPassword.equals(user.getPassword())) {
            throw new BusinessException("Username or password incorrect");
        }

        if (user.getStatus() == 0) {
            throw new BusinessException("Account is disabled");
        }

        String token = jwtUtils.generateToken(user.getId(), user.getTenantId(), user.getUserType(), user.getUsername());
        String refreshToken = jwtUtils.generateRefreshToken(user.getId());

        LoginResponseDTO response = new LoginResponseDTO();
        response.setToken(token);
        response.setRefreshToken(refreshToken);
        response.setExpiresIn(7200L);
        
        UserInfoVO userInfo = new UserInfoVO();
        BeanUtils.copyProperties(user, userInfo);
        response.setUser(userInfo);

        return Result.success(response);
    }

    @PostMapping("/refresh")
    public Result<LoginResponseDTO> refresh(@RequestParam String refreshToken) {
        if (!jwtUtils.validateToken(refreshToken)) {
            throw new BusinessException("Refresh token invalid or expired");
        }

        Long userId = jwtUtils.getUserIdFromToken(refreshToken);
        SysUser user = sysUserService.getById(userId);
        
        if (user == null || user.getStatus() == 0) {
            throw new BusinessException("User not found or disabled");
        }

        String newToken = jwtUtils.generateToken(user.getId(), user.getTenantId(), user.getUserType(), user.getUsername());
        String newRefreshToken = jwtUtils.generateRefreshToken(user.getId());

        LoginResponseDTO response = new LoginResponseDTO();
        response.setToken(newToken);
        response.setRefreshToken(newRefreshToken);
        response.setExpiresIn(7200L);
        
        UserInfoVO userInfo = new UserInfoVO();
        BeanUtils.copyProperties(user, userInfo);
        response.setUser(userInfo);

        return Result.success(response);
    }

    @GetMapping("/info")
    public Result<UserInfoVO> getUserInfo(@RequestHeader("Authorization") String authorization) {
        String token = authorization.substring(7);
        Long userId = jwtUtils.getUserIdFromToken(token);
        
        UserInfoVO userInfo = sysUserService.getUserInfoWithPermissions(userId);
        
        return Result.success(userInfo);
    }
}
