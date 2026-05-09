package projet.hanouti.GestionCommandes.services;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Service de validation des adresses via l'API OpenStreetMap (Nominatim).
 *
 * Utilisé côté acheteur pour vérifier l'adresse lors d'une modification,
 * et côté vendeur pour bloquer la confirmation si l'adresse est invalide.
 *
 * API gratuite, pas de clé requise.
 * Documentation : https://nominatim.openstreetmap.org/
 */
public class AdresseValidationService {

    private static final String NOMINATIM_URL = "https://nominatim.openstreetmap.org/search";
    private static final String USER_AGENT    = "7hanoutiE-App/1.0";

    // =========================================================
    // VALIDATION
    // =========================================================

    /**
     * Vérifie si une adresse est valide en la soumettant à l'API Nominatim.
     * L'adresse est considérée valide si l'API retourne au moins un résultat.
     *
     * @param adresse adresse à valider (ex: "12 Rue de la Liberté, Tunis")
     * @return true si l'adresse est reconnue, false sinon
     */
    public boolean validerAdresse(String adresse) {
        if (adresse == null || adresse.isBlank()) return false;

        try {
            String encoded = URLEncoder.encode(adresse, StandardCharsets.UTF_8);
            String urlStr  = NOMINATIM_URL + "?q=" + encoded + "&format=json&limit=1";

            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("User-Agent", USER_AGENT);
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            int responseCode = conn.getResponseCode();
            if (responseCode != 200) return false;

            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8)
            );
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();

            // Si la réponse est "[]" → aucun résultat → adresse invalide
            String body = response.toString().trim();
            return !body.equals("[]") && body.startsWith("[");

        } catch (Exception e) {
            System.err.println("[AdresseValidationService.validerAdresse] Erreur API : " + e.getMessage());
            // En cas d'erreur réseau → on laisse passer (fail-open)
            return true;
        }
    }

    // =========================================================
    // NORMALISATION
    // =========================================================

    /**
     * Normalise une adresse en récupérant son affichage officiel via Nominatim.
     * Retourne le display_name retourné par l'API, ou l'adresse originale en cas d'échec.
     *
     * @param adresse adresse brute saisie par l'utilisateur
     * @return adresse normalisée ou adresse originale si l'API échoue
     */
    public String normaliserAdresse(String adresse) {
        if (adresse == null || adresse.isBlank()) return adresse;

        try {
            String encoded = URLEncoder.encode(adresse, StandardCharsets.UTF_8);
            String urlStr  = NOMINATIM_URL + "?q=" + encoded + "&format=json&limit=1";

            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("User-Agent", USER_AGENT);
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            if (conn.getResponseCode() != 200) return adresse;

            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8)
            );
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) response.append(line);
            reader.close();

            String body = response.toString().trim();

            // Parse simple du display_name sans dépendance externe
            // Format retourné : [{"display_name": "...", ...}, ...]
            if (body.startsWith("[{")) {
                int start = body.indexOf("\"display_name\":\"");
                if (start != -1) {
                    start += "\"display_name\":\"".length();
                    int end = body.indexOf("\"", start);
                    if (end != -1) {
                        return body.substring(start, end);
                    }
                }
            }

        } catch (Exception e) {
            System.err.println("[AdresseValidationService.normaliserAdresse] Erreur API : " + e.getMessage());
        }

        return adresse; // fallback : retourne l'adresse originale
    }
}