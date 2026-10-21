package com.ecommerce.monolith.cart.internal;

import com.ecommerce.monolith.cart.CartService;
import com.ecommerce.monolith.identity.GuestSessionStarted;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Creates the cart of a new guest when its token is issued. A plain, synchronous
 * {@code @EventListener} on purpose: the cart must exist before the token reaches the guest.
 */
@Component
class GuestCartInitializer {

    private final CartService cartService;

    GuestCartInitializer(CartService cartService) {
        this.cartService = cartService;
    }

    @EventListener
    void on(GuestSessionStarted event) {
        cartService.createGuestCart(event.anonymousId());
    }
}
