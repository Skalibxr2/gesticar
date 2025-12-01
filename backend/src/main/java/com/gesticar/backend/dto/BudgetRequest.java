package com.gesticar.backend.dto;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record BudgetRequest(
        @NotNull BigDecimal amount,
        Boolean approved,
        String notes,
        @NotNull Long workOrderId
) {
}
