package com.example.esercizi;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Without the dev profile (i.e. as in production) dev conveniences must be off.
 * Only the secret is provided, as the JWT_SECRET environment variable would be.
 */
@SpringBootTest(properties = "jwt.secret=prod-like-test-secret-0123456789-abcdefghijklmnop")
@AutoConfigureMockMvc
class ProductionDefaultsTests {

    @Autowired MockMvc mvc;

    @Test
    void h2ConsoleIsNotExposed() throws Exception {
        mvc.perform(get("/h2-console/")).andExpect(status().isUnauthorized());
    }

    @Test
    void noCrossOriginAccessByDefault() throws Exception {
        mvc.perform(options("/api/v1/cart")
                        .header("Origin", "http://localhost:3000")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
    }

    @Test
    void framesAreDenied() throws Exception {
        mvc.perform(get("/api/v1/cart")).andExpect(header().string("X-Frame-Options", "DENY"));
    }
}
