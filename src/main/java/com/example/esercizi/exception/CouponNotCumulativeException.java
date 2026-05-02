package com.example.esercizi.exception;

/**
 * Eccezione lanciata quando si tenta di applicare un coupon
 * mentre è già attiva una promozione incompatibile (es. Sconti Stagionali).
 */
public class CouponNotCumulativeException extends RuntimeException {
    public CouponNotCumulativeException(String message) {
        super(message);
    }
}
