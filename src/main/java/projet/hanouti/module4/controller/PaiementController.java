package com.hanouti.hanoutiem4.controller;

import com.hanouti.hanoutiem4.UserSession;
import com.hanouti.hanoutiem4.dao.PaiementDAO;
import com.hanouti.hanoutiem4.model.Paiement;
import com.hanouti.hanoutiem4.service.EmailService;
import com.hanouti.hanoutiem4.service.StripeService;
import com.hanouti.hanoutiem4.dao.CodePromoDAO;
import javafx.animation.*;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.awt.Desktop;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.net.URL;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;

public class PaiementController {

    @FXML private Label labelCarteType;
    @FXML private Label labelNumeroCarte;
    @FXML private Label labelNomCarte;
    @FXML private Label labelExpiry;
    @FXML private VBox  carteVisuelle;

    @FXML private Button btnCarte;
    @FXML private Button btnCIB;
    @FXML private Button btnD17;
    @FXML private Button btnEspeces;

    @FXML private VBox formulaireCarte;
    @FXML private VBox formD17;
    @FXML private VBox messageLivraison;

    @FXML private TextField fieldNom;
    @FXML private TextField fieldNumero;
    @FXML private TextField fieldExpiry;
    @FXML private TextField fieldCVV;

    @FXML private Label erreurNom;
    @FXML private Label erreurNumero;
    @FXML private Label erreurExpiry;
    @FXML private Label erreurCVV;

    @FXML private TextField fieldD17Numero;
    @FXML private TextField fieldD17Pin;
    @FXML private Label     erreurD17;
    @FXML private Label     erreurD17Numero;
    @FXML private Label     erreurD17Pin;

    @FXML private Label      labelMontant;
    @FXML private Button     themeBtn;
    @FXML private AnchorPane rootPane;
    @FXML private Button     menuBtn;
    @FXML private Button     btnConfirmer;

    private String  selectedMethod = "Carte";
    private double  montantTotal   = 0;
    private double  montantApresReduction = 0;  // montant après code promo
    private int     currentUserId;
    private String  userEmail      = "demo@example.com";
    private boolean isDarkMode     = false;

    // ── Code promo ────────────────────────────────────────
    private CodePromoDAO.ResultatCode codePromoApplique = null;
    private Label  labelCodePromoMsg;
    private Label  labelMontantReduit;
    private Label  labelReductionLigne;

    private AnchorPane drawerOverlay;
    private VBox       drawer;
    private boolean    drawerOpen = false;

    private static final String CLAUDE_API_KEY = "REMPLACE_PAR_TA_CLE_API_ANTHROPIC";
    private static final String ERROR_BORDER   = "-fx-border-color: #EF4444; -fx-border-width: 1.5; -fx-border-radius: 8;";
    private static final String NORMAL_BORDER  = "";
    // Per-method button styles matching badge colors from Historique/Panier
    private static final String INACTIVE_STYLE =
            "-fx-background-color:white;-fx-background-radius:12;"
                    + "-fx-border-color:rgba(99,102,241,0.20);-fx-border-width:1.5;-fx-border-radius:12;"
                    + "-fx-font-weight:bold;-fx-font-size:13px;-fx-padding:10 20;-fx-cursor:hand;"
                    + "-fx-focus-color:transparent;-fx-faint-focus-color:transparent;";
    private static final String ACTIVE_VISA  =
            "-fx-background-color:#eff6ff;-fx-background-radius:12;"
                    + "-fx-border-color:#1a1f71;-fx-border-width:2;-fx-border-radius:12;"
                    + "-fx-text-fill:#1a1f71;-fx-font-weight:bold;-fx-font-size:13px;-fx-padding:10 20;-fx-cursor:hand;"
                    + "-fx-focus-color:transparent;-fx-faint-focus-color:transparent;";
    private static final String ACTIVE_CIB   =
            "-fx-background-color:#fff5f5;-fx-background-radius:12;"
                    + "-fx-border-color:#c41e3a;-fx-border-width:2;-fx-border-radius:12;"
                    + "-fx-text-fill:#c41e3a;-fx-font-weight:bold;-fx-font-size:13px;-fx-padding:10 20;-fx-cursor:hand;"
                    + "-fx-focus-color:transparent;-fx-faint-focus-color:transparent;";
    private static final String ACTIVE_D17   =
            "-fx-background-color:#fff7ed;-fx-background-radius:12;"
                    + "-fx-border-color:#ea580c;-fx-border-width:2;-fx-border-radius:12;"
                    + "-fx-text-fill:#ea580c;-fx-font-weight:bold;-fx-font-size:13px;-fx-padding:10 20;-fx-cursor:hand;"
                    + "-fx-focus-color:transparent;-fx-faint-focus-color:transparent;";
    private static final String ACTIVE_CASH  =
            "-fx-background-color:#f0fdf4;-fx-background-radius:12;"
                    + "-fx-border-color:#059669;-fx-border-width:2;-fx-border-radius:12;"
                    + "-fx-text-fill:#059669;-fx-font-weight:bold;-fx-font-size:13px;-fx-padding:10 20;-fx-cursor:hand;"
                    + "-fx-focus-color:transparent;-fx-faint-focus-color:transparent;";
    private static final String ACTIVE_STYLE = ACTIVE_VISA; // default fallback

