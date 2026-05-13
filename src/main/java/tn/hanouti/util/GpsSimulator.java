package tn.hanouti.util;

import tn.hanouti.dao.SuiviLivraisonDAO;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Simulates a driver moving toward the client step by step.
 *
 * Starts far away (>500m) and moves closer every 4 seconds.
 * When distance drops below 500m, the TrackingCarteController
 * detects it automatically and sends the WhatsApp message.
 *
 * Client is fixed at: 36.8190, 10.1658
 * Driver starts at:   36.8700, 10.1950  (~7 km away)
 * Driver ends at:     36.8195, 10.1663  (~60m away — inside 500m zone)
 */
public class GpsSimulator {

    // Client destination (must match TrackingCarteController.clientLat/Lon)
    private static final double CLIENT_LAT = 36.8190;
    private static final double CLIENT_LON = 10.1658;

    // Driver start position (~7 km from client)
    private static final double START_LAT = 36.8700;
    private static final double START_LON = 10.1950;

    // Driver end position (~60m from client — well inside 500m zone)
    private static final double END_LAT = 36.8195;
    private static final double END_LON = 10.1663;

    // Number of steps to move from start to end
    private static final int STEPS = 20;

    private static ScheduledExecutorService scheduler;

    /**
     * Starts the GPS simulation for a given suivi id.
     * Updates localisation_actuelle in DB every 4 seconds.
     * Stops automatically when the driver reaches the destination.
     *
     * @param idSuivi the id_suivi of the active delivery to simulate
     */
    public static void demarrer(int idSuivi) {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdownNow();
        }

        AtomicInteger step = new AtomicInteger(0);
        Connection cnx = DBConnection.getInstance().getCnx();

        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "gps-simulator");
            t.setDaemon(true);
            return t;
        });

        scheduler.scheduleAtFixedRate(() -> {
            int s = step.getAndIncrement();
            if (s > STEPS) {
                scheduler.shutdownNow();
                System.out.println("[GPS Sim] Simulation terminée.");
                return;
            }

            // Interpolate between start and end
            double ratio = (double) s / STEPS;
            double lat = START_LAT + (END_LAT - START_LAT) * ratio;
            double lon = START_LON + (END_LON - START_LON) * ratio;

            double distKm = HaversineService.calculerDistance(lat, lon, CLIENT_LAT, CLIENT_LON);

            try {
                String coords = lat + "," + lon;
                PreparedStatement ps = cnx.prepareStatement(
                        "UPDATE livraisons SET localisation_actuelle=? WHERE id_livraison=?");
                ps.setString(1, coords);
                ps.setInt(2, idSuivi);
                ps.executeUpdate();

                System.out.printf("[GPS Sim] Étape %d/%d — Position: %.4f,%.4f — Distance client: %.0fm%n",
                        s, STEPS, lat, lon, distKm * 1000);

            } catch (Exception e) {
                System.err.println("[GPS Sim] Erreur DB : " + e.getMessage());
            }

        }, 0, 4, TimeUnit.SECONDS);

        System.out.println("[GPS Sim] ▶ Simulation démarrée pour id_suivi=" + idSuivi);
        System.out.println("[GPS Sim] Le message WhatsApp sera envoyé automatiquement sous 500m.");
    }

    public static void arreter() {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdownNow();
            System.out.println("[GPS Sim] Simulation arrêtée.");
        }
    }
}
