package com.ecommerce.monolith.identity;

import com.ecommerce.monolith.identity.internal.JwtUtil;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Single access point to the authenticated principal.
 * Registered as a Spring bean so it can be injected into services and aspects.
 */
@Component
public class AuthFacade {

    private final JwtUtil jwtUtil;

    public AuthFacade(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    public String getCurrentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof String principal)) {
            throw new AuthenticationCredentialsNotFoundException("No authenticated principal");
        }
        return principal;
    }

    /**
     * Verifies signature and expiration of a guest token and returns its anonymousId: the
     * proof that the caller owns that guest's cart.
     *
     * @throws org.springframework.security.authentication.BadCredentialsException if the
     *         token is invalid, expired or not a guest token
     */
    public String verifiedGuestId(String guestToken) {
        return jwtUtil.extractVerifiedAnonymousId(guestToken);
    }
}
