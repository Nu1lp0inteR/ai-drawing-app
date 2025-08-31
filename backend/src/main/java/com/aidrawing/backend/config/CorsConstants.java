package com.aidrawing.backend.config;

/**
 * CORS 配置常量
 * 统一管理允许的来源，避免配置不一致
 */
public class CorsConstants {
    
    /**
     * 开发环境允许的来源列表
     */
    public static final String[] ALLOWED_ORIGINS = {
        "http://localhost:5173",      // WSL2 内部端口
        "http://localhost:5174",      // Windows 转发端口  
        "http://127.0.0.1:5173",
        "http://127.0.0.1:5174",
        "http://172.31.200.59:5173"   // WSL2 IP
    };
    
    /**
     * 生产环境允许的来源（示例）
     */
    public static final String[] PRODUCTION_ORIGINS = {
        "https://yourdomain.com",
        "https://www.yourdomain.com"
    };
}
