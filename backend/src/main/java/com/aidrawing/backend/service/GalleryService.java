// 文件路径: src/main/java/com/aidrawing/backend/service/GalleryService.java

package com.aidrawing.backend.service;

import com.aidrawing.backend.entity.Drawing;
import com.aidrawing.backend.repository.DrawingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class GalleryService {

    private final DrawingRepository drawingRepository;

    @Autowired
    public GalleryService(DrawingRepository drawingRepository) {
        this.drawingRepository = drawingRepository;
    }

    /**
     * Retrieves all drawing records that are marked as public for the gallery.
     * * 获取所有标记为公开的绘图记录，用于画廊展示。
     */
    public List<Drawing> getPublicGallery() {
        return drawingRepository.findBySharedToGalleryTrueOrderByCreatedAtDesc();
    }

    /**
     * Marks a specific drawing as shared to the gallery.
     * * 将指定的绘图标记为分享到画廊。
     *
     * @param drawingId The ID of the drawing to be shared.
     * @return The updated Drawing entity, or null if not found.
     */
    @Transactional // Ensures the database operation is atomic.
    public Optional<Drawing> shareDrawingToGallery(String drawingId) {
        // Find the drawing by its ID
        Optional<Drawing> drawingOptional = drawingRepository.findById(drawingId);
        
        if (drawingOptional.isPresent()) {
            Drawing drawing = drawingOptional.get();
            drawing.setSharedToGallery(true); // Set the public flag
            Drawing updatedDrawing = drawingRepository.save(drawing); // Save the change
            return Optional.of(updatedDrawing);
        } else {
            // Handle the case where the drawing is not found
            return Optional.empty();
        }
    }
}
