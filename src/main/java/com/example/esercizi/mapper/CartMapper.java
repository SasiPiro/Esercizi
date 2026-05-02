package com.example.esercizi.mapper;

import com.example.esercizi.dto.CartDto;
import com.example.esercizi.dto.CartItemDto;
import com.example.esercizi.dto.ProductDto;
import com.example.esercizi.model.Cart;
import com.example.esercizi.model.CartItem;
import com.example.esercizi.model.Product;
import com.example.esercizi.model.User;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class CartMapper {

    public CartDto toDto(Cart cart, User user) {
        if (cart == null) {
            return null;
        }

        List<CartItemDto> itemDtos = cart.getItems().stream()
                .map(this::toDto)
                .collect(Collectors.toList());

        return new CartDto(
                cart.getId(),
                user != null ? user.getId() : null,
                itemDtos);
    }

    public CartItemDto toDto(CartItem item) {
        if (item == null) {
            return null;
        }

        return new CartItemDto(
                item.getId(),
                toDto(item.getProduct()),
                item.getQuantity());
    }

    public ProductDto toDto(Product product) {
        if (product == null) {
            return null;
        }

        return new ProductDto(
                product.getId(),
                product.getCode(),
                product.getBasePrice());
    }
}
