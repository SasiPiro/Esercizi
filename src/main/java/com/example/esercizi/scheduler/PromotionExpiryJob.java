package com.example.esercizi.scheduler;

import com.example.esercizi.model.Coupon;
import com.example.esercizi.model.Coupon.CouponStatus;
import com.example.esercizi.repository.CouponRepository;
import com.example.esercizi.service.TimeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Scheduled job for promotion lifecycle management (Data
 * Lifecycle Management).
 * </p>
 * <ol>
 * <li>Searches the database for all ACTIVE promotions that have
 * {@code currentUsage >= usageLimit}
 * or a past {@code expiryDate}.</li>
 * <li>Changes their status to {@code EXPIRED}.</li>
 * <li>Logs the number of deactivated promotions.</li>
 * </ol>
 * 
 * <h3>How multithreading works here:</h3>
 * 
 * <pre>
 * ┌────────────────────────────────────────────────────────────────┐
 * │  MAIN THREAD (@Scheduled)                                      │
 * │                                                                │
 * │  1. Loads ALL expired coupons from DB (1 query)                │
 * │  2. Splits them into chunks of 500                             │
 * │  3. Submits each chunk to the ExecutorService                  │
 * │  4. Waits for all chunks to complete                           │
 * │  5. Logs the aggregated result                                 │
 * └───────────────┬───────────────┬───────────────┬────────────────┘
 *                 │               │               │
 *        ┌────────▼──────┐ ┌─────▼────────┐ ┌────▼─────────┐
 *        │ Virtual       │ │ Virtual      │ │ Virtual      │
 *        │ Thread 1      │ │ Thread 2     │ │ Thread N     │
 *        │               │ │              │ │              │
 *        │ Chunk 1:      │ │ Chunk 2:     │ │ Chunk N:     │
 *        │ coupon 1-500  │ │ coupon       │ │ coupon       │
 *        │               │ │ 501-1000     │ │ (N-1)*500+1  │
 *        │ - setExpired  │ │ - setExpired │ │ - setExpired │
 *        │ - saveAll()   │ │ - saveAll()  │ │ - saveAll()  │
 *        │               │ │              │ │              │
 *        │ INDEPENDENT   │ │ INDEPENDENT  │ │ INDEPENDENT  │
 *        │ TRANSACTION   │ │ TRANSACTION  │ │ TRANSACTION  │
 *        └───────────────┘ └──────────────┘ └──────────────┘
 * </pre>
 *
 * <h3>Why does each chunk have its own transaction?</h3>
 * <p>
 * If chunk 2 fails (e.g., deadlock, timeout), chunks 1 and 3 are ALREADY
 * committed.
 * We do not lose the work done. This pattern is called
 * <strong>"Chunk Processing with Independent Transactions"</strong>.
 * 
 * <pre>
 * transactionTemplate.executeWithoutResult(status -> {
 *     // Everything inside here is in ONE transaction
 *     // If an exception is thrown → automatic ROLLBACK
 *     // If completed without errors → automatic COMMIT
 * });
 * </pre>
 */
@Component
public class PromotionExpiryJob {

    private static final Logger log = LoggerFactory.getLogger(PromotionExpiryJob.class);

    /**
     * Size of each chunk.
     * 500 is a good compromise:
     * - Large enough not to create too many tasks (scheduling overhead)
     * - Small enough not to block a single thread for too long
     * - In production, this value would go into application.yaml
     */
    private static final int CHUNK_SIZE = 500;

    private final CouponRepository couponRepository;
    private final TimeService timeService;
    private final ExecutorService executorService;
    private final TransactionTemplate transactionTemplate;

    public PromotionExpiryJob(CouponRepository couponRepository, TimeService timeService,
            ExecutorService executorService, PlatformTransactionManager txManager) {
        this.couponRepository = couponRepository;
        this.timeService = timeService;
        this.executorService = executorService;
        // TransactionTemplate wraps Spring's PlatformTransactionManager
        // and provides a simple API to manage transactions programmatically
        this.transactionTemplate = new TransactionTemplate(txManager);
    }

