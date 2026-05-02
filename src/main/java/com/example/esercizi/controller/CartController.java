package com.example.esercizi.controller;

import com.example.esercizi.dto.CartDto;
import com.example.esercizi.model.Session;
import com.example.esercizi.model.User;
import com.example.esercizi.service.CartService;
import com.example.esercizi.service.SessionService;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletResponse;

@RestController
@RequestMapping("/cart")
public class CartController {

    private final CartService cartService;
    private final SessionService sessionService;

    public CartController(CartService cartService, SessionService sessionService) {
        this.cartService = cartService;
        this.sessionService = sessionService;
    }

    @GetMapping
    public CartDto getCart(@RequestHeader(value = "X-SESSION-ID", required = false) String sessionId, User user,
            HttpServletResponse response) {

        Session session = sessionService.getOrCreate(sessionId);
        response.setHeader("X-SESSION-ID", session.getId());

        return cartService.getOrCreateCart(session, user);
    }

    @PostMapping("/add")
    public CartDto addItem(@RequestHeader(value = "X-SESSION-ID", required = false) String sessionId, User user,
            @RequestParam String productCode,
            @RequestParam int quantity,
            HttpServletResponse response) {

        Session session = sessionService.getOrCreate(sessionId);
        response.setHeader("X-SESSION-ID", session.getId());

        return cartService.addItem(session, user, productCode, quantity);
    }

    @PostMapping("/remove")
    public CartDto removeItem(@RequestHeader(value = "X-SESSION-ID", required = false) String sessionId, User user,
            @RequestParam String productCode,
            HttpServletResponse response) {

        Session session = sessionService.getOrCreate(sessionId);
        response.setHeader("X-SESSION-ID", session.getId());

        return cartService.removeItem(session, user, productCode);
    }
}
