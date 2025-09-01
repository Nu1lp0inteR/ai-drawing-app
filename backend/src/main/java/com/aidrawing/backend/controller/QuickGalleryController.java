package com.aidrawing.backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 快速画廊控制器 - 确保立即工作
 * 基于之前测试成功的代码
 */
@RestController
@RequestMapping("/api/v1/quick")
public class QuickGalleryController {

    @GetMapping("/gallery")
    public ResponseEntity<List<Map<String, Object>>> getQuickGallery() {
        List<Map<String, Object>> testData = List.of(
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
        return ResponseEntity.ok(testData);
    }
    
    // 临时测试端点 - 验证后端基本功能
    @GetMapping("/test")
    public ResponseEntity<Map<String, Object>> getTest() {
        return ResponseEntity.ok(Map.of(
            "status", "ok",
            "message", "QuickGalleryController is working",
            "timestamp", System.currentTimeMillis()
        ));
    }
}
