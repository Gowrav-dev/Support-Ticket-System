package com.helpdesk.controller;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Serves static HTML, CSS, and JavaScript files from resources/static.
 */
public class StaticFileController implements HttpHandler {

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();

        // Default routes
        if (path.equals("/") || path.equals("/index.html") || path.equals("/create-ticket.html")) {
            path = "/index.html";
        } else if (path.equals("/admin") || path.equals("/admin.html") || path.equals("/admin-dashboard.html")) {
            path = "/admin.html";
        }

        String resourcePath = "static" + (path.startsWith("/") ? path : "/" + path);
        InputStream is = getClass().getClassLoader().getResourceAsStream(resourcePath);

        if (is == null) {
            byte[] msg = "<h1>404 Not Found</h1>".getBytes();
            exchange.sendResponseHeaders(404, msg.length);
            try (OutputStream os = exchange.getResponseBody()) { os.write(msg); }
            return;
        }

        String mimeType = "text/html";
        if (path.endsWith(".css")) mimeType = "text/css";
        else if (path.endsWith(".js")) mimeType = "application/javascript";
        else if (path.endsWith(".json")) mimeType = "application/json";

        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] data = new byte[1024];
        int nRead;
        while ((nRead = is.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, nRead);
        }
        is.close();

        byte[] bytes = buffer.toByteArray();
        exchange.getResponseHeaders().set("Content-Type", mimeType + "; charset=UTF-8");
        exchange.sendResponseHeaders(200, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }
}
