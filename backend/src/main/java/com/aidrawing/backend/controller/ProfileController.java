package com.aidrawing.backend.controller;

import com.aidrawing.backend.dto.ProfileDto;
import com.aidrawing.backend.service.JwtService;
import com.aidrawing.backend.service.ProfileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * 个人中心控制器
 * 提供用户作品管理、统计信息等API
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;
    private final JwtService jwtService;

    @GetMapping("/home")
    public ResponseEntity<ProfileDto.ProfileHomeData> getProfileHome(
            Authentication authentication) {
        log.info("🏠 GET /api/v1/profile/home - 获取个人中心主页: user={}", authentication.getName());
        
        try {
            String userId = jwtService.getCurrentUserId();
            if (userId == null) {
                log.warn("⚠️ 无法提取用户ID");
                return ResponseEntity.badRequest().build();
            }
            
            ProfileDto.ProfileHomeData homeData = profileService.getProfileHomeData(userId);
            
            log.info("✅ GET /api/v1/profile/home - 个人中心主页数据获取成功: userId={}", userId);
            return ResponseEntity.ok(homeData);
            
        } catch (Exception e) {
            log.error("❌ GET /api/v1/profile/home - 获取个人中心主页失败: user={}", 
                authentication.getName(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 分页获取用户作品列表
     */
    @GetMapping("/artworks")
    public ResponseEntity<ProfileDto.UserArtworkListResponse> getUserArtworks(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        log.info("🎨 GET /api/v1/profile/artworks - 获取用户作品列表: user={}, page={}, size={}", 
            authentication.getName(), page, size);
        
        try {
            String userId = jwtService.getCurrentUserId();
            if (userId == null) {
                log.warn("⚠️ 无法提取用户ID");
                return ResponseEntity.badRequest().build();
            }
            
            ProfileDto.UserArtworkListResponse response = profileService.getUserArtworks(userId, page, size);
            
            log.info("✅ GET /api/v1/profile/artworks - 用户作品列表获取成功: userId={}, 总数={}", 
                userId, response.getTotalCount());
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("❌ GET /api/v1/profile/artworks - 获取用户作品列表失败: user={}", 
                authentication.getName(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 获取用户统计信息
     */
    @GetMapping("/stats")
    public ResponseEntity<ProfileDto.UserStats> getUserStats(
            Authentication authentication) {
        log.info("📊 GET /api/v1/profile/stats - 获取用户统计: user={}", authentication.getName());
        
        try {
            String userId = jwtService.getCurrentUserId();
            if (userId == null) {
                log.warn("⚠️ 无法提取用户ID");
                return ResponseEntity.badRequest().build();
            }
            
            ProfileDto.UserStats stats = profileService.getUserStats(userId);
            
            log.info("✅ GET /api/v1/profile/stats - 用户统计获取成功: userId={}", userId);
            return ResponseEntity.ok(stats);
            
        } catch (Exception e) {
            log.error("❌ GET /api/v1/profile/stats - 获取用户统计失败: user={}", 
                authentication.getName(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 删除用户作品
     */
    @DeleteMapping("/artworks/{drawingId}")
    public ResponseEntity<Void> deleteArtwork(
            Authentication authentication,
            @PathVariable String drawingId) {
        
        log.info("🗑️ DELETE /api/v1/profile/artworks/{} - 删除作品: user={}", 
            drawingId, authentication.getName());
        
        try {
            String userId = jwtService.getCurrentUserId();
            if (userId == null) {
                log.warn("⚠️ 无法提取用户ID");
                return ResponseEntity.badRequest().build();
            }
            
            profileService.deleteUserArtwork(userId, drawingId);
            
            log.info("✅ DELETE /api/v1/profile/artworks/{} - 作品删除成功: userId={}", 
                drawingId, userId);
            return ResponseEntity.ok().build();
            
        } catch (RuntimeException e) {
            log.warn("⚠️ DELETE /api/v1/profile/artworks/{} - 删除失败: user={}, reason={}", 
                drawingId, authentication.getName(), e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("❌ DELETE /api/v1/profile/artworks/{} - 删除作品失败: user={}", 
                drawingId, authentication.getName(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 切换作品分享状态
     */
    @PutMapping("/artworks/{drawingId}/sharing")
    public ResponseEntity<Void> toggleArtworkSharing(
            Authentication authentication,
            @PathVariable String drawingId,
            @RequestParam boolean shareToGallery) {
        
        log.info("🔄 PUT /api/v1/profile/artworks/{}/sharing - 切换分享状态: user={}, shareToGallery={}", 
            drawingId, authentication.getName(), shareToGallery);
        
        try {
            String userId = jwtService.getCurrentUserId();
            if (userId == null) {
                log.warn("⚠️ 无法提取用户ID");
                return ResponseEntity.badRequest().build();
            }
            
            profileService.toggleArtworkSharing(userId, drawingId, shareToGallery);
            
            log.info("✅ PUT /api/v1/profile/artworks/{}/sharing - 分享状态更新成功: userId={}, 新状态={}", 
                drawingId, userId, shareToGallery);
            return ResponseEntity.ok().build();
            
        } catch (RuntimeException e) {
            log.warn("⚠️ PUT /api/v1/profile/artworks/{}/sharing - 更新失败: user={}, reason={}", 
                drawingId, authentication.getName(), e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("❌ PUT /api/v1/profile/artworks/{}/sharing - 切换分享状态失败: user={}", 
                drawingId, authentication.getName(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
