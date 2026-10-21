package com.ecommerce.monolith.dto;

import java.math.BigDecimal;
import java.util.List;

public record CalculationResultDTO(BigDecimal initialPrice,
                                   BigDecimal finalPrice,
                                   BigDecimal totalDiscountAmount,
                                   List<String> appliedStrategies,
                                   String couponUsed
) implements PricedTotal {
}
