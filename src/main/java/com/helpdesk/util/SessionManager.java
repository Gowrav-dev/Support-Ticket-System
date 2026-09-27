package com.helpdesk.util;

import com.helpdesk.model.User;
import com.sun.net.httpserver.HttpExchange;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Lightweight, in-memory session manager.
 * Stores active user sessions mapped to generated tokens.
 */
public class SessionManager {

    private static final Map<String, User> sessions = new ConcurrentHashMap<>();

    public static String createSession(User user) {
        String token = UUID.randomUUID().toString();
        sessions.put(token, user);
        return token;
    }

    public static User getUser(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        return sessions.get(token.trim());
    }

    public static void removeSession(String token) {
        if (token != null) {
            sessions.remove(token.trim());
        }
    }

    /**
     * Extracts authenticated user from HTTP request headers or query params.
     */
    public static User getUserFromExchange(HttpExchange exchange) {
        // 1. Check Authorization header: "Bearer <token>"
        String authHeader = exchange.getRequestHeaders().getFirst("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7).trim();
            User user = getUser(token);
            if (user != null) return user;
        }

        // 2. Check X-Auth-Token header
        String customHeader = exchange.getRequestHeaders().getFirst("X-Auth-Token");
        if (customHeader != null && !customHeader.isBlank()) {
            User user = getUser(customHeader.trim());
            if (user != null) return user;
        }

        // 3. Check query param ?token=...
        String query = exchange.getRequestURI().getQuery();
        if (query != null && query.contains("token=")) {
            for (String param : query.split("&")) {
                if (param.startsWith("token=")) {
                    String token = param.substring(6).trim();
                    User user = getUser(token);
                    if (user != null) return user;
                }
            }
        }

        return null;
    }
}
