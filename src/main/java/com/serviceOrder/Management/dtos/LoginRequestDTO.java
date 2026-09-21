package com.serviceOrder.Management.dtos;

import jakarta.validation.constraints.NotBlank;

public record LoginRequestDTO(
        @NotBlank(message = "Email is required")
        String email,

        @NotBlank(message = "Password is required")
        String password
) {
    @Override
    public String toString() {
        return "LoginRequestDTO[email=" + email + ", password=***]";
    }
}