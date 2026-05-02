package com.example.esercizi.strategy;

import com.example.esercizi.dto.PromotionContext;

import java.math.BigDecimal;

public interface PromotionStrategy {

    /**
     * Enum che definisce la tipologia di strategia.
     * Utile per applicare regole di priorità (es. VIP sempre, STAGIONALE ha
     * priorità su COUPON).
     */
    enum StrategyType {
        VIP,
        SEASONAL
    }

    /**
     * Determina se questa promo è applicabile al contesto corrente.
     */
    boolean supports(PromotionContext context);

    /**
     * Restituisce la percentuale di sconto da applicare (es. 0.10 per 10%).
     */
    BigDecimal getDiscountPercentage();

    /**
     * Restituisce il tipo di strategia.
     */
    StrategyType getType();
}
