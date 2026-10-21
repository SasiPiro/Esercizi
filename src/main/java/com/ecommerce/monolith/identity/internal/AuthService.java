package com.ecommerce.monolith.identity.internal;

import com.ecommerce.monolith.identity.GuestSessionStarted;
import com.ecommerce.monolith.identity.Role;
import com.ecommerce.monolith.identity.User;
import com.ecommerce.monolith.identity.UserNotFoundException;
import com.ecommerce.monolith.identity.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class AuthService {

        private static final Logger log = LoggerFactory.getLogger(AuthService.class);

        private final UserRepository userRepository;
        private final RoleRepository roleRepository;
        private final PasswordEncoder passwordEncoder;
        private final AuthenticationManager authenticationManager;
        private final JwtUtil jwtUtil;
        private final UserMapper userMapper;
        private final ApplicationEventPublisher events;

        public AuthService(
                        UserRepository userRepository,
                        RoleRepository roleRepository,
                        PasswordEncoder passwordEncoder,
                        AuthenticationManager authenticationManager,
                        JwtUtil jwtUtil,
                        UserMapper userMapper,
                        ApplicationEventPublisher events) {
                this.userRepository = userRepository;
                this.roleRepository = roleRepository;
                this.passwordEncoder = passwordEncoder;
                this.authenticationManager = authenticationManager;
                this.jwtUtil = jwtUtil;
                this.userMapper = userMapper;
                this.events = events;
        }

        /**
         * Starts a session for a new anonymous shopper and returns its signed guest token.
         * The anonymousId inside the token identifies the guest; GuestSessionStarted listeners
         * (the guest cart) run in this transaction, before the token is returned.
         */
        @Transactional
        public String startGuestSession() {
                String anonymousId = UUID.randomUUID().toString();
                events.publishEvent(new GuestSessionStarted(anonymousId));
                return jwtUtil.generateAnonymousToken(anonymousId);
        }

        /**
         * Authenticate user using Spring Security and generate JWT token.
         *
         * <p>
         * No transaction around the whole method: BCrypt verification is deliberately slow,
         * and a transaction would hold a pooled connection for its whole duration. The user
         * lookups (in SecurityUserService and below) run in their own short read-only
         * transactions.
         * </p>
         */
        @Transactional(propagation = Propagation.NOT_SUPPORTED)
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
        @Transactional
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
