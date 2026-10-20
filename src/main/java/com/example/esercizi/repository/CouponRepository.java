package com.example.esercizi.repository;

import com.example.esercizi.model.Coupon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface CouponRepository extends JpaRepository<Coupon, Long> {

    /**
     * Retrieves an active coupon by its unique code.
     */
    Optional<Coupon> findByCodeAndStatus(String code, Coupon.CouponStatus status);

    /**
     * Atomically consumes one use of a coupon, only if it is ACTIVE, not expired and has uses left.
     *
     * @return 1 if the coupon was redeemed, 0 otherwise
     */
    @Modifying(flushAutomatically = true)
    @Query("""
            UPDATE Coupon c
               SET c.currentUsage = c.currentUsage + 1, c.version = c.version + 1
             WHERE c.code = :code
               AND c.status = :status
               AND c.currentUsage < c.usageLimit
               AND c.expiryDate > :now
            """)
    int tryRedeem(@Param("code") String code, @Param("status") Coupon.CouponStatus status,
                  @Param("now") Instant now);

    /**
     * Marks as EXPIRED, in a single statement, every ACTIVE coupon that reached its
     * usage limit or its expiry date. Used by {@code PromotionExpiryJob}.
     *
     * @return the number of coupons expired
     */
    @Modifying
    @Query("""
            UPDATE Coupon c
               SET c.status = :expired, c.version = c.version + 1
             WHERE c.status = :active
               AND (c.currentUsage >= c.usageLimit OR c.expiryDate < :now)
            """)
    int expireStale(@Param("active") Coupon.CouponStatus active,
                    @Param("expired") Coupon.CouponStatus expired,
                    @Param("now") Instant now);
}
