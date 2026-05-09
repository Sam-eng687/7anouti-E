package projet.hanouti.GestionCommandes.controllers;


import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import projet.hanouti.GestionCommandes.entities.Commande;
import projet.hanouti.GestionCommandes.entities.SocieteLivraison;
import projet.hanouti.GestionCommandes.services.LivraisonService;
import projet.hanouti.GestionCommandes.services.SocieteLivraisonService;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

/**
 * Controller — Popup assignation société de livraison.
 * Deux modes : Automatique (meilleure société pré-sélectionnée) ou Manuelle (liste + recherche).
 */
public class PopupAssignerLivraisonController implements Initializable {

    // ── FXML ──────────────────────────────────────────────────
    @FXML private StackPane overlayRoot;
    @FXML private Label     lblSubtitle;

    // Tabs
    @FXML private Button tabAuto, tabManuel;
    @FXML private VBox   panelAuto, panelManuel;

    // Auto panel
    @FXML private Label autoNom, autoZone, autoNote, autoInfo;

    // Manual panel
    @FXML private TextField searchSociete;
    @FXML private ListView<SocieteLivraison> listSocietes;

    // Selection info
    @FXML private VBox  selectionInfo;
    @FXML private Label selNom, selZone, selNote;

    // Footer
    @FXML private Button btnValider;

    // ── State ─────────────────────────────────────────────────
    private final LivraisonService       livraisonService = new LivraisonService();
    private final SocieteLivraisonService societeService   = new SocieteLivraisonService();

    private Commande           commande;
    private SocieteLivraison   societeBestAuto;
    private SocieteLivraison   societeSelectionnee;
    private Runnable           onSuccess;
    private boolean            modeManuel = false;

    private List<SocieteLivraison> allSocietes;

