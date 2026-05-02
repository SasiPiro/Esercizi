package com.example.esercizi.service;

import com.example.esercizi.dto.CartDto;
import com.example.esercizi.mapper.CartMapper;
import com.example.esercizi.model.*;
import com.example.esercizi.repository.CartOwnershipRepository;
import com.example.esercizi.repository.CartRepository;
import com.example.esercizi.repository.ProductRepository;
import com.example.esercizi.exception.ProductNotFoundException;
import com.example.esercizi.exception.UserNotFoundException;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import static com.example.esercizi.constants.AppConstants.ERROR_USER_NOT_FOUND;

@Service
@Transactional
public class CartService {

    private final CartRepository cartRepository;
    private final CartOwnershipRepository ownershipRepository;
    private final ProductRepository productRepository;
    private final CartMapper cartMapper;

    public CartService(CartRepository cartRepository, CartOwnershipRepository ownershipRepository,
            ProductRepository productRepository, CartMapper cartMapper) {
        this.cartRepository = cartRepository;
        this.ownershipRepository = ownershipRepository;
        this.productRepository = productRepository;
        this.cartMapper = cartMapper;
    }

    /*
     * =======================
     * GUEST FLOW
     * =======================
     */

    public CartDto getOrCreateCart(Session session, User user) {
        Cart cart = getCartEntity(session, user);
        return cartMapper.toDto(cart, user);
    }

    public Cart getCartEntity(Session session, User user) {

        // USER LOGGATO
        if (user != null) {
            return ownershipRepository.findByUser(user)
                    .map(CartOwnership::getCart)
                    .orElseGet(() -> createUserCart(user));
        }
        // GUEST
        return ownershipRepository.findBySession(session)
                .map(CartOwnership::getCart)
                .orElseGet(() -> createGuestCart(session));
    }

    private Cart createGuestCart(Session session) {
        Cart cart = cartRepository.save(new Cart());

        CartOwnership ownership = new CartOwnership();
        ownership.setCart(cart);
        ownership.setSession(session);
        ownershipRepository.save(ownership);

        return cart;
    }

    /*
     * =======================
     * LOGIN / REGISTER FLOW
     * =======================
     */

    public Cart assignCartToUser(Session session, User user) {

        if (user == null) {
            throw new UserNotFoundException(ERROR_USER_NOT_FOUND);
        }

        Optional<CartOwnership> guestOwnership = ownershipRepository.findBySession(session);

        Optional<CartOwnership> userOwnership = ownershipRepository.findByUser(user);

        if (guestOwnership.isPresent() && userOwnership.isPresent()) {
            return mergeCarts(
                    guestOwnership.get(),
                    userOwnership.get());
        }

        if (guestOwnership.isPresent()) {
            CartOwnership ownership = guestOwnership.get();
            ownership.setUser(user);
            ownership.setSession(null);
            return ownership.getCart();
        }

        return userOwnership
                .map(CartOwnership::getCart)
                .orElseGet(() -> createUserCart(user));
    }

    private Cart createUserCart(User user) {
        Cart cart = new Cart();
        cart.setCartStatus(Cart.CartStatus.ACTIVE);

        cartRepository.save(cart);

        CartOwnership ownership = new CartOwnership();
        ownership.setCart(cart);
        ownership.setUser(user);

        ownershipRepository.save(ownership);
        return cart;
    }

    /*
     * =======================
     * MERGE
     * =======================
     */

    private Cart mergeCarts(CartOwnership guest, CartOwnership user) {
        Cart guestCart = guest.getCart();
        Cart userCart = user.getCart();

        for (CartItem item : guestCart.getItems()) {
            item.setCart(userCart);
            userCart.getItems().add(item);
        }
        guestCart.getItems().clear();

        cartRepository.delete(guestCart);
        ownershipRepository.delete(guest);

        return userCart;
    }

    /*
     * =======================
     * CART ITEMS MANAGEMENT
     * =======================
     */

    public CartDto addItem(Session session, User user, String productCode, int quantity) {
        Cart cart = getCartEntity(session, user);

        Product product = productRepository.findByCode(productCode)
                .orElseThrow(() -> ProductNotFoundException.forCode(productCode));

        CartItem existing = cart.getItems().stream()
                .filter(i -> i.getProduct().equals(product))
                .findFirst()
                .orElse(null);

        if (existing != null) {
            existing.setQuantity(existing.getQuantity() + quantity);
        } else {
            CartItem item = new CartItem();
            item.setCart(cart);
            item.setProduct(product);
            item.setQuantity(quantity);
            cart.getItems().add(item);
        }
        Cart savedCart = cartRepository.save(cart);
        return cartMapper.toDto(savedCart, user);
    }

    public CartDto removeItem(Session session, User user, String productCode) {
        Cart cart = getCartEntity(session, user);

        cart.getItems().removeIf(item -> item.getProduct().getCode().equals(productCode));

        Cart savedCart = cartRepository.save(cart);
        return cartMapper.toDto(savedCart, user);
    }
}
