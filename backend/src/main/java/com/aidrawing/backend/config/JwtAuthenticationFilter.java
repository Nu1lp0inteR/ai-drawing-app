package com.aidrawing.backend.config;

import com.aidrawing.backend.service.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;

/**
 * JWT认证过滤器
 * JWT Authentication Filter
 * 
 * 职责：
 * 1. 从HTTP请求头中提取JWT Token
 * 2. 验证Token的有效性
 * 3. 解析用户信息并设置到Spring Security上下文中
 * 4. 为后续的授权决策提供用户身份信息
 * 
 * 企业级特性：
 * - 完整的错误处理和日志记录
 * - Token格式验证
 * - 安全的异常处理
 * - 性能优化的过滤逻辑
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    
    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;
    private final RedisTemplate<String, String> redisTemplate;

    @Autowired
    public JwtAuthenticationFilter(JwtService jwtService, UserDetailsService userDetailsService,
                                   RedisTemplate<String, String> redisTemplate) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
        this.redisTemplate = redisTemplate;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        try {
            // 获取Authorization头
            final String authHeader = request.getHeader("Authorization");
            final String requestURI = request.getRequestURI();
            
            logger.debug("🔐 处理请求: {} {}", request.getMethod(), requestURI);
            
            // 检查是否包含Bearer Token
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                logger.debug("⚪ 无JWT Token，跳过认证: {}", requestURI);
                filterChain.doFilter(request, response);
                return;
            }

            // 提取JWT Token
            final String jwt = authHeader.substring(7); // 移除 "Bearer " 前缀
            logger.debug("🎫 提取到JWT Token: {}...", jwt.substring(0, Math.min(jwt.length(), 20)));

            // 验证Token并提取用户信息
            final String username = jwtService.extractUsername(jwt);
            final String userId = jwtService.extractUserId(jwt);
            
            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                logger.debug("👤 Token中的用户名: {}", username);
                
                // 验证Token有效性
                if (jwtService.isTokenValid(jwt)) {
                    if (isTokenBlacklisted(jwt)) {
                        logger.warn("⛔ JWT Token已被黑名单拦截: {}", username);
                    } else {
                        logger.info("✅ JWT Token验证成功: userId={}", userId);
                    
                    // 创建认证对象 - principal 为用户ID（UUID）
                    UsernamePasswordAuthenticationToken authToken = 
                        new UsernamePasswordAuthenticationToken(
                            userId,       // principal 使用 userId，下游代码依赖
                            null,
                            new ArrayList<>()
                        );
                    
                    // 设置请求详情
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    
                    // 将认证信息设置到Spring Security上下文中
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                    
                        logger.debug("🔓 用户认证成功，已设置到Security上下文: {}", username);
                    }
                } else {
                    logger.warn("❌ JWT Token验证失败: {}", username);
                }
            }
        } catch (Exception e) {
            logger.error("❌ JWT认证过滤器处理异常: {}", e.getMessage(), e);
            // 不抛出异常，让请求继续，由后续的授权检查处理
        }

        // 继续过滤链
        filterChain.doFilter(request, response);
    }

    /**
     * 确定是否应该跳过此过滤器
     * 对于公开端点，可以跳过JWT处理以提高性能
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        
        boolean shouldSkip = path.startsWith("/api/v1/auth/") ||
                           path.startsWith("/api/v1/quick/") ||
                           path.startsWith("/api/v1/hybrid/") ||
                           path.startsWith("/api/v1/images/") ||
                           path.startsWith("/websocket/") ||
                           path.equals("/error");
        
        if (shouldSkip) {
            logger.debug("⚪ 跳过JWT过滤器: {}", path);
        }
        
        return shouldSkip;
    }

    private boolean isTokenBlacklisted(String token) {
        try {
            String key = "jwt:blacklist:" + token;
            Boolean exists = redisTemplate.hasKey(key);
            return Boolean.TRUE.equals(exists);
        } catch (Exception e) {
            logger.warn("Redis 黑名单检查失败，放行 (fail-open): {}", e.getMessage());
            return false;
        }
    }
}
