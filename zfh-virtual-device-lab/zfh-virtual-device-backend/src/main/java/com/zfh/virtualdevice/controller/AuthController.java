package com.zfh.virtualdevice.controller;

import com.zfh.virtualdevice.dto.Result;
import com.zfh.virtualdevice.security.JwtUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    
    @Autowired
    private JwtUtils jwtUtils;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    private final String adminUsername = "admin";
    private String adminPassword;
    
    @org.springframework.beans.factory.annotation.PostConstruct
    public void init() {
        this.adminPassword = passwordEncoder.encode("admin123");
    }
    
    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody Map<String, String> credentials) {
        String username = credentials.get("username");
        String password = credentials.get("password");
        
        if (!adminUsername.equals(username) || !passwordEncoder.matches(password, adminPassword)) {
            return Result.error(401, "用户名或密码错误");
        }
        
        String token = jwtUtils.generateToken(username);
        String refreshToken = jwtUtils.generateRefreshToken(username);
        
        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("refreshToken", refreshToken);
        result.put("expiresIn", 3600);
        
        return Result.success(result);
    }
    
    @PostMapping("/refresh")
    public Result<Map<String, Object>> refresh(@RequestBody Map<String, String> request) {
        String refreshToken = request.get("refreshToken");
        
        if (!jwtUtils.validateToken(refreshToken)) {
            return Result.error(401, "无效的刷新令牌");
        }
        
        String username = jwtUtils.getUsernameFromToken(refreshToken);
        String newToken = jwtUtils.generateToken(username);
        String newRefreshToken = jwtUtils.generateRefreshToken(username);
        
        Map<String, Object> result = new HashMap<>();
        result.put("token", newToken);
        result.put("refreshToken", newRefreshToken);
        result.put("expiresIn", 3600);
        
        return Result.success(result);
    }
    
    @GetMapping("/profile")
    public Result<Map<String, Object>> profile() {
        Map<String, Object> profile = new HashMap<>();
        profile.put("username", adminUsername);
        profile.put("roles", new String[]{"ADMIN"});
        return Result.success(profile);
    }
}
