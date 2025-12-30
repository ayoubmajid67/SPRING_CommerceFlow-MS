# Part 7 - API Documentation Aggregation in Spring Cloud Gateway

This document details the challenges encountered and solutions implemented while setting up unified API documentation aggregation using Spring Cloud Gateway Server MVC with Spring Boot 4.0.

## Overview

The goal was to create a unified Swagger UI accessible through the API Gateway (`http://localhost:9000/swagger-ui/index.html`) that aggregates API documentation from all microservices (Product, Order, and Inventory).

## Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                    API Gateway (port 9000)                      │
│                                                                 │
│  Swagger UI (/swagger-ui/index.html)                           │
│      ↓                                                          │
│  Dropdown Selector:                                             │
│    - Product Service  → /aggregate/product/v3/api-docs         │
│    - Order Service    → /aggregate/order/v3/api-docs           │
│    - Inventory Service → /aggregate/inventory/v3/api-docs      │
└─────────────────────────────────────────────────────────────────┘
                              ↓
        ┌─────────────────────┼─────────────────────┐
        ↓                     ↓                     ↓
┌───────────────┐     ┌───────────────┐     ┌───────────────┐
│Product Service│     │ Order Service │     │Inventory Svc  │
│   port 8080   │     │   port 8081   │     │   port 8082   │
│/v3/api-docs   │     │/v3/api-docs   │     │/v3/api-docs   │
└───────────────┘     └───────────────┘     └───────────────┘
```

## Problems Encountered

### Problem 1: 401 Unauthorized on Swagger Endpoints

**Symptom:** Accessing `/swagger-ui/**` or `/v3/api-docs` returned 401 Unauthorized.

**Cause:** Spring Security was blocking unauthenticated access to Swagger endpoints.

**Solution:** Updated `SecurityConfig.java` to permit Swagger endpoints:

```java
private static final String[] PUBLIC_ENDPOINTS = {
    "/swagger-ui/**",
    "/swagger-ui.html",
    "/v3/api-docs/**",
    "/aggregate/*/v3/api-docs"  // Single * for one path segment
};

