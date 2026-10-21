package com.ecommerce.monolith.ordering.internal;

import com.ecommerce.monolith.pricing.CalculationResultDTO;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("api/v1/checkout")
public class PaymentController {

    private final PaymentService paymentService;
    private final OrderService orderService;

    public PaymentController(PaymentService paymentService, OrderService orderService) {
        this.paymentService = paymentService;
        this.orderService = orderService;
    }

    /** Preview of the total for the current cart. Idempotent: does not consume the coupon. */
    @GetMapping("/quote")
    public CalculationResultDTO quote(@RequestParam(required = false) String couponCode) {
        return paymentService.quote(couponCode);
    }

    /**
     * Confirms the purchase of the current cart. The request has no body: items and prices come
     * from the server. The client generates one Idempotency-Key (e.g. a UUID) per checkout
     * attempt and sends the same key when it retries.
     *
     * @return 201 with the new order and its Location, or 200 with the order already placed
     *         with this key
     */
    @PostMapping
    public ResponseEntity<OrderDTO> checkout(
            @RequestHeader("Idempotency-Key") @NotBlank @Size(max = 64) String idempotencyKey,
            @RequestParam(required = false) String couponCode) {
        OrderService.PlacedOrder placed = orderService.placeOrder(idempotencyKey, couponCode);
        if (!placed.created()) {
            return ResponseEntity.ok(placed.order());
        }
        URI location = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/v1/orders/{id}")
                .buildAndExpand(placed.order().id())
                .toUri();
        return ResponseEntity.created(location).body(placed.order());
    }
}
