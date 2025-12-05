package com.majjid.microservices.order.config;



import org.springframework.http.HttpStatus;

public class CustomAppException extends RuntimeException {
    private final HttpStatus status;
   public  static String buildNotFoundMsg(Object objectId,String ObjectType){
       return "The "+ObjectType+" with id " + objectId + " does not exist";
   }
   public static String BuildAlreadyExistsMsg(Object objectId,String ObjectType){
       return "The "+ObjectType+" with id " + objectId + " already exists";
   }
    public CustomAppException(HttpStatus status, String message) {
        super(message);
        this.status = status;

    }
    public HttpStatus getStatus() {
        return status;
    }

}
