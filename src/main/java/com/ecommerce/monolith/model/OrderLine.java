package com.ecommerce.monolith.model;

import jakarta.persistence.*;

import java.math.BigDecimal;

/** One line of an {@link Order}: product code and unit price as they were at checkout. */
@Entity
@Table(name = "order_line")
public class OrderLine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Order order;

    @Column(nullable = false)
    private String productCode;       // snapshot

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal unitPrice;     // snapshot

    @Column(nullable = false)
    private Integer quantity;

    protected OrderLine() {
    }

    OrderLine(Order order, String productCode, BigDecimal unitPrice, Integer quantity) {
        this.order = order;
        this.productCode = productCode;
        this.unitPrice = unitPrice;
        this.quantity = quantity;
    }

    public Long getId() {
        return id;
    }

    public Order getOrder() {
        return order;
    }

    public String getProductCode() {
        return productCode;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public Integer getQuantity() {
        return quantity;
    }
}
