package com.ecommerce.monolith.ordering.internal;

import java.math.BigDecimal;

public record OrderLineDTO(
        String productCode,
        BigDecimal unitPrice,
        Integer quantity) {
}
