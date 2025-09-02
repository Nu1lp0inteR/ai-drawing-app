package com.aidrawing.backend.config;

/**
 * CORS 配置常量
 * 统一管理允许的来源，避免配置不一致
 */
public class CorsConstants {
    
    /**
     * 开发环境允许的来源列表 - 支持多个端口以解决端口冲突
     */
    public static final String[] ALLOWED_ORIGINS = {
        "http://localhost:5173",      // 默认Vite端口
        "http://localhost:5174",      // 第二选择端口
        "http://localhost:5175",      // 第三选择端口
        "http://127.0.0.1:5173",      // 本地回环地址
        "http://127.0.0.1:5174",
        "http://127.0.0.1:5175",
        "http://172.31.200.59:5173"   // WSL2 IP（如果适用）
    };
    
    /**
     * 生产环境允许的来源（示例）
     */
    public static final String[] PRODUCTION_ORIGINS = {
        "https://yourdomain.com",
        "https://www.yourdomain.com"
    };
}
