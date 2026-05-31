package com.aidrawing.backend.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public class DrawingRequest {

    @NotBlank(message = "提示词不能为空")
    private String prompt;

    private String negativePrompt;

    @Min(value = 1, message = "采样步数不能小于1")
    @Max(value = 100, message = "采样步数不能大于100")
    private Integer steps;

    @Min(value = 1, message = "CFG值不能小于1")
    @Max(value = 20, message = "CFG值不能大于20")
    private Double cfg;

    private String samplerName;

    private String seed;

    private String userId;

    private String modelName;

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

    public String getModelName() {
        return modelName;
    }

    public void setModelName(String modelName) {
        this.modelName = modelName;
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
                ", modelName='" + modelName + '\'' +
                '}';
    }
}
