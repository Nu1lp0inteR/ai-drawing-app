package com.aidrawing.backend.consumer;

import com.aidrawing.backend.dto.ComfyUIWSMessage;
import com.aidrawing.backend.dto.DrawingRequest;
import com.aidrawing.backend.entity.Drawing;
import com.aidrawing.backend.event.DrawingCompleteEvent;
import com.aidrawing.backend.repository.DrawingRepository;
import com.aidrawing.backend.service.ComfyUIService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Component
public class DrawingTaskConsumer {

    private static final Logger logger = LoggerFactory.getLogger(DrawingTaskConsumer.class);

    private final ObjectMapper objectMapper;
    private final ComfyUIService comfyUIService;
    private final DrawingRepository drawingRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Autowired
    public DrawingTaskConsumer(
            ObjectMapper objectMapper,
            ComfyUIService comfyUIService,
            DrawingRepository drawingRepository,
            ApplicationEventPublisher eventPublisher
    ) {
        this.objectMapper = objectMapper;
        this.comfyUIService = comfyUIService;
        this.drawingRepository = drawingRepository;
        this.eventPublisher = eventPublisher;
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

            Drawing savedDrawing = drawingRepository.save(drawing);
            logger.info("Drawing metadata saved to database with ID: {}", savedDrawing.getId());

            // 5. Publish an event to notify other parts of the application.
            eventPublisher.publishEvent(new DrawingCompleteEvent(this, savedDrawing));
            logger.info("Published DrawingCompleteEvent for drawing ID: {}", savedDrawing.getId());

            logger.info("==========================================================");
            logger.info("TASK PROCESSING COMPLETED SUCCESSFULLY!");
            logger.info("==========================================================");

        } catch (Exception e) {
            logger.error("An error occurred during task processing:", e);
        }
    }
}
