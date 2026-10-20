package com.example.esercizi.security;

import com.example.esercizi.model.Role;
import com.example.esercizi.model.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class JwtUtil{

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration:86400000}")  // Default: 24 hours
    private Long expiration;

    /**
     * Generate JWT token for user with roles and permissions
     */
    public String generateToken(User user) {
        Map<String, Object> claims = new HashMap<>();

        // User info
        claims.put("userId", user.getId());
        claims.put("username", user.getUsername());
        claims.put("isAnonymous", false);

        // Roles (list of role names)
        List<String> roles = user.getRoles().stream()
                .map(Role::getName)
                .collect(Collectors.toList());
        claims.put("roles", roles);

        return buildToken(claims, user.getUsername());
    }

    public String generateAnonymousToken(String anonymousId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("anonymousId", anonymousId);
        claims.put("isAnonymous", true);
        claims.put("roles", List.of("GUEST"));

        return buildToken(claims, "guest_" + anonymousId);
    }

    private String buildToken(Map<String, Object> claims, String subject) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);

        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));

        return Jwts.builder()
                .claims(claims)
                .subject(subject)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(key)
                .compact();
    }

    /**
     * Get expiration time in milliseconds
     */
    public Long getExpirationTime() {
        return expiration;
    }


    /**
     * Extract all claims from token
     */
    public Claims extractAllClaims(String token) {
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Extract username from token
     */
    public String extractSubject(String token) {
        return extractAllClaims(token).getSubject();
    }

    public String extractUsername(String token) {
        return extractAllClaims(token).get("username",String.class);
    }

    /**
     * Extract user ID from token
     */
    public Long extractUserId(String token) {
        return extractAllClaims(token).get("userId", Long.class);
    }

    public String extractUserEmail(String token) {
        return extractAllClaims(token).get("email", String.class);
    }

    // RBAC
    /**
     * Extract roles from token
     */
    @SuppressWarnings("unchecked")
    public List<String> extractRoles(String token) {
        Object roles = extractAllClaims(token).get("roles");
        if (roles instanceof List) {
            return (List<String>) roles;
        }
        return List.of();
    }

    public boolean isAnonymous(String token) {
        Boolean isAnon = extractAllClaims(token).get("isAnonymous", Boolean.class);
        return isAnon != null && isAnon;
    }

    /**
     * Verifies signature and expiration of a guest token and returns its anonymousId.
     *
     * @throws BadCredentialsException if the token is invalid, expired or not a guest token
     */
    public String extractVerifiedAnonymousId(String token) {
        try {
            Claims claims = extractAllClaims(token);
            String anonymousId = claims.get("anonymousId", String.class);
            if (!Boolean.TRUE.equals(claims.get("isAnonymous", Boolean.class)) || anonymousId == null) {
                throw new BadCredentialsException("Not a guest token");
            }
            return anonymousId;
        } catch (JwtException | IllegalArgumentException e) {
            throw new BadCredentialsException("Invalid guest token", e);
        }
    }

    public String extractAnonymousId(String token) {
        return extractAllClaims(token).get("anonymousId", String.class);
    }


    /**
     * Check if token is expired
     */
    public boolean isTokenExpired(String token) {
        return extractAllClaims(token).getExpiration().before(new Date());
    }

    /**
     * Validate token
     */
    public boolean validateToken(String token) {
        try {
            return !isTokenExpired(token);
        } catch (Exception e) {
            return false;
        }
    }
}
