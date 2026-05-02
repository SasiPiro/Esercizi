package com.example.esercizi.dto;

import java.math.BigDecimal;

public record ProductDto(
                Long id,
                String code,
                BigDecimal basePrice) {
}
