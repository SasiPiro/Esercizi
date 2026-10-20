package com.ecommerce.monolith.repository;

import com.ecommerce.monolith.model.User;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
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

    /**
     * SELECT ... FOR UPDATE on the user row. Used to serialize the creation of a
     * user's cart: concurrent first requests wait here instead of racing to insert.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT u FROM User u WHERE u.id = :id")
    Optional<User> lockById(@Param("id") Long id);

    boolean existsByUsername(@NotBlank(message = "Required Username") @Size(min = 3, max = 50) String username);

}
