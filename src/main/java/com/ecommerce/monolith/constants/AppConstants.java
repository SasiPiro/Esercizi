package com.ecommerce.monolith.constants;

import java.net.URI;

public final class AppConstants {

    private AppConstants() {
    } // non-instantiable

    // Error messages
    public static final String ERROR_PRODUCT_NOT_FOUND = "Product not found";
    public static final String ERROR_PRODUCT_NOT_FOUND_FOR_CODE = "Product not found for code ";
    public static final String ERROR_PRODUCT_NOT_IN_CART = "Product not in the cart: ";
    public static final String ERROR_CART_EMPTY = "Cart is empty";
    public static final String ERROR_USER_NOT_FOUND = "User not found";
    public static final String ERROR_ORDER_NOT_FOUND = "Order not found: ";
    public static final String ERROR_PROMOTION_NOT_FOUND = "Promotion code not found or not active";
    public static final String ERROR_PROMOTION_EXPIRED = "Promotion has expired or reached its usage limit";
    public static final String ERROR_COUPON_NOT_CUMULATIVE = "Coupon cannot be combined with active seasonal promotions";
    public static final String ERROR_INVALID_CREDENTIALS = "Invalid username or password";
    public static final String ERROR_MISSING_RESOURCE = "One or more necessary resources are missing";

    // ERROR URI
    public static final String BASE_URL = "https://ecommerce-monolith.example/errors/";
    public static final URI TYPE_USER_NOT_FOUND = URI.create(BASE_URL + "user-not-found");
    public static final URI TYPE_VALIDATION_ERROR = URI.create(BASE_URL + "validation-error");
    public static final URI TYPE_GENERIC_ERROR = URI.create(BASE_URL + "internal-server-error");
    public static final URI TYPE_PRODUCT_NOT_FOUND = URI.create(BASE_URL + "product-not-found");
    public static final URI TYPE_ORDER_NOT_FOUND = URI.create(BASE_URL + "order-not-found");
    public static final URI TYPE_CART_EMPTY = URI.create(BASE_URL + "cart-empty");
    public static final URI TYPE_PROMOTION_NOT_FOUND = URI.create(BASE_URL + "promotion-not-found");
    public static final URI TYPE_PROMOTION_EXPIRED = URI.create(BASE_URL + "promotion-expired");
    public static final URI TYPE_COUPON_NOT_CUMULATIVE = URI.create(BASE_URL + "coupon-not-cumulative");
    public static final URI TYPE_MISSING_RESOURCE = URI.create(BASE_URL + "missing-resource");
    public static final URI TYPE_UNAUTHORIZED = URI.create(BASE_URL + "unauthorized");
    public static final URI TYPE_FORBIDDEN = URI.create(BASE_URL + "forbidden");
    public static final URI TYPE_DUPLICATE_RESOURCE = URI.create(BASE_URL + "duplicate-resource");
    public static final URI TYPE_CHECKOUT_CONFLICT = URI.create(BASE_URL + "checkout-conflict");
    public static final URI TYPE_IDEMPOTENCY_KEY_REUSED = URI.create(BASE_URL + "idempotency-key-reused");

}
