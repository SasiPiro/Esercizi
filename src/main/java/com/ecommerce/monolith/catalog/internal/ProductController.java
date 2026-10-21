package com.ecommerce.monolith.catalog.internal;

import com.ecommerce.monolith.catalog.ProductDTO;
import com.ecommerce.monolith.catalog.ProductNotFoundException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/v1/products")
public class ProductController {

    private final ProductCatalog productCatalog;

    public ProductController(ProductCatalog productCatalog) {
        this.productCatalog = productCatalog;
    }

    /** One product with its base price; public, no token needed. 404 for an unknown code. */
    @GetMapping("/{code}")
    public ProductDTO get(@PathVariable String code) {
        return productCatalog.findByCode(code)
                .orElseThrow(() -> ProductNotFoundException.forCode(code));
    }
}
