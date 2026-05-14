package tn.hanouti.livreur.util;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * Sends a WhatsApp message via the local Node.js / Baileys microservice.
 *
 * The service must be running at http://localhost:3000
 * Start it with:  cd whatsapp-service && npm start
 *
 * API:  POST /send   { "phone": "+21628650563", "message": "..." }
 */
public class SmsService {

    private static final String SERVICE_URL = "http://localhost:3000/send";

    // ─────────────────────────────────────────────
    // SEND WHATSAPP MESSAGE
    // Returns true if the HTTP call succeeded (2xx), false otherwise.
    // Never throws — failures are logged to stderr only.
    // ─────────────────────────────────────────────
    public static boolean envoyerWhatsApp(String telephone, String message) {
        try {
            // Normalise phone: strip spaces, add +216 prefix if needed
            String phone = normaliserTelephone(telephone);

            // Build JSON body
            String json = "{\"phone\":\"" + escapeJson(phone) + "\"," +
                          "\"message\":\"" + escapeJson(message) + "\"}";

            URL url = new URL(SERVICE_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            conn.setConnectTimeout(3000);
            conn.setReadTimeout(5000);
            conn.setDoOutput(true);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(json.getBytes(StandardCharsets.UTF_8));
            }

            int code = conn.getResponseCode();
            conn.disconnect();

            if (code >= 200 && code < 300) {
                System.out.println("[WhatsApp] ✅ Message envoyé à " + phone);
                return true;
            } else {
                System.err.println("[WhatsApp] ⚠️ HTTP " + code + " pour " + phone);
                return false;
            }

        } catch (Exception e) {
            System.err.println("[WhatsApp] ❌ Erreur envoi : " + e.getMessage());
            return false;
        }
    }

    // ─────────────────────────────────────────────
    // BUILD THE PROXIMITY NOTIFICATION MESSAGE
    // ─────────────────────────────────────────────
    public static String construireMessageApproche(String nomLivreur, int idCommande) {
        return "🛵 7anouti — Votre livreur " + nomLivreur +
               " arrive dans moins de 500m\npour la commande #" + idCommande + ".\n\n" +
               "Préparez-vous à réceptionner votre colis ! 📦";
    }

    // ─────────────────────────────────────────────
    // HELPERS
    // ─────────────────────────────────────────────

    /**
     * Adds +216 prefix for Tunisian numbers if not already international.
     */
    private static String normaliserTelephone(String tel) {
        if (tel == null || tel.isBlank()) return "+21600000000";
        String clean = tel.replaceAll("\\s+", "");
        if (clean.startsWith("+")) return clean;
        if (clean.startsWith("00")) return "+" + clean.substring(2);
        // Assume Tunisian 8-digit number
        return "+216" + clean;
    }

    /** Escapes characters that would break a JSON string literal. */
    private static String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}

