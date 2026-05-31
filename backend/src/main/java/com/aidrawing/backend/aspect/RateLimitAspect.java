package com.aidrawing.backend.aspect;

import com.aidrawing.backend.annotation.RateLimit;
import com.aidrawing.backend.service.JwtService;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Map;
import java.util.concurrent.TimeUnit;

@Aspect
@Component
public class RateLimitAspect {

    private static final Logger logger = LoggerFactory.getLogger(RateLimitAspect.class);

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    private JwtService jwtService;

    @Around("@annotation(rateLimit)")
    public Object checkRateLimit(ProceedingJoinPoint joinPoint, RateLimit rateLimit) throws Throwable {
        try {
            String userId = jwtService.getCurrentUserId();
            String ip = getClientIp();

            String identifier;
            if (userId != null) {
                identifier = "user:" + userId;
            } else if (ip != null) {
                identifier = "ip:" + ip;
            } else {
                identifier = "anonymous";
            }

            String apiKey = rateLimit.key();
            if (apiKey == null || apiKey.isBlank()) {
                apiKey = joinPoint.getSignature().getName();
            }

            String redisKey = "ratelimit:" + identifier + ":" + apiKey;
            int maxRequests = rateLimit.maxRequests();
            int windowSeconds = rateLimit.timeWindowSeconds();

            Long currentCount = redisTemplate.opsForValue().increment(redisKey);
            if (currentCount != null && currentCount == 1) {
                redisTemplate.expire(redisKey, windowSeconds, TimeUnit.SECONDS);
            }

            if (currentCount != null && currentCount > maxRequests) {
                ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
                if (attrs != null) {
                    attrs.getResponse().setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                    attrs.getResponse().setContentType("application/json;charset=UTF-8");
                    attrs.getResponse().getWriter().write(
                        "{\"message\":\"请求过于频繁，请" + windowSeconds + "秒后再试\",\"status\":429}"
                    );
                }
                logger.warn("Rate limit exceeded: key={}, count={}, max={}", redisKey, currentCount, maxRequests);
                return null;
            }

        } catch (Exception e) {
            logger.warn("Rate limit check failed (failing open): {}", e.getMessage());
        }

        return joinPoint.proceed();
    }

    private String getClientIp() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) return null;
        jakarta.servlet.http.HttpServletRequest request = attrs.getRequest();
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
