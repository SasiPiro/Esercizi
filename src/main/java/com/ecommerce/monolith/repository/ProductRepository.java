package com.ecommerce.monolith.repository;

import com.ecommerce.monolith.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {
    Optional<Product> findByCode(String productCode);
}
