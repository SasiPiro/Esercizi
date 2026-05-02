package com.example.esercizi.dto;

public record CartItemDto(
        Long id,
        ProductDto product,
        Integer quantity) {
}
