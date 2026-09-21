package com.serviceOrder.Management.controllers;

import com.serviceOrder.Management.entities.Client;
import com.serviceOrder.Management.entities.OrderService;
import com.serviceOrder.Management.entities.Technician;
import com.serviceOrder.Management.enums.OrderPriority;
import com.serviceOrder.Management.enums.OrderStatus;
import com.serviceOrder.Management.repositories.ClientRepository;
import com.serviceOrder.Management.repositories.OrderServiceRepository;
import com.serviceOrder.Management.repositories.TechnicianRepository;
import com.serviceOrder.Management.repositories.UserRepository;
import org.springframework.security.test.context.support.WithMockUser;

import com.jayway.jsonpath.JsonPath;
import com.serviceOrder.Management.entities.AppUser;
import com.serviceOrder.Management.enums.UserRole;
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
    protected OrderServiceRepository orderRepository;

    @Autowired
    protected ClientRepository clientRepository;

    @Autowired
    protected TechnicianRepository technicianRepository;

    @Autowired
    protected UserRepository userRepository;

    @Autowired
    protected PasswordEncoder passwordEncoder;

    private long tick = 0;

    @BeforeEach
    void cleanDatabase() {
        orderRepository.deleteAll();
        clientRepository.deleteAll();
        technicianRepository.deleteAll();
        userRepository.deleteAll();
    }

    protected Client saveClient() {
        return clientRepository.save(new Client(null, "Acme Ltda", "acme@mail.com", "11222333000181"));
    }

    protected Technician saveTechnician() {
        return technicianRepository.save(new Technician(null, "Carlos Silva", "carlos@mail.com", "Networking"));
    }

    // Each order gets a later createdAt than the previous one, so sorting is deterministic.
    protected OrderService saveOrder(Client client, String title, OrderStatus status) {
        OrderService order = new OrderService(null, title, "Does not turn on", OrderPriority.HIGH, client);
        order.setStatus(status);
        order.setCreatedAt(Instant.parse("2026-01-01T00:00:00Z").plusSeconds(tick++));
        return orderRepository.save(order);
    }

    protected AppUser saveUser(String email, String rawPassword, UserRole role) {
        return userRepository.save(new AppUser(null, "Test User", email, passwordEncoder.encode(rawPassword), role));
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