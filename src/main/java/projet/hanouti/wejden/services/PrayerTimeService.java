package projet.hanouti.wejden.services;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service pour récupérer les horaires de prière depuis l'API Aladhan (gratuite, sans clé).
 * Ville par défaut : Tunis, Tunisie (méthode de calcul : ISNA).
 */
public class PrayerTimeService {

    private static final String API_URL =
        "https://api.aladhan.com/v1/timingsByCity?city=Tunis&country=Tunisia&method=2";

    /**
     * Récupère les horaires de prière du jour.
     * @return Map ordonnée : nom de la prière → heure (ex: "Fajr" → "04:23")
     */
    public Map<String, String> getTodayPrayerTimes() {
        Map<String, String> times = new LinkedHashMap<>();
        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(java.time.Duration.ofSeconds(10))
                    .build();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL))
                    .header("Accept", "application/json")
                    .header("User-Agent", "Mozilla/5.0 (HanoutiDashboard/1.0)")
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                String body = response.body();

                // Extraire le bloc "timings" du JSON
                String[] prayerNames = {"Fajr", "Sunrise", "Dhuhr", "Asr", "Maghrib", "Isha"};
                for (String name : prayerNames) {
                    Pattern pattern = Pattern.compile("\"" + name + "\"\\s*:\\s*\"([^\"]+)\"");
                    Matcher matcher = pattern.matcher(body);
                    if (matcher.find()) {
                        String time = matcher.group(1).split(" ")[0]; // Prend "HH:mm" même si y'a "(EET)"
                        times.put(name, time);
                    }
                }

                // Extraire la date Hijri
                Pattern hijriPattern = Pattern.compile("\"date\"\\s*:\\s*\"([\\d\\-]+)\"[^}]*\"hijri\"");
                Matcher hijriMatcher = hijriPattern.matcher(body);
                if (hijriMatcher.find()) {
                    times.put("_hijriDate", hijriMatcher.group(1));
                }

                // Extraire le mois Hijri (en)
                Pattern monthPattern = Pattern.compile("\"month\"\\s*:\\s*\\{[^}]*\"en\"\\s*:\\s*\"([^\"]+)\"");
                Matcher monthMatcher = monthPattern.matcher(body);
                if (monthMatcher.find()) {
                    times.put("_hijriMonth", monthMatcher.group(1));
                }
            }
        } catch (Exception e) {
            System.err.println("Erreur API Prière: " + e.getMessage());
        }

        // --- VALEURS DE SECOURS (TUNIS) SI L'API ÉCHOUE ---
        if (times.size() < 5) {
            times.put("Fajr", "04:15");
            times.put("Dhuhr", "12:20");
            times.put("Asr", "16:05");
            times.put("Maghrib", "19:25");
            times.put("Isha", "21:00");
            times.put("_hijriDate", "1445");
            times.put("_hijriMonth", "Ramadan");
        }
        return times;
    }

    /**
     * Détermine la prochaine prière à partir de l'heure actuelle.
     * @return Le nom de la prochaine prière, ou "Fajr" si toutes sont passées.
     */
    public String getNextPrayer(Map<String, String> times) {
        try {
            java.time.LocalTime now = java.time.LocalTime.now();
            String[] prayers = {"Fajr", "Sunrise", "Dhuhr", "Asr", "Maghrib", "Isha"};
            for (String prayer : prayers) {
                String timeStr = times.get(prayer);
                if (timeStr != null) {
                    String[] parts = timeStr.split(":");
                    java.time.LocalTime prayerTime = java.time.LocalTime.of(
                        Integer.parseInt(parts[0]), Integer.parseInt(parts[1]));
                    if (now.isBefore(prayerTime)) {
                        return prayer;
                    }
                }
            }
        } catch (Exception e) {
            // Ignore
        }
        return "Fajr"; // Toutes les prières sont passées → prochaine = Fajr de demain
    }
}
