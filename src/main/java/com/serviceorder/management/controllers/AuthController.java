package com.serviceorder.management.controllers;

import com.serviceorder.management.config.ClientIpResolver;
import com.serviceorder.management.controllers.exceptions.InvalidCredentialsException;
import com.serviceorder.management.dtos.CurrentUserDTO;
import com.serviceorder.management.dtos.LoginRequestDTO;
import com.serviceorder.management.dtos.RefreshTokenRequestDTO;
import com.serviceorder.management.dtos.TokenResponseDTO;
import com.serviceorder.management.services.AuthService;
import com.serviceorder.management.services.LoginAttemptService;
import com.serviceorder.management.services.RefreshTokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
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
    private final LoginAttemptService loginAttemptService;
    private final RefreshTokenService refreshTokenService;
    private final ClientIpResolver clientIpResolver;

    public AuthController(AuthService service, LoginAttemptService loginAttemptService,
                          RefreshTokenService refreshTokenService, ClientIpResolver clientIpResolver) {
        this.service = service;
        this.loginAttemptService = loginAttemptService;
        this.refreshTokenService = refreshTokenService;
        this.clientIpResolver = clientIpResolver;
    }

    @Operation(summary = "Logs in",
            description = "Returns an access token (short-lived, send it in the header 'Authorization: Bearer " +
                    "<token>') and a refresh token (long-lived, use it with POST /auth/refresh to get a new " +
                    "access token without logging in again). After 5 failed attempts from the same IP address " +
                    "within 15 minutes, further attempts are rejected with 429 until the window passes")
    @SecurityRequirements
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Login successful"),
            @ApiResponse(responseCode = "400", description = "Invalid input data"),
            @ApiResponse(responseCode = "401", description = "Invalid email or password"),
            @ApiResponse(responseCode = "429", description = "Too many failed attempts from this IP address")
    })
    @PostMapping(value = "/login")
    public ResponseEntity<TokenResponseDTO> login(@Valid @RequestBody LoginRequestDTO dto,
                                                  HttpServletRequest request) {
        // Client-supplied headers are ignored unless app.client-ip-header names one that a trusted CDN overwrites.
        String clientIp = clientIpResolver.resolve(request);
        loginAttemptService.checkAllowed(clientIp);

        try {
            TokenResponseDTO token = service.login(dto);
            loginAttemptService.recordSuccess(clientIp);
            return ResponseEntity.ok(token);
        } catch (InvalidCredentialsException e) {
            loginAttemptService.recordFailure(clientIp);
            throw e;
        }
    }

    @Operation(summary = "Gets a new access token using a refresh token",
            description = "The refresh token used is invalidated (rotated): a new refresh token is returned " +
                    "along with the new access token, and the old refresh token can no longer be used")
    @SecurityRequirements
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "New token pair issued"),
            @ApiResponse(responseCode = "400", description = "Invalid input data"),
            @ApiResponse(responseCode = "401", description = "Invalid, expired or already used refresh token")
    })
    @PostMapping(value = "/refresh")
    public ResponseEntity<TokenResponseDTO> refresh(@Valid @RequestBody RefreshTokenRequestDTO dto) {
        return ResponseEntity.ok(refreshTokenService.refresh(dto.refreshToken()));
    }

    @Operation(summary = "Logs out",
            description = "Invalidates the given refresh token. The current access token remains valid until " +
                    "it expires, since access tokens cannot be revoked early")
    @SecurityRequirements
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Logged out")
    })
    @PostMapping(value = "/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody RefreshTokenRequestDTO dto) {
        refreshTokenService.revoke(dto.refreshToken());
        return ResponseEntity.noContent().build();
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