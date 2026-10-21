package com.ecommerce.monolith.catalog;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

@Entity
@Table(name = "product")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    @NotEmpty(message = "Product code is required")
    private String code;
    @Column(nullable = false , name = "base_price")
    @NotNull(message = "Product price is required")
    @DecimalMin("0.1")
    private BigDecimal basePrice;

    public Product(){}

    public Product(Long id, String code, BigDecimal basePrice) {
        this.id = id;
        this.code = code;
        this.basePrice = basePrice;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public BigDecimal getBasePrice() {
        return basePrice;
    }

    public void setBasePrice(BigDecimal basePrice) {
        this.basePrice = basePrice;
    }

    /**
     * Equal when both have the same id. instanceof and getId() instead of getClass() and
     * the field, so an entity and a Hibernate proxy of it are equal. Constant hashCode:
     * the id is assigned on insert, and a hash must not change while in a Set.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Product other)) return false;
        return id != null && id.equals(other.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
