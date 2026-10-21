package com.ecommerce.monolith.cart.internal;

import com.ecommerce.monolith.cart.CartService;
import com.ecommerce.monolith.identity.internal.JwtUtil;
import jakarta.validation.Valid;
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

    /** Adds units of a product, or more units to its existing line. Returns the whole cart. */
    @PostMapping("/items")
    public CartDTO addItem(@Valid @RequestBody AddCartItemRequestDTO request) {
        return cartService.addItem(request.productCode(), request.quantity());
    }

    /** Sets the quantity of a line already in the cart. */
    @PatchMapping("/items/{productCode}")
    public CartDTO updateItem(@PathVariable @NotBlank @Size(max = 64) String productCode,
            @Valid @RequestBody UpdateCartItemRequestDTO request) {
        return cartService.updateItemQuantity(productCode, request.quantity());
    }

    @DeleteMapping("/items/{productCode}")
    public CartDTO removeItem(@PathVariable @NotBlank @Size(max = 64) String productCode) {
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
