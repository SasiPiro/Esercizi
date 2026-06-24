package com.example.esercizi.scheduler;

import com.example.esercizi.model.Cart;
import com.example.esercizi.model.CartOwnership;
import com.example.esercizi.repository.CartOwnershipRepository;
import com.example.esercizi.repository.CartRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Scheduled job for cleaning up abandoned guest carts.
 *
 * <p>Same pattern as {@link PromotionExpiryJob}:
 * load → chunking → parallel processing → aggregated log.</p>
 *
 * <h3>Key difference compared to PromotionExpiryJob:</h3>
 * <p>
 * Here we perform a <strong>DELETE</strong> (not UPDATE). Each chunk deletes the ownerships
 * and associated carts in batch. Order is important: ownerships first
 * (which have a FK pointing to cart), then the carts. If we reversed the order,
 * the DB would throw a foreign key constraint violation error.
 * </p>
 */
@Component
public class CartCleanupJob {

    private static final Logger logger = LoggerFactory.getLogger(CartCleanupJob.class);
    private static final int CHUNK_SIZE = 500;

    private final CartOwnershipRepository ownershipRepository;
    private final CartRepository cartRepository;
    private final ExecutorService executorService;
    private final TransactionTemplate transactionTemplate;

    public CartCleanupJob(CartOwnershipRepository ownershipRepository, CartRepository cartRepository,
                          ExecutorService executorService, PlatformTransactionManager txManager) {
        this.ownershipRepository = ownershipRepository;
        this.cartRepository = cartRepository;
        this.executorService = executorService;
        this.transactionTemplate = new TransactionTemplate(txManager);
    }

    /**
     * Runs the cleanup every day at 00:01.
     * Deletes guest carts created more than 30 days ago.
     */
    @Scheduled(cron = "0 1 0 * * *")
    public void cleanupExpiredGuestCarts() {
        logger.info("[CART_CLEANUP] Starting cleanup of expired guest carts...");

        LocalDateTime expiryDate = LocalDateTime.now().minusDays(30);
        List<CartOwnership> expiredCarts = ownershipRepository.findByAnonymousIdIsNotNullAndCreatedAtBefore(expiryDate);

        if (expiredCarts.isEmpty()) {
            logger.info("[CART_CLEANUP] No expired guest carts found.");
            return;
        }

        logger.info("[CART_CLEANUP] Found {} expired guest carts. Processing in parallel (chunkSize={})...",
                expiredCarts.size(), CHUNK_SIZE);

        // Partition into chunks and process in parallel
        List<List<CartOwnership>> chunks = partitionList(expiredCarts, CHUNK_SIZE);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failedCount = new AtomicInteger(0);

        List<CompletableFuture<Void>> futures = chunks.stream()
                .map(chunk -> CompletableFuture.runAsync(
                                () -> processChunk(chunk, successCount), executorService)
                        .exceptionally(ex -> {
                            failedCount.addAndGet(chunk.size());
                            logger.error("[CART_CLEANUP] Chunk failed ({} items): {}",
                                    chunk.size(), ex.getMessage(), ex);
                            return null;
                        }))
                .toList();

        // Wait for all chunks to complete
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        logger.info("[CART_CLEANUP] Completed: deleted={} failed={} chunks={}",
                successCount.get(), failedCount.get(), chunks.size());
    }

    /**
     * Processes a single chunk: deletes ownerships and carts in batch.
     *
     * <p>
     * <strong>deleteAllInBatch vs deleteAll</strong>:
     * </p>
     * <ul>
     *     <li>{@code deleteAll()}: loads each entity into the persistence context,
     *     then executes N individual DELETE queries. Respects JPA lifecycle callbacks
     *     (@PreRemove, cascade). Slow with many records.</li>
     *     <li>{@code deleteAllInBatch()}: executes a SINGLE
     *     {@code DELETE FROM table WHERE id IN (...)}. Does not load the entities.
     *     Much faster, but does NOT trigger JPA cascade (requires database CASCADE).</li>
     * </ul>
     */
    private void processChunk(List<CartOwnership> chunk, AtomicInteger counter) {
        transactionTemplate.executeWithoutResult(status -> {
            List<Cart> cartsToDelete = chunk.stream()
                    .map(CartOwnership::getCart)
                    .toList();

            // Order: ownership first (FK) → then cart
            ownershipRepository.deleteAllInBatch(chunk);
            cartRepository.deleteAllInBatch(cartsToDelete);
            counter.addAndGet(chunk.size());

            logger.debug("[CART_CLEANUP] Deleted {} carts in thread={}",
                    chunk.size(), Thread.currentThread().getName());
        });
    }

    /**
     * Splits a list into sub-lists of maximum size {@code chunkSize}.
     */
    private <T> List<List<T>> partitionList(List<T> list, int chunkSize) {
        List<List<T>> partitions = new ArrayList<>();
        for (int i = 0; i < list.size(); i += chunkSize) {
            partitions.add(list.subList(i, Math.min(i + chunkSize, list.size())));
        }
        return partitions;
    }
}
