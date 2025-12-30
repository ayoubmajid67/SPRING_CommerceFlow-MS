package com.majjid.gateway.gateway_service;

import com.majjid.gateway.gateway_service.config.ServiceResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.server.mvc.common.MvcUtils;
import org.springframework.cloud.gateway.server.mvc.handler.GatewayRouterFunctions;
import org.springframework.cloud.gateway.server.mvc.handler.HandlerFunctions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.function.RequestPredicates;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.RouterFunctions;
import org.springframework.web.servlet.function.ServerResponse;

import java.net.URI;

/**
 * Gateway Routes Configuration
 *
 * Uses Eureka service discovery for routing:
 * - Service names (product-service, order-service, inventory-service)
 *   are resolved by Eureka via LoadBalancerClient to actual host:port
 * - No need to hardcode localhost:port
 * - Enables load balancing when multiple instances are running
 */
@Configuration
public class Routes {

    private static final Logger log = LoggerFactory.getLogger(Routes.class);

    // Service names as registered in Eureka
    private static final String PRODUCT_SERVICE = "product-service";
    private static final String ORDER_SERVICE = "order-service";
    private static final String INVENTORY_SERVICE = "inventory-service";

    // HTTP scheme versions for RestTemplate (uses http:// with @LoadBalanced)
    private static final String PRODUCT_SERVICE_HTTP = "http://product-service";
    private static final String ORDER_SERVICE_HTTP = "http://order-service";
    private static final String INVENTORY_SERVICE_HTTP = "http://inventory-service";

    // Dependencies
    private final RestTemplate restTemplate;
    private final ServiceResolver serviceResolver;

    public Routes(RestTemplate restTemplate, ServiceResolver serviceResolver) {
        this.restTemplate = restTemplate;
        this.serviceResolver = serviceResolver;
    }

    // ==========================================
    // OpenAPI Documentation Routes (Using RestTemplate with Eureka)
    // ==========================================
    // Uses @LoadBalanced RestTemplate to resolve service names via Eureka

    @Bean
    @Order(1)
    public RouterFunction<ServerResponse> swaggerRoutes() {
        log.info(">>> Registering swagger-routes with Eureka service discovery");

        return RouterFunctions.route()
                // Product Service API Docs
                .GET("/aggregate/product/v3/api-docs", request -> {
                    String url = PRODUCT_SERVICE_HTTP + "/v3/api-docs";
                    log.info(">>> [SWAGGER] Fetching Product API docs from {}", url);
                    String response = restTemplate.getForObject(url, String.class);
                    return ServerResponse.ok()
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(response);
                })
                .GET("/aggregate/product/v3/api-docs/{*path}", request -> {
                    String subPath = request.pathVariable("path");
                    String url = PRODUCT_SERVICE_HTTP + "/v3/api-docs/" + subPath;
                    log.info(">>> [SWAGGER] Fetching Product API docs from {}", url);
                    String response = restTemplate.getForObject(url, String.class);
                    return ServerResponse.ok()
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(response);
                })

                // Order Service API Docs
                .GET("/aggregate/order/v3/api-docs", request -> {
                    String url = ORDER_SERVICE_HTTP + "/v3/api-docs";
                    log.info(">>> [SWAGGER] Fetching Order API docs from {}", url);
                    String response = restTemplate.getForObject(url, String.class);
                    return ServerResponse.ok()
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(response);
                })
                .GET("/aggregate/order/v3/api-docs/{*path}", request -> {
                    String subPath = request.pathVariable("path");
                    String url = ORDER_SERVICE_HTTP + "/v3/api-docs/" + subPath;
                    log.info(">>> [SWAGGER] Fetching Order API docs from {}", url);
                    String response = restTemplate.getForObject(url, String.class);
                    return ServerResponse.ok()
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(response);
                })

                // Inventory Service API Docs
                .GET("/aggregate/inventory/v3/api-docs", request -> {
                    String url = INVENTORY_SERVICE_HTTP + "/v3/api-docs";
                    log.info(">>> [SWAGGER] Fetching Inventory API docs from {}", url);
                    String response = restTemplate.getForObject(url, String.class);
                    return ServerResponse.ok()
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(response);
                })
                .GET("/aggregate/inventory/v3/api-docs/{*path}", request -> {
                    String subPath = request.pathVariable("path");
                    String url = INVENTORY_SERVICE_HTTP + "/v3/api-docs/" + subPath;
                    log.info(">>> [SWAGGER] Fetching Inventory API docs from {}", url);
                    String response = restTemplate.getForObject(url, String.class);
                    return ServerResponse.ok()
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(response);
                })
                .build();
    }

    // ==========================================
    // Main Service Routes (Using LoadBalancerClient for service resolution)
    // ==========================================
    // Uses ServiceResolver to get actual host:port from Eureka

    @Bean
    @Order(2)
    public RouterFunction<ServerResponse> productServiceRoute() {
        log.info(">>> Registering product-service route with Eureka service discovery");
        return GatewayRouterFunctions.route("product-service")
                .route(RequestPredicates.path("/api/product/**"),
                        request -> {
                            String path = request.requestPath().pathWithinApplication().value();
                            URI resolvedUri = serviceResolver.resolve(PRODUCT_SERVICE, path);
                            log.debug(">>> Routing to product-service: {}", resolvedUri);
                            MvcUtils.setRequestUrl(request, resolvedUri);
                            return HandlerFunctions.http().handle(request);
                        })
                .build();
    }

    @Bean
    @Order(3)
    public RouterFunction<ServerResponse> orderServiceRoute() {
        log.info(">>> Registering order-service route with Eureka service discovery");
        return GatewayRouterFunctions.route("order-service")
                .route(RequestPredicates.path("/api/order/**"),
                        request -> {
                            String path = request.requestPath().pathWithinApplication().value();
                            URI resolvedUri = serviceResolver.resolve(ORDER_SERVICE, path);
                            log.debug(">>> Routing to order-service: {}", resolvedUri);
                            MvcUtils.setRequestUrl(request, resolvedUri);
                            return HandlerFunctions.http().handle(request);
                        })
                .build();
    }

    @Bean
    @Order(4)
    public RouterFunction<ServerResponse> inventoryServiceRoute() {
        log.info(">>> Registering inventory-service route with Eureka service discovery");
        return GatewayRouterFunctions.route("inventory-service")
                .route(RequestPredicates.path("/api/inventory/**"),
                        request -> {
                            String path = request.requestPath().pathWithinApplication().value();
                            URI resolvedUri = serviceResolver.resolve(INVENTORY_SERVICE, path);
                            log.debug(">>> Routing to inventory-service: {}", resolvedUri);
                            MvcUtils.setRequestUrl(request, resolvedUri);
                            return HandlerFunctions.http().handle(request);
                        })
                .build();
    }
}