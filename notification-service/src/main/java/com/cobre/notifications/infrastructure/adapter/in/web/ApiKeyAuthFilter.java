package com.cobre.notifications.infrastructure.adapter.in.web;

import com.cobre.notifications.application.port.out.SubscriptionRepositoryPort;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Every request to /notification_events/** must carry a valid API key, which resolves
 * server-side to exactly one client_id — the caller can never simply declare which client
 * they are.
 */
@Component
public class ApiKeyAuthFilter extends OncePerRequestFilter {

    private static final String HEADER = "X-Api-Key";

    private final SubscriptionRepositoryPort subscriptionRepository;

    public ApiKeyAuthFilter(SubscriptionRepositoryPort subscriptionRepository) {
        this.subscriptionRepository = subscriptionRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        if (!request.getRequestURI().startsWith("/notification_events")) {
            chain.doFilter(request, response);
            return;
        }

        String apiKey = request.getHeader(HEADER);
        if (apiKey == null || apiKey.isBlank()) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Missing " + HEADER + " header");
            return;
        }

        var subscription = subscriptionRepository.findByApiKey(apiKey);
        if (subscription.isEmpty() || !subscription.get().active()) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid API key");
            return;
        }

        try {
            CurrentClientHolder.set(subscription.get().clientId());
            chain.doFilter(request, response);
        } finally {
            CurrentClientHolder.clear();
        }
    }
}
