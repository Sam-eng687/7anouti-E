package tn.hanouti.controller;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import tn.hanouti.dao.CommandeActiveDAO;
import tn.hanouti.model.CommandeActive;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Controller for the customer-facing active orders list screen.
 * Shows all orders currently out for delivery and lets the customer
 * tap one to open the live tracking map.
 */
public class SuiviClientController {

    // ── List ──
    @FXML private ListView<CommandeActive> listCommandes;
    @FXML private Label lblCount;
    @FXML private VBox lblEmpty;

    // ── KPI ──
    @FXML private Label kpiEnCours;

    // ── Detail panel ──
    @FXML private VBox panelVide;
    @FXML private ScrollPane panelDetail;
    @FXML private Label detailNumero;
    @FXML private Label detailStatut;
    @FXML private Label detailAdresse;
    @FXML private Label detailEta;
    @FXML private Label detailLivreur;
    @FXML private Label detailTelephone;
    @FXML private Label detailVehicule;

    // ── Message ──
    @FXML private Label messageLabel;
    @FXML private Button btnModeSwitch;
    private boolean isDarkMode = true;

    private CommandeActiveDAO dao = new CommandeActiveDAO();
    private List<CommandeActive> commandes;
    private CommandeActive commandeSelectionnee;

    // Auto-refresh every 15 seconds so the list stays current
    private ScheduledExecutorService refreshScheduler;

    // ─────────────────────────────────────────────
    // INIT
    // ─────────────────────────────────────────────
    @FXML
    public void initialize() {
        configurerListView();
        chargerCommandes();
        demarrerAutoRefresh();
    }

    // ─────────────────────────────────────────────
    // LISTVIEW CELL FACTORY
    // ─────────────────────────────────────────────
    private void configurerListView() {
        listCommandes.setCellFactory(lv -> new ListCell<CommandeActive>() {
            @Override
            protected void updateItem(CommandeActive c, boolean empty) {
                super.updateItem(c, empty);
                if (empty || c == null) {
                    setGraphic(null);
                    setText(null);
                    return;
                }

                HBox row = new HBox(16);
                row.setStyle("-fx-padding: 14 22; -fx-alignment: center-left;");

                // Order number
                VBox colCmd = new VBox(3);
                Label numLabel = new Label("#" + c.getIdCommande());
                numLabel.setStyle("-fx-font-weight: 800; -fx-font-size: 14px; -fx-text-fill: #38BDF8;");
                Label vehiculeLabel = new Label(c.getVehiculeEmoji() + "  " +
                        (c.getGenreVehicule() != null ? c.getGenreVehicule() : "Voiture"));
                vehiculeLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #8892B0;");
                colCmd.getChildren().addAll(numLabel, vehiculeLabel);
                colCmd.setPrefWidth(130);

                // Address
                Label adresseLabel = new Label(c.getAdresseClient() != null
                        ? c.getAdresseClient() : "—");
                adresseLabel.setPrefWidth(240);
                adresseLabel.setWrapText(true);
                adresseLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #F0F4FF;");

                // Driver
                VBox colLivreur = new VBox(3);
                Label livreurLabel = new Label("👤  " + (c.getNomLivreur() != null
                        ? c.getNomLivreur() : "—"));
                livreurLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #F0F4FF;");
                Label etaLabel = new Label(c.getHeureEstimee() != null
                        ? "⏱  Arrivée : " + c.getHeureEstimee() : "⏱  ETA non défini");
                etaLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #10B981; -fx-font-weight: 700;");
                colLivreur.getChildren().addAll(livreurLabel, etaLabel);
                colLivreur.setPrefWidth(200);

                // Status badge
                String badgeStyle;
                String badgeText = c.getStatutLabel();
                if ("En livraison".equals(badgeText)) {
                    badgeStyle = "-fx-background-color: rgba(56,189,248,0.15); " +
                            "-fx-text-fill: #38BDF8; -fx-background-radius: 20; " +
                            "-fx-padding: 3 12; -fx-font-size: 11px; -fx-font-weight: 700;";
                } else {
                    badgeStyle = "-fx-background-color: rgba(249,115,22,0.15); " +
                            "-fx-text-fill: #F97316; -fx-background-radius: 20; " +
                            "-fx-padding: 3 12; -fx-font-size: 11px; -fx-font-weight: 700;";
                }
                Label statutLabel = new Label(badgeText);
                statutLabel.setStyle(badgeStyle);

                row.getChildren().addAll(colCmd, adresseLabel, colLivreur, statutLabel);
                setGraphic(row);
            }
        });

        listCommandes.setOnMouseClicked(e -> onCommandeCliquee());
    }

