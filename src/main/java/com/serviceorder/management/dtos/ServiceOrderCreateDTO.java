package com.serviceorder.management.dtos;

import com.serviceorder.management.enums.OrderPriority;
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