    // ── Init ──────────────────────────────────────────────────

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        listSocietes.setCellFactory(lv -> new SocieteListCell());
        btnValider.setDisable(true);
    }

    /**
     * Appelé par le controller parent avant d'afficher le popup.
     */
    public void init(Commande commande, boolean darkMode, Runnable onSuccess) {
        this.commande  = commande;
        this.onSuccess = onSuccess;

        overlayRoot.getStyleClass().remove("light-mode");
        if (!darkMode) {
            overlayRoot.getStyleClass().add("light-mode");
        }

        lblSubtitle.setText("Commande : " + commande.getNumeroCommande());

        // Charger la meilleure société (auto)
        societeBestAuto = societeService.selectMeilleureSociete(commande.getAdresseLivraison());
        if (societeBestAuto != null) {
            autoNom.setText(societeBestAuto.getNomSociete());
            autoZone.setText("Zone : " + societeBestAuto.getZoneCouverture());
            autoNote.setText("★ " + String.format("%.1f", societeBestAuto.getNote()) + " / 5.0");
            autoInfo.setText("Sélectionnée selon zone « "
                    + extraireZone(commande.getAdresseLivraison())
                    + " » et la meilleure note disponible.");
            societeSelectionnee = societeBestAuto;
            btnValider.setDisable(false);
        } else {
            autoNom.setText("Aucune société disponible");
            autoZone.setText("—");
            autoNote.setText("—");
            panelAuto.setDisable(true);
        }

        // Charger toutes les sociétés (manuel)
        allSocietes = societeService.getSocietesActives();
        listSocietes.setItems(FXCollections.observableArrayList(allSocietes));
    }

    // ── Tabs ──────────────────────────────────────────────────

    @FXML
    private void onTabAuto() {
        modeManuel = false;
        tabAuto.getStyleClass().clear();
        tabAuto.getStyleClass().addAll("btn-primary");
        tabManuel.getStyleClass().clear();
        tabManuel.getStyleClass().addAll("btn-annuler");

        panelAuto.setVisible(true);   panelAuto.setManaged(true);
        panelManuel.setVisible(false); panelManuel.setManaged(false);

        // Reset selection to auto best
        societeSelectionnee = societeBestAuto;
        updateSelectionInfo(societeSelectionnee);
        btnValider.setDisable(societeSelectionnee == null);
    }

    @FXML
    private void onTabManuel() {
        modeManuel = true;
        tabManuel.getStyleClass().clear();
        tabManuel.getStyleClass().addAll("btn-primary");
        tabAuto.getStyleClass().clear();
        tabAuto.getStyleClass().addAll("btn-annuler");

        panelAuto.setVisible(false);  panelAuto.setManaged(false);
        panelManuel.setVisible(true); panelManuel.setManaged(true);

        societeSelectionnee = null;
        selectionInfo.setVisible(false); selectionInfo.setManaged(false);
        btnValider.setDisable(true);
    }

    // ── Manuel search & selection ─────────────────────────────

    @FXML
    private void onSearchSociete() {
        String q = searchSociete.getText().toLowerCase().trim();
        List<SocieteLivraison> filtered = allSocietes.stream()
                .filter(s -> q.isEmpty()
                        || s.getNomSociete().toLowerCase().contains(q)
                        || s.getZoneCouverture().toLowerCase().contains(q))
                .collect(Collectors.toList());
        listSocietes.setItems(FXCollections.observableArrayList(filtered));
    }

    @FXML
    private void onSocieteSelected() {
        SocieteLivraison s = listSocietes.getSelectionModel().getSelectedItem();
        if (s == null) return;
        societeSelectionnee = s;
        updateSelectionInfo(s);
        btnValider.setDisable(false);
        listSocietes.refresh();
    }

    private void updateSelectionInfo(SocieteLivraison s) {
        if (s == null) {
            selectionInfo.setVisible(false);
            selectionInfo.setManaged(false);
            return;
        }
        selNom.setText(s.getNomSociete());
        selZone.setText("Zone : " + s.getZoneCouverture());
        selNote.setText("★ " + String.format("%.1f", s.getNote()) + " / 5.0");
        selectionInfo.setVisible(true);
        selectionInfo.setManaged(true);
    }

    // ── Validation ────────────────────────────────────────────

    @FXML
    private void onValider() {
        if (commande == null || societeSelectionnee == null) return;

        try {
            if (modeManuel) {
                livraisonService.attribuerManuellement(
                        commande.getIdCommande(),
                        societeSelectionnee.getIdSociete()
                );
            } else {
                livraisonService.attribuerAutomatiquement(commande.getIdCommande());
            }

            if (onSuccess != null) onSuccess.run();
            closePopup();

        } catch (Exception e) {
            showError("Erreur lors de l'assignation : " + e.getMessage());
        }
    }

    // ── Close ─────────────────────────────────────────────────

    @FXML
    private void onClose() {
        closePopup();
    }

    /**
     * Ferme si clic sur l'overlay (en dehors de la popup card).
     */
    @FXML
    private void onOverlayClick(MouseEvent e) {
        if (e.getTarget() == overlayRoot) {
            closePopup();
        }
    }

    /**
     * Empêche la fermeture si clic à l'intérieur de la card.
     */
    @FXML
    private void onCardClick(MouseEvent e) {
        e.consume();
    }

    private void closePopup() {
        Stage stage = (Stage) overlayRoot.getScene().getWindow();
        stage.close();
    }

    // ── Helpers ───────────────────────────────────────────────

    private String extraireZone(String adresse) {
        if (adresse == null || adresse.isBlank()) return "inconnue";
        String[] parts = adresse.trim().split("[,\\s]+");
        return parts[parts.length - 1];
    }

    private void showError(String msg) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Erreur");
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }

    // ══════════════════════════════════════════════════════════
    // INNER — SocieteListCell
    // ══════════════════════════════════════════════════════════
    private class SocieteListCell extends ListCell<SocieteLivraison> {

        private final HBox  row      = new HBox(12);
        private final Label icon     = new Label("🏢");
        private final VBox  info     = new VBox(3);
        private final Label lblNom   = new Label();
        private final Label lblZone  = new Label();
        private final Region sp      = new Region();
        private final VBox  right    = new VBox(4);
        private final Label lblNote  = new Label();
        private final Label lblMode  = new Label("AUTO");

        SocieteListCell() {
            icon.setStyle("-fx-font-size: 22px;");
            info.getChildren().addAll(lblNom, lblZone);
            right.getChildren().addAll(lblNote, lblMode);
            right.setAlignment(Pos.CENTER_RIGHT);
            HBox.setHgrow(sp, Priority.ALWAYS);
            row.getChildren().addAll(icon, info, sp, right);
            row.setAlignment(Pos.CENTER_LEFT);
            row.getStyleClass().add("societe-card");

            lblNom.getStyleClass().add("societe-nom");
            lblZone.getStyleClass().add("societe-zone");
            lblNote.getStyleClass().add("societe-note");
            lblMode.getStyleClass().add("societe-mode-badge");

            VBox.setMargin(row, new javafx.geometry.Insets(3, 0, 3, 0));
        }

        @Override
        protected void updateItem(SocieteLivraison s, boolean empty) {
            super.updateItem(s, empty);
            if (empty || s == null) { setGraphic(null); return; }

            lblNom.setText(s.getNomSociete());
            lblZone.setText("📍 " + s.getZoneCouverture());
            lblNote.setText("★ " + String.format("%.1f", s.getNote()));

            row.getStyleClass().removeAll("societe-card-selected");
            if (isSelected() || (societeSelectionnee != null
                    && societeSelectionnee.getIdSociete() == s.getIdSociete())) {
                row.getStyleClass().add("societe-card-selected");
            }

            setGraphic(row);
        }
    }
}
