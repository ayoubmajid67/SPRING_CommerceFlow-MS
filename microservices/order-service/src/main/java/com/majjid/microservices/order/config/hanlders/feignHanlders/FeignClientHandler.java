package com.majjid.microservices.order.config.hanlders.feignHanlders;

import com.majjid.microservices.order.config.CustomAppException;
import com.majjid.microservices.order.util.FeignMessageSanitizer;
import feign.FeignException;
import feign.codec.DecodeException; // Import DecodeException
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;

@Slf4j
public class FeignClientHandler {
    public static <T> T handleFeignCall(FeignCall<T> feignCall, String serviceName) {
        try {
            return feignCall.execute();
        }

        // 1. Catch the specific decoding exception first
        catch (DecodeException decodeEx) {
            log.error("Feign DECODE ERROR (Type Mismatch/Deserialization) during call to {}: {}", serviceName, decodeEx.getMessage());

            // You might need to examine the 'cause' of the DecodeException for more details
            Throwable rootCause = decodeEx.getCause();
            if (rootCause != null) {
                log.error("Root cause of DecodeException: {}", rootCause.getMessage());
            }

            // Re-throw as a general server error or a specific client error
            throw new CustomAppException(
                    HttpStatus.INTERNAL_SERVER_ERROR, // Or HttpStatus.BAD_GATEWAY (502)
                    "Internal communication error: Failed to process response from " + serviceName
            );
        }

        // 2. Catch general Feign Exceptions (for connectivity or API errors)
        catch (FeignException ex) {
            String raw = "";
            try {
                // Attempt to get the raw body if available
                raw = ex.contentUTF8();
            } catch (Exception ignore) {
                // If contentUTF8 fails, just proceed
            }

            // Log the key information
            log.warn("Feign API or Connectivity Error (HTTP Status: {}) for {} service. Raw body: {}",
                    ex.status(), serviceName, raw);
            log.warn("Detailed Feign Exception Message: {}", ex.getMessage());

            // Check if the status is an HTTP error returned by the remote service
            if (ex.status() > 0) {
                throw new CustomAppException(
                        HttpStatus.valueOf(ex.status()),
                        FeignMessageSanitizer.buildSanitizedMessage(ex.getMessage(), serviceName)
                );
            } else {
                // Handle non-HTTP status errors (e.g., connection refusal, DNS failure)
                throw new CustomAppException(
                        HttpStatus.SERVICE_UNAVAILABLE,
                        "Could not connect to " + serviceName + ". " + ex.getMessage()
                );
            }
        }
    }
}