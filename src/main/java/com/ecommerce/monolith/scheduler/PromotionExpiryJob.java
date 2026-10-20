package com.ecommerce.monolith.scheduler;

import com.ecommerce.monolith.model.Coupon.CouponStatus;
import com.ecommerce.monolith.repository.CouponRepository;
import com.ecommerce.monolith.service.TimeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;

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

    /**
     * The lock is held at least 50 seconds, so across all replicas the job runs at most
     * once per minute even if their schedules are not aligned. The transaction is opened
     * by the repository method, inside the lock.
     */
    @Scheduled(fixedRate = 60_000)
    @SchedulerLock(name = "expireStalePromotions", lockAtLeastFor = "PT50S", lockAtMostFor = "PT5M")
    public void expireStalePromotions() {
        Instant now = timeService.now();
        int expired = couponRepository.expireStale(CouponStatus.ACTIVE, CouponStatus.EXPIRED, now);
        if (expired > 0) {
            log.info("[COUPON_EXPIRY] expired={} at={}", expired, now);
        } else {
            log.debug("[COUPON_EXPIRY] expired=0 at={}", now);
        }
    }
}
