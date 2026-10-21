package com.ecommerce.monolith.cart.internal;

import java.util.List;

public record CartDTO(
                Long id,
                Long userId,
                String anonymousId,
                List<CartItemDTO> items) {
}
