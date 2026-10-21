package com.ecommerce.monolith.shared;

public class MissingResourceException extends RuntimeException {
    public MissingResourceException(String message) {
        super(message);
    }
}
