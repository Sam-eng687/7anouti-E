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
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Modality;
import javafx.stage.Stage;
import netscape.javascript.JSObject;
import projet.hanouti.GestionCommandes.entities.Commande;
import projet.hanouti.GestionCommandes.entities.LigneCommande;
import projet.hanouti.GestionCommandes.enums.ModePaiement;
import projet.hanouti.GestionCommandes.enums.StatutCommande;
import projet.hanouti.common.utils.MyBD;
import projet.hanouti.common.utils.SessionManager;
import projet.hanouti.GestionCommandes.services.AdresseValidationService;
import projet.hanouti.GestionCommandes.services.CommandeService;
import projet.hanouti.GestionCommandes.services.LigneCommandeService;
import projet.hanouti.GestionCommandes.services.SocieteLivraisonService;

import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

/**
 * AcheteurCommandesController
 * • Clic sur commande → detail panel inline (pas de popup)
 * • Adresse modifiable par saisie (Entrée pour valider via OSM) OU par clic sur la carte (reverse geocoding Nominatim)
 * • Date préférée et quantités modifiables
 * • Bouton "Valider les modifications" apparaît dès qu'une modification est détectée, enregistre tout en une fois
 * • Annuler → DELETE from DB → disparaît de la liste
 * • REFUSÉE → bouton "Renvoyer"
 * • EXPÉDIÉE → bouton "Suivre livraison"
 */
public class AcheteurCommandesController implements Initializable {

    // ── FXML ──────────────────────────────────────────────────
    @FXML private HBox  root;
    @FXML private Label kpiTotal, kpiEnCours, kpiLivrees;
    @FXML private HBox  vipStrip;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> filterStatut;
    @FXML private Label lblCount;
    @FXML private ListView<Commande> listCommandes;

    // Detail panel
    @FXML private VBox       emptyState;
    @FXML private ScrollPane detailContent;
    @FXML private Label  detNumero, detDate, detStatut;
    @FXML private Label  detPaiement, detTotal;
    @FXML private TextField detAdresse;
    @FXML private Label  adresseValidIcon, adresseHint, mapClickHint;
    @FXML private StackPane mapContainer;
    @FXML private Label  mapPlaceholder;
    @FXML private DatePicker detDatePref;
    @FXML private ListView<LigneCommande> listLignes;
    @FXML private Label  detNbProduits, detSociete, detStatutLiv;
    @FXML private VBox   motifBox, renvoyerBox;
    @FXML private Label  detMotif;
    @FXML private Button btnAnnuler, btnSuivre, btnFacture;

    // Validation button block
    @FXML private VBox   validationBox;
    @FXML private Label  validationHint;
    @FXML private Button btnValiderModifs, btnAnnulerModifs;

    // ── Services ──────────────────────────────────────────────
    private final CommandeService          cmdService     = new CommandeService();
    private final LigneCommandeService     ligneService   = new LigneCommandeService();
    private final AdresseValidationService adresseService = new AdresseValidationService();
    private final SocieteLivraisonService  societeService = new SocieteLivraisonService();

    // ── State ─────────────────────────────────────────────────
    private ObservableList<Commande> allCommandes = FXCollections.observableArrayList();
    private Commande selectedCommande = null;
    private boolean adresseValide = true;

    /** Snapshot of values when the commande was loaded — used to detect changes */
    private String   originalAdresse;
    private LocalDate originalDatePref;

    /** Tracks pending changes */
    private boolean pendingAdresse  = false;
    private boolean pendingDate     = false;
    private boolean pendingQuantite = false;

    /** Validated (normalized) address ready to be saved */
    private String pendingAdresseValue = null;

