package com.htai.exe201phapluatso.payment.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import vn.payos.PayOS;

@Configuration
public class PayOSConfig {

    private static final Logger log = LoggerFactory.getLogger(PayOSConfig.class);

    @Value("${payos.client-id}")
    private String clientId;

    @Value("${payos.api-key}")
    private String apiKey;

    @Value("${payos.checksum-key}")
    private String checksumKey;

    @Bean
    public PayOS payOS() {
        log.info("========== PAYOS CONFIG ==========");
        log.info("Client ID: {}", clientId);
        log.info("API Key: {}...", apiKey != null && apiKey.length() > 8 ? apiKey.substring(0, 8) : "null");
        log.info("Checksum Key: {}...", checksumKey != null && checksumKey.length() > 8 ? checksumKey.substring(0, 8) : "null");
        log.info("===================================");
        return new PayOS(clientId, apiKey, checksumKey);
    }
}
