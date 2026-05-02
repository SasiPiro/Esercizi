package com.example.esercizi.scheduler;

import com.example.esercizi.model.Coupon;
import com.example.esercizi.model.Coupon.CouponStatus;
import com.example.esercizi.repository.CouponRepository;
import com.example.esercizi.service.TimeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * Job schedulato per la gestione del ciclo di vita delle promozioni (Data
 * Lifecycle Management).
 *
 * <p>
 * Ogni 60 secondi, questo job:
 * </p>
 * <ol>
 * <li>Cerca nel database tutte le promozioni ACTIVE che hanno
 * {@code currentUsage >= usageLimit}
 * oppure una {@code expiryDate} passata.</li>
 * <li>Cambia il loro stato in {@code EXPIRED}.</li>
 * <li>Logga il numero di promozioni disattivate.</li>
 * </ol>
 *
 * <p>
 * <strong>Perché è importante:</strong> In un ambiente di produzione, dati
 * "stale" (vecchi/obsoleti)
 * possono causare problemi di performance e di business. Un utente potrebbe
 * tentare di riscattare
 * un coupon che è tecnicamente scaduto ma non ancora marcato come tale. Questo
 * job garantisce
 * la coerenza dei dati indipendentemente dalle richieste degli utenti.
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
     * Esegue la scadenza automatica delle promozioni ogni 60 secondi.
     *
     * <p>
     * {@code fixedRate = 60_000} significa che il job si avvia ogni 60 secondi
     * dall'inizio dell'esecuzione precedente, indipendentemente dalla durata.
     * </p>
     */
    @Scheduled(fixedRate = 60_000)
    @Transactional
    public void expireStalePromotions() {

        Instant now = timeService.now();

        List<Coupon> expirable = couponRepository.findExpirablePromotions(now);

        if (expirable.isEmpty()) {
            log.info("[PROMOTION_LIFECYCLE] No promotions to expire at timestamp={}", now);
            return;
        }

        expirable.forEach(promo -> {
            promo.setStatus(CouponStatus.EXPIRED);
            log.info(
                    "[PROMOTION_LIFECYCLE] Expiring promotion code={} reason={}",
                    promo.getCode(),
                    promo.getCurrentUsage() >= promo.getUsageLimit() ? "usage_limit_reached" : "date_expired");
        });

        couponRepository.saveAll(expirable);

        // Structured logging per monitoring (ELK/Splunk/Datadog)
        log.info("[PROMOTION_LIFECYCLE] expired_count={} timestamp={}", expirable.size(), now);
    }
}
