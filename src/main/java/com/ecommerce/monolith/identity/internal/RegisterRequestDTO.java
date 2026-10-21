package com.ecommerce.monolith.identity.internal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequestDTO(
       @NotBlank(message = "Required Username") @Size(min = 3, max = 50) String username,
       @NotBlank(message = "Required Password") @Size(min = 6, max = 100) String password
) {
}
