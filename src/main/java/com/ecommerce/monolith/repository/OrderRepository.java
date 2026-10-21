package com.ecommerce.monolith.repository;

import com.ecommerce.monolith.model.Order;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    @EntityGraph(attributePaths = "lines")
    Optional<Order> findWithLinesById(Long id);

    @EntityGraph(attributePaths = "lines")
    List<Order> findByUserIdOrderByPlacedAtDescIdDesc(Long userId);

    @EntityGraph(attributePaths = "lines")
    List<Order> findByAnonymousIdOrderByPlacedAtDescIdDesc(String anonymousId);
}
