package tn.hanouti.livreur.controller;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import tn.hanouti.livreur.dao.LivreurDAO;
import tn.hanouti.livreur.dao.SuiviLivraisonDAO;
import tn.hanouti.livreur.model.Livreur;
import tn.hanouti.livreur.model.SuiviLivraison;

import java.sql.SQLException;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Controller for GestionLivraisons.fxml — Responsable view.
 *
 * Shows all deliveries (all statuses) with a filter.
 * Clicking an EN_ATTENTE delivery opens the assignment panel
 * where the responsable picks an available driver and confirms.
 */
public class GestionLivraisonsController {

    // ── KPI ──
    @FXML private Label kpiEnAttente;
    @FXML private Label kpiAffectees;
    @FXML private Label kpiLivrees;
    @FXML private Label kpiDisponibles;

    // ── Liste livraisons ──
    @FXML private ListView<SuiviLivraison> listLivraisons;
    @FXML private ComboBox<String>         filtreStatut;
    @FXML private Label                    lblCount;

    // ── Panel affectation ──
    @FXML private VBox       panelVide;
    @FXML private ScrollPane panelAffectation;
    @FXML private Label      affectCmd;
    @FXML private Label      affectAdresse;
    @FXML private ListView<Livreur> listeDispo;
    @FXML private Label      lblNbDispo;
    @FXML private Label      messageLabel;

    private final SuiviLivraisonDAO suiviDAO  = new SuiviLivraisonDAO();
    private final LivreurDAO        livreurDAO = new LivreurDAO();

    private List<SuiviLivraison> toutesLivraisons;
    private SuiviLivraison       livraisonSelectionnee;

