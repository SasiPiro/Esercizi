package com.example.esercizi.dto;

public record CartItemDTO(
        Long id,
        ProductDTO product,
        Integer quantity) {
}
