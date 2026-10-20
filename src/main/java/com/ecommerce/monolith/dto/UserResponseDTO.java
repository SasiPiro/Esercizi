package com.ecommerce.monolith.dto;

import java.util.Set;


public record UserResponseDTO(
      Long id,
      String username,
      boolean isActive,
      Set<String> roles) {
}
