package com.ecommerce.monolith.shared;

import org.springframework.http.HttpStatus;

import static com.ecommerce.monolith.shared.AppConstants.ERROR_MISSING_RESOURCE;
import static com.ecommerce.monolith.shared.AppConstants.TYPE_MISSING_RESOURCE;

public class MissingResourceException extends BusinessException {
    public MissingResourceException(String message) {
        super(HttpStatus.BAD_REQUEST, ERROR_MISSING_RESOURCE, TYPE_MISSING_RESOURCE, message);
    }
}
