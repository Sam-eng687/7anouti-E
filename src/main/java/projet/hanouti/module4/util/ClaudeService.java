package com.hanouti.hanoutiem4.util;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * Service IA — utilise Groq API (gratuit, rapide).
 * Modèle : llama-3.3-70b-versatile
 */
public class ClaudeService {

    // ⚠️ Remplace par ta clé Groq : https://console.groq.com → API Keys
    private static final String API_KEY    = "GROQ_API_KEY_HERE";
    private static final String API_URL    = "https://api.groq.com/openai/v1/chat/completions";
    private static final String MODEL      = "llama-3.3-70b-versatile";
    private static final int    TIMEOUT_MS = 20_000;

    public static String ask(String systemPrompt, String userMessage) {
        if (API_KEY.startsWith("REMPLACE") || API_KEY.isBlank()) {
            System.err.println("[Groq] Clé API non configurée dans ClaudeService.java");
            return null;
        }
        try {
            String body = "{"
                    + "\"model\":\"" + MODEL + "\","
                    + "\"max_tokens\":800,"
                    + "\"messages\":["
                    +   "{\"role\":\"system\",\"content\":\"" + escapeJson(systemPrompt) + "\"},"
                    +   "{\"role\":\"user\",\"content\":\"" + escapeJson(userMessage) + "\"}"
                    + "]}";

            URL url = new URL(API_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setConnectTimeout(TIMEOUT_MS);
            conn.setReadTimeout(TIMEOUT_MS);
            conn.setRequestProperty("Content-Type",  "application/json");
            conn.setRequestProperty("Authorization", "Bearer " + API_KEY);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(body.getBytes(StandardCharsets.UTF_8));
            }

            int status = conn.getResponseCode();
            java.io.InputStream stream = (status == 200)
                    ? conn.getInputStream()
                    : conn.getErrorStream();

            String response = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            stream.close();

            if (status != 200) {
                System.err.println("[Groq] Erreur HTTP " + status + ": " + response);
                return null;
            }

            // Groq retourne format OpenAI : choices[0].message.content
            return extractGroqText(response);

        } catch (Exception e) {
            System.err.println("[Groq] Exception: " + e.getMessage());
            return null;
        }
    }

    private static String extractGroqText(String json) {
        // {"choices":[{"message":{"content":"..."}}]}
        String marker = "\"content\":\"";
        int start = json.indexOf(marker);
        if (start == -1) return null;
        start += marker.length();

        StringBuilder sb = new StringBuilder();
        for (int i = start; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '\\' && i + 1 < json.length()) {
                char next = json.charAt(i + 1);
                switch (next) {
                    case '"'  -> { sb.append('"');  i++; }
                    case 'n'  -> { sb.append('\n'); i++; }
                    case 't'  -> { sb.append('\t'); i++; }
                    case 'r'  -> { sb.append('\r'); i++; }
                    case '\\' -> { sb.append('\\'); i++; }
                    default   -> sb.append(c);
                }
            } else if (c == '"') {
                break;
            } else {
                sb.append(c);
            }
        }
        return sb.toString().trim();
    }

    private static String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
