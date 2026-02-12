package com.majjid.microservices.order.config.hanlders.restClient;

import com.majjid.microservices.order.config.CustomAppException;
import com.majjid.microservices.order.util.RestClientMessageSanitizer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;

@Slf4j
public class RestClientHandler {

    public static <T> T handleCall(RestClientCall<T> call, String serviceName) {
        try {
            return call.execute();
        }

        // 1. Client errors (4xx) — the remote service returned a business error
        catch (HttpClientErrorException ex) {
            log.warn("Client Error (HTTP {}) from {} service. Body: {}",
                    ex.getStatusCode().value(), serviceName, ex.getResponseBodyAsString());

            throw new CustomAppException(
                    HttpStatus.valueOf(ex.getStatusCode().value()),
                    RestClientMessageSanitizer.buildSanitizedMessage(
                            ex.getResponseBodyAsString(), serviceName));
        }

        // 2. Server errors (5xx) — the remote service had an internal error
        catch (HttpServerErrorException ex) {
            log.error("Server Error (HTTP {}) from {} service. Body: {}",
                    ex.getStatusCode().value(), serviceName, ex.getResponseBodyAsString());

            throw new CustomAppException(
                    HttpStatus.valueOf(ex.getStatusCode().value()),
                    RestClientMessageSanitizer.buildSanitizedMessage(
                            ex.getResponseBodyAsString(), serviceName));
        }

        // 3. Connection failures (DNS, refused, timeout at network level)
        catch (ResourceAccessException ex) {
            log.error("Connection Error to {} service: {}", serviceName, ex.getMessage());

            throw new CustomAppException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Could not connect to " + serviceName + " service. The service may be down.");
        }

        // 4. Any other RestClient response errors
        catch (RestClientResponseException ex) {
            log.error("RestClient Error (HTTP {}) from {} service: {}",
                    ex.getStatusCode().value(), serviceName, ex.getResponseBodyAsString());

            throw new CustomAppException(
                    HttpStatus.valueOf(ex.getStatusCode().value()),
                    RestClientMessageSanitizer.buildSanitizedMessage(
                            ex.getResponseBodyAsString(), serviceName));
        }
    }
}
