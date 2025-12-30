package com.majjid.gateway.gateway_service.config;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * Configuration for RestTemplate used by Routes.java
 * to proxy API documentation from microservices.
 * 
 * @LoadBalanced enables service discovery - allows using service names
 *               like "http://product-service" instead of
 *               "http://localhost:8080"
 */
@Configuration
public class RestTemplateConfig {

    /**
     * Load-balanced RestTemplate that resolves service names via Eureka.
     * Example: http://product-service/v3/api-docs
     * Eureka will resolve "product-service" to the actual host:port.
     */
    @Bean
    @LoadBalanced
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
