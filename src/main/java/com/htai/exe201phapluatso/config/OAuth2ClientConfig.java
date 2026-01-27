package com.htai.exe201phapluatso.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.security.oauth2.client.endpoint.OAuth2AccessTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2AuthorizationCodeGrantRequest;
import org.springframework.security.oauth2.client.endpoint.RestClientAuthorizationCodeTokenResponseClient;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;

/**
 * Configuration to use JDK HttpClient for OAuth2 instead of reactor-netty.
 * This fixes the "Could not initialize class reactor.netty.http.client.HttpClientSecure" error
 * caused by missing native QUIC libraries on Linux servers.
 */
@Configuration
public class OAuth2ClientConfig {

    @Bean
    public OAuth2AccessTokenResponseClient<OAuth2AuthorizationCodeGrantRequest> accessTokenResponseClient() {
        // Use JDK HttpClient instead of reactor-netty to avoid QUIC native library issues
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        
        RestClient restClient = RestClient.builder()
                .requestFactory(new JdkClientHttpRequestFactory(httpClient))
                .build();
        
        RestClientAuthorizationCodeTokenResponseClient client = 
                new RestClientAuthorizationCodeTokenResponseClient();
        client.setRestClient(restClient);
        
        return client;
    }
}
