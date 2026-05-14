package tn.hanouti.livreur.controller;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.control.ScrollPane;
import javafx.stage.Stage;
import tn.hanouti.livreur.dao.LivreurDAO;
import tn.hanouti.livreur.dao.SuiviLivraisonDAO;
import tn.hanouti.livreur.model.SuiviLivraison;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;

public class SuiviLivraisonController {

    // ── Liste ──
    @FXML private ListView<SuiviLivraison> listView;
    @FXML private ComboBox<String> filtreStatut;
    @FXML private Label lblCount;

    // ── KPI ──
    @FXML private Label labelTotal;
    @FXML private Label labelEnCours;
    @FXML private Label labelLivrees;

    // ── Panel détail ──
    @FXML private VBox quickPanel;
    @FXML private VBox quickEmpty;
    @FXML private ScrollPane quickDetail;
    @FXML private Label qCmd;
    @FXML private Label qTemps;
    @FXML private Label qStatut;
    @FXML private Label qAdresse;
    @FXML private Label qHeure;
    @FXML private Button btnLivree;

    // ── Message ──
    @FXML private Label messageLabel;
    @FXML private Button btnModeSwitch;
    private boolean isDarkMode = true;

    private SuiviLivraisonDAO suiviDAO = new SuiviLivraisonDAO();
    private LivreurDAO livreurDAO = new LivreurDAO();
    private List<SuiviLivraison> tousLesSuivis;
    private SuiviLivraison livraisonSelectionnee;

    // ID du livreur connecté — hardcoded for standalone testing
    // When integrating: replace with the logged-in user's id
    private int idLivreurConnecte = 4;

    /**
     * Called by DashboardLivreurController (or SessionManager integration)
     * to set the connected driver before the screen loads data.
     * Must be called before or right after initialize().
     */
    public void setIdLivreur(int id) {
        this.idLivreurConnecte = id;
        chargerSuivis(); // reload with the correct id
    }

    @FXML
    public void initialize() {
        filtreStatut.setItems(FXCollections.observableArrayList(
                "Tous", "AFFECTEE", "LIVREE"));
        filtreStatut.setValue("Tous");
        filtreStatut.setOnAction(e -> filtrer());

        // Cellule liste
        listView.setCellFactory(lv -> new ListCell<SuiviLivraison>() {
            @Override
            protected void updateItem(SuiviLivraison s, boolean empty) {
                super.updateItem(s, empty);
                if (empty || s == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    HBox row = new HBox(15);
                    row.setStyle("-fx-padding: 10 20; -fx-alignment: center-left;");

                    Label idCmd = new Label("#" + s.getIdCommande());
                    idCmd.setPrefWidth(120);
                    idCmd.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #2563EB;");

                    Label adresse = new Label(s.getAdresseClient() != null
                            ? s.getAdresseClient() : "—");
                    adresse.setPrefWidth(260);
                    adresse.setStyle("-fx-font-size: 12px;");
                    adresse.setWrapText(true);

                    Label temps = new Label(s.getHeureEstimee() != null
                            ? "⏱ " + s.getHeureEstimee() : "⏱ —");
                    temps.setPrefWidth(140);
                    temps.setStyle("-fx-font-size: 12px; -fx-text-fill: #10B981;");

                    String statutStyle;
                    if ("LIVREE".equals(s.getStatut())) {
                        statutStyle = "-fx-background-color: rgba(16,185,129,0.15); " +
                                "-fx-text-fill: #10B981; -fx-background-radius: 6; " +
                                "-fx-padding: 2 10; -fx-font-size: 11px; -fx-font-weight: 700;";
                    } else {
                        statutStyle = "-fx-background-color: rgba(37,99,235,0.15); " +
                                "-fx-text-fill: #38BDF8; -fx-background-radius: 6; " +
                                "-fx-padding: 2 10; -fx-font-size: 11px; -fx-font-weight: 700;";
                    }
                    Label statut = new Label(s.getStatut() != null ? s.getStatut() : "—");
                    statut.setPrefWidth(120);
                    statut.setStyle(statutStyle);

                    row.getChildren().addAll(idCmd, adresse, temps, statut);
                    setGraphic(row);
                }
            }
        });

        chargerSuivis();
    }

    // ─────────────────────────────────────────────
    // CHARGER
    // ─────────────────────────────────────────────
    private void chargerSuivis() {
        try {
            tousLesSuivis = suiviDAO.getByLivreur(idLivreurConnecte);
            afficher(tousLesSuivis);
            mettreAJourStats();
        } catch (SQLException e) {
            messageLabel.setText("Erreur : " + e.getMessage());
        }
    }

    private void afficher(List<SuiviLivraison> liste) {
        listView.setItems(FXCollections.observableArrayList(liste));
        if (lblCount != null)
            lblCount.setText(liste.size() + " livraison(s)");
    }

    private void mettreAJourStats() {
        int total = tousLesSuivis.size();
        long enCours = tousLesSuivis.stream()
                .filter(s -> "AFFECTEE".equals(s.getStatut())).count();
        long livrees = tousLesSuivis.stream()
                .filter(s -> "LIVREE".equals(s.getStatut())).count();
        if (labelTotal != null) labelTotal.setText(String.valueOf(total));
        if (labelEnCours != null) labelEnCours.setText(String.valueOf(enCours));
        if (labelLivrees != null) labelLivrees.setText(String.valueOf(livrees));
    }

