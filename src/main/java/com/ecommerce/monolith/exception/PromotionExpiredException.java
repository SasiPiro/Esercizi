package com.ecommerce.monolith.exception;

public class PromotionExpiredException extends RuntimeException {
    public PromotionExpiredException(String message) {
        super(message);
    }
}
