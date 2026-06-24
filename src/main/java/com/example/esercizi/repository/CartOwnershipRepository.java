package com.example.esercizi.repository;

import com.example.esercizi.model.CartOwnership;
import com.example.esercizi.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface CartOwnershipRepository extends JpaRepository<CartOwnership, Long> {

    Optional<CartOwnership> findByAnonymousId(String anonymousId);
    Optional<CartOwnership> findByUser(User user);
    List<CartOwnership> findByAnonymousIdIsNotNullAndCreatedAtBefore(LocalDateTime expiryDate);
    Optional<CartOwnership> findByCartId(Long cartId);
}
