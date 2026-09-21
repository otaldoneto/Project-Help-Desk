package com.serviceOrder.Management.dtos;

import com.serviceOrder.Management.entities.Client;
import com.serviceOrder.Management.validation.CpfOrCnpj;
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
