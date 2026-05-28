package com.aidrawing.backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 个人中心相关的数据传输对象
 * 用于用户作品管理和个人信息展示
 */
public class ProfileDto {

    /**
     * 用户个人作品项
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserArtworkItem {
        private String id;
        
        private String prompt;
        
        @JsonProperty("negativePrompt")
        private String negativePrompt;
        
        private Integer steps;
        
        private Double cfg;
        
        @JsonProperty("samplerName")
        private String samplerName;
        
        private String seed;
        
        @JsonProperty("storedFilename")
        private String storedFilename;
        
        @JsonProperty("originalFilename")
        private String originalFilename;
        
        @JsonProperty("fileType")
        private String fileType;
        
        @JsonProperty("sharedToGallery")
        private Boolean sharedToGallery;
        
        @JsonProperty("createdAt")
        private LocalDateTime createdAt;
    }

    /**
     * 用户作品列表响应
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserArtworkListResponse {
        private List<UserArtworkItem> artworks;
        
        @JsonProperty("totalCount")
        private Long totalCount;
        
        @JsonProperty("currentPage")
        private Integer currentPage;
        
        @JsonProperty("pageSize")
        private Integer pageSize;
        
        @JsonProperty("totalPages")
        private Integer totalPages;
    }

    /**
     * 用户统计信息
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserStats {
        @JsonProperty("totalArtworks")
        private Long totalArtworks;
        
        @JsonProperty("sharedArtworks")
        private Long sharedArtworks;
        
        @JsonProperty("followingCount")
        private Long followingCount;
        
        @JsonProperty("followersCount")
        private Long followersCount;
        
        @JsonProperty("joinDate")
        private LocalDateTime joinDate;
        
        @JsonProperty("lastActiveDate")
        private LocalDateTime lastActiveDate;
    }

    /**
     * 个人中心主页数据
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProfileHomeData {
        @JsonProperty("userStats")
        private UserStats userStats;
        
        @JsonProperty("recentArtworks")
        private List<UserArtworkItem> recentArtworks;
    }
}
