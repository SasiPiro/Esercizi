package com.ecommerce.monolith.shared.internal;

import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.provider.jdbctemplate.JdbcTemplateLockProvider;
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;

import javax.sql.DataSource;

/**
 * Scheduling with a distributed lock.
 *
 * <p>
 * With several replicas (e.g. a Kubernetes Deployment with 3 pods) every pod runs the
 * {@code @Scheduled} methods. ShedLock stores one row per job in the {@code shedlock}
 * table: the first pod that acquires it runs the job, the others skip that execution.
 * The lock uses the database clock, so clock drift between pods does not matter.
 * </p>
 *
 * <p>
 * Set {@code app.scheduling.enabled=false} to stop the automatic triggers (e.g. in tests);
 * the lock stays active, so the jobs can still be invoked and keep their locking behavior.
 * </p>
 */
@Configuration
@EnableSchedulerLock(defaultLockAtMostFor = "PT10M")
public class SchedulingConfig {

    @Configuration
    @EnableScheduling
    @ConditionalOnProperty(name = "app.scheduling.enabled", havingValue = "true", matchIfMissing = true)
    static class Triggers {
    }

    @Bean
    public LockProvider lockProvider(DataSource dataSource) {
        return new JdbcTemplateLockProvider(
                JdbcTemplateLockProvider.Configuration.builder()
                        .withJdbcTemplate(new JdbcTemplate(dataSource))
                        .usingDbTime()
                        .build());
    }
}
