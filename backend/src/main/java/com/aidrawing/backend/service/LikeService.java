package com.aidrawing.backend.service;

import com.aidrawing.backend.dto.LikeDto;
import com.aidrawing.backend.entity.Like;
import com.aidrawing.backend.entity.User;
import com.aidrawing.backend.entity.Drawing;
import com.aidrawing.backend.repository.LikeRepository;
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
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 点赞系统服务
 * Like System Service
 */
@Service
@Transactional
public class LikeService {

    private static final Logger logger = LoggerFactory.getLogger(LikeService.class);

    @Autowired
    private LikeRepository likeRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DrawingRepository drawingRepository;

    /**
     * 点赞作品
     * @param userId 用户ID
     * @param drawingId 作品ID
     * @return 点赞操作结果
     */
    public LikeDto.LikeActionResponse likeDrawing(String userId, String drawingId) {
        logger.info("👍 用户点赞操作: userId={}, drawingId={}", userId, drawingId);

        // 1. 验证用户和作品
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("用户不存在"));
        Drawing drawing = drawingRepository.findById(drawingId)
            .orElseThrow(() -> new RuntimeException("作品不存在"));

        // 2. 检查是否已经点赞
        if (likeRepository.existsByUserIdAndDrawingId(userId, drawingId)) {
            throw new IllegalStateException("已经点赞了该作品");
        }

        // 3. 创建点赞关系
        Like like = new Like(user, drawing);
        likeRepository.save(like);

        // 4. 获取最新的点赞数
        Long likesCount = likeRepository.countByDrawingId(drawingId);

        logger.info("✅ 点赞成功: userId={}, drawingId={}, 作品点赞数: {}", 
            userId, drawingId, likesCount);

