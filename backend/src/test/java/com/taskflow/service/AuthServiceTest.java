package com.taskflow.service;

import com.taskflow.domain.User;
import com.taskflow.dto.AuthResponse;
import com.taskflow.dto.LoginRequest;
import com.taskflow.dto.RegisterRequest;
import com.taskflow.exception.ConflictException;
import com.taskflow.repository.UserRepository;
import com.taskflow.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService - registro y login")
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthService authService;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User("Erick", "erick@example.com", "encoded-password");
        user.setId(1L);
    }

    @Test
    @DisplayName("registra el usuario con la contraseña hasheada y devuelve un token")
    void register_hashesPasswordAndReturnsToken() {
        when(userRepository.existsByEmailIgnoreCase("erick@example.com")).thenReturn(false);
        when(passwordEncoder.encode("secret123")).thenReturn("encoded-password");
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(jwtService.generateToken(user)).thenReturn("jwt-token");

        AuthResponse response = authService.register(
                new RegisterRequest("Erick", "  Erick@Example.com ", "secret123"));

        assertThat(response.token()).isEqualTo("jwt-token");
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.user().email()).isEqualTo("erick@example.com");

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(saved.capture());
        assertThat(saved.getValue().getPassword()).isEqualTo("encoded-password");
        assertThat(saved.getValue().getEmail()).isEqualTo("erick@example.com");
    }

    @Test
    @DisplayName("rechaza el registro si el email ya está en uso")
    void register_throwsConflictWhenEmailExists() {
        when(userRepository.existsByEmailIgnoreCase("erick@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(
                new RegisterRequest("Erick", "erick@example.com", "secret123")))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Ya existe una cuenta con ese email");

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("devuelve un token con credenciales válidas")
    void login_returnsTokenForValidCredentials() {
        when(userRepository.findByEmailIgnoreCase("erick@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("secret123", "encoded-password")).thenReturn(true);
        when(jwtService.generateToken(user)).thenReturn("jwt-token");

        AuthResponse response = authService.login(
                new LoginRequest("ERICK@example.com", "secret123"));

        assertThat(response.token()).isEqualTo("jwt-token");
        assertThat(response.user().name()).isEqualTo("Erick");
    }

    @Test
    @DisplayName("rechaza el login si la contraseña no coincide")
    void login_throwsBadCredentialsWhenPasswordDoesNotMatch() {
        when(userRepository.findByEmailIgnoreCase("erick@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

        assertThatThrownBy(() -> authService.login(
                new LoginRequest("erick@example.com", "incorrecta")))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Email o contraseña incorrectos");

        verify(jwtService, never()).generateToken(any(User.class));
    }

    @Test
    @DisplayName("rechaza el login si el usuario no existe")
    void login_throwsBadCredentialsWhenUserDoesNotExist() {
        when(userRepository.findByEmailIgnoreCase("nadie@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(
                new LoginRequest("nadie@example.com", "secret123")))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Email o contraseña incorrectos");
    }
}
