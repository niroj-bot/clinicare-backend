package com.clinicare.config;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class JwtUtilTest {

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() throws Exception {
        jwtUtil = new JwtUtil();

        var secretField = JwtUtil.class.getDeclaredField("secret");
        secretField.setAccessible(true);
        secretField.set(jwtUtil, "clinicare-super-secret-key-for-testing-min-32-chars!!");

        var expirationField = JwtUtil.class.getDeclaredField("expiration");
        expirationField.setAccessible(true);
        expirationField.set(jwtUtil, 86400000L); // 1 day
    }

    @Test
    void generateToken_notNull() {
        String token = jwtUtil.generateToken("user@example.com", "USER");
        assertThat(token).isNotNull().isNotBlank();
    }

    @Test
    void extractEmail_returnsCorrectEmail() {
        String email = "user@example.com";
        String token = jwtUtil.generateToken(email, "USER");
        assertThat(jwtUtil.extractEmail(token)).isEqualTo(email);
    }

    @Test
    void extractRole_returnsCorrectRole() {
        String token = jwtUtil.generateToken("user@example.com", "ADMIN");
        assertThat(jwtUtil.extractRole(token)).isEqualTo("ADMIN");
    }

    @Test
    void isValid_validToken_returnsTrue() {
        String token = jwtUtil.generateToken("user@example.com", "USER");
        assertThat(jwtUtil.isValid(token)).isTrue();
    }

    @Test
    void isValid_tamperedToken_returnsFalse() {
        String token   = jwtUtil.generateToken("user@example.com", "USER");
        String tampered = token.substring(0, token.length() - 5) + "XXXXX";
        assertThat(jwtUtil.isValid(tampered)).isFalse();
    }

    @Test
    void twoTokens_forSameEmail_areNotNull() {
        String token1 = jwtUtil.generateToken("user@example.com", "USER");
        String token2 = jwtUtil.generateToken("user@example.com", "USER");
        assertThat(token1).isNotNull();
        assertThat(token2).isNotNull();
    }
}