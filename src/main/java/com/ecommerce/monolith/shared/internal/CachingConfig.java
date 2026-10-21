package com.ecommerce.monolith.shared.internal;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Configuration;

/**
 * Local, in-process caches (Caffeine, configured under spring.cache). One process needs no
 * network hop and no extra infrastructure; the microservices repository uses Redis because
 * several replicas there must share one cache.
 */
@Configuration
@EnableCaching
public class CachingConfig {
}
