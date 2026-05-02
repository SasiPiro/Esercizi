package com.example.esercizi.exception;

public class PromotionExpiredException extends RuntimeException {
    public PromotionExpiredException(String message) {
        super(message);
    }
}
