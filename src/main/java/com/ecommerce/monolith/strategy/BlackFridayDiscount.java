package com.ecommerce.monolith.strategy;

import com.ecommerce.monolith.config.PromotionConfig;
import com.ecommerce.monolith.dto.PromotionContext;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;

import static com.ecommerce.monolith.constants.AppConstants.BLACKFRIDAY_PROMOTION_END;
import static com.ecommerce.monolith.constants.AppConstants.BLACKFRIDAY_PROMOTION_START;

@Component
public class BlackFridayDiscount implements PromotionStrategy {

    private final PromotionConfig promotionConfig;

    public BlackFridayDiscount(PromotionConfig promotionConfig) {
        this.promotionConfig = promotionConfig;
    }

    public boolean supports(PromotionContext promotionContext) {
        Instant now = promotionContext.now();
        return !now.isBefore(BLACKFRIDAY_PROMOTION_START) && !now.isAfter(BLACKFRIDAY_PROMOTION_END);
    }

    @Override
    public BigDecimal getDiscountPercentage() {
        return promotionConfig.getBlackFridayDiscount();
    }

    @Override
    public StrategyType getType() {
        return StrategyType.SEASONAL;
    }
}
