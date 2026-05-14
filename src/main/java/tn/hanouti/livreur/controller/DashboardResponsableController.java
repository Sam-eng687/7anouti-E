package tn.hanouti.livreur.controller;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.scene.Scene;
import tn.hanouti.livreur.dao.LivreurDAO;
import tn.hanouti.livreur.dao.ScoreDAO;
import tn.hanouti.livreur.dao.SuiviLivraisonDAO;
import tn.hanouti.livreur.model.Livreur;
import tn.hanouti.livreur.model.SuiviLivraison;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;

public class DashboardResponsableController {

    // ── KPI ──
    @FXML private Label kpiTotal;
    @FXML private Label kpiDisponibles;
    @FXML private Label kpiEnLivraison;
    @FXML private Label kpiEnAttente;

    // ── Lists (dashboard view) ──
    @FXML private ListView<Livreur>        listLivreurs;
    @FXML private ListView<SuiviLivraison> listAttente;
    @FXML private ListView<String>         listActivite;
    @FXML private Label lblAttente;

    // ── Score bars ──
    @FXML private VBox scoresBars;

    // ── Content area (StackPane) ──
    @FXML private StackPane contentArea;
    @FXML private VBox vueDashboard;
    @FXML private VBox vueContenu;

    // ── Header labels ──
    @FXML private Label labelPageTitre;
    @FXML private Label labelPageSub;

    // ── Sidebar nav ──
    @FXML private Button btnNavDashboard;
    @FXML private Button btnNavLivreurs;
    @FXML private Button btnNavLivraisons;
    @FXML private Label  labelNomResponsable;

    // ── Mode / message ──
    @FXML private Label  messageLabel;
    @FXML private Button btnModeSwitch;
    private boolean isDarkMode = true;

    private final LivreurDAO        livreurDAO = new LivreurDAO();
    private final SuiviLivraisonDAO suiviDAO   = new SuiviLivraisonDAO();
    private final ScoreDAO          scoreDAO   = new ScoreDAO();

    // ─────────────────────────────────────────────
    // ENTRY POINT
    // ─────────────────────────────────────────────
    public void setResponsable(int id, String nom) {
        if (labelNomResponsable != null) labelNomResponsable.setText(nom);
    }

    // ─────────────────────────────────────────────
    // INIT
    // ─────────────────────────────────────────────
    @FXML
    public void initialize() {
        configurerCellules();
        chargerDonnees();
    }

    // ─────────────────────────────────────────────
    // LOAD DATA
    // ─────────────────────────────────────────────
    private void chargerDonnees() {
        try {
            List<Livreur> livreurs = livreurDAO.getAll();
            long dispo       = livreurs.stream().filter(Livreur::isDisponibilite).count();
            long enLivraison = livreurs.stream().filter(l -> !l.isDisponibilite()).count();

            kpiTotal.setText(String.valueOf(livreurs.size()));
            kpiDisponibles.setText(String.valueOf(dispo));
            kpiEnLivraison.setText(String.valueOf(enLivraison));
            listLivreurs.setItems(FXCollections.observableArrayList(livreurs));

            List<SuiviLivraison> enAttente = suiviDAO.getEnAttente();
            kpiEnAttente.setText(String.valueOf(enAttente.size()));
            lblAttente.setText(enAttente.size() + " en attente");
            listAttente.setItems(FXCollections.observableArrayList(enAttente));

            List<String> activite = livreurs.stream()
                    .flatMap(l -> {
                        try {
                            return suiviDAO.getByLivreur(l.getIdLivreur()).stream()
                                    .filter(s -> "LIVREE".equals(s.getStatut()))
                                    .map(s -> "✅  #" + s.getIdCommande() +
                                              "  —  " + l.getNomLivreur() +
                                              "  —  " + (s.getHeureEstimee() != null
                                                         ? s.getHeureEstimee() : "—"));
                        } catch (SQLException e) {
                            return java.util.stream.Stream.empty();
                        }
                    })
                    .limit(15)
                    .collect(Collectors.toList());
            listActivite.setItems(FXCollections.observableArrayList(
                    activite.isEmpty() ? List.of("Aucune activité récente.") : activite));

            construireScoreBars(livreurs);

        } catch (SQLException e) {
            afficherErreur("Erreur : " + e.getMessage());
        }
    }

