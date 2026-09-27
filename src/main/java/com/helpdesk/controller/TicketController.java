package com.helpdesk.controller;

import com.google.gson.JsonObject;
import com.helpdesk.dao.TicketDao;
import com.helpdesk.model.Ticket;
import com.helpdesk.model.User;
import com.helpdesk.service.AiService;
import com.helpdesk.util.JsonUtil;
import com.helpdesk.util.SessionManager;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.util.List;

/**
 * Controller handling all Ticket API endpoints:
 * - POST /api/tickets               (Create ticket + AI analysis)
 * - GET  /api/tickets               (List tickets - user sees own; support staff sees escalated/active)
 * - GET  /api/tickets/{id}          (View ticket details)
 * - POST /api/tickets/{id}/feedback (User feedback: solved by AI vs problem still exists)
 * - POST /api/tickets/{id}/assign   (Support Staff picks/assigns ticket)
 * - POST /api/tickets/{id}/status   (Update ticket status)
 */
public class TicketController implements HttpHandler {

    private final TicketDao ticketDao = new TicketDao();
    private final AiService aiService = new AiService();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        // Enable CORS
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
            if (path.equals("/api/tickets") || path.equals("/api/tickets/")) {
                if ("POST".equalsIgnoreCase(method)) {
                    handleCreateTicket(exchange);
                } else if ("GET".equalsIgnoreCase(method)) {
                    handleGetTickets(exchange);
                }
            } else if (path.startsWith("/api/tickets/")) {
                String sub = path.substring("/api/tickets/".length());
                if (sub.endsWith("/feedback") && "POST".equalsIgnoreCase(method)) {
                    int ticketId = Integer.parseInt(sub.replace("/feedback", "").trim());
                    handleFeedback(exchange, ticketId);
                } else if (sub.endsWith("/assign") && "POST".equalsIgnoreCase(method)) {
                    int ticketId = Integer.parseInt(sub.replace("/assign", "").trim());
                    handleAssignTicket(exchange, ticketId);
                } else if (sub.endsWith("/status") && "POST".equalsIgnoreCase(method)) {
                    int ticketId = Integer.parseInt(sub.replace("/status", "").trim());
                    handleUpdateStatus(exchange, ticketId);
                } else if ("GET".equalsIgnoreCase(method)) {
                    int ticketId = Integer.parseInt(sub.trim());
                    handleGetTicketById(exchange, ticketId);
                }
            } else {
                JsonUtil.sendError(exchange, 404, "Endpoint not found");
            }
        } catch (Exception e) {
            e.printStackTrace();
            JsonUtil.sendError(exchange, 500, "Error: " + e.getMessage());
        }
    }

    // 1. Create Ticket + Run Groq AI (Initial state: PENDING_USER while user reviews AI solution)
    private void handleCreateTicket(HttpExchange exchange) throws Exception {
        String body = JsonUtil.readRequestBody(exchange);
        JsonObject json = JsonUtil.parseObject(body);

        User currentUser = SessionManager.getUserFromExchange(exchange);

        if (currentUser != null && "SUPPORT_STAFF".equalsIgnoreCase(currentUser.getRole())) {
            JsonUtil.sendError(exchange, 403, "Access Denied: Support Staff cannot create user support tickets");
            return;
        }

        String userName = (currentUser != null && currentUser.getFullName() != null)
                ? currentUser.getFullName()
                : (json.has("userName") ? json.get("userName").getAsString().trim() : "Anonymous");

        String title = json.has("title") ? json.get("title").getAsString().trim() : "";
        String description = json.has("description") ? json.get("description").getAsString().trim() : "";

        if (title.isEmpty() || description.isEmpty()) {
            JsonUtil.sendError(exchange, 400, "Title and description are required");
            return;
        }

        // Call Groq AI API for categorization, priority, summary & troubleshooting steps
        AiService.AiResult ai = aiService.analyzeTicket(title, description);

        Ticket ticket = new Ticket();
        ticket.setUserName(userName.isEmpty() ? "Anonymous" : userName);
        ticket.setTitle(title);
        ticket.setDescription(description);
        ticket.setCategory(ai.category);
        ticket.setPriority(ai.priority);
        ticket.setAiSummary(ai.summary);
        ticket.setAiSolution(ai.suggestedSolution);
        // Initially PENDING_USER: waiting for user to check if AI solution resolved their problem
        ticket.setStatus("PENDING_USER");
        ticket.setAssignedTo(null);

        Ticket created = ticketDao.createTicket(ticket);
        JsonUtil.sendSuccess(exchange, 201, "Ticket created", created);
    }

    // 2. Get Tickets
    // Regular users see all their tickets.
    // Support Staff sees tickets escalated to them (status != 'PENDING_USER').
    private void handleGetTickets(HttpExchange exchange) throws Exception {
        User currentUser = SessionManager.getUserFromExchange(exchange);

        List<Ticket> tickets;
        if (currentUser != null && "USER".equalsIgnoreCase(currentUser.getRole())) {
            tickets = ticketDao.findByUserName(currentUser.getFullName());
            if (tickets.isEmpty() && currentUser.getUsername() != null) {
                tickets = ticketDao.findByUserName(currentUser.getUsername());
            }
        } else {
            // Support Staff only gets tickets where user indicated "Problem still exists" (or non-pending)
            tickets = ticketDao.findAll().stream()
                    .filter(t -> !"PENDING_USER".equalsIgnoreCase(t.getStatus()))
                    .toList();
        }

        JsonUtil.sendSuccess(exchange, 200, "Tickets loaded", tickets);
    }

    // 3. Get Single Ticket Details
    private void handleGetTicketById(HttpExchange exchange, int ticketId) throws Exception {
        Ticket ticket = ticketDao.findById(ticketId);
        if (ticket != null) {
            JsonUtil.sendSuccess(exchange, 200, "Ticket found", ticket);
        } else {
            JsonUtil.sendError(exchange, 404, "Ticket not found");
        }
    }

    // 4. User Feedback on AI Solution ("This solved my problem" vs "Problem still exists")
    private void handleFeedback(HttpExchange exchange, int ticketId) throws Exception {
        String body = JsonUtil.readRequestBody(exchange);
        JsonObject json = JsonUtil.parseObject(body);
        boolean solved = json.has("solved") && json.get("solved").getAsBoolean();

        // If solved -> RESOLVED (Support staff not needed)
        // If unsolved -> OPEN (Support staff receives the ticket in their dashboard)
        String newStatus = solved ? "RESOLVED" : "OPEN";
        boolean updated = ticketDao.updateStatus(ticketId, newStatus);

        if (updated) {
            String msg = solved
                    ? "Great! Ticket marked as Resolved by AI solution."
                    : "Ticket forwarded to Support Staff dashboard.";
            JsonUtil.sendSuccess(exchange, 200, msg, newStatus);
        } else {
            JsonUtil.sendError(exchange, 404, "Ticket not found");
        }
    }

    // 5. Assign Ticket to a Support Staff employee
    private void handleAssignTicket(HttpExchange exchange, int ticketId) throws Exception {
        User currentUser = SessionManager.getUserFromExchange(exchange);

        if (currentUser != null && "USER".equalsIgnoreCase(currentUser.getRole())) {
            JsonUtil.sendError(exchange, 403, "Access Denied: Regular users cannot pick or assign tickets");
            return;
        }

        String assignedTo = null;
        if (currentUser != null && "SUPPORT_STAFF".equalsIgnoreCase(currentUser.getRole())) {
            assignedTo = currentUser.getFullName() != null ? currentUser.getFullName() : currentUser.getUsername();
        }

        String body = JsonUtil.readRequestBody(exchange);
        if (!body.isBlank()) {
            JsonObject json = JsonUtil.parseObject(body);
            if (json.has("assignedTo") && !json.get("assignedTo").getAsString().isBlank()) {
                assignedTo = json.get("assignedTo").getAsString().trim();
            }
        }

        if (assignedTo == null || assignedTo.isBlank()) {
            assignedTo = "Support Staff";
        }

        boolean updated = ticketDao.assignTicket(ticketId, assignedTo);
        if (updated) {
            JsonUtil.sendSuccess(exchange, 200, "Ticket assigned to " + assignedTo, null);
        } else {
            JsonUtil.sendError(exchange, 404, "Ticket not found");
        }
    }

    // 6. Update Ticket Status
    private void handleUpdateStatus(HttpExchange exchange, int ticketId) throws Exception {
        User currentUser = SessionManager.getUserFromExchange(exchange);

        if (currentUser != null && "USER".equalsIgnoreCase(currentUser.getRole())) {
            JsonUtil.sendError(exchange, 403, "Access Denied: Regular users cannot change ticket status");
            return;
        }

        String body = JsonUtil.readRequestBody(exchange);
        JsonObject json = JsonUtil.parseObject(body);
        String status = json.has("status") ? json.get("status").getAsString().trim() : "OPEN";

        boolean updated = ticketDao.updateStatus(ticketId, status);
        if (updated) {
            JsonUtil.sendSuccess(exchange, 200, "Status updated to " + status, null);
        } else {
            JsonUtil.sendError(exchange, 404, "Ticket not found");
        }
    }
}
