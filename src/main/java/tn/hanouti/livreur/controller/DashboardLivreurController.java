package tn.hanouti.livreur.controller;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import tn.hanouti.livreur.dao.ScoreDAO;
import tn.hanouti.livreur.dao.SuiviLivraisonDAO;
import tn.hanouti.livreur.model.Score;
import tn.hanouti.livreur.model.SuiviLivraison;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;

public class DashboardLivreurController {

    // ── KPI ──
    @FXML private Label kpiAujourdhui;
    @FXML private Label kpiMois;
    @FXML private Label kpiPonctualite;
    @FXML private Label kpiScore;

    // ── Header ──
    @FXML private Label labelTitre;
    @FXML private Label labelSub;

    // ── Sidebar ──
    @FXML private Label  labelBienvenue;
    @FXML private Button btnNavDashboard;
    @FXML private Button btnNavLivraisons;
    @FXML private Button btnModeSwitch;

    // ── StackPane content area ──
    @FXML private StackPane contentArea;
    @FXML private VBox      vueDashboard;
    @FXML private VBox      vueLivraisons;

    // ── Dashboard lists ──
    @FXML private ListView<SuiviLivraison> listEnCours;
    @FXML private ListView<SuiviLivraison> listHistorique;
    @FXML private ListView<String>         listEvaluations;
    @FXML private Label lblEnCours;
    @FXML private Label lblHistorique;

    // ── Score card ──
    @FXML private Label scoreGrand;
    @FXML private Label labelEtoiles;
    @FXML private Label labelNoteTexte;

    // ── Message ──
    @FXML private Label messageLabel;

    private boolean isDarkMode = true;

    private final SuiviLivraisonDAO suiviDAO = new SuiviLivraisonDAO();
    private final ScoreDAO          scoreDAO = new ScoreDAO();

    private int    idLivreur  = 4;
    private String nomLivreur = "Livreur";

    // ─────────────────────────────────────────────
    // ENTRY POINT
    // ─────────────────────────────────────────────
    public void setLivreur(int id, String nom) {
        this.idLivreur  = id;
        this.nomLivreur = nom;
        if (labelTitre    != null) labelTitre.setText("👋  Bonjour, " + nom + " !");
        if (labelBienvenue != null) labelBienvenue.setText(nom);
        chargerDonnees();
    }

    // ─────────────────────────────────────────────
    // INIT
    // ─────────────────────────────────────────────
    @FXML
    public void initialize() {
        configurerCellules();
    }

    // ─────────────────────────────────────────────
    // LOAD DATA
    // ─────────────────────────────────────────────
    private void chargerDonnees() {
        try {
            List<SuiviLivraison> toutes = suiviDAO.getByLivreur(idLivreur);

            List<SuiviLivraison> enCours = toutes.stream()
                    .filter(s -> "AFFECTEE".equals(s.getStatut()))
                    .collect(Collectors.toList());
            List<SuiviLivraison> livrees = toutes.stream()
                    .filter(s -> "LIVREE".equals(s.getStatut()))
                    .collect(Collectors.toList());

            kpiAujourdhui.setText(String.valueOf(enCours.size()));
            kpiMois.setText(String.valueOf(livrees.size()));

            List<Score> scores = scoreDAO.getByLivreur(idLivreur);
            if (!scores.isEmpty()) {
                long ponctuel = scores.stream().filter(Score::isLivreDansDelai).count();
                kpiPonctualite.setText((int) Math.round(ponctuel * 100.0 / scores.size()) + "%");
            } else {
                kpiPonctualite.setText("—");
            }

            int scoreMoyen = scoreDAO.calculerScoreMoyen(idLivreur);
            kpiScore.setText(String.valueOf(scoreMoyen));
            scoreGrand.setText(scoreMoyen + " / 100");

            double noteSur5 = scoreMoyen / 20.0;
            labelEtoiles.setText(etoiles(noteSur5));
            labelNoteTexte.setText(scores.isEmpty()
                    ? "Aucune évaluation"
                    : String.format("%.1f / 5  (%d évaluation%s)",
                        noteSur5, scores.size(), scores.size() > 1 ? "s" : ""));

            listEnCours.setItems(FXCollections.observableArrayList(enCours));
            lblEnCours.setText(enCours.size() + " en cours");

            listHistorique.setItems(FXCollections.observableArrayList(
                    livrees.stream().limit(10).collect(Collectors.toList())));
            lblHistorique.setText(livrees.size() + " livrée(s) au total");

            List<String> evalTextes = scores.stream().limit(8)
                    .map(s -> etoiles(s.getNote()) + "  " +
                              (s.getCommentaire() != null ? s.getCommentaire() : "—"))
                    .collect(Collectors.toList());
            listEvaluations.setItems(FXCollections.observableArrayList(
                    evalTextes.isEmpty() ? List.of("Aucune évaluation pour l'instant.") : evalTextes));

        } catch (SQLException e) {
            afficherErreur("Erreur : " + e.getMessage());
        }
    }

