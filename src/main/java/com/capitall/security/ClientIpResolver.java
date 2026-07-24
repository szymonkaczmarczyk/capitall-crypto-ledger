package com.capitall.security;

import jakarta.servlet.http.HttpServletRequest;

public final class ClientIpResolver {

    private static final String[] HEADERS = {
            "X-Forwarded-For",
            "X-Real-IP",
            "CF-Connecting-IP",
            "True-Client-IP",
            "Proxy-Client-IP",
            "WL-Proxy-Client-IP"
    };

    private ClientIpResolver() {}

    public static String resolve(HttpServletRequest request) {
        if (request == null) {
            return "unknown";
        }
        for (String header : HEADERS) {
            String value = request.getHeader(header);
            if (value != null && !value.isBlank() && !"unknown".equalsIgnoreCase(value)) {
                int comma = value.indexOf(',');
                String ip = (comma > 0 ? value.substring(0, comma) : value).trim();
                if (!ip.isEmpty()) {
                    return ip;
                }
            }
        }
        String remote = request.getRemoteAddr();
        return (remote == null || remote.isBlank()) ? "unknown" : remote;
    }
}
