package com.example.esercizi.aspect;

import com.example.esercizi.dto.CalculationResultDTO;
import com.example.esercizi.facade.AuthFacade;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;

@Aspect
@Component
public class PromotionTrackingAspect {

    private static final Logger log = LoggerFactory.getLogger(PromotionTrackingAspect.class);
    private final AuthFacade authFacade;

    public PromotionTrackingAspect(AuthFacade authFacade) {
        this.authFacade = authFacade;
    }

    @Around("@annotation(com.example.esercizi.aspect.TrackPromotion)")
    public Object trackPromotionExecution(ProceedingJoinPoint joinPoint) throws Throwable {

            String username = extractUserName();
            StopWatch stopWatch = new StopWatch();
            stopWatch.start();

            try {
                Object result = joinPoint.proceed();
                stopWatch.stop();

                if (result instanceof CalculationResultDTO audit) {
                    log.info("[PROMOTION_TRACKING] user={} | init={} | final={} | saved={} (-{}%) | strategies={} | time_ms={}",
                            // Retrieve user for logging
                            username,
                            audit.initialPrice(),
                            audit.finalPrice(),
                            audit.totalDiscountAmount(),
                            String.format("%.2f", audit.getDiscountPercentage()),
                            audit.appliedStrategies(),
                            stopWatch.getTotalTimeMillis()
                    );
                }
                return result;
            } catch (Throwable ex) {
                log.error("[PROMOTION_ERROR] user={} | method={} | error={} | time_ms={}",
                        username,
                        joinPoint.getSignature().getName(),
                        ex.getMessage(),
                        stopWatch.getTotalTimeMillis());
                throw ex;
            }
    }

    private String extractUserName() {
        try {
            return authFacade.getCurrentUsername();
        } catch (Exception e) {
            return "GUEST";
        }
    }
}
