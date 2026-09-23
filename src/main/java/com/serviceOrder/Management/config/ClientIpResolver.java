package com.serviceOrder.Management.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

// Decides which IP address identifies the caller (used by the login rate limiter).
// By default only the socket address is used: a client-supplied header could be spoofed.
// Behind a CDN that overwrites a header with the real client IP (Cloudflare's CF-Connecting-IP on Render),
// set app.client-ip-header to that header name.
@Component
public class ClientIpResolver {

    private final String clientIpHeader;

    public ClientIpResolver(@Value("${app.client-ip-header:}") String clientIpHeader) {
        this.clientIpHeader = clientIpHeader.trim();
    }

    public String resolve(HttpServletRequest request) {
        if (!clientIpHeader.isEmpty()) {
            String value = request.getHeader(clientIpHeader);
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return request.getRemoteAddr();
    }
}
