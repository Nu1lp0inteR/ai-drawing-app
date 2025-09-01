package com.aidrawing.backend.service;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * 企业级JWT服务
 * Enterprise-level JWT Service
 * 
 * 功能特性：
 * - 安全的密钥管理和Token生成
 * - 完整的Token验证和解析
 * - 灵活的过期时间配置
 * - 详细的安全日志记录
 * - 支持Token刷新机制
 */
@Service
public class JwtService {

    private static final Logger logger = LoggerFactory.getLogger(JwtService.class);

    // JWT密钥配置 - 在生产环境中应从环境变量或配置中心获取
    @Value("${app.jwt.secret:JWT_SECRET_PLACEHOLDER}")
    private String jwtSecret;

    @Value("${app.jwt.access-token-expiration:3600000}") // 1小时 = 3600000ms
    private long accessTokenExpiration;

    @Value("${app.jwt.refresh-token-expiration:604800000}") // 7天 = 604800000ms 
    private long refreshTokenExpiration;

    /**
     * 生成访问Token
     * Generate access token
     */
    public String generateAccessToken(String userId, String username) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("type", "access");
        claims.put("username", username);
        
        return createToken(claims, userId, accessTokenExpiration);
    }

    /**
     * 生成刷新Token
     * Generate refresh token
     */
    public String generateRefreshToken(String userId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("type", "refresh");
        
        return createToken(claims, userId, refreshTokenExpiration);
    }

    /**
     * 创建Token的核心方法
     * Core method for token creation
     */
    private String createToken(Map<String, Object> claims, String subject, long expiration) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);

        try {
            String token = Jwts.builder()
                    .setClaims(claims)
                    .setSubject(subject)
                    .setIssuedAt(now)
                    .setExpiration(expiryDate)
                    .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                    .compact();

            logger.debug("Token created successfully for user: {} (expires: {})", subject, expiryDate);
            return token;

        } catch (Exception e) {
            logger.error("Error creating JWT token for user: {}", subject, e);
            throw new RuntimeException("Token creation failed", e);
        }
    }

    /**
     * 验证Token是否有效
     * Validate if token is valid
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token);
            return true;
        } catch (ExpiredJwtException e) {
            logger.warn("JWT token expired: {}", e.getMessage());
            return false;
        } catch (UnsupportedJwtException e) {
            logger.warn("Unsupported JWT token: {}", e.getMessage());
            return false;
        } catch (MalformedJwtException e) {
            logger.warn("Malformed JWT token: {}", e.getMessage());
            return false;
        } catch (IllegalArgumentException e) {
            logger.warn("JWT token compact of handler are invalid: {}", e.getMessage());
            return false;
        } catch (Exception e) {
            logger.error("JWT token validation error: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 从Token中提取用户ID
     * Extract user ID from token
     */
    public String getUserIdFromToken(String token) {
        return getClaimFromToken(token, Claims::getSubject);
    }

    /**
     * 从Token中提取用户名
     * Extract username from token
     */
    public String getUsernameFromToken(String token) {
        return getClaimFromToken(token, claims -> claims.get("username", String.class));
    }

    /**
     * 从Token中提取过期时间
     * Extract expiration date from token
     */
    public Date getExpirationDateFromToken(String token) {
        return getClaimFromToken(token, Claims::getExpiration);
    }

    /**
     * 检查Token是否已过期
     * Check if token is expired
     */
    public boolean isTokenExpired(String token) {
        final Date expiration = getExpirationDateFromToken(token);
        return expiration.before(new Date());
    }

    /**
     * 从Token中提取特定的声明
     * Extract specific claim from token
     */
    public <T> T getClaimFromToken(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = getAllClaimsFromToken(token);
        return claimsResolver.apply(claims);
    }

    /**
     * 从Token中提取所有声明
     * Extract all claims from token
     */
    private Claims getAllClaimsFromToken(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (Exception e) {
            logger.error("Error parsing JWT claims from token", e);
            throw new RuntimeException("Invalid JWT token", e);
        }
    }

    /**
     * 获取签名密钥
     * Get signing key
     */
    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes());
    }

    /**
     * 检查Token类型（access 或 refresh）
     * Check token type (access or refresh)
     */
    public String getTokenType(String token) {
        return getClaimFromToken(token, claims -> claims.get("type", String.class));
    }

    /**
     * 验证刷新Token
     * Validate refresh token
     */
    public boolean validateRefreshToken(String token) {
        if (!validateToken(token)) {
            return false;
        }
        
        String tokenType = getTokenType(token);
        return "refresh".equals(tokenType);
    }

    /**
     * 验证访问Token
     * Validate access token
     */
    public boolean validateAccessToken(String token) {
        if (!validateToken(token)) {
            return false;
        }
        
        String tokenType = getTokenType(token);
        return "access".equals(tokenType);
    }
}
