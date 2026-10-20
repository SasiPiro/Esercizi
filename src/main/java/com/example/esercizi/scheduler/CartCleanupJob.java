package com.example.esercizi.scheduler;

import com.example.esercizi.repository.CartOwnershipRepository;
import com.example.esercizi.service.TimeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * Deletes abandoned guest carts every day at 00:01.
 *
 * <p>
 * A single {@code DELETE ... WHERE id IN (subquery)}: the database removes the carts and,
 * through ON DELETE CASCADE, their ownership rows and items. No entity is loaded into memory.
 * </p>
 */
@Component
public class CartCleanupJob {

    private static final Logger log = LoggerFactory.getLogger(CartCleanupJob.class);
    static final Duration GUEST_CART_RETENTION = Duration.ofDays(30);

    private final CartOwnershipRepository ownershipRepository;
    private final TimeService timeService;

    public CartCleanupJob(CartOwnershipRepository ownershipRepository, TimeService timeService) {
        this.ownershipRepository = ownershipRepository;
        this.timeService = timeService;
    }

    @Scheduled(cron = "0 1 0 * * *")
    @Transactional
    public int cleanupExpiredGuestCarts() {
        // created_at is written by @CreationTimestamp in the JVM time zone
        LocalDateTime cutoff = LocalDateTime.ofInstant(
                timeService.now().minus(GUEST_CART_RETENTION), ZoneId.systemDefault());
        int deleted = ownershipRepository.deleteGuestCartsCreatedBefore(cutoff);
        log.info("[CART_CLEANUP] deleted={} guest carts created before {}", deleted, cutoff);
        return deleted;
    }
}