    // ─────────────────────────────────────────────
    // INIT
    // ─────────────────────────────────────────────
    @FXML
    public void initialize() {
        // Filtre statut
        filtreStatut.setItems(FXCollections.observableArrayList(
                "Tous", "EN_ATTENTE", "AFFECTEE", "LIVREE"));
        filtreStatut.setValue("Tous");
        filtreStatut.setOnAction(e -> filtrer());

        // Cell factory livraisons
        listLivraisons.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(SuiviLivraison s, boolean empty) {
                super.updateItem(s, empty);
                if (empty || s == null) { setGraphic(null); return; }

                HBox row = new HBox(12);
                row.setStyle("-fx-padding: 10 18; -fx-alignment: center-left;");

                Label cmd = new Label("#" + s.getIdCommande());
                cmd.setPrefWidth(130);
                cmd.setStyle("-fx-font-weight: 800; -fx-font-size: 13px; -fx-text-fill: #38BDF8;");

                Label adr = new Label(s.getAdresseClient() != null
                        ? s.getAdresseClient() : "—");
                adr.setPrefWidth(300);
                adr.setStyle("-fx-font-size: 12px;");
                adr.setWrapText(true);

                Label livreur = new Label(s.getIdLivreur() > 0
                        ? "Livreur #" + s.getIdLivreur() : "—");
                livreur.setPrefWidth(110);
                livreur.setStyle("-fx-font-size: 11px; -fx-text-fill: -text-2;");

                String statutStyle;
                String statutText = s.getStatut() != null ? s.getStatut() : "—";
                switch (statutText) {
                    case "EN_ATTENTE" -> statutStyle =
                        "-fx-background-color: rgba(249,115,22,0.15); -fx-text-fill: #F97316;" +
                        "-fx-background-radius: 6; -fx-padding: 2 10; -fx-font-size: 11px; -fx-font-weight: 700;";
                    case "AFFECTEE" -> statutStyle =
                        "-fx-background-color: rgba(56,189,248,0.15); -fx-text-fill: #38BDF8;" +
                        "-fx-background-radius: 6; -fx-padding: 2 10; -fx-font-size: 11px; -fx-font-weight: 700;";
                    case "LIVREE" -> statutStyle =
                        "-fx-background-color: rgba(16,185,129,0.15); -fx-text-fill: #10B981;" +
                        "-fx-background-radius: 6; -fx-padding: 2 10; -fx-font-size: 11px; -fx-font-weight: 700;";
                    default -> statutStyle = "-fx-font-size: 11px;";
                }
                Label statut = new Label(statutText);
                statut.setPrefWidth(110);
                statut.setStyle(statutStyle);

                row.getChildren().addAll(cmd, adr, livreur, statut);
                setGraphic(row);
            }
        });

        // Cell factory livreurs disponibles
        listeDispo.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Livreur l, boolean empty) {
                super.updateItem(l, empty);
                if (empty || l == null) { setGraphic(null); return; }

                HBox row = new HBox(12);
                row.setStyle("-fx-padding: 9 14; -fx-alignment: center-left;");

                Label nom = new Label(l.getNomLivreur());
                nom.setPrefWidth(140);
                nom.setStyle("-fx-font-weight: 700; -fx-font-size: 13px;");

                Label veh = new Label("🚗 " + (l.getGenreVehicule() != null
                        ? l.getGenreVehicule() : "—"));
                veh.setPrefWidth(120);
                veh.setStyle("-fx-font-size: 12px;");

                Label score = new Label("⭐ " + l.getScore());
                score.setStyle("-fx-font-size: 12px;");

                row.getChildren().addAll(nom, veh, score);
                setGraphic(row);
            }
        });

        chargerDonnees();
    }

    // ─────────────────────────────────────────────
    // LOAD DATA
    // ─────────────────────────────────────────────
    private void chargerDonnees() {
        try {
            // All deliveries — combine EN_ATTENTE + AFFECTEE + LIVREE
            List<SuiviLivraison> enAttente  = suiviDAO.getEnAttente();
            List<SuiviLivraison> affectees  = suiviDAO.getAffectees();
            List<SuiviLivraison> livrees    = suiviDAO.getLivrees();

            toutesLivraisons = new java.util.ArrayList<>();
            toutesLivraisons.addAll(enAttente);
            toutesLivraisons.addAll(affectees);
            toutesLivraisons.addAll(livrees);

            // KPI
            kpiEnAttente.setText(String.valueOf(enAttente.size()));
            kpiAffectees.setText(String.valueOf(affectees.size()));
            kpiLivrees.setText(String.valueOf(livrees.size()));

            List<Livreur> disponibles = livreurDAO.getDisponibles();
            kpiDisponibles.setText(String.valueOf(disponibles.size()));

            afficher(toutesLivraisons);

        } catch (SQLException e) {
            afficherErreur("Erreur chargement : " + e.getMessage());
        }
    }

    private void afficher(List<SuiviLivraison> liste) {
        listLivraisons.setItems(FXCollections.observableArrayList(liste));
        if (lblCount != null)
            lblCount.setText(liste.size() + " livraison(s)");
    }

    // ─────────────────────────────────────────────
    // CLICK ON DELIVERY
    // ─────────────────────────────────────────────
    @FXML
    public void onLivraisonCliquee() {
        SuiviLivraison sel = listLivraisons.getSelectionModel().getSelectedItem();
        if (sel == null) return;

        if (!"EN_ATTENTE".equals(sel.getStatut())) {
            afficherInfo("Seules les livraisons EN ATTENTE peuvent être affectées. "
                    + "Statut actuel : " + sel.getStatut());
            return;
        }

        livraisonSelectionnee = sel;
        affectCmd.setText("Commande  #" + sel.getIdCommande());
        affectAdresse.setText(sel.getAdresseClient() != null
                ? sel.getAdresseClient() : "—");

        // Load available drivers
        try {
            List<Livreur> disponibles = livreurDAO.getDisponibles();
            listeDispo.setItems(FXCollections.observableArrayList(disponibles));
            lblNbDispo.setText(disponibles.size() + " dispo");
        } catch (SQLException e) {
            afficherErreur("Erreur chargement livreurs : " + e.getMessage());
            return;
        }

        // Show panel
        panelVide.setVisible(false);
        panelVide.setManaged(false);
        panelAffectation.setVisible(true);
        panelAffectation.setManaged(true);
        if (messageLabel != null) messageLabel.setText("");
    }

    // ─────────────────────────────────────────────
    // CONFIRM ASSIGNMENT
    // ─────────────────────────────────────────────
    @FXML
    public void confirmerAffectation() {
        Livreur choisi = listeDispo.getSelectionModel().getSelectedItem();
        if (choisi == null) {
            afficherInfo("Sélectionnez un livreur dans la liste !");
            return;
        }
        if (livraisonSelectionnee == null) return;

        try {
            String heure = LocalTime.now().plusMinutes(30).getHour() + "h" +
                    String.format("%02d", LocalTime.now().plusMinutes(30).getMinute());

            suiviDAO.affecterLivreur(livraisonSelectionnee.getIdSuivi(),
                    choisi.getIdLivreur(), heure);
            livreurDAO.assignerLivraison(choisi.getIdLivreur());

            // Success feedback
            if (messageLabel != null) {
                messageLabel.setStyle("-fx-text-fill: #10B981;");
                messageLabel.setText("✅ Commande #" + livraisonSelectionnee.getIdCommande()
                        + " affectée à " + choisi.getNomLivreur() + " !");
            }

            // Reset panel
            panelVide.setVisible(true);
            panelVide.setManaged(true);
            panelAffectation.setVisible(false);
            panelAffectation.setManaged(false);
            livraisonSelectionnee = null;

            chargerDonnees();

        } catch (SQLException e) {
            afficherErreur("Erreur affectation : " + e.getMessage());
        }
    }

    // ─────────────────────────────────────────────
    // FILTER
    // ─────────────────────────────────────────────
    private void filtrer() {
        if (toutesLivraisons == null) return;
        String val = filtreStatut.getValue();
        if ("Tous".equals(val)) {
            afficher(toutesLivraisons);
        } else {
            afficher(toutesLivraisons.stream()
                    .filter(s -> val.equals(s.getStatut()))
                    .collect(Collectors.toList()));
        }
    }

    // ─────────────────────────────────────────────
    // REFRESH BUTTON
    // ─────────────────────────────────────────────
    @FXML
    public void rafraichir() {
        chargerDonnees();
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

    private void afficherInfo(String msg) {
        if (messageLabel != null) {
            messageLabel.setStyle("-fx-text-fill: #F97316;");
            messageLabel.setText(msg);
        }
    }
}

