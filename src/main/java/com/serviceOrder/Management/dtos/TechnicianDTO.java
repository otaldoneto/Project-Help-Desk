package com.serviceOrder.Management.dtos;

import com.serviceOrder.Management.entities.Technician;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record TechnicianDTO(
        Long id,

        @NotBlank(message = "Name is required")
        String name,

        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        String email,

        @NotBlank(message = "Specialty  is required")
        String specialty
) {
    public TechnicianDTO(Technician entity) {
        this(entity.getId(), entity.getName(), entity.getEmail(), entity.getSpecialty());
    }
}
