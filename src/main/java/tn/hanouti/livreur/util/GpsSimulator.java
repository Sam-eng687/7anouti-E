package tn.hanouti.livreur.util;


import tn.hanouti.util.DBConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;

/**
 * Simulates a delivery driver approaching the client's address.
 *
 * The simulator moves the driver from a starting position (~7 km away)
 * to a final position (~60 m from the client) in 20 steps.
 * Each step updates the GPS coordinates in the database so the live
 * tracking map picks them up on the next poll cycle (every 4 seconds).
 *
 * Usage:
 *   GpsSimulator sim = new GpsSimulator(idSuivi);
 *   sim.setOnStep((lat, lon) -> { ... });   // optional callback per step
 *   sim.demarrer();
 *   // later:
 *   sim.arreter();
 */
public class GpsSimulator {

    // ── Simulation parameters ──────────────────────────────────
    /** Starting position — ~7 km north-east of the client */
    private static final double START_LAT = 36.8700;
    private static final double START_LON = 10.1950;

    /** Ending position — ~60 m from the client */
    private static final double END_LAT = 36.8195;
    private static final double END_LON = 10.1663;

    /** Number of interpolation steps */
    private static final int STEPS = 20;

    /** Interval between steps in seconds — matches the polling interval */
    private static final int STEP_INTERVAL_SEC = 4;

    // ── State ──────────────────────────────────────────────────
    private final int idSuivi;
    private int currentStep = 0;
    private ScheduledExecutorService scheduler;

    /** Optional callback invoked on the simulation thread after each DB update */
    private BiConsumer<Double, Double> onStep;

    // ─────────────────────────────────────────────
    // CONSTRUCTOR
    // ─────────────────────────────────────────────
    public GpsSimulator(int idSuivi) {
        this.idSuivi = idSuivi;
    }

    public void setOnStep(BiConsumer<Double, Double> callback) {
        this.onStep = callback;
    }

    // ─────────────────────────────────────────────
    // START
    // ─────────────────────────────────────────────
    public void demarrer() {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdownNow();
        }
        currentStep = 0;
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "gps-simulator");
            t.setDaemon(true);
            return t;
        });
        scheduler.scheduleAtFixedRate(this::avancer,
                0, STEP_INTERVAL_SEC, TimeUnit.SECONDS);
        System.out.println("[GpsSimulator] ▶ Simulation démarrée pour suivi #" + idSuivi);
    }

    // ─────────────────────────────────────────────
    // STOP
    // ─────────────────────────────────────────────
    public void arreter() {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdownNow();
            System.out.println("[GpsSimulator] ⏹ Simulation arrêtée.");
        }
    }

    public boolean isRunning() {
        return scheduler != null && !scheduler.isShutdown();
    }

    // ─────────────────────────────────────────────
    // STEP — interpolate and write to DB
    // ─────────────────────────────────────────────
    private void avancer() {
        if (currentStep > STEPS) {
            arreter();
            return;
        }

        double ratio = (double) currentStep / STEPS;
        double lat = START_LAT + (END_LAT - START_LAT) * ratio;
        double lon = START_LON + (END_LON - START_LON) * ratio;

        // Write to DB
        try {
            mettreAJourPosition(lat, lon);
        } catch (SQLException e) {
            System.err.println("[GpsSimulator] Erreur DB : " + e.getMessage());
        }

        // Notify listener
        if (onStep != null) {
            onStep.accept(lat, lon);
        }

        System.out.printf("[GpsSimulator] Étape %d/%d → %.4f, %.4f%n",
                currentStep, STEPS, lat, lon);

        currentStep++;
    }

    // ─────────────────────────────────────────────
    // DB UPDATE
    // ─────────────────────────────────────────────
    private void mettreAJourPosition(double lat, double lon) throws SQLException {
        Connection cnx = DBConnection.getInstance().getCnx();
        String sql = "UPDATE Suivi_Livraison SET localisation_actuelle = ? WHERE id_suivi = ?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setString(1, lat + "," + lon);
        ps.setInt(2, idSuivi);
        ps.executeUpdate();
        ps.close();
    }

    // ─────────────────────────────────────────────
    // STATIC HELPERS — for use without an instance
    // ─────────────────────────────────────────────

    /** Returns the starting GPS coordinates as a "lat,lon" string. */
    public static String getStartPosition() {
        return START_LAT + "," + START_LON;
    }

    /** Returns the ending GPS coordinates as a "lat,lon" string. */
    public static String getEndPosition() {
        return END_LAT + "," + END_LON;
    }

    /** Distance in km between start and end (for display). */
    public static double getDistanceTotale() {
        return HaversineService.calculerDistance(START_LAT, START_LON, END_LAT, END_LON);
    }
}

