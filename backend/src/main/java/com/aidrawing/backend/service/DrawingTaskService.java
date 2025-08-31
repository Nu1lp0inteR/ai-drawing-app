package com.aidrawing.backend.service;

import com.aidrawing.backend.config.RabbitMQConfig;
import com.aidrawing.backend.dto.DrawingRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Service responsible for sending drawing tasks to the RabbitMQ queue.
 * * 负责将绘图任务发送到 RabbitMQ 队列的服务。
 */
@Service
public class DrawingTaskService {

    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper; // For converting Java objects to JSON strings.

    @Autowired
    public DrawingTaskService(RabbitTemplate rabbitTemplate, ObjectMapper objectMapper) {
        this.rabbitTemplate = rabbitTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * Sends a drawing request to the message queue.
     * * 发送一个绘图请求到消息队列。
     *
     * @param request The drawing parameters.
     */
    public void sendDrawingTask(DrawingRequest request) {
        try {
            // Convert the DrawingRequest object to a JSON string.
            // 将 DrawingRequest 对象转换为 JSON 字符串。
            String message = objectMapper.writeValueAsString(request);

            // Send the message to the specified exchange with the routing key.
            // 将消息发送到指定的交换机并附带路由键。
            rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_NAME, RabbitMQConfig.ROUTING_KEY, message);

            System.out.println("Successfully sent task to RabbitMQ: " + message);
        } catch (Exception e) {
            System.err.println("Error sending task to RabbitMQ: " + e.getMessage());
            // In a real application, you would handle this error more gracefully.
            // 在真实应用中，您需要更优雅地处理此错误。
        }
    }
}
