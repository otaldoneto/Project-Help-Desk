package com.serviceOrder.Management.controllers;

import com.serviceOrder.Management.dtos.CurrentUserDTO;
import com.serviceOrder.Management.dtos.LoginRequestDTO;
import com.serviceOrder.Management.dtos.TokenResponseDTO;
import com.serviceOrder.Management.services.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Authentication", description = "Login and information about the authenticated user")
@RestController
@RequestMapping(value = "/auth")
public class AuthController {
    private final AuthService service;

    public AuthController(AuthService service) {
        this.service = service;
    }

    @Operation(summary = "Logs in",
            description = "Returns a JWT. Send it in the header 'Authorization: Bearer <token>'")
    @SecurityRequirements
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Login successful"),
            @ApiResponse(responseCode = "400", description = "Invalid input data"),
            @ApiResponse(responseCode = "401", description = "Invalid email or password")
    })
    @PostMapping(value = "/login")
    public ResponseEntity<TokenResponseDTO> login(@Valid @RequestBody LoginRequestDTO dto) {
        return ResponseEntity.ok(service.login(dto));
    }

    @Operation(summary = "Shows the authenticated user", description = "Email and roles read from the token")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Token is valid"),
            @ApiResponse(responseCode = "401", description = "Missing, invalid or expired token")
    })
    @GetMapping(value = "/me")
    public ResponseEntity<CurrentUserDTO> me(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(new CurrentUserDTO(jwt.getSubject(), jwt.getClaimAsStringList("roles")));
    }
}