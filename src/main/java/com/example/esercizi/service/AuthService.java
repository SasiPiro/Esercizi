package com.example.esercizi.service;

import com.example.esercizi.dto.LoginRequestDTO;
import com.example.esercizi.dto.LoginResponseDTO;
import com.example.esercizi.dto.RegisterRequestDTO;
import com.example.esercizi.dto.UserResponseDTO;
import com.example.esercizi.exception.DuplicateResourceException;
import com.example.esercizi.exception.UserNotFoundException;
import com.example.esercizi.mapper.UserMapper;
import com.example.esercizi.model.Role;
import com.example.esercizi.model.User;
import com.example.esercizi.repository.RoleRepository;
import com.example.esercizi.repository.UserRepository;
import com.example.esercizi.security.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AuthService {

        private static final Logger log = LoggerFactory.getLogger(AuthService.class);

        private final UserRepository userRepository;
        private final RoleRepository roleRepository;
        private final PasswordEncoder passwordEncoder;
        private final AuthenticationManager authenticationManager;
        private final JwtUtil jwtUtil;
        private final UserMapper userMapper;

        public AuthService(
                        UserRepository userRepository,
                        RoleRepository roleRepository,
                        PasswordEncoder passwordEncoder,
                        AuthenticationManager authenticationManager,
                        JwtUtil jwtUtil,
                        UserMapper userMapper) {
                this.userRepository = userRepository;
                this.roleRepository = roleRepository;
                this.passwordEncoder = passwordEncoder;
                this.authenticationManager = authenticationManager;
                this.jwtUtil = jwtUtil;
                this.userMapper = userMapper;
        }

        /**
         * Authenticate user using Spring Security and generate JWT token
         */
        public LoginResponseDTO login(LoginRequestDTO request) {
                // Delegate authentication to Spring Security
                Authentication authentication;
                try {
                        authentication = authenticationManager.authenticate(
                                        new UsernamePasswordAuthenticationToken(request.username(), request.password()));
                } catch (AuthenticationException ex) {
                        // Logged here because only here the attempted username is known.
                        // The exception handler turns it into a generic 401.
                        log.warn("[AUTH_LOGIN] outcome=failure username={} reason={}",
                                        request.username(), ex.getClass().getSimpleName());
                        throw ex;
                }

                String username = authentication.getName();

                // Fetch user with roles for the token and the response DTO
                User user = userRepository.findByUsernameWithRoles(username)
                                .orElseThrow(() -> new UserNotFoundException("User not found after authentication"));

                String token = jwtUtil.generateToken(user);
                Long expiresIn = jwtUtil.getExpirationTime();
                UserResponseDTO userResponse = userMapper.toDTO(user);

                log.info("[AUTH_LOGIN] outcome=success username={} userId={}", user.getUsername(), user.getId());
                return new LoginResponseDTO(token, expiresIn, userResponse);
        }

        /**
         * Register new user with default CUSTOMER role
         */
        public UserResponseDTO register(RegisterRequestDTO request) {
                // Check if username already exists
                if (userRepository.existsByUsername(request.username())) {
                        log.info("[AUTH_REGISTER] outcome=rejected username={} reason=duplicate_username",
                                        request.username());
                        throw new DuplicateResourceException("Username already exists");
                }

                // Create new user
                User user = new User();
                user.setUsername(request.username());
                user.setPassword(passwordEncoder.encode(request.password()));

                // Assign default CUSTOMER role
                Role customerRole = roleRepository.findByName("CUSTOMER")
                                .orElseThrow(() -> new UserNotFoundException(
                                                "Default CUSTOMER role not found. Please run data initialization."));

                user.addRole(customerRole);

                // Save user
                User savedUser = userRepository.save(user);

                log.info("[AUTH_REGISTER] outcome=success username={} userId={}",
                                savedUser.getUsername(), savedUser.getId());

                return userMapper.toDTO(savedUser);
        }
}
