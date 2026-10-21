package com.ecommerce.monolith;

import com.ecommerce.monolith.model.Coupon;
import com.ecommerce.monolith.model.Order;
import com.ecommerce.monolith.model.OrderLine;
import com.ecommerce.monolith.model.Product;
import com.ecommerce.monolith.repository.CouponRepository;
import com.ecommerce.monolith.repository.OrderRepository;
import com.ecommerce.monolith.repository.ProductRepository;
import com.ecommerce.monolith.security.JwtUtil;
import com.ecommerce.monolith.service.TimeService;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * A confirmed checkout leaves an order that keeps exactly what was sold and at what price.
 * Each test uses its own guest and its own product, so the shared database stays usable.
 */
@SpringBootTest
@ActiveProfiles("dev")
@AutoConfigureMockMvc
class OrderTests {

    /** Outside Black Friday / Christmas, so seasonal promotions never interfere. */
    private static final Instant NOW = Instant.parse("2026-10-20T12:00:00Z");

    @Autowired MockMvc mvc;
    @Autowired OrderRepository orderRepository;
    @Autowired ProductRepository productRepository;
    @Autowired CouponRepository couponRepository;
    @Autowired JwtUtil jwtUtil;

    @MockitoBean TimeService timeService;

    @BeforeEach
    void fixClock() {
        when(timeService.now()).thenReturn(NOW);
    }

    @Test
    void checkoutRecordsAnOrderWithAPriceSnapshot() throws Exception {
        String product = newProduct("12.00");
        String guest = guestToken();
        addItem(guest, product, 3);
        addItem(guest, "P001", 1); // 10.50

        checkout(guest);

        List<Order> orders = orderRepository.findByAnonymousIdOrderByPlacedAtDescIdDesc(anonymousIdOf(guest));
        assertThat(orders).hasSize(1);
        Order order = orders.getFirst();
        assertThat(order.getUserId()).isNull();
        assertThat(order.getPlacedAt()).isEqualTo(NOW);
        assertThat(order.getInitialPrice()).isEqualByComparingTo("46.50");
        assertThat(order.getFinalPrice()).isEqualByComparingTo("46.50");
        assertThat(order.getAppliedStrategies()).isEmpty();
        assertThat(order.getIdempotencyKey()).isNotBlank();
        assertThat(order.getLines()).extracting(OrderLine::getProductCode, OrderLine::getQuantity)
                .containsExactlyInAnyOrder(
                        tuple(product, 3),
                        tuple("P001", 1));
    }

    @Test
    void changingAProductPriceDoesNotChangeAPastOrder() throws Exception {
        String product = newProduct("20.00");
        String guest = guestToken();
        addItem(guest, product, 2);
        checkout(guest);

        Product p = productRepository.findByCode(product).orElseThrow();
        p.setBasePrice(new BigDecimal("99.99"));
        productRepository.save(p);

        Order order = orderRepository.findByAnonymousIdOrderByPlacedAtDescIdDesc(anonymousIdOf(guest)).getFirst();
        assertThat(order.getLines().getFirst().getUnitPrice()).isEqualByComparingTo("20.00");
        assertThat(order.getFinalPrice()).isEqualByComparingTo("40.00");
    }

    @Test
    void aRejectedCheckoutLeavesNoOrder() throws Exception {
        String guest = guestToken();
        mvc.perform(post("/api/v1/checkout").header("Idempotency-Key", UUID.randomUUID().toString()).header("Authorization", "Bearer " + guest))
                .andExpect(status().isBadRequest()); // empty cart

        assertThat(orderRepository.findByAnonymousIdOrderByPlacedAtDescIdDesc(anonymousIdOf(guest))).isEmpty();
    }

    // ---------------------------------------------------------------- idempotency

    @Test
    void theSameKeyTwiceReturnsTheSameOrderAndRedeemsTheCouponOnce() throws Exception {
        String coupon = newCoupon(5);
        String guest = guestToken();
        addItem(guest, "P001", 2); // 21.00, 10% off = 18.90
        String key = UUID.randomUUID().toString();

        MvcResult first = checkout(guest, key, coupon)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.finalPrice").value(18.90))
                .andExpect(jsonPath("$.couponUsed").value(coupon))
                .andReturn();
        long orderId = ((Number) JsonPath.read(first.getResponse().getContentAsString(), "$.id")).longValue();
        assertThat(first.getResponse().getHeader("Location")).endsWith("/api/v1/orders/" + orderId);

        // The client timed out and retries: same order, nothing bought or redeemed again
        checkout(guest, key, coupon)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(orderId))
                .andExpect(jsonPath("$.finalPrice").value(18.90));

