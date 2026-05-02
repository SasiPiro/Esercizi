package com.example.esercizi.dto;

import com.example.esercizi.model.Cart;
import com.example.esercizi.model.User;
import java.time.Instant;

public record PromotionContext(Cart cart, Instant now, User user) {
}
// user da security context