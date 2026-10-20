package com.example.esercizi.repository;

import com.example.esercizi.model.Cart;
import com.example.esercizi.model.CartOwnership;
import com.example.esercizi.model.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

public interface CartOwnershipRepository extends JpaRepository<CartOwnership, Long> {

    /**
     * Loads a user's cart with its items and their products in ONE query.
     * Without the fetch joins, rendering a cart costs 1 query per product (N+1).
     */
    @Query("""
            SELECT c FROM CartOwnership o JOIN o.cart c
              LEFT JOIN FETCH c.items i
              LEFT JOIN FETCH i.product
             WHERE o.user.id = :userId
            """)
    Optional<Cart> findCartWithItemsByUserId(@Param("userId") Long userId);

    /** Same as {@link #findCartWithItemsByUserId(Long)} for a guest cart. */
    @Query("""
            SELECT c FROM CartOwnership o JOIN o.cart c
              LEFT JOIN FETCH c.items i
              LEFT JOIN FETCH i.product
             WHERE o.anonymousId = :anonymousId
            """)
    Optional<Cart> findCartWithItemsByAnonymousId(@Param("anonymousId") String anonymousId);

    @EntityGraph(attributePaths = {"cart", "cart.items", "cart.items.product"})
    Optional<CartOwnership> findByAnonymousId(String anonymousId);

    @EntityGraph(attributePaths = {"cart", "cart.items", "cart.items.product"})
    Optional<CartOwnership> findByUser(User user);

    /**
     * Deletes, in a single statement, the guest carts created before {@code cutoff}.
     * ON DELETE CASCADE removes their ownership rows and items. Used by {@code CartCleanupJob}.
     *
     * @return the number of carts deleted
     */
    @Modifying
    @Transactional
    @Query(value = """
            DELETE FROM cart WHERE id IN (
                SELECT cart_id FROM cart_ownership
                 WHERE anonymous_id IS NOT NULL AND created_at < :cutoff)
            """, nativeQuery = true)
    int deleteGuestCartsCreatedBefore(@Param("cutoff") LocalDateTime cutoff);

    Optional<CartOwnership> findByCartId(Long cartId);
}
