package tn.hanouti.livreur.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;

import java.io.IOException;

/**
 * Shell controller for the Livreur window.
 *
 * Manages the left sidebar and swaps the content area between
 * two views:
 *   1. DashboardLivreur.fxml  — score, KPI, livraisons du jour
 *   2. SuiviLivraison.fxml    — liste livraisons livrées + en cours
 *
 * Entry point: call setLivreur(id, nom) after FXML load.
 */
public class MainLivreurController {

    @FXML private StackPane contentArea;

    // Sidebar nav buttons
    @FXML private Button btnNavDashboard;
    @FXML private Button btnNavLivraisons;

    // Bottom info
    @FXML private Label  labelNomUser;
    @FXML private Button btnModeSwitch;

    private boolean isDarkMode = true;
    private Button  activeBtn;

    // Livreur data
    private int    idLivreur  = 4;
    private String nomLivreur = "Livreur";

    // ─────────────────────────────────────────────
    // ENTRY POINT
    // ─────────────────────────────────────────────
    public void setLivreur(int id, String nom) {
        this.idLivreur  = id;
        this.nomLivreur = nom;
        labelNomUser.setText(nom);
        // Reload dashboard with correct id
        afficherDashboard();
    }

    // ─────────────────────────────────────────────
    // INIT — load dashboard by default
    // ─────────────────────────────────────────────
    @FXML
    public void initialize() {
        activeBtn = btnNavDashboard;
        afficherDashboard();
    }

    // ─────────────────────────────────────────────
    // NAVIGATION ACTIONS
    // ─────────────────────────────────────────────
    @FXML
    public void afficherDashboard() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/livreur/DashboardLivreur.fxml"));
            Node vue = loader.load();

            // Pass livreur id to dashboard
            DashboardLivreurController ctrl = loader.getController();
            ctrl.setLivreur(idLivreur, nomLivreur);

            contentArea.getChildren().setAll(vue);
            setActive(btnNavDashboard);

        } catch (IOException e) {
            System.err.println("[MainLivreur] Erreur dashboard : " + e.getMessage());
        }
    }

    @FXML
    public void afficherLivraisons() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/livreur/SuiviLivraison.fxml"));
            Node vue = loader.load();

            // Pass livreur id
            SuiviLivraisonController ctrl = loader.getController();
            ctrl.setIdLivreur(idLivreur);

            contentArea.getChildren().setAll(vue);
            setActive(btnNavLivraisons);

        } catch (IOException e) {
            System.err.println("[MainLivreur] Erreur livraisons : " + e.getMessage());
        }
    }

    // ─────────────────────────────────────────────
    // ACTIVE BUTTON STYLE
    // ─────────────────────────────────────────────
    private void setActive(Button btn) {
        if (activeBtn != null) {
            activeBtn.getStyleClass().remove("nav-btn-active");
            if (!activeBtn.getStyleClass().contains("nav-btn"))
                activeBtn.getStyleClass().add("nav-btn");
        }
        btn.getStyleClass().remove("nav-btn");
        if (!btn.getStyleClass().contains("nav-btn-active"))
            btn.getStyleClass().add("nav-btn-active");
        activeBtn = btn;
    }

    // ─────────────────────────────────────────────
    // DARK / LIGHT MODE
    // ─────────────────────────────────────────────
    @FXML
    public void switchMode() {
        Scene scene = contentArea.getScene();
        if (scene == null) return;
        scene.getStylesheets().clear();
        if (isDarkMode) {
            scene.getStylesheets().add(
                    getClass().getResource("/css/livreur/Light.css").toExternalForm());
            btnModeSwitch.setText("🌙  Mode nuit");
            isDarkMode = false;
        } else {
            scene.getStylesheets().add(
                    getClass().getResource("/css/livreur/Dark.css").toExternalForm());
            btnModeSwitch.setText("☀️  Mode jour");
            isDarkMode = true;
        }
    }
}

