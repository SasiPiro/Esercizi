package com.ecommerce.monolith.catalog;

import com.ecommerce.monolith.shared.BusinessException;
import org.springframework.http.HttpStatus;

import static com.ecommerce.monolith.shared.AppConstants.ERROR_PRODUCT_NOT_FOUND;
import static com.ecommerce.monolith.shared.AppConstants.ERROR_PRODUCT_NOT_FOUND_FOR_CODE;
import static com.ecommerce.monolith.shared.AppConstants.TYPE_PRODUCT_NOT_FOUND;

public class ProductNotFoundException extends BusinessException {
    public ProductNotFoundException(String message) {
        super(HttpStatus.NOT_FOUND, ERROR_PRODUCT_NOT_FOUND, TYPE_PRODUCT_NOT_FOUND, message);
    }
    public static ProductNotFoundException forCode(String code){
        return new ProductNotFoundException(ERROR_PRODUCT_NOT_FOUND_FOR_CODE + code);
    }
}