    // ─────────────────────────────────────────────
    // CLIC SUR UNE LIVRAISON → affiche panel détail
    // ─────────────────────────────────────────────
    @FXML
    public void onLivraisonClicked() {
        SuiviLivraison selected = listView.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        livraisonSelectionnee = selected;

        // Remplir panel
        qCmd.setText("Commande #" + selected.getIdCommande());
        qAdresse.setText(selected.getAdresseClient() != null
                ? selected.getAdresseClient() : "—");
        qHeure.setText(selected.getHeureEstimee() != null
                ? selected.getHeureEstimee() : "—");
        qTemps.setText(selected.getHeureEstimee() != null
                ? "Arrivée estimée : " + selected.getHeureEstimee() : "Temps non calculé");

        // Statut badge
        if ("LIVREE".equals(selected.getStatut())) {
            qStatut.setText("LIVRÉE");
            qStatut.setStyle("-fx-background-color: rgba(16,185,129,0.15); " +
                    "-fx-text-fill: #10B981; -fx-background-radius: 6; -fx-padding: 3 10;");
        } else {
            qStatut.setText("EN COURS");
            qStatut.setStyle("-fx-background-color: rgba(37,99,235,0.15); " +
                    "-fx-text-fill: #38BDF8; -fx-background-radius: 6; -fx-padding: 3 10;");
        }

        // Bouton "Marquer livrée" visible seulement si AFFECTEE
        boolean peutLivrer = "AFFECTEE".equals(selected.getStatut());
        if (btnLivree != null) {
            btnLivree.setVisible(peutLivrer);
            btnLivree.setManaged(peutLivrer);
        }

        // Afficher panel
        quickEmpty.setVisible(false);
        quickEmpty.setManaged(false);
        quickDetail.setVisible(true);
        quickDetail.setManaged(true);
    }

    // ─────────────────────────────────────────────
    // MARQUER COMME LIVRÉE
    // ─────────────────────────────────────────────
    @FXML
    public void onMarquerLivree() {
        if (livraisonSelectionnee == null) return;

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmation");
        confirm.setContentText("Marquer la commande #"
                + livraisonSelectionnee.getIdCommande() + " comme livrée ?");
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                try {
                    suiviDAO.modifierStatut(livraisonSelectionnee.getIdSuivi(), "LIVREE");
                    // Libérer le livreur — le rendre disponible à nouveau
                    try {
                        livreurDAO.libererLivreur(idLivreurConnecte);
                    } catch (SQLException ex) {
                        System.err.println("[SuiviLivraison] Erreur libération livreur : " + ex.getMessage());
                    }
                    messageLabel.setStyle("-fx-text-fill: #10B981;");
                    messageLabel.setText("✅ Commande #"
                            + livraisonSelectionnee.getIdCommande() + " livrée !");

                    // Reset panel
                    quickEmpty.setVisible(true);
                    quickEmpty.setManaged(true);
                    quickDetail.setVisible(false);
                    quickDetail.setManaged(false);
                    livraisonSelectionnee = null;

                    chargerSuivis();
                } catch (SQLException e) {
                    messageLabel.setStyle("-fx-text-fill: #F472B6;");
                    messageLabel.setText("Erreur : " + e.getMessage());
                }
            }
        });
    }

    // ─────────────────────────────────────────────
    // VOIR SUR LA CARTE — ouvre le suivi client
    // ─────────────────────────────────────────────
    @FXML
    public void onVoirCarte() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/livreur/SuiviClient.fxml"));
            Parent root = loader.load();
            Scene scene = new Scene(root);
            scene.getStylesheets().addAll(listView.getScene().getStylesheets());
            Stage stage = (Stage) listView.getScene().getWindow();
            stage.setScene(scene);
        } catch (IOException e) {
            messageLabel.setStyle("-fx-text-fill: #F472B6;");
            messageLabel.setText("Erreur ouverture suivi client : " + e.getMessage());
        }
    }

    // ─────────────────────────────────────────────
    // FILTRE
    // ─────────────────────────────────────────────
    private void filtrer() {
        String val = filtreStatut.getValue();
        if ("Tous".equals(val)) {
            afficher(tousLesSuivis);
        } else {
            afficher(tousLesSuivis.stream()
                    .filter(s -> val.equals(s.getStatut()))
                    .collect(Collectors.toList()));
        }
    }

    // ─────────────────────────────────────────────
    // SWITCH MODE JOUR / NUIT
    // ─────────────────────────────────────────────
    @FXML
    public void switchMode() {
        Scene scene = listView.getScene();
        scene.getStylesheets().clear();
        if (isDarkMode) {
            scene.getStylesheets().add(
                    getClass().getResource("/css/livreur/Light.css").toExternalForm());
            if (btnModeSwitch != null) btnModeSwitch.setText("🌙  Mode nuit");
            isDarkMode = false;
        } else {
            scene.getStylesheets().add(
                    getClass().getResource("/css/livreur/Dark.css").toExternalForm());
            if (btnModeSwitch != null) btnModeSwitch.setText("☀️  Mode jour");
            isDarkMode = true;
        }
    }

}

