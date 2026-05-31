package com.aidrawing.backend.service;

import com.aidrawing.backend.dto.CommentDto;
import com.aidrawing.backend.entity.Comment;
import com.aidrawing.backend.entity.Drawing;
import com.aidrawing.backend.entity.User;
import com.aidrawing.backend.repository.CommentRepository;
import com.aidrawing.backend.repository.DrawingRepository;
import com.aidrawing.backend.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class CommentService {

    private static final Logger logger = LoggerFactory.getLogger(CommentService.class);

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private DrawingRepository drawingRepository;

    @Autowired
    private UserRepository userRepository;

    public CommentDto.CommentInfo createComment(String userId, CommentDto.CommentCreateRequest request) {
        logger.info("创建评论: userId={}, drawingId={}", userId, request.getDrawingId());

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("用户不存在"));
        Drawing drawing = drawingRepository.findById(request.getDrawingId())
            .orElseThrow(() -> new RuntimeException("作品不存在"));

        Comment comment = new Comment(drawing, user, request.getContent());

        if (request.getParentCommentId() != null) {
            Comment parent = commentRepository.findById(request.getParentCommentId())
                .orElseThrow(() -> new RuntimeException("父评论不存在"));
            comment.setParentComment(parent);
        }

        Comment saved = commentRepository.save(comment);
        logger.info("评论创建成功: commentId={}", saved.getId());
        return new CommentDto.CommentInfo(saved);
    }

    @Transactional(readOnly = true)
    public CommentDto.CommentListResponse getDrawingComments(String drawingId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Comment> commentsPage = commentRepository.findTopLevelByDrawingId(drawingId, pageable);

        List<CommentDto.CommentInfo> infos = commentsPage.getContent().stream()
            .map(CommentDto.CommentInfo::new)
            .collect(Collectors.toList());

        long totalCount = commentRepository.countByDrawingId(drawingId);

        CommentDto.CommentListResponse response = new CommentDto.CommentListResponse();
        response.setComments(infos);
        response.setTotalCount(totalCount);
        response.setCurrentPage(page + 1);
        response.setPageSize(size);
        response.setTotalPages(commentsPage.getTotalPages());
        return response;
    }

    public void deleteComment(String userId, String commentId) {
        Comment comment = commentRepository.findById(commentId)
            .orElseThrow(() -> new RuntimeException("评论不存在"));

        if (!comment.getUser().getId().equals(userId)) {
            throw new RuntimeException("无权删除此评论");
        }

        commentRepository.delete(comment);
        logger.info("评论删除成功: commentId={}", commentId);
    }
}
