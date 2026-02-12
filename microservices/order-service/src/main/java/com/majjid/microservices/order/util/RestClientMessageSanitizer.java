package com.majjid.microservices.order.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Sanitizes error messages from RestClient responses.
 * Extracts the "message" field from JSON error bodies and hides internal URLs.
 */
public final class RestClientMessageSanitizer {

    private static final Pattern URL_PATTERN = Pattern.compile("http://[^\\s\\]]+");
    private static final Pattern MESSAGE_FIELD = Pattern.compile("\"message\"\\s*:\\s*\"([^\"]+)\"");

    private RestClientMessageSanitizer() {
    }

    /**
     * Extracts a clean, user-friendly message from a raw JSON error body.
     * - Hides internal URLs
     * - Extracts the "message" field from JSON if present
     * - Falls back to the raw body if no "message" field is found
     */
    public static String buildSanitizedMessage(String rawBody, String serviceName) {
        if (rawBody == null || rawBody.isEmpty()) {
            return "Error communicating with " + serviceName + " service";
        }

        // Try to extract the "message" field from JSON body
        String innerMessage = extractMessageFromJson(rawBody);
        if (innerMessage != null && !innerMessage.isEmpty()) {
            return innerMessage;
        }

        // Fallback: hide URLs and return sanitized raw body
        String sanitized = URL_PATTERN.matcher(rawBody)
                .replaceAll("[" + serviceName + " Service URL hidden]");
        return sanitized;
    }

    private static String extractMessageFromJson(String json) {
        if (json == null)
            return null;
        Matcher m = MESSAGE_FIELD.matcher(json);
        return m.find() ? m.group(1) : null;
    }
}
