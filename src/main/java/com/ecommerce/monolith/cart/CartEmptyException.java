package com.ecommerce.monolith.cart;

import com.ecommerce.monolith.shared.BusinessException;
import org.springframework.http.HttpStatus;

import static com.ecommerce.monolith.shared.AppConstants.ERROR_CART_EMPTY;
import static com.ecommerce.monolith.shared.AppConstants.TYPE_CART_EMPTY;

public class CartEmptyException extends BusinessException {
    public CartEmptyException(String message) {
        super(HttpStatus.BAD_REQUEST, ERROR_CART_EMPTY, TYPE_CART_EMPTY, message);
    }
}
