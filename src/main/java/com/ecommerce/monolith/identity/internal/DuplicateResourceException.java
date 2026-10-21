package com.ecommerce.monolith.identity.internal;

import com.ecommerce.monolith.shared.BusinessException;
import org.springframework.http.HttpStatus;

import static com.ecommerce.monolith.shared.AppConstants.TYPE_DUPLICATE_RESOURCE;

public class DuplicateResourceException extends BusinessException {
    public DuplicateResourceException(String message) {
        super(HttpStatus.CONFLICT, "Resource already exists", TYPE_DUPLICATE_RESOURCE, message);
    }
}
