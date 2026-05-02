package com.example.esercizi.service;

import com.example.esercizi.model.Cart;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;

@Component
public class CartCalculator {
    /**
     * Calcola il totale del carrello senza applicare sconti.
     */
    public BigDecimal calculateTotal(Cart cart) {
        return cart.getItems().stream()
                .map(item -> item.getProduct()
                        .getBasePrice()
                        .multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
