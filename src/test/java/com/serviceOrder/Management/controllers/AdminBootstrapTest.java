package com.serviceOrder.Management.controllers;

import com.serviceOrder.Management.config.AdminBootstrap;
import com.serviceOrder.Management.entities.AppUser;
import com.serviceOrder.Management.enums.UserRole;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.DefaultApplicationArguments;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdminBootstrapTest extends ApiTestSupport {

    @Autowired
    private AdminBootstrap adminBootstrap;

    @Value("${app.admin.email}")
    private String adminEmail;

    @Value("${app.admin.password}")
    private String adminPassword;

    @Test
    @DisplayName("the initial admin should be created once, with a hashed password")
    void shouldCreateTheAdminOnlyOnce() {
        // cleanDatabase() already removed every user, so the admin does not exist yet
        adminBootstrap.run(new DefaultApplicationArguments());
        adminBootstrap.run(new DefaultApplicationArguments());

        assertEquals(1, userRepository.count());
        AppUser admin = userRepository.findByEmail(adminEmail.toLowerCase()).orElseThrow();
        assertEquals(UserRole.ADMIN, admin.getRole());
        assertTrue(passwordEncoder.matches(adminPassword, admin.getPasswordHash()));
    }
}