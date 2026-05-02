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
     * Recupera un coupon attivo tramite il suo codice univoco.
     */
    Optional<Coupon> findByCodeAndStatus(String code, Coupon.CouponStatus status);

    /**
     * Trova tutte le promozioni ACTIVE che devono essere marcate EXPIRED.
     * Condizioni: utilizzo massimo raggiunto OPPURE data di scadenza superata.
     *
     * <p>
     * Usata dallo Scheduled Job {@code PromotionExpiryJob} ogni 60 secondi.
     * </p>
     */
    @Query("""
            SELECT p FROM Coupon p
            WHERE p.status = com.example.esercizi.model.Coupon$CouponStatus.ACTIVE
            AND (p.currentUsage >= p.usageLimit OR p.expiryDate < :now)
            """)
    List<Coupon> findExpirablePromotions(@Param("now") Instant now);
}
