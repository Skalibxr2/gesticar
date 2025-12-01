package com.gesticar.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record VehicleRequest(
        @NotBlank String licensePlate,
        @NotBlank String brand,
        @NotBlank String model,
        Integer year,
        @NotNull Long customerId
) {
}