    @FXML
    public void initialize() {
        currentUserId = UserSession.getInstance().getUserId();
        isDarkMode = UserSession.getInstance().isDarkMode();
        applyDarkMode();
        btnCarte.setMinWidth(110); btnCIB.setMinWidth(110);
        btnD17.setMinWidth(110);   btnEspeces.setMinWidth(110);
        fieldNom.textProperty().addListener((obs, o, n) -> { labelNomCarte.setText(n.trim().isEmpty() ? "VOTRE NOM" : n.toUpperCase()); if (!n.trim().isEmpty()) clearError(fieldNom, erreurNom); });
        fieldNom.focusedProperty().addListener((obs, o, focused) -> { if (!focused && fieldNom.getText().trim().isEmpty()) setError(fieldNom, erreurNom, "\u274C Ce champ est obligatoire"); });
        fieldNumero.textProperty().addListener((obs, o, n) -> {
            String d = n.replaceAll("[^0-9]", "");
            labelNumeroCarte.setText(d.length() >= 4 ? "\u2022\u2022\u2022\u2022 \u2022\u2022\u2022\u2022 \u2022\u2022\u2022\u2022 " + d.substring(Math.max(0, d.length() - 4)) : "\u2022\u2022\u2022\u2022 \u2022\u2022\u2022\u2022 \u2022\u2022\u2022\u2022 \u2022\u2022\u2022\u2022");
            if (d.isEmpty()) clearError(fieldNumero, erreurNumero);
            else if (d.length() < 16) setError(fieldNumero, erreurNumero, "\u26A0 " + d.length() + " / 16 chiffres saisis");
            else if (d.length() == 16) clearError(fieldNumero, erreurNumero);
            else setError(fieldNumero, erreurNumero, "\u274C Maximum 16 chiffres (saisi : " + d.length() + ")");
        });
        fieldNumero.focusedProperty().addListener((obs, o, focused) -> { if (!focused && fieldNumero.getText().replaceAll("[^0-9]", "").isEmpty()) setError(fieldNumero, erreurNumero, "\u274C Ce champ est obligatoire"); });
        fieldExpiry.textProperty().addListener((obs, o, n) -> { labelExpiry.setText(n.trim().isEmpty() ? "MM/YY" : n); if (n.isEmpty()) clearError(fieldExpiry, erreurExpiry); else if (n.matches("(0[1-9]|1[0-2])/[0-9]{2}")) clearError(fieldExpiry, erreurExpiry); else setError(fieldExpiry, erreurExpiry, "\u26A0 Format attendu : MM/YY (ex: 07/26)"); });
        fieldExpiry.focusedProperty().addListener((obs, o, focused) -> { if (!focused && fieldExpiry.getText().trim().isEmpty()) setError(fieldExpiry, erreurExpiry, "\u274C Ce champ est obligatoire"); });
        fieldCVV.textProperty().addListener((obs, o, n) -> { String d = n.replaceAll("[^0-9]", ""); if (d.isEmpty()) clearError(fieldCVV, erreurCVV); else if (d.length() < 3) setError(fieldCVV, erreurCVV, "\u26A0 " + d.length() + " / 3 chiffres"); else if (d.length() == 3) clearError(fieldCVV, erreurCVV); else setError(fieldCVV, erreurCVV, "\u274C Le CVV doit contenir exactement 3 chiffres"); });
        fieldCVV.focusedProperty().addListener((obs, o, focused) -> { if (!focused && fieldCVV.getText().trim().isEmpty()) setError(fieldCVV, erreurCVV, "\u274C Ce champ est obligatoire"); });
        fieldD17Numero.textProperty().addListener((obs, o, n) -> { String d = n.replaceAll("[^0-9]", ""); if (d.isEmpty()) clearError(fieldD17Numero, erreurD17Numero); else if (d.length() < 16) setError(fieldD17Numero, erreurD17Numero, "\u26A0 " + d.length() + " / 16 chiffres saisis"); else if (d.length() == 16) clearError(fieldD17Numero, erreurD17Numero); else setError(fieldD17Numero, erreurD17Numero, "\u274C Maximum 16 chiffres (saisi : " + d.length() + ")"); });
        fieldD17Numero.focusedProperty().addListener((obs, o, focused) -> { if (!focused && fieldD17Numero.getText().trim().isEmpty()) setError(fieldD17Numero, erreurD17Numero, "\u274C Ce champ est obligatoire"); });
        fieldD17Pin.textProperty().addListener((obs, o, n) -> { String d = n.replaceAll("[^0-9]", ""); if (d.isEmpty()) clearError(fieldD17Pin, erreurD17Pin); else if (d.length() < 4) setError(fieldD17Pin, erreurD17Pin, "\u26A0 " + d.length() + " / 4 chiffres"); else if (d.length() == 4) clearError(fieldD17Pin, erreurD17Pin); else setError(fieldD17Pin, erreurD17Pin, "\u274C Le PIN doit contenir exactement 4 chiffres"); });
        fieldD17Pin.focusedProperty().addListener((obs, o, focused) -> { if (!focused && fieldD17Pin.getText().trim().isEmpty()) setError(fieldD17Pin, erreurD17Pin, "\u274C Ce champ est obligatoire"); });
        selectCarte();
        if (menuBtn != null) { menuBtn.setOnAction(e -> { if (drawerOpen) closeDrawer(); else openDrawer(); }); }
        Platform.runLater(this::setupDrawer);
    }

