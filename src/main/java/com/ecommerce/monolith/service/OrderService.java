package com.ecommerce.monolith.service;

import com.ecommerce.monolith.dto.OrderDTO;
import com.ecommerce.monolith.exception.OrderNotFoundException;
import com.ecommerce.monolith.facade.AuthFacade;
import com.ecommerce.monolith.mapper.OrderMapper;
import com.ecommerce.monolith.model.Order;
import com.ecommerce.monolith.model.User;
import com.ecommerce.monolith.repository.OrderRepository;
import com.ecommerce.monolith.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.ecommerce.monolith.constants.AppConstants.ERROR_ORDER_NOT_FOUND;

/**
 * Order history of the authenticated principal (user or guest). Every lookup is scoped to
 * the caller: another principal's order is reported as not found, not as forbidden, so
 * order ids cannot be probed.
 */
@Service
@Transactional(readOnly = true)
public class OrderService {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final AuthFacade authFacade;
    private final OrderMapper orderMapper;

    public OrderService(OrderRepository orderRepository, UserRepository userRepository,
            AuthFacade authFacade, OrderMapper orderMapper) {
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
        this.authFacade = authFacade;
        this.orderMapper = orderMapper;
    }

    /** The caller's orders, newest first. */
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
