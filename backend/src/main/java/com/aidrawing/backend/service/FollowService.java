package com.aidrawing.backend.service;

import com.aidrawing.backend.dto.FollowDto;
import com.aidrawing.backend.entity.Follow;
import com.aidrawing.backend.entity.User;
import com.aidrawing.backend.repository.FollowRepository;
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
 * 关注系统服务
 * Follow System Service
 */
@Service
@Transactional
public class FollowService {

    private static final Logger logger = LoggerFactory.getLogger(FollowService.class);

    @Autowired
    private FollowRepository followRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DrawingRepository drawingRepository;

    /**
     * 关注用户
     * @param followerId 关注者ID
     * @param followingId 被关注者ID
     * @return 关注操作结果
     */
    public FollowDto.FollowActionResponse followUser(String followerId, String followingId) {
        logger.info("👥 用户关注操作: {} -> {}", followerId, followingId);

        // 1. 验证用户
        if (followerId.equals(followingId)) {
            throw new IllegalArgumentException("不能关注自己");
        }

        User follower = userRepository.findById(followerId)
            .orElseThrow(() -> new RuntimeException("关注者用户不存在"));
        User following = userRepository.findById(followingId)
            .orElseThrow(() -> new RuntimeException("被关注用户不存在"));

        // 2. 检查是否已经关注
        if (followRepository.existsByFollowerIdAndFollowingId(followerId, followingId)) {
            throw new IllegalStateException("已经关注了该用户");
        }

        // 3. 创建关注关系
        Follow follow = new Follow(follower, following);
        followRepository.save(follow);

        // 4. 获取最新的粉丝数
        Long followersCount = followRepository.countByFollowingId(followingId);

        logger.info("✅ 关注成功: {} -> {}, 被关注者粉丝数: {}", 
            follower.getUsername(), following.getUsername(), followersCount);

        return new FollowDto.FollowActionResponse(true, followersCount, "关注成功");
    }

    /**
     * 取消关注用户
     * @param followerId 关注者ID
     * @param followingId 被关注者ID
     * @return 取消关注操作结果
     */
    public FollowDto.FollowActionResponse unfollowUser(String followerId, String followingId) {
        logger.info("👥 取消关注操作: {} -> {}", followerId, followingId);

        // 1. 验证用户存在
        if (!userRepository.existsById(followerId)) {
            throw new RuntimeException("关注者用户不存在");
        }
        if (!userRepository.existsById(followingId)) {
            throw new RuntimeException("被关注用户不存在");
        }

        // 2. 检查关注关系是否存在
        if (!followRepository.existsByFollowerIdAndFollowingId(followerId, followingId)) {
            throw new IllegalStateException("未关注该用户");
        }

        // 3. 删除关注关系
        followRepository.deleteByFollowerIdAndFollowingId(followerId, followingId);

        // 4. 获取最新的粉丝数
        Long followersCount = followRepository.countByFollowingId(followingId);

        logger.info("✅ 取消关注成功: {} -> {}, 被关注者粉丝数: {}", 
            followerId, followingId, followersCount);

        return new FollowDto.FollowActionResponse(false, followersCount, "已取消关注");
    }

    /**
     * 检查是否关注了某用户
     * @param followerId 关注者ID
     * @param followingId 被关注者ID
     * @return 是否关注
     */
    @Transactional(readOnly = true)
    public boolean isFollowing(String followerId, String followingId) {
        if (followerId == null || followingId == null) {
            return false;
        }
        return followRepository.existsByFollowerIdAndFollowingId(followerId, followingId);
    }

    /**
     * 获取用户的关注列表
     * @param userId 用户ID
     * @param page 页码
     * @param size 每页大小
     * @return 关注列表
     */
    @Transactional(readOnly = true)
    public FollowDto.FollowListResponse getFollowingList(String userId, int page, int size) {
        logger.info("📋 获取用户关注列表: userId={}, page={}, size={}", userId, page, size);

        Pageable pageable = PageRequest.of(page, size);
        Page<Follow> followsPage = followRepository.findFollowingsByFollowerId(userId, pageable);

        List<FollowDto.FollowInfo> followInfos = followsPage.getContent().stream()
            .map(follow -> {
                FollowDto.FollowInfo info = new FollowDto.FollowInfo(follow, true);
                enrichUserInfo(info.getUserInfo());
                return info;
            })
            .collect(Collectors.toList());

        FollowDto.FollowListResponse response = new FollowDto.FollowListResponse();
        response.setFollows(followInfos);
        response.setTotalCount(followsPage.getTotalElements());
        response.setCurrentPage(page + 1);
        response.setPageSize(size);
        response.setTotalPages(followsPage.getTotalPages());

        logger.info("✅ 获取关注列表成功: userId={}, 关注数={}", userId, followInfos.size());
        return response;
    }

    /**
     * 获取用户的粉丝列表
     * @param userId 用户ID
     * @param page 页码
     * @param size 每页大小
     * @return 粉丝列表
     */
    @Transactional(readOnly = true)
    public FollowDto.FollowListResponse getFollowersList(String userId, int page, int size) {
        logger.info("📋 获取用户粉丝列表: userId={}, page={}, size={}", userId, page, size);

        Pageable pageable = PageRequest.of(page, size);
        Page<Follow> followsPage = followRepository.findFollowersByFollowingId(userId, pageable);

        List<FollowDto.FollowInfo> followInfos = followsPage.getContent().stream()
            .map(follow -> {
                // 检查是否已回关该粉丝（即粉丝是否也被当前用户关注）
                boolean isFollowedBack = followRepository.existsByFollowerIdAndFollowingId(userId, follow.getFollower().getId());
                FollowDto.FollowInfo info = new FollowDto.FollowInfo(follow, isFollowedBack);
                enrichUserInfo(info.getUserInfo());
                return info;
            })
            .collect(Collectors.toList());

        FollowDto.FollowListResponse response = new FollowDto.FollowListResponse();
        response.setFollows(followInfos);
        response.setTotalCount(followsPage.getTotalElements());
        response.setCurrentPage(page + 1);
        response.setPageSize(size);
        response.setTotalPages(followsPage.getTotalPages());

        logger.info("✅ 获取粉丝列表成功: userId={}, 粉丝数={}", userId, followInfos.size());
        return response;
    }

    /**
     * 获取用户的关注和粉丝统计
     * @param userId 用户ID
     * @return [关注数, 粉丝数]
     */
    @Transactional(readOnly = true)
    public Long[] getFollowCounts(String userId) {
        Long followingCount = followRepository.countByFollowerId(userId);
        Long followersCount = followRepository.countByFollowingId(userId);
        return new Long[]{followingCount, followersCount};
    }

    /**
     * 丰富用户信息（添加统计数据）
     */
    private void enrichUserInfo(FollowDto.UserBasicInfo userInfo) {
        String userId = userInfo.getId();
        
        // 获取作品数
        Long artworkCount = drawingRepository.countByUserId(userId);
        userInfo.setTotalArtworks(artworkCount);
        
        // 获取关注统计
        Long[] followCounts = getFollowCounts(userId);
        userInfo.setFollowingCount(followCounts[0]);
        userInfo.setFollowersCount(followCounts[1]);
    }
}
