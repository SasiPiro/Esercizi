package com.ecommerce.monolith.identity;

/**
 * A guest token is about to be issued for a new anonymous shopper.
 *
 * <p>
 * Published synchronously, before the token is returned: listeners (the cart module creates
 * the guest cart) finish before the guest can send any request, so they cannot race with it.
 * </p>
 */
public record GuestSessionStarted(String anonymousId) {
}
