// 文件路径: src/main/java/com/aidrawing/backend/dto/DrawingRequest.java

package com.aidrawing.backend.dto;

public class DrawingRequest {

    private String prompt;
    private String negativePrompt;
    private Integer steps;
    private Double cfg;
    private String samplerName;
    // We change the type to String to perfectly match the Drawing entity.
    // This provides more flexibility and prevents type mismatch errors.
    // 我们将类型更改为String，以与Drawing实体完美匹配。
    // 这提供了更大的灵活性，并防止了类型不匹配的错误。
    private String seed;
    private String userId; // 用户ID，用于关联图片到用户

    // Getters and Setters
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

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    @Override
    public String toString() {
        return "DrawingRequest{" +
                "prompt='" + prompt + '\'' +
                ", negativePrompt='" + negativePrompt + '\'' +
                ", steps=" + steps +
                ", cfg=" + cfg +
                ", samplerName='" + samplerName + '\'' +
                ", seed='" + seed + '\'' +
                ", userId='" + userId + '\'' +
                '}';
    }
}
