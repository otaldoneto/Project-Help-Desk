package com.serviceorder.management.controllers;

import com.jayway.jsonpath.JsonPath;
import com.serviceorder.management.enums.UserRole;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithAnonymousUser;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WithAnonymousUser
class RefreshTokenFlowTest extends ApiTestSupport {

    @Test
    @DisplayName("login should return a refresh token alongside the access token")
    void loginShouldReturnARefreshToken() throws Exception {
        saveUser("alice@mail.com", "password123", UserRole.USER);

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"alice@mail.com","password":"password123"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty());
    }

    @Test
    @DisplayName("POST /auth/refresh should exchange a refresh token for a new pair, and the new access token should work")
    void refreshShouldIssueANewWorkingAccessToken() throws Exception {
        saveUser("alice@mail.com", "password123", UserRole.USER);
        String loginBody = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"alice@mail.com","password":"password123"}
                                """))
                .andReturn().getResponse().getContentAsString();
        String refreshToken = JsonPath.read(loginBody, "$.refreshToken");

        String refreshResponseBody = mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"refreshToken":"%s"}
                                """.formatted(refreshToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andReturn().getResponse().getContentAsString();

        String newAccessToken = JsonPath.read(refreshResponseBody, "$.accessToken");

        mockMvc.perform(get("/clients").header("Authorization", bearer(newAccessToken)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /auth/refresh should reject a refresh token that was already used (rotation)")
    void refreshShouldRejectAReusedToken() throws Exception {
        saveUser("alice@mail.com", "password123", UserRole.USER);
        String loginBody = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"alice@mail.com","password":"password123"}
                                """))
                .andReturn().getResponse().getContentAsString();
        String refreshToken = JsonPath.read(loginBody, "$.refreshToken");

        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"refreshToken":"%s"}
                                """.formatted(refreshToken)))
                .andExpect(status().isOk());

        // Using the SAME (now rotated-away) refresh token a second time must fail
        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"refreshToken":"%s"}
                                """.formatted(refreshToken)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /auth/refresh should return 400 when the refresh token is blank")
    void refreshShouldReturn400WhenBlank() throws Exception {
        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"refreshToken":""}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /auth/logout should revoke the refresh token, so it can no longer be used to refresh")
    void logoutShouldRevokeTheRefreshToken() throws Exception {
        saveUser("alice@mail.com", "password123", UserRole.USER);
        String loginBody = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"alice@mail.com","password":"password123"}
                                """))
                .andReturn().getResponse().getContentAsString();
        String refreshToken = JsonPath.read(loginBody, "$.refreshToken");

        mockMvc.perform(post("/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"refreshToken":"%s"}
                                """.formatted(refreshToken)))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"refreshToken":"%s"}
                                """.formatted(refreshToken)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /auth/logout should be idempotent (logging out twice is not an error)")
    void logoutShouldBeIdempotent() throws Exception {
        saveUser("alice@mail.com", "password123", UserRole.USER);
        String loginBody = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"alice@mail.com","password":"password123"}
                                """))
                .andReturn().getResponse().getContentAsString();
        String refreshToken = JsonPath.read(loginBody, "$.refreshToken");

        for (int i = 0; i < 2; i++) {
            mockMvc.perform(post("/auth/logout")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"refreshToken":"%s"}
                                    """.formatted(refreshToken)))
                    .andExpect(status().isNoContent());
        }
    }
}