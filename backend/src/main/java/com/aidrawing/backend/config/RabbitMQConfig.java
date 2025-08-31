package com.aidrawing.backend.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration class for RabbitMQ.
 * Defines the necessary queues, exchanges, and bindings.
 * * RabbitMQ 的配置类。
 * 定义了必要的队列、交换机和绑定。
 */
@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE_NAME = "drawing_exchange";
    public static final String QUEUE_NAME = "drawing_task_queue";
    public static final String ROUTING_KEY = "drawing.task.new";

    /**
     * Declares the exchange.
     * An exchange is responsible for routing the messages to different queues.
     * * 声明交换机。
     * 交换机负责将消息路由到不同的队列。
     */
    @Bean
    public TopicExchange drawingExchange() {
        return new TopicExchange(EXCHANGE_NAME);
    }

    /**
     * Declares the queue.
     * A queue is a buffer that stores messages.
     * * 声明队列。
     * 队列是存储消息的缓冲区。
     */
    @Bean
    public Queue drawingTaskQueue() {
        return new Queue(QUEUE_NAME, true); // durable=true
    }

    /**
     * Binds the queue to the exchange with a routing key.
     * Messages sent to the exchange with this routing key will be routed to this queue.
     * * 使用路由键将队列绑定到交换机。
     * 发送到该交换机并带有此路由键的消息将被路由到此队列。
     */
    @Bean
    public Binding binding(Queue queue, TopicExchange exchange) {
        return BindingBuilder.bind(queue).to(exchange).with(ROUTING_KEY);
    }
}
