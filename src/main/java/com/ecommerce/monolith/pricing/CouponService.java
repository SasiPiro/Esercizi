package com.ecommerce.monolith.pricing;

import com.ecommerce.monolith.pricing.Coupon.CouponStatus;
import com.ecommerce.monolith.pricing.internal.CouponRepository;
import com.ecommerce.monolith.shared.TimeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import jakarta.persistence.EntityManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import static com.ecommerce.monolith.shared.AppConstants.*;

@Service
public class CouponService {

    private static final Logger log = LoggerFactory.getLogger(CouponService.class);

    private final CouponRepository couponRepository;
    private final TimeService timeService;
    private final EntityManager entityManager;

    public CouponService(CouponRepository couponRepository, TimeService timeService,
                         EntityManager entityManager) {
        this.couponRepository = couponRepository;
        this.timeService = timeService;
        this.entityManager = entityManager;
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
     * Redeems one use of a coupon and returns the updated Coupon.
     *
     * <p>
     * <strong>Concurrency: atomic conditional UPDATE.</strong>
     * All checks (ACTIVE, not expired, uses left) and the increment happen in a single
     * statement, so the database serializes concurrent redemptions on the row lock:
     * </p>
     * <pre>
     * UPDATE coupon SET current_usage = current_usage + 1, version = version + 1
     *  WHERE code = ? AND status = 'ACTIVE' AND current_usage &lt; usage_limit AND expiry_date &gt; ?
     * </pre>
     * <p>
     * Compared with optimistic locking (read, increment in memory, check version), this
     * never oversells and never rejects a buyer just because someone else redeemed
     * at the same moment while uses were still available.
     * </p>
     *
     * <p>Must run inside the caller's transaction (the checkout), so a failed checkout
     * also gives the coupon use back.</p>
     *
     * @throws PromotionNotFoundException if the code does not exist or is not ACTIVE
     * @throws PromotionExpiredException  if the coupon has expired or has no uses left
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public Coupon redeemCoupon(String couponCode) {
        int updated = couponRepository.tryRedeem(couponCode, CouponStatus.ACTIVE, timeService.now());
        if (updated == 0) {
            validate(couponCode); // throws the specific error (not found / expired)
            throw new PromotionExpiredException(ERROR_PROMOTION_EXPIRED);
        }

        Coupon coupon = couponRepository.findByCodeAndStatus(couponCode, CouponStatus.ACTIVE)
                .orElseThrow(() -> new PromotionNotFoundException(ERROR_PROMOTION_NOT_FOUND));
        entityManager.refresh(coupon); // the bulk UPDATE bypassed the persistence context

        log.info("[COUPON_REDEEMED] code={} usage={} limit={}",
                coupon.getCode(), coupon.getCurrentUsage(), coupon.getUsageLimit());
        return coupon;
    }
}
