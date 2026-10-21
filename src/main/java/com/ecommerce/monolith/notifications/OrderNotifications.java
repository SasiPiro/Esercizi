package com.ecommerce.monolith.notifications;

import com.ecommerce.monolith.ordering.OrderPlaced;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

/**
 * Reacts to placed orders without the ordering module knowing it exists. Today it only
 * logs; an e-mail or a push notification would go here.
 *
 * <p>
 * {@code @ApplicationModuleListener}: runs asynchronously, after the checkout transaction
 * commits, in a transaction of its own. A slow or failing notification never slows down or
 * rolls back a purchase, and a failed one stays in the event publication registry as
 * incomplete, so it can be resubmitted.
 * </p>
 */
@Component
public class OrderNotifications {

    private static final Logger log = LoggerFactory.getLogger(OrderNotifications.class);

    @ApplicationModuleListener
    public void on(OrderPlaced event) {
        log.info("[ORDER_NOTIFICATION] orderId={} userId={} total={}", event.orderId(), event.userId(), event.total());
    }
}
