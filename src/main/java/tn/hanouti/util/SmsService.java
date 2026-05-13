package tn.hanouti.util;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * WhatsApp notification service.
 *
 * Sends WhatsApp messages via a local Node.js microservice
 * running Baileys (whatsapp-service/index.js).
 *
 * ── SETUP ─────────────────────────────────────────────────────
 * 1. Open a terminal in the whatsapp-service/ folder
 * 2. Run: npm install
 * 3. Run: npm start
 * 4. Scan the QR code with your WhatsApp
 *    (WhatsApp → Appareils connectés → Connecter un appareil)
 * 5. Once connected, run the Java app normally
 *
 * The Node service must be running before the Java app sends messages.
 * ─────────────────────────────────────────────────────────────
 */
public class SmsService {

    private static final String ENDPOINT = "http://localhost:3000/send";

    /**
     * Sends a WhatsApp message to the client when the driver is nearby.
     *
     * @param toNumber   Client phone in E.164 format e.g. "+21657145353"
     * @param nomLivreur Driver name
     * @param idCommande Order number
     */
    public static void envoyerNotificationProximite(String toNumber,
                                                     String nomLivreur,
                                                     int idCommande) {
        if (toNumber == null || toNumber.isBlank()) {
            System.out.println("[WhatsApp] Numéro client manquant — message ignoré.");
            return;
        }

        try {
            String message = "🛵 *7anouti* — Votre livreur *" + nomLivreur +
                             "* arrive dans moins de 500m pour la commande *#" +
                             idCommande + "*.\n\nPréparez-vous à réceptionner votre colis ! 📦";

            String json = "{\"phone\":\"" + toNumber + "\","
                        + "\"message\":\"" + message.replace("\"", "\\\"")
                                                     .replace("\n", "\\n") + "\"}";

            URL url = new URL(ENDPOINT);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(json.getBytes(StandardCharsets.UTF_8));
            }

            int status = conn.getResponseCode();
            if (status == 200) {
                System.out.println("[WhatsApp] ✅ Message envoyé à " + toNumber);
            } else {
                System.err.println("[WhatsApp] Erreur HTTP " + status +
                        " — le service Node est-il démarré ?");
            }
            conn.disconnect();

        } catch (java.net.ConnectException e) {
            System.err.println("[WhatsApp] Service Node non démarré. " +
                    "Lancez: cd whatsapp-service && npm start");
        } catch (Exception e) {
            System.err.println("[WhatsApp] Erreur : " + e.getMessage());
        }
    }
}
