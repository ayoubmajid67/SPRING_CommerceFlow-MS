package com.majjid.gateway.gateway_service.filter;

import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;

/**
 * Rate Limiter Filter for Spring Cloud Gateway Server MVC
 * 
 * This filter applies rate limiting to all incoming requests using Resilience4j.
 * It uses the configuration from application.properties:
 * - resilience4j.ratelimiter.configs.default.limitRefreshPeriod
 * - resilience4j.ratelimiter.configs.default.limitForPeriod
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@Slf4j
public class RateLimiterFilter extends OncePerRequestFilter {

    private final RateLimiter rateLimiter;

    public RateLimiterFilter(
            @Value("${resilience4j.ratelimiter.configs.default.limitRefreshPeriod:1s}") String limitRefreshPeriod,
            @Value("${resilience4j.ratelimiter.configs.default.limitForPeriod:10}") int limitForPeriod) {
        
        // Parse duration (e.g., "10s" -> Duration.ofSeconds(10))
        Duration refreshPeriod = parseDuration(limitRefreshPeriod);
        
        log.info(">>> Initializing Rate Limiter: {} requests per {}", limitForPeriod, refreshPeriod);
        
        RateLimiterConfig config = RateLimiterConfig.custom()
                .limitRefreshPeriod(refreshPeriod)
                .limitForPeriod(limitForPeriod)
                .timeoutDuration(Duration.ZERO) // Don't wait, reject immediately
                .build();

        RateLimiterRegistry registry = RateLimiterRegistry.of(config);
        this.rateLimiter = registry.rateLimiter("gatewayRateLimiter");
    }

    private Duration parseDuration(String duration) {
        // Handle formats like "10s", "1m", "500ms"
        if (duration.endsWith("ms")) {
            return Duration.ofMillis(Long.parseLong(duration.replace("ms", "")));
        } else if (duration.endsWith("s")) {
            return Duration.ofSeconds(Long.parseLong(duration.replace("s", "")));
        } else if (duration.endsWith("m")) {
            return Duration.ofMinutes(Long.parseLong(duration.replace("m", "")));
        }
        return Duration.ofSeconds(Long.parseLong(duration));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, 
                                    FilterChain filterChain) throws ServletException, IOException {
        
        String path = request.getRequestURI();
        
        // Only apply rate limiting to API routes
        if (path.startsWith("/api/")) {
            boolean permitted = rateLimiter.acquirePermission();
            
            if (!permitted) {
                log.warn(">>> Rate limit exceeded for request: {} {}", request.getMethod(), path);
                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                response.setContentType("application/json");
                response.getWriter().write("{\"error\": \"Too Many Requests\", \"message\": \"Rate limit exceeded. Please try again later.\"}");
                return;
            }
            
            log.debug(">>> Rate limiter: permission granted for {} {}", request.getMethod(), path);
        }
        
        filterChain.doFilter(request, response);
    }
}
