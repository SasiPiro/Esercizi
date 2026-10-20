package com.ecommerce.monolith.controller;

import com.ecommerce.monolith.dto.CalculationResultDTO;
import com.ecommerce.monolith.service.PaymentService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("api/v1/checkout")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    /** Preview of the total for the current cart. Idempotent: does not consume the coupon. */
    @GetMapping("/quote")
    public CalculationResultDTO quote(@RequestParam(required = false) String couponCode) {
        return paymentService.quote(couponCode);
    }

    /** Confirms the purchase of the current cart. The request has no body: items and prices come from the server. */
    @PostMapping
    public CalculationResultDTO checkout(@RequestParam(required = false) String couponCode) {
        return paymentService.placeOrder(couponCode);
    }
}
