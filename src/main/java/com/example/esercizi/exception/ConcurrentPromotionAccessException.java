package com.example.esercizi.exception;

/**
 * Exception thrown when two users attempt to redeem the last remaining
 * coupon usage concurrently.
 *
 * <p>
 * This scenario is managed via JPA Optimistic Locking:
 * the {@code @Version} field in the {@code Coupon} entity triggers an
 * {@code OptimisticLockException} when the version read in memory
 * no longer matches the one in the database.
 * </p>
 */
public class ConcurrentPromotionAccessException extends RuntimeException {
    public ConcurrentPromotionAccessException(String message) {
        super(message);
    }
}
