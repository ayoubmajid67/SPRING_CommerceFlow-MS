package com.majjid.gateway.gateway_service.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.function.HandlerFilterFunction;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;

/**
 * Custom filter that wraps route handlers to capture and store exceptions
 * in request attributes for access by the fallback route.
 * 
 * This is useful because Spring Cloud Gateway Server MVC's built-in
 * CircuitBreakerFilterFunctions doesn't automatically store exceptions
 * in request attributes when forwarding to a fallback route.
 */
public class CircuitBreakerExceptionFilter {

    private static final Logger log = LoggerFactory.getLogger(CircuitBreakerExceptionFilter.class);

    /**
     * Attribute key used to store the circuit breaker exception.
     * This key can be used by the fallback route to retrieve the exception.
     */
    public static final String CIRCUIT_BREAKER_EXCEPTION_ATTR = "circuitBreaker.exception";

    /**
     * Attribute key used to store the original service name.
     */
    public static final String ORIGINAL_SERVICE_ATTR = "circuitBreaker.originalService";

    /**
     * Attribute key used to store the original request path.
     */
    public static final String ORIGINAL_PATH_ATTR = "circuitBreaker.originalPath";

    /**
     * Attribute key used to store the original HTTP method.
     */
    public static final String ORIGINAL_METHOD_ATTR = "circuitBreaker.originalMethod";

    /**
     * Creates a filter that captures exception details and stores them in request
     * attributes.
     * Use this filter BEFORE the circuit breaker filter in the chain.
     *
     * @param serviceName The name of the service being called
     * @return A HandlerFilterFunction that captures request context
     */
    public static HandlerFilterFunction<ServerResponse, ServerResponse> captureContext(String serviceName) {
        return (request, next) -> {
            // Store original request context in servlet request attributes
            // These will survive the forward to the fallback route
            jakarta.servlet.http.HttpServletRequest servletRequest = request.servletRequest();
            servletRequest.setAttribute(ORIGINAL_SERVICE_ATTR, serviceName);
            servletRequest.setAttribute(ORIGINAL_PATH_ATTR, request.requestPath().pathWithinApplication().value());
            servletRequest.setAttribute(ORIGINAL_METHOD_ATTR, request.method().name());

            try {
                return next.handle(request);
            } catch (Exception e) {
                // Store the exception for access by the fallback route
                servletRequest.setAttribute(CIRCUIT_BREAKER_EXCEPTION_ATTR, e);
                log.debug("Captured exception for service {}: {}", serviceName, e.getMessage());
                throw e; // Re-throw so circuit breaker can handle it
            }
        };
    }

    /**
     * Helper method to extract the stored exception from a request.
     *
     * @param request The server request (from fallback route)
     * @return The stored exception, or null if not found
     */
    public static Throwable getException(ServerRequest request) {
        try {
            Object exception = request.servletRequest().getAttribute(CIRCUIT_BREAKER_EXCEPTION_ATTR);
            if (exception instanceof Throwable) {
                return (Throwable) exception;
            }
        } catch (Exception e) {
            log.debug("Could not retrieve exception from request: {}", e.getMessage());
        }
        return null;
    }

    /**
     * Helper method to extract the original service name from a request.
     *
     * @param request The server request (from fallback route)
     * @return The original service name, or null if not found
     */
    public static String getOriginalService(ServerRequest request) {
        try {
            Object service = request.servletRequest().getAttribute(ORIGINAL_SERVICE_ATTR);
            if (service instanceof String) {
                return (String) service;
            }
        } catch (Exception e) {
            log.debug("Could not retrieve service name from request: {}", e.getMessage());
        }
        return null;
    }

    /**
     * Helper method to extract the original request path from a request.
     *
     * @param request The server request (from fallback route)
     * @return The original path, or null if not found
     */
    public static String getOriginalPath(ServerRequest request) {
        try {
            Object path = request.servletRequest().getAttribute(ORIGINAL_PATH_ATTR);
            if (path instanceof String) {
                return (String) path;
            }
        } catch (Exception e) {
            log.debug("Could not retrieve path from request: {}", e.getMessage());
        }
        return null;
    }

    /**
     * Helper method to extract the original HTTP method from a request.
     *
     * @param request The server request (from fallback route)
     * @return The original HTTP method, or null if not found
     */
    public static String getOriginalMethod(ServerRequest request) {
        try {
            Object method = request.servletRequest().getAttribute(ORIGINAL_METHOD_ATTR);
            if (method instanceof String) {
                return (String) method;
            }
        } catch (Exception e) {
            log.debug("Could not retrieve method from request: {}", e.getMessage());
        }
        return null;
    }
}
