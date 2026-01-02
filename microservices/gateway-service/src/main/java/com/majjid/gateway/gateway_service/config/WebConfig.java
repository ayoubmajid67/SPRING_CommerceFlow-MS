package com.majjid.gateway.gateway_service.config;



import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig {

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**")              // match all endpoints recursively
                        .allowedOrigins("http://localhost:4200")  // allow this origin
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS") // allow common HTTP methods
                        .allowedHeaders("*")             // allow any headers
                        .allowCredentials(true);         // allow cookies, authorization headers, etc.
            }
        };
    }
}