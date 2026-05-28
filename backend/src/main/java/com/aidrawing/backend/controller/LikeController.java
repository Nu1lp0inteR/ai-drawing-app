package com.aidrawing.backend.controller;

import com.aidrawing.backend.dto.LikeDto;
import com.aidrawing.backend.service.LikeService;
import com.aidrawing.backend.service.JwtService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/likes")
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:5174", "http://localhost:5175"})
public class LikeController {

    private static final Logger logger = LoggerFactory.getLogger(LikeController.class);

    @Autowired
    private LikeService likeService;

    @Autowired
    private JwtService jwtService;

    @PostMapping("/drawings/{drawingId}")
    public ResponseEntity<LikeDto.LikeActionResponse> likeDrawing(
            @PathVariable String drawingId) {

        logger.info("👍 POST /api/v1/likes/drawings/{} - 点赞作品", drawingId);

        try {
            String currentUserId = jwtService.getCurrentUserId();
            LikeDto.LikeActionResponse response = likeService.likeDrawing(currentUserId, drawingId);
            logger.info("✅ 点赞成功: userId={}, drawingId={}", currentUserId, drawingId);
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException | IllegalStateException e) {
            logger.warn("⚠️ 点赞操作失败: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(new LikeDto.LikeActionResponse(false, 0L, e.getMessage()));
        } catch (Exception e) {
            logger.error("❌ 点赞操作系统错误", e);
            return ResponseEntity.internalServerError()
                .body(new LikeDto.LikeActionResponse(false, 0L, "系统错误"));
        }
    }

    @DeleteMapping("/drawings/{drawingId}")
    public ResponseEntity<LikeDto.LikeActionResponse> unlikeDrawing(
            @PathVariable String drawingId) {

        logger.info("👍 DELETE /api/v1/likes/drawings/{} - 取消点赞作品", drawingId);

        try {
            String currentUserId = jwtService.getCurrentUserId();
            LikeDto.LikeActionResponse response = likeService.unlikeDrawing(currentUserId, drawingId);
            logger.info("✅ 取消点赞成功: userId={}, drawingId={}", currentUserId, drawingId);
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException | IllegalStateException e) {
            logger.warn("⚠️ 取消点赞操作失败: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(new LikeDto.LikeActionResponse(true, 0L, e.getMessage()));
        } catch (Exception e) {
            logger.error("❌ 取消点赞操作系统错误", e);
            return ResponseEntity.internalServerError()
                .body(new LikeDto.LikeActionResponse(true, 0L, "系统错误"));
        }
    }

    @GetMapping("/drawings/{drawingId}/status")
    public ResponseEntity<LikeDto.LikeActionResponse> getLikeStatus(
            @PathVariable String drawingId) {

        logger.info("👍 GET /api/v1/likes/drawings/{}/status - 检查点赞状态", drawingId);

        try {
            String currentUserId = jwtService.getCurrentUserId();
            boolean isLiked = false;
            if (currentUserId != null) {
                isLiked = likeService.isLiked(currentUserId, drawingId);
            }

            Long likesCount = likeService.getDrawingLikesCount(drawingId);

            logger.info("✅ 点赞状态查询成功: drawingId={}, isLiked={}, likesCount={}",
                drawingId, isLiked, likesCount);
            return ResponseEntity.ok(new LikeDto.LikeActionResponse(isLiked, likesCount, "查询成功"));

        } catch (Exception e) {
            logger.error("❌ 点赞状态查询系统错误", e);
            return ResponseEntity.internalServerError()
                .body(new LikeDto.LikeActionResponse(false, 0L, "系统错误"));
        }
    }

    @GetMapping("/drawings/{drawingId}")
    public ResponseEntity<LikeDto.LikeListResponse> getDrawingLikes(
            @PathVariable String drawingId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        logger.info("📋 GET /api/v1/likes/drawings/{} - 获取作品点赞列表: page={}, size={}",
            drawingId, page, size);

        try {
            LikeDto.LikeListResponse response = likeService.getDrawingLikes(drawingId, page, size);
            logger.info("✅ 作品点赞列表获取成功: drawingId={}, 点赞数={}",
                drawingId, response.getLikes().size());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("❌ 获取作品点赞列表系统错误: drawingId={}", drawingId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/users/{userId}")
    public ResponseEntity<LikeDto.LikeListResponse> getUserLikes(
            @PathVariable String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        logger.info("📋 GET /api/v1/likes/users/{} - 获取用户点赞列表: page={}, size={}",
            userId, page, size);

        try {
            LikeDto.LikeListResponse response = likeService.getUserLikes(userId, page, size);
            logger.info("✅ 用户点赞列表获取成功: userId={}, 点赞数={}",
                userId, response.getLikes().size());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("❌ 获取用户点赞列表系统错误: userId={}", userId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/status/batch")
    public ResponseEntity<List<LikeDto.LikeStatusResponse>> getBatchLikeStatus(
            @RequestBody List<String> drawingIds) {

        logger.info("📊 POST /api/v1/likes/status/batch - 批量获取点赞状态: 作品数={}", drawingIds.size());

        try {
            String currentUserId = jwtService.getCurrentUserId();
            List<LikeDto.LikeStatusResponse> responses = likeService.getBatchLikeStatus(currentUserId, drawingIds);
            logger.info("✅ 批量获取点赞状态成功: userId={}, 处理{}个作品",
                currentUserId, responses.size());
            return ResponseEntity.ok(responses);

        } catch (Exception e) {
            logger.error("❌ 批量获取点赞状态系统错误", e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
