package projet.hanouti.GestionCommandes.controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import projet.hanouti.GestionCommandes.entities.Commande;
import projet.hanouti.GestionCommandes.entities.LigneCommande;
import projet.hanouti.GestionCommandes.entities.SocieteLivraison;
import projet.hanouti.GestionCommandes.enums.StatutCommande;
import projet.hanouti.common.utils.MyBD;
import projet.hanouti.common.utils.SessionManager;
import projet.hanouti.GestionCommandes.services.*;

import java.io.File;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

/**
 * VendeurCommandesController — full rewrite
 *
 * Fixes applied:
 * • Noms de produits affichés (pas d'IDs)
 * • Nom de société affiché (pas d'IDs)
 * • Confirmer → vérifie adresse via API → si invalide : refuse avec motif
 *             → si valide : popup assigner société apparaît
 * • Impossible d'assigner si statut EXPÉDIÉE / LIVRÉE
 * • EXPÉDIÉE / LIVRÉE → aucun bouton d'action (état terminal)
 * • Confirmer → insert dans livraisons → incrémente quantite_vendue produit
 * • Pas de bouton "Voir détails complets"
 * • Filtres ComboBox opaques (CSS géré globalement)
 * • Sidebar overlay transparent (CSS)
 * • ✅ Bouton "Exporter la facture PDF" — génère et ouvre le PDF via FactureService
 */
public class VendeurCommandesController implements Initializable {

    // ── FXML ──────────────────────────────────────────────────
    @FXML private HBox  root;
    @FXML private Label kpiTotal, kpiAttente, kpiLivrees, lblIA, lblCount;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> filterStatut, filterMode;
    @FXML private ListView<Commande> listCommandes;

    // Detail
    @FXML private VBox       emptyState;
    @FXML private ScrollPane detailScroll;
    @FXML private Label  detNumero, detDate, detStatutBadge, detVipBadge;
    @FXML private Label  detScore, detAdresse, detPaiement, detTotal, detDatePref;
    @FXML private Label  detSociete, detStatutLiv, detMotif, detNbProduits;
    @FXML private VBox   motifBox, actionsBox;
    @FXML private ListView<LigneCommande> listLignes;

    // Action buttons
    @FXML private Button btnConfirmer, btnRefuser, btnPreparer, btnExpedier, btnLivrer;
    @FXML private Button btnExporterFacture; // ✅ NEW
    @FXML private Label  lblTerminal;

    // ── Services ──────────────────────────────────────────────
    private final CommandeService          cmdService      = new CommandeService();
    private final LigneCommandeService     ligneService    = new LigneCommandeService();
    private final NotificationService      notifService    = new NotificationService();
    private final AdresseValidationService adresseService  = new AdresseValidationService();
    private final SocieteLivraisonService  societeService  = new SocieteLivraisonService();
    private final LivraisonService         livraisonService = new LivraisonService();
    private final FactureService           factureService  = new FactureService(); // ✅ NEW

