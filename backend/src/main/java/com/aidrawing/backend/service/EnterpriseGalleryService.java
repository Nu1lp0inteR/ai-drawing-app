package com.aidrawing.backend.service;

import com.aidrawing.backend.entity.Drawing;
import com.aidrawing.backend.repository.DrawingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * 企业级画廊服务
 * Enterprise-level Gallery Service
 * 
 * 遵循企业级开发最佳实践：
 * - 完善的日志记录和监控
 * - 健壮的错误处理和降级策略
 * - 高性能缓存策略
 * - 详细的文档和注释
 * - 事务管理和数据一致性保证
 * 
 * Enterprise development best practices:
 * - Comprehensive logging and monitoring
 * - Robust error handling and fallback strategies
 * - High-performance caching strategies
 * - Detailed documentation and comments
 * - Transaction management and data consistency guarantees
 */
@Service
@Transactional(readOnly = true)
public class EnterpriseGalleryService {

    private static final Logger logger = LoggerFactory.getLogger(EnterpriseGalleryService.class);
    
    private final DrawingRepository drawingRepository;

    @Autowired
    public EnterpriseGalleryService(DrawingRepository drawingRepository) {
        this.drawingRepository = drawingRepository;
        logger.info("EnterpriseGalleryService initialized with database repository");
    }

    /**
     * 获取公共画廊中的所有作品 - 企业级实现
     * Retrieves all drawings shared to public gallery - Enterprise implementation
     * 
     * 特性 Features:
     * - 🚀 Redis缓存提升性能 (10分钟TTL)
     * - 🛡️ 优雅错误处理和降级策略
     * - 📊 详细的性能监控和日志
     * - 🔄 自动缓存失效和刷新机制
     */
    @Cacheable(value = "galleryCache", key = "'publicGallery'")
    public List<Drawing> getPublicGallery() {
        long startTime = System.currentTimeMillis();
        logger.info("开始获取公共画廊数据 - Starting to fetch public gallery data");
        
        try {
            List<Drawing> drawings = drawingRepository.findBySharedToGalleryTrueOrderByCreatedAtDesc();
            
            long executionTime = System.currentTimeMillis() - startTime;
            logger.info("✅ 成功获取画廊数据: {} 张作品, 耗时: {} ms", 
                       drawings.size(), executionTime);
            
            // 性能监控 - Performance monitoring
            if (executionTime > 1000) {
                logger.warn("⚠️ 画廊查询性能警告: 查询时间超过1秒 ({} ms)", executionTime);
            }
            
            return drawings;
            
        } catch (DataAccessException e) {
            logger.error("❌ 数据库访问错误: {}", e.getMessage(), e);
            // 企业级降级策略：返回空列表而不是抛出异常，保证前端不崩溃
            return Collections.emptyList();
            
        } catch (Exception e) {
            logger.error("❌ 获取画廊数据时发生未知错误: {}", e.getMessage(), e);
            // 企业级错误处理：记录详细错误但返回空列表
            return Collections.emptyList();
        }
    }

    /**
     * 将作品分享到画廊 - 企业级实现
     * Share artwork to gallery - Enterprise implementation
     * 
     * 特性 Features:
     * - 🔒 事务性操作保证数据一致性
     * - 🗑️ 智能缓存失效机制
     * - 📝 完整的操作审计日志
     * - ✅ 详细的业务验证
     */
    @Transactional
    @CacheEvict(value = "galleryCache", allEntries = true)
    public Optional<Drawing> shareDrawingToGallery(String drawingId) {
        logger.info("🎨 开始分享作品到画廊: {}", drawingId);
        
        try {
            Optional<Drawing> drawingOptional = drawingRepository.findById(drawingId);
            
            if (drawingOptional.isEmpty()) {
                logger.warn("⚠️ 分享失败: 未找到作品 ID {}", drawingId);
                return Optional.empty();
            }
            
            Drawing drawing = drawingOptional.get();
            
            // 业务逻辑验证
            if (drawing.isSharedToGallery()) {
                logger.info("ℹ️ 作品 {} 已经分享到画廊，跳过操作", drawingId);
                return Optional.of(drawing);
            }
            
            // 执行分享操作
            drawing.setSharedToGallery(true);
            Drawing updatedDrawing = drawingRepository.save(drawing);
            
            logger.info("✅ 成功分享作品到画廊: {} (prompt: {})", 
                       drawingId, 
                       drawing.getPrompt() != null ? drawing.getPrompt().substring(0, Math.min(50, drawing.getPrompt().length())) + "..." : "无描述");
            
            return Optional.of(updatedDrawing);
            
        } catch (DataAccessException e) {
            logger.error("❌ 分享作品到画廊时数据库错误: {}", e.getMessage(), e);
            throw new RuntimeException("数据库操作失败，请稍后重试", e);
            
        } catch (Exception e) {
            logger.error("❌ 分享作品到画廊时发生未知错误: {}", e.getMessage(), e);
            throw new RuntimeException("分享操作失败，请稍后重试", e);
        }
    }

    /**
     * 获取作品详情 - 企业级实现
     * Get artwork details - Enterprise implementation
     */
    @Cacheable(value = "drawingDetailsCache", key = "#drawingId")
    public Optional<Drawing> getDrawingDetails(String drawingId) {
        logger.debug("🔍 获取作品详情: {}", drawingId);
        
        try {
            Optional<Drawing> drawing = drawingRepository.findById(drawingId);
            
            if (drawing.isPresent()) {
                logger.debug("✅ 成功获取作品详情: {}", drawingId);
            } else {
                logger.warn("⚠️ 未找到作品: {}", drawingId);
            }
            
            return drawing;
            
        } catch (Exception e) {
            logger.error("❌ 获取作品详情时发生错误: {}", e.getMessage(), e);
            return Optional.empty();
        }
    }

    /**
     * 健康检查方法 - 用于监控和诊断
     * Health check method - for monitoring and diagnostics
     */
    public boolean isHealthy() {
        try {
            // 执行一个简单的数据库查询来检查连接
            drawingRepository.count();
            return true;
        } catch (Exception e) {
            logger.error("画廊服务健康检查失败: {}", e.getMessage());
            return false;
        }
    }
}
