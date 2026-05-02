package com.example.esercizi.service;

import com.example.esercizi.exception.ConcurrentPromotionAccessException;
import com.example.esercizi.exception.PromotionExpiredException;
import com.example.esercizi.exception.PromotionNotFoundException;
import com.example.esercizi.model.Coupon;
import com.example.esercizi.model.Coupon.CouponStatus;
import com.example.esercizi.repository.CouponRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.example.esercizi.constants.AppConstants.*;

@Service
public class CouponService {

    private static final Logger log = LoggerFactory.getLogger(CouponService.class);

    private final CouponRepository couponRepository;
    private final TimeService timeService;

    public CouponService(CouponRepository couponRepository, TimeService timeService) {
        this.couponRepository = couponRepository;
        this.timeService = timeService;
    }

    /**
     * Riscatta un codice coupon e restituisce l'oggetto Promotion aggiornato.
     *
     * <p>
     * <strong>Flusso di concorrenza (Optimistic Locking):</strong>
     * </p>
     * <ol>
     * <li>La transazione legge il record {@code Promotion} dal DB, incluso il campo
     * {@code version}.</li>
     * <li>Incrementa {@code currentUsage} in memoria.</li>
     * <li>Chiama {@code saveAndFlush()} che forza immediatamente l'UPDATE sul DB.
     * JPA genera:
     * {@code UPDATE promotions SET ..., version = version + 1 WHERE id = ? AND version = ?}</li>
     * <li>Se un'altra transazione ha già incrementato la versione, l'UPDATE
     * modifica 0 righe
     * e JPA lancia {@code OptimisticLockingFailureException}.</li>
     * <li>L'eccezione viene catturata e convertita in
     * {@code ConcurrentPromotionAccessException},
     * che il {@code GlobalExceptionHandler} traduce in HTTP 409 CONFLICT.</li>
     * </ol>
     *
     * @param couponCode codice coupon inserito dall'utente
     * @return l'oggetto Promotion riscattato (con usage incrementato)
     * @throws PromotionNotFoundException         se il codice non esiste o non è
     *                                            ACTIVE
     * @throws PromotionExpiredException          se il coupon è scaduto o ha
     *                                            raggiunto il limite di utilizzi
     * @throws ConcurrentPromotionAccessException se un altro utente ha riscattato
     *                                            l'ultimo utilizzo
     *                                            contemporaneamente
     */
    @Transactional
    public Coupon redeemCoupon(String couponCode) {

        // 1. Lookup: trova il coupon ACTIVE con il codice specificato
        Coupon promo = couponRepository
                .findByCodeAndStatus(couponCode, CouponStatus.ACTIVE)
                .orElseThrow(() -> new PromotionNotFoundException(ERROR_PROMOTION_NOT_FOUND));

        // 2. Validazione temporale
        if (promo.getExpiryDate().isBefore(timeService.now())) {
            throw new PromotionExpiredException(ERROR_PROMOTION_EXPIRED);
        }

        // 3. Validazione limite utilizzi
        if (promo.getCurrentUsage() >= promo.getUsageLimit()) {
            throw new PromotionExpiredException(ERROR_PROMOTION_EXPIRED);
        }

        // 4. Incremento usage (in memoria)
        promo.setCurrentUsage(promo.getCurrentUsage() + 1);

        // 5. Flush verso DB: qui scatta il check di @Version
        try {
            couponRepository.saveAndFlush(promo);
        } catch (ObjectOptimisticLockingFailureException ex) {
            log.warn("[COUPON_CONFLICT] code={} version={} error={}",
                    couponCode, promo.getVersion(), ex.getMessage());
            throw new ConcurrentPromotionAccessException(ERROR_CONCURRENT_PROMOTION);
        }

        log.info("[COUPON_REDEEMED] code={} usage={}/{}",
                promo.getCode(),
                promo.getCurrentUsage(),
                promo.getUsageLimit());

        return promo;
    }
}
