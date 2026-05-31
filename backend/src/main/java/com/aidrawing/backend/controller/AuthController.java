package com.aidrawing.backend.controller;

import com.aidrawing.backend.dto.AuthDto;
import com.aidrawing.backend.service.AuthService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 企业级用户认证控制器
 * Enterprise-level Authentication Controller
 * 
 * 提供完整的用户认证API：
 * - 用户注册
 * - 用户登录
 * - Token刷新
 * - 用户登出
 * 
 * API设计原则：
 * - RESTful风格
 * - 统一响应格式
 * - 详细的错误处理
 * - 完整的请求验证
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    private final AuthService authService;

    @Autowired
    public AuthController(AuthService authService) {
        this.authService = authService;
        logger.info("AuthController initialized");
    }

    /**
     * 用户注册
     * User registration
     * 
     * POST /api/v1/auth/register
     * 
     * @param request 注册请求信息
     * @return 注册结果和JWT Token
     */
    @PostMapping("/register")
    public ResponseEntity<AuthDto.ApiResponse<AuthDto.AuthResponse>> register(
            @Valid @RequestBody AuthDto.RegisterRequest request) {
        
        String endpoint = "POST /api/v1/auth/register";
        logger.info("🎯 {} - 开始用户注册: username={}", endpoint, request.getUsername());

        try {
            // 调用认证服务进行注册
            AuthService.AuthResult result = authService.registerUser(
                request.getUsername(), 
                request.getEmail(), 
                request.getPassword()
            );

            if (result.isSuccess()) {
                // 构造成功响应
                AuthDto.UserInfo userInfo = new AuthDto.UserInfo(result.getUser());
                AuthDto.AuthResponse authResponse = new AuthDto.AuthResponse(
                    result.getAccessToken(),
                    result.getRefreshToken(),
                    userInfo
                );

                logger.info("✅ {} - 用户注册成功: userId={}", endpoint, result.getUser().getId());
                return ResponseEntity.ok(AuthDto.ApiResponse.success("注册成功", authResponse));
            } else {
                // 注册失败
                logger.warn("⚠️ {} - 用户注册失败: {}", endpoint, result.getErrorMessage());
                return ResponseEntity.badRequest()
                    .body(AuthDto.ApiResponse.error("注册失败", result.getErrorMessage()));
            }

        } catch (Exception e) {
            logger.error("❌ {} - 注册过程中发生异常", endpoint, e);
            return ResponseEntity.internalServerError()
                .body(AuthDto.ApiResponse.error("服务器内部错误，请稍后重试"));
        }
    }

    /**
     * 用户登录
     * User login
     * 
     * POST /api/v1/auth/login
     * 
     * @param request 登录请求信息
     * @return 登录结果和JWT Token
     */
    @PostMapping("/login")
    public ResponseEntity<AuthDto.ApiResponse<AuthDto.AuthResponse>> login(
            @Valid @RequestBody AuthDto.LoginRequest request) {
        
        String endpoint = "POST /api/v1/auth/login";
        logger.info("🔐 {} - 开始用户登录: identifier={}", endpoint, request.getUsernameOrEmail());

        try {
            // 调用认证服务进行登录
            AuthService.AuthResult result = authService.loginUser(
                request.getUsernameOrEmail(),
                request.getPassword()
            );

            if (result.isSuccess()) {
                // 构造成功响应
                AuthDto.UserInfo userInfo = new AuthDto.UserInfo(result.getUser());
                AuthDto.AuthResponse authResponse = new AuthDto.AuthResponse(
                    result.getAccessToken(),
                    result.getRefreshToken(),
                    userInfo
                );

                logger.info("✅ {} - 用户登录成功: userId={}", endpoint, result.getUser().getId());
                return ResponseEntity.ok(AuthDto.ApiResponse.success("登录成功", authResponse));
            } else {
                // 登录失败
                logger.warn("⚠️ {} - 用户登录失败: {}", endpoint, result.getErrorMessage());
                return ResponseEntity.badRequest()
                    .body(AuthDto.ApiResponse.error("登录失败", result.getErrorMessage()));
            }

        } catch (Exception e) {
            logger.error("❌ {} - 登录过程中发生异常", endpoint, e);
            return ResponseEntity.internalServerError()
                .body(AuthDto.ApiResponse.error("服务器内部错误，请稍后重试"));
        }
    }

    /**
     * 刷新访问Token
     * Refresh access token
     * 
     * POST /api/v1/auth/refresh
     * 
     * @param request 刷新Token请求
     * @return 新的访问Token
     */
    @PostMapping("/refresh")
    public ResponseEntity<AuthDto.ApiResponse<AuthDto.AuthResponse>> refreshToken(
            @Valid @RequestBody AuthDto.RefreshTokenRequest request) {
        
        String endpoint = "POST /api/v1/auth/refresh";
        logger.info("🔄 {} - 开始刷新Token", endpoint);

        try {
            // 调用认证服务刷新Token
            AuthService.AuthResult result = authService.refreshAccessToken(request.getRefreshToken());

            if (result.isSuccess()) {
                // 构造成功响应
                AuthDto.UserInfo userInfo = new AuthDto.UserInfo(result.getUser());
                AuthDto.AuthResponse authResponse = new AuthDto.AuthResponse(
                    result.getAccessToken(),
                    result.getRefreshToken(),
                    userInfo
                );

                logger.info("✅ {} - Token刷新成功: userId={}", endpoint, result.getUser().getId());
                return ResponseEntity.ok(AuthDto.ApiResponse.success("Token刷新成功", authResponse));
            } else {
                // 刷新失败
                logger.warn("⚠️ {} - Token刷新失败: {}", endpoint, result.getErrorMessage());
                return ResponseEntity.badRequest()
                    .body(AuthDto.ApiResponse.error("Token刷新失败", result.getErrorMessage()));
            }

        } catch (Exception e) {
            logger.error("❌ {} - Token刷新过程中发生异常", endpoint, e);
            return ResponseEntity.internalServerError()
                .body(AuthDto.ApiResponse.error("服务器内部错误，请稍后重试"));
        }
    }

    /**
     * 用户登出
     * User logout
     * 
     * POST /api/v1/auth/logout
     * 
     * 从 Authorization header 提取 token，将 access 和 refresh token 加入 Redis 黑名单。
     * 黑名单中的 token 在剩余有效期内无法再使用。
     */
    @PostMapping("/logout")
    public ResponseEntity<AuthDto.ApiResponse<String>> logout(
            @RequestParam(required = false) String refreshToken,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        String endpoint = "POST /api/v1/auth/logout";
        logger.info("🚪 {} - 开始用户登出", endpoint);

        try {
            String accessToken = null;
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                accessToken = authHeader.substring(7);
            }

            if (accessToken == null) {
                return ResponseEntity.badRequest()
                    .body(AuthDto.ApiResponse.error("未提供有效的访问令牌"));
            }

            authService.logoutUser(accessToken, refreshToken);

            return ResponseEntity.ok(AuthDto.ApiResponse.success("登出成功", "用户已安全登出"));

        } catch (Exception e) {
            logger.error("❌ {} - 登出过程中发生异常", endpoint, e);
            return ResponseEntity.internalServerError()
                .body(AuthDto.ApiResponse.error("服务器内部错误，请稍后重试"));
        }
    }

    /**
     * 健康检查端点
     * Health check endpoint
     * 
     * GET /api/v1/auth/health
     * 
     * @return 认证服务健康状态
     */
    @GetMapping("/health")
    public ResponseEntity<AuthDto.ApiResponse<String>> healthCheck() {
        try {
            return ResponseEntity.ok(AuthDto.ApiResponse.success(
                "认证服务运行正常", 
                "Authentication service is healthy"
            ));
        } catch (Exception e) {
            logger.error("认证服务健康检查失败", e);
            return ResponseEntity.status(503)
                .body(AuthDto.ApiResponse.error("认证服务异常"));
        }
    }
}
