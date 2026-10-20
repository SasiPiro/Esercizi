package com.example.esercizi.controller;

import com.example.esercizi.dto.CartDTO;
import com.example.esercizi.security.JwtUtil;
import com.example.esercizi.service.CartService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("api/v1/cart")
public class CartController {

    private final CartService cartService;
    private final JwtUtil jwtUtil;

    public CartController(CartService cartService, JwtUtil jwtUtil) {
        this.cartService = cartService;
        this.jwtUtil = jwtUtil;
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

    /**
     * Merges the caller's guest cart into their user cart after login.
     *
     * <p>
     * The guest proves ownership of the guest cart by sending its signed guest token:
     * the anonymousId is read from the token, never from a request parameter
     * (previously any logged-in user could take over any guest cart by its id).
     * </p>
     */
    @PostMapping("/merge")
    public CartDTO mergeCart(@RequestHeader("X-Guest-Token") String guestToken) {
        return cartService.mergeGuestCartIntoUserCart(jwtUtil.extractVerifiedAnonymousId(guestToken));
    }
}
