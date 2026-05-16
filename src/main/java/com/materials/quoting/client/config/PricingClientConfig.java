package com.materials.quoting.client.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class PricingClientConfig {

    @Bean
    public RestClient metalsPricingRestClient(
            RestClient.Builder builder,
            @Value("${quoting.client.metals-pricing.base-url}") String baseUrl) {

        return builder
                .baseUrl(baseUrl)
                .defaultHeader("Accept", "application/json")
                .build();
    }
}
