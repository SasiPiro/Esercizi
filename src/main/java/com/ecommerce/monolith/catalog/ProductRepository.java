package com.ecommerce.monolith.catalog;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {

    /** Not cached: callers need the managed entity. Cached reads go through ProductCatalog. */
    Optional<Product> findByCode(String code);

    /**
     * Evicts the product from the catalog cache on this instance, so a change is visible
     * here at once.
     */
    @Override
    @CacheEvict(cacheNames = "products", key = "#entity.code")
    <S extends Product> S save(S entity);
}
