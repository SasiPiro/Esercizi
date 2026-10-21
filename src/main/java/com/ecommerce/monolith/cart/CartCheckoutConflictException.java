package com.ecommerce.monolith.cart;

import com.ecommerce.monolith.shared.BusinessException;
import org.springframework.http.HttpStatus;

import static com.ecommerce.monolith.shared.AppConstants.TYPE_CHECKOUT_CONFLICT;

/**
 * Thrown when a cart is already being checked out (e.g. a double-submitted checkout).
 */
public class CartCheckoutConflictException extends BusinessException {
    public CartCheckoutConflictException(String message) {
        super(HttpStatus.CONFLICT, "Checkout conflict", TYPE_CHECKOUT_CONFLICT, message);
    }
}