    private void applyDarkMode() {
        if (isDarkMode) { if (!rootPane.getStyleClass().contains("dark")) rootPane.getStyleClass().add("dark"); themeBtn.setText("\uD83C\uDF19 Mode Nuit"); }
        else { rootPane.getStyleClass().remove("dark"); themeBtn.setText("\u2600 Mode Jour"); }
        if (drawer != null) { if (isDarkMode) { if (!drawer.getStyleClass().contains("dark")) drawer.getStyleClass().add("dark"); } else drawer.getStyleClass().remove("dark"); }
    }

    private void setError(TextField f, Label l, String msg) { l.setText(msg); f.setStyle(ERROR_BORDER); }
    private void clearError(TextField f, Label l)           { l.setText("");   f.setStyle(NORMAL_BORDER); }

// In PaiementController.java

    public void setMontantTotal(double montant) {
        this.montantTotal = montant;
        this.montantApresReduction = montant; // ← ADD THIS LINE
        labelMontant.setText(String.format("%.2f TND", montant));
    }    public void setUserEmail(String email)      { this.userEmail = email; }
    public void setCodePromo(CodePromoDAO.ResultatCode code) { this.codePromoApplique = code; }
    @FXML public void handleThemeToggle() { isDarkMode = !isDarkMode; UserSession.getInstance().setDarkMode(isDarkMode); applyDarkMode(); }

    private void resetButtons() { btnCarte.setStyle(INACTIVE_STYLE); btnCIB.setStyle(INACTIVE_STYLE); btnD17.setStyle(INACTIVE_STYLE); btnEspeces.setStyle(INACTIVE_STYLE); }
    private void hideAllForms() {
        carteVisuelle.setVisible(false);    carteVisuelle.setManaged(false);
        formulaireCarte.setVisible(false);  formulaireCarte.setManaged(false);
        formD17.setVisible(false);          formD17.setManaged(false);
        messageLivraison.setVisible(false); messageLivraison.setManaged(false);
    }

    @FXML public void selectCarte()   { selectedMethod = "Carte";       resetButtons(); btnCarte.setStyle(ACTIVE_VISA);   hideAllForms(); carteVisuelle.setVisible(true); carteVisuelle.setManaged(true); formulaireCarte.setVisible(true); formulaireCarte.setManaged(true); labelCarteType.setText("VISA"); }
    @FXML public void selectCIB()     { selectedMethod = "CIB";         resetButtons(); btnCIB.setStyle(ACTIVE_CIB);      hideAllForms(); carteVisuelle.setVisible(true); carteVisuelle.setManaged(true); formulaireCarte.setVisible(true); formulaireCarte.setManaged(true); labelCarteType.setText("CARTE"); }
    @FXML public void selectD17()     { selectedMethod = "D17";         resetButtons(); btnD17.setStyle(ACTIVE_D17);      hideAllForms(); formD17.setVisible(true); formD17.setManaged(true); }
    @FXML public void selectEspeces() { selectedMethod = "Espèces"; resetButtons(); btnEspeces.setStyle(ACTIVE_CASH); hideAllForms(); messageLivraison.setVisible(true); messageLivraison.setManaged(true); }

    private boolean validateForm() {
        boolean valid = true;
        if (selectedMethod.equals("Carte") || selectedMethod.equals("CIB")) {
            if (fieldNom.getText().trim().isEmpty()) { setError(fieldNom, erreurNom, "\u274C Ce champ est obligatoire"); valid = false; }
            String d = fieldNumero.getText().replaceAll("[^0-9]", "");
            if (d.isEmpty()) { setError(fieldNumero, erreurNumero, "\u274C Ce champ est obligatoire"); valid = false; }
            else if (d.length() != 16) { setError(fieldNumero, erreurNumero, "\u274C Exactement 16 chiffres requis (saisi : " + d.length() + ")"); valid = false; }
            if (fieldExpiry.getText().trim().isEmpty()) { setError(fieldExpiry, erreurExpiry, "\u274C Ce champ est obligatoire"); valid = false; }
            else if (!fieldExpiry.getText().matches("(0[1-9]|1[0-2])/[0-9]{2}")) { setError(fieldExpiry, erreurExpiry, "\u274C Format attendu : MM/YY (ex: 07/26)"); valid = false; }
            else {
                String[] parts = fieldExpiry.getText().split("/");
                int expMonth = Integer.parseInt(parts[0]); int expYear = 2000 + Integer.parseInt(parts[1]);
                java.time.YearMonth expiry = java.time.YearMonth.of(expYear, expMonth); java.time.YearMonth today = java.time.YearMonth.now();
                if (expiry.isBefore(today)) { setError(fieldExpiry, erreurExpiry, "\u274C Carte expir\u00E9e — v\u00E9rifiez la date"); valid = false; }
            }
            if (fieldCVV.getText().trim().isEmpty()) { setError(fieldCVV, erreurCVV, "\u274C Ce champ est obligatoire"); valid = false; }
            else if (!fieldCVV.getText().matches("[0-9]{3}")) { setError(fieldCVV, erreurCVV, "\u274C Le CVV doit contenir exactement 3 chiffres"); valid = false; }
        } else if (selectedMethod.equals("D17")) {
            String cd = fieldD17Numero.getText().replaceAll("[^0-9]", "");
            if (cd.isEmpty()) { setError(fieldD17Numero, erreurD17Numero, "\u274C Ce champ est obligatoire"); valid = false; }
            else if (cd.length() != 16) { setError(fieldD17Numero, erreurD17Numero, "\u274C Exactement 16 chiffres requis (saisi : " + cd.length() + ")"); valid = false; }
            String pin = fieldD17Pin.getText().replaceAll("[^0-9]", "");
            if (pin.isEmpty()) { setError(fieldD17Pin, erreurD17Pin, "\u274C Ce champ est obligatoire"); valid = false; }
            else if (pin.length() != 4) { setError(fieldD17Pin, erreurD17Pin, "\u274C Le PIN doit contenir exactement 4 chiffres"); valid = false; }
            erreurD17.setText("");
        }
        return valid;
    }

