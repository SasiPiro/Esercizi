package com.example.esercizi.mapper;

import com.example.esercizi.dto.CartDTO;
import com.example.esercizi.dto.CartItemDTO;
import com.example.esercizi.dto.ProductDTO;
import com.example.esercizi.model.Cart;
import com.example.esercizi.model.CartItem;
import com.example.esercizi.model.Product;
import com.example.esercizi.model.User;
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
