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
 * Shell controller for the Responsable window.
 *
 * Manages the left sidebar and swaps the content area between
 * three views:
 *   1. DashboardResponsable.fxml  — scores équipe + KPI livraisons
 *   2. GestionLivreurs.fxml       — liste livreurs, ajout/modif/suppression
 *   3. GestionLivraisons.fxml     — livraisons en attente + affectation
 *
 * Entry point: call setResponsable(id, nom) after FXML load.
 */
public class MainResponsableController {

    @FXML private StackPane contentArea;

    // Sidebar nav buttons
    @FXML private Button btnNavDashboard;
    @FXML private Button btnNavLivreurs;
    @FXML private Button btnNavLivraisons;

    // Bottom info
    @FXML private Label  labelNomUser;
    @FXML private Button btnModeSwitch;

    private boolean isDarkMode = true;

    // Currently active button
    private Button activeBtn;

    // Responsable data (set by MainFX or SessionManager)
    private int    idResponsable = 1;
    private String nomResponsable = "Responsable";

    // ─────────────────────────────────────────────
    // ENTRY POINT
    // ─────────────────────────────────────────────
    public void setResponsable(int id, String nom) {
        this.idResponsable = id;
        this.nomResponsable = nom;
        labelNomUser.setText(nom);
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
        chargerVue("/fxml/livreur/DashboardResponsable.fxml", btnNavDashboard);
    }

    @FXML
    public void afficherLivreurs() {
        chargerVue("/fxml/livreur/GestionLivreurs.fxml", btnNavLivreurs);
    }

    @FXML
    public void afficherLivraisons() {
        chargerVue("/fxml/livreur/GestionLivraisons.fxml", btnNavLivraisons);
    }

    // ─────────────────────────────────────────────
    // LOAD VIEW INTO CONTENT AREA
    // ─────────────────────────────────────────────
    private void chargerVue(String fxmlPath, Button navBtn) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Node vue = loader.load();
            contentArea.getChildren().setAll(vue);
            setActive(navBtn);
        } catch (IOException e) {
            System.err.println("[MainResponsable] Erreur chargement vue " + fxmlPath
                    + " : " + e.getMessage());
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
    // DARK / LIGHT MODE — applies to the whole window
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

