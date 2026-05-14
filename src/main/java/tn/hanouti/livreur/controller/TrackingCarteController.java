package tn.hanouti.livreur.controller;

import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import tn.hanouti.livreur.dao.CommandeActiveDAO;
import tn.hanouti.livreur.model.CommandeActive;
import tn.hanouti.livreur.util.GpsSimulator;
import tn.hanouti.livreur.util.HaversineService;
import tn.hanouti.livreur.util.SmsService;

import java.sql.SQLException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Controller for the live order tracking screen.
 *
 * Architecture:
 *  - A WebView loads tracking.html (Leaflet map).
 *  - A background scheduler polls the DB every 4 seconds for the
 *    driver's latest GPS coordinates and ETA.
 *  - On each poll, it calls JavaScript functions in the WebView to
 *    move the driver marker and update the route polyline.
 *  - When the driver is within 500 m of the client, a WhatsApp
 *    notification is sent automatically (once only).
 *  - The "Simuler approche" button starts GpsSimulator which moves
 *    the driver from ~7 km to ~60 m in 20 steps of 4 seconds each.
 */
public class TrackingCarteController {

    // ── Map ──
    @FXML private WebView mapView;
    private WebEngine webEngine;
    private boolean mapReady = false;

    // ── Order info labels ──
    @FXML private Label labelNumeroCommande;
    @FXML private Label labelStatut;
    @FXML private Label labelEta;
    @FXML private Label labelAdresse;

    // ── Driver info labels ──
    @FXML private Label labelNomLivreur;
    @FXML private Label labelTelephone;
    @FXML private Label labelVehicule;

    // ── Progress dots ──
    @FXML private Label dot1;
    @FXML private Label dot2;
    @FXML private Label dot3;

    // ── Simulation button ──
    @FXML private Button btnSimuler;

    // ── Message ──
    @FXML private Label messageLabel;

    private CommandeActiveDAO dao = new CommandeActiveDAO();
    private CommandeActive commande;

    // Polling interval in seconds
    private static final int POLL_INTERVAL_SEC = 4;
    private ScheduledExecutorService pollScheduler;

    // GPS simulator
    private GpsSimulator gpsSimulator;

    // WhatsApp notification — sent only once per tracking session
    private boolean whatsappEnvoye = false;

    // Client destination (fixed Tunis landmark as placeholder)
    // In production this would be geocoded from adresseClient
    private static final double CLIENT_LAT = 36.8190;
    private static final double CLIENT_LON = 10.1658;

    // Default driver position (Tunis area) used when DB has no GPS yet
    private static final double DEFAULT_LAT = 36.8934;
    private static final double DEFAULT_LON = 10.1879;

    // WhatsApp proximity threshold in km
    private static final double SEUIL_WHATSAPP_KM = 0.5;

