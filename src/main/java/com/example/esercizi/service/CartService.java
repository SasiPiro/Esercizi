package com.example.esercizi.service;

import com.example.esercizi.dto.CartDTO;
import com.example.esercizi.exception.MissingResourceException;
import com.example.esercizi.mapper.CartMapper;
import com.example.esercizi.model.*;
import com.example.esercizi.repository.CartOwnershipRepository;
import com.example.esercizi.repository.CartRepository;
import com.example.esercizi.repository.ProductRepository;
import com.example.esercizi.exception.ProductNotFoundException;
import com.example.esercizi.exception.UserNotFoundException;
import com.example.esercizi.facade.AuthFacade;
import com.example.esercizi.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.Optional;
import static com.example.esercizi.constants.AppConstants.ERROR_USER_NOT_FOUND;

@Service
@Transactional
public class CartService {

    /** Upper bound for the quantity of a single cart line. */
    public static final int MAX_QUANTITY_PER_LINE = 99;

    private final CartRepository cartRepository;
    private final CartOwnershipRepository ownershipRepository;
    private final ProductRepository productRepository;
    private final CartMapper cartMapper;
    private final UserRepository userRepository;
    private final AuthFacade authFacade;

    public CartService(CartRepository cartRepository, CartOwnershipRepository ownershipRepository,
            ProductRepository productRepository, CartMapper cartMapper, UserRepository userRepository,
            AuthFacade authFacade) {
        this.cartRepository = cartRepository;
        this.ownershipRepository = ownershipRepository;
        this.productRepository = productRepository;
        this.cartMapper = cartMapper;
        this.userRepository = userRepository;
        this.authFacade = authFacade;
    }

    /*
     * =======================
     * GUEST FLOW
     * =======================
     */

    public CartDTO getOrCreateCart() {
        String principal = authFacade.getCurrentUsername();
        User user = userRepository.findByUsernameWithRoles(principal).orElse(null);
        String anonymousId = (user == null) ? principal : null;
        Cart cart = getCartEntity(anonymousId, user);
        return cartMapper.toDto(cart, user, anonymousId);
    }

    /** The cart of the authenticated principal (user or guest), loaded from the database. */
    public record CurrentCart(Cart cart, User user, String anonymousId) {
    }

    public CurrentCart currentCart() {
        String principal = authFacade.getCurrentUsername();
        User user = userRepository.findByUsernameWithRoles(principal).orElse(null);
        String anonymousId = (user == null) ? principal : null;
        return new CurrentCart(getCartEntity(anonymousId, user), user, anonymousId);
    }

    /**
     * Detaches a purchased cart from its owner, so the next request starts a new cart.
     * The cart row itself is kept with status PURCHASED as a record of the purchase.
     */
    public void detachPurchasedCart(Cart cart) {
        ownershipRepository.findByCartId(cart.getId()).ifPresent(ownershipRepository::delete);
    }

    public Cart getCartEntity(String anonymousId, User user) {

        // LOGGED IN USER
        if (user != null && user.getId() != null) {
            return ownershipRepository.findCartWithItemsByUserId(user.getId())
                    .orElseGet(() -> createUserCart(user));
        }
        // GUEST
        if (anonymousId != null) {
            return ownershipRepository.findCartWithItemsByAnonymousId(anonymousId)
                    .orElseGet(() -> createGuestCart(anonymousId));
        }
        throw new MissingResourceException("User or AnonymousId must be provided");
    }

    private Cart createGuestCart(String anonymousId) {
        Cart cart = cartRepository.save(new Cart());

        CartOwnership ownership = new CartOwnership();
        ownership.setCart(cart);
        ownership.setAnonymousId(anonymousId);
        ownershipRepository.save(ownership);

        return cart;
    }

    /*
     * =======================
     * LOGIN / REGISTER FLOW (MERGE)
     * =======================
     */

    public CartDTO mergeGuestCartIntoUserCart(String anonymousId) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        User user = userRepository.findByUsernameWithRoles(username)
                .orElseThrow(() -> new UserNotFoundException(ERROR_USER_NOT_FOUND));

        Optional<CartOwnership> guestOwnershipOpt = ownershipRepository.findByAnonymousId(anonymousId);
        Optional<CartOwnership> userOwnershipOpt = ownershipRepository.findByUser(user);

        Cart resultCart;

        if (guestOwnershipOpt.isPresent() && userOwnershipOpt.isPresent()) {
            // Case 1: both carts exist → merge guest cart into user cart
            resultCart = mergeCarts(guestOwnershipOpt.get(), userOwnershipOpt.get());

        } else if (guestOwnershipOpt.isPresent()) {
            // Case 2: only guest has a cart → reassign it to the user
            CartOwnership guestOwnership = guestOwnershipOpt.get();
            guestOwnership.setAnonymousId(null);
            guestOwnership.setUser(user);
            guestOwnership.getCart().setCartStatus(Cart.CartStatus.ACTIVE);
            ownershipRepository.save(guestOwnership);
            resultCart = guestOwnership.getCart();

        } else if (userOwnershipOpt.isPresent()) {
            // Case 3: only user has a cart → return it
            resultCart = userOwnershipOpt.get().getCart();

        } else {
            // Case 4: no cart exists → create a new one for the user
            resultCart = createUserCart(user);
        }

        return cartMapper.toDto(resultCart, user, null);
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

        // Copy quantities instead of moving the CartItem entities: the guest cart has
        // orphanRemoval=true, so moved items would be deleted together with it.
        // addOrIncrement also merges lines for products present in both carts.
        for (CartItem item : guestCart.getItems()) {
            userCart.addOrIncrement(item.getProduct(), item.getQuantity());
        }

        // Ownership first (it references the cart), then the cart and its items
        ownershipRepository.delete(guest);
        cartRepository.delete(guestCart);

        return userCart;
    }

    /*
     * =======================
     * CART ITEMS MANAGEMENT
     * =======================
     */

    public CartDTO addItem(String productCode, int quantity) {
        String principal = authFacade.getCurrentUsername();
        User user = userRepository.findByUsernameWithRoles(principal).orElse(null);
        String anonymousId = (user == null) ? principal : null;

        Cart cart = getCartEntity(anonymousId, user);

        Product product = productRepository.findByCode(productCode)
                .orElseThrow(() -> ProductNotFoundException.forCode(productCode));

        int alreadyInCart = cart.getItems().stream()
                .filter(i -> i.getProduct().getId().equals(product.getId()))
                .mapToInt(CartItem::getQuantity)
                .sum();
        if (alreadyInCart + quantity > MAX_QUANTITY_PER_LINE) {
            throw new IllegalArgumentException(
                    "At most " + MAX_QUANTITY_PER_LINE + " units per product, already in cart: " + alreadyInCart);
        }
        cart.addOrIncrement(product, quantity);
        Cart savedCart = cartRepository.save(cart);
        return cartMapper.toDto(savedCart, user, anonymousId);
    }

    public CartDTO removeItem(String productCode) {
        String principal = (String) Objects.requireNonNull(SecurityContextHolder.getContext().getAuthentication())
                .getPrincipal();
        User user = userRepository.findByUsernameWithRoles(principal).orElse(null);
        String anonymousId = (user == null) ? principal : null;
        Cart cart = getCartEntity(anonymousId, user);

        cart.getItems().removeIf(item -> item.getProduct().getCode().equals(productCode));

        Cart savedCart = cartRepository.save(cart);
        return cartMapper.toDto(savedCart, user, anonymousId);
    }
}
