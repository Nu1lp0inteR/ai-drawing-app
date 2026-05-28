package com.aidrawing.backend.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.GenericGenerator;

import java.time.LocalDateTime;

/**
 * 用户关注关系实体
 * User Follow Relationship Entity
 * 
 * 表示用户之间的关注关系
 * - follower: 关注者
 * - following: 被关注者
 */
@Entity
@Table(name = "user_follows", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"follower_id", "following_id"}))
public class Follow {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "id", updatable = false, nullable = false)
    private String id;

    /**
     * 关注者用户
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "follower_id", nullable = false)
    private User follower;

    /**
     * 被关注者用户
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "following_id", nullable = false)
    private User following;

    /**
     * 关注时间
     */
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    // 构造函数
    public Follow() {
        this.createdAt = LocalDateTime.now();
    }

    public Follow(User follower, User following) {
        this();
        this.follower = follower;
        this.following = following;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public User getFollower() {
        return follower;
    }

    public void setFollower(User follower) {
        this.follower = follower;
    }

    public User getFollowing() {
        return following;
    }

    public void setFollowing(User following) {
        this.following = following;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    // 业务方法
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Follow)) return false;
        Follow follow = (Follow) o;
        return id != null && id.equals(follow.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "Follow{" +
                "id='" + id + '\'' +
                ", follower=" + (follower != null ? follower.getUsername() : "null") +
                ", following=" + (following != null ? following.getUsername() : "null") +
                ", createdAt=" + createdAt +
                '}';
    }
}
