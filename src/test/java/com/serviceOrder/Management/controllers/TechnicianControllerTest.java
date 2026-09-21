package com.serviceOrder.Management.controllers;

import com.serviceOrder.Management.entities.Technician;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TechnicianControllerTest extends ApiTestSupport {

    @Test
    @DisplayName("POST /technicians should create a technician")
    void insertShouldReturn201() throws Exception {
        mockMvc.perform(post("/technicians")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Carlos Silva","email":"Carlos@Mail.com","specialty":"Networking"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.email").value("carlos@mail.com"));
    }

    @Test
    @DisplayName("POST /technicians should return 409 when the email already exists")
    void insertShouldReturn409WhenEmailExists() throws Exception {
        saveTechnician();

        mockMvc.perform(post("/technicians")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Other","email":"CARLOS@MAIL.COM","specialty":"Hardware"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("A technician with this email already exists"));
    }

    @Test
    @DisplayName("POST /technicians should return 400 when a field is missing")
    void insertShouldReturn400WhenSpecialtyIsMissing() throws Exception {
        mockMvc.perform(post("/technicians")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Carlos Silva","email":"carlos@mail.com"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("specialty: Specialty is required"));
    }

    @Test
    @DisplayName("GET /technicians/{id} should return 404 when the technician does not exist")
    void findByIdShouldReturn404() throws Exception {
        mockMvc.perform(get("/technicians/{id}", 999999))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Technician not found with id: 999999"));
    }

    @Test
    @DisplayName("GET /technicians should return the requested page ordered by name")
    void findAllShouldReturnRequestedPage() throws Exception {
        technicianRepository.save(new Technician(null, "Carla", "carla@mail.com", "Hardware"));
        technicianRepository.save(new Technician(null, "Bruno", "bruno@mail.com", "Networking"));
        technicianRepository.save(new Technician(null, "Ana", "ana@mail.com", "Software"));

        mockMvc.perform(get("/technicians?size=2&page=0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].name").value("Ana"))
                .andExpect(jsonPath("$.content[1].name").value("Bruno"))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(2));
    }

    @Test
    @DisplayName("GET /technicians should return 400 for an invalid sort property")
    void findAllShouldReturn400ForInvalidSort() throws Exception {
        mockMvc.perform(get("/technicians?sort=foo"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid sort property: 'foo'"));
    }
}