package com.example.esercizi.service;

import com.example.esercizi.aspect.TrackPromotion;
import com.example.esercizi.constants.AppConstants;
import com.example.esercizi.dto.CalculationResultDTO;
import com.example.esercizi.exception.CartCheckoutConflictException;
import com.example.esercizi.exception.CartEmptyException;
import com.example.esercizi.model.Cart;
import com.example.esercizi.model.Coupon;
import com.example.esercizi.repository.CartRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.Optional;

/**
 * Checkout flow, split in two operations:
 * <ul>
 * <li>{@link #quote(String)}: read-only preview of the total. Never consumes a coupon.</li>
 * <li>{@link #placeOrder(String)}: confirms the purchase, redeems the coupon and closes the cart.</li>
 * </ul>
 * In both cases the cart is loaded from the database for the authenticated principal:
 * the client never sends items or prices.
 */
@Service
public class PaymentService {

    private final CartService cartService;
    private final CartRepository cartRepository;
    private final CouponService couponService;
    private final PricingEngine pricingEngine;
    private final TimeService timeService;

    public PaymentService(CartService cartService, CartRepository cartRepository,
            CouponService couponService, PricingEngine pricingEngine, TimeService timeService) {
        this.cartService = cartService;
        this.cartRepository = cartRepository;
        this.couponService = couponService;
        this.pricingEngine = pricingEngine;
        this.timeService = timeService;
    }

    @TrackPromotion
    @Transactional // not readOnly: the first request of a principal may create its empty cart
    public CalculationResultDTO quote(String couponCode) {
        CartService.CurrentCart current = cartService.currentCart();
        Optional<Coupon> coupon = StringUtils.hasText(couponCode)
                ? Optional.of(couponService.validate(couponCode))
                : Optional.empty();
        return pricingEngine.price(current.cart(), current.user(), timeService.now(), coupon);
    }

    @TrackPromotion
    @Transactional
    public CalculationResultDTO placeOrder(String couponCode) {
        CartService.CurrentCart current = cartService.currentCart();
        Cart cart = current.cart();
        Instant now = timeService.now();

        if (cart.getItems().isEmpty()) {
            throw new CartEmptyException(AppConstants.ERROR_CART_EMPTY);
        }
        // Atomic guard against double-submitted checkouts of the same cart
        if (cartRepository.claimForCheckout(cart.getId()) == 0) {
            throw new CartCheckoutConflictException("Cart is already being checked out");
        }

        Optional<Coupon> coupon = Optional.empty();
        if (StringUtils.hasText(couponCode)) {
            // Check the combination rule BEFORE consuming a coupon use
            pricingEngine.assertCouponAllowed(cart, current.user(), now);
            coupon = Optional.of(couponService.redeemCoupon(couponCode));
        }

        CalculationResultDTO result = pricingEngine.price(cart, current.user(), now, coupon);

        cart.setCartStatus(Cart.CartStatus.PURCHASED);
        cartService.replacePurchasedCart(cart);
        // Next step: persist an Order with a price snapshot and publish an OrderPlaced event
        return result;
    }
}
