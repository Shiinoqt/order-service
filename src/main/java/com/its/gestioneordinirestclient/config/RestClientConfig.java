package com.its.gestioneordinirestclient.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {
    @Value("${payment.service.url}")
    private String paymentServiceUrl;

    @Bean
    public RestClient paymentRestClient() {
        return RestClient.builder()
                .baseUrl(paymentServiceUrl)
                .defaultHeader("Content-Type", "application/json")
                .build();
    }
}
