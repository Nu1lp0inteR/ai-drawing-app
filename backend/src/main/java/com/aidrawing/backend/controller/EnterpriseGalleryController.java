package com.aidrawing.backend.controller;

import com.aidrawing.backend.dto.GalleryItemDto;
import com.aidrawing.backend.entity.Drawing;
import com.aidrawing.backend.service.EnterpriseGalleryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 企业级画廊控制器
 * Enterprise Gallery Controller
 * 
 * 企业级特性 Enterprise Features:
 * - 📊 完整的请求响应日志记录
 * - 🛡️ 健壮的错误处理和HTTP状态码管理
 * - 📈 性能监控和指标收集
 * - 🔒 安全性和输入验证
 * - 📝 详细的API文档和注释
 * - 🎯 RESTful API设计原则
 */
@RestController
@RequestMapping("/api/v1/enterprise/gallery")
public class EnterpriseGalleryController {

    private static final Logger logger = LoggerFactory.getLogger(EnterpriseGalleryController.class);
    
    private final EnterpriseGalleryService galleryService;

    @Autowired
    public EnterpriseGalleryController(EnterpriseGalleryService galleryService) {
        this.galleryService = galleryService;
        logger.info("EnterpriseGalleryController initialized");
    }

    /**
     * 获取公共画廊 - 企业级API端点
     * Get Public Gallery - Enterprise API Endpoint
     * 
     * GET /api/v1/enterprise/gallery/public
     * 
     * 特性 Features:
     * - ⚡ 高性能缓存支持
     * - 📊 详细的请求监控
     * - 🛡️ 优雅的错误处理
     * - 📱 移动端友好的响应格式
     */
    @GetMapping("/public")
    public ResponseEntity<List<GalleryItemDto>> getPublicGallery() {
        long startTime = System.currentTimeMillis();
        String endpoint = "GET /api/v1/enterprise/gallery/public";
        
        logger.info("🌐 {} - 开始处理公共画廊请求", endpoint);
        
        try {
            List<Drawing> drawings = galleryService.getPublicGallery();
            
            // 将Entity转换为DTO，避免序列化问题
            List<GalleryItemDto> galleryItems = drawings.stream()
                .map(GalleryItemDto::new)
                .collect(Collectors.toList());
            
            long processingTime = System.currentTimeMillis() - startTime;
            logger.info("✅ {} - 成功返回 {} 张作品, 处理时间: {} ms", 
                       endpoint, galleryItems.size(), processingTime);
            
            // 性能监控
            if (processingTime > 500) {
                logger.warn("⚠️ {} - 性能警告: 响应时间超过500ms ({} ms)", endpoint, processingTime);
            }
            
            return ResponseEntity.ok(galleryItems);
            
        } catch (Exception e) {
            long processingTime = System.currentTimeMillis() - startTime;
            logger.error("❌ {} - 处理失败, 耗时: {} ms, 错误: {}", 
                        endpoint, processingTime, e.getMessage(), e);
            
            // 企业级错误响应：返回500但不暴露内部错误详情
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 分享作品到画廊 - 企业级API端点
     * Share Artwork to Gallery - Enterprise API Endpoint
     * 
     * POST /api/v1/enterprise/gallery/share/{drawingId}
     */
    @PostMapping("/share/{drawingId}")
    public ResponseEntity<?> shareDrawingToGallery(@PathVariable String drawingId) {
        String endpoint = "POST /api/v1/enterprise/gallery/share/" + drawingId;
        logger.info("🎨 {} - 开始处理分享请求", endpoint);
        
        // 输入验证
        if (drawingId == null || drawingId.trim().isEmpty()) {
            logger.warn("⚠️ {} - 无效的作品ID", endpoint);
            return ResponseEntity.badRequest()
                .body(Map.of("error", "无效的作品ID", "code", "INVALID_DRAWING_ID"));
        }
        
        try {
            Optional<Drawing> updatedDrawing = galleryService.shareDrawingToGallery(drawingId);
            
            if (updatedDrawing.isPresent()) {
                logger.info("✅ {} - 分享成功", endpoint);
                return ResponseEntity.ok(Map.of(
                    "message", "作品已成功分享到画廊",
                    "drawingId", drawingId,
                    "status", "shared"
                ));
            } else {
                logger.warn("⚠️ {} - 作品不存在", endpoint);
                return ResponseEntity.notFound().build();
            }
            
        } catch (Exception e) {
            logger.error("❌ {} - 分享失败: {}", endpoint, e.getMessage(), e);
            return ResponseEntity.internalServerError()
                .body(Map.of(
                    "error", "分享操作失败，请稍后重试",
                    "code", "SHARE_OPERATION_FAILED"
                ));
        }
    }

    /**
     * 获取作品详情 - 企业级API端点
     * Get Artwork Details - Enterprise API Endpoint
     * 
     * GET /api/v1/enterprise/gallery/artwork/{drawingId}
     */
    @GetMapping("/artwork/{drawingId}")
    public ResponseEntity<?> getArtworkDetails(@PathVariable String drawingId) {
        String endpoint = "GET /api/v1/enterprise/gallery/artwork/" + drawingId;
        logger.debug("🔍 {} - 获取作品详情", endpoint);
        
        if (drawingId == null || drawingId.trim().isEmpty()) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "无效的作品ID", "code", "INVALID_DRAWING_ID"));
        }
        
        try {
            Optional<Drawing> drawing = galleryService.getDrawingDetails(drawingId);
            
            if (drawing.isPresent()) {
                return ResponseEntity.ok(drawing.get());
            } else {
                return ResponseEntity.notFound()
                    .build();
            }
            
        } catch (Exception e) {
            logger.error("❌ {} - 获取详情失败: {}", endpoint, e.getMessage(), e);
            return ResponseEntity.internalServerError()
                .body(Map.of(
                    "error", "获取作品详情失败",
                    "code", "GET_DETAILS_FAILED"
                ));
        }
    }

    /**
     * 健康检查端点 - 用于监控和负载均衡器
     * Health Check Endpoint - for monitoring and load balancers
     * 
     * GET /api/v1/enterprise/gallery/health
     */
    @GetMapping("/health")
    public ResponseEntity<?> healthCheck() {
        try {
            boolean isHealthy = galleryService.isHealthy();
            
            if (isHealthy) {
                return ResponseEntity.ok(Map.of(
                    "status", "healthy",
                    "service", "enterprise-gallery",
                    "timestamp", System.currentTimeMillis()
                ));
            } else {
                return ResponseEntity.status(503)
                    .body(Map.of(
                        "status", "unhealthy",
                        "service", "enterprise-gallery",
                        "timestamp", System.currentTimeMillis()
                    ));
            }
            
        } catch (Exception e) {
            logger.error("健康检查失败: {}", e.getMessage(), e);
            return ResponseEntity.status(503)
                .body(Map.of(
                    "status", "error",
                    "error", e.getMessage(),
                    "timestamp", System.currentTimeMillis()
                ));
        }
    }
}
