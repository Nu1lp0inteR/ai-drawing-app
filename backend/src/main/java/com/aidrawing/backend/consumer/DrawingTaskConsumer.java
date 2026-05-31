package com.aidrawing.backend.consumer;

import com.aidrawing.backend.dto.ComfyUIWSMessage;
import com.aidrawing.backend.dto.DrawingRequest;
import com.aidrawing.backend.entity.Drawing;
import com.aidrawing.backend.entity.User;
import com.aidrawing.backend.repository.DrawingRepository;
import com.aidrawing.backend.repository.UserRepository;
import com.aidrawing.backend.service.ComfyUIService;
import com.aidrawing.backend.service.CreditService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Component
public class DrawingTaskConsumer {

    private static final Logger logger = LoggerFactory.getLogger(DrawingTaskConsumer.class);

    private final ObjectMapper objectMapper;
    private final ComfyUIService comfyUIService;
    private final SimpMessagingTemplate messagingTemplate;
    private final DrawingRepository drawingRepository;
    private final UserRepository userRepository;
    private final CreditService creditService;

    @Value("${file.storage.path}")
    private String storagePath;

    @Autowired
    public DrawingTaskConsumer(
            ObjectMapper objectMapper,
            ComfyUIService comfyUIService,
            SimpMessagingTemplate messagingTemplate,
            DrawingRepository drawingRepository,
            UserRepository userRepository,
            CreditService creditService
    ) {
        this.objectMapper = objectMapper;
        this.comfyUIService = comfyUIService;
        this.messagingTemplate = messagingTemplate;
        this.drawingRepository = drawingRepository;
        this.userRepository = userRepository;
        this.creditService = creditService;
    }

    @RabbitListener(queues = "${rabbitmq.queue.name:drawing_task_queue}")
    public void receiveDrawingTask(String message) {
        DrawingRequest request = null;
        try {
            logger.info("Received a new task from RabbitMQ: {}", message);

            request = objectMapper.readValue(message, DrawingRequest.class);
            logger.info("Successfully deserialized task. Processing for prompt: {}", request.getPrompt());

            logger.info("Delegating to ComfyUIService...");
            CompletableFuture<ComfyUIWSMessage.ImageInfo> futureResult = comfyUIService.queuePrompt(request);
            ComfyUIWSMessage.ImageInfo finalImageInfo = futureResult.get();

            byte[] imageBytes = comfyUIService.getImage(finalImageInfo);
            String imageBase64 = Base64.getEncoder().encodeToString(imageBytes);

            Map<String, Object> pushMessage = new HashMap<>();
            pushMessage.put("status", "SUCCESS");
            pushMessage.put("prompt", request.getPrompt());
            pushMessage.put("negative_prompt", request.getNegativePrompt());
            pushMessage.put("steps", request.getSteps());
            pushMessage.put("cfg", request.getCfg());
            pushMessage.put("sampler_name", request.getSamplerName());
            pushMessage.put("seed", request.getSeed());
            pushMessage.put("image_base64", imageBase64);
            pushMessage.put("timestamp", java.time.LocalDateTime.now().toString());

            String drawingId = persistDrawing(request, finalImageInfo, imageBytes);
            if (drawingId != null) {
                pushMessage.put("drawing_id", drawingId);
            }

            messagingTemplate.convertAndSend("/topic/drawing_complete", pushMessage);
            logger.info("Task completed. Image base64 pushed to frontend (size: {} bytes)", imageBase64.length());

        } catch (Exception e) {
            logger.error("Task processing failed:", e);

            try {
                if (request != null && request.getUserId() != null) {
                    creditService.addCredits(request.getUserId(), 1, "REFUND",
                        "生成失败退款: " + (request.getPrompt() != null ? request.getPrompt().substring(0, Math.min(request.getPrompt().length(), 40)) : ""));
                }
            } catch (Exception refundError) {
                logger.error("Failed to refund credits: {}", refundError.getMessage());
            }

            try {
                String errorMessage = getErrorMessage(e);
                Map<String, Object> failureNotification = new HashMap<>();
                failureNotification.put("status", "FAILED");
                failureNotification.put("error", errorMessage);
                failureNotification.put("timestamp", java.time.LocalDateTime.now().toString());

                messagingTemplate.convertAndSend("/topic/drawing_complete", failureNotification);
                logger.info("Sent failure notification to frontend: {}", errorMessage);

            } catch (Exception notificationError) {
                logger.error("Error sending failure notification:", notificationError);
            }
        }
    }

    private String getErrorMessage(Exception e) {
        if (e instanceof ResourceAccessException && e.getMessage().contains("Connection refused")) {
            return "服务器出错了，请稍后再试";
        } else {
            return "图片生成遇到未知问题，请稍后重试";
        }
    }

    private String persistDrawing(DrawingRequest request, ComfyUIWSMessage.ImageInfo imageInfo, byte[] imageBytes) {
        try {
            String storedFilename = UUID.randomUUID().toString() + ".png";
            Path storageDir = Paths.get(storagePath);
            Files.createDirectories(storageDir);
            Files.write(storageDir.resolve(storedFilename), imageBytes);
            logger.info("Image saved to storage: {}", storedFilename);

            String userId = request.getUserId();
            if (userId == null) {
                logger.warn("No userId in DrawingRequest, skipping DB persist");
                return null;
            }

            Optional<User> userOpt = userRepository.findById(userId);
            if (userOpt.isEmpty()) {
                logger.warn("User not found: {}, skipping DB persist", userId);
                return null;
            }

            Drawing drawing = new Drawing();
            drawing.setUser(userOpt.get());
            drawing.setPrompt(request.getPrompt());
            drawing.setNegativePrompt(request.getNegativePrompt());
            drawing.setSteps(request.getSteps());
            drawing.setCfg(request.getCfg());
            drawing.setSamplerName(request.getSamplerName());
            drawing.setSeed(request.getSeed());
            drawing.setModelName(request.getModelName());
            drawing.setStoredFilename(storedFilename);
            drawing.setOriginalFilename(imageInfo.getFilename());
            drawing.setFileType("image/png");
            drawing.setSharedToGallery(false);

            Drawing savedDrawing = drawingRepository.save(drawing);
            logger.info("Drawing persisted: id={}, userId={}, sharedToGallery=false", savedDrawing.getId(), userId);
            return savedDrawing.getId();

        } catch (Exception e) {
            logger.error("Failed to persist drawing to database (image still delivered to frontend):", e);
            return null;
        }
    }
}
