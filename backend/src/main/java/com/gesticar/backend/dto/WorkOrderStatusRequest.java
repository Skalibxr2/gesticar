package com.gesticar.backend.dto;

import com.gesticar.backend.domain.WorkOrderStatus;
import jakarta.validation.constraints.NotNull;

public record WorkOrderStatusRequest(@NotNull WorkOrderStatus status) {
}
