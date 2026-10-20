package com.ecommerce.monolith.controller;

import com.ecommerce.monolith.dto.LoginRequestDTO;
import com.ecommerce.monolith.dto.LoginResponseDTO;
import com.ecommerce.monolith.dto.RegisterRequestDTO;
import com.ecommerce.monolith.dto.UserResponseDTO;
import com.ecommerce.monolith.security.JwtUtil;
import com.ecommerce.monolith.service.AuthService;
import com.ecommerce.monolith.service.CartService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("api/v1/auth")
public class AuthController {

    private final AuthService authService;
    private final JwtUtil jwtUtil;
    private final CartService cartService;

    public AuthController(AuthService authService, JwtUtil jwtUtil, CartService cartService) {
        this.authService = authService;
        this.jwtUtil = jwtUtil;
        this.cartService = cartService;
    }

    /**
     * Issues a signed token for an anonymous shopper, so guests can use the cart
     * before registering. The anonymousId inside the token identifies the guest cart.
     */
    @PostMapping("/guest")
    public ResponseEntity<Map<String, Object>> guest() {
        String anonymousId = UUID.randomUUID().toString();
        cartService.createGuestCart(anonymousId);
        String token = jwtUtil.generateAnonymousToken(anonymousId);
        return ResponseEntity.ok(Map.of("token", token, "type", "Bearer", "expiresIn", jwtUtil.getExpirationTime()));
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> login(@Valid @RequestBody LoginRequestDTO request) throws Exception{
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/register")
    public ResponseEntity<UserResponseDTO> register(@Valid @RequestBody RegisterRequestDTO request) throws Exception{
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }
}
