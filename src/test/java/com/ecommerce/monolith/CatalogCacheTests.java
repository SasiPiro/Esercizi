package com.ecommerce.monolith;

import com.ecommerce.monolith.catalog.Product;
import com.ecommerce.monolith.catalog.ProductRepository;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Product lookups by code are served from the local cache after the first one, and a
 * product saved on this instance is never served stale from it.
 */
@SpringBootTest(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("dev")
class CatalogCacheTests {

    @Autowired ProductRepository products;
    @Autowired EntityManagerFactory emf;

    @Test
    void theSecondLookupOfTheSameCodeRunsNoSql() {
        String code = newProduct("4.00");

        assertThat(statementsFor(() -> products.findByCode(code))).isEqualTo(1);
        assertThat(statementsFor(() -> products.findByCode(code))).isZero();
        assertThat(products.findByCode(code)).get().extracting(Product::getCode).isEqualTo(code);
    }

    @Test
    void savingAProductEvictsItFromTheCache() {
        String code = newProduct("4.00");
        Product product = products.findByCode(code).orElseThrow();

        product.setBasePrice(new BigDecimal("5.00"));
        products.save(product);

        assertThat(statementsFor(() -> products.findByCode(code))).isEqualTo(1);
        assertThat(products.findByCode(code).orElseThrow().getBasePrice()).isEqualByComparingTo("5.00");
    }

    @Test
    void unknownCodesAreNotCached() {
        String unknown = "NONE" + UUID.randomUUID().toString().substring(0, 8);

        assertThat(statementsFor(() -> products.findByCode(unknown))).isEqualTo(1);
        assertThat(statementsFor(() -> products.findByCode(unknown))).isEqualTo(1);
    }

    private long statementsFor(Runnable lookup) {
        Statistics stats = emf.unwrap(SessionFactory.class).getStatistics();
        stats.clear();
        lookup.run();
        return stats.getPrepareStatementCount();
    }

    private String newProduct(String price) {
        String code = "C" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        products.save(new Product(null, code, new BigDecimal(price)));
        return code;
    }
}
