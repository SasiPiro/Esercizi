package com.ecommerce.monolith.catalog;

import java.math.BigDecimal;

public record ProductDTO(
                Long id,
                String code,
                BigDecimal basePrice) {
}
