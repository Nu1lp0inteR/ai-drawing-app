// 文件路径: src/main/java/com/aidrawing/backend/service/GalleryService.java

package com.aidrawing.backend.service;

import com.aidrawing.backend.entity.Drawing;
import com.aidrawing.backend.repository.DrawingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
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
     * 获取所有标记为公开的绘图记录，用于画廊展示。
     * 
     * 企业级缓存策略说明：
     * @Cacheable 注解会将方法的返回结果缓存到Redis中
     * - value: 指定缓存名称，对应Redis配置中的galleryCache
     * - key: 使用固定的key，因为所有用户看到的画廊数据是一样的
     * - 当有新的分享作品时，会通过@CacheEvict清除此缓存
     * 
     * 暂时禁用缓存以确保基本功能正常工作
     */
    // @Cacheable(value = "galleryCache", key = "'publicGallery'")
    public List<Drawing> getPublicGallery() {
        try {
            return drawingRepository.findBySharedToGalleryTrueOrderByCreatedAtDesc();
        } catch (Exception e) {
            System.err.println("Error fetching gallery: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    /**
     * Marks a specific drawing as shared to the gallery.
     * 将指定的绘图标记为分享到画廊。
     *
     * 企业级缓存策略说明：
     * @CacheEvict 注解会在方法执行后清除指定的缓存
     * - value: 清除galleryCache缓存
     * - key: 清除publicGallery这个具体的缓存项
     * - 这确保了当有新作品分享时，用户能立即看到最新的画廊内容
     *
     * @param drawingId The ID of the drawing to be shared.
     * @return The updated Drawing entity, or null if not found.
     */
    @Transactional
    @CacheEvict(value = "galleryCache", allEntries = true)
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

    /**
     * Gets the details of a specific drawing by ID.
     * 根据ID获取特定作品的详细信息。
     * 
     * 企业级缓存策略说明：
     * @Cacheable 为每个作品的详情单独缓存
     * - value: 使用drawingDetailsCache缓存区域
     * - key: 使用作品ID作为缓存key，确保每个作品都有独立的缓存项
     * - 作品详情变化频率较低，所以设置了较长的过期时间（1小时）
     */
    @CacheEvict(value = "galleryCache", allEntries = true)
    public void clearGalleryCache() {
    }

    @CacheEvict(value = "drawingDetailsCache", key = "#drawingId")
    public void clearDrawingCache(String drawingId) {
    }

    @Cacheable(value = "drawingDetailsCache", key = "#drawingId")
    public Optional<Drawing> getDrawingDetails(String drawingId) {
        return drawingRepository.findById(drawingId);
    }
}
