package com.cobre.notifications.infrastructure.config;

import com.cobre.notifications.domain.model.DeliveryStatus;
import org.springframework.context.annotation.Configuration;
import org.springframework.format.FormatterRegistry;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** Makes the delivery_status query param case-insensitive (matches the lowercase seed data). */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addFormatters(@NonNull FormatterRegistry registry) {
        registry.addConverter(String.class, DeliveryStatus.class, source -> DeliveryStatus.valueOf(source.trim().toUpperCase()));
    }
}
