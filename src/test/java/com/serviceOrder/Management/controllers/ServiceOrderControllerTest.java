package com.serviceOrder.Management.controllers;

import com.serviceOrder.Management.entities.Client;
import com.serviceOrder.Management.entities.ServiceOrder;
import com.serviceOrder.Management.entities.Technician;
import com.serviceOrder.Management.enums.OrderPriority;
import com.serviceOrder.Management.enums.OrderStatus;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ServiceOrderControllerTest extends ApiTestSupport {

    // ---------- create ----------

    @Test
    @DisplayName("POST /orders should create an OPEN order for an existing client")
    void createShouldReturn201() throws Exception {
        Client client = saveClient();

        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Printer down","description":"Does not turn on","priority":"HIGH","clientId":%d}
                                """.formatted(client.getId())))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.priority").value("HIGH"))
                .andExpect(jsonPath("$.client.id").value(client.getId().intValue()));
    }

    @Test
    @DisplayName("POST /orders should return 404 when the client does not exist")
    void createShouldReturn404WhenClientNotFound() throws Exception {
        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Printer down","description":"Does not turn on","priority":"HIGH","clientId":999999}
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Client not found with id: 999999"));
    }

    @Test
    @DisplayName("POST /orders should return 400 when the title is missing")
    void createShouldReturn400WhenTitleIsMissing() throws Exception {
        Client client = saveClient();

        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"description":"Does not turn on","priority":"HIGH","clientId":%d}
                                """.formatted(client.getId())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("title: Title is required"));
    }

    @Test
    @DisplayName("POST /orders should return 400 when the priority is not a valid value")
    void createShouldReturn400WhenPriorityIsInvalid() throws Exception {
        Client client = saveClient();

        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Printer down","description":"Does not turn on","priority":"URGENT","clientId":%d}
                                """.formatted(client.getId())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Malformed request"));
    }

    // ---------- find ----------

    @Test
    @DisplayName("GET /orders/{id} should return the order with its client")
    void findByIdShouldReturnOrderWithClient() throws Exception {
        ServiceOrder order = saveOrder(saveClient(), "Printer down", OrderStatus.OPEN);

        mockMvc.perform(get("/orders/{id}", order.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Printer down"))
                .andExpect(jsonPath("$.client.name").value("Acme Ltda"));
    }

    @Test
    @DisplayName("GET /orders/{id} should return 404 when the order does not exist")
    void findByIdShouldReturn404() throws Exception {
        mockMvc.perform(get("/orders/{id}", 999999))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Service Order not found with id: 999999"));
    }

    // ---------- list / pagination ----------

    @Test
    @DisplayName("GET /orders should return the requested page, newest first")
    void findAllShouldReturnRequestedPage() throws Exception {
        Client client = saveClient();
        saveOrder(client, "Order 1", OrderStatus.OPEN);
        saveOrder(client, "Order 2", OrderStatus.OPEN);
        saveOrder(client, "Order 3", OrderStatus.OPEN);

        mockMvc.perform(get("/orders?size=2&page=0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].title").value("Order 3"))
                .andExpect(jsonPath("$.content[1].title").value("Order 2"))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(2))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(2));
    }

    @Test
    @DisplayName("GET /orders should cap the page size at 100")
    void findAllShouldCapPageSize() throws Exception {
        mockMvc.perform(get("/orders?size=1000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(100));
    }

    @Test
    @DisplayName("GET /orders should return 400 for an invalid sort property")
    void findAllShouldReturn400ForInvalidSort() throws Exception {
        mockMvc.perform(get("/orders?sort=foo"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid sort property: 'foo'"));
    }

    @Test
    @DisplayName("GET /orders should accept a valid sort property")
    void findAllShouldAcceptValidSort() throws Exception {
        mockMvc.perform(get("/orders?sort=title,asc"))
                .andExpect(status().isOk());
    }

    // ---------- assign ----------

    @Test
    @DisplayName("PUT /orders/{id}/assign/{technicianId} should move the order to IN_PROGRESS")
    void assignShouldMoveOrderToInProgress() throws Exception {
        ServiceOrder order = saveOrder(saveClient(), "Printer down", OrderStatus.OPEN);
        Technician technician = saveTechnician();

        mockMvc.perform(put("/orders/{id}/assign/{technicianId}", order.getId(), technician.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.technician.id").value(technician.getId().intValue()));
    }

    @Test
    @DisplayName("PUT /orders/{id}/assign/{technicianId} should return 404 when the technician does not exist")
    void assignShouldReturn404WhenTechnicianNotFound() throws Exception {
        ServiceOrder order = saveOrder(saveClient(), "Printer down", OrderStatus.OPEN);

        mockMvc.perform(put("/orders/{id}/assign/{technicianId}", order.getId(), 999999))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Technician not found with id: 999999"));
    }

    @Test
    @DisplayName("PUT /orders/{id}/assign/{technicianId} should return 409 when the order is FINISHED")
    void assignShouldReturn409WhenOrderIsFinished() throws Exception {
        ServiceOrder order = saveOrder(saveClient(), "Printer down", OrderStatus.FINISHED);

        mockMvc.perform(put("/orders/{id}/assign/{technicianId}", order.getId(), 1))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message")
                        .value("Cannot assign a technician to a service order with status FINISHED"));
    }

    // ---------- finish ----------

    @Test
    @DisplayName("PUT /orders/{id}/finish should store the report and finish an IN_PROGRESS order")
    void finishShouldStoreReport() throws Exception {
        ServiceOrder order = saveOrder(saveClient(), "Printer down", OrderStatus.IN_PROGRESS);

        mockMvc.perform(put("/orders/{id}/finish", order.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"rootCauseReport":"Power supply replaced"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FINISHED"))
                .andExpect(jsonPath("$.rootCauseReport").value("Power supply replaced"))
                .andExpect(jsonPath("$.finishedAt").isNotEmpty());
    }

    @Test
    @DisplayName("PUT /orders/{id}/finish should return 409 when the order is still OPEN")
    void finishShouldReturn409WhenOrderIsOpen() throws Exception {
        ServiceOrder order = saveOrder(saveClient(), "Printer down", OrderStatus.OPEN);

        mockMvc.perform(put("/orders/{id}/finish", order.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"rootCauseReport":"Power supply replaced"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Cannot finish a service order with status OPEN"));
    }

    @Test
    @DisplayName("PUT /orders/{id}/finish should return 400 when the report is blank")
    void finishShouldReturn400WhenReportIsBlank() throws Exception {
        ServiceOrder order = saveOrder(saveClient(), "Printer down", OrderStatus.IN_PROGRESS);

        mockMvc.perform(put("/orders/{id}/finish", order.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"rootCauseReport":"   "}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("rootCauseReport: Root cause report is required to finish the service order"));
    }

    // ---------- cancel ----------

    @Test
    @DisplayName("PUT /orders/{id}/cancel should cancel an OPEN order")
    void cancelShouldCancelOpenOrder() throws Exception {
        ServiceOrder order = saveOrder(saveClient(), "Printer down", OrderStatus.OPEN);

        mockMvc.perform(put("/orders/{id}/cancel", order.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELED"));
    }

    @Test
    @DisplayName("PUT /orders/{id}/cancel should return 409 when the order is FINISHED")
    void cancelShouldReturn409WhenOrderIsFinished() throws Exception {
        ServiceOrder order = saveOrder(saveClient(), "Printer down", OrderStatus.FINISHED);

        mockMvc.perform(put("/orders/{id}/cancel", order.getId()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Cannot cancel a service order with status FINISHED"));
    }

    // ---------- report ----------

    @Test
    @DisplayName("GET /orders/{id}/report should return a PDF")
    void reportShouldReturnPdf() throws Exception {
        ServiceOrder order = saveOrder(saveClient(), "Printer down", OrderStatus.OPEN);

        MvcResult result = mockMvc.perform(get("/orders/{id}/report", order.getId()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(header().exists("Content-Disposition"))
                .andReturn();

        byte[] pdf = result.getResponse().getContentAsByteArray();
        assertEquals("%PDF", new String(pdf, 0, 4, StandardCharsets.US_ASCII));
    }

    @Test
    @DisplayName("GET /orders/{id}/report should return a JSON 404 when the order does not exist")
    void reportShouldReturn404() throws Exception {
        mockMvc.perform(get("/orders/{id}/report", 999999))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Service Order not found with id: 999999"));
    }

    // ---------- filters ----------

    @Test
    @DisplayName("GET /orders?status= should return only the orders with that status")
    void findAllShouldFilterByStatus() throws Exception {
        Client client = saveClient();
        saveOrder(client, "Order 1", OrderStatus.OPEN);
        saveOrder(client, "Order 2", OrderStatus.FINISHED);
        saveOrder(client, "Order 3", OrderStatus.OPEN);

        mockMvc.perform(get("/orders?status=OPEN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[0].status").value("OPEN"))
                .andExpect(jsonPath("$.content[1].status").value("OPEN"));
    }

    @Test
    @DisplayName("GET /orders?priority= should return only the orders with that priority")
    void findAllShouldFilterByPriority() throws Exception {
        Client client = saveClient();
        saveOrder(client, "Urgent order", OrderStatus.OPEN, OrderPriority.HIGH);
        saveOrder(client, "Calm order", OrderStatus.OPEN, OrderPriority.LOW);

        mockMvc.perform(get("/orders?priority=LOW"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].title").value("Calm order"));
    }

    @Test
    @DisplayName("GET /orders?clientId= should return only the orders of that client")
    void findAllShouldFilterByClient() throws Exception {
        Client client = saveClient();
        Client other = clientRepository.save(new Client(null, "Other", "other@mail.com", "11444777000161"));
        saveOrder(client, "Order of the first client", OrderStatus.OPEN);
        saveOrder(other, "Order of the other client", OrderStatus.OPEN);

        mockMvc.perform(get("/orders").param("clientId", other.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].client.id").value(other.getId().intValue()));
    }

    @Test
    @DisplayName("GET /orders?technicianId= should return only the orders assigned to that technician")
    void findAllShouldFilterByTechnician() throws Exception {
        Client client = saveClient();
        Technician technician = saveTechnician();
        ServiceOrder assigned = saveOrder(client, "Assigned order", OrderStatus.IN_PROGRESS);
        assigned.setTechnician(technician);
        orderRepository.save(assigned);
        saveOrder(client, "Unassigned order", OrderStatus.OPEN);

        mockMvc.perform(get("/orders").param("technicianId", technician.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].title").value("Assigned order"));
    }

    @Test
    @DisplayName("GET /orders?title= should match part of the title, ignoring case")
    void findAllShouldFilterByTitleIgnoringCase() throws Exception {
        Client client = saveClient();
        saveOrder(client, "Printer down", OrderStatus.OPEN);
        saveOrder(client, "Network slow", OrderStatus.OPEN);

        mockMvc.perform(get("/orders").param("title", "PRINTER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].title").value("Printer down"));
    }

    @Test
    @DisplayName("GET /orders?title= should treat % as a normal character, not as a wildcard")
    void findAllShouldSearchWildcardsLiterally() throws Exception {
        Client client = saveClient();
        saveOrder(client, "100% sure it is broken", OrderStatus.OPEN);
        saveOrder(client, "Something else", OrderStatus.OPEN);

        mockMvc.perform(get("/orders").param("title", "%"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].title").value("100% sure it is broken"));
    }

    @Test
    @DisplayName("GET /orders should combine several filters with AND")
    void findAllShouldCombineFilters() throws Exception {
        Client client = saveClient();
        Client other = clientRepository.save(new Client(null, "Other", "other@mail.com", "11444777000161"));
        saveOrder(client, "Open order of the first client", OrderStatus.OPEN);
        saveOrder(client, "Finished order of the first client", OrderStatus.FINISHED);
        saveOrder(other, "Open order of the other client", OrderStatus.OPEN);

        mockMvc.perform(get("/orders")
                        .param("status", "OPEN")
                        .param("clientId", client.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].title").value("Open order of the first client"));
    }

    @Test
    @DisplayName("GET /orders should return 400 when a filter value is not valid")
    void findAllShouldReturn400ForInvalidStatus() throws Exception {
        mockMvc.perform(get("/orders?status=FOO"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid value 'FOO' for parameter 'status'"));
    }
}