    @FXML
    public void handleConfirmer() {
        if (!validateForm()) return;
        if (selectedMethod.equals("Esp\u00E8ces")) { saveAndConfirm("Client", "REF-" + System.currentTimeMillis(), "en attente"); return; }
        String reference = "REF-" + System.currentTimeMillis();
        String nomAffiche;
        if (selectedMethod.equals("Carte") || selectedMethod.equals("CIB")) {
            nomAffiche = fieldNom.getText().trim().isEmpty() ? "Client" : fieldNom.getText().trim();
        } else {
            String d17Num = fieldD17Numero.getText().replaceAll("[^0-9]", "");
            nomAffiche = d17Num.length() >= 4 ? "Client D17-****" + d17Num.substring(d17Num.length() - 4) : "Client D17";
        }
        final String nomFinal = nomAffiche;
        if (btnConfirmer != null) btnConfirmer.setDisable(true);
        new Thread(() -> {
            StripeService.PaymentResult result = StripeService.generatePayment(montantTotal, reference, userEmail);
            if (!result.success) {
                Platform.runLater(() -> { if (btnConfirmer != null) btnConfirmer.setDisable(false); showAlert("Erreur Stripe", "Impossible d'initier le paiement :\n" + result.errorMessage); });
                return;
            }
            if (result.payUrl != null) {
                Platform.runLater(() -> { try { Desktop.getDesktop().browse(new URI(result.payUrl)); } catch (Exception e) { showAlert("Erreur navigateur", "Ouvrez ce lien manuellement :\n" + result.payUrl); } });
            }
            Platform.runLater(() -> { if (btnConfirmer != null) btnConfirmer.setDisable(false); showStripeWaitingDialog(nomFinal, reference, result.paymentToken); });
        }).start();
    }

    private void showStripeWaitingDialog(String nomClient, String reference, String paymentToken) {
        Label icon = new Label("\u23F3"); icon.setStyle("-fx-font-size: 48px;");
        Label titre = new Label("Paiement Stripe ouvert dans votre navigateur");
        titre.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #0C145F;");
        titre.setWrapText(true); titre.setMaxWidth(340); titre.setAlignment(Pos.CENTER);
        Label sousTitre = new Label("Compl\u00E9tez le paiement dans votre navigateur,\npuis revenez ici et cliquez \u00AB J'ai pay\u00E9 \u00BB.");
        sousTitre.setStyle("-fx-font-size: 13px; -fx-text-fill: #555;"); sousTitre.setWrapText(true); sousTitre.setMaxWidth(340); sousTitre.setAlignment(Pos.CENTER);
        Label statusLabel = new Label(""); statusLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #888;");
        Button btnVerif = new Button("\u2705  J'ai pay\u00E9 \u2014 v\u00E9rifier");
        btnVerif.setStyle("-fx-background-color: #192BCC; -fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold; -fx-padding: 12 24; -fx-background-radius: 10; -fx-cursor: hand; -fx-min-width: 260px;");
        Button btnAnnuler = new Button("Annuler");
        btnAnnuler.setStyle("-fx-background-color: transparent; -fx-text-fill: #EF4444; -fx-font-size: 12px; -fx-border-color: #EF4444; -fx-border-radius: 8; -fx-padding: 8 20; -fx-cursor: hand;");
        VBox layout = new VBox(16); layout.setAlignment(Pos.CENTER); layout.setPadding(new Insets(30, 40, 30, 40)); layout.setMinWidth(380);
        layout.setBackground(new Background(new BackgroundFill(Color.WHITE, CornerRadii.EMPTY, Insets.EMPTY)));
        layout.getChildren().addAll(icon, titre, sousTitre, statusLabel, btnVerif, btnAnnuler);
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Paiement en cours..."); dialog.initModality(Modality.APPLICATION_MODAL); dialog.initOwner(btnCarte.getScene().getWindow());
        dialog.setGraphic(null); dialog.setHeaderText(null);
        DialogPane pane = dialog.getDialogPane(); pane.setContent(layout); pane.setStyle("-fx-background-color: white; -fx-padding: 0;");
        pane.getButtonTypes().add(ButtonType.CLOSE); pane.lookupButton(ButtonType.CLOSE).setVisible(false); pane.lookupButton(ButtonType.CLOSE).setManaged(false);
        btnAnnuler.setOnAction(e -> { dialog.setResult(ButtonType.CANCEL); dialog.close(); });
        int[] failCount = {0};
        btnVerif.setOnAction(e -> {
            btnVerif.setDisable(true); statusLabel.setText("\u23F3 V\u00E9rification en cours...");
            new Thread(() -> {
                boolean paid = StripeService.verifyPayment(paymentToken);
                Platform.runLater(() -> {
                    if (paid) { dialog.setResult(ButtonType.OK); dialog.close(); saveAndConfirm(nomClient, reference, "valid\u00E9"); }
                    else {
                        failCount[0]++; statusLabel.setText("\u274C Paiement non d\u00E9tect\u00E9. V\u00E9rifiez votre navigateur.");
                        int cooldownSec = Math.min(5 * failCount[0], 30);
                        new Thread(() -> {
                            for (int i = cooldownSec; i > 0; i--) { final int r = i; Platform.runLater(() -> btnVerif.setText("\uD83D\uDD04  R\u00E9essayer (" + r + "s)")); try { Thread.sleep(1000); } catch (InterruptedException ignored) {} }
                            Platform.runLater(() -> { btnVerif.setText("\uD83D\uDD04  R\u00E9essayer"); btnVerif.setDisable(false); });
                        }).start();
                    }
                });
            }).start();
        });
        dialog.showAndWait();
    }

