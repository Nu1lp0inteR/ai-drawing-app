package com.aidrawing.backend.controller;

import com.aidrawing.backend.dto.UserProfileDto;
import com.aidrawing.backend.service.UserProfileService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 用户个人主页控制器
 * User Profile Controller - 用于展示其他用户的公开信息和作品
 */
@RestController
@RequestMapping("/api/v1/users")
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:5174", "http://localhost:5175"})
public class UserProfileController {

    private static final Logger logger = LoggerFactory.getLogger(UserProfileController.class);

    @Autowired
    private UserProfileService userProfileService;

    /**
     * 获取用户公开个人主页信息
     * @param userId 用户ID
     * @return 用户公开信息和作品
     */
    @GetMapping("/{userId}/profile")
    public ResponseEntity<UserProfileDto> getUserProfile(@PathVariable String userId) {
        logger.info("🔍 GET /api/v1/users/{}/profile - 获取用户公开主页", userId);
        
        try {
            UserProfileDto userProfile = userProfileService.getUserProfile(userId);
            logger.info("✅ 用户主页数据获取成功: userId={}, username={}", 
                userId, userProfile.getUsername());
            return ResponseEntity.ok(userProfile);
            
        } catch (RuntimeException e) {
            logger.error("❌ 获取用户主页失败: userId={}, error={}", userId, e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            logger.error("❌ 获取用户主页系统错误: userId={}", userId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 获取用户公开作品列表（分页）
     * @param userId 用户ID
     * @param page 页码
     * @param size 每页大小
     * @return 用户公开作品列表
     */
    @GetMapping("/{userId}/artworks")
    public ResponseEntity<?> getUserArtworks(
            @PathVariable String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        logger.info("🎨 GET /api/v1/users/{}/artworks - 获取用户公开作品: page={}, size={}", 
            userId, page, size);
        
        try {
            var artworks = userProfileService.getUserPublicArtworks(userId, page, size);
            logger.info("✅ 用户公开作品获取成功: userId={}, 作品数={}", 
                userId, artworks.getArtworks().size());
            return ResponseEntity.ok(artworks);
            
        } catch (RuntimeException e) {
            logger.error("❌ 获取用户公开作品失败: userId={}, error={}", userId, e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            logger.error("❌ 获取用户公开作品系统错误: userId={}", userId, e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
