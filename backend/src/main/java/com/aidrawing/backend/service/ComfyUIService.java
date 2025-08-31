// 文件路径: src/main/java/com/aidrawing/backend/service/ComfyUIService.java

package com.aidrawing.backend.service;

import com.aidrawing.backend.client.ComfyUIWebSocketClient;
import com.aidrawing.backend.dto.ComfyUIHistory;
import com.aidrawing.backend.dto.ComfyUIWSMessage;
import com.aidrawing.backend.dto.DrawingRequest;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
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

    public ComfyUIService(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    public CompletableFuture<ComfyUIWSMessage.ImageInfo> queuePrompt(DrawingRequest request) throws Exception {
        String promptId = submitPrompt(request);
        System.out.println("Successfully queued prompt. ComfyUI prompt_id: " + promptId);
        return waitForCompletionAndGetHistory(promptId);
    }

    @SuppressWarnings("unchecked")
    private String submitPrompt(DrawingRequest request) throws Exception {
        // 1. Load the workflow template from the classpath (Robust Method).
        // 1. 从类路径加载工作流模板 (稳健的企业级方法)。
        ClassPathResource resource = new ClassPathResource("ComfyUI_api.json");
        Map<String, Object> workflow;
        try (InputStream inputStream = resource.getInputStream()) {
            workflow = objectMapper.readValue(inputStream, new TypeReference<Map<String, Object>>() {});
        }

        // 2. Modify the workflow with user inputs.
        // 2. 使用用户输入修改工作流。
        Map<String, Object> node82Inputs = (Map<String, Object>) ((Map<String, Object>) workflow.get("82")).get("inputs");
        node82Inputs.put("positive", request.getPrompt());
        node82Inputs.put("negative", request.getNegativePrompt());

        Map<String, Object> node83Inputs = (Map<String, Object>) ((Map<String, Object>) workflow.get("83")).get("inputs");
        node83Inputs.put("steps", request.getSteps());
        node83Inputs.put("cfg", request.getCfg());

        // This is the validation logic (our "security guard").
        // 这是验证逻辑（我们的“安全守卫”）。
        String samplerName = request.getSamplerName();
        if (samplerName == null || samplerName.isBlank() || "None".equalsIgnoreCase(samplerName)) {
            System.out.println("WARN: Received invalid sampler_name: '" + samplerName + "'. Falling back to default 'euler_ancestral'.");
            samplerName = "euler_ancestral";
        }
        node83Inputs.put("sampler_name", samplerName);

        // Convert String seed to Long for the API and apply to both nodes
        // 注意：我们在这里将字符串类型的seed转换为长整型，以匹配ComfyUI API的要求。
        long seedValue = Long.parseLong(request.getSeed());
        node82Inputs.put("seed", seedValue); // Also update seed in loader for consistency
        node83Inputs.put("seed", seedValue);


        // 3. Prepare the final payload.
        // 3. 准备最终的请求体。
        String clientId = UUID.randomUUID().toString();
        Map<String, Object> payload = Map.of(
                "prompt", workflow,
                "client_id", clientId
        );

        // 4. Send the request to ComfyUI.
        // 4. 发送请求到 ComfyUI.
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

    private CompletableFuture<ComfyUIWSMessage.ImageInfo> waitForCompletionAndGetHistory(String promptId) {
        CompletableFuture<Void> completionSignal = new CompletableFuture<>();
        
        try {
            String wsUrl = comfyuiApiAddress.replaceFirst("http", "ws") + "/ws?clientId=" + UUID.randomUUID().toString();
            System.out.println("Connecting WebSocket client to: " + wsUrl);
            ComfyUIWebSocketClient client = new ComfyUIWebSocketClient(URI.create(wsUrl), promptId, completionSignal);
            client.connect();

            return completionSignal.thenCompose(v -> {
                client.close(); // Close WebSocket connection
                return fetchHistoryAndExtractImage(promptId);
            });
        } catch (Exception e) {
            System.err.println("Error initializing WebSocket connection: " + e.getMessage());
            return CompletableFuture.failedFuture(e);
        }
    }

    private CompletableFuture<ComfyUIWSMessage.ImageInfo> fetchHistoryAndExtractImage(String promptId) {
        try {
            String historyUrl = comfyuiApiAddress + "/history/" + promptId;
            System.out.println("[History] Fetching from: " + historyUrl);
            ResponseEntity<ComfyUIHistory> response = restTemplate.getForEntity(historyUrl, ComfyUIHistory.class);
            ComfyUIHistory history = response.getBody();

            ComfyUIHistory.HistoryEntry entry = history.get(promptId);
            ComfyUIHistory.NodeOutput targetNodeOutput = entry.getOutputs().get("45");
            ComfyUIWSMessage.ImageInfo imageInfo = targetNodeOutput.getImages().get(0);

            System.out.println("[History] Successfully found image in target node [45]: " + imageInfo);
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

        // Ensure parent directory exists
        Files.createDirectories(path.getParent());
        Files.write(path, imageBytes);
        return filename;
    }
}
