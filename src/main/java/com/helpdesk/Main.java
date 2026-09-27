package com.helpdesk;

import com.helpdesk.config.Database;
import com.helpdesk.controller.AuthController;
import com.helpdesk.controller.StaticFileController;
import com.helpdesk.controller.TicketController;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;

/**
 * Main Class: Starts the Java HTTP Server on port 8080.
 */
public class Main {

    private static HttpServer server;

    public static void main(String[] args) {
        int port = 8080;
        try {
            // 1. Initialize Database & seed demo accounts
            Database.initDatabase();

            // 2. Start HTTP Server
            server = startServer(port);

            System.out.println("==============================================================");
            System.out.println("  AI-Powered IT Helpdesk Server running on port " + port);
            System.out.println("  User Portal:          http://localhost:" + port);
            System.out.println("  Login Page:           http://localhost:" + port + "/login.html");
            System.out.println("  Registration Page:    http://localhost:" + port + "/register.html");
            System.out.println("  Support Staff Portal: http://localhost:" + port + "/support.html");
            System.out.println("==============================================================");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static HttpServer startServer(int port) throws IOException {
        HttpServer httpServer = HttpServer.create(new InetSocketAddress(port), 0);

        // Map API and Static file routes
        httpServer.createContext("/api/auth", new AuthController());
        httpServer.createContext("/api/tickets", new TicketController());
        httpServer.createContext("/", new StaticFileController());

        httpServer.start();
        return httpServer;
    }

    public static void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }
}
