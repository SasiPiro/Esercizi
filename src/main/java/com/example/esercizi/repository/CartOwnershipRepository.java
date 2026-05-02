package com.example.esercizi.repository;

import com.example.esercizi.model.CartOwnership;
import com.example.esercizi.model.Session;
import com.example.esercizi.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CartOwnershipRepository extends JpaRepository<CartOwnership, Long> {

    Optional<CartOwnership> findBySession(Session session);
    Optional<CartOwnership> findByUser(User user);
}
