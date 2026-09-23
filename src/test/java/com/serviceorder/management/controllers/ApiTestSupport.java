package com.serviceorder.management.controllers;

import com.serviceorder.management.entities.Client;
import com.serviceorder.management.entities.ServiceOrder;
import com.serviceorder.management.entities.Technician;
import com.serviceorder.management.enums.OrderPriority;
import com.serviceorder.management.enums.OrderStatus;
import com.serviceorder.management.repositories.ClientRepository;
import com.serviceorder.management.repositories.ServiceOrderRepository;
import com.serviceorder.management.repositories.TechnicianRepository;
import com.serviceorder.management.repositories.RefreshTokenRepository;
import com.serviceorder.management.repositories.UserRepository;
import org.springframework.security.test.context.support.WithMockUser;

import com.jayway.jsonpath.JsonPath;
import com.serviceorder.management.entities.AppUser;
import com.serviceorder.management.enums.UserRole;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

@SpringBootTest
@AutoConfigureMockMvc
@WithMockUser(roles = "ADMIN")
abstract class ApiTestSupport {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ServiceOrderRepository orderRepository;

    @Autowired
    protected ClientRepository clientRepository;

    @Autowired
    protected TechnicianRepository technicianRepository;

    @Autowired
    protected UserRepository userRepository;

    @Autowired
    protected RefreshTokenRepository refreshTokenRepository;

    @Autowired
    protected PasswordEncoder passwordEncoder;

    private long tick = 0;

    @BeforeEach
    void cleanDatabase() {
        orderRepository.deleteAll();
        clientRepository.deleteAll();
        technicianRepository.deleteAll();
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();
    }

    protected Client saveClient() {
        return clientRepository.save(new Client(null, "Acme Ltda", "acme@mail.com", "11222333000181"));
    }

    protected Technician saveTechnician() {
        return technicianRepository.save(new Technician(null, "Carlos Silva", "carlos@mail.com", "Networking"));
    }

    // Each order gets a later createdAt than the previous one, so sorting is deterministic.
    protected ServiceOrder saveOrder(Client client, String title, OrderStatus status) {
        ServiceOrder order = new ServiceOrder(null, title, "Does not turn on", OrderPriority.HIGH, client);
        order.setStatus(status);
        order.setCreatedAt(Instant.parse("2026-01-01T00:00:00Z").plusSeconds(tick++));
        return orderRepository.save(order);
    }

    protected ServiceOrder saveOrder(Client client, String title, OrderStatus status, OrderPriority priority) {
        ServiceOrder order = saveOrder(client, title, status);
        order.setPriority(priority);
        return orderRepository.save(order);
    }

    protected AppUser saveUser(String email, String rawPassword, UserRole role) {
        return userRepository.save(new AppUser(null, "Test User", email, passwordEncoder.encode(rawPassword), role, true));
    }

    protected AppUser saveDisabledUser(String email, String rawPassword, UserRole role) {
        return userRepository.save(new AppUser(null, "Test User", email, passwordEncoder.encode(rawPassword), role, false));
    }

    // Logs in through the real endpoint and returns the JWT
    protected String loginAndGetToken(String email, String password) throws Exception {
        String body = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"%s"}
                                """.formatted(email, password)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.accessToken");
    }

    protected String bearer(String token) {
        return "Bearer " + token;
    }
}