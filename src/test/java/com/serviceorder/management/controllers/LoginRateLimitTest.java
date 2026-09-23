package com.serviceorder.management.controllers;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@WithAnonymousUser
class LoginRateLimitTest extends ApiTestSupport {

    @Test
    @DisplayName("repeated failed logins from the same IP should eventually be rejected with 429 and a Retry-After header")
    void repeatedFailedLoginsShouldEventuallyBeRejected() throws Exception {
        boolean rateLimited = false;

        // A fixed, made-up client IP (simulated at the servlet level, not via a header a real
        // client could spoof), so this test never shares state with any other test's login attempts.
        for (int i = 0; i < 10 && !rateLimited; i++) {
            MvcResult result = mockMvc.perform(loginAttempt()).andReturn();

            if (result.getResponse().getStatus() == 429) {
                rateLimited = true;
                assertTrue(result.getResponse().containsHeader("Retry-After"));
            }
        }

        assertTrue(rateLimited, "expected a 429 response within 10 attempts");
    }

    private MockHttpServletRequestBuilder loginAttempt() {
        return post("/auth/login")
                .with(request -> {
                    request.setRemoteAddr("203.0.113.77");
                    return request;
                })
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"email":"nobody@mail.com","password":"wrong-password"}
                        """);
    }
}