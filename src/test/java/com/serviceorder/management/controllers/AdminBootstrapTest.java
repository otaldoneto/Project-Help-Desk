package com.serviceorder.management.controllers;

import com.serviceorder.management.config.AdminBootstrap;
import com.serviceorder.management.entities.AppUser;
import com.serviceorder.management.enums.UserRole;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.DefaultApplicationArguments;

import static org.junit.jupiter.api.Assertions.*;

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

    @Test
    @DisplayName("the application should refuse to start with a blank admin email")
    void shouldRejectBlankAdminEmail() {
        AdminBootstrap bootstrap = new AdminBootstrap(userRepository, passwordEncoder, "  ", "long-enough-password");

        assertThrows(IllegalStateException.class, () -> bootstrap.run(new DefaultApplicationArguments()));
        assertEquals(0, userRepository.count());
    }

    @Test
    @DisplayName("the application should refuse to start with a short admin password")
    void shouldRejectShortAdminPassword() {
        AdminBootstrap bootstrap = new AdminBootstrap(userRepository, passwordEncoder, "admin@example.com", "short");

        assertThrows(IllegalStateException.class, () -> bootstrap.run(new DefaultApplicationArguments()));
        assertEquals(0, userRepository.count());
    }
}