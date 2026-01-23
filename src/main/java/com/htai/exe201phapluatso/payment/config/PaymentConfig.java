package com.htai.exe201phapluatso.payment.config;

import com.htai.exe201phapluatso.payment.service.PaymentStatusTokenService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for payment-related beans.
 */
@Configuration
public class PaymentConfig {

    @Value("${payment.status-token-secret}")
    private String tokenSecret;

    @Value("${payment.status-token-ttl-minutes:10}")
    private long tokenTtlMinutes;

    /**
     * Payment status token service for generating and validating
     * short-lived signed tokens for payment status polling.
     */
    @Bean
    public PaymentStatusTokenService paymentStatusTokenService() {
        return new PaymentStatusTokenService(tokenSecret, tokenTtlMinutes);
    }
}

