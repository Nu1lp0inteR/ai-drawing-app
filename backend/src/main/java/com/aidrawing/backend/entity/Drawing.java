// 文件路径: src/main/java/com/aidrawing/backend/entity/Drawing.java

package com.aidrawing.backend.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.GenericGenerator;

import java.time.LocalDateTime;

@Entity
@Table(name = "drawings")
public class Drawing {

    // Use a more robust Hibernate-specific UUID generator.
    // This is the most reliable way to handle UUID primary keys.
    // 我们使用更稳健的、Hibernate专用的UUID生成器。
    // 这是处理UUID主键最可靠的方式。
    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(
        name = "UUID",
        strategy = "org.hibernate.id.UUIDGenerator"
    )
    @Column(updatable = false, nullable = false)
    private String id;

    // 用户关联 - 每个作品都属于一个用户
    // User association - each artwork belongs to a user
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(columnDefinition = "TEXT")
    private String prompt;

    @Column(columnDefinition = "TEXT")
    private String negativePrompt;

    private Integer steps;
    private Double cfg;
    private String samplerName;
    private String seed;

    // The name of the file as saved on our server's local storage.
    // 图片保存在我们服务器本地存储中的文件名。
    @Column(nullable = false)
    private String storedFilename;

    // The original filename from ComfyUI (useful for debugging).
    // 来自ComfyUI的原始文件名 (便于调试)。
    private String originalFilename;

    // e.g., "image/png"
    private String fileType;

    // Is the drawing shared to the public gallery? Defaults to false.
    // 图片是否分享到公共画廊？默认为false。
    @Column(nullable = false)
    private boolean sharedToGallery = false;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // Getters and Setters

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getPrompt() {
        return prompt;
    }

    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }

    public String getNegativePrompt() {
        return negativePrompt;
    }

    public void setNegativePrompt(String negativePrompt) {
        this.negativePrompt = negativePrompt;
    }

    public Integer getSteps() {
        return steps;
    }

    public void setSteps(Integer steps) {
        this.steps = steps;
    }

    public Double getCfg() {
        return cfg;
    }

    public void setCfg(Double cfg) {
        this.cfg = cfg;
    }

    public String getSamplerName() {
        return samplerName;
    }

    public void setSamplerName(String samplerName) {
        this.samplerName = samplerName;
    }

    public String getSeed() {
        return seed;
    }

    public void setSeed(String seed) {
        this.seed = seed;
    }

    public String getStoredFilename() {
        return storedFilename;
    }

    public void setStoredFilename(String storedFilename) {
        this.storedFilename = storedFilename;
    }

    public String getOriginalFilename() {
        return originalFilename;
    }

    public void setOriginalFilename(String originalFilename) {
        this.originalFilename = originalFilename;
    }

    public String getFileType() {
        return fileType;
    }

    public void setFileType(String fileType) {
        this.fileType = fileType;
    }

    public boolean isSharedToGallery() {
        return sharedToGallery;
    }

    public void setSharedToGallery(boolean sharedToGallery) {
        this.sharedToGallery = sharedToGallery;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }
}
