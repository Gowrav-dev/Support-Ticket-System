package com.helpdesk.service;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Properties;

/**
 * Service to classify tickets and generate troubleshooting steps from Groq LLM API.
 * 
 * Secure Key Resolution Priority:
 * 1. Environment variable: GROQ_API_KEY
 * 2. Local git-ignored .env file: GROQ_API_KEY=gsk_...
 * 3. System property: -DGROQ_API_KEY=...
 * 4. Configuration file: src/main/resources/application.properties (groq.api.key)
 */
public class AiService {

    private static final String[] GROQ_MODELS = {
            "openai/gpt-oss-120b",
            "openai/gpt-oss-20b",
            "qwen/qwen3.8-27b",
            "llama-3.3-70b-versatile",
            "llama-3.1-8b-instant"
    };

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
                return callGroqApiWithFallback(title, description, apiKey.trim());
            } catch (Exception e) {
                System.err.println("[AiService] Notice: Groq API call error: " + e.getMessage());
            }
        } else {
            System.out.println("[AiService] No Groq API key found. Using local smart diagnosis engine.");
        }

        // Seamless local diagnosis fallback when no key is set or API fails
        return generateLocalDiagnosis(title, description);
    }

    public String resolveApiKey() {
        // 1. Check system environment variable: GROQ_API_KEY
        String envKey = System.getenv("GROQ_API_KEY");
        if (envKey != null && !envKey.isBlank()) {
            return envKey.trim();
        }

        // 2. Check local git-ignored .env file in the project root
        String dotEnvKey = loadFromDotEnv();
        if (dotEnvKey != null && !dotEnvKey.isBlank()) {
            return dotEnvKey.trim();
        }

        // 3. Check system property: -DGROQ_API_KEY=...
        String propKey = System.getProperty("GROQ_API_KEY");
        if (propKey != null && !propKey.isBlank()) {
            return propKey.trim();
        }

        // 4. Check application.properties: groq.api.key
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("application.properties")) {
            if (is != null) {
                Properties props = new Properties();
                props.load(is);
                String fileKey = props.getProperty("groq.api.key");
                if (fileKey != null && !fileKey.isBlank() && !fileKey.startsWith("${") && !fileKey.startsWith("your_") && !fileKey.equalsIgnoreCase("placeholder")) {
                    return fileKey.trim();
                }
            }
        } catch (Exception ignored) {}

        return null;
    }

    /**
     * Robust, zero-dependency .env file parser.
     * Handles UTF-8 BOM, comments, whitespace around '=', and quotes.
     */
    private String loadFromDotEnv() {
        File dotEnv = new File(".env");
        if (!dotEnv.exists() || !dotEnv.isFile()) {
            return null;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(dotEnv))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.startsWith("\uFEFF")) {
                    line = line.substring(1).trim(); // Remove UTF-8 BOM
                }
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }

                int eqIdx = line.indexOf('=');
                if (eqIdx > 0) {
                    String key = line.substring(0, eqIdx).trim();
                    String val = line.substring(eqIdx + 1).trim();

                    if (key.equalsIgnoreCase("GROQ_API_KEY") || key.equalsIgnoreCase("GROQ_KEY")) {
                        // Strip inline comments if any
                        if (val.contains("#") && !val.startsWith("\"") && !val.startsWith("'")) {
                            val = val.substring(0, val.indexOf('#')).trim();
                        }
                        // Strip enclosing single or double quotes
                        if ((val.startsWith("\"") && val.endsWith("\"")) || (val.startsWith("'") && val.endsWith("'"))) {
                            if (val.length() >= 2) {
                                val = val.substring(1, val.length() - 1).trim();
                            }
                        }
                        if (!val.isBlank() && !val.equalsIgnoreCase("your_groq_api_key_here") && !val.startsWith("your_")) {
                            return val;
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("[AiService] Could not read .env file: " + e.getMessage());
        }

        return null;
    }

    private AiResult callGroqApiWithFallback(String title, String description, String apiKey) throws Exception {
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(8))
                .build();

        String systemPrompt = """
            You are an expert IT Helpdesk Support AI.
            Analyze the user's issue and return ONLY a valid JSON object with these 4 keys:
            {
              "category": "NETWORK" or "HARDWARE" or "SOFTWARE" or "ACCOUNT" or "OTHER",
              "priority": "LOW" or "MEDIUM" or "HIGH",
              "summary": "1 sentence technical summary of the issue",
              "suggested_solution": "Numbered step-by-step troubleshooting instructions tailored specifically to this issue"
            }
            Do not include any explanation or text outside the JSON object.
        """;

        String userPrompt = "Ticket Title: " + title + "\nTicket Description: " + description;

        for (String modelName : GROQ_MODELS) {
            try {
                JsonObject body = new JsonObject();
                body.addProperty("model", modelName);
                body.addProperty("temperature", 0.3);

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
                        .timeout(Duration.ofSeconds(12))
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
                    System.out.println("[AiService] Live Groq AI analysis completed using [" + modelName + "] for: " + title);
                    return new AiResult(
                            result.has("category") ? result.get("category").getAsString().toUpperCase() : "OTHER",
                            result.has("priority") ? result.get("priority").getAsString().toUpperCase() : "MEDIUM",
                            result.has("summary") ? result.get("summary").getAsString() : "AI diagnostic summary generated.",
                            result.has("suggested_solution") ? result.get("suggested_solution").getAsString() : "1. Restart application.\n2. Contact IT."
                    );
                } else if (response.statusCode() == 404) {
                    // Model not found on this Groq tier, try next model
                    System.err.println("[AiService] Model " + modelName + " not found (404), trying fallback model...");
                } else {
                    System.err.println("[AiService] Groq API returned HTTP " + response.statusCode() + ": " + response.body());
                    break;
                }
            } catch (Exception e) {
                System.err.println("[AiService] Attempt with " + modelName + " failed: " + e.getMessage());
            }
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
        } else if (text.contains("printer") || text.contains("monitor") || text.contains("screen") || text.contains("keyboard") || text.contains("mouse") || text.contains("hardware") || text.contains("laptop") || text.contains("dell") || text.contains("display")) {
            return new AiResult(
                    "HARDWARE",
                    "MEDIUM",
                    "Peripheral or hardware device fault: " + title,
                    "1. Check physical cable connections (HDMI/USB/power).\n2. Disconnect and reconnect docking station.\n3. Update device drivers via Device Manager."
            );
        } else if (text.contains("crash") || text.contains("error") || text.contains("outlook") || text.contains("excel") || text.contains("software") || text.contains("install") || text.contains("app") || text.contains("freeze")) {
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
