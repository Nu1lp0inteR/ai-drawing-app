package com.aidrawing.backend.dto;

import com.aidrawing.backend.entity.Like;
import com.aidrawing.backend.entity.User;
import com.aidrawing.backend.entity.Drawing;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 点赞系统相关的数据传输对象
 * Like System Data Transfer Objects
 */
public class LikeDto {

    /**
     * 用户基本信息（用于点赞列表）
     */
    public static class UserBasicInfo {
        private String id;
        private String username;
        
        @JsonProperty("joinDate")
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
        private LocalDateTime joinDate;

        // 默认构造函数
        public UserBasicInfo() {}

        // 从User实体构造
        public UserBasicInfo(User user) {
            this.id = user.getId();
            this.username = user.getUsername();
            this.joinDate = user.getCreatedAt();
        }

        // Getters and Setters
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public LocalDateTime getJoinDate() { return joinDate; }
        public void setJoinDate(LocalDateTime joinDate) { this.joinDate = joinDate; }
    }

    /**
     * 作品基本信息（用于点赞列表）
     */
    public static class DrawingBasicInfo {
        private String id;
        private String prompt;
        private String storedFilename;
        
        @JsonProperty("createdAt")
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
        private LocalDateTime createdAt;

        // 默认构造函数
        public DrawingBasicInfo() {}

        // 从Drawing实体构造
        public DrawingBasicInfo(Drawing drawing) {
            this.id = drawing.getId();
            this.prompt = drawing.getPrompt();
            this.storedFilename = drawing.getStoredFilename();
            this.createdAt = drawing.getCreatedAt();
        }

        // Getters and Setters
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getPrompt() { return prompt; }
        public void setPrompt(String prompt) { this.prompt = prompt; }
        public String getStoredFilename() { return storedFilename; }
        public void setStoredFilename(String storedFilename) { this.storedFilename = storedFilename; }
        public LocalDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    }

    /**
     * 点赞关系信息
     */
    public static class LikeInfo {
        private String id;
        
        @JsonProperty("userInfo")
        private UserBasicInfo userInfo;
        
        @JsonProperty("drawingInfo")
        private DrawingBasicInfo drawingInfo;
        
        @JsonProperty("likedAt")
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
        private LocalDateTime likedAt;

        // 默认构造函数
        public LikeInfo() {}

        // 从Like实体构造（用于获取作品的点赞用户列表）
        public LikeInfo(Like like, boolean includeDrawing) {
            this.id = like.getId();
            this.likedAt = like.getCreatedAt();
            this.userInfo = new UserBasicInfo(like.getUser());
            if (includeDrawing && like.getDrawing() != null) {
                this.drawingInfo = new DrawingBasicInfo(like.getDrawing());
            }
        }

        // Getters and Setters
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public UserBasicInfo getUserInfo() { return userInfo; }
        public void setUserInfo(UserBasicInfo userInfo) { this.userInfo = userInfo; }
        public DrawingBasicInfo getDrawingInfo() { return drawingInfo; }
        public void setDrawingInfo(DrawingBasicInfo drawingInfo) { this.drawingInfo = drawingInfo; }
        public LocalDateTime getLikedAt() { return likedAt; }
        public void setLikedAt(LocalDateTime likedAt) { this.likedAt = likedAt; }
    }

    /**
     * 点赞列表响应
     */
    public static class LikeListResponse {
        private List<LikeInfo> likes;
        
        @JsonProperty("totalCount")
        private Long totalCount;
        
        @JsonProperty("currentPage")
        private Integer currentPage;
        
        @JsonProperty("pageSize")
        private Integer pageSize;
        
        @JsonProperty("totalPages")
        private Integer totalPages;

        // 默认构造函数
        public LikeListResponse() {}

        // Getters and Setters
        public List<LikeInfo> getLikes() { return likes; }
        public void setLikes(List<LikeInfo> likes) { this.likes = likes; }
        public Long getTotalCount() { return totalCount; }
        public void setTotalCount(Long totalCount) { this.totalCount = totalCount; }
        public Integer getCurrentPage() { return currentPage; }
        public void setCurrentPage(Integer currentPage) { this.currentPage = currentPage; }
        public Integer getPageSize() { return pageSize; }
        public void setPageSize(Integer pageSize) { this.pageSize = pageSize; }
        public Integer getTotalPages() { return totalPages; }
        public void setTotalPages(Integer totalPages) { this.totalPages = totalPages; }
    }

    /**
     * 点赞/取消点赞响应
     */
    public static class LikeActionResponse {
        @JsonProperty("isLiked")
        private Boolean isLiked;
        
        @JsonProperty("likesCount")
        private Long likesCount;
        
        @JsonProperty("message")
        private String message;

        // 默认构造函数
        public LikeActionResponse() {}

        public LikeActionResponse(Boolean isLiked, Long likesCount, String message) {
            this.isLiked = isLiked;
            this.likesCount = likesCount;
            this.message = message;
        }

        // Getters and Setters
        public Boolean getIsLiked() { return isLiked; }
        public void setIsLiked(Boolean isLiked) { this.isLiked = isLiked; }
        public Long getLikesCount() { return likesCount; }
        public void setLikesCount(Long likesCount) { this.likesCount = likesCount; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
    }

    /**
     * 点赞状态响应（用于批量查询）
     */
    public static class LikeStatusResponse {
        @JsonProperty("drawingId")
        private String drawingId;
        
        @JsonProperty("isLiked")
        private Boolean isLiked;
        
        @JsonProperty("likesCount")
        private Long likesCount;

        // 默认构造函数
        public LikeStatusResponse() {}

        public LikeStatusResponse(String drawingId, Boolean isLiked, Long likesCount) {
            this.drawingId = drawingId;
            this.isLiked = isLiked;
            this.likesCount = likesCount;
        }

        // Getters and Setters
        public String getDrawingId() { return drawingId; }
        public void setDrawingId(String drawingId) { this.drawingId = drawingId; }
        public Boolean getIsLiked() { return isLiked; }
        public void setIsLiked(Boolean isLiked) { this.isLiked = isLiked; }
        public Long getLikesCount() { return likesCount; }
        public void setLikesCount(Long likesCount) { this.likesCount = likesCount; }
    }
}
