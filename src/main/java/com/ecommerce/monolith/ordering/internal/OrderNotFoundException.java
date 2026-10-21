package com.ecommerce.monolith.ordering.internal;

import com.ecommerce.monolith.shared.BusinessException;
import org.springframework.http.HttpStatus;

import static com.ecommerce.monolith.shared.AppConstants.TYPE_ORDER_NOT_FOUND;

public class OrderNotFoundException extends BusinessException {
    public OrderNotFoundException(String message) {
        super(HttpStatus.NOT_FOUND, "Order not found", TYPE_ORDER_NOT_FOUND, message);
    }
}
