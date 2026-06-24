package com.example.esercizi.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequestDTO(
        @NotBlank(message = "Username or email is required") @Size(min = 3, max = 50) String username,
       @NotBlank(message = "Required Password") @Size(min = 4, max = 100) String password
) {
}
