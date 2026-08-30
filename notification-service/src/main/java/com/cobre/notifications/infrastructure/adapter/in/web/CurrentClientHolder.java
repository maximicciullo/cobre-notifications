package com.cobre.notifications.infrastructure.adapter.in.web;

/**
 * Holds the authenticated caller's clientId for the duration of one request. Populated by
 * {@link ApiKeyAuthFilter} — never trust a client_id coming from the request body/path itself
 * (that would defeat the whole point of the A01 mitigation, see SECURITY.md).
 */
public final class CurrentClientHolder {

    private static final ThreadLocal<String> CURRENT_CLIENT_ID = new ThreadLocal<>();

    private CurrentClientHolder() {
    }

    static void set(String clientId) {
        CURRENT_CLIENT_ID.set(clientId);
    }

    static void clear() {
        CURRENT_CLIENT_ID.remove();
    }

    public static String get() {
        String clientId = CURRENT_CLIENT_ID.get();
        if (clientId == null) {
            throw new IllegalStateException("No authenticated client in context");
        }
        return clientId;
    }
}
