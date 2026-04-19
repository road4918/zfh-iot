## Chunk 7: Authentication & Security

### Task 13: JWT Authentication

**Files:**
- Create: `zfh-virtual-device-backend/src/main/java/com/zfh/virtualdevice/security/JwtUtils.java`
- Create: `zfh-virtual-device-backend/src/main/java/com/zfh/virtualdevice/security/JwtAuthenticationFilter.java`
- Create: `zfh-virtual-device-backend/src/main/java/com/zfh/virtualdevice/security/SecurityConfig.java`
- Create: `zfh-virtual-device-backend/src/main/java/com/zfh/virtualdevice/controller/AuthController.java`

- [ ] **Step 1: Create JWT utilities**

```java
package com.zfh.virtualdevice.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtUtils {
    
    @Value("${jwt.secret}")
    private String secret;
    
    @Value("${jwt.expiration}")
    private Long expiration;
    
    @Value("${jwt.refresh-expiration}")
    private Long refreshExpiration;
    
    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }
    
    public String generateToken(String username) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);
        
        return Jwts.builder()
                .setSubject(username)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }
    
    public String generateRefreshToken(String username) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + refreshExpiration);
        
        return Jwts.builder()
                .setSubject(username)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }
    
    public String getUsernameFromToken(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
        return claims.getSubject();
    }
    
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
```

- [ ] **Step 2: Create authentication filter**

```java
package com.zfh.virtualdevice.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    
    @Autowired
    private JwtUtils jwtUtils;
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                    HttpServletResponse response, 
                                    FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            if (jwtUtils.validateToken(token)) {
                String username = jwtUtils.getUsernameFromToken(token);
                UsernamePasswordAuthenticationToken auth = 
                    new UsernamePasswordAuthenticationToken(username, null, new ArrayList<>());
                SecurityContextHolder.getContext().setAuthentication(auth);
            }
        }
        
        filterChain.doFilter(request, response);
    }
}
```

- [ ] **Step 3: Create WebSocket JWT handshake interceptor**

```java
package com.zfh.virtualdevice.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

@Component
public class WebSocketJwtInterceptor implements HandshakeInterceptor {
    
    @Autowired
    private JwtUtils jwtUtils;
    
    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {
        if (request instanceof ServletServerHttpRequest) {
            ServletServerHttpRequest servletRequest = (ServletServerHttpRequest) request;
            String token = servletRequest.getServletRequest().getParameter("token");
            
            if (token != null && jwtUtils.validateToken(token)) {
                String username = jwtUtils.getUsernameFromToken(token);
                attributes.put("username", username);
                return true;
            }
        }
        return false; // Reject handshake if token is invalid
    }
    
    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
        // No action needed
    }
}
```

- [ ] **Step 4: Create security config**

```java
package com.zfh.virtualdevice.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {
    
    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;
    
    @Autowired
    private WebSocketJwtInterceptor webSocketJwtInterceptor;
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf().disable()
            .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            .and()
            .authorizeRequests()
            .antMatchers("/api/auth/**", "/h2-console/**").permitAll()
            .anyRequest().authenticated()
            .and()
            .headers().frameOptions().sameOrigin()
            .and()
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        
        return http.build();
    }
    
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
    
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
```

- [ ] **Step 5: Update WebSocket config with JWT interceptor**

Modify: `zfh-virtual-device-backend/src/main/java/com/zfh/virtualdevice/websocket/WebSocketConfig.java`

```java
package com.zfh.virtualdevice.websocket;

import com.zfh.virtualdevice.security.WebSocketJwtInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.*;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    
    @Autowired
    private WebSocketJwtInterceptor jwtInterceptor;
    
    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic");
        config.setApplicationDestinationPrefixes("/app");
    }
    
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws/virtual-device")
                .setAllowedOriginPatterns("*")
                .addInterceptors(jwtInterceptor)
                .withSockJS();
    }
}
```

- [ ] **Step 6: Create auth controller**

```java
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
    
    // In-memory user for MVP (should use database in production)
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
```

- [ ] **Step 7: Add JWT dependency to POM**

Modify: `zfh-virtual-device-backend/pom.xml`

Add to dependencies:
```xml
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-api</artifactId>
    <version>0.11.5</version>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-impl</artifactId>
    <version>0.11.5</version>
    <scope>runtime</scope>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-jackson</artifactId>
    <version>0.11.5</version>
    <scope>runtime</scope>
</dependency>
```

- [ ] **Step 8: Add JWT configuration to application.yml**

Modify: `zfh-virtual-device-backend/src/main/resources/application.yml`

Add at the end:
```yaml
jwt:
  secret: zfh-virtual-device-lab-secret-key-2026
  expiration: 3600000  # 1 hour
  refresh-expiration: 604800000  # 7 days
```

- [ ] **Step 9: Test authentication**

```bash
# Login
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'

# Test protected endpoint without token
curl http://localhost:8080/api/gateways
# Expected: 403 Forbidden

# Test with token
curl http://localhost:8080/api/gateways \
  -H "Authorization: Bearer {token_from_login}"
# Expected: 200 OK with data
```

- [ ] **Step 10: Commit**

```bash
git add zfh-virtual-device-backend/src/main/java/com/zfh/virtualdevice/security/
git add zfh-virtual-device-backend/src/main/java/com/zfh/virtualdevice/controller/AuthController.java
git add zfh-virtual-device-backend/pom.xml
git add zfh-virtual-device-backend/src/main/resources/application.yml
git commit -m "feat: add JWT authentication and security"
```

---

