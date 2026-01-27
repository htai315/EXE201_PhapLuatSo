package com.htai.exe201phapluatso.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.security.oauth2.client.endpoint.DefaultAuthorizationCodeTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2AccessTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2AuthorizationCodeGrantRequest;
import org.springframework.security.oauth2.client.http.OAuth2ErrorResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;

/**
 * Configuration to use JDK HttpClient for OAuth2 instead of reactor-netty.
 * This fixes the "Could not initialize class reactor.netty.http.client.HttpClientSecure" error
 * caused by missing native QUIC libraries on Linux servers.
 */
@Configuration
public class OAuth2ClientConfig {

    @Bean
    public OAuth2AccessTokenResponseClient<OAuth2AuthorizationCodeGrantRequest> accessTokenResponseClient() {
        DefaultAuthorizationCodeTokenResponseClient client = new DefaultAuthorizationCodeTokenResponseClient();
        
        // Use JDK HttpClient instead of reactor-netty to avoid QUIC native library issues
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        
        RestTemplate restTemplate = new RestTemplate(new JdkClientHttpRequestFactory(httpClient));
        restTemplate.setErrorHandler(new OAuth2ErrorResponseErrorHandler());
        
        client.setRestOperations(restTemplate);
        
        return client;
    }
}