    // ── State ─────────────────────────────────────────────────
    private ObservableList<Commande> allCommandes = FXCollections.observableArrayList();
    private Commande selectedCommande = null;
    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        filterStatut.setItems(FXCollections.observableArrayList(
                "Tous","CREEE","CONFIRMEE","EN_PREPARATION","EXPEDIEE","LIVREE","ANNULEE","REFUSEE"));
        filterMode.setItems(FXCollections.observableArrayList("Tous","CARTE","ESPECES"));
        listCommandes.setCellFactory(lv -> new CommandeCell());
        listLignes.setCellFactory(lv -> new LigneCell());
        loadCommandes();
    }

    // ── Data ──────────────────────────────────────────────────
    private void loadCommandes() {
        int id = SessionManager.getCurrentUserId();
        List<Commande> list = cmdService.getCommandesPrioritisees(id);
        allCommandes = FXCollections.observableArrayList(list);
        listCommandes.setItems(allCommandes);

        kpiTotal.setText(String.valueOf(list.size()));
        kpiAttente.setText(String.valueOf(list.stream().filter(c ->
                c.getStatut() == StatutCommande.CREEE
                        || c.getStatut() == StatutCommande.CONFIRMEE
                        || c.getStatut() == StatutCommande.EN_PREPARATION).count()));
        kpiLivrees.setText(String.valueOf(list.stream()
                .filter(c -> c.getStatut() == StatutCommande.LIVREE).count()));
        lblCount.setText(list.size() + " commande(s)");

        long prio = list.stream().filter(c -> c.getScorePriorite() >= 40).count();
        lblIA.setText(prio > 0
                ? prio + " commande(s) haute priorité — médicaments et VIP priorisés."
                : "Toutes les commandes sont traitées selon leur score de priorité.");
    }

    // ── Selection ─────────────────────────────────────────────
    @FXML
    private void onCommandeSelected() {
        Commande c = listCommandes.getSelectionModel().getSelectedItem();
        if (c == null) return;
        selectedCommande = c;
        showDetail(c);
        listCommandes.refresh();
    }

    private void showDetail(Commande c) {
        emptyState.setVisible(false);   emptyState.setManaged(false);
        detailScroll.setVisible(true);  detailScroll.setManaged(true);

        // Header
        detNumero.setText(c.getNumeroCommande());
        detDate.setText(c.getDateCreation() != null ? c.getDateCreation().format(DT_FMT) : "—");
        applyBadge(detStatutBadge, c.getStatut());
        boolean vip = cmdService.isAcheteurVIP(c.getIdAcheteur());
        detVipBadge.setVisible(vip); detVipBadge.setManaged(vip);
        detScore.setText("⚡ " + c.getScorePriorite() + " pts");

        // Infos
        detAdresse.setText(c.getAdresseLivraison());
        detPaiement.setText(c.getModePaiement().name());
        detTotal.setText(String.format("%.2f TND", c.getTotal()));
        detDatePref.setText(c.getDateLivraisonPreferee() != null
                ? c.getDateLivraisonPreferee().toString() : "Livraison standard");

        // Produits (real names)
        List<LigneCommande> lignes = ligneService.getByCommande(c.getIdCommande());
        enrichWithProductNames(lignes);
        listLignes.setItems(FXCollections.observableArrayList(lignes));
        detNbProduits.setText(lignes.size() + " produit(s)");

        // Société (real name)
        if (c.getIdSocieteLivraison() != null) {
            SocieteLivraison soc = societeService.getById(c.getIdSocieteLivraison());
            detSociete.setText(soc != null ? "🚚 " + soc.getNomSociete() : "—");
            var liv = livraisonService.getByCommande(c.getIdCommande());
            detStatutLiv.setText(liv != null ? "Statut : " + formatStatutLiv(liv.getStatutLivraison()) : "");
        } else {
            detSociete.setText("Non encore assignée");
            detStatutLiv.setText("");
        }

        // Motif refus
        boolean ref = c.getStatut() == StatutCommande.REFUSEE;
        motifBox.setVisible(ref); motifBox.setManaged(ref);
        if (ref && c.getMotifRefus() != null) detMotif.setText(c.getMotifRefus());

        // Actions
        updateActions(c.getStatut());
    }

    private void updateActions(StatutCommande s) {
        // Hide all first
        btnConfirmer.setVisible(false);       btnConfirmer.setManaged(false);
        btnRefuser.setVisible(false);         btnRefuser.setManaged(false);
        btnPreparer.setVisible(false);        btnPreparer.setManaged(false);
        btnExpedier.setVisible(false);        btnExpedier.setManaged(false);
        btnLivrer.setVisible(false);          btnLivrer.setManaged(false);
        lblTerminal.setVisible(false);        lblTerminal.setManaged(false);

        // ✅ Export PDF always available whenever a commande is selected
        show(btnExporterFacture);

        switch (s) {
            case CREEE -> {
                show(btnConfirmer); show(btnRefuser);
            }
            case CONFIRMEE -> {
                show(btnPreparer); show(btnRefuser);
            }
            case EN_PREPARATION -> show(btnExpedier);
            case EXPEDIEE       -> show(btnLivrer);
            case LIVREE -> {
                lblTerminal.setText("✅ Commande livrée avec succès.");
                show(lblTerminal);
            }
            case ANNULEE -> {
                lblTerminal.setText("🚫 Commande annulée par l'acheteur.");
                show(lblTerminal);
            }
            case REFUSEE -> {
                lblTerminal.setText("❌ Commande refusée — l'acheteur peut la modifier et la renvoyer.");
                show(lblTerminal);
            }
        }
    }

    // ── EXPORTER FACTURE PDF ──────────────────────────────────
    /**
     * Génère la facture PDF via FactureService puis l'ouvre avec le
     * lecteur PDF par défaut du système. Si Desktop n'est pas supporté,
     * affiche simplement le chemin du fichier généré.
     */
    @FXML
    private void onExporterFacture() {
        if (selectedCommande == null) return;

        List<LigneCommande> lignes = ligneService.getByCommande(selectedCommande.getIdCommande());
        enrichWithProductNames(lignes);

        String cheminPdf = factureService.genererPDF(selectedCommande, lignes);
        if (cheminPdf == null) {
            showAlert(Alert.AlertType.ERROR, "Erreur export",
                    "Impossible de générer la facture PDF.\nVérifiez les logs pour plus de détails.");
            return;
        }

        // Also update the commande's facture_pdf field if not already set
        if (selectedCommande.getFacturePdf() == null) {
            selectedCommande.setFacturePdf(cheminPdf);
            // Optionally persist via cmdService if you have an update method:
            // cmdService.updateFacturePdf(selectedCommande.getIdCommande(), cheminPdf);
        }

        // Open with system default PDF viewer
        try {
            if (java.awt.Desktop.isDesktopSupported()
                    && java.awt.Desktop.getDesktop().isSupported(java.awt.Desktop.Action.OPEN)) {
                java.awt.Desktop.getDesktop().open(new File(cheminPdf));
                showAlert(Alert.AlertType.INFORMATION, "Facture exportée",
                        "✅ Facture générée et ouverte :\n" + cheminPdf);
            } else {
                // Fallback: just show the path
                showAlert(Alert.AlertType.INFORMATION, "Facture générée",
                        "✅ Fichier PDF disponible ici :\n" + cheminPdf);
            }
        } catch (Exception e) {
            System.err.println("[onExporterFacture] Impossible d'ouvrir le PDF : " + e.getMessage());
            showAlert(Alert.AlertType.INFORMATION, "Facture générée",
                    "✅ Fichier PDF disponible ici :\n" + cheminPdf
                            + "\n\n(Impossible d'ouvrir automatiquement : " + e.getMessage() + ")");
        }
    }

    // ── CONFIRMER → vérifie adresse → popup société ───────────
    @FXML
    private void onConfirmer() {
        if (selectedCommande == null) return;

        // 1. Validate address via OpenStreetMap API
        String adresse = selectedCommande.getAdresseLivraison();
        boolean adresseOk = adresseService.validerAdresse(adresse);

        if (!adresseOk) {
            // Refuse automatically with address motif
            String motifAdresse = "Adresse de livraison introuvable ou invalide : « "
                    + adresse + " ». Veuillez corriger votre adresse et renvoyer la commande.";
            cmdService.refuser(selectedCommande.getIdCommande(), motifAdresse);
            notifService.envoyerNotification(
                    selectedCommande.getIdAcheteur(),
                    projet.hanouti.GestionCommandes.enums.TypeNotification.COMMANDE,
                    projet.hanouti.GestionCommandes.enums.EventNotification.COMMANDE_REFUSEE,
                    "❌ Commande refusée — adresse invalide",
                    motifAdresse,
                    selectedCommande.getIdCommande()
            );
            refreshSelected();
            showAlert(Alert.AlertType.WARNING, "Adresse invalide",
                    "L'adresse de livraison est introuvable. La commande a été refusée automatiquement.\n"
                            + "L'acheteur a été notifié et peut corriger son adresse.");
            return;
        }

        // 2. Confirm the commande
        cmdService.confirmer(selectedCommande.getIdCommande());

        // 3. Open assign societe popup
        ouvrirPopupAssigner(selectedCommande, () -> {
            refreshSelected();
            notifService.envoyerNotification(
                    selectedCommande.getIdAcheteur(),
                    projet.hanouti.GestionCommandes.enums.TypeNotification.COMMANDE,
                    projet.hanouti.GestionCommandes.enums.EventNotification.COMMANDE_CONFIRMEE,
                    "✅ Commande confirmée",
                    "Votre commande " + selectedCommande.getNumeroCommande() + " a été confirmée.",
                    selectedCommande.getIdCommande()
            );
        });
    }

    // ── REFUSER ───────────────────────────────────────────────
    @FXML
    private void onRefuser() {
        if (selectedCommande == null) return;
        TextInputDialog dlg = new TextInputDialog();
        dlg.setTitle("Refus de commande");
        dlg.setHeaderText(selectedCommande.getNumeroCommande());
        dlg.setContentText("Motif du refus (obligatoire) :");
        dlg.showAndWait().ifPresent(motif -> {
            if (motif.isBlank()) {
                showAlert(Alert.AlertType.WARNING, "Motif requis", "Saisissez un motif."); return;
            }
            cmdService.refuser(selectedCommande.getIdCommande(), motif);
            notifService.envoyerNotification(
                    selectedCommande.getIdAcheteur(),
                    projet.hanouti.GestionCommandes.enums.TypeNotification.COMMANDE,
                    projet.hanouti.GestionCommandes.enums.EventNotification.COMMANDE_REFUSEE,
                    "❌ Commande refusée",
                    "Commande " + selectedCommande.getNumeroCommande()
                            + " refusée. Motif : " + motif,
                    selectedCommande.getIdCommande()
            );
            refreshSelected();
        });
    }

    // ── EN PRÉPARATION ────────────────────────────────────────
    @FXML
    private void onPreparer() {
        if (selectedCommande == null) return;
        cmdService.passerEnPreparation(selectedCommande.getIdCommande());
        refreshSelected();
    }

    // ── EXPÉDIER ──────────────────────────────────────────────
    @FXML
    private void onExpedier() {
        if (selectedCommande == null) return;
        cmdService.expedier(selectedCommande.getIdCommande());
        notifService.envoyerNotification(
                selectedCommande.getIdAcheteur(),
                projet.hanouti.GestionCommandes.enums.TypeNotification.COMMANDE,
                projet.hanouti.GestionCommandes.enums.EventNotification.COMMANDE_EXPEDIEE,
                "🚀 Commande expédiée !",
                "Votre commande " + selectedCommande.getNumeroCommande() + " est en route.",
                selectedCommande.getIdCommande()
        );
        refreshSelected();
    }

    // ── LIVRER → incrémente quantite_vendue ───────────────────
    @FXML
    private void onLivrer() {
        if (selectedCommande == null) return;
        cmdService.livrer(selectedCommande.getIdCommande());

        // Increment quantite_vendue for each product in this order
        List<LigneCommande> lignes = ligneService.getByCommande(selectedCommande.getIdCommande());
        for (LigneCommande l : lignes) {
            incrementQuantiteVendue(l.getIdProduit(), l.getQuantite());
        }

        // Update livraison status
        var liv = livraisonService.getByCommande(selectedCommande.getIdCommande());
        if (liv != null) {
            livraisonService.updateStatut(liv.getIdLivraison(),
                    projet.hanouti.GestionCommandes.enums.StatutLivraison.LIVREE);
        }

        refreshSelected();
    }

    // ── Popup assigner société ────────────────────────────────
    private void ouvrirPopupAssigner(Commande commande, Runnable onSuccess) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/FXML/GestionCommandes/PopupAssignerLivraison.fxml")
            );
            Parent popup = loader.load();
            PopupAssignerLivraisonController ctrl = loader.getController();

            boolean isDark = root.getScene() != null
                    && !root.getScene().getRoot().getStyleClass().contains("light-mode");
            ctrl.init(commande, isDark, onSuccess);

            Stage st = new Stage(StageStyle.TRANSPARENT);
            Scene sc = new Scene(popup);
            sc.setFill(null);
            if (root.getScene() != null)
                sc.getStylesheets().addAll(root.getScene().getStylesheets());
            st.setScene(sc);
            st.initModality(Modality.APPLICATION_MODAL);
            st.initOwner(root.getScene().getWindow());
            st.showAndWait();

        } catch (Exception e) {
            System.err.println("[VendeurCommandesController.ouvrirPopupAssigner] " + e.getMessage());
        }
    }

    // ── Filters ───────────────────────────────────────────────
    @FXML private void onSearch()       { applyFilters(); }
    @FXML private void onFilterStatut() { applyFilters(); }
    @FXML private void onFilterMode()   { applyFilters(); }

    @FXML
    private void onReset() {
        searchField.clear(); filterStatut.setValue(null); filterMode.setValue(null);
        listCommandes.setItems(allCommandes);
        lblCount.setText(allCommandes.size() + " commande(s)");
    }

    private void applyFilters() {
        String q = searchField.getText().toLowerCase().trim();
        String st = filterStatut.getValue(), pm = filterMode.getValue();
        var f = allCommandes.stream().filter(c ->
                (q.isEmpty() || c.getNumeroCommande().toLowerCase().contains(q)
                        || c.getAdresseLivraison().toLowerCase().contains(q))
                        && (st == null || st.equals("Tous") || c.getStatut().name().equals(st))
                        && (pm == null || pm.equals("Tous") || c.getModePaiement().name().equals(pm))
        ).collect(Collectors.toList());
        listCommandes.setItems(FXCollections.observableArrayList(f));
        lblCount.setText(f.size() + " commande(s)");
    }

    // ── Helpers ───────────────────────────────────────────────

    private void refreshSelected() {
        loadCommandes();
        if (selectedCommande != null) {
            Commande r = cmdService.getById(selectedCommande.getIdCommande());
            if (r != null) { selectedCommande = r; showDetail(r); }
        }
    }

    /** Fetch product names from DB and set them on LigneCommande objects */
    private void enrichWithProductNames(List<LigneCommande> lignes) {
        try {
            Connection cnx = MyBD.getInstance().getConnection();
            for (LigneCommande l : lignes) {
                try (PreparedStatement ps = cnx.prepareStatement(
                        "SELECT nom FROM produit WHERE id_produit = ?")) {
                    ps.setInt(1, l.getIdProduit());
                    ResultSet rs = ps.executeQuery();
                    if (rs.next()) l.setNomProduit(rs.getString("nom"));
                }
            }
        } catch (Exception e) {
            System.err.println("[enrichWithProductNames] " + e.getMessage());
        }
    }

    /** Increment quantite_vendue in produits table after delivery */
    private void incrementQuantiteVendue(int idProduit, int quantite) {
        try {
            Connection cnx = MyBD.getInstance().getConnection();
            try (PreparedStatement ps = cnx.prepareStatement(
                    "UPDATE produit SET quantite_vendu = COALESCE(quantite_vendu,0) + ? " +
                            "WHERE id_produit = ?")) {
                ps.setInt(1, quantite);
                ps.setInt(2, idProduit);
                ps.executeUpdate();
            }
        } catch (Exception e) {
            System.err.println("[incrementQuantiteVendue] " + e.getMessage());
        }
    }

    private String formatStatutLiv(projet.hanouti.GestionCommandes.enums.StatutLivraison s) {
        return switch (s) {
            case ASSIGNEE  -> "📋 Assignée";
            case EN_COURS  -> "🚗 En cours";
            case LIVREE    -> "✅ Livrée";
            case ANNULEE   -> "🚫 Annulée";
        };
    }

    private void applyBadge(Label l, StatutCommande s) {
        l.getStyleClass().clear();
        l.getStyleClass().addAll("statut-badge", "badge-" + s.name());
        l.setText(switch (s) {
            case CREEE          -> "🆕 Créée";
            case CONFIRMEE      -> "✅ Confirmée";
            case EN_PREPARATION -> "🔧 Préparation";
            case EXPEDIEE       -> "🚀 Expédiée";
            case LIVREE         -> "📬 Livrée";
            case ANNULEE        -> "🚫 Annulée";
            case REFUSEE        -> "❌ Refusée";
        });
    }

    private void show(Button b) { b.setVisible(true);  b.setManaged(true); }
    private void show(Label  l) { l.setVisible(true);  l.setManaged(true); }

    private void showAlert(Alert.AlertType t, String title, String msg) {
        Alert a = new Alert(t); a.setTitle(title);
        a.setHeaderText(null); a.setContentText(msg); a.showAndWait();
    }

    // ── CommandeCell ──────────────────────────────────────────
    private class CommandeCell extends ListCell<Commande> {
        private final VBox  card  = new VBox(8);
        private final HBox  r1    = new HBox(8), r2 = new HBox(8), r3 = new HBox(8);
        private final Label lNum  = new Label(), lStat  = new Label(),
                lVip  = new Label("⭐ VIP"),
                lDate = new Label(), lAddr  = new Label(),
                lScore = new Label(), lTotal = new Label();

        CommandeCell() {
            Region s1 = new Region(), s2 = new Region(), s3 = new Region();
            HBox.setHgrow(s1, Priority.ALWAYS);
            HBox.setHgrow(s2, Priority.ALWAYS);
            HBox.setHgrow(s3, Priority.ALWAYS);
            r1.getChildren().addAll(lNum, s1, lStat, lVip);
            r2.getChildren().addAll(lAddr);
            r3.getChildren().addAll(lScore, s3, lTotal);
            card.getChildren().addAll(r1, lDate, r2, r3);
            VBox.setMargin(card, new Insets(3, 0, 3, 0));
            lNum.getStyleClass().add("cmd-numero");
            lDate.getStyleClass().add("cmd-date");
            lStat.getStyleClass().add("statut-badge");
            lVip.getStyleClass().add("vip-badge");
            lAddr.getStyleClass().add("cmd-adresse");
            lScore.getStyleClass().add("cmd-score");
            lTotal.getStyleClass().add("cmd-total");
        }

        @Override
        protected void updateItem(Commande c, boolean empty) {
            super.updateItem(c, empty);
            if (empty || c == null) { setGraphic(null); return; }
            lNum.setText(c.getNumeroCommande());
            lDate.setText(c.getDateCreation() != null
                    ? "📅 " + c.getDateCreation().format(DT_FMT) : "");
            String addr = c.getAdresseLivraison();
            lAddr.setText("📍 " + (addr.length() > 35 ? addr.substring(0, 35) + "…" : addr));
            lScore.setText("⚡ " + c.getScorePriorite() + " pts");
            lTotal.setText(String.format("%.2f TND", c.getTotal()));
            lStat.getStyleClass().removeIf(s -> s.startsWith("badge-"));
            lStat.getStyleClass().add("badge-" + c.getStatut().name());
            lStat.setText(switch (c.getStatut()) {
                case CREEE -> "🆕"; case CONFIRMEE -> "✅";
                case EN_PREPARATION -> "🔧"; case EXPEDIEE -> "🚀";
                case LIVREE -> "📬"; case ANNULEE -> "🚫"; case REFUSEE -> "❌";
            });
            boolean vip = cmdService.isAcheteurVIP(c.getIdAcheteur());
            lVip.setVisible(vip); lVip.setManaged(vip);
            card.getStyleClass().clear();
            card.getStyleClass().addAll("cmd-card", "statut-" + c.getStatut().name());
            boolean sel = selectedCommande != null
                    && selectedCommande.getIdCommande() == c.getIdCommande();
            if (sel) card.getStyleClass().add("cmd-card-selected");
            setGraphic(card);
        }
    }

    // ── LigneCell — product name, not ID ─────────────────────
    private static class LigneCell extends ListCell<LigneCommande> {
        private final HBox  row  = new HBox(10);
        private final Label lNom = new Label(), lQte = new Label(), lPrix = new Label();

        LigneCell() {
            Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
            row.getChildren().addAll(lNom, lQte, sp, lPrix);
            row.getStyleClass().add("ligne-item");
            row.setAlignment(Pos.CENTER_LEFT);
            row.setPadding(new Insets(8, 10, 8, 10));
            lNom.getStyleClass().add("ligne-produit");
            lQte.getStyleClass().add("ligne-qte");
            lPrix.getStyleClass().add("ligne-prix");
        }

        @Override
        protected void updateItem(LigneCommande l, boolean empty) {
            super.updateItem(l, empty);
            if (empty || l == null) { setGraphic(null); return; }
            String nom = (l.getNomProduit() != null && !l.getNomProduit().isBlank())
                    ? l.getNomProduit() : "Produit inconnu";
            lNom.setText(nom);
            lQte.setText("× " + l.getQuantite());
            lPrix.setText(String.format("%.2f TND", l.getSousTotal()));
            setGraphic(row);
        }
    }
}
