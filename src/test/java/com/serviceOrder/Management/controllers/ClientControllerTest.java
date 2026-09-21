package com.serviceOrder.Management.controllers;

import com.serviceOrder.Management.entities.Client;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import com.serviceOrder.Management.enums.OrderStatus;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

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
                                {"name":"Acme Ltda","email":"Acme@Mail.COM","cpfOrCnpj":"11.222.333/0001-81"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.email").value("acme@mail.com"))
                .andExpect(jsonPath("$.cpfOrCnpj").value("11222333000181"));
    }

    @Test
    @DisplayName("POST /clients should return 400 listing every invalid field")
    void insertShouldReturn400WithAllFieldErrors() throws Exception {
        mockMvc.perform(post("/clients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"","email":"not-an-email","cpfOrCnpj":"11222333000181"}
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
                                {"name":"Other","email":"ACME@MAIL.COM","cpfOrCnpj":"11444777000161"}
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
                                {"name":"Other","email":"other@mail.com","cpfOrCnpj":"11.222.333/0001-81"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("A client with this CPF/CNPJ already exists"));
    }

    @Test
    @DisplayName("GET /clients should return the requested page ordered by name")
    void findAllShouldReturnRequestedPage() throws Exception {
        clientRepository.save(new Client(null, "Carla", "carla@mail.com", "11111111111"));
        clientRepository.save(new Client(null, "Bruno", "bruno@mail.com", "22222222222"));
        clientRepository.save(new Client(null, "Ana", "ana@mail.com", "33333333333"));

        mockMvc.perform(get("/clients?size=2&page=0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].name").value("Ana"))
                .andExpect(jsonPath("$.content[1].name").value("Bruno"))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(2))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(2));
    }

    @Test
    @DisplayName("GET /clients should return 400 for an invalid sort property")
    void findAllShouldReturn400ForInvalidSort() throws Exception {
        mockMvc.perform(get("/clients?sort=foo"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid sort property: 'foo'"));
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

    @Test
    @DisplayName("PUT /clients/{id} should update the client and normalize the data")
    void updateShouldReturn200() throws Exception {
        Client client = saveClient();

        mockMvc.perform(put("/clients/{id}", client.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Acme Corp","email":"Corp@Mail.COM","cpfOrCnpj":"45.723.174/0001-10"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(client.getId().intValue()))
                .andExpect(jsonPath("$.name").value("Acme Corp"))
                .andExpect(jsonPath("$.email").value("corp@mail.com"))
                .andExpect(jsonPath("$.cpfOrCnpj").value("45723174000110"));
    }

    @Test
    @DisplayName("PUT /clients/{id} should allow keeping the client's own email and CPF/CNPJ")
    void updateShouldAllowKeepingOwnUniqueValues() throws Exception {
        Client client = saveClient();

        mockMvc.perform(put("/clients/{id}", client.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Acme Renamed","email":"acme@mail.com","cpfOrCnpj":"11222333000181"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Acme Renamed"));
    }

    @Test
    @DisplayName("PUT /clients/{id} should return 409 when the email belongs to another client")
    void updateShouldReturn409WhenEmailBelongsToAnotherClient() throws Exception {
        saveClient();
        Client other = clientRepository.save(new Client(null, "Other", "other@mail.com", "11444777000161"));

        mockMvc.perform(put("/clients/{id}", other.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Other","email":"ACME@MAIL.COM","cpfOrCnpj":"11444777000161"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("A client with this email already exists"));
    }

    @Test
    @DisplayName("PUT /clients/{id} should return 409 when the CPF/CNPJ belongs to another client")
    void updateShouldReturn409WhenDocumentBelongsToAnotherClient() throws Exception {
        saveClient();
        Client other = clientRepository.save(new Client(null, "Other", "other@mail.com", "11444777000161"));

        mockMvc.perform(put("/clients/{id}", other.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Other","email":"other@mail.com","cpfOrCnpj":"11.222.333/0001-81"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("A client with this CPF/CNPJ already exists"));
    }

    @Test
    @DisplayName("PUT /clients/{id} should return 404 when the client does not exist")
    void updateShouldReturn404() throws Exception {
        mockMvc.perform(put("/clients/{id}", 999999)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Acme Ltda","email":"acme@mail.com","cpfOrCnpj":"11222333000181"}
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Client not found with id: 999999"));
    }

    @Test
    @DisplayName("DELETE /clients/{id} should delete a client without service orders")
    void deleteShouldReturn204() throws Exception {
        Client client = saveClient();

        mockMvc.perform(delete("/clients/{id}", client.getId()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/clients/{id}", client.getId()))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("DELETE /clients/{id} should return 409 when the client has service orders")
    void deleteShouldReturn409WhenClientHasOrders() throws Exception {
        Client client = saveClient();
        saveOrder(client, "Printer down", OrderStatus.OPEN);

        mockMvc.perform(delete("/clients/{id}", client.getId()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Cannot delete a client that has service orders"));
    }

    @Test
    @DisplayName("DELETE /clients/{id} should return 404 when the client does not exist")
    void deleteShouldReturn404() throws Exception {
        mockMvc.perform(delete("/clients/{id}", 999999))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /clients should return 400 when the CPF/CNPJ check digits are wrong")
    void insertShouldReturn400WhenDocumentIsInvalid() throws Exception {
        mockMvc.perform(post("/clients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Acme Ltda","email":"acme@mail.com","cpfOrCnpj":"12345678000199"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("cpfOrCnpj: Invalid CPF or CNPJ"));
    }

    @Test
    @DisplayName("POST /clients should accept a valid CPF with separators")
    void insertShouldAcceptValidCpf() throws Exception {
        mockMvc.perform(post("/clients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Maria Souza","email":"maria@mail.com","cpfOrCnpj":"529.982.247-25"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.cpfOrCnpj").value("52998224725"));
    }

    @Test
    @DisplayName("POST /clients should accept a valid alphanumeric CNPJ")
    void insertShouldAcceptAlphanumericCnpj() throws Exception {
        mockMvc.perform(post("/clients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Nova Empresa","email":"nova@mail.com","cpfOrCnpj":"12.ABC.345/01DE-35"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.cpfOrCnpj").value("12ABC34501DE35"));
    }

    @Test
    @DisplayName("POST /clients should reject an alphanumeric CNPJ written in lower case")
    void insertShouldRejectLowerCaseAlphanumericCnpj() throws Exception {
        mockMvc.perform(post("/clients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Nova Empresa","email":"nova@mail.com","cpfOrCnpj":"12.abc.345/01de-35"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("cpfOrCnpj: Invalid CPF or CNPJ"));
    }
}