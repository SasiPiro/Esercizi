package com.ecommerce.monolith;

import com.ecommerce.monolith.catalog.Product;
import com.ecommerce.monolith.catalog.ProductDTO;
import com.ecommerce.monolith.catalog.ProductRepository;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Product pages are served from the local cache after the first request, the cache holds
 * immutable DTOs, and a product saved on this instance is never served stale from it.
 * No token is sent: the catalog is public.
 */
@SpringBootTest(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("dev")
@AutoConfigureMockMvc
class CatalogCacheTests {

    @Autowired MockMvc mvc;
    @Autowired ProductRepository products;
    @Autowired CacheManager cacheManager;
    @Autowired EntityManagerFactory emf;

    @Test
    void theSecondRequestForTheSameProductRunsNoSql() throws Exception {
        String code = newProduct("4.00");

        assertThat(statementsFor(code, status().isOk())).isEqualTo(1);
        assertThat(statementsFor(code, status().isOk())).isZero();
        mvc.perform(get("/api/v1/products/" + code))
                .andExpect(jsonPath("$.code").value(code))
                .andExpect(jsonPath("$.basePrice").value(4.00));
    }

    @Test
    void theCacheHoldsDtosNotEntities() throws Exception {
        String code = newProduct("4.00");
        mvc.perform(get("/api/v1/products/" + code)).andExpect(status().isOk());

        assertThat(cacheManager.getCache("products").get(code).get()).isInstanceOf(ProductDTO.class);
    }

    @Test
    void savingAProductEvictsItFromTheCache() throws Exception {
        String code = newProduct("4.00");
        mvc.perform(get("/api/v1/products/" + code)).andExpect(status().isOk());

        Product product = products.findByCode(code).orElseThrow();
        product.setBasePrice(new BigDecimal("5.00"));
        products.save(product);

        assertThat(statementsFor(code, status().isOk())).isEqualTo(1);
        mvc.perform(get("/api/v1/products/" + code)).andExpect(jsonPath("$.basePrice").value(5.00));
    }

    @Test
    void unknownCodesAreNotCached() throws Exception {
        String unknown = "NONE" + UUID.randomUUID().toString().substring(0, 8);

        assertThat(statementsFor(unknown, status().isNotFound())).isEqualTo(1);
        assertThat(statementsFor(unknown, status().isNotFound())).isEqualTo(1);
    }

    private long statementsFor(String code, ResultMatcher expected) throws Exception {
        Statistics stats = emf.unwrap(SessionFactory.class).getStatistics();
        stats.clear();
        mvc.perform(get("/api/v1/products/" + code)).andExpect(expected);
        return stats.getPrepareStatementCount();
    }

    private String newProduct(String price) {
        String code = "C" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        products.save(new Product(null, code, new BigDecimal(price)));
        return code;
    }
}
