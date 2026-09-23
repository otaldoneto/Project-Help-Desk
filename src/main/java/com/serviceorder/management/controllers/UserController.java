package com.serviceorder.management.controllers;

import com.serviceorder.management.dtos.ChangePasswordDTO;
import com.serviceorder.management.dtos.PageResponseDTO;
import com.serviceorder.management.dtos.UserCreateDTO;
import com.serviceorder.management.dtos.UserDTO;
import com.serviceorder.management.services.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Users", description = "User administration. Most endpoints are ADMIN only; changing your own password is open to any authenticated user")
@RestController
@RequestMapping(value = "/users")
public class UserController {
    private final UserService service;

    public UserController(UserService service) {
        this.service = service;
    }

    @Operation(summary = "Lists users (ADMIN only)", description = "Paginated list ordered by name")
    @GetMapping
    public ResponseEntity<PageResponseDTO<UserDTO>> findAll(
            @PageableDefault(size = 20, sort = {"name", "id"}) Pageable pageable) {
        return ResponseEntity.ok(PageResponseDTO.from(service.findAll(pageable)));
    }

    @Operation(summary = "Creates a user (ADMIN only)", description = "The password is stored as a BCrypt hash")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "User created"),
            @ApiResponse(responseCode = "400", description = "Invalid input data"),
            @ApiResponse(responseCode = "401", description = "Missing, invalid or expired token"),
            @ApiResponse(responseCode = "403", description = "Only an ADMIN can create users"),
            @ApiResponse(responseCode = "409", description = "Email already registered")
    })
    @PostMapping
    public ResponseEntity<UserDTO> create(@Valid @RequestBody UserCreateDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(dto));
    }

    @Operation(summary = "Disables a user (ADMIN only)",
            description = "A disabled user cannot log in. Tokens already issued remain valid until they expire")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User disabled"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @PutMapping(value = "/{id}/disable")
    public ResponseEntity<UserDTO> disable(@PathVariable Long id) {
        return ResponseEntity.ok(service.setEnabled(id, false));
    }

    @Operation(summary = "Re-enables a user (ADMIN only)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User enabled"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @PutMapping(value = "/{id}/enable")
    public ResponseEntity<UserDTO> enable(@PathVariable Long id) {
        return ResponseEntity.ok(service.setEnabled(id, true));
    }

    @Operation(summary = "Changes the authenticated user's own password",
            description = "Open to any authenticated user, not just ADMIN")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Password changed"),
            @ApiResponse(responseCode = "400", description = "Invalid input data"),
            @ApiResponse(responseCode = "401", description = "Missing, invalid or expired token, or current password is incorrect")
    })
    @PutMapping(value = "/me/password")
    public ResponseEntity<Void> changeMyPassword(@AuthenticationPrincipal Jwt jwt,
                                                 @Valid @RequestBody ChangePasswordDTO dto) {
        service.changePassword(jwt.getSubject(), dto);
        return ResponseEntity.noContent().build();
    }
}