package com.majjid.microservices.order.util;


import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class FeignMessageSanitizer {

    private static final Pattern URL_PATTERN = Pattern.compile("http://[^\\s\\]]+");
    private static final Pattern MESSAGE_FIELD = Pattern.compile("\"message\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern EMPTY_BRACKETS = Pattern.compile("\\[\\s*\\]");

    private FeignMessageSanitizer() { }

    /**
     * Minimal sanitizer:
     * - hides http:// URLs with "[{service} Service URL hidden]"
     * - extracts inner JSON "message" if present and appends it cleanly
     * - removes JSON block and leftover empty brackets/artifacts
     */
    public static String buildSanitizedMessage(String raw, String serviceName) {
        if (raw == null || raw.isEmpty()) return "";

        // 1) Replace URLs
        String s = URL_PATTERN.matcher(raw).replaceAll("[" + serviceName + " Service URL hidden]");

        // 2) Extract first JSON payload (simple brace depth scan)
        String json = extractFirstJson(s);

        // 3) Extract "message" value from the original raw JSON (prefer original to avoid removed content)
        String innerMsg = extractMessageFromJson(json != null ? json : raw);

        // 4) Remove JSON payload and empty brackets, normalize duplicated brackets
        if (json != null) s = s.replace(json, "");
        s = s.replaceAll("\\[\\[+", "\\[").replaceAll("]]+", "]"); // normalize double brackets
        s = EMPTY_BRACKETS.matcher(s).replaceAll("").replaceAll("\\s+", " ").trim();

        // 5) Tidy trailing punctuation then append inner message if present
        s = s.replaceAll("\\s*:\\s*$", "").replaceAll("\\s*\\]\\s*$", "]").trim();
        if (innerMsg != null && !innerMsg.isEmpty()) {
            // ensure there's a closing bracket before appending message
            if (!s.endsWith("]")) {
                int idx = s.lastIndexOf(']');
                if (idx >= 0) s = s.substring(0, idx + 1);
            }
            s = s + ": " + innerMsg;
        }

        return s;
    }

    // --- helpers (kept minimal) ---
    private static String extractFirstJson(String text) {
        if (text == null) return null;
        int start = text.indexOf('{');
        if (start < 0) return null;
        int depth = 0;
        for (int i = start; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '{') depth++;
            else if (c == '}') {
                depth--;
                if (depth == 0) return text.substring(start, i + 1);
            }
        }
        return null;
    }

    private static String extractMessageFromJson(String json) {
        if (json == null) return null;
        Matcher m = MESSAGE_FIELD.matcher(json);
        return m.find() ? m.group(1) : null;
    }
}