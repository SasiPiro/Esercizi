package com.example.esercizi.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Rappresenta un codice sconto (coupon) riscattabile dall'utente.
 *
 * <p>
 * Questa entità utilizza <strong>Optimistic Locking</strong> tramite il campo
 * {@code version}
 * annotato con {@link Version}. Quando due transazioni concorrenti tentano di
 * modificare
 * lo stesso record (es. incrementare {@code currentUsage}), JPA confronta il
 * valore di
 * {@code version} letto con quello attuale nel DB: se differiscono, la seconda
 * transazione
 * riceve un {@code OptimisticLockException}.
 * </p>
 *
 * <p>
 * Differenza rispetto alle promozioni automatiche ({@code PromotionStrategy}):
 * le strategy si attivano in base al contesto (utente VIP, data corrente),
 * mentre
 * un {@code Coupon} richiede un codice esplicito dall'utente e ha limiti di
 * utilizzo.
 * </p>
 */
@Entity
@Table(name = "coupon")
public class Coupon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    @NotEmpty(message = "Coupon code is required")
    private String code;

    @Column(length = 255)
    private String description;

    @Column(name = "discount_percentage", nullable = false, precision = 5, scale = 4)
    @NotNull(message = "Discount percentage is required")
    @DecimalMin(value = "0.0001", message = "Discount must be greater than 0")
    @DecimalMax(value = "1.0000", message = "Discount must be at most 100%")
    private BigDecimal discountPercentage;

    @Column(name = "usage_limit", nullable = false)
    @NotNull(message = "Usage limit is required")
    private Integer usageLimit;

    @Column(name = "current_usage", nullable = false)
    private Integer currentUsage = 0;

    @Column(name = "expiry_date", nullable = false)
    @NotNull(message = "Expiry date is required")
    private Instant expiryDate;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private CouponStatus status;

    /**
     * Campo di versione per Optimistic Locking.
     * JPA incrementa automaticamente questo valore ad ogni UPDATE.
     * Se due transazioni leggono la stessa versione e una aggiorna per prima,
     * la seconda fallirà con {@code OptimisticLockException}.
     */
    @Version
    private Long version;

    // --- Enum ---

    public enum CouponStatus {
        ACTIVE,
        EXPIRED
    }

    // --- Constructors ---

    public Coupon() {
    }

    public Coupon(String code, String description, BigDecimal discountPercentage,
                  Integer usageLimit, Instant expiryDate) {
        this.code = code;
        this.description = description;
        this.discountPercentage = discountPercentage;
        this.usageLimit = usageLimit;
        this.currentUsage = 0;
        this.expiryDate = expiryDate;
        this.status = CouponStatus.ACTIVE;
    }

    // --- Getters & Setters ---

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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getDiscountPercentage() {
        return discountPercentage;
    }

    public void setDiscountPercentage(BigDecimal discountPercentage) {
        this.discountPercentage = discountPercentage;
    }

    public Integer getUsageLimit() {
        return usageLimit;
    }

    public void setUsageLimit(Integer usageLimit) {
        this.usageLimit = usageLimit;
    }

    public Integer getCurrentUsage() {
        return currentUsage;
    }

    public void setCurrentUsage(Integer currentUsage) {
        this.currentUsage = currentUsage;
    }

    public Instant getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(Instant expiryDate) {
        this.expiryDate = expiryDate;
    }

    public CouponStatus getStatus() {
        return status;
    }

    public void setStatus(CouponStatus status) {
        this.status = status;
    }

    public Long getVersion() {
        return version;
    }

    @Override
    public String toString() {
        return "Promotion{" +
                "id=" + id +
                ", code='" + code + '\'' +
                ", status=" + status +
                ", usage=" + currentUsage + "/" + usageLimit +
                ", version=" + version +
                '}';
    }
}
