package com.example.esercizi.strategy;

import com.example.esercizi.dto.PromotionContext;

import java.math.BigDecimal;

public interface PromotionStrategy {

    /**
     * Enum defining the type of strategy.
     * Useful for applying priority rules (e.g. VIP is always active, SEASONAL has
     * priority over COUPON).
     */
    enum StrategyType {
        VIP,
        SEASONAL
    }

    /**
     * Determines if this promotion is applicable to the current context.
     */
    boolean supports(PromotionContext context);

    /**
     * Returns the discount percentage to apply (e.g. 0.10 for 10%).
     */
    BigDecimal getDiscountPercentage();

    /**
     * Returns the strategy type.
     */
    StrategyType getType();
}
