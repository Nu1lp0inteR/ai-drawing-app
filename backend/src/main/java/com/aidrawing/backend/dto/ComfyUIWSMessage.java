// 文件路径: src/main/java/com/aidrawing/backend/dto/ComfyUIWSMessage.java

package com.aidrawing.backend.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ComfyUIWSMessage {

    private String type;
    private DataPayload data;

    // Getters and Setters
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public DataPayload getData() { return data; }
    public void setData(DataPayload data) { this.data = data; }

    // --- Nested Classes for Data Payload ---

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class DataPayload {
        // This field is present in 'status' messages
        private StatusInfo status;
        
        // This field is present in 'executed' messages
        private String node;
        private OutputPayload output;

        public StatusInfo getStatus() { return status; }
        public void setStatus(StatusInfo status) { this.status = status; }
        public String getNode() { return node; }
        public void setNode(String node) { this.node = node; }
        public OutputPayload getOutput() { return output; }
        public void setOutput(OutputPayload output) { this.output = output; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class StatusInfo {
        @JsonProperty("exec_info")
        private ExecInfo execInfo;

        public ExecInfo getExecInfo() { return execInfo; }
        public void setExecInfo(ExecInfo execInfo) { this.execInfo = execInfo; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ExecInfo {
        @JsonProperty("queue_remaining")
        private int queueRemaining;

        public int getQueueRemaining() { return queueRemaining; }
        public void setQueueRemaining(int queueRemaining) { this.queueRemaining = queueRemaining; }
    }
    
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class OutputPayload {
        private java.util.List<ImageInfo> images;

        public java.util.List<ImageInfo> getImages() { return images; }
        public void setImages(java.util.List<ImageInfo> images) { this.images = images; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ImageInfo {
        private String filename;
        private String subfolder;
        private String type;

        // Getters and Setters
        public String getFilename() { return filename; }
        public void setFilename(String filename) { this.filename = filename; }
        public String getSubfolder() { return subfolder; }
        public void setSubfolder(String subfolder) { this.subfolder = subfolder; }
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }

        @Override
        public String toString() {
            return "ImageInfo{" +
                    "filename='" + filename + '\'' +
                    ", subfolder='" + subfolder + '\'' +
                    ", type='" + type + '\'' +
                    '}';
        }
    }
}
