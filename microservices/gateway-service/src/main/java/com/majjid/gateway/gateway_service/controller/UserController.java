package com.majjid.gateway.gateway_service.controller;

import com.majjid.gateway.gateway_service.utils.SecurityUtils;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class UserController {

    @GetMapping("/api/me")
    public Map<String, Object> currentUser() {

    return SecurityUtils.getCurrentUserInfo();
    }
}
