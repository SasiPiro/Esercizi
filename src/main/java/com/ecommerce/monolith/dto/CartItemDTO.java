package com.ecommerce.monolith.dto;

public record CartItemDTO(
        Long id,
        ProductDTO product,
        Integer quantity) {
}
