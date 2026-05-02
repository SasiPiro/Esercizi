package com.example.esercizi.controller;

import com.example.esercizi.dto.CalculationResultDto;
import com.example.esercizi.model.Session;
import com.example.esercizi.model.User;
import com.example.esercizi.service.PaymentService;
import com.example.esercizi.service.SessionService;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletResponse;
import java.math.BigDecimal;

@RestController
@RequestMapping("/checkout")
public class PaymentController {

    private final PaymentService paymentService;
    private final SessionService sessionService;

    public PaymentController(PaymentService paymentService, SessionService sessionService) {
        this.paymentService = paymentService;
        this.sessionService = sessionService;
    }

    @PostMapping
    public CalculationResultDto checkout(@RequestHeader(value = "X-SESSION-ID", required = false) String sessionId, User user,
                                         @RequestParam(required = false) String couponCode,
                                         HttpServletResponse response) {

        Session session = sessionService.getOrCreate(sessionId);
        response.setHeader("X-SESSION-ID", session.getId());

        return paymentService.calculateTotal(session, user, couponCode);
    }
}
