// 文件路径: src/main/java/com/aidrawing/backend/controller/DrawingController.java

package com.aidrawing.backend.controller;

import com.aidrawing.backend.dto.DrawingRequest;
import com.aidrawing.backend.entity.Drawing;
import com.aidrawing.backend.service.DrawingTaskService;
import com.aidrawing.backend.service.GalleryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/ai-drawing")
public class DrawingController {

    private final DrawingTaskService drawingTaskService;
    private final GalleryService galleryService;

    @Autowired
    public DrawingController(DrawingTaskService drawingTaskService, GalleryService galleryService) {
        this.drawingTaskService = drawingTaskService;
        this.galleryService = galleryService;
    }

    @PostMapping("/generate")
    public ResponseEntity<?> generateImage(@RequestBody DrawingRequest request) {
        drawingTaskService.sendDrawingTask(request);
        return ResponseEntity.ok(Map.of("message", "Task has been successfully queued.", "status", "QUEUED"));
    }

    /**
     * Gets the public gallery history.
     * * 获取公共画廊的历史记录。
     */
    @GetMapping("/history")
    public ResponseEntity<List<Drawing>> getPublicHistory() {
        List<Drawing> history = galleryService.getPublicGallery();
        return ResponseEntity.ok(history);
    }

    /**
     * New endpoint to share a drawing to the public gallery.
     * * 用于将绘图分享到公共画廊的新端点。
     *
     * @param drawingId The ID of the drawing to share.
     * @return A success response or a not found error.
     */
    @PostMapping("/{drawingId}/share")
    public ResponseEntity<?> shareDrawing(@PathVariable String drawingId) {
        Optional<Drawing> updatedDrawing = galleryService.shareDrawingToGallery(drawingId);
        if (updatedDrawing.isPresent()) {
            return ResponseEntity.ok(Map.of("message", "Drawing successfully shared to the gallery."));
        } else {
            return ResponseEntity.status(404).body(Map.of("message", "Drawing not found with ID: " + drawingId));
        }
    }
}

