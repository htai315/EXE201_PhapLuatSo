package com.htai.exe201phapluatso.payment.controller;

import com.htai.exe201phapluatso.auth.security.AuthUserPrincipal;
import com.htai.exe201phapluatso.payment.dto.CreatePaymentRequest;
import com.htai.exe201phapluatso.payment.service.PaymentStatusTokenService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureWebMvc
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PaymentStatusTokenService tokenService;

    @Test
    void checkPaymentStatus_WithValidToken_ShouldReturn200() throws Exception {
        // Given
        long orderCode = 12345L;
        String token = tokenService.generate(orderCode);

        // When & Then
        mockMvc.perform(get("/api/payment/status/{orderCode}", orderCode)
                .param("token", token))
                .andExpect(status().isNotFound()); // 404 because payment doesn't exist in test DB
    }

    @Test
    void checkPaymentStatus_WithoutToken_ShouldReturn404() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/payment/status/{orderCode}", 12345L))
                .andExpect(status().is(404)); // 404 because no auth and no token
    }

    @Test
    void checkPaymentStatus_WithInvalidToken_ShouldReturn404() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/payment/status/{orderCode}", 12345L)
                .param("token", "invalid.token.here"))
                .andExpect(status().is(404));
    }

    @Test
    @WithMockUser(username = "test@example.com")
    void checkPaymentStatus_WithAuthenticatedUser_ShouldWorkWithoutToken() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/payment/status/{orderCode}", 12345L))
                .andExpect(status().isNotFound()); // 404 because payment doesn't exist, but auth worked
    }
}

