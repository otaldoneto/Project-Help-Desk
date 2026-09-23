package com.serviceorder.management.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangePasswordDTO(
        @NotBlank(message = "Current password is required")
        String currentPassword,

        // BCrypt only uses the first 72 bytes of the password
        @NotBlank(message = "New password is required")
        @Size(min = 8, max = 72, message = "New password must have between 8 and 72 characters")
        String newPassword
) {
    @Override
    public String toString() {
        return "ChangePasswordDTO[currentPassword=***, newPassword=***]";
    }
}