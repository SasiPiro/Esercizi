package com.example.esercizi.aspect;

import com.example.esercizi.dto.CalculationResultDto;
import com.example.esercizi.model.User;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.support.AopUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;

import java.math.BigDecimal;

@Aspect
@Component
public class PromotionTrackingAspect {

    private static final Logger log = LoggerFactory.getLogger(PromotionTrackingAspect.class);

    @Around("@annotation(com.example.esercizi.aspect.TrackPromotion)")
    public Object trackPromotionExecution(ProceedingJoinPoint joinPoint) throws Throwable {

            String username = extractUserName(joinPoint.getArgs());
            StopWatch stopWatch = new StopWatch();
            stopWatch.start();

            try {
                Object result = joinPoint.proceed();
                stopWatch.stop();

                if (result instanceof CalculationResultDto audit) {
                    log.info("[PROMOTION_TRACKING] user={} | init={} | final={} | saved={} (-{}%) | strategies={} | time_ms={}",
                            // Recuperiamo lo user dagli argomenti per il log
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

    private String extractUserName(Object[] args) {
        if (args == null || args.length < 2 || args[1] == null) {
            return "GUEST";
        }
        if (args[1] instanceof User user) {
            return user.getUsername();
        }
        return "UNKNOWN";
    }
}
