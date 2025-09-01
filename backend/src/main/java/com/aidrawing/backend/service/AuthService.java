package com.aidrawing.backend.service;

import com.aidrawing.backend.entity.User;
import com.aidrawing.backend.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * 企业级用户认证服务
 * Enterprise-level Authentication Service
 * 
 * 核心功能：
 * - 用户注册与登录验证
 * - 密码安全管理
 * - 刷新Token管理
 * - 用户状态管理
 * - 详细的安全审计
 */
@Service
@Transactional
public class AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);

    // 邮箱格式验证正则表达式
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$"
    );

    // 用户名格式验证正则表达式 (3-20字符，字母数字下划线)
    private static final Pattern USERNAME_PATTERN = Pattern.compile(
        "^[a-zA-Z0-9_]{3,20}$"
    );

    // 密码强度验证正则表达式 (至少8位，包含字母和数字)
    private static final Pattern PASSWORD_PATTERN = Pattern.compile(
        "^(?=.*[a-zA-Z])(?=.*\\d)[a-zA-Z\\d@$!%*?&]{8,}$"
    );

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RedisTemplate<String, String> redisTemplate;

    @Autowired
    public AuthService(UserRepository userRepository,
                      PasswordEncoder passwordEncoder,
                      JwtService jwtService,
                      RedisTemplate<String, String> redisTemplate) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.redisTemplate = redisTemplate;
    }

    /**
     * 用户注册
     * User registration
     */
    public AuthResult registerUser(String username, String email, String password) {
        logger.info("🎯 开始用户注册流程: username={}, email={}", username, email);

        try {
            // 1. 输入验证
            ValidationResult validation = validateRegistrationInput(username, email, password);
            if (!validation.isValid()) {
                logger.warn("⚠️ 注册输入验证失败: {}", validation.getErrorMessage());
                return AuthResult.failure(validation.getErrorMessage());
            }

            // 2. 检查用户是否已存在
            if (userRepository.existsByUsername(username)) {
                logger.warn("⚠️ 用户名已存在: {}", username);
                return AuthResult.failure("用户名已存在");
            }

            if (userRepository.existsByEmail(email)) {
                logger.warn("⚠️ 邮箱已被注册: {}", email);
                return AuthResult.failure("邮箱已被注册");
            }

            // 3. 创建用户
            String hashedPassword = passwordEncoder.encode(password);
            User newUser = new User(username, email, hashedPassword);
            User savedUser = userRepository.save(newUser);

            logger.info("✅ 用户注册成功: id={}, username={}", savedUser.getId(), savedUser.getUsername());

            // 4. 生成JWT Token
            String accessToken = jwtService.generateAccessToken(savedUser.getId(), savedUser.getUsername());
            String refreshToken = jwtService.generateRefreshToken(savedUser.getId());

            // 5. 存储刷新Token到Redis
            storeRefreshToken(savedUser.getId(), refreshToken);

            return AuthResult.success(accessToken, refreshToken, savedUser);

        } catch (Exception e) {
            logger.error("❌ 用户注册失败: username={}, email={}", username, email, e);
            return AuthResult.failure("注册失败，请稍后重试");
        }
    }

    /**
     * 用户登录
     * User login
     */
    public AuthResult loginUser(String usernameOrEmail, String password) {
        logger.info("🔐 开始用户登录流程: identifier={}", usernameOrEmail);

        try {
            // 1. 查找用户
            Optional<User> userOptional = userRepository.findByUsernameOrEmail(usernameOrEmail, usernameOrEmail);
            if (userOptional.isEmpty()) {
                logger.warn("⚠️ 用户不存在: {}", usernameOrEmail);
                return AuthResult.failure("用户名或密码错误");
            }

            User user = userOptional.get();

            // 2. 验证密码
            if (!passwordEncoder.matches(password, user.getPassword())) {
                logger.warn("⚠️ 密码验证失败: userId={}", user.getId());
                return AuthResult.failure("用户名或密码错误");
            }

            logger.info("✅ 用户登录成功: id={}, username={}", user.getId(), user.getUsername());

            // 3. 生成JWT Token
            String accessToken = jwtService.generateAccessToken(user.getId(), user.getUsername());
            String refreshToken = jwtService.generateRefreshToken(user.getId());

            // 4. 存储刷新Token到Redis
            storeRefreshToken(user.getId(), refreshToken);

            return AuthResult.success(accessToken, refreshToken, user);

        } catch (Exception e) {
            logger.error("❌ 用户登录失败: identifier={}", usernameOrEmail, e);
            return AuthResult.failure("登录失败，请稍后重试");
        }
    }

    /**
     * 刷新访问Token
     * Refresh access token
     */
    public AuthResult refreshAccessToken(String refreshToken) {
        logger.info("🔄 开始刷新Token流程");

        try {
            // 1. 验证刷新Token
            if (!jwtService.validateRefreshToken(refreshToken)) {
                logger.warn("⚠️ 无效的刷新Token");
                return AuthResult.failure("刷新Token无效");
            }

            // 2. 从Token中提取用户ID
            String userId = jwtService.getUserIdFromToken(refreshToken);

            // 3. 验证Redis中的刷新Token
            String storedRefreshToken = getStoredRefreshToken(userId);
            if (!refreshToken.equals(storedRefreshToken)) {
                logger.warn("⚠️ 刷新Token不匹配: userId={}", userId);
                return AuthResult.failure("刷新Token无效");
            }

            // 4. 查找用户
            Optional<User> userOptional = userRepository.findById(userId);
            if (userOptional.isEmpty()) {
                logger.warn("⚠️ 用户不存在: userId={}", userId);
                return AuthResult.failure("用户不存在");
            }

            User user = userOptional.get();

            // 5. 生成新的访问Token
            String newAccessToken = jwtService.generateAccessToken(user.getId(), user.getUsername());

            logger.info("✅ Token刷新成功: userId={}", userId);
            return AuthResult.success(newAccessToken, refreshToken, user);

        } catch (Exception e) {
            logger.error("❌ Token刷新失败", e);
            return AuthResult.failure("Token刷新失败");
        }
    }

    /**
     * 用户登出
     * User logout
     */
    public void logoutUser(String userId) {
        logger.info("🚪 用户登出: userId={}", userId);
        try {
            // 删除Redis中的刷新Token
            removeRefreshToken(userId);
            logger.info("✅ 用户登出成功: userId={}", userId);
        } catch (Exception e) {
            logger.error("❌ 用户登出失败: userId={}", userId, e);
        }
    }

    /**
     * 验证注册输入
     * Validate registration input
     */
    private ValidationResult validateRegistrationInput(String username, String email, String password) {
        if (username == null || username.trim().isEmpty()) {
            return ValidationResult.invalid("用户名不能为空");
        }

        if (!USERNAME_PATTERN.matcher(username).matches()) {
            return ValidationResult.invalid("用户名格式不正确（3-20字符，仅支持字母数字下划线）");
        }

        if (email == null || email.trim().isEmpty()) {
            return ValidationResult.invalid("邮箱不能为空");
        }

        if (!EMAIL_PATTERN.matcher(email).matches()) {
            return ValidationResult.invalid("邮箱格式不正确");
        }

        if (password == null || password.trim().isEmpty()) {
            return ValidationResult.invalid("密码不能为空");
        }

        if (!PASSWORD_PATTERN.matcher(password).matches()) {
            return ValidationResult.invalid("密码强度不足（至少8位，包含字母和数字）");
        }

        return ValidationResult.valid();
    }

    /**
     * 存储刷新Token到Redis
     * Store refresh token to Redis
     */
    private void storeRefreshToken(String userId, String refreshToken) {
        String key = "refresh_token:" + userId;
        redisTemplate.opsForValue().set(key, refreshToken, Duration.ofDays(7));
        logger.debug("刷新Token已存储到Redis: userId={}", userId);
    }

    /**
     * 从Redis获取刷新Token
     * Get refresh token from Redis
     */
    private String getStoredRefreshToken(String userId) {
        String key = "refresh_token:" + userId;
        return redisTemplate.opsForValue().get(key);
    }

    /**
     * 从Redis删除刷新Token
     * Remove refresh token from Redis
     */
    private void removeRefreshToken(String userId) {
        String key = "refresh_token:" + userId;
        redisTemplate.delete(key);
        logger.debug("刷新Token已从Redis删除: userId={}", userId);
    }

    /**
     * 验证结果内部类
     * Validation result inner class
     */
    private static class ValidationResult {
        private final boolean valid;
        private final String errorMessage;

        private ValidationResult(boolean valid, String errorMessage) {
            this.valid = valid;
            this.errorMessage = errorMessage;
        }

        public static ValidationResult valid() {
            return new ValidationResult(true, null);
        }

        public static ValidationResult invalid(String errorMessage) {
            return new ValidationResult(false, errorMessage);
        }

        public boolean isValid() {
            return valid;
        }

        public String getErrorMessage() {
            return errorMessage;
        }
    }

    /**
     * 认证结果类
     * Authentication result class
     */
    public static class AuthResult {
        private final boolean success;
        private final String accessToken;
        private final String refreshToken;
        private final User user;
        private final String errorMessage;

        private AuthResult(boolean success, String accessToken, String refreshToken, User user, String errorMessage) {
            this.success = success;
            this.accessToken = accessToken;
            this.refreshToken = refreshToken;
            this.user = user;
            this.errorMessage = errorMessage;
        }

        public static AuthResult success(String accessToken, String refreshToken, User user) {
            return new AuthResult(true, accessToken, refreshToken, user, null);
        }

        public static AuthResult failure(String errorMessage) {
            return new AuthResult(false, null, null, null, errorMessage);
        }

        // Getters
        public boolean isSuccess() { return success; }
        public String getAccessToken() { return accessToken; }
        public String getRefreshToken() { return refreshToken; }
        public User getUser() { return user; }
        public String getErrorMessage() { return errorMessage; }
    }
}
