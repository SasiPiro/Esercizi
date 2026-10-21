package com.ecommerce.monolith.identity.internal;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Authenticates requests carrying "Authorization: Bearer &lt;jwt&gt;".
 *
 * <p>
 * The token is parsed and verified ONCE (signature + expiration). Requests without a
 * token simply continue unauthenticated: public endpoints work, protected ones get 401
 * from the entry point. Invalid tokens are logged at DEBUG without stack trace, so a
 * client sending garbage cannot flood the logs.
 * </p>
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtUtil jwtUtil;

    public JwtAuthenticationFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (header != null && header.startsWith(BEARER_PREFIX)
                && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                Claims claims = jwtUtil.parse(header.substring(BEARER_PREFIX.length()));
                String principal = JwtUtil.principalOf(claims);
                if (principal != null) {
                    List<GrantedAuthority> authorities = JwtUtil.rolesOf(claims).stream()
                            .distinct()
                            .<GrantedAuthority>map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                            .toList();
                    SecurityContextHolder.getContext().setAuthentication(
                            new UsernamePasswordAuthenticationToken(principal, null, authorities));
                }
            } catch (JwtException | IllegalArgumentException e) {
                log.debug("[JWT_REJECTED] path={} reason={}", request.getRequestURI(), e.getClass().getSimpleName());
            }
        }
        filterChain.doFilter(request, response);
    }
}
