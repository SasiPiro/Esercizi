package com.ecommerce.monolith.dto;

import java.math.BigDecimal;

public record ProductDTO(
                Long id,
                String code,
                BigDecimal basePrice) {
}
