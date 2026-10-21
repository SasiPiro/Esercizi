package com.ecommerce.monolith.pricing.internal;

import com.ecommerce.monolith.cart.Cart;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;

@Component
public class CartCalculator {
    /**
     * Calculates the total price of the cart without applying discounts.
     */
    public BigDecimal calculateTotal(Cart cart) {
        return cart.getItems().stream()
                .map(item -> item.getProduct()
                        .getBasePrice()
                        .multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
