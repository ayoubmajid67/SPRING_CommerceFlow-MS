package com.majjid.gateway.gateway_service.filter;

import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.function.HandlerFilterFunction;
import org.springframework.web.servlet.function.ServerResponse;

public class Resilience4jRetryFilter {

    private static final Logger log = LoggerFactory.getLogger(Resilience4jRetryFilter.class);

    public static HandlerFilterFunction<ServerResponse, ServerResponse> retry(String retryName,
            RetryRegistry retryRegistry) {
        return (request, next) -> {
            // 1️⃣ Get the Retry instance
            Retry retry = retryRegistry.retry(retryName);

            // 2️⃣ Register an event listener to log every retry attempt
            retry.getEventPublisher()
                    .onRetry(event -> log.info(
                            "Retry attempt #{} for {} due to exception: {}",
                            event.getNumberOfRetryAttempts(),
                            retryName,
                            event.getLastThrowable() != null ? event.getLastThrowable().toString() : "none"));

            // 3️⃣ Execute the request with retries
            try {
                return retry.executeCallable(() -> {
                    int attempt = (int) retry.getMetrics().getNumberOfFailedCallsWithRetryAttempt() + 1;

                    log.info("Executing request for {} (attempt #{})", retryName, attempt);
                    ServerResponse response = next.handle(request);

                    // 1. Map 5xx Server Errors to CustomAppException
                    if (response.statusCode().is5xxServerError()) {
                        throw new com.majjid.gateway.gateway_service.config.CustomAppException(
                                org.springframework.http.HttpStatus.resolve(response.statusCode().value()),
                                "Upstream service returned server error: " + response.statusCode());
                    }

                    return response;
                });
            } catch (Exception e) {
                log.error("All retries exhausted for {}", retryName);
                if (e instanceof RuntimeException) {
                    throw e;
                }
                throw new RuntimeException(e);
            }
        };
    }

}
