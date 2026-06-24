package com.example.esercizi.controller;

import com.example.esercizi.dto.CalculationResultDTO;
import com.example.esercizi.dto.CartDTO;
import com.example.esercizi.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("api/v1/checkout")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping
    public CalculationResultDTO checkout(@Valid @RequestBody CartDTO cartDTO,
            @RequestParam(required = false) String couponCode) {
        return paymentService.calculateTotal(cartDTO, couponCode);
    }
}
