package tn.hanouti.livreur.util;

public class HaversineService {

    // Rayon de la Terre en km
    private static final double RAYON_TERRE = 6371.0;

    // Coordonnées fixes du livreur (Esprit, El Ghazella, Ariana)
    private static final double LAT_LIVREUR = 36.8934;
    private static final double LON_LIVREUR = 10.1879;

    // ─────────────────────────────────────────────
    // CALCUL DE DISTANCE — formule Haversine
    // Retourne la distance en km entre deux points GPS
    // ─────────────────────────────────────────────
    public static double calculerDistance(double lat1, double lon1,
                                          double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return RAYON_TERRE * c;
    }

    // ─────────────────────────────────────────────
    // CALCUL DU TEMPS ESTIMÉ
    // Retourne un texte lisible : "Environ 15 min" ou "15 - 20 min"
    // ─────────────────────────────────────────────
    public static String estimerTemps(double latClient, double lonClient) {
        double distance = calculerDistance(LAT_LIVREUR, LON_LIVREUR, latClient, lonClient);
        int heure = java.time.LocalTime.now().getHour();

        // ── Vitesse selon la distance ──
        double vitesseKmh;
        if (distance < 5) {
            vitesseKmh = 30; // ville
        } else if (distance < 15) {
            vitesseKmh = 60; // banlieue
        } else {
            vitesseKmh = 80; // route
        }

        // ── Coefficient heure de pointe ──
        double coefficient = 1.0;
        if ((heure >= 7 && heure <= 9) || (heure >= 17 && heure <= 19)) {
            coefficient = 1.4; // trafic dense
        }

        // ── Calcul du temps en minutes ──
        double tempsBrut = (distance / vitesseKmh) * 60 * coefficient;
        int tempsMin = (int) Math.ceil(tempsBrut);

        // ── Fourchette de ±5 min ──
        int tempsMax = tempsMin + 5;

        // ── Format lisible ──
        String distanceTexte = String.format("%.1f km", distance);

        if (tempsMin < 5) {
            return "Distance : " + distanceTexte + " — Moins de 5 min";
        } else if (tempsMin < 60) {
            return "Distance : " + distanceTexte + " — " + tempsMin + " - " + tempsMax + " min";
        } else {
            int heures = tempsMin / 60;
            int minutes = tempsMin % 60;
            return "Distance : " + distanceTexte + " — Environ " + heures + "h" +
                    String.format("%02d", minutes);
        }
    }

    // ─────────────────────────────────────────────
    // EXTRAIRE LES MINUTES — pour calculer l'heure absolue
    // ─────────────────────────────────────────────
    public static int extraireMinutes(double latClient, double lonClient) {
        double distance = calculerDistance(LAT_LIVREUR, LON_LIVREUR, latClient, lonClient);
        int heure = java.time.LocalTime.now().getHour();

        double vitesseKmh;
        if (distance < 5) vitesseKmh = 30;
        else if (distance < 15) vitesseKmh = 60;
        else vitesseKmh = 80;

        double coefficient = 1.0;
        if ((heure >= 7 && heure <= 9) || (heure >= 17 && heure <= 19)) {
            coefficient = 1.4;
        }

        return (int) Math.ceil((distance / vitesseKmh) * 60 * coefficient);
    }
}
