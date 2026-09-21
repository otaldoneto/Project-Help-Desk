package com.serviceOrder.Management.controllers;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class OpenApiDocsTest extends ApiTestSupport {

    @Test
    @DisplayName("GET /v3/api-docs should expose the API documentation")
    void apiDocsShouldBeAvailable() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("Service Order Management API"))
                .andExpect(jsonPath("$.paths['/clients']").exists())
                .andExpect(jsonPath("$.paths['/technicians']").exists())
                .andExpect(jsonPath("$.paths['/orders/{id}/report']").exists());
    }
}