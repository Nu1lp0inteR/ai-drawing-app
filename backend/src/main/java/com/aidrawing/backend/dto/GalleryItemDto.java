package com.aidrawing.backend.dto;

import com.aidrawing.backend.entity.Drawing;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;

/**
 * 画廊项目数据传输对象
 * Gallery Item Data Transfer Object
 * 
 * 解决序列化问题的企业级解决方案：
 * - 明确控制JSON序列化格式
 * - 避免JPA实体直接序列化的复杂性
 * - 提供清晰的API契约
 */
public class GalleryItemDto {
    
    private String id;
    private String prompt;
    
    @JsonProperty("negativePrompt") // 驼峰命名需要明确指定
    private String negativePrompt;
    
    private Integer steps;
    private Double cfg;
    
    @JsonProperty("samplerName") // 驼峰命名需要明确指定
    private String samplerName;
    
    private String seed;
    
    @JsonProperty("storedFilename") // 驼峰命名需要明确指定
    private String storedFilename;
    
    @JsonProperty("sharedToGallery") // 驼峰命名需要明确指定
    private boolean sharedToGallery;
    
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @JsonProperty("createdAt") // 驼峰命名需要明确指定
    private LocalDateTime createdAt;

    // 默认构造函数
    public GalleryItemDto() {}

    // 从Drawing实体构造DTO的便利构造函数
    public GalleryItemDto(Drawing drawing) {
        this.id = drawing.getId();
        this.prompt = drawing.getPrompt();
        this.negativePrompt = drawing.getNegativePrompt();
        this.steps = drawing.getSteps();
        this.cfg = drawing.getCfg();
        this.samplerName = drawing.getSamplerName();
        this.seed = drawing.getSeed();
        this.storedFilename = drawing.getStoredFilename();
        this.sharedToGallery = drawing.isSharedToGallery();
        this.createdAt = drawing.getCreatedAt();
    }

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
}
