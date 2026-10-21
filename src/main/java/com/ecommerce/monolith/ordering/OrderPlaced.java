package com.ecommerce.monolith.ordering;

import java.math.BigDecimal;

/**
 * An order was placed. Published inside the checkout transaction and stored in the event
 * publication registry with it; listeners run only after that transaction commits.
 *
 * @param orderId the new order
 * @param userId  the buyer, {@code null} for a guest
 * @param total   the final price paid
 */
public record OrderPlaced(Long orderId, Long userId, BigDecimal total) {
}
