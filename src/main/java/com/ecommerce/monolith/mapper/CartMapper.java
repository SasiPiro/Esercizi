package com.ecommerce.monolith.mapper;

import com.ecommerce.monolith.dto.CartDTO;
import com.ecommerce.monolith.dto.CartItemDTO;
import com.ecommerce.monolith.dto.ProductDTO;
import com.ecommerce.monolith.model.Cart;
import com.ecommerce.monolith.model.CartItem;
import com.ecommerce.monolith.model.Product;
import com.ecommerce.monolith.model.User;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class CartMapper {

    public CartDTO toDto(Cart cart, User user, String anonymousId) {
        if (cart == null) {
            return null;
        }

        List<CartItemDTO> itemDtos = cart.getItems().stream()
                .map(this::toDto)
                .collect(Collectors.toList());

        return new CartDTO(
                cart.getId(),
                user != null ? user.getId() : null,
                anonymousId,
                itemDtos);
    }

    public CartItemDTO toDto(CartItem item) {
        if (item == null) {
            return null;
        }

        return new CartItemDTO(
                item.getId(),
                toDto(item.getProduct()),
                item.getQuantity());
    }

    public ProductDTO toDto(Product product) {
        if (product == null) {
            return null;
        }

        return new ProductDTO(
                product.getId(),
                product.getCode(),
                product.getBasePrice());
    }

    // NOTE: no DTO -> entity mapping for carts on purpose. Carts are always loaded from the
    // database, so prices can never come from a client-supplied payload.
}
