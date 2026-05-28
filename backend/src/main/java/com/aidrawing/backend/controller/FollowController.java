package com.aidrawing.backend.controller;

import com.aidrawing.backend.dto.FollowDto;
import com.aidrawing.backend.service.FollowService;
import com.aidrawing.backend.service.JwtService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/follows")
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:5174", "http://localhost:5175"})
public class FollowController {

    private static final Logger logger = LoggerFactory.getLogger(FollowController.class);

    @Autowired
    private FollowService followService;

    @Autowired
    private JwtService jwtService;

    @PostMapping("/{targetUserId}")
    public ResponseEntity<FollowDto.FollowActionResponse> followUser(
            @PathVariable String targetUserId) {

        logger.info("👥 POST /api/v1/follows/{} - 关注用户", targetUserId);

        try {
            String currentUserId = jwtService.getCurrentUserId();
            FollowDto.FollowActionResponse response = followService.followUser(currentUserId, targetUserId);
            logger.info("✅ 关注成功: {} -> {}", currentUserId, targetUserId);
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException | IllegalStateException e) {
            logger.warn("⚠️ 关注操作失败: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(new FollowDto.FollowActionResponse(false, 0L, e.getMessage()));
        } catch (Exception e) {
            logger.error("❌ 关注操作系统错误", e);
            return ResponseEntity.internalServerError()
                .body(new FollowDto.FollowActionResponse(false, 0L, "系统错误"));
        }
    }

    @DeleteMapping("/{targetUserId}")
    public ResponseEntity<FollowDto.FollowActionResponse> unfollowUser(
            @PathVariable String targetUserId) {

        logger.info("👥 DELETE /api/v1/follows/{} - 取消关注用户", targetUserId);

        try {
            String currentUserId = jwtService.getCurrentUserId();
            FollowDto.FollowActionResponse response = followService.unfollowUser(currentUserId, targetUserId);
            logger.info("✅ 取消关注成功: {} -> {}", currentUserId, targetUserId);
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException | IllegalStateException e) {
            logger.warn("⚠️ 取消关注操作失败: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(new FollowDto.FollowActionResponse(true, 0L, e.getMessage()));
        } catch (Exception e) {
            logger.error("❌ 取消关注操作系统错误", e);
            return ResponseEntity.internalServerError()
                .body(new FollowDto.FollowActionResponse(true, 0L, "系统错误"));
        }
    }

    @GetMapping("/{targetUserId}/status")
    public ResponseEntity<FollowDto.FollowActionResponse> getFollowStatus(
            @PathVariable String targetUserId) {

        logger.info("👥 GET /api/v1/follows/{}/status - 检查关注状态", targetUserId);

        try {
            String currentUserId = jwtService.getCurrentUserId();
            if (currentUserId == null) {
                return ResponseEntity.ok(new FollowDto.FollowActionResponse(false, 0L, "未登录"));
            }

            boolean isFollowing = followService.isFollowing(currentUserId, targetUserId);
            Long[] followCounts = followService.getFollowCounts(targetUserId);
            Long followersCount = followCounts[1];

            logger.info("✅ 关注状态查询成功: {} -> {} = {}", currentUserId, targetUserId, isFollowing);
            return ResponseEntity.ok(new FollowDto.FollowActionResponse(isFollowing, followersCount, "查询成功"));

        } catch (Exception e) {
            logger.error("❌ 关注状态查询系统错误", e);
            return ResponseEntity.internalServerError()
                .body(new FollowDto.FollowActionResponse(false, 0L, "系统错误"));
        }
    }

    @GetMapping("/users/{userId}/following")
    public ResponseEntity<FollowDto.FollowListResponse> getFollowingList(
            @PathVariable String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        logger.info("📋 GET /api/v1/follows/users/{}/following - 获取关注列表: page={}, size={}", userId, page, size);

        try {
            FollowDto.FollowListResponse response = followService.getFollowingList(userId, page, size);
            logger.info("✅ 关注列表获取成功: userId={}, 关注数={}", userId, response.getFollows().size());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("❌ 获取关注列表系统错误: userId={}", userId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/users/{userId}/followers")
    public ResponseEntity<FollowDto.FollowListResponse> getFollowersList(
            @PathVariable String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        logger.info("📋 GET /api/v1/follows/users/{}/followers - 获取粉丝列表: page={}, size={}", userId, page, size);

        try {
            FollowDto.FollowListResponse response = followService.getFollowersList(userId, page, size);
            logger.info("✅ 粉丝列表获取成功: userId={}, 粉丝数={}", userId, response.getFollows().size());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("❌ 获取粉丝列表系统错误: userId={}", userId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/current-user/following")
    public ResponseEntity<FollowDto.FollowListResponse> getCurrentUserFollowingList(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        logger.info("📋 GET /api/v1/follows/current-user/following - 获取当前用户关注列表: page={}, size={}", page, size);

        try {
            String currentUserId = jwtService.getCurrentUserId();
            FollowDto.FollowListResponse response = followService.getFollowingList(currentUserId, page, size);
            logger.info("✅ 当前用户关注列表获取成功: userId={}, 关注数={}", currentUserId, response.getFollows().size());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("❌ 获取当前用户关注列表系统错误", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/current-user/followers")
    public ResponseEntity<FollowDto.FollowListResponse> getCurrentUserFollowersList(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        logger.info("📋 GET /api/v1/follows/current-user/followers - 获取当前用户粉丝列表: page={}, size={}", page, size);

        try {
            String currentUserId = jwtService.getCurrentUserId();
            FollowDto.FollowListResponse response = followService.getFollowersList(currentUserId, page, size);
            logger.info("✅ 当前用户粉丝列表获取成功: userId={}, 粉丝数={}", currentUserId, response.getFollows().size());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("❌ 获取当前用户粉丝列表系统错误", e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
