package com.aidrawing.backend.service;

import com.aidrawing.backend.dto.UserProfileDto;
import com.aidrawing.backend.dto.ProfileDto;
import com.aidrawing.backend.entity.User;
import com.aidrawing.backend.entity.Drawing;
import com.aidrawing.backend.repository.UserRepository;
import com.aidrawing.backend.repository.DrawingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 用户个人主页服务
 * User Profile Service - 处理用户公开信息和作品展示
 */
@Service
@Transactional(readOnly = true)
public class UserProfileService {

    private static final Logger logger = LoggerFactory.getLogger(UserProfileService.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DrawingRepository drawingRepository;

    @Autowired
    private FollowService followService;

    /**
     * 获取用户公开个人主页信息
     * @param userId 用户ID
     * @return 用户公开信息和统计数据
     */
    public UserProfileDto getUserProfile(String userId) {
        logger.info("🔍 获取用户公开主页: userId={}", userId);

        // 1. 查找用户
        Optional<User> userOptional = userRepository.findById(userId);
        if (userOptional.isEmpty()) {
            logger.warn("⚠️ 用户不存在: userId={}", userId);
            throw new RuntimeException("用户不存在");
        }

        User user = userOptional.get();
        UserProfileDto userProfile = new UserProfileDto(user);

        // 2. 获取用户统计信息
        logger.info("📊 获取用户统计信息: userId={}", userId);
        
        // 总作品数
        Long totalArtworks = drawingRepository.countByUserId(userId);
        userProfile.setTotalArtworks(totalArtworks);

        // 已分享作品数（公开作品数）
        Long sharedArtworks = drawingRepository.countByUserIdAndSharedToGallery(userId, true);
        userProfile.setSharedArtworks(sharedArtworks);

        // 最后活跃时间（最新作品的创建时间）
        Optional<Drawing> latestDrawing = drawingRepository.findTopByUserIdOrderByCreatedAtDesc(userId);
        if (latestDrawing.isPresent()) {
            userProfile.setLastActiveDate(latestDrawing.get().getCreatedAt());
        } else {
            userProfile.setLastActiveDate(user.getCreatedAt()); // 如果没有作品，使用注册时间
        }

        // 3. 获取关注/粉丝统计
        logger.info("👥 获取关注统计信息: userId={}", userId);
        Long[] followCounts = followService.getFollowCounts(userId);
        userProfile.setFollowingCount(followCounts[0]); // 关注数
        userProfile.setFollowersCount(followCounts[1]); // 粉丝数

        logger.info("✅ 用户主页信息获取成功: userId={}, username={}, 总作品={}, 公开作品={}, 关注数={}, 粉丝数={}", 
            userId, user.getUsername(), totalArtworks, sharedArtworks, followCounts[0], followCounts[1]);

        return userProfile;
    }

    /**
     * 获取用户公开作品列表（只返回已分享到画廊的作品）
     * @param userId 用户ID
     * @param page 页码
     * @param size 每页大小
     * @return 用户公开作品列表
     */
    public ProfileDto.UserArtworkListResponse getUserPublicArtworks(String userId, int page, int size) {
        logger.info("🎨 获取用户公开作品: userId={}, page={}, size={}", userId, page, size);

        // 验证用户存在
        if (!userRepository.existsById(userId)) {
            logger.warn("⚠️ 用户不存在: userId={}", userId);
            throw new RuntimeException("用户不存在");
        }

        // 创建分页请求
        Pageable pageable = PageRequest.of(page, size);

        // 只查询已分享到画廊的作品（公开作品）
        Page<Drawing> drawingsPage = drawingRepository.findByUserIdAndSharedToGalleryOrderByCreatedAtDesc(
            userId, true, pageable);

        // 转换为DTO
        List<ProfileDto.UserArtworkItem> artworkDtos = drawingsPage.getContent().stream()
            .map(this::convertToUserArtworkItem)
            .collect(Collectors.toList());

        // 构建返回结果
        ProfileDto.UserArtworkListResponse result = new ProfileDto.UserArtworkListResponse();
        result.setArtworks(artworkDtos);
        result.setTotalCount(drawingsPage.getTotalElements());
        result.setCurrentPage(page + 1); // 前端使用1开始的页码
        result.setTotalPages(drawingsPage.getTotalPages());
        result.setPageSize(size);

        logger.info("✅ 用户公开作品获取成功: userId={}, 当前页作品数={}, 总公开作品数={}", 
            userId, artworkDtos.size(), drawingsPage.getTotalElements());

        return result;
    }

    /**
     * 将Drawing实体转换为UserArtworkItem DTO
     */
    private ProfileDto.UserArtworkItem convertToUserArtworkItem(Drawing drawing) {
        ProfileDto.UserArtworkItem item = new ProfileDto.UserArtworkItem();
        item.setId(drawing.getId());
        item.setPrompt(drawing.getPrompt());
        item.setNegativePrompt(drawing.getNegativePrompt());
        item.setSteps(drawing.getSteps());
        item.setCfg(drawing.getCfg());
        item.setSamplerName(drawing.getSamplerName());
        item.setSeed(drawing.getSeed());
        item.setStoredFilename(drawing.getStoredFilename());
        item.setOriginalFilename(drawing.getOriginalFilename());
        item.setFileType(drawing.getFileType());
        item.setSharedToGallery(drawing.isSharedToGallery());
        item.setCreatedAt(drawing.getCreatedAt());
        return item;
    }
}
