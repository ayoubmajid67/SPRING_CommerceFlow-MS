package com.majjid.gateway.gateway_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
/**
 *
 * The app registers its name + host + port in Eureka
 *
 * The app can query Eureka to find other services
 *
 * Enables client-side load balancing (lb://)
 */

@EnableDiscoveryClient
public class GatewayServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(GatewayServiceApplication.class, args);
	}

}
