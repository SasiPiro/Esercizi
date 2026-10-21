package com.ecommerce.monolith.cart.internal;

import com.ecommerce.monolith.cart.CartService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** The new quantity of a cart line (replaces it, does not add to it). */
public record UpdateCartItemRequestDTO(
        @NotNull @Min(1) @Max(CartService.MAX_QUANTITY_PER_LINE) Integer quantity) {
}
