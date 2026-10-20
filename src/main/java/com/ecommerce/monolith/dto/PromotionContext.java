package com.ecommerce.monolith.dto;

import com.ecommerce.monolith.model.Cart;
import com.ecommerce.monolith.model.User;
import java.time.Instant;

public record PromotionContext(Cart cart, Instant now, User user) {
}
// user from security context