package com.ecommerce.monolith.exception;

public class PromotionNotFoundException extends RuntimeException {
    public PromotionNotFoundException(String message) {
        super(message);
    }
}
