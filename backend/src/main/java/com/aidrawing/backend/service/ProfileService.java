package com.aidrawing.backend.service;

import com.aidrawing.backend.dto.ProfileDto;
import com.aidrawing.backend.entity.Drawing;
import com.aidrawing.backend.entity.User;
import com.aidrawing.backend.repository.DrawingRepository;
import com.aidrawing.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 个人中心业务逻辑服务
 * 负责用户作品管理、统计信息等功能
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProfileService {

    private final DrawingRepository drawingRepository;
    private final UserRepository userRepository;

    /**
     * 获取用户的作品列表（分页）
     */
    public ProfileDto.UserArtworkListResponse getUserArtworks(String userId, int page, int size) {
        log.info("🎨 获取用户作品列表: userId={}, page={}, size={}", userId, page, size);
        
        try {
            // 创建分页和排序参数（按创建时间倒序）
            Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
            
            // 查询用户的作品
            Page<Drawing> drawingPage = drawingRepository.findByUserId(userId, pageable);
            
            // 转换为DTO
            List<ProfileDto.UserArtworkItem> artworkItems = drawingPage.getContent()
                .stream()
                .map(this::convertToUserArtworkItem)
                .collect(Collectors.toList());
            
            ProfileDto.UserArtworkListResponse response = new ProfileDto.UserArtworkListResponse(
                artworkItems,
                drawingPage.getTotalElements(),
                page,
                size,
                drawingPage.getTotalPages()
            );
            
            log.info("✅ 成功获取用户作品: 总数={}, 当前页={}/{}", 
                drawingPage.getTotalElements(), page + 1, drawingPage.getTotalPages());
            
            return response;
            
        } catch (Exception e) {
            log.error("❌ 获取用户作品列表失败: userId={}", userId, e);
            throw new RuntimeException("获取作品列表失败", e);
        }
    }

    /**
     * 获取用户统计信息
     */
    public ProfileDto.UserStats getUserStats(String userId) {
        log.info("📊 获取用户统计信息: userId={}", userId);
        
        try {
            // 获取用户信息
            User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
            
            // 统计总作品数
            Long totalArtworks = drawingRepository.countByUserId(userId);
            
            // 统计已分享作品数
            Long sharedArtworks = drawingRepository.countByUserIdAndSharedToGallery(userId, true);
            
            // 获取最后活跃时间（最新作品的创建时间）
            LocalDateTime lastActiveDate = drawingRepository.findTopByUserIdOrderByCreatedAtDesc(userId)
                .map(Drawing::getCreatedAt)
                .orElse(user.getCreatedAt());
            
            ProfileDto.UserStats stats = new ProfileDto.UserStats(
                totalArtworks,
                sharedArtworks,
                user.getCreatedAt(),
                lastActiveDate
            );
            
            log.info("✅ 用户统计信息: 总作品={}, 已分享={}", totalArtworks, sharedArtworks);
            return stats;
            
        } catch (Exception e) {
            log.error("❌ 获取用户统计信息失败: userId={}", userId, e);
            throw new RuntimeException("获取统计信息失败", e);
        }
    }

    /**
     * 获取个人中心主页数据
     */
    public ProfileDto.ProfileHomeData getProfileHomeData(String userId) {
        log.info("🏠 获取个人中心主页数据: userId={}", userId);
        
        try {
            // 获取统计信息
            ProfileDto.UserStats userStats = getUserStats(userId);
            
            // 获取最近的作品（最多10张）
            List<Drawing> recentDrawings = drawingRepository.findTop10ByUserIdOrderByCreatedAtDesc(userId);
            List<ProfileDto.UserArtworkItem> recentArtworks = recentDrawings.stream()
                .map(this::convertToUserArtworkItem)
                .collect(Collectors.toList());
            
            ProfileDto.ProfileHomeData homeData = new ProfileDto.ProfileHomeData(userStats, recentArtworks);
            
            log.info("✅ 个人中心主页数据获取成功: 最近作品数={}", recentArtworks.size());
            return homeData;
            
        } catch (Exception e) {
            log.error("❌ 获取个人中心主页数据失败: userId={}", userId, e);
            throw new RuntimeException("获取主页数据失败", e);
        }
    }

    /**
     * 删除用户作品
     */
    public void deleteUserArtwork(String userId, String drawingId) {
        log.info("🗑️ 删除用户作品: userId={}, drawingId={}", userId, drawingId);
        
        try {
            // 验证作品是否属于该用户
            Drawing drawing = drawingRepository.findById(drawingId)
                .orElseThrow(() -> new RuntimeException("作品不存在"));
            
            if (!drawing.getUser().getId().equals(userId)) {
                throw new RuntimeException("无权限删除此作品");
            }
            
            // 删除作品记录
            drawingRepository.delete(drawing);
            
            log.info("✅ 用户作品删除成功: drawingId={}", drawingId);
            
        } catch (Exception e) {
            log.error("❌ 删除用户作品失败: userId={}, drawingId={}", userId, drawingId, e);
            throw new RuntimeException("删除作品失败", e);
        }
    }

    /**
     * 切换作品分享状态
     */
    public void toggleArtworkSharing(String userId, String drawingId, boolean shareToGallery) {
        log.info("🔄 切换作品分享状态: userId={}, drawingId={}, shareToGallery={}", 
            userId, drawingId, shareToGallery);
        
        try {
            // 验证作品是否属于该用户
            Drawing drawing = drawingRepository.findById(drawingId)
                .orElseThrow(() -> new RuntimeException("作品不存在"));
            
            if (!drawing.getUser().getId().equals(userId)) {
                throw new RuntimeException("无权限修改此作品");
            }
            
            // 更新分享状态
            drawing.setSharedToGallery(shareToGallery);
            drawingRepository.save(drawing);
            
            log.info("✅ 作品分享状态更新成功: drawingId={}, 新状态={}", drawingId, shareToGallery);
            
        } catch (Exception e) {
            log.error("❌ 切换作品分享状态失败: userId={}, drawingId={}", userId, drawingId, e);
            throw new RuntimeException("更新分享状态失败", e);
        }
    }

    /**
     * 将Drawing实体转换为UserArtworkItem DTO
     */
    private ProfileDto.UserArtworkItem convertToUserArtworkItem(Drawing drawing) {
        return new ProfileDto.UserArtworkItem(
            drawing.getId(),
            drawing.getPrompt(),
            drawing.getNegativePrompt(),
            drawing.getSteps(),
            drawing.getCfg(),
            drawing.getSamplerName(),
            drawing.getSeed(),
            drawing.getStoredFilename(),
            drawing.getOriginalFilename(),
            drawing.getFileType(),
            drawing.isSharedToGallery(),
            drawing.getCreatedAt()
        );
    }
}
