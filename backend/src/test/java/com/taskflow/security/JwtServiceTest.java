package com.taskflow.security;

import com.taskflow.domain.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("JwtService - generación y validación de tokens")
class JwtServiceTest {

    private static final String SECRET =
            "unit-test-secret-key-with-at-least-32-bytes-long-123456";

    @Test
    @DisplayName("genera un token válido con el email del usuario como subject")
    void generateToken_containsUserEmail() {
        JwtService jwtService = new JwtService(SECRET, 60_000);
        User user = new User("Erick", "erick@example.com", "encoded");
        user.setId(7L);

        String token = jwtService.generateToken(user);

        assertThat(token).isNotBlank();
        assertThat(jwtService.isValid(token)).isTrue();
        assertThat(jwtService.extractEmail(token)).isEqualTo("erick@example.com");
    }

    @Test
    @DisplayName("rechaza un token manipulado")
    void isValid_returnsFalseForTamperedToken() {
        JwtService jwtService = new JwtService(SECRET, 60_000);
        User user = new User("Erick", "erick@example.com", "encoded");
        user.setId(7L);

        String token = jwtService.generateToken(user);
        String tampered = token.substring(0, token.length() - 2) + "xx";

        assertThat(jwtService.isValid(tampered)).isFalse();
    }

    @Test
    @DisplayName("rechaza un token expirado")
    void isValid_returnsFalseForExpiredToken() {
        JwtService expiredIssuer = new JwtService(SECRET, -1_000);
        User user = new User("Erick", "erick@example.com", "encoded");
        user.setId(7L);

        String token = expiredIssuer.generateToken(user);

        assertThat(expiredIssuer.isValid(token)).isFalse();
    }

    @Test
    @DisplayName("rechaza un token firmado con otra clave")
    void isValid_returnsFalseForTokenSignedWithAnotherKey() {
        JwtService issuer = new JwtService(SECRET, 60_000);
        JwtService verifier = new JwtService(
                "another-secret-key-that-is-also-long-enough-987654321", 60_000);
        User user = new User("Erick", "erick@example.com", "encoded");
        user.setId(7L);

        String token = issuer.generateToken(user);

        assertThat(verifier.isValid(token)).isFalse();
    }
}
