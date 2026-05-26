package com.its.gestioneordinirestclient.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * Configuration class responsible for setting up and initializing {@link RestClient} beans
 * used for HTTP communication with external microservices.
 */
@Configuration
public class RestClientConfig {

    /**
     * The base URL of the external payment microservice, injected from the
     * application properties (e.g., {@code application.properties} or {@code application.yml}).
     */
    @Value("${payment.service.url}")
    private String paymentServiceUrl;

    /**
     * Creates and configures a {@link RestClient} bean specifically for interacting with
     * the external payment microservice.
     * <p>
     * The client is pre-configured with the designated base URL and a default
     * {@code Content-Type: application/json} header for all outgoing requests.
     * </p>
     *
     * @return a fully configured {@link RestClient} instance ready for injection
     */
    @Bean
    public RestClient paymentRestClient() {
        return RestClient.builder()
                .baseUrl(paymentServiceUrl)
                .defaultHeader("Content-Type", "application/json")
                .build();
    }
}