package com.aidrawing.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Global Cross-Origin Resource Sharing (CORS) configuration for the application.
 * This provides a centralized way to manage CORS settings.
 * * 应用的全局跨域资源共享（CORS）配置。
 * 这提供了一种集中管理CORS设置的方式。
 */
@Configuration
public class WebConfig {

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(@NonNull CorsRegistry registry) {
                // The @NonNull annotation has been added to address the warning.
                // 已添加 @NonNull 注解以解决警告。
                registry.addMapping("/api/**") // Apply CORS to all paths under /api/
                        // **核心修复**：同时允许 localhost 和 127.0.0.1 这两个源
                        .allowedOrigins(CorsConstants.ALLOWED_ORIGINS) // 统一的来源配置
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS") // Allow all standard HTTP methods
                        .allowedHeaders("*") // Allow all headers
                        .allowCredentials(true); // Allow credentials (e.g., cookies)
            }
        };
    }
}