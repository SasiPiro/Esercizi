package com.ecommerce.monolith;

import com.ecommerce.monolith.notifications.OrderNotifications;
import com.ecommerce.monolith.ordering.OrderPlaced;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.modulith.events.IncompleteEventPublications;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.util.AopTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Placing an order publishes OrderPlaced; the notifications module reacts after the commit,
 * and the event publication registry keeps every delivery that did not succeed.
 */
@SpringBootTest
@ActiveProfiles("dev")
@AutoConfigureMockMvc
class OrderEventsTests {

    private static final Duration WAIT = Duration.ofSeconds(10);

    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired IncompleteEventPublications incompletePublications;

    @MockitoSpyBean OrderNotifications notifications;

    @Test
    void placingAnOrderPublishesOrderPlacedAndTheListenerRuns() throws Exception {
        long orderId = placeOrder();

        verify(notifications, timeout(WAIT.toMillis())).on(argThat(event -> isFor(event, orderId)));
        await().atMost(WAIT).until(() -> completed(orderId));
        assertThat(publications(orderId)).isEqualTo(1);
    }

    @Test
    void aFailedListenerLeavesThePublicationIncompleteUntilItIsResubmitted() throws Exception {
        // The first delivery fails (e.g. the mail server is down), later ones succeed
        doThrowOnce();
        long orderId = placeOrder(); // the checkout itself is not affected

        verify(notifications, timeout(WAIT.toMillis())).on(argThat(event -> isFor(event, orderId)));
        await().atMost(WAIT).until(() -> publications(orderId) == 1);
        await().during(Duration.ofMillis(300)).atMost(WAIT).until(() -> !completed(orderId));

        incompletePublications.resubmitIncompletePublications(
                publication -> publication.getEvent() instanceof OrderPlaced event && isFor(event, orderId));

        verify(notifications, timeout(WAIT.toMillis()).times(2)).on(argThat(event -> isFor(event, orderId)));
        await().atMost(WAIT).until(() -> completed(orderId));
    }

    // ---------------------------------------------------------------- helpers

    private void doThrowOnce() {
        // Stub the spy itself, not the async proxy around it: a call through the proxy runs
        // the listener machinery instead of recording the stubbing
        OrderNotifications spy = AopTestUtils.getUltimateTargetObject(notifications);
        doThrow(new IllegalStateException("notification channel unavailable"))
                .doCallRealMethod()
                .when(spy).on(any());
    }

    private long placeOrder() throws Exception {
        String guest = JsonPath.read(mvc.perform(post("/api/v1/auth/guest"))
                .andReturn().getResponse().getContentAsString(), "$.token");
        mvc.perform(post("/api/v1/cart/items").header("Authorization", "Bearer " + guest)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"productCode\":\"P001\",\"quantity\":1}"))
                .andExpect(status().isOk());
        String body = mvc.perform(post("/api/v1/checkout").header("Authorization", "Bearer " + guest)
                        .header("Idempotency-Key", UUID.randomUUID().toString()))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }

    private static boolean isFor(OrderPlaced event, long orderId) {
        return event != null && event.orderId() == orderId;
    }

    /** Publications of this order's event to the notifications listener. */
    private int publications(long orderId) {
        return jdbc.queryForObject("""
                SELECT COUNT(*) FROM event_publication
                 WHERE event_type = ? AND listener_id LIKE '%OrderNotifications%'
                   AND serialized_event LIKE ?
                """, Integer.class, OrderPlaced.class.getName(), "%\"orderId\":" + orderId + ",%");
    }

    private boolean completed(long orderId) {
        return jdbc.queryForObject("""
                SELECT COUNT(*) FROM event_publication
                 WHERE event_type = ? AND listener_id LIKE '%OrderNotifications%'
                   AND serialized_event LIKE ? AND completion_date IS NOT NULL
                """, Integer.class, OrderPlaced.class.getName(), "%\"orderId\":" + orderId + ",%") == 1;
    }
}
