package projet.hanouti.GestionCommandes.controllers;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import projet.hanouti.GestionCommandes.entities.Commande;
import projet.hanouti.GestionCommandes.entities.LigneCommande;
import projet.hanouti.GestionCommandes.enums.StatutCommande;
import projet.hanouti.common.utils.MyBD;
import projet.hanouti.GestionCommandes.services.CommandeService;
import projet.hanouti.GestionCommandes.services.LigneCommandeService;
import projet.hanouti.GestionCommandes.services.LivraisonService;
import projet.hanouti.GestionCommandes.services.NotificationService;
import projet.hanouti.GestionCommandes.services.SocieteLivraisonService;

import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Controller du popup de visualisation complète d'une commande.
 * Utilisé à la fois par l'acheteur (role="ACHETEUR") et le vendeur (role="VENDEUR").
 * Les boutons d'action sont affichés selon le rôle et le statut courant.
 */
public class PopupDetailCommandeController implements Initializable {

    // ── FXML ──────────────────────────────────────────────────
    @FXML private StackPane overlayRoot;

    // Header
    @FXML private Label popNumero, popDate, popStatut, popVip;

    // Info
    @FXML private Label popAdresse, popPaiement, popDatePref, popTotal;
    @FXML private Label popScore, popSociete, popStatutLiv;
    @FXML private Label popNbProduits;

    // Lignes
    @FXML private ListView<LigneCommande> popListLignes;

    // Motif refus
    @FXML private VBox  popMotifBox;
    @FXML private Label popMotif;

    // Facture
    @FXML private VBox   popFactureBox;
    @FXML private Button popBtnPdf, popBtnQr;

    // Actions — acheteur
    @FXML private Button actBtnModifierAdresse, actBtnAnnuler;

    // Actions — vendeur
    @FXML private Button actBtnConfirmer, actBtnRefuser, actBtnPreparer,
            actBtnExpedier, actBtnLivrer, actBtnAssigner;

    // ── State ─────────────────────────────────────────────────
    private final CommandeService      cmdService   = new CommandeService();
    private final LigneCommandeService ligneService = new LigneCommandeService();
    private final LivraisonService     livraisonService = new LivraisonService();
    private final NotificationService  notifService     = new NotificationService();
    private final SocieteLivraisonService societeService   = new SocieteLivraisonService();

