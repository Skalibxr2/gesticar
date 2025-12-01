package com.gesticar.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record TaskRequest(
        @NotBlank String title,
        String details,
        Integer estimatedHours,
        @NotNull Long workOrderId
) {
}
