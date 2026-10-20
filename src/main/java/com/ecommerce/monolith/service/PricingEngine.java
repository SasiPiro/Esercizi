package com.ecommerce.monolith.service;

import com.ecommerce.monolith.constants.AppConstants;
import com.ecommerce.monolith.dto.CalculationResultDTO;
import com.ecommerce.monolith.dto.PromotionContext;
import com.ecommerce.monolith.exception.CartEmptyException;
import com.ecommerce.monolith.exception.CouponNotCumulativeException;
import com.ecommerce.monolith.model.Cart;
import com.ecommerce.monolith.model.Coupon;
import com.ecommerce.monolith.model.User;
import com.ecommerce.monolith.strategy.PromotionStrategy;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Pure pricing logic: no repositories, no security context.
 *
 * <p>
 * It only ever receives a {@link Cart} loaded from the database, so product prices
 * always come from the server and never from the client request.
 * </p>
 */
@Component
public class PricingEngine {

    private final List<PromotionStrategy> strategies;
    private final CartCalculator cartCalculator;

    public PricingEngine(List<PromotionStrategy> strategies, CartCalculator cartCalculator) {
        this.strategies = strategies;
        this.cartCalculator = cartCalculator;
    }

    /**
     * Throws {@link CouponNotCumulativeException} when a coupon is combined with an
     * active seasonal promotion. Call it before redeeming the coupon.
     */
    public void assertCouponAllowed(Cart cart, User user, Instant now) {
        PromotionContext context = new PromotionContext(cart, now, user);
        boolean seasonalActive = strategies.stream()
                .anyMatch(s -> s.getType() == PromotionStrategy.StrategyType.SEASONAL && s.supports(context));
        if (seasonalActive) {
            throw new CouponNotCumulativeException(AppConstants.ERROR_COUPON_NOT_CUMULATIVE);
        }
    }

    public CalculationResultDTO price(Cart cart, User user, Instant now, Optional<Coupon> coupon) {
        if (cart == null || cart.getItems() == null || cart.getItems().isEmpty()) {
            throw new CartEmptyException(AppConstants.ERROR_CART_EMPTY);
        }

        BigDecimal baseTotal = cartCalculator.calculateTotal(cart);
        PromotionContext context = new PromotionContext(cart, now, user);

        List<String> applied = new ArrayList<>();
        BigDecimal rate = BigDecimal.ZERO;
        for (PromotionStrategy strategy : strategies) {
            if (strategy.supports(context)) {
                rate = rate.add(strategy.getDiscountPercentage());
                applied.add(strategy.getClass().getSimpleName());
            }
        }

        if (coupon.isPresent()) {
            // Rule: a coupon cannot be combined with seasonal promotions (VIP is always combinable)
            assertCouponAllowed(cart, user, now);
            rate = rate.add(coupon.get().getDiscountPercentage());
            applied.add("COUPON:" + coupon.get().getCode());
        }

        // Cap at 100% discount for safety. Formula: Price * (1 - TotalDiscount)
        BigDecimal finalRate = rate.min(BigDecimal.ONE).max(BigDecimal.ZERO);
        BigDecimal finalTotal = baseTotal.multiply(BigDecimal.ONE.subtract(finalRate))
                .setScale(2, RoundingMode.HALF_UP);

        return new CalculationResultDTO(
                baseTotal,
                finalTotal,
                baseTotal.subtract(finalTotal),
                applied,
                coupon.map(Coupon::getCode).orElse(null));
    }
}
