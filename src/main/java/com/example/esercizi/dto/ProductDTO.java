package com.example.esercizi.dto;

import java.math.BigDecimal;

public record ProductDTO(
                Long id,
                String code,
                BigDecimal basePrice) {
}
