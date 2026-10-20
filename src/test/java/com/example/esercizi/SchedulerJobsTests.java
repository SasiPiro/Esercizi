package com.example.esercizi;

import com.example.esercizi.model.Coupon;
import com.example.esercizi.model.Coupon.CouponStatus;
import com.example.esercizi.repository.CouponRepository;
import com.example.esercizi.scheduler.CartCleanupJob;
import com.example.esercizi.scheduler.PromotionExpiryJob;
import com.example.esercizi.service.TimeService;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Automatic triggers off: the test invokes the jobs itself, with a fixed clock
@SpringBootTest(properties = "app.scheduling.enabled=false")
@AutoConfigureMockMvc
class SchedulerJobsTests {

    private static final Instant NOW = Instant.parse("2026-10-20T12:00:00Z");

    @Autowired MockMvc mvc;
    @Autowired CouponRepository couponRepository;
    @Autowired PromotionExpiryJob promotionExpiryJob;
    @Autowired CartCleanupJob cartCleanupJob;
    @Autowired JdbcTemplate jdbc;

    @MockitoBean TimeService timeService;

    @BeforeEach
    void fixClock() {
        when(timeService.now()).thenReturn(NOW);
    }

    @Test
    void expiresCouponsPastTheirDateOrUsageLimitOnlyAndRunsOncePerLockWindow() {
        String pastDate = coupon(1, 0, NOW.minus(Duration.ofDays(1)));
        String usedUp = coupon(1, 1, NOW.plus(Duration.ofDays(30)));
        String stillValid = coupon(5, 1, NOW.plus(Duration.ofDays(30)));

        promotionExpiryJob.expireStalePromotions();

        assertThat(couponStatus(pastDate)).isEqualTo(CouponStatus.EXPIRED);
        assertThat(couponStatus(usedUp)).isEqualTo(CouponStatus.EXPIRED);
        assertThat(couponStatus(stillValid)).isEqualTo(CouponStatus.ACTIVE);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM shedlock WHERE name = 'expireStalePromotions'",
                Integer.class)).isEqualTo(1);

        // A second trigger inside lockAtLeastFor (another replica, or the same one) is skipped
        String staleAfterFirstRun = coupon(1, 0, NOW.minus(Duration.ofDays(1)));
        promotionExpiryJob.expireStalePromotions();
        assertThat(couponStatus(staleAfterFirstRun)).isEqualTo(CouponStatus.ACTIVE);
    }

    @Test
    void deletesOnlyGuestCartsOlderThanTheRetention() throws Exception {
        long oldGuestCart = guestCartWithOneItem();
        long recentGuestCart = guestCartWithOneItem();
        jdbc.update("UPDATE cart_ownership SET created_at = ? WHERE cart_id = ?",
                java.sql.Timestamp.from(NOW.minus(Duration.ofDays(31))), oldGuestCart);

        cartCleanupJob.cleanupExpiredGuestCarts();

        assertThat(rows("cart", "id", oldGuestCart)).isZero();
        assertThat(rows("cart_ownership", "cart_id", oldGuestCart)).isZero();
        assertThat(rows("cart_item", "cart_id", oldGuestCart)).isZero();
        assertThat(rows("cart", "id", recentGuestCart)).isEqualTo(1);
        assertThat(rows("cart", "id", 1L)).as("seed user cart is never touched").isEqualTo(1);
    }

    private long guestCartWithOneItem() throws Exception {
        String token = JsonPath.read(mvc.perform(post("/api/v1/auth/guest"))
                .andReturn().getResponse().getContentAsString(), "$.token");
        mvc.perform(post("/api/v1/cart/add").param("productCode", "P001").param("quantity", "1")
                .header("Authorization", "Bearer " + token)).andExpect(status().isOk());
        Number id = JsonPath.read(mvc.perform(get("/api/v1/cart").header("Authorization", "Bearer " + token))
                .andReturn().getResponse().getContentAsString(), "$.id");
        return id.longValue();
    }

    private int rows(String table, String column, long id) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM " + table + " WHERE " + column + " = ?", Integer.class, id);
    }

    private String coupon(int usageLimit, int currentUsage, Instant expiry) {
        String code = "J" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        Coupon c = new Coupon(code, "job test", new BigDecimal("0.1000"), usageLimit, expiry);
        c.setCurrentUsage(currentUsage);
        couponRepository.save(c);
        return code;
    }

    private CouponStatus couponStatus(String code) {
        return couponRepository.findAll().stream()
                .filter(c -> c.getCode().equals(code)).findFirst().orElseThrow().getStatus();
    }
}
