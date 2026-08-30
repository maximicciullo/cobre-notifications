package com.cobre.notifications.infrastructure.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Configuration
public class RestClientConfig {

    @Bean
    public RestClient webhookRestClient(
            @Value("${notification.delivery.http-timeout-seconds:5}") long timeoutSeconds
    ) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        int timeoutMillis = (int) Duration.ofSeconds(timeoutSeconds).toMillis();
        factory.setConnectTimeout(timeoutMillis);
        factory.setReadTimeout(timeoutMillis);
        return RestClient.builder()
                .requestFactory(factory)
                .build();
    }
}
