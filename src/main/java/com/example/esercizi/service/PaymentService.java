package com.example.esercizi.service;

import com.example.esercizi.dto.CalculationResultDto;
import com.example.esercizi.dto.PromotionContext;
import com.example.esercizi.model.Cart;
import com.example.esercizi.model.Session;
import com.example.esercizi.model.User;
import com.example.esercizi.strategy.PromotionStrategy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.example.esercizi.aspect.TrackPromotion;
import com.example.esercizi.constants.AppConstants;
import com.example.esercizi.exception.CartEmptyException;
import com.example.esercizi.exception.CouponNotCumulativeException;
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
    private final CartService cartService;

    public PaymentService(List<PromotionStrategy> strategies, TimeService timeService,
            CartCalculator cartCalculator, CouponService couponService, CartService cartService) {
        this.strategies = strategies;
        this.timeService = timeService;
        this.cartCalculator = cartCalculator;
        this.couponService = couponService;
        this.cartService = cartService;
    }

    /**
     * Calcola il totale del carrello orchestrando il recupero dell'Entity.
     * Metodo principale per i controller.
     */
    @TrackPromotion
    public CalculationResultDto calculateTotal(Session session, User user, String couponCode) {
        Cart cart = cartService.getCartEntity(session, user);
        return calculateTotal(cart, user, couponCode);
    }

    /**
     * Calcola il totale del carrello con approccio ibrido:
     * <ol>
     * <li><strong>Promozioni automatiche</strong> (Strategy Pattern): applica la
     * prima
     * promozione valida in base al contesto (utente VIP, periodo Black Friday,
     * ecc.)</li>
     * <li><strong>Codice coupon</strong> (se presente): applica lo sconto
     * aggiuntivo
     * tramite {@link CouponService}, con gestione della concorrenza via Optimistic
     * Locking</li>
     * </ol>
     *
     * @param cart       Carrello da pagare
     * @param user       Utente corrente (può essere null per guest)
     * @param couponCode Codice coupon opzionale (può essere null)
     * @return Prezzo finale dopo tutti gli sconti applicati
     */
    // 5. Calcolo finale: Base * (1 - Total%)
    @TrackPromotion
    private CalculationResultDto calculateTotal(Cart cart, User user, String couponCode) {
        // Check empty cart
        if (cart == null || cart.getItems() == null || cart.getItems().isEmpty()) {
            throw new CartEmptyException(AppConstants.ERROR_CART_EMPTY);
        }

        // 1. Calcola prezzo base
        BigDecimal baseTotal = cartCalculator.calculateTotal(cart);
        List<String> strategiesApplied = new ArrayList<>();
        PromotionContext context = new PromotionContext(cart, timeService.now(), user);

        BigDecimal accumulatedDiscountPercentage = BigDecimal.ZERO;
        boolean isSeasonalActive = false;

        // 2. Itera sulle strategie automatiche (VIP, Seasonal)
        for (PromotionStrategy strategy : strategies) {
            if (strategy.supports(context)) {
                accumulatedDiscountPercentage = accumulatedDiscountPercentage.add(strategy.getDiscountPercentage());
                strategiesApplied.add(strategy.getClass().getSimpleName());

                if (strategy.getType() == PromotionStrategy.StrategyType.SEASONAL) {
                    isSeasonalActive = true;
                }
            }
        }

        // 3. Gestione Coupon
        if (StringUtils.hasText(couponCode)) {
            // Regola: Coupon NON cumulabile con promo stagionali (Black Friday, Natale)
            // Eccezione: VIP è sempre cumulabile (già sommato sopra)
            if (isSeasonalActive) {
                throw new CouponNotCumulativeException(AppConstants.ERROR_COUPON_NOT_CUMULATIVE);
            }

            // Riscatta coupon e aggiungi percentuale
            Coupon promo = couponService.redeemCoupon(couponCode);
            accumulatedDiscountPercentage = accumulatedDiscountPercentage.add(promo.getDiscountPercentage());
            strategiesApplied.add("COUPON:" + couponCode);
        }

        // 4. Cap al 100% di sconto per sicurezza
        BigDecimal finalRate = accumulatedDiscountPercentage.min(BigDecimal.ONE).max(BigDecimal.ZERO);

        // Formula: Prezzo * (1 - ScontoTotale)
        // Es: 100 * (1 - 0.30) = 70
        BigDecimal multiplier = BigDecimal.ONE.subtract(finalRate);

        BigDecimal finalTotal = baseTotal.multiply(multiplier).setScale(2, RoundingMode.HALF_UP);

        return new CalculationResultDto(
                baseTotal,
                finalTotal,
                baseTotal.subtract(finalTotal),
                strategiesApplied,
                couponCode
        );
    }
}
