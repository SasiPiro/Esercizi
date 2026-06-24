package com.example.esercizi.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Configuration of the thread pool for scheduled jobs.
 *
 * <h3>What is an ExecutorService?</h3>
 * <p>
 * It is a "thread manager". Instead of creating threads manually with
 * {@code new Thread(...)}, you delegate the work to an ExecutorService that:
 * </p>
 * <ul>
 *     <li>Creates the threads for you</li>
 *     <li>Reuses them (pool) or creates new ones (virtual)</li>
 *     <li>Closes them when the application shuts down</li>
 * </ul>
 *
 * <h3>Why Virtual Threads (Java 21)?</h3>
 * <p>
 * <strong>Platform Threads</strong> (classic): each thread occupies ~1MB of stack memory
 * and corresponds to an operating system thread. Creating 10,000 = 10GB of RAM.<br/>
 * <strong>Virtual Threads</strong>: they occupy only a few KB, and are managed by the JVM (not the OS).
 * You can create millions of them without issues. Perfect for I/O-bound tasks like DB queries.
 * </p>
 *
 * <h3>Why a @Bean and not Executors.newVirtualThreadPerTaskExecutor() inline?</h3>
 * <ul>
 *     <li><strong>Reusability</strong>: the same executor can be injected into any job</li>
 *     <li><strong>Testability</strong>: in tests you can mock it or replace it with a single-thread executor</li>
 *     <li><strong>Lifecycle</strong>: Spring calls {@code close()} automatically on shutdown
 *         thanks to {@code destroyMethod = "close"}</li>
 * </ul>
 */
@Configuration
public class AsyncConfig {

    @Bean(destroyMethod = "close")
    public ExecutorService taskExecutor() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }
}
