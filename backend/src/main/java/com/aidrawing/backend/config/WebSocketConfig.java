package com.aidrawing.backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * Configuration for WebSocket message handling.
 * This class sets up the message broker and registers the STOMP endpoints.
 * It is the "control tower" for our real-time communication.
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    /**
     * Configures the message broker.
     * The message broker is responsible for routing messages from one client to another.
     * '/topic' is defined as a public channel for broadcasting messages.
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic");
        config.setApplicationDestinationPrefixes("/app");
    }

    /**
     * Registers the STOMP endpoint.
     * The endpoint is the URL that clients will connect to.
     * Crucially, we configure CORS for this endpoint here.
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // This is the key fix for the entire communication issue!
        // We configure CORS directly on the WebSocket endpoint, allowing our Vue frontend to connect.
        // 这是解决所有通信问题的关键修复！
        // 我们直接在WebSocket端点上配置CORS，允许我们的Vue前端进行连接。
        registry.addEndpoint("/ws")
                .setAllowedOrigins(CorsConstants.ALLOWED_ORIGINS) // 统一的来源配置
                .withSockJS();
    }
}

