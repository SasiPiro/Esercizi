package com.ecommerce.monolith.catalog.internal;

import com.ecommerce.monolith.catalog.ProductDTO;
import com.ecommerce.monolith.catalog.ProductRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Read side of the catalog. Products are read far more often than they change, so lookups
 * by code are cached in-process (Caffeine, see spring.cache in application.yaml).
 *
 * <p>
 * The cache holds immutable {@link ProductDTO} records, never entities: a cached value is
 * shared by every request thread, and a JPA entity there could be modified or lazily loaded
 * outside its session. The cart does not use this cache: adding an item needs the managed
 * {@code Product} entity, which Hibernate loads in that transaction anyway.
 * </p>
 */
@Service
public class ProductCatalog {

    private final ProductRepository productRepository;

    public ProductCatalog(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    /**
     * The product with this code. Unknown codes are not cached, so a product created later
     * is found at once. Each instance has its own cache: an update saved on another instance
     * is visible here only when the entry expires (5 minutes).
     */
    @Cacheable(cacheNames = "products", key = "#code", unless = "#result == null")
    public Optional<ProductDTO> findByCode(String code) {
        return productRepository.findByCode(code)
                .map(p -> new ProductDTO(p.getId(), p.getCode(), p.getBasePrice()));
    }
}
