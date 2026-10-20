package com.ecommerce.monolith.repository;

import com.ecommerce.monolith.model.Cart;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CartRepository extends JpaRepository<Cart, Long> {

    /**
     * Atomically moves an open cart to CHECKOUT.
     * Returns 0 if the cart is already in checkout or purchased, so two concurrent
     * checkouts of the same cart cannot both succeed.
     */
    @Modifying(flushAutomatically = true)
    @Query("""
            UPDATE Cart c SET c.cartStatus = com.ecommerce.monolith.model.Cart$CartStatus.CHECKOUT
             WHERE c.id = :id
               AND c.cartStatus IN (com.ecommerce.monolith.model.Cart$CartStatus.ACTIVE,
                                    com.ecommerce.monolith.model.Cart$CartStatus.TEMPORARY)
            """)
    int claimForCheckout(@Param("id") Long id);
}
