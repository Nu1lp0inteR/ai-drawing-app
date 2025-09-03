package com.aidrawing.backend.controller;

import com.aidrawing.backend.dto.GalleryItemDto;
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
    public ResponseEntity<List<GalleryItemDto>> getHybridGallery() {
        logger.info("🔄 开始混合画廊查询 - 优先使用数据库数据");
        
        try {
            // 第一步：尝试从数据库获取真实数据（包含作者信息）
            List<Drawing> dbDrawings = drawingRepository.findBySharedToGalleryTrueWithUserOrderByCreatedAtDesc();
            logger.info("✅ 成功从数据库获取 {} 条记录", dbDrawings.size());
            
            if (!dbDrawings.isEmpty()) {
                // 使用DTO转换，包含作者信息
                List<GalleryItemDto> result = new ArrayList<>();
                for (Drawing drawing : dbDrawings) {
                    result.add(new GalleryItemDto(drawing));
                }
                
                logger.info("🎨 返回 {} 张数据库中的真实作品（含作者信息）", result.size());
                return ResponseEntity.ok(result);
            }
            
        } catch (Exception e) {
            logger.warn("⚠️ 数据库查询失败，使用备用数据: {}", e.getMessage());
        }
        
        // 第二步：回退到硬编码数据（确保系统可用性）
        logger.info("📦 使用备用硬编码数据");
        List<GalleryItemDto> fallbackData = createFallbackData();
        
        return ResponseEntity.ok(fallbackData);
    }

    /**
     * 创建fallback数据（包含作者信息）
     */
    private List<GalleryItemDto> createFallbackData() {
        List<GalleryItemDto> fallbackData = new ArrayList<>();
        
        // 示例作品1
        GalleryItemDto item1 = new GalleryItemDto();
        item1.setId("6f6f61de-5147-4118-8842-2ccddb289c4c");
        item1.setPrompt("1girl, solo, masterpiece, best quality, looking at viewer, white background, standing, long hair, purple hair, blue eyes, maid apron, maid, fukuro daizi");
        item1.setNegativePrompt("lowres, bad anatomy, bad hands, text, error, missing fingers, extra digit, fewer digits, cropped, worst quality, low quality, normal quality, jpeg artifacts, signature, watermark, username, blurry");
        item1.setSteps(24);
        item1.setCfg(6.0);
        item1.setSamplerName("euler_ancestral");
        item1.setSeed("231131524336935");
        item1.setStoredFilename("6f6f61de-5147-4118-8842-2ccddb289c4c.png");
        item1.setSharedToGallery(true);
        item1.setAuthorName("示例用户");
        item1.setAuthorId("demo-user-1");
        
        // 示例作品2
        GalleryItemDto item2 = new GalleryItemDto();
        item2.setId("fcf74fc3-d03d-4c93-8ebe-43e91ab87f65");
        item2.setPrompt("1girl, solo, masterpiece, best quality, looking at viewer, white background, standing, long hair, purple hair, blue eyes, maid apron, maid, fukuro daizi, hagoonha");
        item2.setNegativePrompt("lowres, bad anatomy, bad hands, text, error, missing fingers, extra digit, fewer digits, cropped, worst quality, low quality, normal quality, jpeg artifacts, signature, watermark, username, blurry");
        item2.setSteps(24);
        item2.setCfg(6.0);
        item2.setSamplerName("euler_ancestral");
        item2.setSeed("683017158422039");
        item2.setStoredFilename("fcf74fc3-d03d-4c93-8ebe-43e91ab87f65.png");
        item2.setSharedToGallery(true);
        item2.setAuthorName("AI艺术家");
        item2.setAuthorId("demo-user-2");
        
        fallbackData.add(item1);
        fallbackData.add(item2);
        
        return fallbackData;
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
