package com.serviceOrder.Management.controllers;

import com.serviceOrder.Management.entities.Client;
import com.serviceOrder.Management.entities.ServiceOrder;
import com.serviceOrder.Management.entities.Technician;
import com.serviceOrder.Management.enums.OrderStatus;
import com.serviceOrder.Management.enums.UserRole;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuditingTest extends ApiTestSupport {

    @Test
    @DisplayName("POST /orders should record the creator as createdBy and lastModifiedBy")
    void createShouldRecordTheCreator() throws Exception {
        Client client = saveClient();
        saveUser("alice@mail.com", "password123", UserRole.USER);
        String aliceToken = loginAndGetToken("alice@mail.com", "password123");

        mockMvc.perform(post("/orders")
                        .header("Authorization", bearer(aliceToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Printer down","description":"Does not turn on","priority":"HIGH","clientId":%d}
                                """.formatted(client.getId())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.createdBy").value("alice@mail.com"))
                .andExpect(jsonPath("$.lastModifiedBy").value("alice@mail.com"))
                .andExpect(jsonPath("$.lastModifiedAt").isNotEmpty());
    }

    @Test
    @DisplayName("A later change should update lastModifiedBy, but keep the original createdBy")
    void laterChangeShouldUpdateLastModifiedByOnly() throws Exception {
        Client client = saveClient();
        Technician technician = saveTechnician();
        saveUser("alice@mail.com", "password123", UserRole.USER);
        saveUser("bob@mail.com", "password123", UserRole.USER);
        String aliceToken = loginAndGetToken("alice@mail.com", "password123");
        String bobToken = loginAndGetToken("bob@mail.com", "password123");

        String body = mockMvc.perform(post("/orders")
                        .header("Authorization", bearer(aliceToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Printer down","description":"Does not turn on","priority":"HIGH","clientId":%d}
                                """.formatted(client.getId())))
                .andReturn().getResponse().getContentAsString();
        Number orderId = com.jayway.jsonpath.JsonPath.read(body, "$.id");

        mockMvc.perform(put("/orders/{id}/assign/{technicianId}", orderId, technician.getId())
                        .header("Authorization", bearer(bobToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.createdBy").value("alice@mail.com"))
                .andExpect(jsonPath("$.lastModifiedBy").value("bob@mail.com"));
    }

    @Test
    @DisplayName("Direct repository saves are also audited using the current authenticated user")
    void directSaveShouldAlsoBeAudited() {
        Client client = saveClient();
        ServiceOrder order = saveOrder(client, "Printer down", OrderStatus.OPEN);

        Assertions.assertEquals("user", order.getCreatedBy());
        Assertions.assertEquals("user", order.getLastModifiedBy());
        Assertions.assertNotNull(order.getLastModifiedAt());
    }
}