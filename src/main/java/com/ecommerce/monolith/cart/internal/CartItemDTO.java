package com.ecommerce.monolith.cart.internal;

import com.ecommerce.monolith.catalog.ProductDTO;

public record CartItemDTO(
        Long id,
        ProductDTO product,
        Integer quantity) {
}
