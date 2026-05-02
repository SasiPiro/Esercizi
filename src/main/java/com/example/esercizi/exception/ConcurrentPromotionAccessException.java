package com.example.esercizi.exception;

/**
 * Eccezione lanciata quando due utenti tentano di riscattare
 * contemporaneamente l'ultimo utilizzo rimasto di un coupon.
 *
 * <p>
 * Questa situazione è gestita tramite Optimistic Locking di JPA:
 * il campo {@code @Version} nell'entità {@code Coupon} causa un
 * {@code OptimisticLockException} quando la versione letta in memoria
 * non corrisponde più a quella nel database.
 * </p>
 */
public class ConcurrentPromotionAccessException extends RuntimeException {
    public ConcurrentPromotionAccessException(String message) {
        super(message);
    }
}
