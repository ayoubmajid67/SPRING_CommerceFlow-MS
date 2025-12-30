package com.majjid.gateway.gateway_service.controller;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

/**
 * Controller to aggregate OpenAPI documentation from all microservices.
 * This enables the Swagger UI dropdown to select between different services.
 */
@RestController
@RequestMapping("/aggregate")
public class SwaggerAggregationController {

    private final RestTemplate restTemplate;

    public SwaggerAggregationController(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @GetMapping(value = "/product/v3/api-docs", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> getProductApiDocs() {
        String apiDocs = restTemplate.getForObject("http://localhost:8080/v3/api-docs", String.class);
        return ResponseEntity.ok(apiDocs);
    }

    @GetMapping(value = "/order/v3/api-docs", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> getOrderApiDocs() {
        String apiDocs = restTemplate.getForObject("http://localhost:8081/v3/api-docs", String.class);
        return ResponseEntity.ok(apiDocs);
    }

    @GetMapping(value = "/inventory/v3/api-docs", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> getInventoryApiDocs() {
        String apiDocs = restTemplate.getForObject("http://localhost:8082/v3/api-docs", String.class);
        return ResponseEntity.ok(apiDocs);
    }
}
