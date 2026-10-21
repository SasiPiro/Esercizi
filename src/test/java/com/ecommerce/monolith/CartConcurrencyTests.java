package com.ecommerce.monolith;

import com.ecommerce.monolith.cart.internal.CartOwnershipRepository;
import com.ecommerce.monolith.identity.UserRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The first requests of a new user often arrive together (several browser tabs,
 * double clicks, client retries). They must all see the same, single cart.
 */
@SpringBootTest
@ActiveProfiles("dev")
@AutoConfigureMockMvc
class CartConcurrencyTests {

    @Autowired MockMvc mvc;
    @Autowired UserRepository userRepository;
    @Autowired CartOwnershipRepository ownershipRepository;

    @Test
    void concurrentFirstRequestsOfANewUserCreateExactlyOneCart() throws Exception {
        String username = "cc" + UUID.randomUUID().toString().substring(0, 8);
        String credentials = "{\"username\":\"" + username + "\",\"password\":\"secret123\"}";
        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content(credentials))
                .andExpect(status().isCreated());
        String body = mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content(credentials))
                .andReturn().getResponse().getContentAsString();
        String token = JsonPath.read(body, "$.token");

        int requests = 16;
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger ok = new AtomicInteger();
        ConcurrentHashMap<Integer, Integer> statuses = new ConcurrentHashMap<>();
        ConcurrentHashMap<Integer, Boolean> cartIds = new ConcurrentHashMap<>();

        try (var pool = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int i = 0; i < requests; i++) {
                pool.submit(() -> {
                    start.await();
                    var response = mvc.perform(get("/api/v1/cart").header("Authorization", "Bearer " + token))
                            .andReturn().getResponse();
                    statuses.merge(response.getStatus(), 1, Integer::sum);
                    if (response.getStatus() == 200) {
                        ok.incrementAndGet();
                        cartIds.put(JsonPath.read(response.getContentAsString(), "$.id"), true);
                    }
                    return null;
                });
            }
            start.countDown();
        }

        assertThat(statuses).as("HTTP status -> count").containsOnlyKeys(200);
        assertThat(ok).hasValue(requests);
        assertThat(cartIds).as("distinct cart ids returned").hasSize(1);
        var user = userRepository.findByUsernameWithRoles(username).orElseThrow();
        assertThat(ownershipRepository.findByUser(user)).isPresent();
    }
}
