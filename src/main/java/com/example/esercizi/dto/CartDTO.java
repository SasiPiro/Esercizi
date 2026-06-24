package com.example.esercizi.dto;

import java.util.List;

public record CartDTO(
                Long id,
                Long userId,
                String anonymousId,
                List<CartItemDTO> items) {
}
