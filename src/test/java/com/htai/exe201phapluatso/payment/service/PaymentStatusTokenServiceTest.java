package com.htai.exe201phapluatso.payment.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class PaymentStatusTokenServiceTest {

    private PaymentStatusTokenService tokenService;
    private final String secret = "test-secret-key-for-unit-tests";
    private final long ttlMinutes = 10;

    @BeforeEach
    void setUp() {
        tokenService = new PaymentStatusTokenService(secret, ttlMinutes);
    }

    @Test
    void generate_ShouldCreateValidToken() {
        // When
        String token = tokenService.generate(12345L);

        // Then
        assertNotNull(token);
        assertFalse(token.isEmpty());
        assertTrue(token.contains(".")); // Should be JWT-like format
    }

    @Test
    void validateAndExtractOrderCode_WithValidToken_ShouldReturnOrderCode() {
        // Given
        long orderCode = 12345L;
        String token = tokenService.generate(orderCode);

        // When
        long extractedOrderCode = tokenService.validateAndExtractOrderCode(token, orderCode);

        // Then
        assertEquals(orderCode, extractedOrderCode);
    }

    @Test
    void validateAndExtractOrderCode_WithWrongOrderCode_ShouldThrowException() {
        // Given
        String token = tokenService.generate(12345L);

        // When & Then
        assertThrows(IllegalArgumentException.class, () ->
            tokenService.validateAndExtractOrderCode(token, 99999L)
        );
    }

    @Test
    void validateAndExtractOrderCode_WithTamperedToken_ShouldThrowException() {
        // Given
        String token = tokenService.generate(12345L);
        String tamperedToken = token.replace('1', '9'); // Tamper with payload

        // When & Then
        assertThrows(IllegalArgumentException.class, () ->
            tokenService.validateAndExtractOrderCode(tamperedToken, 12345L)
        );
    }

    @Test
    void validateAndExtractOrderCode_WithInvalidToken_ShouldThrowException() {
        // When & Then
        assertThrows(IllegalArgumentException.class, () ->
            tokenService.validateAndExtractOrderCode("invalid.token.here", 12345L)
        );
    }

    @Test
    void validateAndExtractOrderCode_WithEmptyToken_ShouldThrowException() {
        // When & Then
        assertThrows(IllegalArgumentException.class, () ->
            tokenService.validateAndExtractOrderCode("", 12345L)
        );
    }

    @Test
    void validateAndExtractOrderCode_WithNullToken_ShouldThrowException() {
        // When & Then
        assertThrows(IllegalArgumentException.class, () ->
            tokenService.validateAndExtractOrderCode(null, 12345L)
        );
    }
}