    // ─────────────────────────────────────────────
    // CELL FACTORIES
    // ─────────────────────────────────────────────
    private void configurerCellules() {
        javafx.util.Callback<ListView<SuiviLivraison>, ListCell<SuiviLivraison>> factory =
            lv -> new ListCell<>() {
                @Override protected void updateItem(SuiviLivraison s, boolean empty) {
                    super.updateItem(s, empty);
                    if (empty || s == null) { setGraphic(null); return; }
                    HBox row = new HBox(15);
                    row.setStyle("-fx-padding: 10 18; -fx-alignment: center-left;");
                    Label cmd = new Label("#" + s.getIdCommande());
                    cmd.setPrefWidth(120);
                    cmd.setStyle("-fx-font-weight: 800; -fx-font-size: 13px; -fx-text-fill: #38BDF8;");
                    Label adr = new Label(s.getAdresseClient() != null ? s.getAdresseClient() : "—");
                    adr.setPrefWidth(260); adr.setStyle("-fx-font-size: 12px;"); adr.setWrapText(true);
                    Label eta = new Label(s.getHeureEstimee() != null ? "⏱ " + s.getHeureEstimee() : "—");
                    eta.setPrefWidth(120); eta.setStyle("-fx-font-size: 12px; -fx-text-fill: #10B981;");
                    boolean livree = "LIVREE".equals(s.getStatut());
                    Label statut = new Label(livree ? "LIVRÉE" : "EN COURS");
                    statut.setStyle(livree
                        ? "-fx-background-color: rgba(16,185,129,0.15); -fx-text-fill: #10B981;" +
                          "-fx-background-radius: 6; -fx-padding: 2 10; -fx-font-size: 11px; -fx-font-weight: 700;"
                        : "-fx-background-color: rgba(56,189,248,0.15); -fx-text-fill: #38BDF8;" +
                          "-fx-background-radius: 6; -fx-padding: 2 10; -fx-font-size: 11px; -fx-font-weight: 700;");
                    row.getChildren().addAll(cmd, adr, eta, statut);
                    setGraphic(row);
                }
            };
        listEnCours.setCellFactory(factory);
        listHistorique.setCellFactory(factory);

        listEvaluations.setCellFactory(lv -> new ListCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setGraphic(null); return; }
                Label lbl = new Label(item);
                lbl.setStyle("-fx-font-size: 12px; -fx-padding: 8 14;");
                lbl.setWrapText(true);
                setGraphic(lbl);
            }
        });
    }

    // ─────────────────────────────────────────────
    // NAVIGATION — swap inside StackPane (sidebar stays)
    // ─────────────────────────────────────────────
    @FXML
    public void navDashboard() {
        afficherVue(vueDashboard);
        setNavActif("dashboard");
        if (labelTitre != null) labelTitre.setText("👋  Bonjour, " + nomLivreur + " !");
        if (labelSub   != null) labelSub.setText("Votre tableau de bord — livraisons du jour");
        chargerDonnees();
    }

    @FXML
    public void navMesLivraisons() {
        setNavActif("livraisons");
        if (labelTitre != null) labelTitre.setText("📦  Mes Livraisons");
        if (labelSub   != null) labelSub.setText("Consultez et gérez vos livraisons assignées");
        chargerSuiviDansContenu();
    }

    /**
     * Loads SuiviLivraison.fxml into vueLivraisons (inside the StackPane).
     * The sidebar stays visible — only the content area changes.
     */
    private void chargerSuiviDansContenu() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/livreur/SuiviLivraison.fxml"));
            Node contenu = loader.load();

            SuiviLivraisonController ctrl = loader.getController();
            ctrl.setIdLivreur(idLivreur);

            vueLivraisons.getChildren().setAll(contenu);
            VBox.setVgrow(contenu, javafx.scene.layout.Priority.ALWAYS);
            afficherVue(vueLivraisons);

        } catch (IOException e) {
            afficherErreur("Erreur chargement livraisons : " + e.getMessage());
        }
    }

    private void afficherVue(VBox vue) {
        vueDashboard.setVisible(false);
        vueDashboard.setManaged(false);
        vueLivraisons.setVisible(false);
        vueLivraisons.setManaged(false);
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
        if (btnNavLivraisons != null) btnNavLivraisons.setStyle("livraisons".equals(actif) ? on : off);
    }

    // ─────────────────────────────────────────────
    // DARK / LIGHT MODE
    // ─────────────────────────────────────────────
    @FXML
    public void switchMode() {
        Scene scene = vueDashboard.getScene();
        if (scene == null) return;
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
    private String etoiles(double note) {
        int plein = Math.max(0, Math.min(5, (int) Math.round(note)));
        return "★".repeat(plein) + "☆".repeat(5 - plein);
    }

    private void afficherErreur(String msg) {
        if (messageLabel != null) {
            messageLabel.setStyle("-fx-text-fill: #F472B6;");
            messageLabel.setText(msg);
        }
    }
}

