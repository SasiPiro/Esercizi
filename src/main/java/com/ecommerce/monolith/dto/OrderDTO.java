package com.ecommerce.monolith.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record OrderDTO(
        Long id,
        Instant placedAt,
        List<OrderLineDTO> lines,
        BigDecimal initialPrice,
        BigDecimal finalPrice,
        BigDecimal totalDiscountAmount,
        List<String> appliedStrategies,
        String couponUsed) implements PricedTotal {
}
