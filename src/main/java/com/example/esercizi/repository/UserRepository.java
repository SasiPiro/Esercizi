package com.example.esercizi.repository;

import com.example.esercizi.model.User;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    @Query("""
        SELECT DISTINCT u 
        FROM User u 
        LEFT JOIN FETCH u.roles r 
        WHERE u.username = :username
        """)
    Optional<User> findByUsernameWithRoles(@Param("username") String username);

    boolean existsByUsername(@NotBlank(message = "Required Username") @Size(min = 3, max = 50) String username);

}
