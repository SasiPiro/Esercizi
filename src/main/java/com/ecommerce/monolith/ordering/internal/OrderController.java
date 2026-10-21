package com.ecommerce.monolith.ordering.internal;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("api/v1/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    /** The caller's orders, newest first. */
    @GetMapping
    public List<OrderDTO> list() {
        return orderService.list();
    }

    /** One of the caller's orders; 404 for an order of someone else. */
    @GetMapping("/{id}")
    public OrderDTO get(@PathVariable Long id) {
        return orderService.get(id);
    }
}
