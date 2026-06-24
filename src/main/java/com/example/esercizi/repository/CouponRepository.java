package com.example.esercizi.repository;

import com.example.esercizi.model.Coupon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface CouponRepository extends JpaRepository<Coupon, Long> {

    /**
     * Retrieves an active coupon by its unique code.
     */
    Optional<Coupon> findByCodeAndStatus(String code, Coupon.CouponStatus status);

    /**
     * Finds all ACTIVE promotions that need to be marked as EXPIRED.
     * Conditions: maximum usage reached OR expiry date has passed.
     *
     * <p>
     * Used by the {@code PromotionExpiryJob} Scheduled Job every 60 seconds.
     * </p>
     */
    @Query("""
            SELECT p FROM Coupon p
            WHERE p.status = com.example.esercizi.model.Coupon$CouponStatus.ACTIVE
            AND (p.currentUsage >= p.usageLimit OR p.expiryDate < :now)
            """)
    List<Coupon> findExpirablePromotions(@Param("now") Instant now);
}
