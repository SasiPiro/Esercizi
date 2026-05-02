package com.example.esercizi.model;

import jakarta.persistence.*;
import java.util.List;

@Entity
@Table(name = "cart")
public class Cart {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Relazione bidirezionale: un carrello ha molti articoli
    // mappedBy dice a JPA che il legame è gestito dal campo "cart" in
    // CartItemEntity
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
        TEMPORARY, // carrello creato ma non associato a user
        ACTIVE, // carrello in uso dall'utente
        CHECKOUT, // in fase di pagamento
        PURCHASED // completato
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