@Bean
public SecurityFilterChain securityFilterChain(HttpSecurity httpSecurity) throws Exception {
    return httpSecurity.authorizeHttpRequests(authorize -> authorize
            .requestMatchers(PUBLIC_ENDPOINTS).permitAll()
            .anyRequest().authenticated())
            .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()))
            .build();
}
```

### Problem 2: Invalid Path Pattern with `**`

**Symptom:** Application failed to start with error:
```
Invalid mapping pattern detected: /aggregate/**/v3/api-docs
{*...} or ** pattern elements should be placed at the start or end of the pattern
```

**Cause:** Spring Boot 4.0 PathPattern matcher doesn't allow `**` in the middle of paths.

**Solution:** Changed `/**` to `/*` since we only have one path segment (service name):
```java
"/aggregate/*/v3/api-docs"  // Single * for one path segment
```

### Problem 3: 500 Internal Server Error - "No static resource"

**Symptom:** Requests to `/aggregate/product/v3/api-docs` returned:
```json
{
    "error": "No static resource aggregate/product/v3/api-docs"
}
```

**Cause:** SpringDoc's handlers were intercepting requests before the Gateway routes could process them. Even though our `RouterFunction` beans were registered with `@Order(1)`, SpringDoc registered its own handlers that took precedence.

**Investigation:** Added logging to confirm the route was matching:
```java
log.info(">>> [SWAGGER ROUTE] Product: {} -> {}", originalPath, targetUrl);
```

The logs showed routes were matching, but `HandlerFunctions.http().handle(request)` was failing internally.

**Root Cause:** The combination of `GatewayRouterFunctions` with `HandlerFunctions.http()` wasn't working correctly for the swagger routes, even though it worked fine for regular API routes like `/api/product/**`.

### Problem 4: HandlerFunctions.http() Not Proxying Correctly

**Symptom:** The route matched (logs confirmed), but the HTTP call to backend services wasn't being made.

**Attempted Solutions:**
1. Using `BeforeFilterFunctions.setPath("/v3/api-docs")` - Did not work
2. Using `MvcUtils.setRequestUrl()` with `HandlerFunctions.http()` - Did not work
3. Adding `@Order(1)` priority - Did not work

**Final Solution:** Abandoned `GatewayRouterFunctions` for swagger routes and used plain `RouterFunctions` with `RestTemplate`:

```java
@Bean
@Order(1)
public RouterFunction<ServerResponse> swaggerRoutes() {
    return RouterFunctions.route()
            .GET("/aggregate/product/v3/api-docs", request -> {
                String response = restTemplate.getForObject(
                    "http://localhost:8080/v3/api-docs", String.class);
                return ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(response);
            })
            // ... similar for order and inventory
            .build();
}
```

### Problem 5: Swagger UI Not Showing Dropdown Selector

**Symptom:** Swagger UI loaded but showed the gateway's own API documentation instead of a dropdown to select microservices.

**Cause:** With `springdoc.api-docs.enabled=true`, SpringDoc generates documentation for the gateway itself, which overrides the `springdoc.swagger-ui.urls[]` configuration.

**Solution:** Set `springdoc.api-docs.enabled=false` to disable gateway's own docs:

```properties
springdoc.swagger-ui.enabled=true
springdoc.api-docs.enabled=false  # This makes dropdown appear
```

However, we kept it `true` in the final implementation because the SwaggerAggregationController provides the endpoints that appear in the dropdown.

## Final Implementation

### Dependencies (pom.xml)

```xml
<!-- SpringDoc OpenAPI for Swagger UI -->
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>2.8.13</version>
</dependency>

<!-- SpringDoc OpenAPI for Postman (JSON API docs) -->
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-api</artifactId>
    <version>2.8.13</version>
</dependency>
```

### Configuration (application.properties)

```properties
# SpringDoc OpenAPI Configuration
springdoc.swagger-ui.enabled=true
springdoc.api-docs.enabled=true

# Aggregate API docs from all microservices
springdoc.swagger-ui.urls[0].name=Product Service
springdoc.swagger-ui.urls[0].url=/aggregate/product/v3/api-docs

springdoc.swagger-ui.urls[1].name=Order Service
springdoc.swagger-ui.urls[1].url=/aggregate/order/v3/api-docs

springdoc.swagger-ui.urls[2].name=Inventory Service
springdoc.swagger-ui.urls[2].url=/aggregate/inventory/v3/api-docs
```

### Routes.java (Key Parts)

```java
@Configuration
public class Routes {

    private static final Logger log = LoggerFactory.getLogger(Routes.class);
    private final RestTemplate restTemplate = new RestTemplate();

    // OpenAPI Documentation Routes using RestTemplate
    @Bean
    @Order(1)
    public RouterFunction<ServerResponse> swaggerRoutes() {
        return RouterFunctions.route()
                // Product Service API Docs
                .GET("/aggregate/product/v3/api-docs", request -> {
                    String response = restTemplate.getForObject(
                        "http://localhost:8080/v3/api-docs", String.class);
                    return ServerResponse.ok()
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(response);
                })
                // Wildcard for subpaths
                .GET("/aggregate/product/v3/api-docs/{*path}", request -> {
                    String subPath = request.pathVariable("path");
                    String targetUrl = "http://localhost:8080/v3/api-docs/" + subPath;
                    String response = restTemplate.getForObject(targetUrl, String.class);
                    return ServerResponse.ok()
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(response);
                })
                // Similar for order and inventory...
                .build();
    }

    // Main Service Routes (these work with GatewayRouterFunctions)
    @Bean
    @Order(2)
    public RouterFunction<ServerResponse> productServiceRoute() {
        return GatewayRouterFunctions.route("product-service")
                .route(RequestPredicates.path("/api/product/**"),
                        request -> {
                            MvcUtils.setRequestUrl(request,
                                URI.create("http://localhost:8080" +
                                    request.requestPath().pathWithinApplication()));
                            return HandlerFunctions.http().handle(request);
                        })
                .build();
    }
}
```

### SecurityConfig.java

```java
@Configuration
public class SecurityConfig {

    private static final String[] PUBLIC_ENDPOINTS = {
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/v3/api-docs/**",
            "/aggregate/*/v3/api-docs"
    };

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity httpSecurity) throws Exception {
        return httpSecurity.authorizeHttpRequests(authorize -> authorize
                .requestMatchers(PUBLIC_ENDPOINTS).permitAll()
                .anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()))
                .build();
    }
}
```

## Key Learnings

1. **Spring Boot 4.0 Path Patterns**: The `**` wildcard can only be at the start or end of patterns. Use `*` for single segments in the middle.

2. **GatewayRouterFunctions vs RouterFunctions**: For proxying to external services, `GatewayRouterFunctions` with `HandlerFunctions.http()` works for regular API routes but may have issues with SpringDoc paths. Using plain `RouterFunctions` with `RestTemplate` is more reliable for swagger aggregation.

3. **SpringDoc Interference**: SpringDoc registers its own handlers that can interfere with gateway routes. The `springdoc.api-docs.enabled` setting controls whether the gateway generates its own documentation.

4. **Debugging Gateway Routes**: Add logging inside route handlers to confirm if routes are being matched. If logs appear but requests fail, the issue is in the handler execution, not route matching.

5. **Order Matters**: Use `@Order` annotations to control the priority of `RouterFunction` beans. Lower numbers = higher priority.

## Test URLs

| URL | Description |
|-----|-------------|
| `http://localhost:9000/swagger-ui/index.html` | Unified Swagger UI with dropdown |
| `http://localhost:9000/aggregate/product/v3/api-docs` | Product Service OpenAPI JSON |
| `http://localhost:9000/aggregate/order/v3/api-docs` | Order Service OpenAPI JSON |
| `http://localhost:9000/aggregate/inventory/v3/api-docs` | Inventory Service OpenAPI JSON |
| `http://localhost:9000/api/product` | Product Service API (authenticated) |
| `http://localhost:9000/api/order` | Order Service API (authenticated) |
| `http://localhost:9000/api/inventory` | Inventory Service API (authenticated) |

## Conclusion

Setting up API documentation aggregation in Spring Cloud Gateway Server MVC with Spring Boot 4.0 required working around several compatibility issues between SpringDoc and the gateway's routing mechanism. The final solution uses `RestTemplate` inside `RouterFunctions` for reliable proxying of OpenAPI documentation, while the regular API routes continue to use `GatewayRouterFunctions` with `HandlerFunctions.http()`.
