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
import java.util.Set;

@Component
public class JwtUtil {

    /** HS256 requires a key of at least 256 bits. */
    static final int MIN_SECRET_BYTES = 32;

    /**
     * Secrets that were committed to the repository at some point: anyone can read them,
     * so a token signed with them proves nothing.
     */
    private static final Set<String> KNOWN_PUBLIC_SECRETS = Set.of(
            "your-secret-key-must-be-at-least-256-bits-long-for-hs256-algorithm-change-in-production");

    private final SecretKey key;
    private final long expiration;

    /**
     * Fails fast at startup when the secret is missing, too short or publicly known:
     * better a crash on deploy than an application that accepts forged tokens.
     */
    public JwtUtil(@Value("${jwt.secret:}") String secret,
                   @Value("${jwt.expiration:86400000}") long expiration) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("jwt.secret is not set: provide it with the JWT_SECRET environment "
                    + "variable (or run with the 'dev' profile for local development)");
        }
        if (KNOWN_PUBLIC_SECRETS.contains(secret)) {
            throw new IllegalStateException("jwt.secret is a publicly known value: generate a new one");
        }
        if (secret.getBytes(StandardCharsets.UTF_8).length < MIN_SECRET_BYTES) {
            throw new IllegalStateException("jwt.secret must be at least " + MIN_SECRET_BYTES + " bytes long");
        }
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)); // built once, not per call
        this.expiration = expiration;
    }

    /**
     * Generate JWT token for user with roles
     */
    public String generateToken(User user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId());
        claims.put("username", user.getUsername());
        claims.put("isAnonymous", false);
        claims.put("roles", user.getRoles().stream().map(Role::getName).toList());
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
        return Jwts.builder()
                .claims(claims)
                .subject(subject)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expiration))
                .signWith(key)
                .compact();
    }

    /** Get expiration time in milliseconds */
    public Long getExpirationTime() {
        return expiration;
    }

    /**
     * Verifies signature and expiration and returns the claims.
     * Parse a token ONCE per request and read every claim from the result.
     *
     * @throws JwtException if the token is invalid or expired
     */
    public Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public static boolean isAnonymous(Claims claims) {
        return Boolean.TRUE.equals(claims.get("isAnonymous", Boolean.class));
    }

    /** Principal name: the username for users, the anonymousId for guests. */
    public static String principalOf(Claims claims) {
        return isAnonymous(claims)
                ? claims.get("anonymousId", String.class)
                : claims.get("username", String.class);
    }

    public static List<String> rolesOf(Claims claims) {
        Object roles = claims.get("roles");
        if (roles instanceof List<?> list) {
            return list.stream().map(String::valueOf).toList();
        }
        return List.of();
    }

    /**
     * Verifies signature and expiration of a guest token and returns its anonymousId.
     *
     * @throws BadCredentialsException if the token is invalid, expired or not a guest token
     */
    public String extractVerifiedAnonymousId(String token) {
        try {
            Claims claims = parse(token);
            String anonymousId = claims.get("anonymousId", String.class);
            if (!isAnonymous(claims) || anonymousId == null) {
                throw new BadCredentialsException("Not a guest token");
            }
            return anonymousId;
        } catch (JwtException | IllegalArgumentException e) {
            throw new BadCredentialsException("Invalid guest token", e);
        }
    }
}
