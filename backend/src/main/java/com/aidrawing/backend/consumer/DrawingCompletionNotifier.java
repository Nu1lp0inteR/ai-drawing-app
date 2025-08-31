package com.aidrawing.backend.consumer;

import com.aidrawing.backend.event.DrawingCompleteEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
public class DrawingCompletionNotifier {

    private static final Logger logger = LoggerFactory.getLogger(DrawingCompletionNotifier.class);

    private final SimpMessagingTemplate messagingTemplate;

    @Autowired
    public DrawingCompletionNotifier(@Lazy SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @EventListener
    @Async // This annotation is the key fix!
    public void handleDrawingComplete(DrawingCompleteEvent event) {
        try {
            logger.info("Received drawing completion event for ID: {}. Processing asynchronously.", event.getDrawing().getId());
            messagingTemplate.convertAndSend("/topic/drawing_complete", event.getDrawing());
            logger.info("Successfully sent WebSocket notification to frontend for drawing ID: {}", event.getDrawing().getId());
        } catch (Exception e) {
            logger.error("Failed to send WebSocket notification for drawing ID: {}", event.getDrawing().getId(), e);
        }
    }
}
