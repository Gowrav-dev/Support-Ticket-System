package com.helpdesk.controller;

import com.google.gson.JsonObject;
import com.helpdesk.model.User;
import com.helpdesk.service.AuthService;
import com.helpdesk.util.JsonUtil;
import com.helpdesk.util.SessionManager;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;

/**
 * Controller handling user authentication:
 * - POST /api/auth/register (Register as USER or SUPPORT_STAFF)
 * - POST /api/auth/login    (Login and receive session token)
 * - GET  /api/auth/me       (Get currently logged-in user)
 * - POST /api/auth/logout   (Logout)
 */
public class AuthController implements HttpHandler {

    private final AuthService authService = new AuthService();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type, Authorization, X-Auth-Token");

        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(204, -1);
            return;
        }

        String path = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod();

        try {
            if (path.equals("/api/auth/register") && "POST".equalsIgnoreCase(method)) {
                handleRegister(exchange);
            } else if (path.equals("/api/auth/login") && "POST".equalsIgnoreCase(method)) {
                handleLogin(exchange);
            } else if (path.equals("/api/auth/me") && "GET".equalsIgnoreCase(method)) {
                handleMe(exchange);
            } else if (path.equals("/api/auth/logout") && "POST".equalsIgnoreCase(method)) {
                handleLogout(exchange);
            } else {
                JsonUtil.sendError(exchange, 404, "Auth endpoint not found: " + path);
            }
        } catch (IllegalArgumentException e) {
            JsonUtil.sendError(exchange, 400, e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            JsonUtil.sendError(exchange, 500, "Authentication error: " + e.getMessage());
        }
    }

    private void handleRegister(HttpExchange exchange) throws Exception {
        String body = JsonUtil.readRequestBody(exchange);
        JsonObject json = JsonUtil.parseObject(body);

        String username = json.has("username") ? json.get("username").getAsString() : "";
        String password = json.has("password") ? json.get("password").getAsString() : "";
        String fullName = json.has("fullName") ? json.get("fullName").getAsString() : "";
        String role = json.has("role") ? json.get("role").getAsString() : "USER";

        User registered = authService.register(username, password, fullName, role);
        JsonUtil.sendSuccess(exchange, 201, "Account registered successfully", registered);
    }

    private void handleLogin(HttpExchange exchange) throws Exception {
        String body = JsonUtil.readRequestBody(exchange);
        JsonObject json = JsonUtil.parseObject(body);

        String username = json.has("username") ? json.get("username").getAsString() : "";
        String password = json.has("password") ? json.get("password").getAsString() : "";

        AuthService.AuthResult result = authService.login(username, password);
        JsonUtil.sendSuccess(exchange, 200, "Login successful", result);
    }

    private void handleMe(HttpExchange exchange) throws Exception {
        User user = SessionManager.getUserFromExchange(exchange);
        if (user != null) {
            JsonUtil.sendSuccess(exchange, 200, "Authenticated", user);
        } else {
            JsonUtil.sendError(exchange, 401, "Not logged in");
        }
    }

    private void handleLogout(HttpExchange exchange) throws Exception {
        String authHeader = exchange.getRequestHeaders().getFirst("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            SessionManager.removeSession(authHeader.substring(7).trim());
        }
        JsonUtil.sendSuccess(exchange, 200, "Logged out successfully", null);
    }
}
