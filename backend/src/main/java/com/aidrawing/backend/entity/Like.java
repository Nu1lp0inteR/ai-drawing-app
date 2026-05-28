package com.aidrawing.backend.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.GenericGenerator;

import java.time.LocalDateTime;

/**
 * 作品点赞实体
 * Artwork Like Entity
 * 
 * 表示用户对作品的点赞关系
 * - user: 点赞用户
 * - drawing: 被点赞的作品
 */
@Entity
@Table(name = "artwork_likes", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "drawing_id"}))
public class Like {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "id", updatable = false, nullable = false)
    private String id;

    /**
     * 点赞用户
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * 被点赞的作品
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "drawing_id", nullable = false)
    private Drawing drawing;

    /**
     * 点赞时间
     */
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    // 构造函数
    public Like() {
        this.createdAt = LocalDateTime.now();
    }

    public Like(User user, Drawing drawing) {
        this();
        this.user = user;
        this.drawing = drawing;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Drawing getDrawing() {
        return drawing;
    }

    public void setDrawing(Drawing drawing) {
        this.drawing = drawing;
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
        if (!(o instanceof Like)) return false;
        Like like = (Like) o;
        return id != null && id.equals(like.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "Like{" +
                "id='" + id + '\'' +
                ", user=" + (user != null ? user.getUsername() : "null") +
                ", drawing=" + (drawing != null ? drawing.getId() : "null") +
                ", createdAt=" + createdAt +
                '}';
    }
}
