package com.serviceOrder.Management.dtos;

import jakarta.validation.constraints.NotBlank;

public record OrderServiceFinishDTO(
        @NotBlank(message = "Root cause report is required to finish the service order")
        String rootCauseReport
) {
}
