package com.serviceOrder.Management.controllers;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ClientControllerTest extends ApiTestSupport {

    @Test
    @DisplayName("POST /clients should create a client and normalize email and CPF/CNPJ")
    void insertShouldReturn201() throws Exception {
        mockMvc.perform(post("/clients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Acme Ltda","email":"Acme@Mail.COM","cpfOrCnpj":"12.345.678/0001-99"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.email").value("acme@mail.com"))
                .andExpect(jsonPath("$.cpfOrCnpj").value("12345678000199"));
    }

    @Test
    @DisplayName("POST /clients should return 400 listing every invalid field")
    void insertShouldReturn400WithAllFieldErrors() throws Exception {
        mockMvc.perform(post("/clients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"","email":"not-an-email","cpfOrCnpj":"12345678000199"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation error"))
                .andExpect(jsonPath("$.message").value("email: Invalid email format; name: Name is required"));
    }

    @Test
    @DisplayName("POST /clients should return 409 when the email already exists, ignoring case")
    void insertShouldReturn409WhenEmailExists() throws Exception {
        saveClient();

        mockMvc.perform(post("/clients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Other","email":"ACME@MAIL.COM","cpfOrCnpj":"99999999000199"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("A client with this email already exists"));
    }

    @Test
    @DisplayName("POST /clients should return 409 when the CPF/CNPJ already exists, ignoring formatting")
    void insertShouldReturn409WhenDocumentExists() throws Exception {
        saveClient();

        mockMvc.perform(post("/clients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Other","email":"other@mail.com","cpfOrCnpj":"12.345.678/0001-99"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("A client with this CPF/CNPJ already exists"));
    }

    @Test
    @DisplayName("GET /clients should list the saved clients")
    void findAllShouldReturnClients() throws Exception {
        saveClient();

        mockMvc.perform(get("/clients"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Acme Ltda"));
    }

    @Test
    @DisplayName("GET /clients/{id} should return 404 in the standard error format")
    void findByIdShouldReturn404() throws Exception {
        mockMvc.perform(get("/clients/{id}", 999999))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Resource not found"))
                .andExpect(jsonPath("$.message").value("Client not found with id: 999999"))
                .andExpect(jsonPath("$.path").value("/clients/999999"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @DisplayName("GET /clients/{id} should return 400 when the id is not a number")
    void findByIdShouldReturn400WhenIdIsNotNumeric() throws Exception {
        mockMvc.perform(get("/clients/{id}", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid value 'abc' for parameter 'id'"));
    }
}