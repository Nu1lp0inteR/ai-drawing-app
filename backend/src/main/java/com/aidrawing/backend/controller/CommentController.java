package com.aidrawing.backend.controller;

import com.aidrawing.backend.dto.CommentDto;
import com.aidrawing.backend.service.CommentService;
import com.aidrawing.backend.service.JwtService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/comments")
public class CommentController {

    private static final Logger logger = LoggerFactory.getLogger(CommentController.class);

    @Autowired
    private CommentService commentService;

    @Autowired
    private JwtService jwtService;

    @PostMapping
    public ResponseEntity<?> createComment(@RequestBody CommentDto.CommentCreateRequest request) {
        String userId = jwtService.getCurrentUserId();
        if (userId == null) {
            return ResponseEntity.status(401).body(Map.of("message", "请先登录"));
        }

        try {
            CommentDto.CommentInfo comment = commentService.createComment(userId, request);
            return ResponseEntity.ok(comment);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/drawings/{drawingId}")
    public ResponseEntity<?> getDrawingComments(
            @PathVariable String drawingId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        try {
            CommentDto.CommentListResponse response = commentService.getDrawingComments(drawingId, page, size);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("获取评论失败: drawingId={}", drawingId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @DeleteMapping("/{commentId}")
    public ResponseEntity<?> deleteComment(@PathVariable String commentId) {
        String userId = jwtService.getCurrentUserId();
        if (userId == null) {
            return ResponseEntity.status(401).body(Map.of("message", "请先登录"));
        }

        try {
            commentService.deleteComment(userId, commentId);
            return ResponseEntity.ok(Map.of("message", "评论已删除"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
}
