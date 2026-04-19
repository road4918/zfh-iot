package com.zfh.iot.config;

import com.zfh.iot.modules.auth.shiro.JwtFilter;
import com.zfh.iot.modules.auth.shiro.ShiroRealm;
import javax.servlet.Filter;
import org.apache.shiro.mgt.SecurityManager;
import org.apache.shiro.spring.security.interceptor.AuthorizationAttributeSourceAdvisor;
import org.apache.shiro.spring.web.ShiroFilterFactoryBean;
import org.apache.shiro.spring.web.config.ShiroFilterChainDefinition;
import org.apache.shiro.web.mgt.DefaultWebSecurityManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

@Configuration
public class ShiroConfig {

    @Bean
    public ShiroRealm shiroRealm() {
        return new ShiroRealm();
    }

    @Bean
    public SecurityManager securityManager(ShiroRealm shiroRealm) {
        DefaultWebSecurityManager securityManager = new DefaultWebSecurityManager();
        securityManager.setRealm(shiroRealm);
        return securityManager;
    }

    @Bean
    public ShiroFilterChainDefinition shiroFilterChainDefinition() {
        return () -> {
            Map<String, String> filterChainDefinitionMap = new LinkedHashMap<>();
            filterChainDefinitionMap.put("/auth/login", "anon");
            filterChainDefinitionMap.put("/auth/refresh", "anon");
            filterChainDefinitionMap.put("/swagger-ui/**", "anon");
            filterChainDefinitionMap.put("/v3/api-docs/**", "anon");
            filterChainDefinitionMap.put("/**", "jwt");
            return filterChainDefinitionMap;
        };
    }

    @Bean
    public ShiroFilterFactoryBean shiroFilterFactoryBean(SecurityManager securityManager) {
        ShiroFilterFactoryBean filterFactoryBean = new ShiroFilterFactoryBean();
        filterFactoryBean.setSecurityManager(securityManager);
        
        Map<String, Filter> filters = new HashMap<>();
        filters.put("jwt", new JwtFilter());
        filterFactoryBean.setFilters(filters);
        
        Map<String, String> filterMap = new LinkedHashMap<>();
        filterMap.put("/auth/login", "anon");
        filterMap.put("/auth/refresh", "anon");
        filterMap.put("/swagger-ui/**", "anon");
        filterMap.put("/v3/api-docs/**", "anon");
        filterMap.put("/**", "jwt");
        
        filterFactoryBean.setFilterChainDefinitionMap(filterMap);
        return filterFactoryBean;
    }

    @Bean
    public AuthorizationAttributeSourceAdvisor authorizationAttributeSourceAdvisor(SecurityManager securityManager) {
        AuthorizationAttributeSourceAdvisor advisor = new AuthorizationAttributeSourceAdvisor();
        advisor.setSecurityManager(securityManager);
        return advisor;
    }
}
