package com.aidrawing.backend.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * 企业级Spring Security配置
 * Enterprise-level Spring Security Configuration
 * 
 * 安全策略：
 * - JWT无状态认证
 * - CORS跨域配置
 * - 公开端点和受保护端点配置
 * - 密码加密策略
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Autowired
    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    /**
     * 密码编码器配置
     * Password encoder configuration
     * 
     * 使用BCrypt算法，成本因子12，提供强密码保护
     * Uses BCrypt algorithm with strength 12 for strong password protection
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    /**
     * 主要安全配置链
     * Main security filter chain
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // 禁用CSRF，因为使用JWT无状态认证
            // Disable CSRF as we're using stateless JWT authentication
            .csrf(csrf -> csrf.disable())
            
            // 配置CORS
            // Configure CORS
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            
            // 会话管理：无状态
            // Session management: stateless
            .sessionManagement(session -> 
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            
            // HTTP请求授权配置
            // HTTP request authorization configuration
            .authorizeHttpRequests(authz -> authz
                // 公开端点 - 无需认证
                // Public endpoints - no authentication required
                .requestMatchers(
                    "/api/v1/auth/**",           // 认证相关API
                    "/api/v1/quick/**",          // 快速测试API
                    "/api/v1/hybrid/**",         // 混合画廊API (暂时公开)
                    "/api/v1/enterprise/gallery/public", // 企业级公共画廊
                    "/api/v1/enterprise/gallery/health", // 健康检查
                    "/api/v1/users/*/profile",   // 用户公开个人主页
                    "/api/v1/users/*/artworks",  // 用户公开作品列表
                    "/api/v1/follows/users/*/following", // 公开查看用户关注列表
                    "/api/v1/follows/users/*/followers", // 公开查看用户粉丝列表
                    "/api/v1/follows/*/status",          // 公开查看关注状态
                    "/api/v1/images/**",         // 图片访问
                    "/ws/**",                    // WebSocket连接 - 修正路径
                    "/error"                     // 错误页面
                ).permitAll()
                
                // 受保护端点 - 需要认证
                // Protected endpoints - authentication required
                .requestMatchers(
                    "/api/v1/ai-drawing/generate",           // AI图片生成
                    "/api/v1/ai-drawing/share",              // 分享到画廊 (multipart)
                    "/api/v1/ai-drawing/*/share",            // 分享到画廊 (by ID)
                    "/api/v1/ai-drawing/*/unshare",          // 取消分享
                    "/api/v1/drawings/*",                    // 作品详情/删除 (需认证)
                    "/api/v1/user/**",                       // 用户相关API
                    "/api/v1/profile/**",                    // 用户档案API
                    "/api/v1/follows/current-user/**",      // 当前用户的关注/粉丝列表
                    "/api/v1/follows/*",                     // 关注/取消关注操作
                    "/api/v1/likes/**"                       // 点赞相关API
                ).authenticated()
                
                // 其他所有请求默认需要认证
                // All other requests require authentication by default
                .anyRequest().authenticated()
            )
            
            // 禁用默认登录页面
            // Disable default login page
            .formLogin(form -> form.disable())
            
            // 禁用HTTP Basic认证
            // Disable HTTP Basic authentication
            .httpBasic(basic -> basic.disable())
            
            // 添加JWT认证过滤器
            // Add JWT authentication filter
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * CORS配置源
     * CORS configuration source
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // 允许的源
        // Allowed origins
        configuration.setAllowedOriginPatterns(List.of(
            "http://localhost:*",     // 本地开发
            "http://127.0.0.1:*",     // 本地开发
            "https://*.yourdomain.com" // 生产域名 (需要替换为实际域名)
        ));
        
        // 允许的HTTP方法
        // Allowed HTTP methods
        configuration.setAllowedMethods(Arrays.asList(
            "GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"
        ));
        
        // 允许的请求头
        // Allowed headers
        configuration.setAllowedHeaders(Arrays.asList(
            "Authorization",
            "Content-Type",
            "X-Requested-With",
            "Accept",
            "Origin",
            "Access-Control-Request-Method",
            "Access-Control-Request-Headers"
        ));
        
        // 暴露的响应头
        // Exposed response headers
        configuration.setExposedHeaders(Arrays.asList(
            "Access-Control-Allow-Origin",
            "Access-Control-Allow-Credentials",
            "Authorization"
        ));
        
        // 允许携带凭证
        // Allow credentials
        configuration.setAllowCredentials(true);
        
        // 预检请求缓存时间（秒）
        // Pre-flight request cache time (seconds)
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        
        return source;
    }
}
