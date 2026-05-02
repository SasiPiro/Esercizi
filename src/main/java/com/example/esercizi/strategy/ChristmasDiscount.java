package com.example.esercizi.strategy;

import com.example.esercizi.config.PromotionConfig;
import com.example.esercizi.dto.PromotionContext;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;

import static com.example.esercizi.constants.AppConstants.CHRISTMAS_PROMOTION_END;
import static com.example.esercizi.constants.AppConstants.CHRISTMAS_PROMOTION_START;

@Component
public class ChristmasDiscount implements PromotionStrategy {

    private final PromotionConfig promotionConfig;

    public ChristmasDiscount(PromotionConfig promotionConfig) {
        this.promotionConfig = promotionConfig;
    }

    public boolean supports(PromotionContext context) {
        Instant now = context.now();
        return !now.isBefore(CHRISTMAS_PROMOTION_START) && !now.isAfter(CHRISTMAS_PROMOTION_END);
    }

    @Override
    public BigDecimal getDiscountPercentage() {
        return promotionConfig.getChristmasDiscount();
    }

    @Override
    public StrategyType getType() {
        return StrategyType.SEASONAL;
    }
}
