package com.serviceorder.management.dtos;

import jakarta.validation.constraints.NotBlank;

public record ServiceOrderFinishDTO(
        @NotBlank(message = "Root cause report is required to finish the service order")
        String rootCauseReport
) {
}
