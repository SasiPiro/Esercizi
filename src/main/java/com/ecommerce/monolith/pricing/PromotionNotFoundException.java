package com.ecommerce.monolith.pricing;

import com.ecommerce.monolith.shared.BusinessException;
import org.springframework.http.HttpStatus;

import static com.ecommerce.monolith.shared.AppConstants.ERROR_PROMOTION_NOT_FOUND;
import static com.ecommerce.monolith.shared.AppConstants.TYPE_PROMOTION_NOT_FOUND;

public class PromotionNotFoundException extends BusinessException {
    public PromotionNotFoundException(String message) {
        super(HttpStatus.NOT_FOUND, ERROR_PROMOTION_NOT_FOUND, TYPE_PROMOTION_NOT_FOUND, message);
    }
}