    private WebView currentMapWebView = null;
    /** Strong reference — prevents JSObject bridge from being garbage collected */
    private MapBridge mapBridge = null;

    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    // ── Init ──────────────────────────────────────────────────
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        filterStatut.setItems(FXCollections.observableArrayList(
                "Tous","CREEE","CONFIRMEE","EN_PREPARATION","EXPEDIEE","LIVREE","REFUSEE"
        ));
        listCommandes.setCellFactory(lv -> new CommandeCell());
        loadCommandes();
    }

    // ── Data ──────────────────────────────────────────────────
    private void loadCommandes() {
        int id = SessionManager.getCurrentUserId();
        boolean vip = cmdService.isAcheteurVIP(id);
        vipStrip.setVisible(vip); vipStrip.setManaged(vip);

        List<Commande> list = cmdService.getByAcheteur(id).stream()
                .filter(c -> c.getStatut() != StatutCommande.ANNULEE)
                .collect(Collectors.toList());

        allCommandes = FXCollections.observableArrayList(list);
        listCommandes.setItems(allCommandes);

        kpiTotal.setText(String.valueOf(list.size()));
        kpiEnCours.setText(String.valueOf(list.stream()
                .filter(c -> c.getStatut() == StatutCommande.CONFIRMEE
                        || c.getStatut() == StatutCommande.EN_PREPARATION
                        || c.getStatut() == StatutCommande.EXPEDIEE).count()));
        kpiLivrees.setText(String.valueOf(list.stream()
                .filter(c -> c.getStatut() == StatutCommande.LIVREE).count()));
        lblCount.setText(list.size() + " commande(s)");
    }

    // ── Commande clicked ─────────────────────────────────────
    @FXML
    private void onCommandeClicked() {
        Commande c = listCommandes.getSelectionModel().getSelectedItem();
        if (c == null) return;
        selectedCommande = c;
        showDetail(c);
        listCommandes.refresh();
    }

    private void showDetail(Commande c) {
        // Reset pending change state
        resetPendingChanges();

        emptyState.setVisible(false);   emptyState.setManaged(false);
        detailContent.setVisible(true); detailContent.setManaged(true);

        // Header
        detNumero.setText(c.getNumeroCommande());
        detDate.setText(c.getDateCreation() != null ? c.getDateCreation().format(DT_FMT) : "—");
        applyBadge(detStatut, c.getStatut());

        // Infos
        detPaiement.setText(c.getModePaiement().name());
        detTotal.setText(String.format("%.2f TND", c.getTotal()));

        // Adresse
        originalAdresse = c.getAdresseLivraison();
        detAdresse.setText(originalAdresse);
        adresseValidIcon.setText("");
        adresseHint.setText("💡 Saisissez une adresse (Entrée) ou cliquez un point sur la carte");
        boolean modifiable = isModifiable(c.getStatut());
        detAdresse.setEditable(modifiable);
        detAdresse.setStyle(modifiable ? "" : "-fx-opacity:0.7;");

        // Date préférée
        originalDatePref = c.getDateLivraisonPreferee();
        detDatePref.setValue(originalDatePref);
        detDatePref.setDisable(!modifiable);

        // Produits
        List<LigneCommande> lignes = ligneService.getByCommande(c.getIdCommande());
        enrichLignesWithProductNames(lignes);
        listLignes.setCellFactory(lv -> new LigneCell(modifiable));
        listLignes.setItems(FXCollections.observableArrayList(lignes));
        detNbProduits.setText(lignes.size() + " produit(s)");

        // Société
        if (c.getIdSocieteLivraison() != null) {
            var soc = societeService.getById(c.getIdSocieteLivraison());
            detSociete.setText(soc != null ? "🚚 " + soc.getNomSociete() : "—");
        } else {
            detSociete.setText("Non encore assignée");
        }
        detStatutLiv.setText("");

        // Map (with click support if modifiable)
        if (!c.getAdresseLivraison().isBlank()) {
            loadMap(c.getAdresseLivraison(), modifiable);
        }
        mapClickHint.setVisible(modifiable); mapClickHint.setManaged(modifiable);

        // Motif refus
        boolean refusee = c.getStatut() == StatutCommande.REFUSEE;
        motifBox.setVisible(refusee); motifBox.setManaged(refusee);
        if (refusee && c.getMotifRefus() != null) detMotif.setText(c.getMotifRefus());

        // Actions
        // Annuler only for non-refused modifiable orders
        boolean canCancel = modifiable && c.getStatut() != StatutCommande.REFUSEE;
        btnAnnuler.setVisible(canCancel); btnAnnuler.setManaged(canCancel);
        btnSuivre.setVisible(c.getStatut() == StatutCommande.EXPEDIEE);
        btnSuivre.setManaged(c.getStatut() == StatutCommande.EXPEDIEE);
        // renvoyerBox only visible for REFUSEE AND only after changes have been validated
        // It is shown by showRenvoyerIfReady() called from onValiderModifications
        renvoyerBox.setVisible(false); renvoyerBox.setManaged(false);
        boolean aFacture = c.getStatut() == StatutCommande.LIVREE
                && c.getFacturePdf() != null && !c.getFacturePdf().isBlank();
        btnFacture.setVisible(aFacture); btnFacture.setManaged(aFacture);
    }

    private boolean isModifiable(StatutCommande s) {
        if (s == StatutCommande.CREEE
                || s == StatutCommande.CONFIRMEE
                || s == StatutCommande.EN_PREPARATION) {
            return true;
        }
        if (s == StatutCommande.REFUSEE && selectedCommande != null) {
            String motif = selectedCommande.getMotifRefus();
            return motif != null && motif.toLowerCase().contains("adresse");
        }
        return false;
    }

    // ── Pending changes tracking ──────────────────────────────

    private void resetPendingChanges() {
        pendingAdresse  = false;
        pendingDate     = false;
        pendingQuantite = false;
        pendingAdresseValue = null;
        adresseValide = true;
        updateValidationBox();
    }

    /**
     * Called whenever any field changes. Shows/hides the validation button.
     */
    private void markDirty(String reason) {
        boolean hasPending = pendingAdresse || pendingDate || pendingQuantite;
        validationBox.setVisible(hasPending);
        validationBox.setManaged(hasPending);
        if (hasPending) {
            validationHint.setText("✏️  " + reason);
        }
    }

    private void updateValidationBox() {
        boolean hasPending = pendingAdresse || pendingDate || pendingQuantite;
        validationBox.setVisible(hasPending);
        validationBox.setManaged(hasPending);
    }

    // ── Valider les modifications (save all pending) ──────────
    @FXML
    private void onValiderModifications() {
        if (selectedCommande == null) return;

        if (pendingAdresse) {
            if (pendingAdresseValue == null) {
                Alert a = new Alert(Alert.AlertType.WARNING);
                a.setContentText("L'adresse n'est pas encore validée.\nAppuyez sur Entrée dans le champ adresse pour la valider avant d'enregistrer.");
                a.showAndWait();
                return;
            }
            if (!adresseValide) {
                Alert a = new Alert(Alert.AlertType.WARNING);
                a.setContentText("L'adresse saisie est introuvable. Veuillez la corriger.");
                a.showAndWait();
                return;
            }
            cmdService.updateAdresse(selectedCommande.getIdCommande(), pendingAdresseValue);
            selectedCommande.setAdresseLivraison(pendingAdresseValue);
            originalAdresse = pendingAdresseValue;
        }

        if (pendingDate) {
            LocalDate d = detDatePref.getValue();
            cmdService.updateDateLivraisonPreferee(selectedCommande.getIdCommande(), d);
            selectedCommande.setDateLivraisonPreferee(d);
            originalDatePref = d;
        }

        if (pendingQuantite) {
            // Recalculate total by summing quantite * prix_unitaire from ligne_commandes.
            // commandes has no sous_total column — only the total column is updated.
            double newTotal = recalculerTotalDepuisLignes(selectedCommande.getIdCommande());
            updateTotalCommande(selectedCommande.getIdCommande(), newTotal);
            selectedCommande.setTotal(newTotal);
            detTotal.setText(String.format("%.2f TND", newTotal));
        }

        resetPendingChanges();
        adresseValidIcon.setText("✅");
        adresseHint.setText("✅  Toutes les modifications ont été enregistrées");
        listCommandes.refresh();

        // For a refused commande: reveal the "Renvoyer" button now that changes are saved
        if (selectedCommande.getStatut() == StatutCommande.REFUSEE) {
            renvoyerBox.setVisible(true); renvoyerBox.setManaged(true);
        }
    }

    /** SUM(quantite * prix_unitaire) from ligne_commandes */
    private double recalculerTotalDepuisLignes(int idCommande) {
        try {
            Connection cnx = MyBD.getInstance().getConnection();
            try (PreparedStatement ps = cnx.prepareStatement(
                    "SELECT SUM(quantite * prix_unitaire) FROM ligne_commandes WHERE id_commande = ?")) {
                ps.setInt(1, idCommande);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) return rs.getDouble(1);
            }
        } catch (Exception e) {
            System.err.println("[recalculerTotalDepuisLignes] " + e.getMessage());
        }
        return 0.0;
    }

    /** UPDATE commandes SET total = ? */
    private void updateTotalCommande(int idCommande, double total) {
        try {
            Connection cnx = MyBD.getInstance().getConnection();
            try (PreparedStatement ps = cnx.prepareStatement(
                    "UPDATE commandes SET total = ? WHERE id_commande = ?")) {
                ps.setDouble(1, total);
                ps.setInt(2, idCommande);
                ps.executeUpdate();
            }
        } catch (Exception e) {
            System.err.println("[updateTotalCommande] " + e.getMessage());
        }
    }

    /** Discard pending changes and restore original values */
    @FXML
    private void onAnnulerModifications() {
        if (selectedCommande == null) return;

        // Restore address
        detAdresse.setText(originalAdresse);
        adresseValidIcon.setText("");
        adresseHint.setText("💡 Saisissez une adresse (Entrée) ou cliquez un point sur la carte");

        // Restore date
        detDatePref.setValue(originalDatePref);

        // Reload map — isModifiable now includes REFUSEE so the map stays clickable
        if (!originalAdresse.isBlank())
            loadMap(originalAdresse, isModifiable(selectedCommande.getStatut()));

        // Reload lignes (restores quantities from DB)
        List<LigneCommande> lignes = ligneService.getByCommande(selectedCommande.getIdCommande());
        enrichLignesWithProductNames(lignes);
        listLignes.setItems(FXCollections.observableArrayList(lignes));
        detTotal.setText(String.format("%.2f TND", selectedCommande.getTotal()));

        // Hide renvoyer box — requires a fresh validated save before resending
        renvoyerBox.setVisible(false); renvoyerBox.setManaged(false);

        resetPendingChanges();
    }

    // ── Adresse live validation (typed) ──────────────────────
    @FXML
    private void onAdresseKeyReleased() {
        // Only give visual feedback — do NOT touch pendingAdresse here.
        // pendingAdresse is set only after Entrée (onAdresseChanged) or map click (MapBridge).
        // This prevents the button appearing before the user has validated anything.
        String current = detAdresse.getText().trim();
        if (!current.equals(originalAdresse)) {
            adresseValidIcon.setText("");
            adresseHint.setText("⚠️  Appuyez sur Entrée pour valider et afficher la carte");
        } else {
            // Restored to original — cancel any pending address change
            adresseValidIcon.setText("");
            adresseHint.setText("💡 Saisissez une adresse (Entrée) ou cliquez un point sur la carte");
            pendingAdresse = false;
            pendingAdresseValue = null;
            adresseValide = true;
            updateValidationBox();
        }
    }

    @FXML
    private void onAdresseChanged() {
        if (selectedCommande == null) return;
        String newAddr = detAdresse.getText().trim();
        if (newAddr.isBlank()) return;

        adresseValidIcon.setText("⏳");
        adresseHint.setText("Validation en cours…");
        new Thread(() -> {
            boolean valid = adresseService.validerAdresse(newAddr);
            String normalized = valid ? adresseService.normaliserAdresse(newAddr) : newAddr;
            javafx.application.Platform.runLater(() -> {
                adresseValide = valid;
                if (valid) {
                    adresseValidIcon.setText("✅");
                    adresseHint.setText("✅ Adresse validée — cliquez 'Valider les modifications' pour enregistrer");
                    detAdresse.setText(normalized);
                    pendingAdresseValue = normalized;
                    // Always mark dirty after a successful Enter validation
                    pendingAdresse = true;
                    markDirty("Adresse modifiée");
                    loadMap(normalized, isModifiable(selectedCommande.getStatut()));
                } else {
                    adresseValidIcon.setText("❌");
                    adresseHint.setText("❌ Adresse introuvable — veuillez la corriger.");
                    pendingAdresse = false;
                    pendingAdresseValue = null;
                    updateValidationBox();
                }
            });
        }).start();
    }

    // ── Map (OpenStreetMap + Leaflet, clic → reverse geocoding) ──
    private void loadMap(String adresse, boolean clickable) {
        try {
            WebView webView = new WebView();
            WebEngine engine = webView.getEngine();
            currentMapWebView = webView;

            // Expose a Java bridge so Leaflet can call back into JavaFX
            // Keep a strong reference in mapBridge field — otherwise GC kills it and JS calls silently fail
            mapBridge = new MapBridge();
            engine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
                if (newState == javafx.concurrent.Worker.State.SUCCEEDED) {
                    JSObject window = (JSObject) engine.executeScript("window");
                    window.setMember("javaApp", mapBridge);
                }
            });

            String encoded = java.net.URLEncoder.encode(adresse, java.nio.charset.StandardCharsets.UTF_8);
            String escapedAddr = adresse.replace("'", "\\'").replace("\n", " ");

            // clickHandlerJS: if clickable, register a click listener that reverse geocodes
            String clickHandlerJS = clickable ? """
                map.on('click', function(e) {
                    const lat = e.latlng.lat, lng = e.latlng.lng;
                    fetch('https://nominatim.openstreetmap.org/reverse?lat=' + lat + '&lon=' + lng + '&format=json')
                        .then(r => r.json())
                        .then(data => {
                            const addr = data.display_name || '';
                            if (marker) { marker.setLatLng([lat, lng]); }
                            else { marker = L.marker([lat, lng]).addTo(map); }
                            marker.bindPopup(addr).openPopup();
                            if (window.javaApp) { window.javaApp.onMapClick(addr, lat, lng); }
                        });
                });
                """ : "";

            String html = """
                <!DOCTYPE html><html><head>
                <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css"/>
                <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
                <style>
                  body{margin:0;padding:0;}
                  #map{width:100%%;height:200px;}
                  .leaflet-container{cursor:%s !important;}
                </style>
                </head><body>
                <div id="map"></div>
                <script>
                var map = null, marker = null;
                fetch('https://nominatim.openstreetmap.org/search?q=%s&format=json&limit=1')
                  .then(r => r.json()).then(data => {
                    var lat = 36.8065, lng = 10.1815; // Tunis fallback
                    if (data.length > 0) { lat = parseFloat(data[0].lat); lng = parseFloat(data[0].lon); }
                    map = L.map('map').setView([lat, lng], 14);
                    L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png',
                      {attribution:'© OpenStreetMap contributors'}).addTo(map);
                    marker = L.marker([lat, lng]).addTo(map).bindPopup('%s').openPopup();
                    %s
                  });
                </script></body></html>
                """.formatted(
                    clickable ? "crosshair" : "grab",
                    encoded,
                    escapedAddr,
                    clickHandlerJS
            );

            engine.loadContent(html);
            webView.setPrefHeight(200);
            mapPlaceholder.setVisible(false); mapPlaceholder.setManaged(false);
            mapContainer.getChildren().setAll(webView);
        } catch (Exception e) {
            mapPlaceholder.setVisible(true); mapPlaceholder.setManaged(true);
            mapPlaceholder.setText("🗺️ Carte indisponible");
        }
    }

    /**
     * Bridge object exposed to JavaScript via window.javaApp.
     * Leaflet calls onMapClick(address, lat, lng) when the user clicks the map.
     */
    public class MapBridge {
        public void onMapClick(String address, double lat, double lng) {
            javafx.application.Platform.runLater(() -> {
                if (selectedCommande == null || address == null || address.isBlank()) return;

                // Update the address TextField with the reverse-geocoded address
                detAdresse.setText(address);
                adresseValidIcon.setText("📍");
                adresseHint.setText("📍 Point sélectionné sur la carte — cliquez 'Valider les modifications' pour enregistrer");
                adresseValide = true;
                pendingAdresseValue = address;
                // Always dirty when coming from map click — user explicitly chose a new point
                pendingAdresse = true;
                markDirty("Adresse choisie sur la carte");
            });
        }
    }

    // ── Date préférée ─────────────────────────────────────────
    @FXML
    private void onDatePrefChanged() {
        if (selectedCommande == null) return;
        LocalDate d = detDatePref.getValue();
        boolean changed = (d == null && originalDatePref != null)
                || (d != null && !d.equals(originalDatePref));
        pendingDate = changed;
        if (changed) {
            markDirty("Date de livraison modifiée — cliquez 'Valider' pour enregistrer");
        } else {
            updateValidationBox();
        }
    }

    @FXML
    private void onClearDatePref() {
        detDatePref.setValue(null);
        pendingDate = originalDatePref != null; // dirty only if there was a date before
        if (pendingDate) {
            markDirty("Date de livraison effacée — cliquez 'Valider' pour enregistrer");
        } else {
            updateValidationBox();
        }
    }

    // ── Annuler commande → DELETE from DB ────────────────────
    @FXML
    private void onAnnuler() {
        if (selectedCommande == null) return;
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Annuler la commande");
        confirm.setContentText("Annuler définitivement " + selectedCommande.getNumeroCommande() + " ?");
        confirm.showAndWait().ifPresent(r -> {
            if (r == ButtonType.OK) {
                deleteCommande(selectedCommande.getIdCommande());
                allCommandes.remove(selectedCommande);
                listCommandes.setItems(FXCollections.observableArrayList(allCommandes));
                selectedCommande = null;
                resetPendingChanges();
                detailContent.setVisible(false); detailContent.setManaged(false);
                emptyState.setVisible(true);     emptyState.setManaged(true);
                loadCommandes();
            }
        });
    }

    private void deleteCommande(int idCommande) {
        try {
            Connection cnx = MyBD.getInstance().getConnection();
            try (PreparedStatement ps = cnx.prepareStatement(
                    "DELETE FROM ligne_commandes WHERE id_commande = ?")) {
                ps.setInt(1, idCommande); ps.executeUpdate();
            }
            try (PreparedStatement ps = cnx.prepareStatement(
                    "DELETE FROM commandes WHERE id_commande = ?")) {
                ps.setInt(1, idCommande); ps.executeUpdate();
            }
        } catch (Exception e) {
            System.err.println("[AcheteurCommandesController.deleteCommande] " + e.getMessage());
        }
    }

    // ── Suivre livraison ──────────────────────────────────────
    @FXML
    private void onSuivreLivraison() {
        if (selectedCommande == null) return;
        Alert info = new Alert(Alert.AlertType.INFORMATION);
        info.setTitle("Suivi livraison");
        info.setHeaderText("Commande : " + selectedCommande.getNumeroCommande());
        info.setContentText("Le module de suivi en temps réel sera disponible "
                + "une fois intégré avec le module de votre collègue.\n"
                + "Société assignée : " + getSocieteNom(selectedCommande.getIdSocieteLivraison()));
        info.showAndWait();
    }

    // ── Renvoyer commande (si REFUSÉE, après validation des modifications) ────
    @FXML
    private void onRenvoyer() {
        if (selectedCommande == null) return;

        // Address must have been validated and saved via "Valider les modifications" first
        String adresseFinale = selectedCommande.getAdresseLivraison();
        if (adresseFinale == null || adresseFinale.isBlank()) {
            Alert a = new Alert(Alert.AlertType.WARNING);
            a.setContentText("L'adresse de livraison est vide. Veuillez la saisir et valider avant de renvoyer.");
            a.showAndWait(); return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Renvoyer la commande");
        confirm.setContentText(
                "La commande sera recréée avec :\n" +
                        "• Adresse : " + adresseFinale + "\n" +
                        "• Quantités mises à jour\n\n" +
                        "Confirmer l'envoi au vendeur ?"
        );
        confirm.showAndWait().ifPresent(r -> {
            if (r == ButtonType.OK) {
                // Fetch lignes fresh from DB — quantities are already updated there
                List<LigneCommande> lignes = ligneService.getByCommande(selectedCommande.getIdCommande());

                Commande nouvelle = cmdService.createFromPanier(
                        selectedCommande.getIdAcheteur(),
                        selectedCommande.getIdVendeur(),
                        lignes,
                        selectedCommande.getModePaiement(),
                        adresseFinale
                );
                if (detDatePref.getValue() != null)
                    cmdService.updateDateLivraisonPreferee(nouvelle.getIdCommande(), detDatePref.getValue());

// Delete the old refused commande from DB
                int oldId = selectedCommande.getIdCommande();
                deleteCommande(oldId);
                allCommandes.removeIf(c -> c.getIdCommande() == oldId);

                loadCommandes();
                resetPendingChanges();

                Alert ok = new Alert(Alert.AlertType.INFORMATION);
                ok.setContentText("✅ Commande " + nouvelle.getNumeroCommande() + " créée et envoyée au vendeur !");
                ok.showAndWait();

                selectedCommande = null;
                detailContent.setVisible(false); detailContent.setManaged(false);
                emptyState.setVisible(true);     emptyState.setManaged(true);
            }
        });
    }

    // ── Facture ───────────────────────────────────────────────
    @FXML
    private void onFacture() {
        if (selectedCommande == null || selectedCommande.getFacturePdf() == null) return;
        try { java.awt.Desktop.getDesktop().open(new java.io.File(selectedCommande.getFacturePdf())); }
        catch (Exception e) { new Alert(Alert.AlertType.ERROR, "Erreur : " + e.getMessage()).showAndWait(); }
    }

    // ── Filters ───────────────────────────────────────────────
    @FXML private void onSearch() { applyFilters(); }
    @FXML private void onFilter() { applyFilters(); }

    @FXML
    private void onReset() {
        searchField.clear(); filterStatut.setValue(null);
        listCommandes.setItems(allCommandes);
        lblCount.setText(allCommandes.size() + " commande(s)");
    }

    private void applyFilters() {
        String q = searchField.getText().toLowerCase().trim();
        String st = filterStatut.getValue();
        var f = allCommandes.stream().filter(c ->
                (q.isEmpty() || c.getNumeroCommande().toLowerCase().contains(q)
                        || c.getAdresseLivraison().toLowerCase().contains(q))
                        && (st == null || st.equals("Tous") || c.getStatut().name().equals(st))
        ).collect(Collectors.toList());
        listCommandes.setItems(FXCollections.observableArrayList(f));
        lblCount.setText(f.size() + " commande(s)");
    }

    // ── Helpers ───────────────────────────────────────────────

    private void enrichLignesWithProductNames(List<LigneCommande> lignes) {
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
            System.err.println("[enrichLignesWithProductNames] " + e.getMessage());
        }
    }

    private String getSocieteNom(Integer idSociete) {
        if (idSociete == null) return "Non assignée";
        var s = societeService.getById(idSociete);
        return s != null ? s.getNomSociete() : "—";
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

    private String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() > max ? s.substring(0, max) + "…" : s;
    }

    // ══════════════════════════════════════════════════════════
    // CommandeCell
    // ══════════════════════════════════════════════════════════
    private class CommandeCell extends ListCell<Commande> {
        private final VBox  card  = new VBox(7);
        private final HBox  r1    = new HBox(8), r2 = new HBox(8);
        private final Label lNum  = new Label(), lStat = new Label(),
                lDate = new Label(), lAddr = new Label(),
                lTotal = new Label();

        CommandeCell() {
            Region s1 = new Region(), s2 = new Region();
            HBox.setHgrow(s1, Priority.ALWAYS); HBox.setHgrow(s2, Priority.ALWAYS);
            r1.getChildren().addAll(lNum, s1, lStat);
            r2.getChildren().addAll(lAddr, s2, lTotal);
            card.getChildren().addAll(r1, lDate, r2);
            VBox.setMargin(card, new Insets(3, 0, 3, 0));
            lNum.getStyleClass().add("cmd-numero");
            lDate.getStyleClass().add("cmd-date");
            lStat.getStyleClass().add("statut-badge");
            lAddr.getStyleClass().add("cmd-adresse");
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
            lAddr.setText("📍 " + (addr.length() > 32 ? addr.substring(0, 32) + "…" : addr));
            lTotal.setText(String.format("%.2f TND", c.getTotal()));
            lStat.getStyleClass().removeIf(s -> s.startsWith("badge-"));
            lStat.getStyleClass().add("badge-" + c.getStatut().name());
            lStat.setText(switch (c.getStatut()) {
                case CREEE -> "🆕"; case CONFIRMEE -> "✅";
                case EN_PREPARATION -> "🔧"; case EXPEDIEE -> "🚀";
                case LIVREE -> "📬"; case ANNULEE -> "🚫"; case REFUSEE -> "❌";
            });
            card.getStyleClass().clear();
            card.getStyleClass().addAll("cmd-card", "statut-" + c.getStatut().name());
            boolean sel = selectedCommande != null
                    && selectedCommande.getIdCommande() == c.getIdCommande();
            if (sel) card.getStyleClass().add("cmd-card-selected");
            setGraphic(card);
        }
    }

    // ══════════════════════════════════════════════════════════
    // LigneCell — inline quantity spinner, marks dirty on change
    // ══════════════════════════════════════════════════════════
    private class LigneCell extends ListCell<LigneCommande> {
        private final HBox  row     = new HBox(10);
        private final Label lNom    = new Label();
        private final Region sp     = new Region();
        private final Spinner<Integer> spinner = new Spinner<>(1, 999, 1);
        private final Label lPrix   = new Label();
        private final boolean editable;

        LigneCell(boolean editable) {
            this.editable = editable;
            HBox.setHgrow(sp, Priority.ALWAYS);
            spinner.setPrefWidth(80);
            spinner.setEditable(true);
            row.getChildren().addAll(lNom, sp, spinner, lPrix);
            row.getStyleClass().add("ligne-item");
            row.setAlignment(Pos.CENTER_LEFT);
            lNom.getStyleClass().add("ligne-produit");
            lPrix.getStyleClass().add("ligne-prix");

            spinner.valueProperty().addListener((obs, oldV, newV) -> {
                LigneCommande ligne = getItem();
                if (ligne == null || newV == null || newV.equals(oldV)) return;
                ligne.setQuantite(newV);
                ligne.setSousTotal(ligneService.calculerSousTotal(newV, ligne.getPrixUnitaire()));
                // Save quantity immediately (no need to batch this one)
                ligneService.updateQuantite(ligne.getIdLigne(), newV);
                lPrix.setText(String.format("%.2f TND", ligne.getSousTotal()));
                if (selectedCommande != null) {
                    // Use direct SQL SUM — commandes has no sous_total column
                    double newTotal = recalculerTotalDepuisLignes(selectedCommande.getIdCommande());
                    selectedCommande.setTotal(newTotal);
                    detTotal.setText(String.format("%.2f TND", newTotal));
                }
                // Mark as dirty so user sees the validation button
                pendingQuantite = true;
                markDirty("Quantité modifiée — cliquez 'Valider' pour confirmer");
            });
        }

        @Override
        protected void updateItem(LigneCommande l, boolean empty) {
            super.updateItem(l, empty);
            if (empty || l == null) { setGraphic(null); return; }
            String nom = (l.getNomProduit() != null && !l.getNomProduit().isBlank())
                    ? l.getNomProduit() : "Produit #" + l.getIdProduit();
            lNom.setText(nom);
            spinner.getValueFactory().setValue(l.getQuantite());
            spinner.setDisable(!editable);
            lPrix.setText(String.format("%.2f TND", l.getSousTotal()));
            setGraphic(row);
        }
    }
}