    private Commande commande;
    private String   role;       // "ACHETEUR" or "VENDEUR"
    private Runnable onRefresh;

    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        popListLignes.setCellFactory(lv -> new LigneCell());
    }

    // ── Init (called by parent controller) ────────────────────

    /**
     * @param commande  la commande à afficher
     * @param role      "ACHETEUR" ou "VENDEUR"
     * @param onRefresh callback appelé après une action modifiant la commande
     */
    public void init(Commande commande, String role, Runnable onRefresh) {
        this.commande  = commande;
        this.role      = role;
        this.onRefresh = onRefresh;
        populateView();
    }

    // ── Populate ──────────────────────────────────────────────

    private void populateView() {
        // Header
        popNumero.setText(commande.getNumeroCommande());
        popDate.setText(commande.getDateCreation() != null
                ? commande.getDateCreation().format(DT_FMT) : "—");
        applyBadge(popStatut, commande.getStatut());

        boolean vip = cmdService.isAcheteurVIP(commande.getIdAcheteur());
        popVip.setVisible(vip); popVip.setManaged(vip);

        // Infos
        popAdresse.setText(commande.getAdresseLivraison());
        popPaiement.setText(commande.getModePaiement().name());
        popDatePref.setText(commande.getDateLivraisonPreferee() != null
                ? commande.getDateLivraisonPreferee().toString() : "Livraison standard");
        popTotal.setText(String.format("%.2f TND", commande.getTotal()));

        // Score + livraison
        popScore.setText("⚡ " + commande.getScorePriorite() + " pts");
        if (commande.getIdSocieteLivraison() != null) {
            var soc = societeService.getById(commande.getIdSocieteLivraison());
            popSociete.setText(soc != null ? "🚚 " + soc.getNomSociete() : "—");
            var liv = livraisonService.getByCommande(commande.getIdCommande());
            popStatutLiv.setText(liv != null ? "Statut : " + liv.getStatutLivraison().name() : "");
        } else {
            popSociete.setText("Aucune société assignée");
            popStatutLiv.setText("");
        }

        // Lignes
        List<LigneCommande> lignes = ligneService.getByCommande(commande.getIdCommande());
        enrichWithProductNames(lignes);
        popListLignes.setItems(FXCollections.observableArrayList(lignes));
        popNbProduits.setText(lignes.size() + " produit(s)");

        // Motif refus
        boolean ref = commande.getStatut() == StatutCommande.REFUSEE;
        popMotifBox.setVisible(ref); popMotifBox.setManaged(ref);
        if (ref && commande.getMotifRefus() != null) popMotif.setText(commande.getMotifRefus());

        // Facture
        boolean aFacture = commande.getFacturePdf() != null && !commande.getFacturePdf().isBlank();
        popFactureBox.setVisible(aFacture); popFactureBox.setManaged(aFacture);

        // Actions selon rôle
        hideAllActions();
        if ("ACHETEUR".equals(role)) {
            setupActionsAcheteur();
        } else if ("VENDEUR".equals(role)) {
            setupActionsVendeur();
        }
    }

    private void setupActionsAcheteur() {
        boolean modifiable = commande.getStatut() == StatutCommande.CREEE
                || commande.getStatut() == StatutCommande.CONFIRMEE
                || commande.getStatut() == StatutCommande.EN_PREPARATION;
        show(actBtnModifierAdresse, modifiable);
        show(actBtnAnnuler, modifiable);
    }

    private void setupActionsVendeur() {
        switch (commande.getStatut()) {
            case CREEE           -> { show(actBtnConfirmer); show(actBtnRefuser); }
            case CONFIRMEE       -> { show(actBtnPreparer);  show(actBtnRefuser); }
            case EN_PREPARATION  -> show(actBtnExpedier);
            case EXPEDIEE        -> show(actBtnLivrer);
            default -> {}
        }
        // Assigner livraison toujours visible si pas encore assignée
        if (commande.getIdSocieteLivraison() == null
                && commande.getStatut() != StatutCommande.ANNULEE
                && commande.getStatut() != StatutCommande.REFUSEE
                && commande.getStatut() != StatutCommande.LIVREE) {
            show(actBtnAssigner);
        }
    }

    // ── Actions ACHETEUR ──────────────────────────────────────

    @FXML
    private void onModifierAdresse() {
        TextInputDialog dlg = new TextInputDialog(commande.getAdresseLivraison());
        dlg.setTitle("Modifier l'adresse"); dlg.setContentText("Nouvelle adresse :");
        dlg.showAndWait().ifPresent(addr -> {
            if (addr.isBlank()) return;
            try {
                cmdService.updateAdresse(commande.getIdCommande(), addr);
                commande = cmdService.getById(commande.getIdCommande());
                populateView();
                if (onRefresh != null) onRefresh.run();
            } catch (Exception e) { alert(Alert.AlertType.WARNING, "Impossible", e.getMessage()); }
        });
    }

    @FXML
    private void onAnnuler() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setContentText("Annuler " + commande.getNumeroCommande() + " ?");
        confirm.showAndWait().ifPresent(r -> {
            if (r == ButtonType.OK) {
                cmdService.annuler(commande.getIdCommande());
                if (onRefresh != null) onRefresh.run();
                closePopup();
            }
        });
    }

    // ── Actions VENDEUR ───────────────────────────────────────

    @FXML
    private void onConfirmer() {
        cmdService.confirmer(commande.getIdCommande());
        sendNotif("Commande confirmée",
                "Votre commande " + commande.getNumeroCommande() + " a été confirmée.",
                projet.hanouti.GestionCommandes.enums.TypeNotification.COMMANDE, projet.hanouti.GestionCommandes.enums.EventNotification.COMMANDE_CONFIRMEE);
        refreshAndRepopulate();
    }

    @FXML
    private void onRefuser() {
        TextInputDialog dlg = new TextInputDialog();
        dlg.setTitle("Refus"); dlg.setContentText("Motif (obligatoire) :");
        dlg.showAndWait().ifPresent(motif -> {
            if (motif.isBlank()) { alert(Alert.AlertType.WARNING, "Motif requis", "Saisissez un motif."); return; }
            cmdService.refuser(commande.getIdCommande(), motif);
            sendNotif("Commande refusée",
                    "Commande " + commande.getNumeroCommande() + " refusée. Motif : " + motif,
                    projet.hanouti.GestionCommandes.enums.TypeNotification.COMMANDE, projet.hanouti.GestionCommandes.enums.EventNotification.COMMANDE_REFUSEE);
            refreshAndRepopulate();
        });
    }

    @FXML private void onPreparer()  { cmdService.passerEnPreparation(commande.getIdCommande()); refreshAndRepopulate(); }

    @FXML
    private void onExpedier() {
        cmdService.expedier(commande.getIdCommande());
        sendNotif("Commande expédiée !",
                "Votre commande " + commande.getNumeroCommande() + " est en route.",
                projet.hanouti.GestionCommandes.enums.TypeNotification.COMMANDE, projet.hanouti.GestionCommandes.enums.EventNotification.COMMANDE_EXPEDIEE);
        refreshAndRepopulate();
    }

    @FXML private void onLivrer() { cmdService.livrer(commande.getIdCommande()); refreshAndRepopulate(); }

    @FXML
    private void onAssigner() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/FXML/GestionCommandes/PopupAssignerLivraison.fxml")
            );
            Parent popup = loader.load();
            PopupAssignerLivraisonController ctrl = loader.getController();
            boolean isLight = overlayRoot.getStyleClass().contains("light-mode")
                    || (overlayRoot.getScene() != null
                    && overlayRoot.getScene().getRoot().getStyleClass().contains("light-mode"));
            ctrl.init(commande, !isLight, () -> {
                commande = cmdService.getById(commande.getIdCommande());
                populateView();
                if (onRefresh != null) onRefresh.run();
            });
            if (isLight && !popup.getStyleClass().contains("light-mode")) {
                popup.getStyleClass().add("light-mode");
            }
            Stage st = new Stage(StageStyle.TRANSPARENT);
            Scene sc = new Scene(popup); sc.setFill(null);
            if (overlayRoot.getScene() != null)
                sc.getStylesheets().addAll(overlayRoot.getScene().getStylesheets());
            st.setScene(sc); st.initModality(Modality.APPLICATION_MODAL);
            st.initOwner(overlayRoot.getScene().getWindow()); st.showAndWait();
        } catch (Exception e) {
            System.err.println("[PopupDetailCommandeController.onAssigner] " + e.getMessage());
        }
    }

    // ── Facture ───────────────────────────────────────────────

    @FXML
    private void onOpenPdf() {
        if (commande.getFacturePdf() == null) return;
        try { java.awt.Desktop.getDesktop().open(new java.io.File(commande.getFacturePdf())); }
        catch (Exception e) { alert(Alert.AlertType.ERROR, "Erreur", e.getMessage()); }
    }

    @FXML
    private void onOpenQr() {
        if (commande.getFactureQr() == null) return;
        try { java.awt.Desktop.getDesktop().open(new java.io.File(commande.getFactureQr())); }
        catch (Exception e) { alert(Alert.AlertType.ERROR, "Erreur", e.getMessage()); }
    }

    // ── Close ─────────────────────────────────────────────────

    @FXML private void onClose()  { closePopup(); }

    @FXML
    private void onOverlayClick(MouseEvent e) {
        if (e.getTarget() == overlayRoot) closePopup();
    }

    @FXML
    private void onCardClick(MouseEvent e) { e.consume(); }

    private void closePopup() {
        ((Stage) overlayRoot.getScene().getWindow()).close();
    }

    // ── Helpers ───────────────────────────────────────────────

    private void refreshAndRepopulate() {
        commande = cmdService.getById(commande.getIdCommande());
        populateView();
        if (onRefresh != null) onRefresh.run();
    }

    private void hideAllActions() {
        hide(actBtnModifierAdresse); hide(actBtnAnnuler);
        hide(actBtnConfirmer); hide(actBtnRefuser); hide(actBtnPreparer);
        hide(actBtnExpedier); hide(actBtnLivrer); hide(actBtnAssigner);
    }

    private void show(Button b) { b.setVisible(true);  b.setManaged(true); }
    private void hide(Button b) { b.setVisible(false); b.setManaged(false); }

    private void show(Button b, boolean condition) {
        b.setVisible(condition); b.setManaged(condition);
    }

    private void applyBadge(Label l, StatutCommande s) {
        l.getStyleClass().clear();
        l.getStyleClass().addAll("statut-badge", "badge-" + s.name());
        l.setText(switch (s) {
            case CREEE          -> "🆕 Créée";
            case CONFIRMEE      -> "✅ Confirmée";
            case EN_PREPARATION -> "🔧 En préparation";
            case EXPEDIEE       -> "🚀 Expédiée";
            case LIVREE         -> "📬 Livrée";
            case ANNULEE        -> "🚫 Annulée";
            case REFUSEE        -> "❌ Refusée";
        });
    }

    private void sendNotif(String titre, String msg,
                           projet.hanouti.GestionCommandes.enums.TypeNotification type,
                           projet.hanouti.GestionCommandes.enums.EventNotification event) {
        notifService.envoyerNotification(commande.getIdAcheteur(), type, event, titre, msg,
                commande.getIdCommande());
    }

    private void alert(Alert.AlertType t, String title, String msg) {
        Alert a = new Alert(t); a.setTitle(title); a.setHeaderText(null);
        a.setContentText(msg); a.showAndWait();
    }


    private void enrichWithProductNames(List<LigneCommande> lignes) {
        try {
            java.sql.Connection cnx = MyBD.getInstance().getConnection();
            for (LigneCommande l : lignes) {
                try (java.sql.PreparedStatement ps = cnx.prepareStatement(
                        "SELECT nom FROM produit WHERE id_produit = ?")) {
                    ps.setInt(1, l.getIdProduit());
                    java.sql.ResultSet rs = ps.executeQuery();
                    if (rs.next()) l.setNomProduit(rs.getString("nom"));
                }
            }
        } catch (Exception e) {
            System.err.println("[PopupDetailCommande.enrichWithProductNames] " + e.getMessage());
        }
    }

    // ── LigneCell ─────────────────────────────────────────────

    private static class LigneCell extends ListCell<LigneCommande> {
        private final HBox  row = new HBox(10);
        private final Label lP = new Label(), lQ = new Label(), lPx = new Label();

        LigneCell() {
            Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
            row.getChildren().addAll(lP, lQ, sp, lPx);
            row.getStyleClass().add("ligne-item");
            row.setPadding(new Insets(8, 10, 8, 10));
            lP.getStyleClass().add("ligne-produit");
            lQ.getStyleClass().add("ligne-qte");
            lPx.getStyleClass().add("ligne-prix");
        }

        @Override
        protected void updateItem(LigneCommande l, boolean empty) {
            super.updateItem(l, empty);
            if (empty || l == null) { setGraphic(null); return; }
            String nom = (l.getNomProduit() != null && !l.getNomProduit().isBlank())
                    ? l.getNomProduit() : "Produit inconnu";
            lP.setText(nom);
            lQ.setText("× " + l.getQuantite());
            lPx.setText(String.format("%.2f TND", l.getSousTotal()));
            setGraphic(row);
        }
    }
}
