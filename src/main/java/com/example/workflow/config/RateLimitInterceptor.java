package com.example.workflow.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
@Slf4j
public class RateLimitInterceptor implements HandlerInterceptor {

    private final RedisTemplate<String, Object> redisTemplate;
    
    // 100 requests per minute
    private static final int MAX_REQUESTS_PER_MINUTE = 100;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String ipAddress = request.getRemoteAddr();
        String key = "rate_limit:" + ipAddress;
        
        try {
            Long requests = redisTemplate.opsForValue().increment(key);
            
            if (requests != null && requests == 1) {
                redisTemplate.expire(key, Duration.ofMinutes(1));
            }
            
            if (requests != null && requests > MAX_REQUESTS_PER_MINUTE) {
                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                response.getWriter().write("Too many requests. Please try again later.");
                return false;
            }
        } catch (Exception e) {
            // If Redis goes down, we fallback to allowing the request
            log.warn("Redis is down. Bypassing rate limiter.", e);
        }
        
        return true;
    }
}
