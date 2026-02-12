package com.pingxin403.cuckoo.gateway.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * JwtTokenProvider 单元测试
 */
class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;

    @BeforeEach
    void setUp() {
        jwtTokenProvider = new JwtTokenProvider();
        ReflectionTestUtils.setField(jwtTokenProvider, "jwtSecret", 
            "cuckoo-microservices-secret-key-for-jwt-token-generation-and-validation");
        ReflectionTestUtils.setField(jwtTokenProvider, "jwtExpiration", 86400000L);
    }

    @Test
    void testGenerateToken_Success() {
        // Given
        Long userId = 123L;

        // When
        String token = jwtTokenProvider.generateToken(userId);

        // Then
        assertThat(token).isNotNull();
        assertThat(token).isNotEmpty();
        assertThat(token.split("\\.")).hasSize(3); // JWT 格式：header.payload.signature
    }

    @Test
    void testValidateToken_ValidToken() {
        // Given
        Long userId = 123L;
        String token = jwtTokenProvider.generateToken(userId);

        // When
        boolean isValid = jwtTokenProvider.validateToken(token);

        // Then
        assertThat(isValid).isTrue();
    }

    @Test
    void testValidateToken_InvalidToken() {
        // Given
        String invalidToken = "invalid.token.here";

        // When
        boolean isValid = jwtTokenProvider.validateToken(invalidToken);

        // Then
        assertThat(isValid).isFalse();
    }

    @Test
    void testValidateToken_EmptyToken() {
        // Given
        String emptyToken = "";

        // When
        boolean isValid = jwtTokenProvider.validateToken(emptyToken);

        // Then
        assertThat(isValid).isFalse();
    }

    @Test
    void testGetUserIdFromToken_Success() {
        // Given
        Long userId = 456L;
        String token = jwtTokenProvider.generateToken(userId);

        // When
        Long extractedUserId = jwtTokenProvider.getUserIdFromToken(token);

        // Then
        assertThat(extractedUserId).isEqualTo(userId);
    }

    @Test
    void testTokenRoundTrip() {
        // Given
        Long userId = 789L;

        // When
        String token = jwtTokenProvider.generateToken(userId);
        boolean isValid = jwtTokenProvider.validateToken(token);
        Long extractedUserId = jwtTokenProvider.getUserIdFromToken(token);

        // Then
        assertThat(isValid).isTrue();
        assertThat(extractedUserId).isEqualTo(userId);
    }
}
