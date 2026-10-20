package com.example.esercizi.aspect;

import com.example.esercizi.dto.CalculationResultDTO;
import com.example.esercizi.facade.AuthFacade;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * One {@code [PRICING]} line per tracked call, in the same key=value format as the rest
 * of the application.
 *
 * <p>
 * A failed call is logged here only as an outcome ({@code outcome=rejected}), at the
 * annotation's level and without stack trace: the GlobalExceptionHandler is the single
 * place that logs errors, so the same failure is not reported twice.
 * </p>
 */
@Aspect
@Component
public class PromotionTrackingAspect {

    private static final Logger log = LoggerFactory.getLogger(PromotionTrackingAspect.class);
    private final AuthFacade authFacade;

    public PromotionTrackingAspect(AuthFacade authFacade) {
        this.authFacade = authFacade;
    }

    @Around("@annotation(track)")
    public Object trackPromotionExecution(ProceedingJoinPoint joinPoint, TrackPromotion track) throws Throwable {
        String operation = joinPoint.getSignature().getName();
        String principal = currentPrincipal();
        long start = System.nanoTime();

        try {
            Object result = joinPoint.proceed();
            if (result instanceof CalculationResultDTO pricing) {
                log.atLevel(track.level()).log(
                        "[PRICING] operation={} outcome=success principal={} initial={} final={} saved={} "
                                + "discountPct={} strategies={} coupon={} durationMs={}",
                        operation, principal,
                        pricing.initialPrice(), pricing.finalPrice(), pricing.totalDiscountAmount(),
                        // Locale.ROOT: always "10.00", never "10,00" depending on the server locale
                        String.format(Locale.ROOT, "%.2f", pricing.getDiscountPercentage()),
                        pricing.appliedStrategies(), pricing.couponUsed(),
                        elapsedMillis(start));
            }
            return result;
        } catch (Throwable ex) {
            log.atLevel(track.level()).log(
                    "[PRICING] operation={} outcome=rejected principal={} reason={} durationMs={}",
                    operation, principal, ex.getClass().getSimpleName(), elapsedMillis(start));
            throw ex;
        }
    }

    private static long elapsedMillis(long startNanos) {
        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos);
    }

    private String currentPrincipal() {
        try {
            return authFacade.getCurrentUsername();
        } catch (RuntimeException e) {
            return "anonymous";
        }
    }
}
