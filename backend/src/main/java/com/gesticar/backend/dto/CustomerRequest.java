package com.gesticar.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record CustomerRequest(
        @NotBlank String rut,
        @NotBlank String firstName,
        @NotBlank String lastName,
        String phone,
        @Email String email
) {
}
