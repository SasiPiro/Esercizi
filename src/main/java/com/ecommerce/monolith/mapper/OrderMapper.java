package com.ecommerce.monolith.mapper;

import com.ecommerce.monolith.dto.OrderDTO;
import com.ecommerce.monolith.dto.OrderLineDTO;
import com.ecommerce.monolith.model.Order;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
public class OrderMapper {

    public OrderDTO toDto(Order order) {
        List<OrderLineDTO> lines = order.getLines().stream()
                .map(line -> new OrderLineDTO(line.getProductCode(), line.getUnitPrice(), line.getQuantity()))
                .toList();
        List<String> strategies = order.getAppliedStrategies().isEmpty()
                ? List.of()
                : Arrays.asList(order.getAppliedStrategies().split(","));

        return new OrderDTO(
                order.getId(),
                order.getPlacedAt(),
                lines,
                order.getInitialPrice(),
                order.getFinalPrice(),
                order.getInitialPrice().subtract(order.getFinalPrice()),
                strategies,
                order.getCouponCode());
    }
}
