package com.serviceOrder.Management.dtos;

import com.serviceOrder.Management.enums.OrderPriority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ServiceOrderCreateDTO(
        @NotBlank(message = "Title is required")
        String title,

        @NotBlank(message = "Description is required")
        String description,

        @NotNull(message = "Priority is required")
        OrderPriority priority,

        @NotNull(message = "Client ID is required")
        Long clientId

) {
}
