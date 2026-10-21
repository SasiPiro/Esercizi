package com.ecommerce.monolith.identity;

import com.ecommerce.monolith.shared.BusinessException;
import org.springframework.http.HttpStatus;

import static com.ecommerce.monolith.shared.AppConstants.ERROR_USER_NOT_FOUND;
import static com.ecommerce.monolith.shared.AppConstants.TYPE_USER_NOT_FOUND;

public class UserNotFoundException extends BusinessException {
    public UserNotFoundException(String message) {
        super(HttpStatus.NOT_FOUND, ERROR_USER_NOT_FOUND, TYPE_USER_NOT_FOUND, message);
    }
}
