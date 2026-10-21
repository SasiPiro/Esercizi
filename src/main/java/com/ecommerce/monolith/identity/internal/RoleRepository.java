package com.ecommerce.monolith.identity.internal;

import com.ecommerce.monolith.identity.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.Set;

public interface RoleRepository extends JpaRepository<Role, Long> {

    /**
     * Finds role by name
     */
    Optional<Role> findByName(String name);

    /**
     * Checks if a role exists by name
     */
    boolean existsByName(String name);

    /**
     * Finds roles by a set of names (useful for batch operations)
     */
    Set<Role> findByNameIn(Set<String> names);
}
