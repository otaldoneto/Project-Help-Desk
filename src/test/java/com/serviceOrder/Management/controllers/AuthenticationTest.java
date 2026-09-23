package com.serviceOrder.Management.controllers;

import com.serviceOrder.Management.enums.UserRole;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithAnonymousUser;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WithAnonymousUser
class AuthenticationTest extends ApiTestSupport {

    @Test
    @DisplayName("POST /auth/login should return a Bearer token for valid credentials, ignoring email case")
    void loginShouldReturnToken() throws Exception {
        saveUser("ana@mail.com", "password123", UserRole.USER);

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"Ana@Mail.com","password":"password123"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(900));
    }

    @Test
    @DisplayName("POST /auth/login should return 401 when the password is wrong")
    void loginShouldReturn401WhenPasswordIsWrong() throws Exception {
        saveUser("ana@mail.com", "password123", UserRole.USER);

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"ana@mail.com","password":"wrong-password"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
    }

    @Test
    @DisplayName("POST /auth/login should return the same 401 when the email does not exist")
    void loginShouldReturn401WhenUserDoesNotExist() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"nobody@mail.com","password":"password123"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
    }

    @Test
    @DisplayName("POST /auth/login should return 400 when the fields are blank")
    void loginShouldReturn400WhenFieldsAreBlank() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"","password":""}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("email: Email is required; password: Password is required"));
    }

    @Test
    @DisplayName("GET /auth/me should return the email and roles stored in the token")
    void meShouldReturnEmailAndRoles() throws Exception {
        saveUser("admin@mail.com", "password123", UserRole.ADMIN);
        String token = loginAndGetToken("admin@mail.com", "password123");

        mockMvc.perform(get("/auth/me").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("admin@mail.com"))
                .andExpect(jsonPath("$.roles[0]").value("ADMIN"));
    }
}