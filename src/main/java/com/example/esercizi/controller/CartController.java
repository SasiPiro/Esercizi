package com.example.esercizi.controller;

import com.example.esercizi.dto.CartDTO;
import com.example.esercizi.model.Cart;
import com.example.esercizi.model.User;
import com.example.esercizi.repository.UserRepository;
import com.example.esercizi.service.CartService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Objects;

@RestController
@RequestMapping("api/v1/cart")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping
    public CartDTO getCart() {
        return cartService.getOrCreateCart();
    }

    @PostMapping("/add")
    public CartDTO addItem(@RequestParam String productCode,
            @RequestParam int quantity) {
        return cartService.addItem(productCode, quantity);
    }

    @PostMapping("/remove")
    public CartDTO removeItem(@RequestParam String productCode) {
        return cartService.removeItem(productCode);
    }

    @PostMapping("/merge")
    public CartDTO mergeCart(@RequestParam String anonymousId) {
        return cartService.mergeGuestCartIntoUserCart(anonymousId);
    }
}
