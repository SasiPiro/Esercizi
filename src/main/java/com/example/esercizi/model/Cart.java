package com.example.esercizi.model;

import jakarta.persistence.*;
import java.util.List;

@Entity
@Table(name = "cart")
public class Cart {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Bidirectional relationship: a cart has many items
    // mappedBy tells JPA that the mapping is managed by the "cart" field in CartItem
    @OneToMany(mappedBy = "cart", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CartItem> items;

    @Column(name = "cart_status")
    @Enumerated(EnumType.STRING)
    private CartStatus cartStatus;

    public Cart(Long id, List<CartItem> items) {
        this.id = id;
        this.items = items;
        this.cartStatus = CartStatus.TEMPORARY;
    }

    public Cart() {
    }

    public enum CartStatus {
        TEMPORARY, // cart created but not associated with a user
        ACTIVE, // cart in use by the user
        CHECKOUT, // in the payment/checkout phase
        PURCHASED // completed/purchased
    }

    public Long getId() {
        return id;
    }

    public List<CartItem> getItems() {
        return items;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setItems(List<CartItem> items) {
        this.items = items;
    }

    public CartStatus getCartStatus() {
        return cartStatus;
    }

    public void setCartStatus(CartStatus cartStatus) {
        this.cartStatus = cartStatus;
    }
}
