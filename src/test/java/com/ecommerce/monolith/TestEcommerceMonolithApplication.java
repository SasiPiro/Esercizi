package com.ecommerce.monolith;

import org.springframework.boot.SpringApplication;

/**
 * Runs the application locally against PostgreSQL in Docker, no database to install:
 * {@code ./mvnw spring-boot:test-run -Dspring-boot.run.profiles=dev}
 */
public class TestEcommerceMonolithApplication {

    public static void main(String[] args) {
        SpringApplication.from(EcommerceMonolithApplication::main)
                .with(TestcontainersConfiguration.class)
                .run(args);
    }
}
