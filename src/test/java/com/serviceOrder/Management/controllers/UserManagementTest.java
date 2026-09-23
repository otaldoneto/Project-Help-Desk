package com.serviceOrder.Management.controllers;

import com.serviceOrder.Management.entities.AppUser;
import com.serviceOrder.Management.enums.UserRole;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithAnonymousUser;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WithAnonymousUser
class UserManagementTest extends ApiTestSupport {

    private String adminToken() throws Exception {
        saveUser("admin@mail.com", "password123", UserRole.ADMIN);
        return loginAndGetToken("admin@mail.com", "password123");
    }

    @Test
    @DisplayName("GET /users should list users, and be ADMIN only")
    void findAllShouldListUsersForAdminOnly() throws Exception {
        String token = adminToken();
        saveUser("alice@mail.com", "password123", UserRole.USER);

        mockMvc.perform(get("/users").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[0].enabled").value(true));
    }

    @Test
    @DisplayName("GET /users should return 403 for a USER")
    void findAllShouldReturn403ForUser() throws Exception {
        saveUser("alice@mail.com", "password123", UserRole.USER);
        String aliceToken = loginAndGetToken("alice@mail.com", "password123");

        mockMvc.perform(get("/users").header("Authorization", bearer(aliceToken)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("PUT /users/{id}/disable should stop the user from logging in")
    void disableShouldPreventLogin() throws Exception {
        String token = adminToken();
        AppUser bob = saveUser("bob@mail.com", "password123", UserRole.USER);

        mockMvc.perform(put("/users/{id}/disable", bob.getId()).header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(false));

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"bob@mail.com","password":"password123"}
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("PUT /users/{id}/enable should let a disabled user log in again")
    void enableShouldAllowLoginAgain() throws Exception {
        String token = adminToken();
        AppUser bob = saveDisabledUser("bob@mail.com", "password123", UserRole.USER);

        mockMvc.perform(put("/users/{id}/enable", bob.getId()).header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(true));

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"bob@mail.com","password":"password123"}
                                """))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("PUT /users/{id}/disable should return 403 for a USER")
    void disableShouldReturn403ForUser() throws Exception {
        AppUser bob = saveUser("bob@mail.com", "password123", UserRole.USER);
        saveUser("alice@mail.com", "password123", UserRole.USER);
        String aliceToken = loginAndGetToken("alice@mail.com", "password123");

        mockMvc.perform(put("/users/{id}/disable", bob.getId()).header("Authorization", bearer(aliceToken)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("PUT /users/me/password should change the password and work for a USER, not just ADMIN")
    void changeMyPasswordShouldWorkForAnyAuthenticatedUser() throws Exception {
        saveUser("alice@mail.com", "password123", UserRole.USER);
        String aliceToken = loginAndGetToken("alice@mail.com", "password123");

        mockMvc.perform(put("/users/me/password")
                        .header("Authorization", bearer(aliceToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"currentPassword":"password123","newPassword":"new-password-456"}
                                """))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"alice@mail.com","password":"new-password-456"}
                                """))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("PUT /users/me/password should return 401 when the current password is wrong")
    void changeMyPasswordShouldReturn401WhenCurrentPasswordIsWrong() throws Exception {
        saveUser("alice@mail.com", "password123", UserRole.USER);
        String aliceToken = loginAndGetToken("alice@mail.com", "password123");

        mockMvc.perform(put("/users/me/password")
                        .header("Authorization", bearer(aliceToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"currentPassword":"wrong-password","newPassword":"new-password-456"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Current password is incorrect"));
    }

    @Test
    @DisplayName("PUT /users/me/password should return 400 when the new password is too short")
    void changeMyPasswordShouldReturn400WhenNewPasswordIsTooShort() throws Exception {
        saveUser("alice@mail.com", "password123", UserRole.USER);
        String aliceToken = loginAndGetToken("alice@mail.com", "password123");

        mockMvc.perform(put("/users/me/password")
                        .header("Authorization", bearer(aliceToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"currentPassword":"password123","newPassword":"short"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("newPassword: New password must have between 8 and 72 characters"));
    }
}