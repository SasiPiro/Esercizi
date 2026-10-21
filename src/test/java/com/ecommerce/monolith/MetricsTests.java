package com.ecommerce.monolith;

import com.ecommerce.monolith.pricing.Coupon;
import com.ecommerce.monolith.pricing.CouponService;
import com.ecommerce.monolith.pricing.internal.CouponRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.micrometer.metrics.test.autoconfigure.AutoConfigureMetrics;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The Prometheus endpoint exposes HTTP latency histograms and the business metrics, and is
 * reachable by the scraper without a token.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("dev")
@AutoConfigureMockMvc
@AutoConfigureMetrics // metrics export is off in tests unless asked for
class MetricsTests {

    @Autowired MockMvc mvc;
    @Autowired CouponRepository couponRepository;
    @Autowired CouponService couponService;
    @Autowired TransactionTemplate tx;

    @Test
    void prometheusExposesHttpHistogramsAndBusinessMetrics() throws Exception {
        String coupon = newCoupon();
        String guest = JsonPath.read(mvc.perform(post("/api/v1/auth/guest"))
                .andReturn().getResponse().getContentAsString(), "$.token");
        mvc.perform(post("/api/v1/cart/items").header("Authorization", "Bearer " + guest)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"productCode\":\"P001\",\"quantity\":2}"))
                .andExpect(status().isOk());
        mvc.perform(post("/api/v1/checkout").param("couponCode", coupon)
                        .header("Authorization", "Bearer " + guest)
                        .header("Idempotency-Key", UUID.randomUUID().toString()))
                .andExpect(status().isCreated());

        String scrape = prometheus();
        assertThat(scrape).contains("http_server_requests_seconds_bucket{");
        assertThat(scrape).containsPattern("shop_coupon_redeemed_total\\{code=\"" + coupon + "\"} 1\\.0");
        assertThat(scrape).containsPattern("shop_checkout_amount_count\\{outcome=\"success\"} \\d+");
        assertThat(scrape).containsPattern("shop_checkout_amount_sum\\{outcome=\"success\"} \\d+");
    }

    @Test
    void aRedemptionThatRollsBackIsNotCounted() throws Exception {
        String coupon = newCoupon();

        tx.executeWithoutResult(status -> {
            couponService.redeemCoupon(coupon);
            status.setRollbackOnly(); // e.g. the checkout failed after the redemption
        });
        assertThat(prometheus()).doesNotContain("code=\"" + coupon + "\"");

        tx.executeWithoutResult(status -> couponService.redeemCoupon(coupon));
        assertThat(prometheus()).containsPattern("shop_coupon_redeemed_total\\{code=\"" + coupon + "\"} 1\\.0");
    }

    @Test
    void healthIsPublic() throws Exception {
        mvc.perform(get("/actuator/health")).andExpect(status().isOk());
    }

    private String prometheus() throws Exception {
        return mvc.perform(get("/actuator/prometheus"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }

    private String newCoupon() {
        String code = "M" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        couponRepository.save(new Coupon(code, "metrics test", new BigDecimal("0.1000"), 10,
                Instant.parse("2099-01-01T00:00:00Z")));
        return code;
    }
}
