package com.example.esercizi.model;

import jakarta.persistence.*;

@Entity
@Table(name = "cart_ownership")
public class CartOwnership {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "cart_id", nullable = false)
    private Cart cart;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user; // nullable: guest

    @OneToOne
    private Session session; // per guest anonimi

    public CartOwnership(Long id, Cart cart, User user, Session session) {
        this.id = id;
        this.cart = cart;
        this.user = user;
        this.session = session;
    }

    public CartOwnership() {}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Cart getCart() {
        return cart;
    }

    public void setCart(Cart cart) {
        this.cart = cart;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Session getSession() { return session; }

    public void setSession(Session session) { this.session = session; }
}
