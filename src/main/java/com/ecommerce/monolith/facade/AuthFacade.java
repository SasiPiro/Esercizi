package com.ecommerce.monolith.facade;

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

    public String getCurrentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof String principal)) {
            throw new AuthenticationCredentialsNotFoundException("No authenticated principal");
        }
        return principal;
    }
}
