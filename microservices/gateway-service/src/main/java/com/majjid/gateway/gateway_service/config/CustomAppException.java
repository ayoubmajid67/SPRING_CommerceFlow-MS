package com.majjid.gateway.gateway_service.config;

import org.springframework.http.HttpStatus;

public class CustomAppException extends RuntimeException {
    private final HttpStatus status;

    public static String buildNotFoundMsg(Object objectId, String objectType) {
        return "The " + objectType + " with id " + objectId + " does not exist";
    }

    public static String buildAlreadyExistsMsg(Object objectId, String objectType) {
        return "The " + objectType + " with id " + objectId + " already exists";
    }

    public CustomAppException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