    // ─────────────────────────────────────────────
    // SCORE BARS
    // ─────────────────────────────────────────────
    private void construireScoreBars(List<Livreur> livreurs) {
        scoresBars.getChildren().clear();
        for (Livreur l : livreurs) {
            int score = l.getScore();
            HBox row = new HBox(8);
            row.setStyle("-fx-alignment: center-left;");

            Label nom = new Label(l.getNomLivreur());
            nom.setPrefWidth(130);
            nom.setStyle("-fx-font-size: 11px; -fx-text-fill: -text-2;");

            StackPane barBg = new StackPane();
            barBg.setPrefHeight(10);
            barBg.setPrefWidth(100);
            barBg.setStyle("-fx-background-color: rgba(59,130,246,0.10); -fx-background-radius: 5;");

            double fillWidth = (score / 100.0) * 100;
            Label barFill = new Label();
            barFill.setPrefHeight(10);
            barFill.setPrefWidth(fillWidth);
            barFill.setMaxWidth(fillWidth);

            String color = score >= 80 ? "#10B981" : score >= 60 ? "#38BDF8"
                         : score >= 40 ? "#F97316" : "#F472B6";
            barFill.setStyle("-fx-background-color: " + color + "; -fx-background-radius: 5;");
            barBg.getChildren().add(barFill);
            StackPane.setAlignment(barFill, javafx.geometry.Pos.CENTER_LEFT);

            Label valeur = new Label(score + "");
            valeur.setStyle("-fx-font-size: 11px; -fx-font-weight: 700; -fx-text-fill: " + color + ";");

            row.getChildren().addAll(nom, barBg, valeur);
            scoresBars.getChildren().add(row);
        }
    }

