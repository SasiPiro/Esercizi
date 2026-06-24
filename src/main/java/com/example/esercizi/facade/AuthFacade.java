package com.example.esercizi.facade;

import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Objects;

public class AuthFacade {

    public String getCurrentUsername() {
        String principal = (String) Objects.requireNonNull(
                SecurityContextHolder.getContext().getAuthentication()).getPrincipal();
        return principal;
    }
}
