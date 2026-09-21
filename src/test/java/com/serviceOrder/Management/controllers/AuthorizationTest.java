package com.serviceOrder.Management.controllers;

import com.serviceOrder.Management.entities.Client;
import com.serviceOrder.Management.enums.UserRole;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.test.context.support.WithAnonymousUser;

import java.time.Instant;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WithAnonymousUser
class AuthorizationTest extends ApiTestSupport {

    @Autowired
    private JwtEncoder jwtEncoder;

    @Test
    @DisplayName("a request without a token should return 401 in the standard error format")
    void requestWithoutTokenShouldReturn401() throws Exception {
        mockMvc.perform(get("/clients"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().exists("WWW-Authenticate"))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.path").value("/clients"));
    }

    @Test
    @DisplayName("a request with a made-up token should return 401")
    void requestWithInvalidTokenShouldReturn401() throws Exception {
        mockMvc.perform(get("/clients").header("Authorization", "Bearer not-a-real-token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("a request with a correctly signed but expired token should return 401")
    void requestWithExpiredTokenShouldReturn401() throws Exception {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("service-order-management")
                .subject("ana@mail.com")
                .issuedAt(now.minusSeconds(7200))
                .expiresAt(now.minusSeconds(3600))
                .claim("roles", List.of("USER"))
                .build();
        String expiredToken = jwtEncoder
                .encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();

        mockMvc.perform(get("/clients").header("Authorization", bearer(expiredToken)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("a USER should be able to read data")
    void userCanReadData() throws Exception {
        saveUser("ana@mail.com", "password123", UserRole.USER);
        String token = loginAndGetToken("ana@mail.com", "password123");

        mockMvc.perform(get("/clients").header("Authorization", bearer(token)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("a USER should get 403 when deleting a client")
    void userCannotDeleteClient() throws Exception {
        Client client = saveClient();
        saveUser("ana@mail.com", "password123", UserRole.USER);
        String token = loginAndGetToken("ana@mail.com", "password123");

        mockMvc.perform(delete("/clients/{id}", client.getId()).header("Authorization", bearer(token)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Forbidden"));
    }

    @Test
    @DisplayName("a USER should get 403 when creating users")
    void userCannotCreateUsers() throws Exception {
        saveUser("ana@mail.com", "password123", UserRole.USER);
        String token = loginAndGetToken("ana@mail.com", "password123");

        mockMvc.perform(post("/users")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Bruno Lima","email":"bruno@mail.com","password":"secret1234","role":"USER"}
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("an ADMIN should be able to delete a client")
    void adminCanDeleteClient() throws Exception {
        Client client = saveClient();
        saveUser("admin@mail.com", "password123", UserRole.ADMIN);
        String token = loginAndGetToken("admin@mail.com", "password123");

        mockMvc.perform(delete("/clients/{id}", client.getId()).header("Authorization", bearer(token)))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("the API documentation should be public")
    void apiDocsShouldBePublic() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk());
    }
}