    private void saveAndConfirm(String nomClient, String reference, String statut) {
        try {
            PaiementDAO dao = new PaiementDAO();
            Paiement paiement = new Paiement(1, montantApresReduction, selectedMethod, statut);
            paiement.setReferenceTransaction(reference);
            dao.addPaiementForUser(paiement, currentUserId);
            // ✅ Sauvegarder les articles dans lignes_commande
            com.hanouti.hanoutiem4.dao.PanierDAO panierDAO2 = new com.hanouti.hanoutiem4.dao.PanierDAO();
            java.util.List<com.hanouti.hanoutiem4.model.Panier> items = panierDAO2.getCartItems(currentUserId);
            java.sql.Connection conn = com.hanouti.hanoutiem4.util.DBConnection.getInstance().getConnection();
            String sql = "INSERT INTO lignes_commande (reference_transaction, produit_id, nom_produit, quantite, prix_unitaire) VALUES (?, ?, ?, ?, ?)";
            for (com.hanouti.hanoutiem4.model.Panier item : items) {
                try (java.sql.PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setString(1, reference);
                    ps.setInt(2, item.getProduitId());
                    ps.setString(3, item.getNomProduit() != null ? item.getNomProduit() : "Produit #" + item.getProduitId());
                    ps.setInt(4, item.getQuantite());
                    ps.setDouble(5, item.getPrixUnitaire());
                    ps.executeUpdate();
                }
            }
            // ✅ ADD THIS BLOCK — increment promo usage after successful payment
            if (codePromoApplique != null && codePromoApplique.valide) {
                CodePromoDAO codeDAO = new CodePromoDAO();
                codeDAO.incrementerUtilisation(codePromoApplique.codeId);
            }

            com.hanouti.hanoutiem4.dao.PanierDAO panierDAO = new com.hanouti.hanoutiem4.dao.PanierDAO();
            panierDAO.clearCart(currentUserId);
            EmailService.sendPaymentConfirmation(
                    userEmail, nomClient, reference, selectedMethod, montantApresReduction
            );
            showAIConfirmationDialog(nomClient, reference);
        } catch (SQLException e) { showAlert("Erreur base de donn\u00E9es", "Impossible d'enregistrer le paiement : " + e.getMessage()); }
    }

