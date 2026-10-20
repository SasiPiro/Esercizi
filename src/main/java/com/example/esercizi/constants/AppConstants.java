package com.example.esercizi.constants;

import java.net.URI;
import java.time.Instant;

public final class AppConstants {

    private AppConstants() {
    } // non-instantiable

    // Header
    public static final String SESSION_HEADER = "X-SESSION-ID";

    // Error messages
    public static final String ERROR_PRODUCT_NOT_FOUND = "Product not found";
    public static final String ERROR_PRODUCT_NOT_FOUND_FOR_CODE = "Product not found for code ";
    public static final String ERROR_CART_EMPTY = "Cart is empty";
    public static final String ERROR_USER_NOT_FOUND = "User not found";
    public static final String ERROR_PROMOTION_NOT_FOUND = "Promotion code not found or not active";
    public static final String ERROR_PROMOTION_EXPIRED = "Promotion has expired or reached its usage limit";
    public static final String ERROR_CONCURRENT_PROMOTION = "Promotion was claimed by another user. Please try again";
    public static final String ERROR_COUPON_NOT_CUMULATIVE = "Coupon cannot be combined with active seasonal promotions";
    public static final String ERROR_MISSING_RESOURCE = "One or more necessary resources are missing";

    // ERROR URI
    public static final String BASE_URL = "https://api.esercizi.it/errors/";
    public static final URI TYPE_USER_NOT_FOUND = URI.create(BASE_URL + "user-not-found");
    public static final URI TYPE_VALIDATION_ERROR = URI.create(BASE_URL + "validation-error");
    public static final URI TYPE_GENERIC_ERROR = URI.create(BASE_URL + "internal-server-error");
    public static final URI TYPE_PRODUCT_NOT_FOUND = URI.create(BASE_URL + "product-not-found");
    public static final URI TYPE_CART_EMPTY = URI.create(BASE_URL + "cart-empty");
    public static final URI TYPE_PROMOTION_NOT_FOUND = URI.create(BASE_URL + "promotion-not-found");
    public static final URI TYPE_PROMOTION_EXPIRED = URI.create(BASE_URL + "promotion-expired");
    public static final URI TYPE_CONCURRENT_PROMOTION = URI.create(BASE_URL + "concurrent-promotion-access");
    public static final URI TYPE_COUPON_NOT_CUMULATIVE = URI.create(BASE_URL + "coupon-not-cumulative");
    public static final URI TYPE_MISSING_RESOURCE = URI.create(BASE_URL + "missing-resource");
    public static final URI TYPE_CHECKOUT_CONFLICT = URI.create(BASE_URL + "checkout-conflict");

    // Promotion duration
    public static final Instant BLACKFRIDAY_PROMOTION_START = Instant.parse("2026-11-25T00:00:00Z");
    public static final Instant BLACKFRIDAY_PROMOTION_END = Instant.parse("2026-11-27T23:59:59Z");
    public static final Instant CHRISTMAS_PROMOTION_START = Instant.parse("2026-12-20T00:00:00Z");
    public static final Instant CHRISTMAS_PROMOTION_END = Instant.parse("2026-12-26T23:59:59Z");

}
