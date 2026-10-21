package com.ecommerce.monolith.pricing;

public class PromotionExpiredException extends RuntimeException {
    public PromotionExpiredException(String message) {
        super(message);
    }
}
