package com.ecommerce.monolith.model;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * A confirmed purchase.
 *
 * <p>
 * Prices and product codes are a snapshot taken at checkout: they never change when the
 * catalog does, which is what invoices, refunds and disputes need. The owner is kept as a
 * plain id (user) or anonymous id (guest), exactly one of the two.
 * </p>
 */
@Entity
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Unique: a retried checkout with the same key can never create a second order. */
    @Column(nullable = false, unique = true, length = 64)
    private String idempotencyKey;

    private Long userId;          // null for guests

    private String anonymousId;   // null for users

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal initialPrice;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal finalPrice;

    private String couponCode;

    /** Comma-separated, in the order they were applied, e.g. "VipExclusiveDiscount,COUPON:SAVE10". */
    @Column(nullable = false)
    private String appliedStrategies;

    @Column(nullable = false)
    private Instant placedAt;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id")
    private List<OrderLine> lines = new ArrayList<>();

    public Order() {
    }

    public Order(String idempotencyKey, Long userId, String anonymousId, Instant placedAt) {
        this.idempotencyKey = idempotencyKey;
        this.userId = userId;
        this.anonymousId = anonymousId;
        this.placedAt = placedAt;
    }

    /** Copies code and current price of the product: later catalog changes do not affect it. */
    public void addLine(Product product, int quantity) {
        lines.add(new OrderLine(this, product.getCode(), product.getBasePrice(), quantity));
    }

    public Long getId() {
        return id;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public Long getUserId() {
        return userId;
    }

    public String getAnonymousId() {
        return anonymousId;
    }

    public BigDecimal getInitialPrice() {
        return initialPrice;
    }

    public void setInitialPrice(BigDecimal initialPrice) {
        this.initialPrice = initialPrice;
    }

    public BigDecimal getFinalPrice() {
        return finalPrice;
    }

    public void setFinalPrice(BigDecimal finalPrice) {
        this.finalPrice = finalPrice;
    }

    public String getCouponCode() {
        return couponCode;
    }

    public void setCouponCode(String couponCode) {
        this.couponCode = couponCode;
    }

    public String getAppliedStrategies() {
        return appliedStrategies;
    }

    public void setAppliedStrategies(String appliedStrategies) {
        this.appliedStrategies = appliedStrategies;
    }

    public Instant getPlacedAt() {
        return placedAt;
    }

    public List<OrderLine> getLines() {
        return lines;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Order other)) return false;
        return id != null && id.equals(other.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
