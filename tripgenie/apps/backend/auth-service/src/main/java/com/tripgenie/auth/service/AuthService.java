package com.tripgenie.auth.service;

import com.tripgenie.auth.audit.service.AuthAuditLogService;
import com.tripgenie.auth.domain.Role;
import com.tripgenie.auth.domain.User;
import com.tripgenie.auth.repository.RoleRepository;
import com.tripgenie.auth.repository.UserRepository;
import com.tripgenie.common.dto.AuthResponse;
import com.tripgenie.common.dto.LoginRequest;
import com.tripgenie.common.dto.RegisterRequest;
import com.tripgenie.common.exception.ConflictException;
import com.tripgenie.common.exception.UnauthorizedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.Map;

@Service
public class AuthService {
    private static final String DEFAULT_ROLE = "ROLE_USER";

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;
    private final AuthAuditLogService auditLogService;

    public AuthService(UserRepository userRepository,
                       RoleRepository roleRepository,
                       PasswordEncoder passwordEncoder,
                       JwtTokenService jwtTokenService,
                       AuthAuditLogService auditLogService) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenService = jwtTokenService;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = request.email().trim().toLowerCase();
        try {
            if (userRepository.existsByEmailIgnoreCase(email)) {
                throw new ConflictException("EMAIL_ALREADY_REGISTERED", "Email is already registered");
            }
            Role role = roleRepository.findByName(DEFAULT_ROLE).orElseGet(() -> roleRepository.save(new Role(DEFAULT_ROLE)));
            User user = new User();
            user.setEmail(email);
            user.setPasswordHash(passwordEncoder.encode(request.password()));
            user.setFirstName(request.firstName().trim());
            user.setLastName(request.lastName().trim());
            user.setRoles(Set.of(role));
            User savedUser = userRepository.save(user);
            AuthResponse response = jwtTokenService.issueToken(savedUser);
            auditLogService.recordSuccess(savedUser.getId(), "USER_REGISTERED", Map.of("email", email));
            return response;
        } catch (RuntimeException exception) {
            auditLogService.recordFailure(null, "USER_REGISTERED", Map.of(
                    "email", email,
                    "error", exception.getClass().getSimpleName(),
                    "message", message(exception)));
            throw exception;
        }
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        String email = request.email().trim().toLowerCase();
        try {
            User user = userRepository.findByEmailIgnoreCase(email)
                    .orElseThrow(() -> new UnauthorizedException("INVALID_CREDENTIALS", "Invalid email or password"));
            if (!user.isEnabled() || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
                throw new UnauthorizedException("INVALID_CREDENTIALS", "Invalid email or password");
            }
            AuthResponse response = jwtTokenService.issueToken(user);
            auditLogService.recordSuccess(user.getId(), "LOGIN_SUCCESS", Map.of("email", email));
            return response;
        } catch (RuntimeException exception) {
            auditLogService.recordFailure(null, "LOGIN_FAILURE", Map.of(
                    "email", email,
                    "error", exception.getClass().getSimpleName(),
                    "message", message(exception)));
            throw exception;
        }
    }

    private String message(RuntimeException exception) {
        return exception.getMessage() == null ? "" : exception.getMessage();
    }
}
