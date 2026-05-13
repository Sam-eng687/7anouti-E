package edu.hanouti.services;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service pour récupérer la météo actuelle depuis l'API Open-Meteo (gratuite, sans clé).
 * Coordonnées par défaut : Tunis, Tunisie.
 */
public class WeatherService {

    private static final String API_URL =
        "https://api.open-meteo.com/v1/forecast?" +
        "latitude=36.8065&longitude=10.1815" +
        "&current=temperature_2m,relative_humidity_2m,apparent_temperature,weather_code,wind_speed_10m" +
        "&daily=temperature_2m_max,temperature_2m_min,weather_code,sunrise,sunset" +
        "&timezone=Africa%2FTunis&forecast_days=5";

    /**
     * Récupère les données météo actuelles et les prévisions sur 5 jours.
     * @return Map contenant les données météo.
     */
    public Map<String, Object> getCurrentWeather() {
        Map<String, Object> weather = new LinkedHashMap<>();
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL))
                    .header("Accept", "application/json")
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                String body = response.body();

                // Dashboard attend: temp (double), code (int), humidity (double), wind (double)
                try {
                    weather.put("temp", Double.parseDouble(extractJsonValue(body, "temperature_2m", true)));
                    weather.put("code", Integer.parseInt(extractJsonValue(body, "weather_code", true)));
                    weather.put("humidity", Double.parseDouble(extractJsonValue(body, "relative_humidity_2m", true)));
                    weather.put("wind", Double.parseDouble(extractJsonValue(body, "wind_speed_10m", true)));
                    weather.put("apparent_temp", Double.parseDouble(extractJsonValue(body, "apparent_temperature", true)));
                } catch (Exception e) {
                    System.err.println("Erreur parsing météo: " + e.getMessage());
                }

                // Daily forecast (arrays) - gardés en String pour l'instant ou transformés si besoin
                weather.put("daily_max", extractJsonArray(body, "temperature_2m_max"));
                weather.put("daily_min", extractJsonArray(body, "temperature_2m_min"));
                weather.put("daily_codes", extractJsonArray(body, "weather_code"));
                weather.put("daily_dates", extractJsonArray(body, "\"time\""));

                // Sunrise/sunset
                String sunriseArr = extractJsonArray(body, "sunrise");
                String sunsetArr = extractJsonArray(body, "sunset");
                if (sunriseArr != null && !sunriseArr.isEmpty()) {
                    String[] parts = sunriseArr.split(",");
                    if (parts.length > 0) {
                        String sr = parts[0].trim().replace("\"", "");
                        if (sr.contains("T"))
                            weather.put("sunrise", sr.substring(sr.indexOf("T") + 1));
                    }
                }
                if (sunsetArr != null && !sunsetArr.isEmpty()) {
                    String[] parts = sunsetArr.split(",");
                    if (parts.length > 0) {
                        String ss = parts[0].trim().replace("\"", "");
                        if (ss.contains("T"))
                            weather.put("sunset", ss.substring(ss.indexOf("T") + 1));
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Erreur réseau météo: " + e.getMessage());
        }
        return weather;
    }

    /**
     * Retourne l'icône emoji correspondant au code météo WMO.
     */
    public static String getWeatherEmoji(int code) {
        if (code == 0) return "☀️";
        if (code <= 3) return "⛅";
        if (code <= 48) return "🌫️";
        if (code <= 57) return "🌧️";
        if (code <= 67) return "🌧️";
        if (code <= 77) return "❄️";
        if (code <= 82) return "🌧️";
        if (code <= 86) return "🌨️";
        if (code <= 99) return "⛈️";
        return "🌤️";
    }

    /**
     * Retourne une description textuelle du code météo WMO.
     */
    public static String getWeatherDescription(int code) {
        if (code == 0) return "Ciel dégagé";
        if (code == 1) return "Peu nuageux";
        if (code == 2) return "Partiellement nuageux";
        if (code == 3) return "Couvert";
        if (code <= 48) return "Brouillard";
        if (code <= 55) return "Bruine";
        if (code <= 57) return "Bruine verglaçante";
        if (code <= 65) return "Pluie";
        if (code <= 67) return "Pluie verglaçante";
        if (code <= 75) return "Neige";
        if (code == 77) return "Grains de neige";
        if (code <= 82) return "Averses";
        if (code <= 86) return "Averses de neige";
        if (code == 95) return "Orage";
        if (code <= 99) return "Orage avec grêle";
        return "Variable";
    }

    /** Extraire une valeur simple du JSON dans le bloc "current" */
    private String extractJsonValue(String json, String key, boolean fromCurrent) {
        try {
            // Chercher dans le bloc "current"
            String searchIn = json;
            if (fromCurrent) {
                int currentIdx = json.indexOf("\"current\"");
                if (currentIdx >= 0) {
                    searchIn = json.substring(currentIdx);
                    int endBrace = searchIn.indexOf("}");
                    if (endBrace >= 0) searchIn = searchIn.substring(0, endBrace + 1);
                }
            }
            Pattern p = Pattern.compile("\"" + key + "\"\\s*:\\s*([\\d.\\-]+)");
            Matcher m = p.matcher(searchIn);
            if (m.find()) return m.group(1);
        } catch (Exception e) { /* ignore */ }
        return "";
    }

    /** Extraire un tableau JSON brut */
    private String extractJsonArray(String json, String key) {
        try {
            // Pour "daily" arrays
            int keyIdx = json.lastIndexOf(key);
            if (keyIdx < 0) keyIdx = json.indexOf(key);
            if (keyIdx >= 0) {
                int bracketStart = json.indexOf("[", keyIdx);
                if (bracketStart >= 0) {
                    int bracketEnd = json.indexOf("]", bracketStart);
                    if (bracketEnd >= 0) {
                        return json.substring(bracketStart + 1, bracketEnd);
                    }
                }
            }
        } catch (Exception e) { /* ignore */ }
        return "";
    }
}
