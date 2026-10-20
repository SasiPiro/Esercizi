package com.example.esercizi;

import com.example.esercizi.exception.PromotionExpiredException;
import com.example.esercizi.model.Coupon;
import com.example.esercizi.repository.CouponRepository;
import com.example.esercizi.service.CouponService;
import com.example.esercizi.service.TimeService;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Regression tests for the P0 review fixes. Each test uses its own guest, user or coupon,
 * so tests do not depend on each other even though they share the in-memory database.
 */
@SpringBootTest
@ActiveProfiles("dev")
@AutoConfigureMockMvc
class CheckoutFlowTests {

    /** Outside Black Friday / Christmas, so seasonal promotions never interfere. */
    private static final Instant NOW = Instant.parse("2026-10-20T12:00:00Z");
    private static final Instant FAR_FUTURE = Instant.parse("2030-01-01T00:00:00Z");

    @Autowired MockMvc mvc;
    @Autowired CouponRepository couponRepository;
    @Autowired CouponService couponService;
    @Autowired TransactionTemplate tx;

    @MockitoBean TimeService timeService;

    @BeforeEach
    void fixClock() {
        when(timeService.now()).thenReturn(NOW);
    }

    // ---------------------------------------------------------------- auth

    @Test
    void registerThenLogin() throws Exception {
        String username = "user" + UUID.randomUUID().toString().substring(0, 8);
        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"secret123\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.roles[0]").value("CUSTOMER"));

        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"secret123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    void wrongPasswordIs401NotInternalError() throws Exception {
        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"wrong-password\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void seedUsersCanLogIn() throws Exception {
        assertThat(login("admin", "admin-password")).isNotBlank();
        assertThat(login("JAY", "jay-password")).isNotBlank();
    }

    @Test
    void protectedEndpointWithoutTokenIs401() throws Exception {
        mvc.perform(get("/api/v1/cart")).andExpect(status().isUnauthorized());
    }

    // ---------------------------------------------------------------- cart & checkout

    @Test
    void guestCartWorksAndQuoteUsesServerPrices() throws Exception {
        String guest = guestToken();
        addItem(guest, "P004", 2); // 2 x 37.20

        mvc.perform(get("/api/v1/checkout/quote").header("Authorization", bearer(guest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.initialPrice").value(74.40))
                .andExpect(jsonPath("$.finalPrice").value(74.40));
    }

    @Test
    void checkoutIgnoresClientSuppliedPrices() throws Exception {
        String guest = guestToken();
        addItem(guest, "P004", 10); // 10 x 37.20 = 372.00

        // The old API priced this body: 10 x 0.01. The endpoint no longer reads a body.
        String tamperedBody = """
                {"id":1,"items":[{"id":1,"product":{"id":4,"code":"P004","basePrice":0.01},"quantity":10}]}
                """;
        mvc.perform(post("/api/v1/checkout").header("Authorization", bearer(guest))
                        .contentType(MediaType.APPLICATION_JSON).content(tamperedBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.initialPrice").value(372.00))
                .andExpect(jsonPath("$.finalPrice").value(372.00));

        // The purchased cart is closed: the next request gets a new, empty cart
        mvc.perform(get("/api/v1/cart").header("Authorization", bearer(guest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isEmpty());
        mvc.perform(post("/api/v1/checkout").header("Authorization", bearer(guest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void invalidQuantitiesAre400() throws Exception {
        String guest = guestToken();
        mvc.perform(post("/api/v1/cart/add").param("productCode", "P001").param("quantity", "-5")
                        .header("Authorization", bearer(guest)))
                .andExpect(status().isBadRequest());

        addItem(guest, "P001", 99);
        mvc.perform(post("/api/v1/cart/add").param("productCode", "P001").param("quantity", "1")
                        .header("Authorization", bearer(guest)))
                .andExpect(status().isBadRequest());
    }

    // ---------------------------------------------------------------- coupons

    @Test
    void quoteDoesNotConsumeTheCouponButCheckoutDoes() throws Exception {
        String code = newCoupon("0.2000", 1);
        String guest = guestToken();
        addItem(guest, "P001", 2); // 21.00

        for (int i = 0; i < 3; i++) {
            mvc.perform(get("/api/v1/checkout/quote").param("couponCode", code).header("Authorization", bearer(guest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.finalPrice").value(16.80));
        }
        assertThat(usage(code)).isZero();

        mvc.perform(post("/api/v1/checkout").param("couponCode", code).header("Authorization", bearer(guest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.finalPrice").value(16.80));
        assertThat(usage(code)).isEqualTo(1);

        // The only use is gone: another shopper cannot use it
        String other = guestToken();
        addItem(other, "P001", 1);
        mvc.perform(get("/api/v1/checkout/quote").param("couponCode", code).header("Authorization", bearer(other)))
                .andExpect(status().isGone());
    }

    @Test
    void lastCouponUseIsWonByExactlyOneOfManyConcurrentBuyers() throws Exception {
        String code = newCoupon("0.1000", 1);
        int buyers = 20;
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger winners = new AtomicInteger();
        AtomicInteger rejected = new AtomicInteger();

        try (var pool = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int i = 0; i < buyers; i++) {
                pool.submit(() -> {
                    start.await();
                    try {
                        tx.executeWithoutResult(s -> couponService.redeemCoupon(code));
                        winners.incrementAndGet();
                    } catch (PromotionExpiredException expected) {
                        rejected.incrementAndGet();
                    }
                    return null;
                });
            }
            start.countDown();
        }

        assertThat(winners).hasValue(1);
        assertThat(rejected).hasValue(buyers - 1);
        assertThat(usage(code)).isEqualTo(1);
    }

    // ---------------------------------------------------------------- merge

    @Test
    void mergeRequiresAValidGuestToken() throws Exception {
        String user = login("admin", "admin-password");
        mvc.perform(post("/api/v1/cart/merge").header("Authorization", bearer(user))
                        .header("X-Guest-Token", "not-a-token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void mergeMovesGuestItemsIntoUserCart() throws Exception {
        String username = "merge" + UUID.randomUUID().toString().substring(0, 8);
        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"secret123\"}"))
                .andExpect(status().isCreated());
        String user = login(username, "secret123");
        addItem(user, "P002", 1);

        String guest = guestToken();
        addItem(guest, "P002", 2);
        addItem(guest, "P003", 1);

        mvc.perform(post("/api/v1/cart/merge").header("Authorization", bearer(user)).header("X-Guest-Token", guest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(2))
                .andExpect(jsonPath("$.items[?(@.product.code == 'P002')].quantity").value(3))
                .andExpect(jsonPath("$.items[?(@.product.code == 'P003')].quantity").value(1));

        // the merged items survive the deletion of the guest cart
        mvc.perform(get("/api/v1/cart").header("Authorization", bearer(user)))
                .andExpect(jsonPath("$.items.length()").value(2));
    }

    // ---------------------------------------------------------------- helpers

    private String guestToken() throws Exception {
        String body = mvc.perform(post("/api/v1/auth/guest"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.token");
    }

    private String login(String username, String password) throws Exception {
        String body = mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.token");
    }

    private void addItem(String token, String productCode, int quantity) throws Exception {
        mvc.perform(post("/api/v1/cart/add").param("productCode", productCode)
                        .param("quantity", String.valueOf(quantity)).header("Authorization", bearer(token)))
                .andExpect(status().isOk());
    }

    private String newCoupon(String discount, int usageLimit) {
        String code = "T" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        couponRepository.save(new Coupon(code, "test", new BigDecimal(discount), usageLimit, FAR_FUTURE));
        return code;
    }

    private int usage(String code) {
        return couponRepository.findAll().stream()
                .filter(c -> c.getCode().equals(code))
                .findFirst().orElseThrow().getCurrentUsage();
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }
}
