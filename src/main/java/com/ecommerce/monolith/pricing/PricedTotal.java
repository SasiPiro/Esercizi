package com.ecommerce.monolith.pricing;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * The prices of a quote or of a placed order: what the pricing audit
 * ({@code @TrackPromotion}) logs.
 */
public interface PricedTotal {

    BigDecimal initialPrice();

    BigDecimal finalPrice();

    BigDecimal totalDiscountAmount();

    List<String> appliedStrategies();

    String couponUsed();

    /** Total savings as a percentage of the initial price. */
    default double getDiscountPercentage() {
        if (initialPrice().compareTo(BigDecimal.ZERO) == 0) return 0;
        return totalDiscountAmount()
                .divide(initialPrice(), 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .doubleValue();
    }
}
