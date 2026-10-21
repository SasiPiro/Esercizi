package com.ecommerce.monolith;

import com.ecommerce.monolith.catalog.Product;
import com.ecommerce.monolith.identity.User;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.Hibernate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * An entity and a Hibernate proxy of the same row must be equal, otherwise Set membership
 * and contains() depend on how the object happened to be loaded.
 */
@SpringBootTest
@ActiveProfiles("dev")
class EntityEqualityTests {

    @Autowired EntityManagerFactory emf;

    @Test
    void productEqualsAProxyOfItself() {
        Product loaded = inNewContext(em -> em.find(Product.class, 1L));
        Product other = inNewContext(em -> em.find(Product.class, 2L));

        try (EntityManager em = emf.createEntityManager()) {
            Product proxy = em.getReference(Product.class, 1L);
            assertEqualToProxy(loaded, proxy);
            assertThat(proxy).isNotEqualTo(other);
        }
    }

    @Test
    void userEqualsAProxyOfItself() {
        User loaded = inNewContext(em -> em.find(User.class, 1L));

        try (EntityManager em = emf.createEntityManager()) {
            assertEqualToProxy(loaded, em.getReference(User.class, 1L));
        }
    }

    @Test
    void transientEntitiesAreEqualOnlyToThemselves() {
        Product a = new Product(null, "X", null);
        Product b = new Product(null, "X", null);
        assertThat(a).isEqualTo(a).isNotEqualTo(b);
    }

    private static void assertEqualToProxy(Object loaded, Object proxy) {
        assertThat(Hibernate.isInitialized(proxy)).as("a real, uninitialized proxy").isFalse();
        assertThat(proxy.getClass()).isNotEqualTo(loaded.getClass());

        assertThat(loaded).isEqualTo(proxy);
        assertThat(proxy).isEqualTo(loaded);
        assertThat(proxy.hashCode()).isEqualTo(loaded.hashCode());
        Set<Object> set = new HashSet<>(Set.of(loaded));
        assertThat(set).contains(proxy);
    }

    /**
     * Loads in its own persistence context, so a later getReference (kept open, because
     * equals and hashCode on a proxy initialize it) returns a proxy, not the cached entity.
     */
    private <T> T inNewContext(Function<EntityManager, T> work) {
        try (EntityManager em = emf.createEntityManager()) {
            return work.apply(em);
        }
    }
}
