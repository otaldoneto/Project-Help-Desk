package com.serviceorder.management.controllers;

import com.serviceorder.management.entities.AppUser;
import com.serviceorder.management.enums.UserRole;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithAnonymousUser;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WithAnonymousUser
class UserControllerTest extends ApiTestSupport {

    private String adminToken() throws Exception {
        saveUser("admin@mail.com", "password123", UserRole.ADMIN);
        return loginAndGetToken("admin@mail.com", "password123");
    }

    @Test
    @DisplayName("POST /users should create a user, store only a BCrypt hash and let the new user log in")
    void adminCreatesUser() throws Exception {
        String token = adminToken();

        mockMvc.perform(post("/users")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Bruno Lima","email":"Bruno@Mail.com","password":"secret1234","role":"USER"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("bruno@mail.com"))
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());

        AppUser saved = userRepository.findByEmail("bruno@mail.com").orElseThrow();
        assertNotEquals("secret1234", saved.getPasswordHash());
        assertTrue(saved.getPasswordHash().startsWith("$2"), "expected a BCrypt hash");

        // The new user can log in with the password that was sent
        loginAndGetToken("bruno@mail.com", "secret1234");
    }

    @Test
    @DisplayName("POST /users should return 409 when the email is already registered")
    void createShouldReturn409WhenEmailExists() throws Exception {
        String token = adminToken();
        saveUser("bruno@mail.com", "password123", UserRole.USER);

        mockMvc.perform(post("/users")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Bruno Lima","email":"BRUNO@MAIL.COM","password":"secret1234","role":"USER"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("A user with this email already exists"));
    }

    @Test
    @DisplayName("POST /users should return 400 when the password is too short")
    void createShouldReturn400WhenPasswordIsTooShort() throws Exception {
        String token = adminToken();

        mockMvc.perform(post("/users")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Bruno Lima","email":"bruno@mail.com","password":"short","role":"USER"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("password: Password must have between 8 and 72 characters"));
    }
}