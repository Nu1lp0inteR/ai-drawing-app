// 文件路径: src/main/java/com/aidrawing/backend/dto/ComfyUIHistory.java

package com.aidrawing.backend.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

// This class represents the entire history response, which is a Map from prompt_id to HistoryEntry
@JsonIgnoreProperties(ignoreUnknown = true)
public class ComfyUIHistory extends java.util.HashMap<String, ComfyUIHistory.HistoryEntry> {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class HistoryEntry {
        @JsonProperty("outputs")
        private Map<String, NodeOutput> outputs;

        public Map<String, NodeOutput> getOutputs() {
            return outputs;
        }

        public void setOutputs(Map<String, NodeOutput> outputs) {
            this.outputs = outputs;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class NodeOutput {
        @JsonProperty("images")
        private List<ComfyUIWSMessage.ImageInfo> images;

        public List<ComfyUIWSMessage.ImageInfo> getImages() {
            return images;
        }

        public void setImages(List<ComfyUIWSMessage.ImageInfo> images) {
            this.images = images;
        }
    }
}

