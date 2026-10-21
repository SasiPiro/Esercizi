package com.ecommerce.monolith;

import com.ecommerce.monolith.identity.User;
import com.ecommerce.monolith.pricing.internal.BlackFridayDiscount;
import com.ecommerce.monolith.pricing.internal.ChristmasDiscount;
import com.ecommerce.monolith.pricing.internal.PromotionConfig;
import com.ecommerce.monolith.pricing.internal.PromotionContext;
import com.ecommerce.monolith.pricing.internal.VipExclusiveDiscount;
import com.ecommerce.monolith.shared.TimeService;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.MonthDay;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Seasonal windows come from configuration (promotions.* in application.yaml) and repeat
 * every year: nothing in the code is tied to 2026.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("dev")
@AutoConfigureMockMvc
class PromotionStrategyTests {

    @Autowired PromotionConfig config;
    @Autowired BlackFridayDiscount blackFriday;
    @Autowired ChristmasDiscount christmas;
    @Autowired VipExclusiveDiscount vip;
    @Autowired MockMvc mvc;

    @MockitoBean TimeService timeService;

    @Test
    void windowsAreBoundFromConfiguration() {
        assertThat(config.getBlackFriday().getStart()).isEqualTo(MonthDay.of(11, 25));
        assertThat(config.getBlackFriday().getEnd()).isEqualTo(MonthDay.of(11, 27));
        assertThat(config.getBlackFriday().getDiscount()).isEqualByComparingTo("0.15");
        assertThat(config.getChristmas().getStart()).isEqualTo(MonthDay.of(12, 20));
        assertThat(config.getChristmas().getEnd()).isEqualTo(MonthDay.of(12, 26));
        assertThat(config.getChristmas().getDiscount()).isEqualByComparingTo("0.20");
    }

    @Test
    void blackFridayIsActiveInsideItsWindowEveryYear() {
        assertThat(blackFriday.supports(at("2026-11-25T00:00:00Z"))).isTrue();
        assertThat(blackFriday.supports(at("2027-11-26T12:00:00Z"))).isTrue();
        assertThat(blackFriday.supports(at("2030-11-27T23:59:59Z"))).isTrue();

        assertThat(blackFriday.supports(at("2027-11-24T23:59:59Z"))).isFalse();
        assertThat(blackFriday.supports(at("2027-11-28T00:00:00Z"))).isFalse();
    }

    @Test
    void christmasIsActiveInsideItsWindowEveryYear() {
        assertThat(christmas.supports(at("2026-12-20T00:00:00Z"))).isTrue();
        assertThat(christmas.supports(at("2028-12-26T23:59:59Z"))).isTrue();

        assertThat(christmas.supports(at("2027-12-19T23:59:59Z"))).isFalse();
        assertThat(christmas.supports(at("2027-12-27T00:00:00Z"))).isFalse();
        assertThat(christmas.supports(at("2027-11-26T12:00:00Z"))).isFalse();
    }

    @Test
    void aWindowMayCrossTheNewYear() {
        PromotionConfig.SeasonalPromotion newYear = new PromotionConfig.SeasonalPromotion();
        newYear.setStart(MonthDay.of(12, 28));
        newYear.setEnd(MonthDay.of(1, 3));

        assertThat(config.isActive(newYear, Instant.parse("2027-12-31T10:00:00Z"))).isTrue();
        assertThat(config.isActive(newYear, Instant.parse("2028-01-03T10:00:00Z"))).isTrue();
        assertThat(config.isActive(newYear, Instant.parse("2028-01-04T00:00:00Z"))).isFalse();
        assertThat(config.isActive(newYear, Instant.parse("2027-12-27T23:59:59Z"))).isFalse();
    }

    @Test
    void vipDependsOnTheUserNotOnTheDate() {
        User vipUser = new User("v", "x");
        vipUser.setVip(true);
        assertThat(vip.supports(new PromotionContext(null, Instant.parse("2027-03-01T00:00:00Z"), vipUser))).isTrue();
        assertThat(vip.supports(new PromotionContext(null, Instant.parse("2027-03-01T00:00:00Z"), new User()))).isFalse();
        assertThat(vip.supports(new PromotionContext(null, Instant.parse("2027-03-01T00:00:00Z"), null))).isFalse();
    }

    @Test
    void checkoutQuoteAppliesBlackFridayIn2027() throws Exception {
        when(timeService.now()).thenReturn(Instant.parse("2027-11-26T12:00:00Z"));
        String guest = JsonPath.read(mvc.perform(post("/api/v1/auth/guest"))
                .andReturn().getResponse().getContentAsString(), "$.token");
        mvc.perform(post("/api/v1/cart/items").contentType(MediaType.APPLICATION_JSON)
                .content("{\"productCode\":\"P001\",\"quantity\":2}")
                .header("Authorization", "Bearer " + guest)).andExpect(status().isOk()); // 21.00

        mvc.perform(get("/api/v1/checkout/quote").header("Authorization", "Bearer " + guest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.appliedStrategies[0]").value("BlackFridayDiscount"))
                .andExpect(jsonPath("$.finalPrice").value(new BigDecimal("17.85")));
    }

    private static PromotionContext at(String instant) {
        return new PromotionContext(null, Instant.parse(instant), null);
    }
}
