package com.serviceOrder.Management.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ClientIpResolverTest {

    private MockHttpServletRequest requestFrom(String remoteAddr, String headerName, String headerValue) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr(remoteAddr);
        if (headerName != null) {
            request.addHeader(headerName, headerValue);
        }
        return request;
    }

    @Test
    @DisplayName("should ignore client headers when no header is configured")
    void shouldUseRemoteAddressWhenNoHeaderIsConfigured() {
        ClientIpResolver resolver = new ClientIpResolver("");

        assertEquals("10.0.0.1", resolver.resolve(requestFrom("10.0.0.1", "CF-Connecting-IP", "203.0.113.5")));
    }

    @Test
    @DisplayName("should use the configured header when it is present")
    void shouldUseConfiguredHeader() {
        ClientIpResolver resolver = new ClientIpResolver("CF-Connecting-IP");

        assertEquals("203.0.113.5", resolver.resolve(requestFrom("10.0.0.1", "CF-Connecting-IP", " 203.0.113.5 ")));
    }

    @Test
    @DisplayName("should fall back to the remote address when the configured header is missing or blank")
    void shouldFallBackToRemoteAddress() {
        ClientIpResolver resolver = new ClientIpResolver("CF-Connecting-IP");

        assertEquals("10.0.0.1", resolver.resolve(requestFrom("10.0.0.1", null, null)));
        assertEquals("10.0.0.1", resolver.resolve(requestFrom("10.0.0.1", "CF-Connecting-IP", "  ")));
    }
}
