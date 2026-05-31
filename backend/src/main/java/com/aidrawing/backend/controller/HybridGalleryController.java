package com.aidrawing.backend.controller;

import com.aidrawing.backend.dto.GalleryItemDto;
import com.aidrawing.backend.dto.LikeDto;
import com.aidrawing.backend.entity.Drawing;
import com.aidrawing.backend.repository.DrawingRepository;
import com.aidrawing.backend.service.LikeService;
import com.aidrawing.backend.service.JwtService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/gallery")
public class HybridGalleryController {

    private static final Logger logger = LoggerFactory.getLogger(HybridGalleryController.class);

    private final DrawingRepository drawingRepository;

    @Autowired
    private LikeService likeService;

    @Autowired
    private JwtService jwtService;

    @Autowired
    public HybridGalleryController(DrawingRepository drawingRepository) {
        this.drawingRepository = drawingRepository;
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> getPublicGallery(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String model) {

        logger.info("画廊查询: page={}, size={}, keyword={}, model={}", page, size, keyword, model);

        try {
            PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
            Page<Drawing> drawingPage;

            boolean hasKeyword = keyword != null && !keyword.isBlank();
            boolean hasModel = model != null && !model.isBlank();

            if (hasKeyword && hasModel) {
                drawingPage = drawingRepository.searchByKeywordAndModel(keyword.trim(), model.trim(), pageRequest);
            } else if (hasKeyword) {
                drawingPage = drawingRepository.searchByKeyword(keyword.trim(), pageRequest);
            } else if (hasModel) {
                drawingPage = drawingRepository.findByModelName(model.trim(), pageRequest);
            } else {
                drawingPage = drawingRepository.findBySharedToGalleryTrueWithUserOrderByCreatedAtDesc(pageRequest);
            }

            if (drawingPage.isEmpty()) {
                logger.info("画廊为空");
                return ResponseEntity.ok(Map.of(
                    "content", List.of(),
                    "page", page,
                    "size", size,
                    "totalElements", 0,
                    "totalPages", 0
                ));
            }

            String currentUserId = jwtService.getCurrentUserId();
            List<String> drawingIds = drawingPage.getContent().stream()
                .map(Drawing::getId)
                .collect(Collectors.toList());

            List<LikeDto.LikeStatusResponse> likeStatuses = likeService.getBatchLikeStatus(currentUserId, drawingIds);
            Map<String, LikeDto.LikeStatusResponse> likeStatusMap = likeStatuses.stream()
                .collect(HashMap::new, (m, s) -> m.put(s.getDrawingId(), s), HashMap::putAll);

            List<GalleryItemDto> content = new ArrayList<>();
            for (Drawing drawing : drawingPage.getContent()) {
                GalleryItemDto dto = new GalleryItemDto(drawing);
                LikeDto.LikeStatusResponse likeStatus = likeStatusMap.get(drawing.getId());
                dto.setLikesCount(likeStatus != null ? likeStatus.getLikesCount() : 0L);
                dto.setIsLiked(likeStatus != null && likeStatus.getIsLiked());
                content.add(dto);
            }

            logger.info("返回 {} 张作品 (第 {} 页/共 {} 页)", content.size(), page + 1, drawingPage.getTotalPages());

            return ResponseEntity.ok(Map.of(
                "content", content,
                "page", page,
                "size", size,
                "totalElements", drawingPage.getTotalElements(),
                "totalPages", drawingPage.getTotalPages()
            ));

        } catch (Exception e) {
            logger.warn("数据库查询失败: {}", e.getMessage());
            return ResponseEntity.ok(Map.of(
                "content", List.of(),
                "page", page,
                "size", size,
                "totalElements", 0,
                "totalPages", 0
            ));
        }
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getDatabaseStatus() {
        try {
            long totalDrawings = drawingRepository.count();
            long sharedDrawings = drawingRepository.countBySharedToGalleryTrue();

            return ResponseEntity.ok(Map.of(
                "status", "connected",
                "totalDrawings", totalDrawings,
                "sharedDrawings", sharedDrawings,
                "timestamp", System.currentTimeMillis()
            ));

        } catch (Exception e) {
            logger.error("数据库状态检查失败: {}", e.getMessage());
            return ResponseEntity.ok(Map.of(
                "status", "error",
                "error", e.getMessage(),
                "timestamp", System.currentTimeMillis()
            ));
        }
    }

    @CacheEvict(value = "galleryCache", allEntries = true)
    public void clearGalleryCache() {
        logger.info("画廊缓存已清除");
    }

    @GetMapping("/hot")
    public ResponseEntity<Map<String, Object>> getHotGallery(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        logger.info("热门排行查询: page={}, size={}", page, size);

        try {
            long start = (long) page * size;
            long end = start + size - 1;

            List<String> hotIds = likeService.getHotDrawingIds(start, end);
            Long totalCount = likeService.getHotRankCount();

            if (hotIds.isEmpty()) {
                return ResponseEntity.ok(Map.of(
                    "content", List.of(),
                    "page", page,
                    "size", size,
                    "totalElements", totalCount,
                    "totalPages", (int) Math.ceil((double) totalCount / size)
                ));
            }

            List<Drawing> drawings = drawingRepository.findAllById(hotIds);
            Map<String, Drawing> drawingMap = drawings.stream()
                .collect(Collectors.toMap(Drawing::getId, d -> d));

            List<Drawing> orderedDrawings = hotIds.stream()
                .filter(id -> {
                    Drawing d = drawingMap.get(id);
                    return d != null && d.isSharedToGallery();
                })
                .map(drawingMap::get)
                .collect(Collectors.toList());

            String currentUserId = jwtService.getCurrentUserId();
            List<String> drawingIds = orderedDrawings.stream()
                .map(Drawing::getId)
                .collect(Collectors.toList());

            List<LikeDto.LikeStatusResponse> likeStatuses = likeService.getBatchLikeStatus(currentUserId, drawingIds);
            Map<String, LikeDto.LikeStatusResponse> likeStatusMap = likeStatuses.stream()
                .collect(HashMap::new, (m, s) -> m.put(s.getDrawingId(), s), HashMap::putAll);

            List<GalleryItemDto> content = orderedDrawings.stream().map(drawing -> {
                GalleryItemDto dto = new GalleryItemDto(drawing);
                LikeDto.LikeStatusResponse likeStatus = likeStatusMap.get(drawing.getId());
                dto.setLikesCount(likeStatus != null ? likeStatus.getLikesCount() : 0L);
                dto.setIsLiked(likeStatus != null && likeStatus.getIsLiked());
                return dto;
            }).collect(Collectors.toList());

            logger.info("热门排行返回 {} 张作品", content.size());

            return ResponseEntity.ok(Map.of(
                "content", content,
                "page", page,
                "size", size,
                "totalElements", totalCount,
                "totalPages", (int) Math.ceil((double) totalCount / size)
            ));

        } catch (Exception e) {
            logger.warn("热门排行查询失败: {}", e.getMessage());
            return ResponseEntity.ok(Map.of(
                "content", List.of(),
                "page", page,
                "size", size,
                "totalElements", 0,
                "totalPages", 0
            ));
        }
    }
}
