package com.aidrawing.backend.consumer;

import com.aidrawing.backend.dto.ComfyUIWSMessage;
import com.aidrawing.backend.dto.DrawingRequest;
import com.aidrawing.backend.entity.Drawing;
import com.aidrawing.backend.entity.User;
import com.aidrawing.backend.event.DrawingCompleteEvent;
import com.aidrawing.backend.repository.DrawingRepository;
import com.aidrawing.backend.repository.UserRepository;
import com.aidrawing.backend.service.ComfyUIService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;

import java.util.concurrent.CompletableFuture;

@Component
public class DrawingTaskConsumer {

    private static final Logger logger = LoggerFactory.getLogger(DrawingTaskConsumer.class);

    private final ObjectMapper objectMapper;
    private final ComfyUIService comfyUIService;
    private final DrawingRepository drawingRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final SimpMessagingTemplate messagingTemplate;

    @Autowired
    public DrawingTaskConsumer(
            ObjectMapper objectMapper,
            ComfyUIService comfyUIService,
            DrawingRepository drawingRepository,
            UserRepository userRepository,
            ApplicationEventPublisher eventPublisher,
            SimpMessagingTemplate messagingTemplate
    ) {
        this.objectMapper = objectMapper;
        this.comfyUIService = comfyUIService;
        this.drawingRepository = drawingRepository;
        this.userRepository = userRepository;
        this.eventPublisher = eventPublisher;
        this.messagingTemplate = messagingTemplate;
    }

    @RabbitListener(queues = "${rabbitmq.queue.name:drawing_task_queue}")
    public void receiveDrawingTask(String message) {
        try {
            logger.info("Received a new task from RabbitMQ: {}", message);

            // 1. Deserialize the message.
            DrawingRequest request = objectMapper.readValue(message, DrawingRequest.class);
            logger.info("Successfully deserialized task. Processing for prompt: {}", request.getPrompt());

            // 2. Delegate to ComfyUIService and wait for the result.
            logger.info("Delegating to ComfyUIService...");
            CompletableFuture<ComfyUIWSMessage.ImageInfo> futureResult = comfyUIService.queuePrompt(request);
            ComfyUIWSMessage.ImageInfo finalImageInfo = futureResult.get();

            // 3. Save the image locally.
            String newFilename = comfyUIService.saveImageLocally(finalImageInfo);
            logger.info("Image saved to local storage at: {}", newFilename);

            // 4. Create and save the drawing entity.
            Drawing drawing = new Drawing();
            drawing.setPrompt(request.getPrompt());
            drawing.setNegativePrompt(request.getNegativePrompt());
            drawing.setSteps(request.getSteps());
            drawing.setCfg(request.getCfg());
            drawing.setSamplerName(request.getSamplerName());
            drawing.setSeed(request.getSeed());
            drawing.setStoredFilename(newFilename);
            drawing.setOriginalFilename(finalImageInfo.getFilename());
            drawing.setFileType("image/png");
            
            // 设置用户关联
            if (request.getUserId() != null) {
                User user = userRepository.findById(request.getUserId()).orElse(null);
                if (user != null) {
                    drawing.setUser(user);
                    logger.info("🔗 [Consumer] 设置图片关联用户: {} ({})", user.getUsername(), request.getUserId());
                } else {
                    logger.warn("⚠️ [Consumer] 未找到用户ID: {}", request.getUserId());
                }
            } else {
                logger.warn("⚠️ [Consumer] 请求中没有用户ID，图片将不关联到任何用户");
            }

            Drawing savedDrawing = drawingRepository.save(drawing);
            logger.info("Drawing metadata saved to database with ID: {}", savedDrawing.getId());

            // 5. Publish an event to notify other parts of the application.
            eventPublisher.publishEvent(new DrawingCompleteEvent(this, savedDrawing));
            logger.info("Published DrawingCompleteEvent for drawing ID: {}", savedDrawing.getId());

            logger.info("==========================================================");
            logger.info("TASK PROCESSING COMPLETED SUCCESSFULLY!");
            logger.info("==========================================================");

        } catch (Exception e) {
            logger.error("❌ 任务处理失败:", e);
            
            // 发送失败通知给前端
            try {
                String errorMessage = getErrorMessage(e);
                String failureNotification = String.format(
                    "{\"status\":\"FAILED\",\"error\":\"%s\",\"timestamp\":\"%s\"}", 
                    errorMessage, 
                    java.time.LocalDateTime.now()
                );
                
                messagingTemplate.convertAndSend("/topic/drawing_complete", failureNotification);
                logger.info("🔔 已发送失败通知给前端: {}", errorMessage);
                
            } catch (Exception notificationError) {
                logger.error("❌ 发送失败通知时出错:", notificationError);
            }
        }
    }

    /**
     * 根据异常类型生成用户友好的错误消息
     */
    private String getErrorMessage(Exception e) {
        if (e instanceof ResourceAccessException && e.getMessage().contains("Connection refused")) {
            return "服务器出错了，请稍后再试";
        } else {
            return "图片生成遇到未知问题，请稍后重试";
        }
    }
}
