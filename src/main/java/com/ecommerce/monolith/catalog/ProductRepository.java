package com.ecommerce.monolith.catalog;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {

    /**
     * Cached in-process (Caffeine, see spring.cache in application.yaml): products are read on
     * every add to cart and change rarely. Unknown codes are not cached.
     *
     * <p>
     * Each instance has its own cache: after an update, other instances see the new product
     * only when their entry expires (5 minutes). Acceptable for catalog lookups; prices at
     * checkout never come from here, they are read with the cart from the database.
     * </p>
     */
    @Cacheable(cacheNames = "products", key = "#code", unless = "#result == null")
    Optional<Product> findByCode(String code);

    /** Evicts the cached product on this instance, so a change is visible here at once. */
    @Override
    @CacheEvict(cacheNames = "products", key = "#entity.code")
    <S extends Product> S save(S entity);
}
