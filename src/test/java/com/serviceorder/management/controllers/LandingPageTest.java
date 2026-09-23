package com.serviceorder.management.controllers;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithAnonymousUser;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.head;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WithAnonymousUser
class LandingPageTest extends ApiTestSupport {

    @Test
    @DisplayName("GET / should forward to the public landing page without a token")
    void rootShouldForwardToTheLandingPage() throws Exception {
        // MockMvc does not follow forwards, so the forward itself is what proves the welcome page is mapped and public
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(forwardedUrl("index.html"));
    }

    @Test
    @DisplayName("GET /index.html should serve the landing page without a token")
    void landingPageShouldBePublic() throws Exception {
        mockMvc.perform(get("/index.html"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andExpect(content().string(containsString("Service Order Management API")));
    }

    @Test
    @DisplayName("HEAD / should also be public, for uptime checks and link previews")
    void headRequestShouldBePublic() throws Exception {
        mockMvc.perform(head("/index.html")).andExpect(status().isOk());
    }

    @Test
    @DisplayName("Opening the landing page must not make the rest of the API public")
    void apiShouldStayProtected() throws Exception {
        mockMvc.perform(get("/clients")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/orders")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/")).andExpect(status().isUnauthorized());
    }
}
