package com.ecommerce.monolith;

import com.ecommerce.monolith.identity.internal.JwtUtil;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("dev")
@AutoConfigureMockMvc
class JwtSecurityTests {

    /** The default secret that used to be committed in application.yaml. */
    private static final String OLD_PUBLIC_SECRET =
            "your-secret-key-must-be-at-least-256-bits-long-for-hs256-algorithm-change-in-production";

    @Autowired MockMvc mvc;

    @Test
    void refusesToStartWithoutAStrongPrivateSecret() {
        assertThatThrownBy(() -> new JwtUtil("", 1000)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> new JwtUtil("too-short", 1000)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> new JwtUtil(OLD_PUBLIC_SECRET, 1000)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void tokenForgedWithThePreviouslyPublicSecretIsRejected() throws Exception {
        String forged = Jwts.builder()
                .claims(java.util.Map.of("userId", 2, "username", "admin", "isAnonymous", false,
                        "roles", List.of("ADMIN")))
                .subject("admin")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 3_600_000))
                .signWith(Keys.hmacShaKeyFor(OLD_PUBLIC_SECRET.getBytes(StandardCharsets.UTF_8)))
                .compact();

        mvc.perform(get("/api/v1/cart").header("Authorization", "Bearer " + forged))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void garbageTokenIs401() throws Exception {
        mvc.perform(get("/api/v1/cart").header("Authorization", "Bearer not.a.jwt"))
                .andExpect(status().isUnauthorized());
    }
}
