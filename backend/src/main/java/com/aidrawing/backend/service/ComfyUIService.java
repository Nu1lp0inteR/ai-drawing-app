package com.aidrawing.backend.service;

import com.aidrawing.backend.client.ComfyUIWebSocketClient;
import com.aidrawing.backend.config.ComfyUIModelProperties;
import com.aidrawing.backend.dto.ComfyUIHistory;
import com.aidrawing.backend.dto.ComfyUIWSMessage;
import com.aidrawing.backend.dto.DrawingRequest;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Service
public class ComfyUIService {

    @Value("${comfyui.api.address}")
    private String comfyuiApiAddress;

    @Value("${file.storage.path}")
    private String storagePath;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final ComfyUIModelProperties modelProperties;

    public ComfyUIService(RestTemplate restTemplate, ObjectMapper objectMapper,
                          ComfyUIModelProperties modelProperties) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.modelProperties = modelProperties;
    }

    public ComfyUIModelProperties getModelProperties() {
        return modelProperties;
    }

    public ComfyUIModelProperties.ModelConfig resolveModel(DrawingRequest request) {
        String modelKey = request.getModelName();
        if (modelKey == null || modelKey.isBlank()) {
            modelKey = modelProperties.getModels().keySet().iterator().next();
        }
        ComfyUIModelProperties.ModelConfig config = modelProperties.getModel(modelKey);
        if (config == null) {
            throw new IllegalArgumentException("Unknown model: " + modelKey);
        }
        return config;
    }

    public CompletableFuture<ComfyUIWSMessage.ImageInfo> queuePrompt(DrawingRequest request) throws Exception {
        ComfyUIModelProperties.ModelConfig modelConfig = resolveModel(request);
        String promptId = submitPrompt(request, modelConfig);
        String outputNode = modelConfig.getOutputNode();
        System.out.println("Successfully queued prompt. ComfyUI prompt_id: " + promptId
                + ", model: " + modelConfig.getName() + ", output node: " + outputNode);
        return waitForCompletionAndGetHistory(promptId, outputNode);
    }

    @SuppressWarnings("unchecked")
    private String submitPrompt(DrawingRequest request, ComfyUIModelProperties.ModelConfig modelConfig) throws Exception {
        ClassPathResource resource = new ClassPathResource(modelConfig.getWorkflow());
        Map<String, Object> workflow;
        try (InputStream inputStream = resource.getInputStream()) {
            workflow = objectMapper.readValue(inputStream, new TypeReference<Map<String, Object>>() {});
        }

        Map<String, Object> promptInputs = (Map<String, Object>) ((Map<String, Object>) workflow.get(modelConfig.getPromptNode())).get("inputs");
        promptInputs.put(modelConfig.getPromptField(), request.getPrompt());

        Map<String, Object> negativeInputs = (Map<String, Object>) ((Map<String, Object>) workflow.get(modelConfig.getNegativeNode())).get("inputs");
        negativeInputs.put(modelConfig.getNegativeField(), request.getNegativePrompt());

        Map<String, Object> stepsInputs = (Map<String, Object>) ((Map<String, Object>) workflow.get(modelConfig.getStepsNode())).get("inputs");
        stepsInputs.put("steps", request.getSteps());
        stepsInputs.put("cfg", request.getCfg());

        String samplerName = request.getSamplerName();
        if (samplerName == null || samplerName.isBlank() || "None".equalsIgnoreCase(samplerName)) {
            System.out.println("WARN: Received invalid sampler_name: '" + samplerName + "'. Falling back to default 'euler_ancestral'.");
            samplerName = "euler_ancestral";
        }
        stepsInputs.put("sampler_name", samplerName);

        Map<String, Object> seedInputs = (Map<String, Object>) ((Map<String, Object>) workflow.get(modelConfig.getSeedNode())).get("inputs");
        seedInputs.put(modelConfig.getSeedField(), Long.parseLong(request.getSeed()));

        String clientId = UUID.randomUUID().toString();
        Map<String, Object> payload = Map.of(
                "prompt", workflow,
                "client_id", clientId
        );

        String url = comfyuiApiAddress + "/prompt";
        System.out.println("Sending POST request to: " + url);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);
            System.out.println("Successfully queued prompt. ComfyUI response: " + response.getBody());
            return (String) response.getBody().get("prompt_id");
        } catch (HttpClientErrorException e) {
            System.err.println("Error submitting prompt to ComfyUI. Status: " + e.getStatusCode() + ", Body: " + e.getResponseBodyAsString());
            throw e;
        }
    }

    private CompletableFuture<ComfyUIWSMessage.ImageInfo> waitForCompletionAndGetHistory(String promptId, String outputNode) {
        CompletableFuture<Void> completionSignal = new CompletableFuture<>();

        try {
            String wsUrl = comfyuiApiAddress.replaceFirst("http", "ws") + "/ws?clientId=" + UUID.randomUUID().toString();
            System.out.println("Connecting WebSocket client to: " + wsUrl);
            ComfyUIWebSocketClient client = new ComfyUIWebSocketClient(URI.create(wsUrl), promptId, completionSignal);
            client.connect();

            return completionSignal.thenCompose(v -> {
                client.close();
                return fetchHistoryAndExtractImage(promptId, outputNode);
            });
        } catch (Exception e) {
            System.err.println("Error initializing WebSocket connection: " + e.getMessage());
            return CompletableFuture.failedFuture(e);
        }
    }

    private CompletableFuture<ComfyUIWSMessage.ImageInfo> fetchHistoryAndExtractImage(String promptId, String outputNode) {
        try {
            String historyUrl = comfyuiApiAddress + "/history/" + promptId;
            System.out.println("[History] Fetching from: " + historyUrl);
            ResponseEntity<ComfyUIHistory> response = restTemplate.getForEntity(historyUrl, ComfyUIHistory.class);
            ComfyUIHistory history = response.getBody();

            ComfyUIHistory.HistoryEntry entry = history.get(promptId);
            ComfyUIHistory.NodeOutput targetNodeOutput = entry.getOutputs().get(outputNode);
            ComfyUIWSMessage.ImageInfo imageInfo = targetNodeOutput.getImages().get(0);

            System.out.println("[History] Successfully found image in output node [" + outputNode + "]: " + imageInfo);
            return CompletableFuture.completedFuture(imageInfo);
        } catch (Exception e) {
            System.err.println("[History] Failed to fetch or parse history: " + e.getMessage());
            return CompletableFuture.failedFuture(e);
        }
    }

    public byte[] getImage(ComfyUIWSMessage.ImageInfo imageInfo) {
        String url = UriComponentsBuilder.fromHttpUrl(comfyuiApiAddress + "/view")
                .queryParam("filename", imageInfo.getFilename())
                .queryParam("subfolder", imageInfo.getSubfolder())
                .queryParam("type", imageInfo.getType())
                .toUriString();

        System.out.println("[Image Download] Fetching from: " + url);
        return restTemplate.getForObject(url, byte[].class);
    }

    public String saveImageLocally(ComfyUIWSMessage.ImageInfo imageInfo) throws Exception {
        byte[] imageBytes = getImage(imageInfo);
        String filename = UUID.randomUUID().toString() + ".png";
        Path path = Paths.get(storagePath, filename);

        Files.createDirectories(path.getParent());
        Files.write(path, imageBytes);
        return filename;
    }
}
