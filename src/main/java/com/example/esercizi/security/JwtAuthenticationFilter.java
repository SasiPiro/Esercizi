package com.example.esercizi.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtTokenUtil;

    public JwtAuthenticationFilter(JwtUtil jwtTokenUtil) {
        this.jwtTokenUtil = jwtTokenUtil;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String requestTokenHeader = request.getHeader("Authorization");
        String jwtToken = null;

        if (requestTokenHeader != null && requestTokenHeader.startsWith("Bearer ")
                && SecurityContextHolder.getContext().getAuthentication() == null) {
            jwtToken = requestTokenHeader.substring(7);
            try {
                jwtTokenUtil.validateToken(jwtToken);

                String principal;
                List<String> roles = jwtTokenUtil.extractRoles(jwtToken);

                if (jwtTokenUtil.isAnonymous(jwtToken)) {
                    principal = jwtTokenUtil.extractAnonymousId(jwtToken);
                } else {
                    principal = jwtTokenUtil.extractUsername(jwtToken);
                }

                Set<GrantedAuthority> authorities = new HashSet<>();
                roles.stream().distinct().forEach(r -> authorities.add(new SimpleGrantedAuthority("ROLE_" + r)));

                UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken = new UsernamePasswordAuthenticationToken(
                        principal, null, authorities);
                SecurityContextHolder.getContext().setAuthentication(usernamePasswordAuthenticationToken);
            } catch (Exception e) {
                logger.error("Unable to validate JWT Token", e);
            }
        } else {
            String exceptionMessage = "JWT Token non presente nell'header authorization o non inizia con \"Bearer\"";
            logger.warn(exceptionMessage);
        }
        filterChain.doFilter(request, response);
    }
}
