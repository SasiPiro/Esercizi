package com.ecommerce.monolith.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConfigurationPropertiesBinding;
import org.springframework.context.annotation.Bean;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.MonthDay;
import java.time.ZoneId;

/**
 * Promotion percentages and seasonal windows, from {@code promotions} in application.yaml.
 *
 * <p>
 * Windows are days of the year ({@code MM-dd}, both ends included), so they repeat every
 * year without a release; moving a window (e.g. Black Friday, which changes date every
 * year) is a configuration change.
 * </p>
 */
@Component
@ConfigurationProperties(prefix = "promotions")
public class PromotionConfig {

    private BigDecimal vipDiscount;
    private ZoneId zone = ZoneId.of("UTC");
    private SeasonalPromotion blackFriday = new SeasonalPromotion();
    private SeasonalPromotion christmas = new SeasonalPromotion();

    public BigDecimal getVipDiscount() {
        return vipDiscount;
    }

    public void setVipDiscount(BigDecimal vipDiscount) {
        this.vipDiscount = vipDiscount;
    }

    /** Time zone in which a window's days start and end. */
    public ZoneId getZone() {
        return zone;
    }

    public void setZone(ZoneId zone) {
        this.zone = zone;
    }

    public SeasonalPromotion getBlackFriday() {
        return blackFriday;
    }

    public void setBlackFriday(SeasonalPromotion blackFriday) {
        this.blackFriday = blackFriday;
    }

    public SeasonalPromotion getChristmas() {
        return christmas;
    }

    public void setChristmas(SeasonalPromotion christmas) {
        this.christmas = christmas;
    }

    /** True when {@code now} falls inside the window of {@code promotion}. */
    public boolean isActive(SeasonalPromotion promotion, Instant now) {
        return promotion.contains(MonthDay.from(now.atZone(zone)));
    }

    public static class SeasonalPromotion {
        private BigDecimal discount;
        private MonthDay start;
        private MonthDay end;

        public BigDecimal getDiscount() {
            return discount;
        }

        public void setDiscount(BigDecimal discount) {
            this.discount = discount;
        }

        public MonthDay getStart() {
            return start;
        }

        public void setStart(MonthDay start) {
            this.start = start;
        }

        public MonthDay getEnd() {
            return end;
        }

        public void setEnd(MonthDay end) {
            this.end = end;
        }

        /** Both ends included; a window may cross the new year (e.g. 12-28 to 01-03). */
        boolean contains(MonthDay day) {
            if (start == null || end == null) {
                return false;
            }
            if (!start.isAfter(end)) {
                return !day.isBefore(start) && !day.isAfter(end);
            }
            return !day.isBefore(start) || !day.isAfter(end);
        }
    }

    /**
     * Binds {@code "11-25"} (or the ISO form {@code "--11-25"}) to a MonthDay. An anonymous
     * class, not a lambda: the binder needs the converter's generic types.
     */
    @Bean
    @ConfigurationPropertiesBinding
    static Converter<String, MonthDay> monthDayConverter() {
        return new Converter<>() {
            @Override
            public MonthDay convert(String value) {
                return MonthDay.parse(value.startsWith("--") ? value : "--" + value);
            }
        };
    }
}
