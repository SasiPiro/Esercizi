package com.ecommerce.monolith.dto;

import java.math.BigDecimal;

public record OrderLineDTO(
        String productCode,
        BigDecimal unitPrice,
        Integer quantity) {
}
