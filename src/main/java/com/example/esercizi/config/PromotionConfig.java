package com.example.esercizi.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@ConfigurationProperties(prefix = "promotions")
    public class PromotionConfig {
        private BigDecimal vipDiscount;
        private BigDecimal blackFridayDiscount;
        private BigDecimal christmasDiscount;

    // Getters e Setters
    public BigDecimal getVipDiscount() {
        return vipDiscount;
    }

    public void setVipDiscount(BigDecimal vipDiscount) {
        this.vipDiscount = vipDiscount;
    }

    public BigDecimal getBlackFridayDiscount() {
        return blackFridayDiscount;
    }

    public void setBlackFridayDiscount(BigDecimal blackFridayDiscount) {
        this.blackFridayDiscount = blackFridayDiscount;
    }

    public BigDecimal getChristmasDiscount() {
        return christmasDiscount;
    }

    public void setChristmasDiscount(BigDecimal christmasDiscount) {
        this.christmasDiscount = christmasDiscount;
    }
}
