package com.aidrawing.backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Configuration class to enable asynchronous processing.
 * This allows @Async annotation to work on methods, executing them in a separate thread.
 */
@Configuration
@EnableAsync
public class AsyncConfig {
}