    // ─────────────────────────────────────────────
    // LOAD DATA
    // ─────────────────────────────────────────────
    private void chargerCommandes() {
        try {
            commandes = dao.getCommandesActives();
            listCommandes.setItems(FXCollections.observableArrayList(commandes));

            int count = commandes.size();
            if (lblCount != null) lblCount.setText(count + " commande(s) active(s)");
            if (kpiEnCours != null) kpiEnCours.setText(String.valueOf(count));

            boolean vide = commandes.isEmpty();
            if (lblEmpty != null) {
                lblEmpty.setVisible(vide);
                lblEmpty.setManaged(vide);
            }
            listCommandes.setVisible(!vide);
            listCommandes.setManaged(!vide);

        } catch (SQLException e) {
            afficherErreur("Erreur chargement : " + e.getMessage());
        }
    }

    // ─────────────────────────────────────────────
    // CLICK ON ORDER → show detail panel
    // ─────────────────────────────────────────────
    @FXML
    public void onCommandeCliquee() {
        CommandeActive selected = listCommandes.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        commandeSelectionnee = selected;

        detailNumero.setText("Commande  #" + selected.getIdCommande());
        detailAdresse.setText(selected.getAdresseClient() != null
                ? selected.getAdresseClient() : "—");
        detailEta.setText(selected.getHeureEstimee() != null
                ? selected.getHeureEstimee() : "Non défini");
        detailLivreur.setText(selected.getNomLivreur() != null
                ? selected.getNomLivreur() : "—");
        detailTelephone.setText(selected.getTelephoneLivreur() != null
                ? selected.getTelephoneLivreur() : "—");
        detailVehicule.setText(selected.getVehiculeEmoji() + "  " +
                (selected.getGenreVehicule() != null ? selected.getGenreVehicule() : "—"));

        String statutText = selected.getStatutLabel();
        detailStatut.setText(statutText);
        if ("En livraison".equals(statutText)) {
            detailStatut.setStyle("-fx-background-color: rgba(56,189,248,0.15); " +
                    "-fx-text-fill: #38BDF8; -fx-background-radius: 20; -fx-padding: 4 14;");
        } else {
            detailStatut.setStyle("-fx-background-color: rgba(249,115,22,0.15); " +
                    "-fx-text-fill: #F97316; -fx-background-radius: 20; -fx-padding: 4 14;");
        }

        panelVide.setVisible(false);
        panelVide.setManaged(false);
        panelDetail.setVisible(true);
        panelDetail.setManaged(true);
    }

    // ─────────────────────────────────────────────
    // OPEN TRACKING MAP
    // ─────────────────────────────────────────────
    @FXML
    public void ouvrirCarte() {
        if (commandeSelectionnee == null) return;
        arreterAutoRefresh();

        // Client phone for SMS proximity alert
        // When integrating: replace with the real client phone from the users table
        commandeSelectionnee.setTelephoneClient("+21628650563");

        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/TrackingCarte.fxml"));
            Parent root = loader.load();

            TrackingCarteController ctrl = loader.getController();
            ctrl.chargerCommande(commandeSelectionnee);

            Scene scene = new Scene(root);
            scene.getStylesheets().addAll(listCommandes.getScene().getStylesheets());
            Stage stage = (Stage) listCommandes.getScene().getWindow();
            stage.setScene(scene);
        } catch (IOException e) {
            afficherErreur("Erreur ouverture carte : " + e.getMessage());
        }
    }

    // ─────────────────────────────────────────────
    // AUTO-REFRESH (every 15 s)
    // ─────────────────────────────────────────────
    private void demarrerAutoRefresh() {
        refreshScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "suivi-client-refresh");
            t.setDaemon(true);
            return t;
        });
        refreshScheduler.scheduleAtFixedRate(
                () -> Platform.runLater(this::chargerCommandes),
                15, 15, TimeUnit.SECONDS);
    }

    public void arreterAutoRefresh() {
        if (refreshScheduler != null && !refreshScheduler.isShutdown()) {
            refreshScheduler.shutdownNow();
        }
    }

    // ─────────────────────────────────────────────
    // MANUAL REFRESH BUTTON
    // ─────────────────────────────────────────────
    @FXML
    public void rafraichir() {
        chargerCommandes();
        messageLabel.setStyle("-fx-text-fill: #10B981;");
        messageLabel.setText("✅ Liste mise à jour.");
    }

    // ─────────────────────────────────────────────
    // DARK / LIGHT MODE
    // ─────────────────────────────────────────────
    @FXML
    public void switchMode() {
        Scene scene = listCommandes.getScene();
        scene.getStylesheets().clear();
        if (isDarkMode) {
            scene.getStylesheets().add(
                    getClass().getResource("/css/light.css").toExternalForm());
            if (btnModeSwitch != null) btnModeSwitch.setText("🌙  Mode nuit");
            isDarkMode = false;
        } else {
            scene.getStylesheets().add(
                    getClass().getResource("/css/dark.css").toExternalForm());
            if (btnModeSwitch != null) btnModeSwitch.setText("☀️  Mode jour");
            isDarkMode = true;
        }
    }

    // ─────────────────────────────────────────────
    // HELPERS
    // ─────────────────────────────────────────────
    private void afficherErreur(String msg) {
        if (messageLabel != null) {
            messageLabel.setStyle("-fx-text-fill: #F472B6;");
            messageLabel.setText(msg);
        }
    }
}
