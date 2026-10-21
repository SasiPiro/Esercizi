package com.ecommerce.monolith.ordering.internal;

import com.ecommerce.monolith.identity.AuthFacade;
import com.ecommerce.monolith.identity.User;
import com.ecommerce.monolith.identity.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import static com.ecommerce.monolith.shared.AppConstants.ERROR_ORDER_NOT_FOUND;

/**
 * Orders of the authenticated principal (user or guest): idempotent checkout and history.
 * Every lookup is scoped to the caller: another principal's order is reported as not found,
 * not as forbidden, so order ids cannot be probed.
 *
 * <p>
 * No class-level transaction: {@link #placeOrder} must run outside any transaction scope.
 * </p>
 */
@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final AuthFacade authFacade;
    private final OrderMapper orderMapper;
    private final PaymentService paymentService;

    public OrderService(OrderRepository orderRepository, UserRepository userRepository,
            AuthFacade authFacade, OrderMapper orderMapper, PaymentService paymentService) {
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
        this.authFacade = authFacade;
        this.orderMapper = orderMapper;
        this.paymentService = paymentService;
    }

    /** The order and whether this call created it ({@code false}: a replay of the same key). */
    public record PlacedOrder(OrderDTO order, boolean created) {
    }

    /**
     * Places an order for the caller's cart, at most once per idempotency key.
     *
     * <ol>
     * <li>If the caller already placed an order with this key, it is returned and nothing
     * else happens: a client that timed out and retries does not buy twice.</li>
     * <li>Otherwise the checkout runs and creates the order in one transaction.</li>
     * <li>If two requests with the same key race, only one can commit. The other fails, on the
     * unique key, on the cart claim, or because it found the fresh empty cart the winner left
     * behind; whatever the reason, it answers with the winner's order.</li>
     * </ol>
     * No transaction here: the checkout runs in its own, so after a lost race the lookup
     * sees the order the winner committed. Not even a NOT_SUPPORTED scope: inside one, the
     * lookups would share an EntityManager that keeps its connection until the method ends,
     * so every request would hold two pooled connections during the checkout, and a burst of
     * retries could exhaust the pool and wait on itself.
     *
     * @throws IdempotencyKeyConflictException if the key was used by another principal
     */
    public PlacedOrder placeOrder(String idempotencyKey, String couponCode) {
        Optional<OrderDTO> previous = findPlacedWith(idempotencyKey);
        if (previous.isPresent()) {
            return new PlacedOrder(previous.get(), false);
        }
        try {
            return new PlacedOrder(paymentService.placeOrder(idempotencyKey, couponCode), true);
        } catch (RuntimeException failure) {
            // Not a lost race when no order has this key: then the failure is the answer
            return findPlacedWith(idempotencyKey)
                    .map(winner -> new PlacedOrder(winner, false))
                    .orElseThrow(() -> failure);
        }
    }

    private Optional<OrderDTO> findPlacedWith(String idempotencyKey) {
        Owner owner = currentOwner();
        return orderRepository.findByIdempotencyKey(idempotencyKey).map(order -> {
            if (!owner.owns(order)) {
                throw new IdempotencyKeyConflictException("Idempotency key already used: generate a new one");
            }
            return orderMapper.toDto(order);
        });
    }

    /** The caller's orders, newest first. */
    @Transactional(readOnly = true)
    public List<OrderDTO> list() {
        Owner owner = currentOwner();
        List<Order> orders = owner.userId() != null
                ? orderRepository.findByUserIdOrderByPlacedAtDescIdDesc(owner.userId())
                : orderRepository.findByAnonymousIdOrderByPlacedAtDescIdDesc(owner.anonymousId());
        return orders.stream().map(orderMapper::toDto).toList();
    }

    /**
     * @throws OrderNotFoundException if the order does not exist or belongs to someone else
     */
    @Transactional(readOnly = true)
    public OrderDTO get(Long id) {
        Owner owner = currentOwner();
        return orderRepository.findWithLinesById(id)
                .filter(owner::owns)
                .map(orderMapper::toDto)
                .orElseThrow(() -> new OrderNotFoundException(ERROR_ORDER_NOT_FOUND + id));
    }

    private Owner currentOwner() {
        String principal = authFacade.getCurrentUsername();
        return userRepository.findByUsernameWithRoles(principal)
                .map(User::getId)
                .map(userId -> new Owner(userId, null))
                .orElseGet(() -> new Owner(null, principal));
    }

    /** Exactly one of the two is set: a registered user or a guest. */
    record Owner(Long userId, String anonymousId) {
        boolean owns(Order order) {
            return userId != null ? userId.equals(order.getUserId()) : anonymousId.equals(order.getAnonymousId());
        }
    }
}
