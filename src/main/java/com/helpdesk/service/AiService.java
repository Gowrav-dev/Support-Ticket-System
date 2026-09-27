package com.helpdesk.service;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Properties;

/**
 * Service to classify the ticket and get solutions from the Groq LLM API.
 * 
 * Supports Groq API key from:
 * 1. Environment variable: GROQ_API_KEY
 * 2. System property: -DGROQ_API_KEY=...
 * 3. Configuration file: src/main/resources/application.properties (groq.api.key=...)
 * 
 * If no key is set or the network is unavailable, it uses an intelligent
 * rule-based local diagnosis engine so the application works seamlessly 100% of the time.
 */
public class AiService {

    public static class AiResult {
        public String category = "OTHER";
        public String priority = "MEDIUM";
        public String summary = "Support ticket submitted.";
        public String suggestedSolution = "1. Check physical device connections.\n2. Restart computer or application.\n3. Contact administrator if issue persists.";

        public AiResult() {}

        public AiResult(String category, String priority, String summary, String suggestedSolution) {
            this.category = category;
            this.priority = priority;
            this.summary = summary;
            this.suggestedSolution = suggestedSolution;
        }
    }

    public AiResult analyzeTicket(String title, String description) {
        String apiKey = resolveApiKey();

        // If a Groq API key is configured, call the live Groq LLM API
        if (apiKey != null && !apiKey.isBlank()) {
            try {
                return callGroqApi(title, description, apiKey.trim());
            } catch (Exception e) {
                System.err.println("[AiService] Notice: Groq API call failed (" + e.getMessage() + "). Using local smart diagnosis.");
            }
        }

        // Seamless local diagnosis fallback when no key is set or offline
        return generateLocalDiagnosis(title, description);
    }

    private String resolveApiKey() {
        // 1. Check environment variable: GROQ_API_KEY
        String envKey = System.getenv("GROQ_API_KEY");
        if (envKey != null && !envKey.isBlank()) {
            return envKey.trim();
        }

        // 2. Check system property: -DGROQ_API_KEY=...
        String propKey = System.getProperty("GROQ_API_KEY");
        if (propKey != null && !propKey.isBlank()) {
            return propKey.trim();
        }

        // 3. Check application.properties: groq.api.key=...
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("application.properties")) {
            if (is != null) {
                Properties props = new Properties();
                props.load(is);
                String fileKey = props.getProperty("groq.api.key");
                if (fileKey != null && !fileKey.isBlank() && !fileKey.startsWith("your_") && !fileKey.equalsIgnoreCase("placeholder")) {
                    return fileKey.trim();
                }
            }
        } catch (Exception ignored) {}

        return null;
    }

    private AiResult callGroqApi(String title, String description, String apiKey) throws Exception {
        HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8)).build();

        String systemPrompt = """
            You are an IT Support AI.
            Analyze the user's issue and return ONLY a valid JSON object with these 4 keys:
            {
              "category": "NETWORK" or "HARDWARE" or "SOFTWARE" or "ACCOUNT" or "OTHER",
              "priority": "LOW" or "MEDIUM" or "HIGH",
              "summary": "1 sentence technical summary",
              "suggested_solution": "Numbered step-by-step troubleshooting instructions"
            }
            Do not include markdown ticks or text outside the JSON object.
        """;

        String userPrompt = "Title: " + title + "\nDescription: " + description;

        JsonObject body = new JsonObject();
        body.addProperty("model", "llama-3.3-70b-versatile");
        body.addProperty("temperature", 0.2);

        JsonObject responseFormat = new JsonObject();
        responseFormat.addProperty("type", "json_object");
        body.add("response_format", responseFormat);

        JsonArray messages = new JsonArray();
        JsonObject sys = new JsonObject();
        sys.addProperty("role", "system");
        sys.addProperty("content", systemPrompt);
        messages.add(sys);

        JsonObject usr = new JsonObject();
        usr.addProperty("role", "user");
        usr.addProperty("content", userPrompt);
        messages.add(usr);

        body.add("messages", messages);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.groq.com/openai/v1/chat/completions"))
                .timeout(Duration.ofSeconds(10))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            JsonObject respJson = JsonParser.parseString(response.body()).getAsJsonObject();
            String content = respJson.getAsJsonArray("choices")
                    .get(0).getAsJsonObject()
                    .getAsJsonObject("message")
                    .get("content").getAsString();

            JsonObject result = JsonParser.parseString(content).getAsJsonObject();
            return new AiResult(
                    result.has("category") ? result.get("category").getAsString().toUpperCase() : "OTHER",
                    result.has("priority") ? result.get("priority").getAsString().toUpperCase() : "MEDIUM",
                    result.has("summary") ? result.get("summary").getAsString() : "AI diagnostic summary generated.",
                    result.has("suggested_solution") ? result.get("suggested_solution").getAsString() : "1. Restart application.\n2. Contact IT."
            );
        }

        return generateLocalDiagnosis(title, description);
    }

    private AiResult generateLocalDiagnosis(String title, String description) {
        String text = (title + " " + description).toLowerCase();

        if (text.contains("vpn") || text.contains("wifi") || text.contains("wi-fi") || text.contains("internet") || text.contains("network") || text.contains("dns")) {
            return new AiResult(
                    "NETWORK",
                    "HIGH",
                    "Network or connectivity disruption detected: " + title,
                    "1. Toggle Wi-Fi / Ethernet adapter off and on.\n2. Flush DNS cache via terminal (`ipconfig /flushdns`).\n3. Verify VPN client credentials and reconnect."
            );
        } else if (text.contains("password") || text.contains("login") || text.contains("account") || text.contains("lock") || text.contains("access") || text.contains("permission")) {
            return new AiResult(
                    "ACCOUNT",
                    "HIGH",
                    "Authentication or user access problem: " + title,
                    "1. Confirm user credentials and keyboard Caps Lock status.\n2. Use self-service password reset if active.\n3. Request IT Support Staff to unlock or refresh Active Directory account."
            );
        } else if (text.contains("printer") || text.contains("monitor") || text.contains("screen") || text.contains("keyboard") || text.contains("mouse") || text.contains("hardware") || text.contains("laptop")) {
            return new AiResult(
                    "HARDWARE",
                    "MEDIUM",
                    "Peripheral or hardware device fault: " + title,
                    "1. Check physical cable connections (HDMI/USB/power).\n2. Disconnect and reconnect docking station.\n3. Update device drivers via Device Manager."
            );
        } else if (text.contains("crash") || text.contains("error") || text.contains("outlook") || text.contains("excel") || text.contains("software") || text.contains("install") || text.contains("app")) {
            return new AiResult(
                    "SOFTWARE",
                    "MEDIUM",
                    "Application error or software instability: " + title,
                    "1. Force quit application and restart.\n2. Clear local application cache/temp files.\n3. Check for pending operating system and application updates."
            );
        } else {
            return new AiResult(
                    "OTHER",
                    "MEDIUM",
                    "General technical support request: " + title,
                    "1. Save your work and restart the system.\n2. Record exact error messages or screenshots.\n3. A Support Staff member will review and assist shortly."
            );
        }
    }
}
