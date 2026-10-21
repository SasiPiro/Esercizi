package com.ecommerce.monolith.pricing.internal;

import com.ecommerce.monolith.cart.Cart;
import com.ecommerce.monolith.identity.User;
import java.time.Instant;

public record PromotionContext(Cart cart, Instant now, User user) {
}
// user from security context