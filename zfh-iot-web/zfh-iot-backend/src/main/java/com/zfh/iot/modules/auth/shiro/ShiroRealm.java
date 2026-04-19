package com.zfh.iot.modules.auth.shiro;

import com.zfh.iot.modules.auth.utils.JwtUtils;
import com.zfh.iot.modules.system.entity.SysUser;
import com.zfh.iot.modules.system.service.SysUserService;
import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.SimpleAuthenticationInfo;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.authz.SimpleAuthorizationInfo;
import org.apache.shiro.realm.AuthorizingRealm;
import org.apache.shiro.subject.PrincipalCollection;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Set;

@Slf4j
public class ShiroRealm extends AuthorizingRealm {

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private SysUserService sysUserService;

    @Override
    public boolean supports(AuthenticationToken token) {
        return token instanceof JwtToken;
    }

    @Override
    protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken token) throws AuthenticationException {
        String jwt = (String) token.getPrincipal();
        
        log.debug("Authenticating JWT token: {}...", jwt.substring(0, Math.min(20, jwt.length())));
        
        try {
            Claims claims = jwtUtils.parseToken(jwt);
            Long userId = Long.parseLong(claims.getSubject());
            
            log.debug("Token parsed successfully, userId: {}", userId);
            
            SysUser user = sysUserService.getById(userId);
            if (user == null) {
                log.error("User not found for userId: {}", userId);
                throw new AuthenticationException("User not found or disabled");
            }
            
            if (user.getStatus() == 0) {
                log.error("User is disabled: {}", userId);
                throw new AuthenticationException("User not found or disabled");
            }
            
            log.debug("Authentication successful for user: {}", user.getUsername());
            return new SimpleAuthenticationInfo(jwt, jwt, getName());
        } catch (Exception e) {
            log.error("Authentication error: {}", e.getMessage());
            throw new AuthenticationException(e.getMessage());
        }
    }

    @Override
    protected AuthorizationInfo doGetAuthorizationInfo(PrincipalCollection principals) {
        String jwt = (String) principals.getPrimaryPrincipal();
        Long userId = jwtUtils.getUserIdFromToken(jwt);
        
        SimpleAuthorizationInfo info = new SimpleAuthorizationInfo();
        
        SysUser user = sysUserService.getById(userId);
        if (user != null && "admin".equals(user.getUsername())) {
            info.addRole("admin");
            Set<String> permissions = sysUserService.getUserPermissions(userId);
            info.setStringPermissions(permissions);
        } else {
            Set<String> roles = sysUserService.getUserRoles(userId);
            info.setRoles(roles);
            Set<String> permissions = sysUserService.getUserPermissions(userId);
            info.setStringPermissions(permissions);
        }
        
        return info;
    }
}
