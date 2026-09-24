package com.serviceorder.management.controllers;

import com.jayway.jsonpath.JsonPath;
import com.serviceorder.management.entities.Client;
import com.serviceorder.management.entities.ServiceOrder;
import com.serviceorder.management.entities.Technician;
import com.serviceorder.management.enums.OrderStatus;
import com.serviceorder.management.enums.UserRole;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.function.Function;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// The VIEWER role backs the public demo account: it must be able to read everything and change nothing.
@WithAnonymousUser
class ViewerAccessTest extends ApiTestSupport {

    private static final String PASSWORD = "password123";

    private Client client;
    private Technician technician;
    private ServiceOrder openOrder;
    private ServiceOrder inProgressOrder;
    private String viewerToken;
    private String adminToken;

    @BeforeEach
    void setUpData() throws Exception {
        client = saveClient();
        technician = saveTechnician();
        openOrder = saveOrder(client, "Printer down", OrderStatus.OPEN);
        inProgressOrder = saveOrder(client, "Network down", OrderStatus.IN_PROGRESS);
        saveUser("viewer@mail.com", PASSWORD, UserRole.VIEWER);
        saveUser("admin@mail.com", PASSWORD, UserRole.ADMIN);
        viewerToken = loginAndGetToken("viewer@mail.com", PASSWORD);
        adminToken = loginAndGetToken("admin@mail.com", PASSWORD);
    }

    static Stream<Arguments> readRequests() {
        return Stream.of(
                request("GET /clients", t -> get("/clients")),
                request("GET /clients/{id}", t -> get("/clients/{id}", t.client.getId())),
                request("GET /technicians", t -> get("/technicians")),
                request("GET /technicians/{id}", t -> get("/technicians/{id}", t.technician.getId())),
                request("GET /orders", t -> get("/orders")),
                request("GET /orders?status=OPEN", t -> get("/orders").param("status", "OPEN")),
                request("GET /orders/{id}", t -> get("/orders/{id}", t.openOrder.getId())),
                request("GET /orders/{id}/report", t -> get("/orders/{id}/report", t.openOrder.getId())),
                request("GET /auth/me", t -> get("/auth/me")));
    }

    static Stream<Arguments> writeRequests() {
        return Stream.of(
                request("POST /clients", t -> post("/clients").contentType(MediaType.APPLICATION_JSON).content("""
                        {"name":"Other Ltda","email":"other@mail.com","cpfOrCnpj":"11444777000161"}
                        """)),
                request("PUT /clients/{id}", t -> put("/clients/{id}", t.client.getId())
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"name":"Acme Renamed","email":"acme@mail.com","cpfOrCnpj":"11222333000181"}
                                """)),
                request("DELETE /clients/{id}", t -> delete("/clients/{id}", t.client.getId())),
                request("POST /technicians", t -> post("/technicians").contentType(MediaType.APPLICATION_JSON).content("""
                        {"name":"Bruno Lima","email":"bruno@mail.com","specialty":"Printers"}
                        """)),
                request("PUT /technicians/{id}", t -> put("/technicians/{id}", t.technician.getId())
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"name":"Carlos Silva","email":"carlos@mail.com","specialty":"Servers"}
                                """)),
                request("DELETE /technicians/{id}", t -> delete("/technicians/{id}", t.technician.getId())),
                request("POST /orders", t -> post("/orders").contentType(MediaType.APPLICATION_JSON).content("""
                        {"title":"Monitor flickering","description":"Flickers after 10 minutes","priority":"LOW","clientId":%d}
                        """.formatted(t.client.getId()))),
                request("PUT /orders/{id}/assign/{technicianId}",
                        t -> put("/orders/{id}/assign/{technicianId}", t.openOrder.getId(), t.technician.getId())),
                request("PUT /orders/{id}/finish", t -> put("/orders/{id}/finish", t.inProgressOrder.getId())
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"rootCauseReport":"Power supply replaced"}
                                """)),
                request("PUT /orders/{id}/cancel", t -> put("/orders/{id}/cancel", t.openOrder.getId())),
                request("PUT /users/me/password", t -> put("/users/me/password")
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"currentPassword":"%s","newPassword":"another-password"}
                                """.formatted(PASSWORD))),
                request("POST /users", t -> post("/users").contentType(MediaType.APPLICATION_JSON).content("""
                        {"name":"Bruno Lima","email":"bruno@mail.com","password":"secret1234","role":"USER"}
                        """)));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("readRequests")
    @DisplayName("a VIEWER should be able to read")
    void viewerCanRead(String name, Function<ViewerAccessTest, MockHttpServletRequestBuilder> request) throws Exception {
        mockMvc.perform(request.apply(this).header("Authorization", bearer(viewerToken)))
                .andExpect(status().isOk());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("writeRequests")
    @DisplayName("a VIEWER should get 403 on every write, while an ADMIN gets through the same request")
    void viewerCannotWrite(String name, Function<ViewerAccessTest, MockHttpServletRequestBuilder> request) throws Exception {
        mockMvc.perform(request.apply(this).header("Authorization", bearer(viewerToken)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Forbidden"));

        // Control: the same request passes security for an ADMIN, so the 403 above comes from the role and not
        // from a wrong URL or method in this test.
        int adminStatus = mockMvc.perform(request.apply(this).header("Authorization", bearer(adminToken)))
                .andReturn().getResponse().getStatus();
        assertThat(adminStatus).isNotIn(401, 403, 404, 405);
    }

    @Test
    @DisplayName("a VIEWER should get 403 when listing users")
    void viewerCannotListUsers() throws Exception {
        mockMvc.perform(get("/users").header("Authorization", bearer(viewerToken)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("the VIEWER's writes should leave the data untouched")
    void viewerWritesDoNotChangeData() throws Exception {
        mockMvc.perform(put("/orders/{id}/cancel", openOrder.getId()).header("Authorization", bearer(viewerToken)))
                .andExpect(status().isForbidden());

        assertThat(orderRepository.findById(openOrder.getId()).orElseThrow().getStatus()).isEqualTo(OrderStatus.OPEN);
    }

    @Test
    @DisplayName("a VIEWER should be able to refresh its token and log out")
    void viewerCanRefreshAndLogOut() throws Exception {
        String login = mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON).content("""
                        {"email":"viewer@mail.com","password":"%s"}
                        """.formatted(PASSWORD)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String refreshToken = JsonPath.read(login, "$.refreshToken");

        String refreshed = mockMvc.perform(post("/auth/refresh").contentType(MediaType.APPLICATION_JSON).content("""
                        {"refreshToken":"%s"}
                        """.formatted(refreshToken)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        mockMvc.perform(post("/auth/logout").contentType(MediaType.APPLICATION_JSON).content("""
                        {"refreshToken":"%s"}
                        """.formatted(JsonPath.<String>read(refreshed, "$.refreshToken"))))
                .andExpect(status().is2xxSuccessful());
    }

    @Test
    @DisplayName("an ADMIN should be able to create a VIEWER, who can then log in")
    void adminCanCreateViewer() throws Exception {
        mockMvc.perform(post("/users")
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Demo","email":"demo@mail.com","password":"demo-password","role":"VIEWER"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("VIEWER"));

        String token = loginAndGetToken("demo@mail.com", "demo-password");
        mockMvc.perform(get("/orders").header("Authorization", bearer(token)))
                .andExpect(status().isOk());
    }

    private static Arguments request(String name, Function<ViewerAccessTest, MockHttpServletRequestBuilder> builder) {
        return Arguments.of(name, builder);
    }
}
