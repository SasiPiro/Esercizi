package com.ecommerce.monolith.aspect;

import org.slf4j.event.Level;

import java.lang.annotation.*;

/**
 * Logs one {@code [PRICING]} line per call of a method returning a pricing result
 * ({@link com.ecommerce.monolith.dto.PricedTotal}: a quote or a placed order):
 * outcome, prices, applied promotions and duration.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface TrackPromotion {

    /**
     * Log level of the line. Frequent read-only calls (e.g. a cart preview shown on every
     * page view) should use DEBUG; business events (a confirmed purchase) INFO.
     */
    Level level() default Level.INFO;
}
