package com.example.esercizi.exception;

/**
 * Exception thrown when attempting to apply a coupon
 * when an incompatible promotion is already active (e.g. Seasonal Discounts).
 */
public class CouponNotCumulativeException extends RuntimeException {
    public CouponNotCumulativeException(String message) {
        super(message);
    }
}
