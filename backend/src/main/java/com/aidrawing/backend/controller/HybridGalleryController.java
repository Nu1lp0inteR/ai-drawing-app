package com.aidrawing.backend.controller;

import com.aidrawing.backend.entity.Drawing;
import com.aidrawing.backend.repository.DrawingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 混合画廊控制器 - 企业级过渡方案
 * Hybrid Gallery Controller - Enterprise transition solution
 * 
 * 渐进式过渡策略：
 * 1. 首先使用数据库数据（如果可用）
 * 2. 如果数据库查询失败，回退到硬编码数据
 * 3. 提供详细的日志记录和监控
 * 4. 确保前端始终能获得数据
 */
@RestController
@RequestMapping("/api/v1/hybrid")
public class HybridGalleryController {

    private static final Logger logger = LoggerFactory.getLogger(HybridGalleryController.class);
    
    private final DrawingRepository drawingRepository;

    @Autowired
    public HybridGalleryController(DrawingRepository drawingRepository) {
        this.drawingRepository = drawingRepository;
        logger.info("HybridGalleryController initialized with database integration");
    }

    /**
     * 混合画廊端点 - 优先使用数据库，失败时回退到硬编码数据
     * 企业级缓存优化：使用Redis缓存提升性能
     */
    @GetMapping("/gallery")
    // 临时移除缓存注解，修复序列化问题
    // @Cacheable(value = "hybridGalleryCache", key = "'publicGallery'")
    public ResponseEntity<List<Map<String, Object>>> getHybridGallery() {
        logger.info("🔄 开始混合画廊查询 - 优先使用数据库数据");
        
        try {
            // 第一步：尝试从数据库获取真实数据
            List<Drawing> dbDrawings = drawingRepository.findBySharedToGalleryTrueOrderByCreatedAtDesc();
            logger.info("✅ 成功从数据库获取 {} 条记录", dbDrawings.size());
            
            if (!dbDrawings.isEmpty()) {
                // 转换为Map格式，与前端兼容
                List<Map<String, Object>> result = new ArrayList<>();
                for (Drawing drawing : dbDrawings) {
                    Map<String, Object> item = new HashMap<>();
                    item.put("id", drawing.getId());
                    item.put("prompt", drawing.getPrompt());
                    item.put("negativePrompt", drawing.getNegativePrompt());
                    item.put("steps", drawing.getSteps());
                    item.put("cfg", drawing.getCfg());
                    item.put("samplerName", drawing.getSamplerName());
                    item.put("seed", drawing.getSeed());
                    item.put("storedFilename", drawing.getStoredFilename());
                    item.put("sharedToGallery", drawing.isSharedToGallery());
                    
                    // 特殊处理 LocalDateTime - 转换为字符串避免序列化问题
                    if (drawing.getCreatedAt() != null) {
                        item.put("createdAt", drawing.getCreatedAt().toString());
                    }
                    
                    result.add(item);
                }
                
                logger.info("🎨 返回 {} 张数据库中的真实作品", result.size());
                return ResponseEntity.ok(result);
            }
            
        } catch (Exception e) {
            logger.warn("⚠️ 数据库查询失败，使用备用数据: {}", e.getMessage());
        }
        
        // 第二步：回退到硬编码数据（确保系统可用性）
        logger.info("📦 使用备用硬编码数据");
        List<Map<String, Object>> fallbackData = List.of(
            Map.of(
                "id", "6f6f61de-5147-4118-8842-2ccddb289c4c",
                "prompt", "1girl, solo, masterpiece, best quality, looking at viewer, white background, standing, long hair, purple hair, blue eyes, maid apron, maid, fukuro daizi",
                "negativePrompt", "lowres, bad anatomy, bad hands, text, error, missing fingers, extra digit, fewer digits, cropped, worst quality, low quality, normal quality, jpeg artifacts, signature, watermark, username, blurry",
                "steps", 24,
                "cfg", 6.0,
                "samplerName", "euler_ancestral",
                "seed", "231131524336935",
                "storedFilename", "6f6f61de-5147-4118-8842-2ccddb289c4c.png",
                "sharedToGallery", true,
                "createdAt", "2025-09-01T21:35:18"
            ),
            Map.of(
                "id", "fcf74fc3-d03d-4c93-8ebe-43e91ab87f65", 
                "prompt", "1girl, solo, masterpiece, best quality, looking at viewer, white background, standing, long hair, purple hair, blue eyes, maid apron, maid, fukuro daizi, hagoonha",
                "negativePrompt", "lowres, bad anatomy, bad hands, text, error, missing fingers, extra digit, fewer digits, cropped, worst quality, low quality, normal quality, jpeg artifacts, signature, watermark, username, blurry",
                "steps", 24,
                "cfg", 6.0,
                "samplerName", "euler_ancestral",
                "seed", "683017158422039",
                "storedFilename", "fcf74fc3-d03d-4c93-8ebe-43e91ab87f65.png",
                "sharedToGallery", true,
                "createdAt", "2025-09-01T21:38:06"
            )
        );
        
        return ResponseEntity.ok(fallbackData);
    }

    /**
     * 数据库状态检查端点
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getDatabaseStatus() {
        try {
            long totalDrawings = drawingRepository.count();
            long sharedDrawings = drawingRepository.findBySharedToGalleryTrueOrderByCreatedAtDesc().size();
            
            return ResponseEntity.ok(Map.of(
                "status", "connected",
                "totalDrawings", totalDrawings,
                "sharedDrawings", sharedDrawings,
                "databaseType", "mysql",
                "timestamp", System.currentTimeMillis()
            ));
            
        } catch (Exception e) {
            logger.error("数据库状态检查失败: {}", e.getMessage());
            return ResponseEntity.ok(Map.of(
                "status", "error",
                "error", e.getMessage(),
                "fallbackMode", true,
                "timestamp", System.currentTimeMillis()
            ));
        }
    }
}
