package com.ecommerce.monolith.ordering.internal;

import com.ecommerce.monolith.shared.BusinessException;
import org.springframework.http.HttpStatus;

import static com.ecommerce.monolith.shared.AppConstants.TYPE_IDEMPOTENCY_KEY_REUSED;

/**
 * Thrown when an Idempotency-Key was already used by another shopper: the request is
 * rejected without revealing anything about the order created with that key.
 */
public class IdempotencyKeyConflictException extends BusinessException {
    public IdempotencyKeyConflictException(String message) {
        super(HttpStatus.CONFLICT, "Idempotency key already used", TYPE_IDEMPOTENCY_KEY_REUSED, message);
    }
}
