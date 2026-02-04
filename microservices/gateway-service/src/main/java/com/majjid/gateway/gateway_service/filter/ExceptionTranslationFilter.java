package com.majjid.gateway.gateway_service.filter;

import com.majjid.gateway.gateway_service.config.CustomAppException;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.servlet.function.HandlerFilterFunction;
import org.springframework.web.servlet.function.ServerResponse;

public class ExceptionTranslationFilter {

    private static final Logger log = LoggerFactory.getLogger(ExceptionTranslationFilter.class);

    /**
     * Filter that inspects the response status code and throws a CustomAppException
     * if the status represents a failure (5xx or 404).
     * This ensures that upstream Circuit Breakers and Retry filters can detect the
     * failure.
     */
    public static HandlerFilterFunction<ServerResponse, ServerResponse> checkStatus() {
        return (request, next) -> {
            ServerResponse response = next.handle(request);

            // 1. Map 5xx Server Errors to CustomAppException
            if (response.statusCode().is5xxServerError()) {
                log.error("Upstream service returned server error: {}", response.statusCode());
                throw new CustomAppException(
                        HttpStatus.resolve(response.statusCode().value()),
                        "Upstream service returned server error: " + response.statusCode());
            }

            return response;
        };
    }
}
