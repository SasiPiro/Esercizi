package com.ecommerce.monolith;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * PostgreSQL in Docker for the tests: correctness here relies on database behavior
 * (conditional updates, row locks, unique constraints), so it is proven on the real database.
 * {@code @ServiceConnection} sets URL and credentials.
 *
 * <p>
 * One database per Spring context, started and stopped with it, after every bean that uses
 * it. Test classes that share a context share its database: tests create their own users,
 * carts and coupons with random names instead of relying on cleanup.
 * </p>
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

    @Bean
    @ServiceConnection
    PostgreSQLContainer postgres() {
        return new PostgreSQLContainer("postgres:16-alpine");
    }
}
