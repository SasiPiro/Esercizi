package com.ecommerce.monolith.pricing;

public class PromotionNotFoundException extends RuntimeException {
    public PromotionNotFoundException(String message) {
        super(message);
    }
}
