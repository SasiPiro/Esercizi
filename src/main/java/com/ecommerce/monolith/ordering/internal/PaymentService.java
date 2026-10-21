package com.ecommerce.monolith.ordering.internal;

import com.ecommerce.monolith.cart.Cart;
import com.ecommerce.monolith.cart.CartEmptyException;
import com.ecommerce.monolith.cart.CartItem;
import com.ecommerce.monolith.cart.CartService;
import com.ecommerce.monolith.ordering.OrderPlaced;
import com.ecommerce.monolith.pricing.CalculationResultDTO;
import com.ecommerce.monolith.pricing.Coupon;
import com.ecommerce.monolith.pricing.CouponService;
import com.ecommerce.monolith.pricing.PricingEngine;
import com.ecommerce.monolith.pricing.TrackPromotion;
import com.ecommerce.monolith.shared.AfterCommit;
import com.ecommerce.monolith.shared.AppConstants;
import com.ecommerce.monolith.shared.TimeService;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.event.Level;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.Optional;

/**
 * Checkout flow, split in two operations:
 * <ul>
 * <li>{@link #quote(String)}: read-only preview of the total. Never consumes a coupon.</li>
 * <li>{@link #placeOrder(String, String)}: confirms the purchase, redeems the coupon, closes the
 * cart and records an {@link Order} with a snapshot of what was sold and at what price.</li>
 * </ul>
 * In both cases the cart is loaded from the database for the authenticated principal:
 * the client never sends items or prices. Replaying a checkout with the same idempotency
 * key is handled by {@link OrderService#placeOrder(String, String)}, which calls this.
 */
@Service
public class PaymentService {

    private final CartService cartService;
    private final CouponService couponService;
    private final PricingEngine pricingEngine;
    private final TimeService timeService;
    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;
    private final ApplicationEventPublisher events;
    private final MeterRegistry meterRegistry;

    public PaymentService(CartService cartService, CouponService couponService,
            PricingEngine pricingEngine, TimeService timeService,
            OrderRepository orderRepository, OrderMapper orderMapper, ApplicationEventPublisher events,
            MeterRegistry meterRegistry) {
        this.cartService = cartService;
        this.couponService = couponService;
        this.pricingEngine = pricingEngine;
        this.timeService = timeService;
        this.orderRepository = orderRepository;
        this.orderMapper = orderMapper;
        this.events = events;
        this.meterRegistry = meterRegistry;
    }

    @TrackPromotion(level = Level.DEBUG) // previews are frequent: not a business event
    @Transactional // not readOnly: the first request of a principal may create its empty cart
    public CalculationResultDTO quote(String couponCode) {
        CartService.CurrentCart current = cartService.currentCart();
        Optional<Coupon> coupon = StringUtils.hasText(couponCode)
                ? Optional.of(couponService.validate(couponCode))
                : Optional.empty();
        return pricingEngine.price(current.cart(), current.user(), timeService.now(), coupon);
    }

    /**
     * @throws org.springframework.dao.DataIntegrityViolationException if an order with this
     *         idempotency key already exists (unique constraint): nothing is redeemed or closed
     */
    @TrackPromotion
    @Transactional
    public OrderDTO placeOrder(String idempotencyKey, String couponCode) {
        CartService.CurrentCart current = cartService.currentCart();
        Cart cart = current.cart();
        Instant now = timeService.now();

        if (cart.getItems().isEmpty()) {
            throw new CartEmptyException(AppConstants.ERROR_CART_EMPTY);
        }
        // Atomic guard against double-submitted checkouts of the same cart
        cartService.claimForCheckout(cart);

        Optional<Coupon> coupon = Optional.empty();
        if (StringUtils.hasText(couponCode)) {
            // Check the combination rule BEFORE consuming a coupon use
            pricingEngine.assertCouponAllowed(cart, current.user(), now);
            coupon = Optional.of(couponService.redeemCoupon(couponCode));
        }

        CalculationResultDTO result = pricingEngine.price(cart, current.user(), now, coupon);
        // Same transaction as the coupon redemption: no order without its redemption, and vice versa
        Order order = recordOrder(idempotencyKey, current, result, now);
        // Stored in the publication registry in this transaction; listeners run after commit
        events.publishEvent(new OrderPlaced(order.getId(), order.getUserId(), order.getFinalPrice()));
        // Count, sum and max of the amounts paid; never tagged by user or order (unbounded
        // cardinality). Not named "...total": Prometheus reserves that suffix for counters.
        AfterCommit.run(() -> meterRegistry.summary("shop.checkout.amount", "outcome", "success")
                .record(order.getFinalPrice().doubleValue()));

        cart.setCartStatus(Cart.CartStatus.PURCHASED);
        cartService.replacePurchasedCart(cart);
        return orderMapper.toDto(order);
    }

    private Order recordOrder(String idempotencyKey, CartService.CurrentCart current,
            CalculationResultDTO result, Instant now) {
        Long userId = current.user() != null ? current.user().getId() : null;
        Order order = new Order(idempotencyKey, userId, current.anonymousId(), now);
        for (CartItem item : current.cart().getItems()) {
            order.addLine(item.getProduct(), item.getQuantity());
        }
        order.setInitialPrice(result.initialPrice());
        order.setFinalPrice(result.finalPrice());
        order.setCouponCode(result.couponUsed());
        order.setAppliedStrategies(String.join(",", result.appliedStrategies()));
        return orderRepository.save(order);
    }
}