    /**
     * Automatically expires promotions every 60 seconds.
     * <p>
     * NOTE: Does NOT have @Transactional because each chunk manages its own
     * transaction.
     * If we put @Transactional here, it would create a transaction on the main
     * thread
     * that would NOT propagate to child threads (JPA transactions are
     * thread-bound).
     * </p>
     */
    @Scheduled(fixedRate = 60_000)
    public void expireStalePromotions() {

        Instant now = timeService.now();

        // ═══════════════════════════════════════════════════
        // STEP 1: Load data (main thread, 1 query)
        // ═══════════════════════════════════════════════════
        List<Coupon> expirable = couponRepository.findExpirablePromotions(now);

        if (expirable.isEmpty()) {
            log.info("[PROMOTION_LIFECYCLE] No promotions to expire at timestamp={}", now);
            return;
        }

        log.info("[PROMOTION_LIFECYCLE] Found {} promotions to expire. Processing in parallel (chunkSize={})...",
                expirable.size(), CHUNK_SIZE);

        // ═══════════════════════════════════════════════════
        // STEP 2: Split into chunks
        // ═══════════════════════════════════════════════════
        // E.g.: 1,200 coupons with CHUNK_SIZE=500 → 3 chunks: [500, 500, 200]
        List<List<Coupon>> chunks = partitionList(expirable, CHUNK_SIZE);

        // ═══════════════════════════════════════════════════
        // STEP 3: Submit each chunk in parallel
        // ═══════════════════════════════════════════════════
        // AtomicInteger because it's shared between threads → must be thread-safe.
        // If we used a regular int, two threads incrementing concurrently
        // could "lose" an increment (race condition).
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failedCount = new AtomicInteger(0);

        // CompletableFuture.runAsync(task, executor):
        // - "runAsync" = execute asynchronously (do not block the current thread)
        // - "task" = the lambda to execute (our processChunk)
        // - "executor" = where to run it (our Virtual Threads pool)
        //
        // .exceptionally() = if the task fails, catch the exception and log
        // instead of propagating it. This way a failed chunk does not block the others.
        List<CompletableFuture<Void>> futures = chunks.stream()
                .map(chunk -> CompletableFuture.runAsync(
                        () -> processChunk(chunk, successCount), executorService)
                        .exceptionally(ex -> {
                            failedCount.addAndGet(chunk.size());
                            log.error("[PROMOTION_LIFECYCLE] Chunk failed ({} items): {}",
                                    chunk.size(), ex.getMessage(), ex);
                            return null; // required for Void type
                        }))
                .toList();

        // ═══════════════════════════════════════════════════
        // STEP 4: Wait for ALL tasks
        // ═══════════════════════════════════════════════════
        // .allOf(futures) = creates a Future that completes when ALL futures complete
        // .join() = blocks the current thread until all complete
        //
        // "But if it blocks the thread, isn't it like sequential?"
        // NO! The chunks are ALREADY running in parallel. Here we are just
        // waiting for all of them to finish before proceeding to the final log.
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        // ═══════════════════════════════════════════════════
        // STEP 5: Aggregated log
        // ═══════════════════════════════════════════════════
        log.info("[PROMOTION_LIFECYCLE] Completed: expired={} failed={} chunks={} timestamp={}",
                successCount.get(), failedCount.get(), chunks.size(), now);
    }

    /**
     * Processes a single chunk in an independent transaction.
     *
     * <p>
     * This method runs in a separate Virtual Thread.
     * The transaction is created by TransactionTemplate, not by @Transactional.
     * </p>
     *
     * @param chunk   sub-list of coupons to process
     * @param counter atomic counter shared among all threads
     */
    private void processChunk(List<Coupon> chunk, AtomicInteger counter) {
        // executeWithoutResult: run this lambda inside a transaction
        // - If the lambda terminates normally → COMMIT
        // - If the lambda throws an exception → ROLLBACK
        transactionTemplate.executeWithoutResult(status -> {
            chunk.forEach(promo -> {
                promo.setStatus(CouponStatus.EXPIRED);
                log.debug("[PROMOTION_LIFECYCLE] Expiring code={} reason={} thread={}",
                        promo.getCode(),
                        promo.getCurrentUsage() >= promo.getUsageLimit()
                                ? "usage_limit_reached"
                                : "date_expired",
                        Thread.currentThread().getName());
            });

            // saveAll performs a batch UPDATE, much more efficient than N individual save() calls
            couponRepository.saveAll(chunk);
            counter.addAndGet(chunk.size());
        });
    }

    /**
     * Splits a list into sub-lists of maximum size {@code chunkSize}.
     *
     * <p>
     * Example: {@code [1,2,3,4,5]} with {@code chunkSize=2} →
     * {@code [[1,2], [3,4], [5]]}
     * </p>
     *
     * <p>
     * NOTE: the sub-lists are "views" of the original list (subList),
     * not copies. This is intentional: it saves memory.
     * </p>
     */
    private <T> List<List<T>> partitionList(List<T> list, int chunkSize) {
        List<List<T>> partitions = new ArrayList<>();
        for (int i = 0; i < list.size(); i += chunkSize) {
            partitions.add(list.subList(i, Math.min(i + chunkSize, list.size())));
        }
        return partitions;
    }
}
