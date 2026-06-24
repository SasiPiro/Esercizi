package com.example.esercizi.service;

import com.example.esercizi.dto.CalculationResultDTO;
import com.example.esercizi.dto.CartDTO;
import com.example.esercizi.dto.PromotionContext;
import com.example.esercizi.mapper.CartMapper;
import com.example.esercizi.model.Cart;
import com.example.esercizi.model.User;
import com.example.esercizi.repository.UserRepository;
import com.example.esercizi.strategy.PromotionStrategy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.example.esercizi.aspect.TrackPromotion;
import com.example.esercizi.constants.AppConstants;
import com.example.esercizi.exception.CartEmptyException;
import com.example.esercizi.exception.CouponNotCumulativeException;
import com.example.esercizi.facade.AuthFacade;
import com.example.esercizi.model.Coupon;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
public class PaymentService {

    private final List<PromotionStrategy> strategies;
    private final TimeService timeService;
    private final CartCalculator cartCalculator;
    private final CouponService couponService;
    private final UserRepository userRepository;
    private final CartMapper cartMapper;
    private final AuthFacade authFacade;

    public PaymentService(List<PromotionStrategy> strategies, TimeService timeService,
            CartCalculator cartCalculator, CouponService couponService,
            CartService cartService, UserRepository userRepository,
            AuthFacade authFacade, CartMapper cartMapper) {
        this.strategies = strategies;
        this.timeService = timeService;
        this.cartCalculator = cartCalculator;
        this.couponService = couponService;
        this.userRepository = userRepository;
        this.cartMapper = cartMapper;
        this.authFacade = authFacade;
    }

    /**
     * Calculates the total price of the cart using a hybrid approach:
     * <ol>
     * <li><strong>Automatic promotions</strong> (Strategy Pattern): applies the
     * active promotions based on the context (VIP user, Black Friday period,
     * etc.)</li>
     * <li><strong>Coupon code</strong> (if present): applies the additional discount
     * via {@link CouponService}, with concurrency managed through Optimistic
     * Locking</li>
     * </ol>
     *
     * @param cartDTO    Cart to pay for
     * @param couponCode Optional coupon code (can be null)
     * @return Final price after all applied discounts
     */
    // Final calculation: Base * (1 - Total%)
    @TrackPromotion
    public CalculationResultDTO calculateTotal(CartDTO cartDTO, String couponCode) {
        // Check empty cart
        if (cartDTO == null || cartDTO.items() == null || cartDTO.items().isEmpty()) {
            throw new CartEmptyException(AppConstants.ERROR_CART_EMPTY);
        }

        String principal = authFacade.getCurrentUsername();
        User user = userRepository.findByUsernameWithRoles(principal).orElse(null);

        Cart cart = cartMapper.toEntity(cartDTO);

        // 1. Calculate base price
        BigDecimal baseTotal = cartCalculator.calculateTotal(cart);
        List<String> strategiesApplied = new ArrayList<>();
        PromotionContext context = new PromotionContext(cart, timeService.now(), user);

        BigDecimal accumulatedDiscountPercentage = BigDecimal.ZERO;
        boolean isSeasonalActive = false;

        // 2. Iterate over automatic strategies (VIP, Seasonal)
        for (PromotionStrategy strategy : strategies) {
            if (strategy.supports(context)) {
                accumulatedDiscountPercentage = accumulatedDiscountPercentage.add(strategy.getDiscountPercentage());
                strategiesApplied.add(strategy.getClass().getSimpleName());

                if (strategy.getType() == PromotionStrategy.StrategyType.SEASONAL) {
                    isSeasonalActive = true;
                }
            }
        }

        // 3. Coupon management
        if (StringUtils.hasText(couponCode)) {
            // Rule: Coupon cannot be combined with seasonal promotions (Black Friday, Christmas)
            // Exception: VIP is always combinable (already added above)
            if (isSeasonalActive) {
                throw new CouponNotCumulativeException(AppConstants.ERROR_COUPON_NOT_CUMULATIVE);
            }

            // Redeem coupon and add percentage
            Coupon promo = couponService.redeemCoupon(couponCode);
            accumulatedDiscountPercentage = accumulatedDiscountPercentage.add(promo.getDiscountPercentage());
            strategiesApplied.add("COUPON:" + couponCode);
        }

        // 4. Cap at 100% discount for safety
        BigDecimal finalRate = accumulatedDiscountPercentage.min(BigDecimal.ONE).max(BigDecimal.ZERO);

        // Formula: Price * (1 - TotalDiscount)
        // E.g.: 100 * (1 - 0.30) = 70
        BigDecimal multiplier = BigDecimal.ONE.subtract(finalRate);

        BigDecimal finalTotal = baseTotal.multiply(multiplier).setScale(2, RoundingMode.HALF_UP);

        return new CalculationResultDTO(
                baseTotal,
                finalTotal,
                baseTotal.subtract(finalTotal),
                strategiesApplied,
                couponCode);
    }
}
