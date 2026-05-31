package com.aidrawing.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
@ConfigurationProperties(prefix = "comfyui")
public class ComfyUIModelProperties {

    private Map<String, ModelConfig> models = new LinkedHashMap<>();

    public Map<String, ModelConfig> getModels() {
        return models;
    }

    public void setModels(Map<String, ModelConfig> models) {
        this.models = models;
    }

    public ModelConfig getModel(String key) {
        return models.get(key);
    }

    public static class ModelConfig {
        private String name;
        private String workflow;
        private String promptNode;
        private String promptField;
        private String negativeNode;
        private String negativeField;
        private String seedNode;
        private String seedField;
        private String stepsNode;
        private String cfgNode;
        private String samplerNode;
        private String outputNode;
        private String outputField;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getWorkflow() { return workflow; }
        public void setWorkflow(String workflow) { this.workflow = workflow; }

        public String getPromptNode() { return promptNode; }
        public void setPromptNode(String promptNode) { this.promptNode = promptNode; }

        public String getPromptField() { return promptField; }
        public void setPromptField(String promptField) { this.promptField = promptField; }

        public String getNegativeNode() { return negativeNode; }
        public void setNegativeNode(String negativeNode) { this.negativeNode = negativeNode; }

        public String getNegativeField() { return negativeField; }
        public void setNegativeField(String negativeField) { this.negativeField = negativeField; }

        public String getSeedNode() { return seedNode; }
        public void setSeedNode(String seedNode) { this.seedNode = seedNode; }

        public String getSeedField() { return seedField; }
        public void setSeedField(String seedField) { this.seedField = seedField; }

        public String getStepsNode() { return stepsNode; }
        public void setStepsNode(String stepsNode) { this.stepsNode = stepsNode; }

        public String getCfgNode() { return cfgNode; }
        public void setCfgNode(String cfgNode) { this.cfgNode = cfgNode; }

        public String getSamplerNode() { return samplerNode; }
        public void setSamplerNode(String samplerNode) { this.samplerNode = samplerNode; }

        public String getOutputNode() { return outputNode; }
        public void setOutputNode(String outputNode) { this.outputNode = outputNode; }

        public String getOutputField() { return outputField; }
        public void setOutputField(String outputField) { this.outputField = outputField; }
    }
}
