package com.aidrawing.backend.dto;

import com.aidrawing.backend.entity.User;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;

/**
 * 用户个人主页数据传输对象
 * User Profile Data Transfer Object - 用于展示用户公开信息
 */
public class UserProfileDto {
    
    private String id;
    private String username;
    private String email;
    
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @JsonProperty("joinDate")
    private LocalDateTime joinDate;
    
    // 统计信息
    @JsonProperty("totalArtworks")
    private Long totalArtworks;
    
    @JsonProperty("sharedArtworks") 
    private Long sharedArtworks;
    
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @JsonProperty("lastActiveDate")
    private LocalDateTime lastActiveDate;
    
    // 关注/粉丝统计
    @JsonProperty("followingCount")
    private Long followingCount;
    
    @JsonProperty("followersCount")
    private Long followersCount;

    // 默认构造函数
    public UserProfileDto() {}

    // 从User实体构造DTO的便利构造函数
    public UserProfileDto(User user) {
        this.id = user.getId();
        this.username = user.getUsername();
        this.email = user.getEmail(); // 注意：实际产品中可能需要隐藏邮箱
        this.joinDate = user.getCreatedAt();
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public LocalDateTime getJoinDate() {
        return joinDate;
    }

    public void setJoinDate(LocalDateTime joinDate) {
        this.joinDate = joinDate;
    }

    public Long getTotalArtworks() {
        return totalArtworks;
    }

    public void setTotalArtworks(Long totalArtworks) {
        this.totalArtworks = totalArtworks;
    }

    public Long getSharedArtworks() {
        return sharedArtworks;
    }

    public void setSharedArtworks(Long sharedArtworks) {
        this.sharedArtworks = sharedArtworks;
    }

    public LocalDateTime getLastActiveDate() {
        return lastActiveDate;
    }

    public void setLastActiveDate(LocalDateTime lastActiveDate) {
        this.lastActiveDate = lastActiveDate;
    }

    public Long getFollowingCount() {
        return followingCount;
    }

    public void setFollowingCount(Long followingCount) {
        this.followingCount = followingCount;
    }

    public Long getFollowersCount() {
        return followersCount;
    }

    public void setFollowersCount(Long followersCount) {
        this.followersCount = followersCount;
    }
}
