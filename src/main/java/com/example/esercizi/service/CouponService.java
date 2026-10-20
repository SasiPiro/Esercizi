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
     * Read-only check used by the checkout preview: the coupon must exist, be ACTIVE,
     * not expired and have uses left. Does NOT consume a use.
     */
    @Transactional(readOnly = true)
    public Coupon validate(String couponCode) {
        Coupon coupon = couponRepository
                .findByCodeAndStatus(couponCode, CouponStatus.ACTIVE)
                .orElseThrow(() -> new PromotionNotFoundException(ERROR_PROMOTION_NOT_FOUND));
        if (!coupon.getExpiryDate().isAfter(timeService.now())
                || coupon.getCurrentUsage() >= coupon.getUsageLimit()) {
            throw new PromotionExpiredException(ERROR_PROMOTION_EXPIRED);
        }
        return coupon;
    }

    /**
     * Redeems a coupon code and returns the updated Coupon object.
     *
     * <p>
     * <strong>Concurrency Flow (Optimistic Locking):</strong>
     * </p>
     * <ol>
     * <li>The transaction reads the {@code Coupon} record from the DB, including the {@code version} field.</li>
     * <li>Increments {@code currentUsage} in memory.</li>
     * <li>Calls {@code saveAndFlush()} which immediately forces the UPDATE on the DB.
     * JPA generates:
     * {@code UPDATE coupon SET ..., version = version + 1 WHERE id = ? AND version = ?}</li>
     * <li>If another transaction has already incremented the version, the UPDATE
     * modifies 0 rows
     * and JPA throws {@code OptimisticLockingFailureException}.</li>
     * <li>The exception is caught and converted into a
     * {@code ConcurrentPromotionAccessException},
     * which the {@code GlobalExceptionHandler} translates to HTTP 409 CONFLICT.</li>
     * </ol>
     *
     * @param couponCode coupon code entered by the user
     * @return the redeemed Coupon object (with incremented usage)
     * @throws PromotionNotFoundException         if the code does not exist or is not ACTIVE
     * @throws PromotionExpiredException          if the coupon has expired or has reached its usage limit
     * @throws ConcurrentPromotionAccessException if another user redeemed the last usage concurrently
     */
    @Transactional
    public Coupon redeemCoupon(String couponCode) {

        // 1. Lookup: find the ACTIVE coupon with the specified code
        Coupon promo = couponRepository
                .findByCodeAndStatus(couponCode, CouponStatus.ACTIVE)
                .orElseThrow(() -> new PromotionNotFoundException(ERROR_PROMOTION_NOT_FOUND));

        // 2. Temporal validation
        if (promo.getExpiryDate().isBefore(timeService.now())) {
            throw new PromotionExpiredException(ERROR_PROMOTION_EXPIRED);
        }

        // 3. Usage limit validation
        if (promo.getCurrentUsage() >= promo.getUsageLimit()) {
            throw new PromotionExpiredException(ERROR_PROMOTION_EXPIRED);
        }

        // 4. Usage increment (in memory)
        promo.setCurrentUsage(promo.getCurrentUsage() + 1);

        // 5. Flush to DB: the @Version check triggers here
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
