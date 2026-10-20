package com.ecommerce.monolith.strategy;

import com.ecommerce.monolith.config.PromotionConfig;
import com.ecommerce.monolith.dto.PromotionContext;
import com.ecommerce.monolith.model.User;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class VipExclusiveDiscount implements PromotionStrategy {

    private final PromotionConfig promotionConfig;

    public VipExclusiveDiscount(PromotionConfig promotionConfig) {
        this.promotionConfig = promotionConfig;
    }

    public boolean supports(PromotionContext promotionContext) {
        User user = promotionContext.user();
        return user != null && user.isVip();
    }

    @Override
    public BigDecimal getDiscountPercentage() {
        return promotionConfig.getVipDiscount();
    }

    @Override
    public StrategyType getType() {
        return StrategyType.VIP;
    }
}
