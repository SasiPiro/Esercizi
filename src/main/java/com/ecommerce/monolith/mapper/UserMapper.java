package com.ecommerce.monolith.mapper;

import com.ecommerce.monolith.dto.UserResponseDTO;
import com.ecommerce.monolith.model.Role;
import com.ecommerce.monolith.model.User;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class UserMapper {


    /**
     * Converte User entity → UserResponseDTO
     */
    public UserResponseDTO toDTO(User user) {
        if (user == null) return null;
        return new UserResponseDTO(
                user.getId(),
                user.getUsername(),
                user.isActive(),
                extractRoleNames(user.getRoles()));
    }

    public static Set<String> extractRoleNames(Set<Role> roles) {
        if (roles == null || roles.isEmpty()) {
            return Collections.emptySet();
        }
        return roles.stream()
                .map(Role::getName)
                .collect(Collectors.toSet());
    }
}