    private void showAIConfirmationDialog(String nomClient, String reference) {
        Label checkIcon = new Label("\u2713"); checkIcon.setFont(Font.font("System", FontWeight.BOLD, 36)); checkIcon.setTextFill(Color.WHITE); checkIcon.setAlignment(Pos.CENTER); checkIcon.setMinSize(80, 80); checkIcon.setMaxSize(80, 80);
        checkIcon.setBackground(new Background(new BackgroundFill(Color.web("#22C55E"), new CornerRadii(40), Insets.EMPTY)));
        Label titre = new Label(selectedMethod.equals("Esp\u00E8ces") ? "Commande confirm\u00E9e !" : "Paiement confirm\u00E9 !"); titre.setFont(Font.font("System", FontWeight.BOLD, 20)); titre.setTextFill(Color.web("#0C145F"));
        Label sousTitre = new Label(selectedMethod.equals("Esp\u00E8ces") ? "Vous paierez \u00E0 la livraison \uD83D\uDE9A" : "Votre paiement a \u00E9t\u00E9 enregistr\u00E9 avec succ\u00E8s"); sousTitre.setFont(Font.font(13)); sousTitre.setTextFill(Color.web("#888888"));
        String fallback = String.format("\u2705 Merci %s ! Votre paiement de %.2f TND via %s a bien \u00E9t\u00E9 enregistr\u00E9.\nR\u00E9f\u00E9rence : %s\nNous vous remercions de votre confiance chez 7anouti-E \uD83D\uDED2", nomClient, montantTotal, selectedMethod, reference);
        Label aiLabel = new Label(fallback); aiLabel.setWrapText(true); aiLabel.setMaxWidth(360); aiLabel.setFont(Font.font(12)); aiLabel.setTextFill(Color.web("#1E1B4B")); aiLabel.setPadding(new Insets(12));
        aiLabel.setBackground(new Background(new BackgroundFill(Color.web("#EEF2FF"), new CornerRadii(10), Insets.EMPTY)));
        aiLabel.setBorder(new Border(new BorderStroke(Color.web("#6366F1", 0.3), BorderStrokeStyle.SOLID, new CornerRadii(10), new BorderWidths(1))));
        Separator sep1 = new Separator();
        HBox rowMethode = buildDetailRow("M\u00E9thode", selectedMethod, false);
        HBox rowMontant = buildDetailRow("Montant", String.format("%.2f TND", montantTotal), true);
        HBox rowRef     = buildDetailRow("R\u00E9f\u00E9rence", reference, false);
        Separator sep2 = new Separator();
        Button btnHistorique = new Button("\uD83D\uDCCB  Voir mon historique");
        btnHistorique.setStyle("-fx-background-color: #192BCC; -fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold; -fx-padding: 12 24; -fx-background-radius: 10; -fx-cursor: hand; -fx-min-width: 300px;");
        Button btnRetour = new Button("Retour au panier");
        btnRetour.setStyle("-fx-background-color: transparent; -fx-text-fill: #192BCC; -fx-font-size: 13px; -fx-border-color: #192BCC; -fx-border-radius: 10; -fx-padding: 10 24; -fx-cursor: hand; -fx-min-width: 300px;");
        VBox layout = new VBox(14); layout.setAlignment(Pos.CENTER); layout.setPadding(new Insets(30, 36, 30, 36)); layout.setMinWidth(400);
        layout.setBackground(new Background(new BackgroundFill(Color.WHITE, CornerRadii.EMPTY, Insets.EMPTY)));
        layout.getChildren().addAll(checkIcon, titre, sousTitre, sep1, rowMethode, rowMontant, rowRef, sep2, aiLabel, btnHistorique, btnRetour);
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Paiement confirm\u00E9"); dialog.initModality(Modality.APPLICATION_MODAL); dialog.initOwner(btnCarte.getScene().getWindow());
        dialog.setGraphic(null); dialog.setHeaderText(null);
        DialogPane pane = dialog.getDialogPane(); pane.setContent(layout); pane.setStyle("-fx-background-color: white; -fx-padding: 0;");
        pane.getButtonTypes().add(ButtonType.CLOSE); pane.lookupButton(ButtonType.CLOSE).setVisible(false); pane.lookupButton(ButtonType.CLOSE).setManaged(false);
        btnHistorique.setOnAction(e -> { dialog.setResult(ButtonType.OK); dialog.close(); navigateTo("HistoriquePaiement.fxml", "7anouti-E \u2014 Historique"); });
        btnRetour.setOnAction(e -> { dialog.setResult(ButtonType.CANCEL); dialog.close(); navigateTo("Panier.fxml", "7anouti-E \u2014 Panier"); });
        String method = selectedMethod; double montant = montantTotal;
        new Thread(() -> { String aiMsg = callClaudeAPI(nomClient, reference, method, montant); if (aiMsg != null && !aiMsg.isBlank()) Platform.runLater(() -> aiLabel.setText(aiMsg)); }).start();
        dialog.showAndWait();
    }

    private String callClaudeAPI(String nom, String ref, String methode, double montant) {
        if (CLAUDE_API_KEY.startsWith("REMPLACE") || CLAUDE_API_KEY.isBlank()) { System.out.println("[Claude API] Cl\u00E9 non configur\u00E9e."); return null; }
        try {
            URL url = new URL("https://api.anthropic.com/v1/messages");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST"); conn.setRequestProperty("Content-Type", "application/json"); conn.setRequestProperty("x-api-key", CLAUDE_API_KEY); conn.setRequestProperty("anthropic-version", "2023-06-01");
            conn.setConnectTimeout(10_000); conn.setReadTimeout(15_000); conn.setDoOutput(true);
            String prompt = String.format("Tu es un assistant de l'application 7anouti-E. \u00C9cris un message de confirmation de paiement chaleureux et professionnel en fran\u00E7ais pour le client '%s'. D\u00E9tails : r\u00E9f\u00E9rence %s, m\u00E9thode %s, montant %.2f TND. Maximum 3 phrases, ton amical et rassurant. Pas de salutation formelle, va directement au message.", nom, ref, methode, montant);
            String escaped = prompt.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "");
            String body = "{\"model\":\"claude-sonnet-4-20250514\",\"max_tokens\":200,\"messages\":[{\"role\":\"user\",\"content\":\"" + escaped + "\"}]}";
            try (OutputStream os = conn.getOutputStream()) { os.write(body.getBytes(StandardCharsets.UTF_8)); }
            if (conn.getResponseCode() == 200) {
                String response = new String(conn.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
                String marker = "\"text\":\""; int start = response.indexOf(marker);
                if (start >= 0) { start += marker.length(); StringBuilder sb = new StringBuilder(); int i = start;
                    while (i < response.length()) { char c = response.charAt(i);
                        if (c == '\\' && i + 1 < response.length()) { char nx = response.charAt(i + 1); if (nx == '"') { sb.append('"'); i += 2; continue; } if (nx == 'n') { sb.append('\n'); i += 2; continue; } if (nx == 't') { sb.append('\t'); i += 2; continue; } if (nx == '\\') { sb.append('\\'); i += 2; continue; } }
                        if (c == '"') break; sb.append(c); i++; }
                    String extracted = sb.toString().trim(); if (!extracted.isEmpty()) return extracted; }
            }
        } catch (Exception e) { System.err.println("[Claude API] Exception : " + e.getMessage()); }
        return null;
    }

