package com.ecommerce.monolith.strategy;

import com.ecommerce.monolith.config.PromotionConfig;
import com.ecommerce.monolith.dto.PromotionContext;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class ChristmasDiscount implements PromotionStrategy {

    private final PromotionConfig promotionConfig;

    public ChristmasDiscount(PromotionConfig promotionConfig) {
        this.promotionConfig = promotionConfig;
    }

    public boolean supports(PromotionContext context) {
        return promotionConfig.isActive(promotionConfig.getChristmas(), context.now());
    }

    @Override
    public BigDecimal getDiscountPercentage() {
        return promotionConfig.getChristmas().getDiscount();
    }

    @Override
    public StrategyType getType() {
        return StrategyType.SEASONAL;
    }
}
