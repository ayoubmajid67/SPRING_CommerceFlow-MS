package com.majjid.gateway.gateway_service.utils;


import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;


import java.util.Map;

public class SecurityUtils {

    public static Map<String, Object> getCurrentUserInfo() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated() || auth.getPrincipal().equals("anonymousUser")) {
            return Map.of(
                    "authenticated", false
            );
        }

        Jwt jwt = (Jwt) auth.getPrincipal();

        return Map.of(
                "authenticated", true,
                "id", jwt.getSubject(),
                "username", jwt.getClaimAsString("preferred_username"),
                "email", jwt.getClaimAsString("email"),
                "roles", auth.getAuthorities()
        );
    }
}
