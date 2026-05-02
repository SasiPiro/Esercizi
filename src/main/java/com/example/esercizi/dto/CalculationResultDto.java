package com.example.esercizi.dto;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

public record CalculationResultDto (BigDecimal initialPrice,
                                    BigDecimal finalPrice,
                                    BigDecimal totalDiscountAmount,
                                    List<String> appliedStrategies,
                                    String couponUsed
) {
    // Metodo utility per calcolare la percentuale totale di risparmio
    public double getDiscountPercentage() {
        if (initialPrice.compareTo(BigDecimal.ZERO) == 0) return 0;
        return totalDiscountAmount
                .divide(initialPrice, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .doubleValue();
    }
}
