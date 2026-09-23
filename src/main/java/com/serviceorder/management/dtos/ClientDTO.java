package com.serviceorder.management.dtos;

import com.serviceorder.management.entities.Client;
import com.serviceorder.management.validation.CpfOrCnpj;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record ClientDTO(
        Long id,

        @NotBlank(message = "Name is required")
        String name,

        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        String email,

        @NotBlank(message = "CPF/CNPJ is required")
        @CpfOrCnpj
        String cpfOrCnpj
) {
    public ClientDTO(Client entity) {
        this(entity.getId(), entity.getName(), entity.getEmail(), entity.getCpfOrCnpj());
    }
}
