package com.example.esercizi.scheduler;

import com.example.esercizi.model.Coupon.CouponStatus;
import com.example.esercizi.repository.CouponRepository;
import com.example.esercizi.service.TimeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Promotion lifecycle: every minute, marks as EXPIRED the ACTIVE coupons that reached
 * their usage limit or their expiry date.
 *
 * <p>
 * The work is a single {@code UPDATE ... WHERE} executed by the database. The previous
 * version loaded every coupon, split them into chunks and saved them from parallel
 * threads: the entities were detached, so {@code saveAll} ran a SELECT and an UPDATE per
 * coupon, and a concurrent redemption made a whole chunk fail on the optimistic lock.
 * A set-based statement is faster, atomic and cannot conflict that way.
 * </p>
 */
@Component
public class PromotionExpiryJob {

    private static final Logger log = LoggerFactory.getLogger(PromotionExpiryJob.class);

    private final CouponRepository couponRepository;
    private final TimeService timeService;

    public PromotionExpiryJob(CouponRepository couponRepository, TimeService timeService) {
        this.couponRepository = couponRepository;
        this.timeService = timeService;
    }

    @Scheduled(fixedRate = 60_000)
    @Transactional
    public int expireStalePromotions() {
        Instant now = timeService.now();
        int expired = couponRepository.expireStale(CouponStatus.ACTIVE, CouponStatus.EXPIRED, now);
        if (expired > 0) {
            log.info("[PROMOTION_LIFECYCLE] expired={} timestamp={}", expired, now);
        } else {
            log.debug("[PROMOTION_LIFECYCLE] nothing to expire at timestamp={}", now);
        }
        return expired;
    }
}
