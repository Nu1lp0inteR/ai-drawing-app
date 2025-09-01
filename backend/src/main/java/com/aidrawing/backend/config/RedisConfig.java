package com.aidrawing.backend.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

/**
 * Redis 缓存配置类
 * 
 * 企业级缓存策略说明：
 * 1. 使用JSON序列化器确保跨平台兼容性
 * 2. 配置合理的过期时间避免内存泄漏
 * 3. 支持多种缓存策略（画廊缓存、作品详情缓存等）
 */
@Configuration
@EnableCaching
public class RedisConfig {

    /**
     * 配置Redis模板
     * 使用Jackson2JsonRedisSerializer进行对象的序列化和反序列化
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        
        // Key的序列化方式
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        
        // Value的序列化方式 - 使用JSON格式
        GenericJackson2JsonRedisSerializer serializer = new GenericJackson2JsonRedisSerializer();
        template.setValueSerializer(serializer);
        template.setHashValueSerializer(serializer);
        
        template.afterPropertiesSet();
        return template;
    }

    /**
     * 配置缓存管理器
     * 为不同类型的数据设置不同的过期时间
     */
    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        // 默认缓存配置 - 10分钟过期
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(10))
                .serializeKeysWith(org.springframework.data.redis.serializer.RedisSerializationContext.SerializationPair
                        .fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(org.springframework.data.redis.serializer.RedisSerializationContext.SerializationPair
                        .fromSerializer(new GenericJackson2JsonRedisSerializer()));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                // 为画廊设置较短的过期时间，因为需要实时性
                .withCacheConfiguration("galleryCache", 
                    defaultConfig.entryTtl(Duration.ofMinutes(5)))
                // 为作品详情设置较长的过期时间，因为变化频率低
                .withCacheConfiguration("drawingDetailsCache", 
                    defaultConfig.entryTtl(Duration.ofHours(1)))
                .build();
    }
}