        return new LikeDto.LikeActionResponse(true, likesCount, "点赞成功");
    }

    /**
     * 取消点赞作品
     * @param userId 用户ID
     * @param drawingId 作品ID
     * @return 取消点赞操作结果
     */
    public LikeDto.LikeActionResponse unlikeDrawing(String userId, String drawingId) {
        logger.info("👍 取消点赞操作: userId={}, drawingId={}", userId, drawingId);

        // 1. 验证用户和作品存在
        if (!userRepository.existsById(userId)) {
            throw new RuntimeException("用户不存在");
        }
        if (!drawingRepository.existsById(drawingId)) {
            throw new RuntimeException("作品不存在");
        }

        // 2. 检查点赞关系是否存在
        if (!likeRepository.existsByUserIdAndDrawingId(userId, drawingId)) {
            throw new IllegalStateException("未点赞该作品");
        }

        // 3. 删除点赞关系
        likeRepository.deleteByUserIdAndDrawingId(userId, drawingId);

        // 4. 获取最新的点赞数
        Long likesCount = likeRepository.countByDrawingId(drawingId);

        logger.info("✅ 取消点赞成功: userId={}, drawingId={}, 作品点赞数: {}", 
            userId, drawingId, likesCount);

        return new LikeDto.LikeActionResponse(false, likesCount, "已取消点赞");
    }

    /**
     * 检查是否点赞了某作品
     * @param userId 用户ID
     * @param drawingId 作品ID
     * @return 是否点赞
     */
    @Transactional(readOnly = true)
    public boolean isLiked(String userId, String drawingId) {
        if (userId == null || drawingId == null) {
            return false;
        }
        return likeRepository.existsByUserIdAndDrawingId(userId, drawingId);
    }

    /**
     * 获取作品的点赞列表
     * @param drawingId 作品ID
     * @param page 页码
     * @param size 每页大小
     * @return 点赞列表
     */
    @Transactional(readOnly = true)
    public LikeDto.LikeListResponse getDrawingLikes(String drawingId, int page, int size) {
        logger.info("📋 获取作品点赞列表: drawingId={}, page={}, size={}", drawingId, page, size);

        Pageable pageable = PageRequest.of(page, size);
        Page<Like> likesPage = likeRepository.findLikesByDrawingId(drawingId, pageable);

        List<LikeDto.LikeInfo> likeInfos = likesPage.getContent().stream()
            .map(like -> new LikeDto.LikeInfo(like, false)) // 不包含作品信息，因为已知drawingId
            .collect(Collectors.toList());

        LikeDto.LikeListResponse response = new LikeDto.LikeListResponse();
        response.setLikes(likeInfos);
        response.setTotalCount(likesPage.getTotalElements());
        response.setCurrentPage(page + 1);
        response.setPageSize(size);
        response.setTotalPages(likesPage.getTotalPages());

        logger.info("✅ 获取作品点赞列表成功: drawingId={}, 点赞数={}", drawingId, likeInfos.size());
        return response;
    }

    /**
     * 获取用户的点赞列表
     * @param userId 用户ID
     * @param page 页码
     * @param size 每页大小
     * @return 用户点赞列表
     */
    @Transactional(readOnly = true)
    public LikeDto.LikeListResponse getUserLikes(String userId, int page, int size) {
        logger.info("📋 获取用户点赞列表: userId={}, page={}, size={}", userId, page, size);

        Pageable pageable = PageRequest.of(page, size);
        Page<Like> likesPage = likeRepository.findLikesByUserId(userId, pageable);

        List<LikeDto.LikeInfo> likeInfos = likesPage.getContent().stream()
            .map(like -> new LikeDto.LikeInfo(like, true)) // 包含作品信息
            .collect(Collectors.toList());

        LikeDto.LikeListResponse response = new LikeDto.LikeListResponse();
        response.setLikes(likeInfos);
        response.setTotalCount(likesPage.getTotalElements());
        response.setCurrentPage(page + 1);
        response.setPageSize(size);
        response.setTotalPages(likesPage.getTotalPages());

        logger.info("✅ 获取用户点赞列表成功: userId={}, 点赞数={}", userId, likeInfos.size());
        return response;
    }

    /**
     * 获取作品的点赞数
     * @param drawingId 作品ID
     * @return 点赞数
     */
    @Transactional(readOnly = true)
    public Long getDrawingLikesCount(String drawingId) {
        return likeRepository.countByDrawingId(drawingId);
    }

    /**
     * 批量获取多个作品的点赞数和用户点赞状态
     * @param userId 用户ID（可为null，表示未登录）
     * @param drawingIds 作品ID列表
     * @return 点赞状态列表
     */
    @Transactional(readOnly = true)
    public List<LikeDto.LikeStatusResponse> getBatchLikeStatus(String userId, List<String> drawingIds) {
        logger.info("📊 批量获取点赞状态: userId={}, drawingIds.size={}", userId, drawingIds.size());

        // 1. 获取所有作品的点赞数
        List<Object[]> likeCountsRaw = likeRepository.countLikesByDrawingIds(drawingIds);
        Map<String, Long> likeCountsMap = likeCountsRaw.stream()
            .collect(Collectors.toMap(
                row -> (String) row[0], 
                row -> (Long) row[1]
            ));

        // 2. 获取用户的点赞状态（如果用户已登录）
        List<String> likedDrawingIds = List.of();
        if (userId != null) {
            likedDrawingIds = likeRepository.findLikedDrawingIdsByUserAndDrawingIds(userId, drawingIds);
        }

        // 3. 构建响应
        final List<String> finalLikedDrawingIds = likedDrawingIds;
        List<LikeDto.LikeStatusResponse> responses = drawingIds.stream()
            .map(drawingId -> new LikeDto.LikeStatusResponse(
                drawingId,
                finalLikedDrawingIds.contains(drawingId),
                likeCountsMap.getOrDefault(drawingId, 0L)
            ))
            .collect(Collectors.toList());

        logger.info("✅ 批量获取点赞状态成功: 处理{}个作品", responses.size());
        return responses;
    }

    /**
     * 获取用户的点赞统计
     * @param userId 用户ID
     * @return [用户点赞数, 用户获得的点赞数]
     */
    @Transactional(readOnly = true)
    public Long[] getLikeCounts(String userId) {
        Long userLikesCount = likeRepository.countByUserId(userId); // 用户点赞了多少作品
        Long receivedLikesCount = likeRepository.countLikesReceivedByUserId(userId); // 用户作品获得多少点赞
        return new Long[]{userLikesCount, receivedLikesCount};
    }
}