    private HBox buildDetailRow(String label, String value, boolean valueBlue) {
        HBox row = new HBox(); row.setAlignment(Pos.CENTER_LEFT);
        Label k = new Label(label); k.setStyle("-fx-font-size: 13px; -fx-text-fill: #666;");
        Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS);
        Label v = new Label(value); v.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: " + (valueBlue ? "#192BCC" : "#0C145F") + ";");
        row.getChildren().addAll(k, spacer, v); return row;
    }

    @FXML public void handleRetour() { navigateTo("Panier.fxml", "7anouti-E \u2014 Panier"); }

    private void navigateTo(String fxmlFile, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/hanouti/hanoutiem4/" + fxmlFile));
            Scene scene = new Scene(loader.load(), 1000, 850);
            Stage stage = (Stage) btnCarte.getScene().getWindow();
            stage.setTitle(title); stage.setScene(scene); stage.setMaximized(false);
        } catch (IOException e) { showAlert("Navigation impossible", "Erreur: " + e.getMessage()); }
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING); alert.initOwner(btnCarte.getScene().getWindow());
        alert.setTitle(title); alert.setContentText(message); alert.showAndWait();
    }

    private void setupDrawer() {
        drawerOverlay = new AnchorPane(); drawerOverlay.setStyle("-fx-background-color: rgba(0,0,0,0.38);");
        drawerOverlay.setVisible(false); drawerOverlay.setManaged(false); drawerOverlay.setOnMouseClicked(e -> closeDrawer());
        AnchorPane.setTopAnchor(drawerOverlay, 0.0); AnchorPane.setBottomAnchor(drawerOverlay, 0.0); AnchorPane.setLeftAnchor(drawerOverlay, 0.0); AnchorPane.setRightAnchor(drawerOverlay, 0.0);
        drawer = new VBox(0); drawer.getStyleClass().add("side-drawer"); drawer.setPrefWidth(290); drawer.setMaxWidth(290); drawer.setOnMouseClicked(javafx.event.Event::consume);
        if (isDarkMode) drawer.getStyleClass().add("dark");
        drawer.getChildren().addAll(buildDrawerHeader(), new Separator(), buildNavSection("paiement"), new Separator());
        Region spacer = new Region(); VBox.setVgrow(spacer, Priority.ALWAYS); drawer.getChildren().add(spacer);
        drawer.getChildren().addAll(new Separator(), buildDrawerFooter());
        AnchorPane.setTopAnchor(drawer, 0.0); AnchorPane.setRightAnchor(drawer, 0.0); AnchorPane.setBottomAnchor(drawer, 0.0);
        drawer.setTranslateX(310); drawerOverlay.getChildren().add(drawer); rootPane.getChildren().add(drawerOverlay);
    }

    private VBox buildDrawerHeader() {
        VBox header = new VBox(10); header.setStyle("-fx-padding: 28 22 20 22;"); header.setAlignment(Pos.CENTER_LEFT);
        HBox topRow = new HBox(); topRow.setAlignment(Pos.CENTER_RIGHT);
        Button closeBtn = new Button("\u2715"); closeBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: rgba(120,120,140,0.80); -fx-font-size: 16px; -fx-cursor: hand; -fx-padding: 0;"); closeBtn.setOnAction(e -> closeDrawer()); topRow.getChildren().add(closeBtn);
        StackPane avatar = new StackPane(); Circle bg = new Circle(26); bg.setFill(Color.web("#6366F1", 0.85));
        Label initials = new Label(getInitials(UserSession.getInstance().getUserName())); initials.setStyle("-fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: bold;"); avatar.getChildren().addAll(bg, initials);
        Label nameLabel  = new Label(UserSession.getInstance().getUserName());  nameLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: -t1;");
        Label emailLabel = new Label(UserSession.getInstance().getUserEmail()); emailLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: -t3;"); emailLabel.setWrapText(true);
        Label roleLabel  = new Label("Client Hanouti-E  \u2713"); roleLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #22C55E; -fx-font-weight: bold; -fx-background-color: rgba(34,197,94,0.12); -fx-background-radius: 20; -fx-padding: 2 8;");
        header.getChildren().addAll(topRow, avatar, nameLabel, emailLabel, roleLabel); return header;
    }

    private VBox buildNavSection(String activePage) {
        VBox nav = new VBox(4); nav.setStyle("-fx-padding: 12 10;");
        Label navTitle = new Label("NAVIGATION"); navTitle.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: -t3; -fx-padding: 4 12 8 12;"); nav.getChildren().add(navTitle);
        nav.getChildren().add(buildNavItem("\uD83D\uDED2", "Mon Panier",               "panier".equals(activePage),     () -> { closeDrawer(); navigateTo("Panier.fxml",             "7anouti-E \u2014 Panier"); }));
        nav.getChildren().add(buildNavItem("\uD83D\uDCCB", "Historique des paiements", "historique".equals(activePage), () -> { closeDrawer(); navigateTo("HistoriquePaiement.fxml", "7anouti-E \u2014 Historique"); }));
        return nav;
    }

    private HBox buildNavItem(String icon, String label, boolean active, Runnable action) {
        HBox row = new HBox(12); row.setAlignment(Pos.CENTER_LEFT);
        String baseStyle = "-fx-padding: 10 12; -fx-background-radius: 10; -fx-cursor: hand; ";
        row.setStyle(baseStyle + (active ? "-fx-background-color: rgba(99,102,241,0.15);" : "-fx-background-color: transparent;"));
        Label iconLbl = new Label(icon); iconLbl.setStyle("-fx-font-size: 16px; -fx-min-width: 22;");
        Label textLbl = new Label(label); textLbl.setStyle("-fx-font-size: 13px; -fx-text-fill: " + (active ? "#6366F1" : "-t1") + "; -fx-font-weight: " + (active ? "bold" : "normal") + ";");
        row.getChildren().addAll(iconLbl, textLbl); row.setOnMouseClicked(e -> action.run());
        row.setOnMouseEntered(e -> { if (!active) row.setStyle(baseStyle + "-fx-background-color: rgba(99,102,241,0.08);"); });
        row.setOnMouseExited(e ->  { if (!active) row.setStyle(baseStyle + "-fx-background-color: transparent;"); });
        return row;
    }

    private VBox buildDrawerFooter() {
        VBox footer = new VBox(10); footer.setStyle("-fx-padding: 16 16 28 16;"); footer.setAlignment(Pos.CENTER_LEFT);
        Label versionLbl = new Label("7anouti-E  v1.0"); versionLbl.setStyle("-fx-font-size: 10px; -fx-text-fill: -t3;");
        HBox userRow = new HBox(8); userRow.setAlignment(Pos.CENTER_LEFT);
        Label userIcon = new Label("\uD83D\uDC64"); userIcon.setStyle("-fx-font-size: 13px;");
        Label userLbl  = new Label(UserSession.getInstance().getUserName()); userLbl.setStyle("-fx-font-size: 12px; -fx-text-fill: -t2; -fx-font-weight: bold;");
        userRow.getChildren().addAll(userIcon, userLbl);
        Button btnDeconnect = new Button("\u23CF   Se d\u00E9connecter"); btnDeconnect.setMaxWidth(Double.MAX_VALUE);
        String dBase  = "-fx-background-color: rgba(239,68,68,0.10); -fx-border-color: rgba(239,68,68,0.40); -fx-border-width: 1.5; -fx-border-radius: 10; -fx-background-radius: 10; -fx-text-fill: #EF4444; -fx-font-size: 13px; -fx-font-weight: bold; -fx-padding: 10 16; -fx-cursor: hand;";
        String dHover = "-fx-background-color: rgba(239,68,68,0.18); -fx-border-color: #EF4444; -fx-border-width: 1.5; -fx-border-radius: 10; -fx-background-radius: 10; -fx-text-fill: #EF4444; -fx-font-size: 13px; -fx-font-weight: bold; -fx-padding: 10 16; -fx-cursor: hand;";
        btnDeconnect.setStyle(dBase); btnDeconnect.setOnMouseEntered(e -> btnDeconnect.setStyle(dHover)); btnDeconnect.setOnMouseExited(e -> btnDeconnect.setStyle(dBase));
        btnDeconnect.setOnAction(e -> {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION); confirm.setTitle("D\u00E9connexion"); confirm.setHeaderText("Voulez-vous vous d\u00E9connecter ?"); confirm.setContentText("Vous serez d\u00E9connect\u00E9 de votre compte \u00AB " + UserSession.getInstance().getUserName() + " \u00BB.");
            ButtonType btnOui = new ButtonType("Se d\u00E9connecter"); ButtonType btnNon = new ButtonType("Annuler", ButtonBar.ButtonData.CANCEL_CLOSE);
            confirm.getButtonTypes().setAll(btnOui, btnNon);
            confirm.showAndWait().ifPresent(response -> { if (response == btnOui) { UserSession.getInstance().logout(); ((Stage) rootPane.getScene().getWindow()).close(); } });
        });
        footer.getChildren().addAll(versionLbl, userRow, btnDeconnect); return footer;
    }

    private String getInitials(String name) {
        if (name == null || name.isBlank()) return "?";
        String[] parts = name.trim().split("\\s+");
        if (parts.length == 1) return parts[0].substring(0, Math.min(2, parts[0].length())).toUpperCase();
        return ("" + parts[0].charAt(0) + parts[1].charAt(0)).toUpperCase();
    }

    private void openDrawer() {
        drawerOverlay.setVisible(true); drawerOverlay.setManaged(true);
        FadeTransition fade = new FadeTransition(Duration.millis(200), drawerOverlay); fade.setFromValue(0); fade.setToValue(1);
        TranslateTransition slide = new TranslateTransition(Duration.millis(280), drawer); slide.setToX(0); slide.setInterpolator(Interpolator.EASE_OUT);
        new ParallelTransition(fade, slide).play(); drawerOpen = true;
    }

    private void closeDrawer() {
        TranslateTransition slide = new TranslateTransition(Duration.millis(230), drawer); slide.setToX(310); slide.setInterpolator(Interpolator.EASE_IN);
        FadeTransition fade = new FadeTransition(Duration.millis(220), drawerOverlay); fade.setFromValue(1); fade.setToValue(0);
        ParallelTransition pt = new ParallelTransition(fade, slide);
        pt.setOnFinished(e -> { drawerOverlay.setVisible(false); drawerOverlay.setManaged(false); }); pt.play(); drawerOpen = false;
    }
}