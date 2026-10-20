package com.ecommerce.monolith.exception;

import static com.ecommerce.monolith.constants.AppConstants.ERROR_PRODUCT_NOT_FOUND_FOR_CODE;

public class ProductNotFoundException extends RuntimeException {
    public ProductNotFoundException(String message) {
        super(message);
    }
    public static ProductNotFoundException forCode(String code){
        return new ProductNotFoundException(ERROR_PRODUCT_NOT_FOUND_FOR_CODE + code);
    }
}
