package com.ecommerce.monolith.exception;

/**
 * Thrown when an Idempotency-Key was already used by another shopper: the request is
 * rejected without revealing anything about the order created with that key.
 */
public class IdempotencyKeyConflictException extends RuntimeException {
    public IdempotencyKeyConflictException(String message) {
        super(message);
    }
}
