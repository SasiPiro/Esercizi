package com.ecommerce.monolith.pricing;

import com.ecommerce.monolith.shared.BusinessException;
import org.springframework.http.HttpStatus;

import static com.ecommerce.monolith.shared.AppConstants.ERROR_COUPON_NOT_CUMULATIVE;
import static com.ecommerce.monolith.shared.AppConstants.TYPE_COUPON_NOT_CUMULATIVE;

/**
 * Exception thrown when attempting to apply a coupon
 * when an incompatible promotion is already active (e.g. Seasonal Discounts).
 */
public class CouponNotCumulativeException extends BusinessException {
    public CouponNotCumulativeException(String message) {
        super(HttpStatus.CONFLICT, ERROR_COUPON_NOT_CUMULATIVE, TYPE_COUPON_NOT_CUMULATIVE, message);
    }
}
