package com.majjid.microservices.product.Dto;

import lombok.Data;
import org.springframework.http.HttpStatus;

import javax.management.ObjectName;

@Data
public class ResponseDto<T> {
    private T data;
    private String message;
    private HttpStatus status;
    private boolean success;

    // Private constructor for builder
    private ResponseDto(T data, String message, HttpStatus status, boolean success) {
        this.data = data;
        this.message = message;
        this.status = status;
        this.success = success;
    }

    // Static factory methods
    public static <T> ResponseDto<T> success(T data) {
        return new ResponseDto<>(data, "Operation completed successfully", HttpStatus.OK, true);
    }

    public static <T> ResponseDto<T> success(T data, String message) {
        return new ResponseDto<>(data, message, HttpStatus.OK, true);
    }

    public static <T> ResponseDto<T> retrieved(T data, String objectName) {
        return new ResponseDto<>(data, "The " + objectName + " retrieved with success", HttpStatus.OK, true);
    }
    //
    public static <T> ResponseDto<T> listed(T data, String objectName) {
        return new ResponseDto<>(data, "The " + objectName + " listed with success", HttpStatus.OK, true);
    }



    public static <T> ResponseDto<T> created(T data, String objectName) {
        return new ResponseDto<>(data, "The " + objectName + " created with success", HttpStatus.CREATED, true);
    }
    public static <T> ResponseDto<T> updated(T data, String objectName) {
        return new ResponseDto<>(data, "The " + objectName + " updated with success", HttpStatus.OK, true);
    }
    public static <T> ResponseDto<T> deleted(T data, String objectName) {
        return new ResponseDto<>(data, "The " + objectName + " deleted with success", HttpStatus.OK, true);
    }
}