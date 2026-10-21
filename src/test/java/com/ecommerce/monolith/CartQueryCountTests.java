package com.ecommerce.monolith;

import com.jayway.jsonpath.JsonPath;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Guards against N+1 queries: reading a cart must cost the same number of SQL
 * statements whether it holds 1 product or 4.
 */
@SpringBootTest(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
@ActiveProfiles("dev")
@AutoConfigureMockMvc
class CartQueryCountTests {

    @Autowired MockMvc mvc;
    @Autowired EntityManagerFactory emf;

    @Test
    void readingACartCostsAConstantNumberOfQueries() throws Exception {
        long withOneProduct = queriesToReadCart(userWithProducts("P001"), 1);
        long withFourProducts = queriesToReadCart(userWithProducts("P001", "P002", "P003", "P004"), 4);

        assertThat(withFourProducts).isEqualTo(withOneProduct);
        // user + roles (1 query) and cart + items + products (1 query)
        assertThat(withFourProducts).isLessThanOrEqualTo(2);
    }

    private long queriesToReadCart(String token, int expectedItems) throws Exception {
        Statistics stats = emf.unwrap(SessionFactory.class).getStatistics();
        stats.clear();
        mvc.perform(get("/api/v1/cart").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(expectedItems));
        return stats.getPrepareStatementCount();
    }

    private String userWithProducts(String... productCodes) throws Exception {
        String username = "qc" + UUID.randomUUID().toString().substring(0, 8);
        String credentials = "{\"username\":\"" + username + "\",\"password\":\"secret123\"}";
        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content(credentials))
                .andExpect(status().isCreated());
        String body = mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content(credentials))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String token = JsonPath.read(body, "$.token");
        for (String code : productCodes) {
            mvc.perform(post("/api/v1/cart/items").contentType(MediaType.APPLICATION_JSON)
                            .content("{\"productCode\":\"" + code + "\",\"quantity\":1}")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk());
        }
        return token;
    }
}
