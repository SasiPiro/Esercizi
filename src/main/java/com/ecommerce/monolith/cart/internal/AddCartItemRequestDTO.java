package com.ecommerce.monolith.cart.internal;

import com.ecommerce.monolith.cart.CartService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AddCartItemRequestDTO(
        @NotBlank @Size(max = 64) String productCode,
        @NotNull @Min(1) @Max(CartService.MAX_QUANTITY_PER_LINE) Integer quantity) {
}