        assertThat(usage(coupon)).isEqualTo(1);
        assertThat(orderRepository.findByAnonymousIdOrderByPlacedAtDescIdDesc(anonymousIdOf(guest))).hasSize(1);
    }

    @Test
    void concurrentRequestsWithTheSameKeyCreateOneOrder() throws Exception {
        String coupon = newCoupon(50);
        String guest = guestToken();
        addItem(guest, "P003", 4);
        String key = UUID.randomUUID().toString();

        int requests = 10;
        CountDownLatch start = new CountDownLatch(1);
        Map<Integer, Integer> statuses = new ConcurrentHashMap<>();
        Set<Object> orderIds = ConcurrentHashMap.newKeySet();
        try (var pool = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int i = 0; i < requests; i++) {
                pool.submit(() -> {
                    start.await();
                    var response = checkout(guest, key, coupon).andReturn().getResponse();
                    statuses.merge(response.getStatus(), 1, Integer::sum);
                    if (response.getStatus() < 300) {
                        orderIds.add(JsonPath.read(response.getContentAsString(), "$.id"));
                    }
                    return null;
                });
            }
            start.countDown();
        }

        assertThat(statuses).as("HTTP status -> count").containsOnlyKeys(200, 201).containsEntry(201, 1);
        assertThat(orderIds).hasSize(1);
        assertThat(orderRepository.findByAnonymousIdOrderByPlacedAtDescIdDesc(anonymousIdOf(guest))).hasSize(1);
        assertThat(usage(coupon)).isEqualTo(1);
    }

    @Test
    void checkoutRequiresAnIdempotencyKey() throws Exception {
        String guest = guestToken();
        addItem(guest, "P001", 1);
        mvc.perform(post("/api/v1/checkout").header("Authorization", "Bearer " + guest))
                .andExpect(status().isBadRequest());
        checkout(guest, "k".repeat(65), null).andExpect(status().isBadRequest());
        assertThat(orderRepository.findByAnonymousIdOrderByPlacedAtDescIdDesc(anonymousIdOf(guest))).isEmpty();
    }

    @Test
    void aKeyUsedByAnotherShopperIsRejected() throws Exception {
        String key = UUID.randomUUID().toString();
        String first = guestToken();
        addItem(first, "P001", 1);
        checkout(first, key, null).andExpect(status().isCreated());

        String second = guestToken();
        addItem(second, "P001", 1);
        checkout(second, key, null)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.id").doesNotExist());
        assertThat(orderRepository.findByAnonymousIdOrderByPlacedAtDescIdDesc(anonymousIdOf(second))).isEmpty();
    }

    // ---------------------------------------------------------------- history

    @Test
    void ordersAreVisibleOnlyToTheirOwner() throws Exception {
        String alice = newUser();
        String bob = newUser();
        addItem(alice, "P002", 1);
        checkout(alice);

        long aliceOrder = ((Number) JsonPath.read(mvc.perform(get("/api/v1/orders").header("Authorization", "Bearer " + alice))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andReturn().getResponse().getContentAsString(), "$[0].id")).longValue();

        mvc.perform(get("/api/v1/orders/" + aliceOrder).header("Authorization", "Bearer " + alice))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lines[0].productCode").value("P002"));

        mvc.perform(get("/api/v1/orders/" + aliceOrder).header("Authorization", "Bearer " + bob))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/orders").header("Authorization", "Bearer " + bob))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
        String guest = guestToken();
        mvc.perform(get("/api/v1/orders/" + aliceOrder).header("Authorization", "Bearer " + guest))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/orders/" + aliceOrder)).andExpect(status().isUnauthorized());
    }

    @Test
    void historyShowsTheSnapshotNewestFirst() throws Exception {
        String product = newProduct("5.00");
        String guest = guestToken();
        addItem(guest, product, 2);
        checkout(guest);
        when(timeService.now()).thenReturn(NOW.plusSeconds(60));
        addItem(guest, "P001", 1);
        checkout(guest);

        Product p = productRepository.findByCode(product).orElseThrow();
        p.setBasePrice(new BigDecimal("50.00"));
        productRepository.save(p);

        mvc.perform(get("/api/v1/orders").header("Authorization", "Bearer " + guest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].lines[0].productCode").value("P001"))
                .andExpect(jsonPath("$[1].lines[0].productCode").value(product))
                .andExpect(jsonPath("$[1].lines[0].unitPrice").value(5.00))
                .andExpect(jsonPath("$[1].finalPrice").value(10.00))
                .andExpect(jsonPath("$[1].appliedStrategies").isEmpty());
    }

    // ---------------------------------------------------------------- helpers

    private void checkout(String token) throws Exception {
        mvc.perform(post("/api/v1/checkout").header("Idempotency-Key", UUID.randomUUID().toString()).header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated());
    }

    private ResultActions checkout(String token, String idempotencyKey, String couponCode) throws Exception {
        var request = post("/api/v1/checkout").header("Authorization", "Bearer " + token)
                .header("Idempotency-Key", idempotencyKey);
        return mvc.perform(couponCode == null ? request : request.param("couponCode", couponCode));
    }

    private String newCoupon(int usageLimit) {
        String code = "I" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        couponRepository.save(new Coupon(code, "test", new BigDecimal("0.1000"), usageLimit,
                Instant.parse("2030-01-01T00:00:00Z")));
        return code;
    }

    private int usage(String code) {
        return couponRepository.findAll().stream()
                .filter(c -> c.getCode().equals(code))
                .findFirst().orElseThrow().getCurrentUsage();
    }

    private String newProduct(String price) {
        String code = "O" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        productRepository.save(new Product(null, code, new BigDecimal(price)));
        return code;
    }

    private String guestToken() throws Exception {
        return JsonPath.read(mvc.perform(post("/api/v1/auth/guest"))
                .andReturn().getResponse().getContentAsString(), "$.token");
    }

    private String newUser() throws Exception {
        String credentials = "{\"username\":\"or" + UUID.randomUUID().toString().substring(0, 8)
                + "\",\"password\":\"secret123\"}";
        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content(credentials))
                .andExpect(status().isCreated());
        return JsonPath.read(mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(credentials)).andReturn().getResponse().getContentAsString(), "$.token");
    }

    private String anonymousIdOf(String guestToken) {
        return jwtUtil.extractVerifiedAnonymousId(guestToken);
    }

    private void addItem(String token, String productCode, int quantity) throws Exception {
        mvc.perform(post("/api/v1/cart/items").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productCode\":\"" + productCode + "\",\"quantity\":" + quantity + "}"))
                .andExpect(status().isOk());
    }
}
