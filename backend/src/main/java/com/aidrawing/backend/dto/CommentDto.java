package com.aidrawing.backend.dto;

import com.aidrawing.backend.entity.Comment;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.util.List;

public class CommentDto {

    public static class CommentCreateRequest {
        @JsonProperty("drawing_id")
        private String drawingId;
        private String content;
        @JsonProperty("parent_comment_id")
        private String parentCommentId;

        public String getDrawingId() { return drawingId; }
        public void setDrawingId(String drawingId) { this.drawingId = drawingId; }

        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }

        public String getParentCommentId() { return parentCommentId; }
        public void setParentCommentId(String parentCommentId) { this.parentCommentId = parentCommentId; }
    }

    public static class CommentInfo {
        private String id;
        private String content;
        @JsonProperty("drawing_id")
        private String drawingId;
        @JsonProperty("user_id")
        private String userId;
        @JsonProperty("username")
        private String username;
        @JsonProperty("parent_comment_id")
        private String parentCommentId;
        @JsonProperty("created_at")
        private LocalDateTime createdAt;

        public CommentInfo() {}

        public CommentInfo(Comment comment) {
            this.id = comment.getId();
            this.content = comment.getContent();
            this.drawingId = comment.getDrawing().getId();
            this.userId = comment.getUser().getId();
            this.username = comment.getUser().getUsername();
            this.parentCommentId = comment.getParentComment() != null
                ? comment.getParentComment().getId() : null;
            this.createdAt = comment.getCreatedAt();
        }

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }

        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }

        public String getDrawingId() { return drawingId; }
        public void setDrawingId(String drawingId) { this.drawingId = drawingId; }

        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }

        public String getParentCommentId() { return parentCommentId; }
        public void setParentCommentId(String parentCommentId) { this.parentCommentId = parentCommentId; }

        public LocalDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    }

    public static class CommentListResponse {
        private List<CommentInfo> comments;
        @JsonProperty("total_count")
        private long totalCount;
        @JsonProperty("current_page")
        private int currentPage;
        @JsonProperty("page_size")
        private int pageSize;
        @JsonProperty("total_pages")
        private int totalPages;

        public List<CommentInfo> getComments() { return comments; }
        public void setComments(List<CommentInfo> comments) { this.comments = comments; }

        public long getTotalCount() { return totalCount; }
        public void setTotalCount(long totalCount) { this.totalCount = totalCount; }

        public int getCurrentPage() { return currentPage; }
        public void setCurrentPage(int currentPage) { this.currentPage = currentPage; }

        public int getPageSize() { return pageSize; }
        public void setPageSize(int pageSize) { this.pageSize = pageSize; }

        public int getTotalPages() { return totalPages; }
        public void setTotalPages(int totalPages) { this.totalPages = totalPages; }
    }
}
