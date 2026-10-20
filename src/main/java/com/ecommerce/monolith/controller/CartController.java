package com.ecommerce.monolith.controller;

import com.ecommerce.monolith.dto.CartDTO;
import com.ecommerce.monolith.security.JwtUtil;
import com.ecommerce.monolith.service.CartService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
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
    public CartDTO addItem(@RequestParam @NotBlank @Size(max = 64) String productCode,
            @RequestParam @Min(1) @Max(CartService.MAX_QUANTITY_PER_LINE) int quantity) {
        return cartService.addItem(productCode, quantity);
    }

    @PostMapping("/remove")
    public CartDTO removeItem(@RequestParam @NotBlank @Size(max = 64) String productCode) {
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
