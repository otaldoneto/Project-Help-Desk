package com.serviceOrder.Management.controllers;

import com.serviceOrder.Management.entities.Client;
import com.serviceOrder.Management.entities.OrderService;
import com.serviceOrder.Management.entities.Technician;
import com.serviceOrder.Management.enums.OrderPriority;
import com.serviceOrder.Management.enums.OrderStatus;
import com.serviceOrder.Management.repositories.ClientRepository;
import com.serviceOrder.Management.repositories.OrderServiceRepository;
import com.serviceOrder.Management.repositories.TechnicianRepository;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

@SpringBootTest
@AutoConfigureMockMvc
abstract class ApiTestSupport {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected OrderServiceRepository orderRepository;

    @Autowired
    protected ClientRepository clientRepository;

    @Autowired
    protected TechnicianRepository technicianRepository;

    private long tick = 0;

    @BeforeEach
    void cleanDatabase() {
        orderRepository.deleteAll();
        clientRepository.deleteAll();
        technicianRepository.deleteAll();
    }

    protected Client saveClient() {
        return clientRepository.save(new Client(null, "Acme Ltda", "acme@mail.com", "12345678000199"));
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
}