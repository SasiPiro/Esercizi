package com.ecommerce.monolith;

import com.ecommerce.monolith.model.Order;
import com.ecommerce.monolith.model.OrderLine;
import com.ecommerce.monolith.model.Product;
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

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

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
        mvc.perform(post("/api/v1/checkout").header("Authorization", "Bearer " + guest))
                .andExpect(status().isBadRequest()); // empty cart

        assertThat(orderRepository.findByAnonymousIdOrderByPlacedAtDescIdDesc(anonymousIdOf(guest))).isEmpty();
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
        mvc.perform(post("/api/v1/checkout").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
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
