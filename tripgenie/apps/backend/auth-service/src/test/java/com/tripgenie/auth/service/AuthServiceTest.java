package com.tripgenie.auth.service;

import com.tripgenie.auth.domain.Role;
import com.tripgenie.auth.domain.User;
import com.tripgenie.auth.repository.RoleRepository;
import com.tripgenie.auth.repository.UserRepository;
import com.tripgenie.common.dto.AuthResponse;
import com.tripgenie.common.dto.LoginRequest;
import com.tripgenie.common.dto.RegisterRequest;
import com.tripgenie.common.exception.ConflictException;
import com.tripgenie.common.exception.UnauthorizedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {
    @Mock
    private UserRepository userRepository;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private JwtTokenService jwtTokenService;

    private PasswordEncoder passwordEncoder;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder();
        authService = new AuthService(userRepository, roleRepository, passwordEncoder, jwtTokenService);
    }

    @Test
    void registerHashesPasswordAssignsUserRoleAndReturnsToken() {
        RegisterRequest request = new RegisterRequest("NEW@EXAMPLE.COM", "password123", " Jane ", " Doe ");
        AuthResponse authResponse = new AuthResponse("token", "Bearer", Instant.now().plusSeconds(3600), UUID.randomUUID(), "new@example.com", Set.of("USER"));

        when(userRepository.existsByEmailIgnoreCase("new@example.com")).thenReturn(false);
        when(roleRepository.findByName("ROLE_USER")).thenReturn(Optional.of(new Role("ROLE_USER")));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(jwtTokenService.issueToken(any(User.class))).thenReturn(authResponse);

        AuthResponse result = authService.register(request);

        assertThat(result).isEqualTo(authResponse);
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User saved = userCaptor.getValue();
        assertThat(saved.getEmail()).isEqualTo("new@example.com");
        assertThat(saved.getFirstName()).isEqualTo("Jane");
        assertThat(saved.getLastName()).isEqualTo("Doe");
        assertThat(saved.getPasswordHash()).isNotEqualTo("password123");
        assertThat(passwordEncoder.matches("password123", saved.getPasswordHash())).isTrue();
        assertThat(saved.getRoles()).extracting(Role::getName).containsExactly("ROLE_USER");
    }

    @Test
    void registerRejectsDuplicateEmail() {
        when(userRepository.existsByEmailIgnoreCase("jane@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(new RegisterRequest("jane@example.com", "password123", "Jane", "Doe")))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void loginRejectsInvalidPassword() {
        User user = new User();
        user.setEmail("jane@example.com");
        user.setPasswordHash(passwordEncoder.encode("correct-password"));
        when(userRepository.findByEmailIgnoreCase("jane@example.com")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.login(new LoginRequest("jane@example.com", "wrong-password")))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void loginReturnsTokenForValidCredentials() {
        User user = new User();
        user.setEmail("jane@example.com");
        user.setPasswordHash(passwordEncoder.encode("correct-password"));
        AuthResponse authResponse = new AuthResponse("token", "Bearer", Instant.now().plusSeconds(3600), UUID.randomUUID(), "jane@example.com", Set.of("USER"));

        when(userRepository.findByEmailIgnoreCase("jane@example.com")).thenReturn(Optional.of(user));
        when(jwtTokenService.issueToken(user)).thenReturn(authResponse);

        assertThat(authService.login(new LoginRequest("jane@example.com", "correct-password"))).isEqualTo(authResponse);
    }
}
