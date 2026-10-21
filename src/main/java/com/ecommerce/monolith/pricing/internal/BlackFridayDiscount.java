package com.ecommerce.monolith.pricing.internal;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class BlackFridayDiscount implements PromotionStrategy {

    private final PromotionConfig promotionConfig;

    public BlackFridayDiscount(PromotionConfig promotionConfig) {
        this.promotionConfig = promotionConfig;
    }

    public boolean supports(PromotionContext promotionContext) {
        return promotionConfig.isActive(promotionConfig.getBlackFriday(), promotionContext.now());
    }

    @Override
    public BigDecimal getDiscountPercentage() {
        return promotionConfig.getBlackFriday().getDiscount();
    }

    @Override
    public StrategyType getType() {
        return StrategyType.SEASONAL;
    }
}
