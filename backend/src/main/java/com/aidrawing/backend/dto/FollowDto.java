package com.aidrawing.backend.dto;

import com.aidrawing.backend.entity.Follow;
import com.aidrawing.backend.entity.User;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 关注系统相关的数据传输对象
 * Follow System Data Transfer Objects
 */
public class FollowDto {

    /**
     * 用户基本信息（用于关注列表）
     */
    public static class UserBasicInfo {
        private String id;
        private String username;
        
        @JsonProperty("joinDate")
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
        private LocalDateTime joinDate;
        
        @JsonProperty("totalArtworks")
        private Long totalArtworks;
        
        @JsonProperty("followingCount")
        private Long followingCount;
        
        @JsonProperty("followersCount")
        private Long followersCount;

        // 默认构造函数
        public UserBasicInfo() {}

        // 从User实体构造
        public UserBasicInfo(User user) {
            this.id = user.getId();
            this.username = user.getUsername();
            this.joinDate = user.getCreatedAt();
            this.totalArtworks = 0L; // 需要额外查询
            this.followingCount = 0L; // 需要额外查询
            this.followersCount = 0L; // 需要额外查询
        }

        // Getters and Setters
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public LocalDateTime getJoinDate() { return joinDate; }
        public void setJoinDate(LocalDateTime joinDate) { this.joinDate = joinDate; }
        public Long getTotalArtworks() { return totalArtworks; }
        public void setTotalArtworks(Long totalArtworks) { this.totalArtworks = totalArtworks; }
        public Long getFollowingCount() { return followingCount; }
        public void setFollowingCount(Long followingCount) { this.followingCount = followingCount; }
        public Long getFollowersCount() { return followersCount; }
        public void setFollowersCount(Long followersCount) { this.followersCount = followersCount; }
    }

    /**
     * 关注关系信息
     */
    public static class FollowInfo {
        private String id;
        
        @JsonProperty("userInfo")
        private UserBasicInfo userInfo;
        
        @JsonProperty("followedAt")
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
        private LocalDateTime followedAt;

        // 默认构造函数
        public FollowInfo() {}

        // 从Follow实体构造（关注列表）
        public FollowInfo(Follow follow, boolean isFollowing) {
            this.id = follow.getId();
            this.followedAt = follow.getCreatedAt();
            if (isFollowing) {
                // 获取被关注的用户信息
                this.userInfo = new UserBasicInfo(follow.getFollowing());
            } else {
                // 获取关注者的用户信息
                this.userInfo = new UserBasicInfo(follow.getFollower());
            }
        }

        // Getters and Setters
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public UserBasicInfo getUserInfo() { return userInfo; }
        public void setUserInfo(UserBasicInfo userInfo) { this.userInfo = userInfo; }
        public LocalDateTime getFollowedAt() { return followedAt; }
        public void setFollowedAt(LocalDateTime followedAt) { this.followedAt = followedAt; }
    }

    /**
     * 关注列表响应
     */
    public static class FollowListResponse {
        private List<FollowInfo> follows;
        
        @JsonProperty("totalCount")
        private Long totalCount;
        
        @JsonProperty("currentPage")
        private Integer currentPage;
        
        @JsonProperty("pageSize")
        private Integer pageSize;
        
        @JsonProperty("totalPages")
        private Integer totalPages;

        // 默认构造函数
        public FollowListResponse() {}

        // Getters and Setters
        public List<FollowInfo> getFollows() { return follows; }
        public void setFollows(List<FollowInfo> follows) { this.follows = follows; }
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
     * 关注/取消关注响应
     */
    public static class FollowActionResponse {
        @JsonProperty("isFollowing")
        private Boolean isFollowing;
        
        @JsonProperty("followersCount")
        private Long followersCount;
        
        @JsonProperty("message")
        private String message;

        // 默认构造函数
        public FollowActionResponse() {}

        public FollowActionResponse(Boolean isFollowing, Long followersCount, String message) {
            this.isFollowing = isFollowing;
            this.followersCount = followersCount;
            this.message = message;
        }

        // Getters and Setters
        public Boolean getIsFollowing() { return isFollowing; }
        public void setIsFollowing(Boolean isFollowing) { this.isFollowing = isFollowing; }
        public Long getFollowersCount() { return followersCount; }
        public void setFollowersCount(Long followersCount) { this.followersCount = followersCount; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
    }
}
