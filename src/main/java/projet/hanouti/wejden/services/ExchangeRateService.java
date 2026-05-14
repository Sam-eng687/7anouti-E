package projet.hanouti.wejden.services;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Service de taux de change live via exchangerate-api.com
 * Taux de base : TND (Dinar Tunisien)
 * Cache de 1 heure pour eviter les appels excessifs
 */
public class ExchangeRateService {

    // API gratuite - 1500 req/mois
    private static final String API_URL = "https://api.exchangerate-api.com/v4/latest/TND";

    // Cache
    private static Map<String, Double> cachedRates = new HashMap<>();
    private static LocalDateTime lastFetch = null;
    private static final int CACHE_MINUTES = 60;

    // Taux de secours si API indisponible
    private static final Map<String, Double> FALLBACK_RATES = new HashMap<>();
    static {
        FALLBACK_RATES.put("EUR", 0.30);  // 1 TND ≈ 0.30 EUR
        FALLBACK_RATES.put("USD", 0.32);  // 1 TND ≈ 0.32 USD
        FALLBACK_RATES.put("GBP", 0.25);  // 1 TND ≈ 0.25 GBP
    }

    /** Retourne le taux TND → devise cible */
    public static double getRate(String targetCurrency) {
        refreshIfNeeded();
        return cachedRates.getOrDefault(targetCurrency,
               FALLBACK_RATES.getOrDefault(targetCurrency, 1.0));
    }

    /** Convertit un montant TND vers une devise */
    public static double convert(double amountTND, String targetCurrency) {
        return amountTND * getRate(targetCurrency);
    }

    /** Formate un montant converti avec symbole */
    public static String format(double amountTND, String currency) {
        double converted = convert(amountTND, currency);
        switch (currency) {
            case "EUR": return String.format("%.0f \u20AC", converted);
            case "USD": return String.format("%.0f $", converted);
            case "GBP": return String.format("%.0f \u00A3", converted);
            default:    return String.format("%.0f %s", converted, currency);
        }
    }

    /** Rafraichit le cache si necessaire */
    private static void refreshIfNeeded() {
        // Si cache valide, retourner immediatement
        if (lastFetch != null &&
            lastFetch.plusMinutes(CACHE_MINUTES).isAfter(LocalDateTime.now()) &&
            !cachedRates.isEmpty()) {
            return;
        }
        // Utiliser les taux de secours immediatement
        if (cachedRates.isEmpty()) {
            cachedRates = new HashMap<>(FALLBACK_RATES);
        }
        // Tenter l'API en arriere-plan (sans bloquer l'UI)
        fetchRates();
    }

    /** Appel API en arriere-plan */
    private static void fetchRates() {
        new Thread(() -> {
            try {
                URL url = new URL(API_URL);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(3000);
                conn.setReadTimeout(3000);

                if (conn.getResponseCode() == 200) {
                    BufferedReader br = new BufferedReader(
                        new InputStreamReader(conn.getInputStream()));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) sb.append(line);
                    br.close();

                    String json = sb.toString();
                    Map<String, Double> rates = new HashMap<>();

                    for (String currency : new String[]{"EUR", "USD", "GBP"}) {
                        String key = "\"" + currency + "\":";
                        int idx = json.indexOf(key);
                        if (idx >= 0) {
                            int start = idx + key.length();
                            int end = json.indexOf(",", start);
                            if (end < 0) end = json.indexOf("}", start);
                            try {
                                double rate = Double.parseDouble(json.substring(start, end).trim());
                                rates.put(currency, rate);
                            } catch (Exception ignored) {}
                        }
                    }

                    if (!rates.isEmpty()) {
                        cachedRates = rates;
                        lastFetch = LocalDateTime.now();
                        System.out.println("[Exchange] Taux live : EUR=" +
                            String.format("%.3f", rates.getOrDefault("EUR", 0.0)) +
                            " USD=" + String.format("%.3f", rates.getOrDefault("USD", 0.0)));
                    }
                }
                conn.disconnect();
            } catch (Exception e) {
                // Silencieux — on utilise les taux de secours
                if (lastFetch == null) {
                    cachedRates = new HashMap<>(FALLBACK_RATES);
                    lastFetch = LocalDateTime.now();
                }
            }
        }).start();
    }

    /** Indique si les taux sont frais (< 1h) */
    public static boolean isFresh() {
        return lastFetch != null &&
               lastFetch.plusMinutes(CACHE_MINUTES).isAfter(LocalDateTime.now());
    }

    /** Heure de la derniere mise a jour */
    public static String getLastUpdateTime() {
        if (lastFetch == null) return "Jamais";
        return lastFetch.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"));
    }
}
