package com.gesticar.backend.dto;

import com.gesticar.backend.domain.WorkOrderStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record WorkOrderRequest(
        @NotBlank String code,
        @NotBlank String description,
        WorkOrderStatus status,
        @NotNull Long customerId,
        Long vehicleId
) {
}
