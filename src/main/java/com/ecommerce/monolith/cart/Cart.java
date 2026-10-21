package com.ecommerce.monolith.cart;

import com.ecommerce.monolith.catalog.Product;
import jakarta.persistence.*;
import java.util.ArrayList;
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
    private List<CartItem> items = new ArrayList<>();

    @Column(name = "cart_status")
    @Enumerated(EnumType.STRING)
    private CartStatus cartStatus = CartStatus.TEMPORARY;

    public Cart(Long id, List<CartItem> items) {
        this.id = id;
        this.items = items != null ? new ArrayList<>(items) : new ArrayList<>();
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

    /**
     * Adds a product to the cart, or increments the quantity if the product is already present.
     */
    public void addOrIncrement(Product product, int quantity) {
        items.stream()
                .filter(i -> i.getProduct().equals(product))
                .findFirst()
                .ifPresentOrElse(
                        existing -> existing.setQuantity(existing.getQuantity() + quantity),
                        () -> {
                            CartItem item = new CartItem();
                            item.setCart(this);
                            item.setProduct(product);
                            item.setQuantity(quantity);
                            items.add(item);
                        });
    }
}
