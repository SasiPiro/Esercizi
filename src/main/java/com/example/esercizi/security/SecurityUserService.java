package com.example.esercizi.security;

import com.example.esercizi.model.Role;
import com.example.esercizi.model.User;
import com.example.esercizi.repository.UserRepository;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

@Service
public class SecurityUserService implements UserDetailsService {

    private final UserRepository userRepository;

    public SecurityUserService(UserRepository userRepository) {
        super();
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {

        // Try email first, then username
        User user = userRepository.findByUsernameWithRoles(username)
                        .orElseThrow(() -> new UsernameNotFoundException( "User not found: " + username)
                );
        return buildUserDetails(user);
    }

    /**
     * Converts User entity (domain model) to UserDetails (Spring Security)
     */
    private UserDetails buildUserDetails(User user) {
        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getUsername())
                .password(user.getPassword())
                .disabled(!user.isActive())
                .authorities(getAuthorities(user))
                .build();
    }

    /**
     * Extracts all authorities (permissions + roles) from the user
     */
    private Collection<? extends GrantedAuthority> getAuthorities(User user) {
        Set<GrantedAuthority> authorities = new HashSet<>();

        // Add roles with ROLE_ prefix (for @PreAuthorize("hasRole('ADMIN')"))
        user.getRoles().stream()
                .map(Role::getName)
                .forEach(roleName ->
                        authorities.add(new SimpleGrantedAuthority("ROLE_" + roleName))
                );
        return authorities;
    }

}