    // ─────────────────────────────────────────────
    // INIT
    // ─────────────────────────────────────────────
    @FXML
    public void initialize() {
        webEngine = mapView.getEngine();

        // Listen for the page to finish loading before calling JS
        webEngine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            if (newState == Worker.State.SUCCEEDED) {
                mapReady = true;
                if (commande != null) {
                    initialiserCarte();
                }
            }
        });

        // Load the tracking HTML page
        String url = getClass().getResource("/html/livreur/tracking.html").toExternalForm();
        webEngine.load(url);
    }

    // ─────────────────────────────────────────────
    // CALLED BY SuiviClientController after navigation
    // ─────────────────────────────────────────────
    public void chargerCommande(CommandeActive c) {
        this.commande = c;
        remplirInfos(c);

        // If the map is already loaded, initialize immediately
        if (mapReady) {
            initialiserCarte();
        }
        // Otherwise initialize() listener will call it once the page loads

        demarrerPolling();
    }

    // ─────────────────────────────────────────────
    // FILL STATIC INFO LABELS
    // ─────────────────────────────────────────────
    private void remplirInfos(CommandeActive c) {
        labelNumeroCommande.setText("Commande  #" + c.getIdCommande());
        labelAdresse.setText(c.getAdresseClient() != null ? c.getAdresseClient() : "—");
        labelEta.setText(c.getHeureEstimee() != null ? c.getHeureEstimee() : "—");
        labelNomLivreur.setText(c.getNomLivreur() != null ? c.getNomLivreur() : "—");
        labelTelephone.setText(c.getTelephoneLivreur() != null ? c.getTelephoneLivreur() : "—");
        labelVehicule.setText(c.getVehiculeEmoji() + "  " +
                (c.getGenreVehicule() != null ? c.getGenreVehicule() : "—"));

        mettreAJourStatut(c.getStatut());
    }

    // ─────────────────────────────────────────────
    // INITIALIZE MAP WITH CURRENT DATA
    // ─────────────────────────────────────────────
    private void initialiserCarte() {
        double[] driverCoords = parseCoords(commande.getLocalisationActuelle());
        double driverLat = driverCoords[0];
        double driverLon = driverCoords[1];

        String script = String.format(
                "initialiserCarte(%f, %f, %f, %f, '%s', '%s');",
                driverLat, driverLon,
                CLIENT_LAT, CLIENT_LON,
                escapeJs(commande.getNomLivreur()),
                escapeJs(commande.getAdresseClient())
        );
        executeJs(script);
    }

    // ─────────────────────────────────────────────
    // POLLING — DB every POLL_INTERVAL_SEC seconds
    // ─────────────────────────────────────────────
    private void demarrerPolling() {
        pollScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "tracking-poll");
            t.setDaemon(true);
            return t;
        });
        pollScheduler.scheduleAtFixedRate(this::pollMiseAJour,
                POLL_INTERVAL_SEC, POLL_INTERVAL_SEC, TimeUnit.SECONDS);
    }

    private void pollMiseAJour() {
        if (commande == null) return;
        try {
            String localisation = dao.getLocalisationActuelle(commande.getIdSuivi());
            String eta = dao.getHeureEstimee(commande.getIdSuivi());

            Platform.runLater(() -> {
                // Update ETA label
                if (eta != null) {
                    labelEta.setText(eta);
                    commande.setHeureEstimee(eta);
                }

                // Move driver marker on the map
                if (localisation != null && !localisation.isBlank()) {
                    double[] coords = parseCoords(localisation);
                    double lat = coords[0];
                    double lon = coords[1];

                    deplacerMarqueurLivreur(lat, lon);
                    commande.setLocalisationActuelle(localisation);

                    // ── WhatsApp proximity check ──
                    verifierProximiteWhatsApp(lat, lon);
                }
            });
        } catch (SQLException e) {
            Platform.runLater(() -> {
                if (messageLabel != null) {
                    messageLabel.setStyle("-fx-text-fill: #F472B6;");
                    messageLabel.setText("Erreur mise à jour : " + e.getMessage());
                }
            });
        }
    }

    // ─────────────────────────────────────────────
    // WHATSAPP PROXIMITY NOTIFICATION
    // Sent once when driver is within 500 m of the client
    // ─────────────────────────────────────────────
    private void verifierProximiteWhatsApp(double driverLat, double driverLon) {
        if (whatsappEnvoye) return;

        double distanceKm = HaversineService.calculerDistance(
                driverLat, driverLon, CLIENT_LAT, CLIENT_LON);

        if (distanceKm <= SEUIL_WHATSAPP_KM) {
            whatsappEnvoye = true; // mark before async call to avoid double-send

            // Numéro fixe du client — 22564847
            String telephone = "22564847";
            String nomLivreur = commande.getNomLivreur() != null
                    ? commande.getNomLivreur() : "votre livreur";
            int idCommande = commande.getIdCommande();

            String message = SmsService.construireMessageApproche(nomLivreur, idCommande);

            // Send in background thread — don't block the JavaFX thread
            Thread t = new Thread(() -> {
                boolean ok = SmsService.envoyerWhatsApp(telephone, message);
                Platform.runLater(() -> {
                    if (messageLabel != null) {
                        if (ok) {
                            messageLabel.setStyle("-fx-text-fill: #10B981;");
                            messageLabel.setText("📱 Notification WhatsApp envoyée !");
                        } else {
                            messageLabel.setStyle("-fx-text-fill: #F97316;");
                            messageLabel.setText("⚠️ WhatsApp non disponible (service Node.js arrêté ?)");
                        }
                    }
                });
            }, "whatsapp-sender");
            t.setDaemon(true);
            t.start();

            System.out.printf("[WhatsApp] Livreur à %.0f m — notification envoyée !%n",
                    distanceKm * 1000);
        }
    }

    // ─────────────────────────────────────────────
    // SIMULATION BUTTON — "🚗 Simuler approche (auto)"
    // ─────────────────────────────────────────────
    @FXML
    public void simulerApproche() {
        if (commande == null) return;

        if (gpsSimulator != null && gpsSimulator.isRunning()) {
            // Stop if already running
            gpsSimulator.arreter();
            if (btnSimuler != null) btnSimuler.setText("🚗  Simuler approche (auto)");
            if (messageLabel != null) {
                messageLabel.setStyle("-fx-text-fill: #8892B0;");
                messageLabel.setText("Simulation arrêtée.");
            }
            return;
        }

        // Reset WhatsApp flag so it fires again during this simulation
        whatsappEnvoye = false;

        gpsSimulator = new GpsSimulator(commande.getIdSuivi());
        gpsSimulator.demarrer();

        if (btnSimuler != null) btnSimuler.setText("⏹  Arrêter simulation");
        if (messageLabel != null) {
            messageLabel.setStyle("-fx-text-fill: #38BDF8;");
            messageLabel.setText("🚗 Simulation en cours — " +
                    String.format("%.1f km", GpsSimulator.getDistanceTotale()) +
                    " en 20 étapes de 4 s...");
        }
    }

    // ─────────────────────────────────────────────
    // JAVASCRIPT BRIDGE METHODS
    // ─────────────────────────────────────────────

    /**
     * Moves the driver marker to new coordinates and redraws the route.
     * Called on every poll cycle from the JavaFX thread.
     */
    private void deplacerMarqueurLivreur(double lat, double lon) {
        if (!mapReady) return;
        String script = String.format("deplacerLivreur(%f, %f);", lat, lon);
        executeJs(script);
    }

    private void executeJs(String script) {
        try {
            webEngine.executeScript(script);
        } catch (Exception e) {
            System.err.println("JS error: " + e.getMessage());
        }
    }

    // ─────────────────────────────────────────────
    // STATUS DISPLAY
    // ─────────────────────────────────────────────
    private void mettreAJourStatut(String statut) {
        if (labelStatut == null) return;
        if ("AFFECTEE".equals(statut)) {
            labelStatut.setText("En livraison");
            labelStatut.setStyle("-fx-background-color: rgba(56,189,248,0.15); " +
                    "-fx-text-fill: #38BDF8; -fx-background-radius: 20; -fx-padding: 4 14;");
            // Progress: step 2 active
            if (dot1 != null) dot1.setStyle("-fx-text-fill: #10B981; -fx-font-size: 18;");
            if (dot2 != null) dot2.setStyle("-fx-text-fill: #38BDF8; -fx-font-size: 18;");
            if (dot3 != null) dot3.setStyle("-fx-text-fill: #4A5577; -fx-font-size: 18;");
        } else if ("LIVREE".equals(statut)) {
            labelStatut.setText("Livrée ✅");
            labelStatut.setStyle("-fx-background-color: rgba(16,185,129,0.15); " +
                    "-fx-text-fill: #10B981; -fx-background-radius: 20; -fx-padding: 4 14;");
            if (dot1 != null) dot1.setStyle("-fx-text-fill: #10B981; -fx-font-size: 18;");
            if (dot2 != null) dot2.setStyle("-fx-text-fill: #10B981; -fx-font-size: 18;");
            if (dot3 != null) dot3.setStyle("-fx-text-fill: #10B981; -fx-font-size: 18;");
            arreterPolling();
        } else {
            labelStatut.setText("En attente");
            labelStatut.setStyle("-fx-background-color: rgba(249,115,22,0.15); " +
                    "-fx-text-fill: #F97316; -fx-background-radius: 20; -fx-padding: 4 14;");
        }
    }

    // ─────────────────────────────────────────────
    // CLEANUP
    // ─────────────────────────────────────────────
    public void arreterPolling() {
        if (pollScheduler != null && !pollScheduler.isShutdown()) {
            pollScheduler.shutdownNow();
        }
    }

    // ─────────────────────────────────────────────
    // HELPERS
    // ─────────────────────────────────────────────

    /**
     * Parses a "lat,lon" string. Returns default Tunis coords on failure.
     */
    private double[] parseCoords(String localisationActuelle) {
        if (localisationActuelle != null && localisationActuelle.contains(",")) {
            try {
                String[] parts = localisationActuelle.split(",");
                return new double[]{
                        Double.parseDouble(parts[0].trim()),
                        Double.parseDouble(parts[1].trim())
                };
            } catch (NumberFormatException ignored) {}
        }
        return new double[]{DEFAULT_LAT, DEFAULT_LON};
    }

    /** Escapes single quotes for safe JS string injection. */
    private String escapeJs(String s) {
        if (s == null) return "";
        return s.replace("'", "\\'");
    }
}

