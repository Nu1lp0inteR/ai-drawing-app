package com.aidrawing.backend.dto;

import com.aidrawing.backend.entity.User;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

/**
 * 认证相关的数据传输对象
 * Authentication related Data Transfer Objects
 * 
 * 包含注册、登录、用户信息等API的请求和响应对象
 * Contains request and response objects for registration, login, user info APIs
 */
public class AuthDto {

    /**
     * 用户注册请求
     * User registration request
     */
    public static class RegisterRequest {
        @NotBlank(message = "用户名不能为空")
        @Size(min = 3, max = 20, message = "用户名长度必须在3-20个字符之间")
        @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "用户名只能包含字母、数字和下划线")
        private String username;

        @NotBlank(message = "邮箱不能为空")
        @Email(message = "邮箱格式不正确")
        private String email;

        @NotBlank(message = "密码不能为空")
        @Size(min = 8, message = "密码长度至少8位")
        @Pattern(regexp = "^(?=.*[a-zA-Z])(?=.*\\d)[a-zA-Z\\d@$!%*?&]+$", 
                message = "密码必须包含至少一个字母和一个数字")
        private String password;

        // Constructors
        public RegisterRequest() {}

        public RegisterRequest(String username, String email, String password) {
            this.username = username;
            this.email = email;
            this.password = password;
        }

        // Getters and Setters
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }

        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }

    /**
     * 用户登录请求
     * User login request
     */
    public static class LoginRequest {
        @NotBlank(message = "用户名或邮箱不能为空")
        @JsonProperty("usernameOrEmail") // 明确指定JSON字段名
        private String usernameOrEmail;

        @NotBlank(message = "密码不能为空")
        private String password;

        // Constructors
        public LoginRequest() {}

        public LoginRequest(String usernameOrEmail, String password) {
            this.usernameOrEmail = usernameOrEmail;
            this.password = password;
        }

        // Getters and Setters
        public String getUsernameOrEmail() { return usernameOrEmail; }
        public void setUsernameOrEmail(String usernameOrEmail) { this.usernameOrEmail = usernameOrEmail; }

        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }

    /**
     * Token刷新请求
     * Token refresh request
     */
    public static class RefreshTokenRequest {
        @NotBlank(message = "刷新Token不能为空")
        @JsonProperty("refreshToken") // 明确指定JSON字段名
        private String refreshToken;

        // Constructors
        public RefreshTokenRequest() {}

        public RefreshTokenRequest(String refreshToken) {
            this.refreshToken = refreshToken;
        }

        // Getters and Setters
        public String getRefreshToken() { return refreshToken; }
        public void setRefreshToken(String refreshToken) { this.refreshToken = refreshToken; }
    }

    /**
     * 认证响应
     * Authentication response
     */
    public static class AuthResponse {
        @JsonProperty("accessToken") // 明确指定JSON字段名
        private String accessToken;
        
        @JsonProperty("refreshToken") // 明确指定JSON字段名
        private String refreshToken;
        
        @JsonProperty("tokenType") // 明确指定JSON字段名
        private String tokenType = "Bearer";
        
        private UserInfo user;

        // Constructors
        public AuthResponse() {}

        public AuthResponse(String accessToken, String refreshToken, UserInfo user) {
            this.accessToken = accessToken;
            this.refreshToken = refreshToken;
            this.user = user;
        }

        // Getters and Setters
        public String getAccessToken() { return accessToken; }
        public void setAccessToken(String accessToken) { this.accessToken = accessToken; }

        public String getRefreshToken() { return refreshToken; }
        public void setRefreshToken(String refreshToken) { this.refreshToken = refreshToken; }

        public String getTokenType() { return tokenType; }
        public void setTokenType(String tokenType) { this.tokenType = tokenType; }

        public UserInfo getUser() { return user; }
        public void setUser(UserInfo user) { this.user = user; }
    }

    /**
     * 用户信息
     * User information
     */
    public static class UserInfo {
        private String id;
        private String username;
        private String email;
        
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
        @JsonProperty("createdAt") // 明确指定JSON字段名
        private LocalDateTime createdAt;

        // Constructors
        public UserInfo() {}

        public UserInfo(User user) {
            this.id = user.getId();
            this.username = user.getUsername();
            this.email = user.getEmail();
            this.createdAt = user.getCreatedAt();
        }

        public UserInfo(String id, String username, String email, LocalDateTime createdAt) {
            this.id = id;
            this.username = username;
            this.email = email;
            this.createdAt = createdAt;
        }

        // Getters and Setters
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }

        public LocalDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    }

    /**
     * API通用响应
     * Common API response
     */
    public static class ApiResponse<T> {
        private boolean success;
        private String message;
        private T data;
        private String error;

        // Constructors
        public ApiResponse() {}

        private ApiResponse(boolean success, String message, T data, String error) {
            this.success = success;
            this.message = message;
            this.data = data;
            this.error = error;
        }

        public static <T> ApiResponse<T> success(T data) {
            return new ApiResponse<>(true, "操作成功", data, null);
        }

        public static <T> ApiResponse<T> success(String message, T data) {
            return new ApiResponse<>(true, message, data, null);
        }

        public static <T> ApiResponse<T> error(String error) {
            return new ApiResponse<>(false, null, null, error);
        }

        public static <T> ApiResponse<T> error(String message, String error) {
            return new ApiResponse<>(false, message, null, error);
        }

        // Getters and Setters
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public T getData() { return data; }
        public void setData(T data) { this.data = data; }

        public String getError() { return error; }
        public void setError(String error) { this.error = error; }
    }
}
