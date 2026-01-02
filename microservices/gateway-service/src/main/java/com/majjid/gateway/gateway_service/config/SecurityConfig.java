package com.majjid.gateway.gateway_service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

        @Bean
        public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
                http
                                // ✅ Enable CORS - uses configuration from WebConfig
                                .cors(Customizer.withDefaults())
                                // ✅ Disable CSRF for REST APIs (stateless)
                                .csrf(csrf -> csrf.disable())
                                .authorizeHttpRequests(auth -> auth

                                                // ===== Public endpoints =====
                                                .requestMatchers(
                                                                "/swagger-ui/**",
                                                                "/swagger-ui.html",
                                                                "/v3/api-docs/**",
                                                                "/aggregate/**")
                                                .permitAll()

                                                // ✅ PUBLIC: Product GET requests
                                                .requestMatchers(HttpMethod.GET, "/api/product/**").permitAll()

                                                // 🔐 ADMIN only: Product write operations
                                                .requestMatchers(HttpMethod.POST, "/api/product/**").hasRole("ADMIN")
                                                .requestMatchers(HttpMethod.PUT, "/api/product/**").hasRole("ADMIN")
                                                .requestMatchers(HttpMethod.DELETE, "/api/product/**").hasRole("ADMIN")

                                                // 🔐 Authenticated users: Orders (place, view, cancel own orders)
                                                .requestMatchers("/api/order/**").authenticated()
                                                .requestMatchers(HttpMethod.POST, "/api/order/**").hasRole("USER")

                                                // 🔐 ADMIN only: Inventory - all operations
                                                .requestMatchers("/api/inventory/**").hasRole("ADMIN")

                                                // 🔐 Authenticated: User info
                                                .requestMatchers("/api/me").authenticated()

                                                .anyRequest().denyAll())
                                .oauth2ResourceServer(oauth2 -> oauth2.jwt(
                                                jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())));

                return http.build();
        }

        @Bean
        public JwtAuthenticationConverter jwtAuthenticationConverter() {
                JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
                converter.setJwtGrantedAuthoritiesConverter(new KeycloakRoleConverter());
                return converter;
        }
}
