package com.ecommerce.monolith.service;

import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;

@Component
public class TimeService {

    private final Clock clock;

    public TimeService() {
        this.clock = Clock.systemUTC();
    }

    public Instant now() {
        return Instant.now(clock);
    }
}