    // ─────────────────────────────────────────────
    // CELL FACTORIES
    // ─────────────────────────────────────────────
    private void configurerCellules() {
        listLivreurs.setCellFactory(lv -> new ListCell<>() {
            @Override protected void updateItem(Livreur l, boolean empty) {
                super.updateItem(l, empty);
                if (empty || l == null) { setGraphic(null); return; }
                HBox row = new HBox(12);
                row.setStyle("-fx-padding: 9 18; -fx-alignment: center-left;");
                Label nom = new Label(l.getNomLivreur() + (l.isResponsable() ? "  👑" : ""));
                nom.setPrefWidth(190); nom.setStyle("-fx-font-weight: 700; -fx-font-size: 12px;");
                Label tel = new Label(l.getTelephone());
                tel.setPrefWidth(110); tel.setStyle("-fx-font-size: 11px;");
                Label veh = new Label(l.getGenreVehicule() != null ? l.getGenreVehicule() : "—");
                veh.setPrefWidth(80); veh.setStyle("-fx-font-size: 11px;");
                boolean dispo = l.isDisponibilite();
                Label dispoBadge = new Label(dispo ? "✅ Dispo" : "🔴 Occupé");
                dispoBadge.setPrefWidth(90);
                dispoBadge.setStyle(dispo
                    ? "-fx-background-color: rgba(16,185,129,0.15); -fx-text-fill: #10B981;" +
                      "-fx-background-radius: 6; -fx-padding: 2 8; -fx-font-size: 10px; -fx-font-weight: 700;"
                    : "-fx-background-color: rgba(244,114,182,0.15); -fx-text-fill: #F472B6;" +
                      "-fx-background-radius: 6; -fx-padding: 2 8; -fx-font-size: 10px; -fx-font-weight: 700;");
                Label score = new Label("⭐ " + l.getScore());
                score.setStyle("-fx-font-size: 11px;");
                row.getChildren().addAll(nom, tel, veh, dispoBadge, score);
                setGraphic(row);
            }
        });

        listAttente.setCellFactory(lv -> new ListCell<>() {
            @Override protected void updateItem(SuiviLivraison s, boolean empty) {
                super.updateItem(s, empty);
                if (empty || s == null) { setGraphic(null); return; }
                HBox row = new HBox(12);
                row.setStyle("-fx-padding: 9 18; -fx-alignment: center-left;");
                Label cmd = new Label("#" + s.getIdCommande());
                cmd.setPrefWidth(130);
                cmd.setStyle("-fx-font-weight: 800; -fx-font-size: 13px; -fx-text-fill: #F97316;");
                Label adr = new Label(s.getAdresseClient() != null ? s.getAdresseClient() : "—");
                adr.setPrefWidth(300); adr.setStyle("-fx-font-size: 11px;"); adr.setWrapText(true);
                Button btnAffecter = new Button("Affecter →");
                btnAffecter.setStyle(
                    "-fx-background-color: linear-gradient(to right, #2563EB, #8B5CF6);" +
                    "-fx-text-fill: white; -fx-font-size: 11px; -fx-font-weight: 700;" +
                    "-fx-background-radius: 8; -fx-padding: 4 12; -fx-cursor: hand;");
                btnAffecter.setOnAction(e -> navLivraisons());
                row.getChildren().addAll(cmd, adr, btnAffecter);
                setGraphic(row);
            }
        });

        listActivite.setCellFactory(lv -> new ListCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setGraphic(null); return; }
                Label lbl = new Label(item);
                lbl.setStyle("-fx-font-size: 11px; -fx-padding: 8 14;");
                setGraphic(lbl);
            }
        });
    }

    // ─────────────────────────────────────────────
    // NAVIGATION SIDEBAR — swap content in StackPane
    // ─────────────────────────────────────────────
    @FXML
    public void navDashboard() {
        afficherVue(vueDashboard);
        setNavActif("dashboard");
        labelPageTitre.setText("📊  Dashboard Responsable");
        labelPageSub.setText("Vue d'ensemble de l'équipe et des livraisons");
        chargerDonnees();
    }

    @FXML
    public void navLivreurs() {
        setNavActif("livreurs");
        labelPageTitre.setText("👥  Gestion des Livreurs");
        labelPageSub.setText("Gérez votre équipe de livraison");
        chargerFXMLDansContenu("/fxml/livreur/GestionLivreurs.fxml");
    }

    @FXML
    public void navLivraisons() {
        setNavActif("livraisons");
        labelPageTitre.setText("📦  Livraisons en attente");
        labelPageSub.setText("Affectez les commandes aux livreurs disponibles");
        chargerFXMLDansContenu("/fxml/livreur/GestionLivreurs.fxml");
    }

    /**
     * Loads a FXML into vueContenu (inside the StackPane) without touching the sidebar.
     */
    private void chargerFXMLDansContenu(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Node contenu = loader.load();
            vueContenu.getChildren().setAll(contenu);
            VBox.setVgrow(contenu, javafx.scene.layout.Priority.ALWAYS);
            afficherVue(vueContenu);
        } catch (IOException e) {
            afficherErreur("Erreur chargement : " + e.getMessage());
        }
    }

    /**
     * Shows one view in the StackPane and hides the other.
     */
    private void afficherVue(VBox vue) {
        vueDashboard.setVisible(false);
        vueDashboard.setManaged(false);
        vueContenu.setVisible(false);
        vueContenu.setManaged(false);

        vue.setVisible(true);
        vue.setManaged(true);
    }

    private void setNavActif(String actif) {
        String on  = "-fx-background-color: rgba(59,130,246,0.15); -fx-text-fill: #38BDF8;" +
                     "-fx-font-weight: 700; -fx-font-size: 13px; -fx-padding: 10 14;" +
                     "-fx-background-radius: 10; -fx-border-color: transparent;" +
                     "-fx-alignment: center-left; -fx-cursor: hand;";
        String off = "-fx-background-color: transparent; -fx-text-fill: #8892B0;" +
                     "-fx-font-size: 13px; -fx-padding: 10 14; -fx-background-radius: 10;" +
                     "-fx-border-color: transparent; -fx-alignment: center-left; -fx-cursor: hand;";
        if (btnNavDashboard  != null) btnNavDashboard.setStyle("dashboard".equals(actif)   ? on : off);
        if (btnNavLivreurs   != null) btnNavLivreurs.setStyle("livreurs".equals(actif)     ? on : off);
        if (btnNavLivraisons != null) btnNavLivraisons.setStyle("livraisons".equals(actif) ? on : off);
    }

    // ─────────────────────────────────────────────
    // AJOUTER LIVREUR (dialog modal)
    // ─────────────────────────────────────────────
    @FXML
    public void ouvrirAjout() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/livreur/AjouterLivreur.fxml"));
            javafx.scene.Parent root = loader.load();
            AjouterLivreurController ctrl = loader.getController();
            ctrl.setOnSuccess(this::chargerDonnees);
            Stage stage = new Stage();
            stage.setTitle("Nouveau livreur");
            stage.setScene(new Scene(root));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.showAndWait();
        } catch (IOException e) {
            afficherErreur("Erreur : " + e.getMessage());
        }
    }

    // ─────────────────────────────────────────────
    // DARK / LIGHT MODE
    // ─────────────────────────────────────────────
    @FXML
    public void switchMode() {
        Scene scene = listLivreurs.getScene();
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

