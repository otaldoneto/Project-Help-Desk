package com.serviceOrder.Management.controllers;

import com.serviceOrder.Management.controllers.exceptions.InvalidCredentialsException;
import com.serviceOrder.Management.dtos.CurrentUserDTO;
import com.serviceOrder.Management.dtos.LoginRequestDTO;
import com.serviceOrder.Management.dtos.TokenResponseDTO;
import com.serviceOrder.Management.services.AuthService;
import com.serviceOrder.Management.services.LoginAttemptService;
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

    public AuthController(AuthService service, LoginAttemptService loginAttemptService) {
        this.service = service;
        this.loginAttemptService = loginAttemptService;
    }

    @Operation(summary = "Logs in",
            description = "Returns a JWT. Send it in the header 'Authorization: Bearer <token>'. " +
                    "After 5 failed attempts from the same IP address within 15 minutes, further attempts are " +
                    "rejected with 429 until the window passes")
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
        // Not trusting a client-supplied X-Forwarded-For here, since this app has no trusted reverse
        // proxy in front of it that would overwrite it: an attacker could otherwise spoof a new IP
        // on every request and bypass the limit entirely.
        String clientIp = request.getRemoteAddr();
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