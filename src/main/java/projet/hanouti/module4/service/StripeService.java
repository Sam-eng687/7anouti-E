package com.hanouti.hanoutiem4.service;

import javax.net.ssl.*;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.cert.X509Certificate;

/**
 * StripeService — intégration Stripe (mode test sandbox)
 * Remplace PaymeeService. L'interface publique est identique :
 *   - PaymentResult generatePayment(double, String, String)
 *   - boolean       verifyPayment(String)
 *
 * Stripe ne supporte pas le TND, on envoie le montant en EUR
 * (même valeur numérique). En sandbox tout est fictif.
 */
public class StripeService {

    // ⚠️ Clé secrète TEST uniquement — ne jamais exposer en production
    private static final String SECRET_KEY = "STRIPE_KEY_HERE";
    private static final String BASE_URL   = "https://api.stripe.com/v1";

    // ─────────────────────────────────────────────────────────────────────
    //  INNER CLASS — même structure que PaymeeService.PaymentResult
    // ─────────────────────────────────────────────────────────────────────
    public static class PaymentResult {
        public final boolean success;
        public final String  paymentToken;   // = Stripe Payment Intent ID (pi_...)
        public final String  payUrl;         // = lien Stripe Checkout
        public final String  errorMessage;

        public PaymentResult(String paymentToken, String payUrl) {
            this.success      = true;
            this.paymentToken = paymentToken;
            this.payUrl       = payUrl;
            this.errorMessage = null;
        }

        public PaymentResult(String errorMessage, boolean isError) {
            this.success      = false;
            this.paymentToken = null;
            this.payUrl       = null;
            this.errorMessage = errorMessage;
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    //  API PUBLIQUE
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Crée une Stripe Checkout Session et retourne l'URL de paiement.
     * Le montant est envoyé en EUR (même valeur numérique que le TND affiché).
     */
    public static PaymentResult generatePayment(double montantTND, String orderId, String email) {
        try {
            // Stripe travaille en centimes
            long amountCents = Math.round(montantTND * 100);
            String safeOrder = orderId.replace("\"", "").replace("&", "");
            String safeEmail = (email != null && !email.isBlank()) ? email : "client@7anouti.tn";

            // Paramètres de la Checkout Session (format URL-encoded)
            String body =
                    "line_items[0][price_data][currency]=eur"
                            + "&line_items[0][price_data][product_data][name]=Commande+7anouti-E"
                            + "&line_items[0][price_data][product_data][description]=" + urlEncode("Réf : " + safeOrder)
                            + "&line_items[0][price_data][unit_amount]=" + amountCents
                            + "&line_items[0][quantity]=1"
                            + "&mode=payment"
                            + "&customer_email=" + urlEncode(safeEmail)
                            + "&success_url=https://7anouti.tn/paiement/success?session_id={CHECKOUT_SESSION_ID}"
                            + "&cancel_url=https://7anouti.tn/paiement/fail"
                            + "&metadata[order_id]=" + urlEncode(safeOrder);

            System.out.println();
            System.out.println("=================================================================");
            System.out.println("  STRIPE PAYMENT API  --  CREATE CHECKOUT SESSION");
            System.out.println("=================================================================");
            System.out.println("  Endpoint : POST " + BASE_URL + "/checkout/sessions");
            System.out.println("  Montant  : " + montantTND + " TND → " + amountCents + " cents EUR");
            System.out.println("  OrderId  : " + safeOrder);
            System.out.println("=================================================================");

            URL url = new URL(BASE_URL + "/checkout/sessions");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Authorization", "Bearer " + SECRET_KEY);
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            conn.setConnectTimeout(10_000);
            conn.setReadTimeout(15_000);
            conn.setDoOutput(true);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(body.getBytes(StandardCharsets.UTF_8));
            }

            int code = conn.getResponseCode();
            if (code == 200 || code == 201) {
                String response = new String(conn.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
                System.out.println("[StripeService] Réponse : " + response);

                String sessionId  = extractJsonString(response, "id");
                String payUrl     = extractJsonString(response, "url");

                if (sessionId != null && payUrl != null) {
                    return new PaymentResult(sessionId, payUrl);
                }
                return new PaymentResult("Réponse Stripe invalide : champs manquants", true);
            }

            String errBody = new String(conn.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);
            System.err.println("[StripeService] Erreur HTTP " + code + " : " + errBody);
            String errMsg = extractJsonString(errBody, "message");
            return new PaymentResult("Erreur Stripe " + code + " : " + (errMsg != null ? errMsg : errBody), true);

        } catch (Exception e) {
            System.err.println("[StripeService] Exception : " + e.getMessage());
            return new PaymentResult("Erreur réseau : " + e.getMessage(), true);
        }
    }

    /**
     * Vérifie si la Checkout Session est payée.
     * paymentToken = session ID (cs_test_...)
     */
    public static boolean verifyPayment(String paymentToken) {
        try {
            URL url = new URL(BASE_URL + "/checkout/sessions/" + paymentToken);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Authorization", "Bearer " + SECRET_KEY);
            conn.setConnectTimeout(10_000);
            conn.setReadTimeout(15_000);

            int code = conn.getResponseCode();
            if (code == 200) {
                String response = new String(conn.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
                System.out.println("[StripeService] Vérification : " + response);
                String status = extractJsonString(response, "payment_status");
                return "paid".equals(status);
            }
            System.err.println("[StripeService] Vérification — HTTP " + code);
            return false;

        } catch (Exception e) {
            System.err.println("[StripeService] Exception vérification : " + e.getMessage());
            return false;
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    //  UTILITAIRES
    // ─────────────────────────────────────────────────────────────────────

    private static String urlEncode(String s) {
        try { return java.net.URLEncoder.encode(s, "UTF-8"); }
        catch (Exception e) { return s; }
    }

    private static String extractJsonString(String json, String key) {
        String marker = "\"" + key + "\"";
        int idx = json.indexOf(marker);
        if (idx < 0) return null;
        idx = json.indexOf('"', idx + marker.length() + 1);
        if (idx < 0) return null;
        StringBuilder sb = new StringBuilder();
        int i = idx + 1;
        while (i < json.length()) {
            char c = json.charAt(i);
            if (c == '\\' && i + 1 < json.length()) {
                char next = json.charAt(i + 1);
                if (next == '"')  { sb.append('"');  i += 2; continue; }
                if (next == 'n')  { sb.append('\n'); i += 2; continue; }
                if (next == '\\') { sb.append('\\'); i += 2; continue; }
            }
            if (c == '"') break;
            sb.append(c);
            i++;
        }
        return sb.toString();
    }
}
