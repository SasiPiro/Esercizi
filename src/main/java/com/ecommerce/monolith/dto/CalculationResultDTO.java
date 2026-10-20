package com.ecommerce.monolith.dto;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

public record CalculationResultDTO(BigDecimal initialPrice,
                                   BigDecimal finalPrice,
                                   BigDecimal totalDiscountAmount,
                                   List<String> appliedStrategies,
                                   String couponUsed
) {
    // Utility method to calculate the total savings percentage
    public double getDiscountPercentage() {
        if (initialPrice.compareTo(BigDecimal.ZERO) == 0) return 0;
        return totalDiscountAmount
                .divide(initialPrice, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .doubleValue();
    }
}
