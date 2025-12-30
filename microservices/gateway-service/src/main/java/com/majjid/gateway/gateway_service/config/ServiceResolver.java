package com.majjid.gateway.gateway_service.config;

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.stereotype.Component;

import java.net.URI;

/**
 * Service Resolver that uses LoadBalancerClient to resolve service names
 * to actual host:port URIs via Eureka.
 *
 * This is needed because Spring Cloud Gateway MVC's RestClientProxyExchange
 * does not automatically integrate with Spring Cloud LoadBalancer for the lb://
 * scheme.
 */
@Component
public class ServiceResolver {

    private final LoadBalancerClient loadBalancerClient;

    public ServiceResolver(LoadBalancerClient loadBalancerClient) {
        this.loadBalancerClient = loadBalancerClient;
    }

    /**
     * Resolves a service name to an actual URI using Eureka service discovery.
     *
     * @param serviceName The logical service name (e.g., "product-service")
     * @param path        The path to append (e.g., "/api/product")
     * @return The resolved URI (e.g., "http://192.168.1.100:8080/api/product")
     * @throws IllegalStateException if the service is not found in Eureka
     */
    public URI resolve(String serviceName, String path) {
        ServiceInstance instance = loadBalancerClient.choose(serviceName);
        if (instance == null) {
            throw new IllegalStateException("No instance available for service: " + serviceName);
        }
        return URI.create(instance.getUri().toString() + path);
    }
}
