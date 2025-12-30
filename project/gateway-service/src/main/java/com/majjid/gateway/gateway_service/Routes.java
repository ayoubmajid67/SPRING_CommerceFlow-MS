package com.majjid.gateway.gateway_service;

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

@Configuration
public class Routes {

        private static final Logger log = LoggerFactory.getLogger(Routes.class);

        private final RestTemplate restTemplate = new RestTemplate();

        // ==========================================
        // OpenAPI Documentation Routes (Using RestTemplate)
        // ==========================================
        // Since HandlerFunctions.http() isn't working, we use RestTemplate directly

        @Bean
        @Order(1)
        public RouterFunction<ServerResponse> swaggerRoutes() {
                log.info(">>> Registering swagger-routes bean with RestTemplate approach");

                return RouterFunctions.route()
                                // Product Service API Docs
                                .GET("/aggregate/product/v3/api-docs", request -> {
                                        log.info(">>> [SWAGGER] Fetching Product API docs from http://localhost:8080/v3/api-docs");
                                        String response = restTemplate.getForObject("http://localhost:8080/v3/api-docs",
                                                        String.class);
                                        return ServerResponse.ok()
                                                        .contentType(MediaType.APPLICATION_JSON)
                                                        .body(response);
                                })
                                .GET("/aggregate/product/v3/api-docs/{*path}", request -> {
                                        String subPath = request.pathVariable("path");
                                        String targetUrl = "http://localhost:8080/v3/api-docs/" + subPath;
                                        log.info(">>> [SWAGGER] Fetching Product API docs from {}", targetUrl);
                                        String response = restTemplate.getForObject(targetUrl, String.class);
                                        return ServerResponse.ok()
                                                        .contentType(MediaType.APPLICATION_JSON)
                                                        .body(response);
                                })

                                // Order Service API Docs
                                .GET("/aggregate/order/v3/api-docs", request -> {
                                        log.info(">>> [SWAGGER] Fetching Order API docs from http://localhost:8081/v3/api-docs");
                                        String response = restTemplate.getForObject("http://localhost:8081/v3/api-docs",
                                                        String.class);
                                        return ServerResponse.ok()
                                                        .contentType(MediaType.APPLICATION_JSON)
                                                        .body(response);
                                })
                                .GET("/aggregate/order/v3/api-docs/{*path}", request -> {
                                        String subPath = request.pathVariable("path");
                                        String targetUrl = "http://localhost:8081/v3/api-docs/" + subPath;
                                        log.info(">>> [SWAGGER] Fetching Order API docs from {}", targetUrl);
                                        String response = restTemplate.getForObject(targetUrl, String.class);
                                        return ServerResponse.ok()
                                                        .contentType(MediaType.APPLICATION_JSON)
                                                        .body(response);
                                })

                                // Inventory Service API Docs
                                .GET("/aggregate/inventory/v3/api-docs", request -> {
                                        log.info(">>> [SWAGGER] Fetching Inventory API docs from http://localhost:8082/v3/api-docs");
                                        String response = restTemplate.getForObject("http://localhost:8082/v3/api-docs",
                                                        String.class);
                                        return ServerResponse.ok()
                                                        .contentType(MediaType.APPLICATION_JSON)
                                                        .body(response);
                                })
                                .GET("/aggregate/inventory/v3/api-docs/{*path}", request -> {
                                        String subPath = request.pathVariable("path");
                                        String targetUrl = "http://localhost:8082/v3/api-docs/" + subPath;
                                        log.info(">>> [SWAGGER] Fetching Inventory API docs from {}", targetUrl);
                                        String response = restTemplate.getForObject(targetUrl, String.class);
                                        return ServerResponse.ok()
                                                        .contentType(MediaType.APPLICATION_JSON)
                                                        .body(response);
                                })
                                .build();
        }

        // ==========================================
        // Main Service Routes (Using GatewayRouterFunctions)
        // ==========================================

        @Bean
        @Order(2)
        public RouterFunction<ServerResponse> productServiceRoute() {
                return GatewayRouterFunctions.route("product-service")
                                .route(RequestPredicates.path("/api/product/**"),
                                                request -> {
                                                        MvcUtils.setRequestUrl(request,
                                                                        URI.create("http://localhost:8080" +
                                                                                        request.requestPath()
                                                                                                        .pathWithinApplication()));
                                                        return HandlerFunctions.http().handle(request);
                                                })
                                .build();
        }

        @Bean
        @Order(3)
        public RouterFunction<ServerResponse> orderServiceRoute() {
                return GatewayRouterFunctions.route("order-service")
                                .route(RequestPredicates.path("/api/order/**"),
                                                request -> {
                                                        MvcUtils.setRequestUrl(request,
                                                                        URI.create("http://localhost:8081" +
                                                                                        request.requestPath()
                                                                                                        .pathWithinApplication()));
                                                        return HandlerFunctions.http().handle(request);
                                                })
                                .build();
        }

        @Bean
        @Order(4)
        public RouterFunction<ServerResponse> inventoryServiceRoute() {
                return GatewayRouterFunctions.route("inventory-service")
                                .route(RequestPredicates.path("/api/inventory/**"),
                                                request -> {
                                                        MvcUtils.setRequestUrl(request,
                                                                        URI.create("http://localhost:8082" +
                                                                                        request.requestPath()
                                                                                                        .pathWithinApplication()));
                                                        return HandlerFunctions.http().handle(request);
                                                })
                                .build();
        }
}