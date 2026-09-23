package com.serviceorder.management.dtos;

import com.serviceorder.management.enums.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UserCreateDTO(
        @NotBlank(message = "Name is required")
        String name,

        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        String email,

        // BCrypt only uses the first 72 bytes of the password
        @NotBlank(message = "Password is required")
        @Size(min = 8, max = 72, message = "Password must have between 8 and 72 characters")
        String password,

        @NotNull(message = "Role is required")
        UserRole role
) {
    @Override
    public String toString() {
        return "UserCreateDTO[name=" + name + ", email=" + email + ", password=***, role=" + role + "]";
    }
}