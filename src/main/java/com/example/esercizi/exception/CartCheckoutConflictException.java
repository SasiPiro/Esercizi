package com.example.esercizi.exception;

/**
 * Thrown when a cart is already being checked out (e.g. a double-submitted checkout).
 */
public class CartCheckoutConflictException extends RuntimeException {
    public CartCheckoutConflictException(String message) {
        super(message);
    }
}
