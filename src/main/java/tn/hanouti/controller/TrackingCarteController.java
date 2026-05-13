package tn.hanouti.controller;

import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import tn.hanouti.dao.CommandeActiveDAO;
import tn.hanouti.model.CommandeActive;
import tn.hanouti.util.GpsSimulator;
import tn.hanouti.util.HaversineService;
import tn.hanouti.util.SmsService;

import java.io.IOException;
import java.sql.SQLException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

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

    // ── Message ──
    @FXML private Label messageLabel;

    private CommandeActiveDAO dao = new CommandeActiveDAO();
    private CommandeActive commande;

    private static final int POLL_INTERVAL_SEC = 4;
    private ScheduledExecutorService pollScheduler;

    private static final double DEFAULT_LAT = 36.8934;
    private static final double DEFAULT_LON = 10.1879;

    // WhatsApp proximity alert — sent once when driver is within 500m
    private static final double SMS_THRESHOLD_KM = 0.5;
    private boolean smsSent = false;

    // Client destination coords
    private double clientLat = 36.8190;
    private double clientLon = 10.1658;

    // ─────────────────────────────────────────────
    // INIT
    // ─────────────────────────────────────────────
    @FXML
    public void initialize() {
        webEngine = mapView.getEngine();
        webEngine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            if (newState == Worker.State.SUCCEEDED) {
                mapReady = true;
                if (commande != null) initialiserCarte();
            }
        });
        String url = getClass().getResource("/html/tracking.html").toExternalForm();
        webEngine.load(url);
    }

    // ─────────────────────────────────────────────
    // CALLED BY SuiviClientController
    // ─────────────────────────────────────────────
    public void chargerCommande(CommandeActive c) {
        this.commande = c;
        remplirInfos(c);
        if (mapReady) initialiserCarte();
        demarrerPolling();
    }

    // ─────────────────────────────────────────────
    // FILL INFO LABELS
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
    // INITIALIZE MAP
    // ─────────────────────────────────────────────
    private void initialiserCarte() {
        double[] driverCoords = parseCoords(commande.getLocalisationActuelle());
        String script = String.format(
                "initialiserCarte(%f, %f, %f, %f, '%s', '%s');",
                driverCoords[0], driverCoords[1],
                clientLat, clientLon,
                escapeJs(commande.getNomLivreur()),
                escapeJs(commande.getAdresseClient())
        );
        executeJs(script);
    }

    // ─────────────────────────────────────────────
    // POLLING — every 4 seconds
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
                if (eta != null) {
                    labelEta.setText(eta);
                    commande.setHeureEstimee(eta);
                }
                if (localisation != null && !localisation.isBlank()) {
                    double[] coords = parseCoords(localisation);
                    double driverLat = coords[0];
                    double driverLon = coords[1];

                    deplacerMarqueurLivreur(driverLat, driverLon);
                    commande.setLocalisationActuelle(localisation);

                    // ── WhatsApp proximity check ──────────────────
                    if (!smsSent) {
                        double distanceKm = HaversineService.calculerDistance(
                                driverLat, driverLon, clientLat, clientLon);
                        if (distanceKm <= SMS_THRESHOLD_KM) {
                            smsSent = true;
                            String tel = commande.getTelephoneClient();
                            String nom = commande.getNomLivreur();
                            int cmd = commande.getIdCommande();
                            new Thread(() ->
                                SmsService.envoyerNotificationProximite(tel, nom, cmd),
                                "whatsapp-sender"
                            ).start();
                            if (messageLabel != null) {
                                messageLabel.setStyle("-fx-text-fill: #10B981;");
                                messageLabel.setText("📱 WhatsApp envoyé — livreur à " +
                                        String.format("%.0f", distanceKm * 1000) + "m !");
                            }
                        }
                    }
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
    // JAVASCRIPT BRIDGE
    // ─────────────────────────────────────────────
    private void deplacerMarqueurLivreur(double lat, double lon) {
        if (!mapReady) return;
        executeJs(String.format("deplacerLivreur(%f, %f);", lat, lon));
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
    // SIMULATE DRIVER APPROACHING
    // ─────────────────────────────────────────────
    @FXML
    public void simulerApproche() {
        if (commande == null) return;
        smsSent = false;
        if (messageLabel != null) {
            messageLabel.setStyle("-fx-text-fill: #10B981;");
            messageLabel.setText("🚗 Simulation démarrée — WhatsApp envoyé automatiquement sous 500m...");
        }
        GpsSimulator.demarrer(commande.getIdSuivi());
    }

    // ─────────────────────────────────────────────
    // NAVIGATION
    // ─────────────────────────────────────────────
    @FXML
    public void retourListe() {
        arreterPolling();
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/SuiviClient.fxml"));
            Parent root = loader.load();
            Scene scene = new Scene(root);
            scene.getStylesheets().addAll(mapView.getScene().getStylesheets());
            Stage stage = (Stage) mapView.getScene().getWindow();
            stage.setScene(scene);
        } catch (IOException e) {
            if (messageLabel != null) {
                messageLabel.setStyle("-fx-text-fill: #F472B6;");
                messageLabel.setText("Erreur navigation : " + e.getMessage());
            }
        }
    }

    // ─────────────────────────────────────────────
    // CLEANUP
    // ─────────────────────────────────────────────
    public void arreterPolling() {
        if (pollScheduler != null && !pollScheduler.isShutdown()) {
            pollScheduler.shutdownNow();
        }
        GpsSimulator.arreter();
    }

    // ─────────────────────────────────────────────
    // HELPERS
    // ─────────────────────────────────────────────
    private double[] parseCoords(String loc) {
        if (loc != null && loc.contains(",")) {
            try {
                String[] parts = loc.split(",");
                return new double[]{
                        Double.parseDouble(parts[0].trim()),
                        Double.parseDouble(parts[1].trim())
                };
            } catch (NumberFormatException ignored) {}
        }
        return new double[]{DEFAULT_LAT, DEFAULT_LON};
    }

    private String escapeJs(String s) {
        if (s == null) return "";
        return s.replace("'", "\\'");
    }
}
