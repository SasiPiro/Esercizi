package com.ecommerce.monolith.pricing;

import com.ecommerce.monolith.shared.BusinessException;
import org.springframework.http.HttpStatus;

import static com.ecommerce.monolith.shared.AppConstants.ERROR_PROMOTION_EXPIRED;
import static com.ecommerce.monolith.shared.AppConstants.TYPE_PROMOTION_EXPIRED;

public class PromotionExpiredException extends BusinessException {
    public PromotionExpiredException(String message) {
        super(HttpStatus.GONE, ERROR_PROMOTION_EXPIRED, TYPE_PROMOTION_EXPIRED, message);
    }
}
