package com.example.esercizi.mapper;

import com.example.esercizi.dto.CartDTO;
import com.example.esercizi.dto.CartItemDTO;
import com.example.esercizi.dto.ProductDTO;
import com.example.esercizi.model.Cart;
import com.example.esercizi.model.CartItem;
import com.example.esercizi.model.Product;
import com.example.esercizi.model.User;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
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

    public Product toEntity (ProductDTO productDTO){
        if (productDTO == null) {
            return null;
        }

        return new Product(
                productDTO.id(),
                productDTO.code(),
                productDTO.basePrice()
        );
    }

    public Cart toEntity(CartDTO dto) {
        if (dto == null) {
            return null;
        }

        // 1. Map the list of CartItemDTOs to CartItems
        List<CartItem> entityItems = new ArrayList<>();
        if (dto.items() != null) {
            entityItems = dto.items().stream()
                    .map(itemDto -> {
                        CartItem item = new CartItem();
                        item.setId(itemDto.id());
                        item.setQuantity(itemDto.quantity());
                        item.setProduct(toEntity(itemDto.product()));
                        return item;
                    })
                    .toList();
        }
        Cart cart = new Cart(dto.id(), entityItems);

        // Associate each item with this cart
        if (cart.getItems() != null) {
            cart.getItems().forEach(item -> item.setCart(cart));
        }
        return cart;
    }
}
