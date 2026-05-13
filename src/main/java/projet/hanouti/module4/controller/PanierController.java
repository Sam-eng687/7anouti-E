package com.hanouti.hanoutiem4.controller;

import com.hanouti.hanoutiem4.UserSession;
import com.hanouti.hanoutiem4.dao.CodePromoDAO;
import com.hanouti.hanoutiem4.dao.PanierDAO;
import com.hanouti.hanoutiem4.dao.ProduitDAO;
import com.hanouti.hanoutiem4.dao.PromotionDAO;
import com.hanouti.hanoutiem4.model.Panier;
import com.hanouti.hanoutiem4.model.Produit;
import com.hanouti.hanoutiem4.model.Promotion;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import com.hanouti.hanoutiem4.util.DrawerHelper;
import javafx.animation.*;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;
import javafx.stage.Stage;
import javafx.util.Duration;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import javafx.scene.control.ButtonBar;

public class PanierController {

    @FXML private AnchorPane rootPane;
    @FXML private HBox       headerBar;
    @FXML private Button     menuBtn;
    @FXML private StackPane  logoBoxPlaceholder;
    @FXML private Button     themeBtn;
    @FXML private Button     notifBellBtn;
    @FXML private Button     cartCountBadge;
    @FXML private Button     aiRefreshBtn;
    @FXML private Button     profileBadge;
    @FXML private Label      labelArticlesBadge;
    @FXML private Button     viderBtn;
    @FXML private VBox       cartItemsContainer;
    @FXML private VBox       notifPanel;
    @FXML private ScrollPane cartScrollPane;
    @FXML private VBox       aiPanel;
    @FXML private ScrollPane aiScrollPane;
    @FXML private HBox       aiResizeHandle;
    private double           aiHeight = 260;   // current pref height of aiScrollPane
    /** IDs of notifications the user dismissed — prevents them from reappearing this session */
    private final java.util.Set<String> dismissedNotifIds = new java.util.HashSet<>();
    @FXML private HBox       aiHeader;
    @FXML private StackPane  aiIconBox;
    @FXML private Label      aiChevron;
    @FXML private VBox       aiBody;
    @FXML private VBox       aiSuggestionsContainer;
    @FXML private StackPane  sumIconBox;
    @FXML private StackPane  cartSectionIconBox;
    @FXML private Label      labelSousTotal;
    @FXML private Label      labelLivraison;
    @FXML private Label      labelTotal;
    @FXML private HBox       rowReduction;
    @FXML private Label      labelReduction;
    @FXML private TextField  promoField;
    @FXML private Button     promoApplyBtn;
    @FXML private Label      labelPromoMessage;

    private boolean      isDarkMode   = false;
    private boolean      aiExpanded   = false;
    private double       livraisonFee = 7.0;
    private double       discountAmt  = 0.0;
    private double       promoMinimum = 0.0;  // montantMin du code actif
    private int          promoCurrent = -1;   // codeId du code actif (-1 = aucun)
    private PanierDAO    panierDAO;
    private CodePromoDAO codePromoDAO;
    private ProduitDAO   produitDAO;
    private PromotionDAO promotionDAO;
    private List<Panier> panierList   = new ArrayList<>();
    private int          currentUserId;
    private DrawerHelper drawerHelper;

    private static final String IC_CART  = "M6 19m-1 0a1 1 0 1 0 2 0a1 1 0 1 0-2 0M17 19m-1 0a1 1 0 1 0 2 0a1 1 0 1 0-2 0M17 17H6a1 1 0 0 1-1-1V5H3M6 5l1.5 9h9.5l1.5-9H6z";
    private static final String IC_MENU  = "M3 6h18M3 12h18M3 18h18";
    private static final String IC_BELL  = "M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9M13.73 21a2 2 0 0 1-3.46 0";
    private static final String IC_CARD  = "M1 4h22v16H1zM1 10h22";
    private static final String IC_TRASH = "M3 6h18M19 6l-1 14a2 2 0 0 1-2 2H8a2 2 0 0 1-2-2L5 6M10 11v6M14 11v6M9 6V4a1 1 0 0 1 1-1h4a1 1 0 0 1 1 1v2";
    private static final String IC_EYE   = "M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8M12 9a3 3 0 1 0 0 6 3 3 0 0 0 0-6z";
    private static final String IC_BULB  = "M9 18h6M10 22h4M12 2a7 7 0 0 1 5.39 11.47c-.76 1.03-1.39 1.92-1.39 3.03v.5H8v-.5c0-1.11-.63-2-1.39-3.03A7 7 0 0 1 12 2z";

    @FXML
    public void initialize() {
        currentUserId = UserSession.getInstance().getUserId();
        isDarkMode    = UserSession.getInstance().isDarkMode();
        // aiPanel starts collapsed — no forced prefHeight, it auto-sizes to its header

        try { panierDAO    = new PanierDAO();    } catch (Exception e) { showAlert("Erreur", "DB panier."); }
        try { codePromoDAO = new CodePromoDAO(); } catch (Exception ignored) {}
        try { produitDAO    = new ProduitDAO();    } catch (Exception ignored) {}
        try { promotionDAO  = new PromotionDAO();  } catch (Exception ignored) {}

        // FIX: Hide notification badge (no real alerts)
        // FIX: Hide topbar theme btn — only in drawer
        if (themeBtn != null)   { themeBtn.setVisible(false); themeBtn.setManaged(false); }

        // Hamburger — FIX: wire themeChangeCallback so dark mode applies to rootPane
        if (menuBtn != null) {
            drawerHelper = new DrawerHelper(rootPane, isDarkMode, "panier");
            drawerHelper.setThemeChangeCallback(() -> {
                isDarkMode = UserSession.getInstance().isDarkMode();
                applyDarkClass();
            });
            menuBtn.setOnAction(e -> { bounceNode(menuBtn); drawerHelper.toggle(); });
        }

        applyDarkClass();

        if (profileBadge != null) {
            profileBadge.setText(getInitials(UserSession.getInstance().getUserName()));
            profileBadge.setOnAction(e -> { bounceNode(profileBadge); showProfileMenu(); });
        }

        // Style vider button — remove emoji from FXML text, keep only SVG icon
        if (viderBtn != null) {
            // Same style as "Supprimer tout" in Historique (transparent + red outline)
            viderBtn.setGraphic(svgIcon(IC_TRASH, "#ef4444", 15));
            viderBtn.setGraphicTextGap(7);
            viderBtn.setContentDisplay(javafx.scene.control.ContentDisplay.LEFT);
            String vBase = "-fx-background-color:transparent;-fx-background-radius:8;" +
                    "-fx-border-color:rgba(239,68,68,0.45);-fx-border-width:1;-fx-border-radius:8;" +
                    "-fx-text-fill:#94a3b8;-fx-font-size:12px;-fx-font-weight:600;" +
                    "-fx-padding:8 16;-fx-cursor:hand;" +
                    "-fx-focus-color:transparent;-fx-faint-focus-color:transparent;";
            String vHov = "-fx-background-color:#ef4444;-fx-background-radius:8;" +
                    "-fx-border-color:#ef4444;-fx-border-width:1;-fx-border-radius:8;" +
                    "-fx-text-fill:white;-fx-font-size:12px;-fx-font-weight:600;" +
                    "-fx-padding:8 16;-fx-cursor:hand;" +
                    "-fx-effect:dropshadow(gaussian,rgba(239,68,68,0.30),10,0.2,0,2);" +
                    "-fx-focus-color:transparent;-fx-faint-focus-color:transparent;";
            viderBtn.setStyle(vBase);
            // Swap icon color: red on transparent ↔ white on red background
            SVGPath viderIconRed   = svgIcon(IC_TRASH, "#ef4444", 15);
            SVGPath viderIconWhite = svgIcon(IC_TRASH, "#ffffff", 15);
            viderBtn.setGraphic(viderIconRed);
            viderBtn.setOnMouseEntered(e -> { viderBtn.setStyle(vHov); viderBtn.setGraphic(viderIconWhite); });
            viderBtn.setOnMouseExited(e  -> { viderBtn.setStyle(vBase); viderBtn.setGraphic(viderIconRed); });
        }

        javafx.application.Platform.runLater(() -> {
            buildTopbarIcons();
            buildCartSectionIcon();
            buildSummaryIcon();
            buildAiIcon();
            loadData();
            playEntrance(headerBar);
        });
    }

    // FIX: Apply dark class to rootPane
    private void applyDarkClass() {
        if (rootPane == null) return;
        if (isDarkMode) { if (!rootPane.getStyleClass().contains("dark")) rootPane.getStyleClass().add("dark"); }
        else             rootPane.getStyleClass().remove("dark");
    }

    private void buildTopbarIcons() {
        if (logoBoxPlaceholder != null) {
            logoBoxPlaceholder.setStyle("-fx-background-color:linear-gradient(to bottom right,#4f46e5,#6366f1);-fx-background-radius:13;");
            DropShadow ds = new DropShadow(); ds.setColor(Color.web("#4f46e5", 0.38)); ds.setRadius(10); ds.setOffsetY(3);
            logoBoxPlaceholder.setEffect(ds); logoBoxPlaceholder.getChildren().add(svgIcon(IC_CART, "#ffffff", 20));
        }
        if (menuBtn != null) { menuBtn.setGraphic(svgIcon(IC_MENU, "#4f46e5", 18)); menuBtn.setText(null); }
        // aiRefreshBtn — sync/refresh icon
        if (aiRefreshBtn != null) {
            aiRefreshBtn.setGraphic(svgIcon("M23 4v6h-6M1 20v-6h6M3.51 9a9 9 0 0 1 14.85-3.36L23 10M1 14l4.64 4.36A9 9 0 0 0 20.49 15", "#4f46e5", 14));
            aiRefreshBtn.setText(null);
            aiRefreshBtn.setStyle(
                    "-fx-background-color:rgba(99,102,241,0.10);-fx-background-radius:8;" +
                            "-fx-border-color:rgba(99,102,241,0.22);-fx-border-width:1;-fx-border-radius:8;" +
                            "-fx-min-width:28;-fx-min-height:28;-fx-max-width:28;-fx-max-height:28;" +
                            "-fx-padding:0;-fx-cursor:hand;-fx-focus-color:transparent;");
        }

        if (notifBellBtn != null) { notifBellBtn.setGraphic(svgIcon(IC_BELL, "#4f46e5", 16)); notifBellBtn.setText(null); }
    }

    private void buildCartSectionIcon() {
        if (cartSectionIconBox == null) return;
        cartSectionIconBox.setStyle("-fx-background-color:rgba(99,102,241,0.10);-fx-border-color:rgba(99,102,241,0.22);-fx-border-width:1;-fx-border-radius:12;-fx-background-radius:12;");
        cartSectionIconBox.getChildren().add(svgIcon(IC_CART, "#4f46e5", 20));
    }

    private void buildSummaryIcon() {
        if (sumIconBox == null) return;
        sumIconBox.setStyle("-fx-background-color:rgba(99,102,241,0.10);-fx-border-color:rgba(99,102,241,0.22);-fx-border-width:1;-fx-border-radius:9;-fx-background-radius:9;");
        sumIconBox.getChildren().add(svgIcon(IC_CARD, "#4f46e5", 18));
    }

    private void buildAiIcon() {
        if (aiIconBox == null) return;
        aiIconBox.setStyle("-fx-background-color:linear-gradient(to bottom right,#4f46e5,#6366f1);-fx-background-radius:10;");
        DropShadow ds = new DropShadow(); ds.setColor(Color.web("#4f46e5", 0.28)); ds.setRadius(8); ds.setOffsetY(2);
        aiIconBox.setEffect(ds); aiIconBox.getChildren().add(svgIcon(IC_BULB, "#ffffff", 18));
    }

    // ── AI Suggestions — smart keyword + real DB ──────────────────────
    @FXML private void handleToggleAI() {
        aiExpanded = !aiExpanded;
        if (aiChevron != null) aiChevron.setText(aiExpanded ? "▴" : "▾");
        if (aiBody == null) return;

        if (!aiExpanded) {
            // ── CLOSE: fade out aiBody then hide ──────────────────────
            FadeTransition fadeOut = new FadeTransition(Duration.millis(180), aiBody);
            fadeOut.setFromValue(1.0); fadeOut.setToValue(0.0);
            // Animate aiPanel shrinking back to header-only height
            double closedH = 80;
            javafx.animation.Timeline shrink = new javafx.animation.Timeline(
                    new javafx.animation.KeyFrame(Duration.millis(200),
                            new javafx.animation.KeyValue(aiPanel.prefHeightProperty(), closedH,
                                    javafx.animation.Interpolator.EASE_BOTH))
            );
            shrink.setOnFinished(e -> {
                aiBody.setVisible(false); aiBody.setManaged(false);
                aiBody.setOpacity(1.0);
                aiPanel.setPrefHeight(javafx.scene.layout.Region.USE_COMPUTED_SIZE);
                aiPanel.setMinHeight(javafx.scene.layout.Region.USE_COMPUTED_SIZE);
                aiHeight = 260; // reset for next open
            });
            new javafx.animation.ParallelTransition(fadeOut, shrink).play();
            return;
        }

        // ── OPEN: show body first (small), then animate to comfortable size ──
        aiBody.setVisible(true); aiBody.setManaged(true);
        aiBody.setOpacity(0);

        // Start panel at compact height, expand smoothly
        double openTarget = aiHeight;   // 260 on first open, last used value after
        aiPanel.setPrefHeight(120);
        aiPanel.setMinHeight(120);

        javafx.animation.Timeline expand = new javafx.animation.Timeline(
                new javafx.animation.KeyFrame(Duration.millis(280),
                        new javafx.animation.KeyValue(aiPanel.prefHeightProperty(), openTarget,
                                javafx.animation.Interpolator.EASE_OUT),
                        new javafx.animation.KeyValue(aiPanel.minHeightProperty(), openTarget,
                                javafx.animation.Interpolator.EASE_OUT))
        );
        FadeTransition fadeIn = new FadeTransition(Duration.millis(220), aiBody);
        fadeIn.setFromValue(0.0); fadeIn.setToValue(1.0);
        new javafx.animation.ParallelTransition(expand, fadeIn).play();

        if (aiSuggestionsContainer != null) {
            aiSuggestionsContainer.getChildren().clear();
            Label loading = new Label("⏳ Analyse de votre panier...");
            loading.setStyle("-fx-text-fill:#94a3b8;-fx-font-size:13px;");
            aiSuggestionsContainer.getChildren().add(loading);
        }

        new Thread(() -> {
            // Build keyword map: each cart item → list of complementary keywords
            // Strategy: multi-pass — one query per keyword, 2 results each → diverse suggestions
            java.util.Map<String, List<String>> keywordMap = new java.util.LinkedHashMap<>();
            java.util.Set<Integer> cartIds = new java.util.HashSet<Integer>();

            for (Panier p : panierList) {
                cartIds.add(Integer.valueOf(p.getProduitId()));
                String nom = p.getNomProduit().toLowerCase();
                List<String> kws = new ArrayList<>();

                if (nom.contains("laptop") || nom.contains("ordinateur") || nom.contains("dell")
                        || nom.contains("hp") || nom.contains("lenovo") || nom.contains("macbook")) {
                    // Laptop → case, mouse, mousepad, cable, stand, keyboard, hub
                    kws.add("housse"); kws.add("sacoche"); kws.add("sac laptop");
                    kws.add("souris"); kws.add("tapis souris"); kws.add("tapis de souris");
                    kws.add("câble"); kws.add("support"); kws.add("hub"); kws.add("refroidisseur");
                } else if (nom.contains("souris") || nom.contains("mouse")) {
                    kws.add("tapis"); kws.add("tapis de souris"); kws.add("tapis souris");
                    kws.add("clavier"); kws.add("hub usb"); kws.add("repose poignet");
                } else if (nom.contains("montre") || nom.contains("casio") || nom.contains("watch")
                        || nom.contains("swatch") || nom.contains("seiko")) {
                    kws.add("bracelet"); kws.add("chargeur montre"); kws.add("verre trempé");
                    kws.add("coffret montre"); kws.add("rangement montre");
                } else if (nom.contains("téléphone") || nom.contains("samsung") || nom.contains("iphone")
                        || nom.contains("xiaomi") || nom.contains("huawei") || nom.contains("redmi")) {
                    kws.add("coque"); kws.add("verre trempé"); kws.add("chargeur rapide");
                    kws.add("câble usb"); kws.add("écouteurs"); kws.add("support voiture");
                } else if (nom.contains("casque") || nom.contains("écouteurs") || nom.contains("airpod")) {
                    kws.add("étui"); kws.add("câble jack"); kws.add("micro");
                    kws.add("amplificateur"); kws.add("oreillettes");
                } else if (nom.contains("tablette") || nom.contains("ipad")) {
                    kws.add("housse tablette"); kws.add("stylet"); kws.add("clavier tablette");
                    kws.add("support tablette"); kws.add("câble usb");
                } else if (nom.contains("câble") || nom.contains("usb")) {
                    kws.add("hub"); kws.add("adaptateur"); kws.add("multiprise"); kws.add("chargeur");
                } else if (nom.contains("caméra") || nom.contains("appareil photo")) {
                    kws.add("trépied"); kws.add("carte mémoire"); kws.add("sac photo"); kws.add("objectif");
                } else if (nom.contains("imprimante")) {
                    kws.add("encre"); kws.add("cartouche"); kws.add("papier"); kws.add("câble usb");
                } else {
                    // Generic: use words from product name + broad accessory terms
                    for (String w : nom.split(" ")) if (w.length() > 3) kws.add(w);
                    kws.add("accessoire"); kws.add("protection"); kws.add("câble");
                }
                keywordMap.put(nom, kws);
            }
            if (keywordMap.isEmpty()) keywordMap.put("generic", List.of("accessoire", "câble", "protection"));

            // Multi-pass: query each keyword individually, take best 2 per keyword
            // → guarantees diverse results even if one keyword yields nothing
            List<Produit> suggestions = new ArrayList<>();
            java.util.Set<Integer> seenIds = new java.util.HashSet<Integer>();
            try {
                if (produitDAO != null) {
                    for (List<String> kws : keywordMap.values()) {
                        for (String kw : kws) {
                            if (suggestions.size() >= 6) break;
                            List<Produit> batch = produitDAO.searchByKeywords(List.of(kw));
                            for (Produit prod : batch) {
                                if (suggestions.size() >= 6) break;
                                if (!cartIds.contains(Integer.valueOf(prod.getId()))
                                        && seenIds.add(Integer.valueOf(prod.getId()))) {
                                    suggestions.add(prod);
                                }
                            }
                            if (suggestions.size() >= 6) break;
                        }
                    }
                    // If still empty, broad fallback
                    if (suggestions.isEmpty()) {
                        List<Produit> fallback = produitDAO.searchByKeywords(List.of("accessoire", "câble", "housse", "tapis"));
                        fallback.removeIf(p2 -> cartIds.contains(Integer.valueOf(p2.getId())));
                        if (!fallback.isEmpty())
                            suggestions.addAll(fallback.subList(0, Math.min(6, fallback.size())));
                    }
                }
            } catch (Exception ignored) {}

            final List<Produit> finalSugg = suggestions;
            javafx.application.Platform.runLater(() -> {
                if (aiSuggestionsContainer == null) return;
                aiSuggestionsContainer.getChildren().clear();
                if (finalSugg.isEmpty()) {
                    Label none = new Label("Aucun produit complémentaire trouvé.");
                    none.setStyle("-fx-text-fill:#94a3b8;-fx-font-size:13px;");
                    aiSuggestionsContainer.getChildren().add(none);
                } else {
                    for (Produit prod : finalSugg)
                        aiSuggestionsContainer.getChildren().add(buildSuggestionRow(prod));
                }
            });
        }).start();
    }

    /** Called by the refresh button in the AI suggestions panel header */
    @FXML private void handleRefreshAI() {
        // Force-reload: reset expanded state then trigger toggle
        aiExpanded = false;
        handleToggleAI();
    }

    private HBox buildSuggestionRow(Produit prod) {
        HBox row = new HBox(12); row.setAlignment(Pos.CENTER_LEFT);
        row.setStyle("-fx-padding:10 0;-fx-border-color:#f8f7ff;-fx-border-width:0 0 1 0;");

        Label emojiLbl = new Label(prod.getNom().substring(0, 1).toUpperCase());
        emojiLbl.setStyle("-fx-font-size:18px;-fx-min-width:42;-fx-min-height:42;-fx-max-width:42;-fx-max-height:42;-fx-alignment:center;-fx-background-color:#f0f0ff;-fx-background-radius:10;-fx-font-weight:bold;-fx-text-fill:#4f46e5;");

        VBox info = new VBox(3); HBox.setHgrow(info, Priority.ALWAYS);
        Label nameLbl = new Label(prod.getNom());
        nameLbl.setStyle("-fx-font-size:13px;-fx-font-weight:bold;-fx-text-fill:" + (isDarkMode ? "#e0e7ff" : "#1e1b4b") + ";");
        HBox priceRow = new HBox(6); priceRow.setAlignment(Pos.CENTER_LEFT);
        // Promo lookup for suggestion card (badge "En stock" supprimé — pas notre module)
        Promotion suggPromo = null;
        try { if (promotionDAO != null) suggPromo = promotionDAO.getActivePromoForProduct(prod.getId()); }
        catch (SQLException ignored) {}

        if (suggPromo != null) {
            double reduit = prod.getPrix() * (1.0 - suggPromo.getPourcentage() / 100.0);
            Label prixBarre = new Label(String.format("%.2f DT", prod.getPrix()));
            prixBarre.getStyleClass().add("prix-barre");
            Label priceLbl = new Label(String.format("%.2f DT", reduit));
            priceLbl.setStyle("-fx-font-size:12px;-fx-font-weight:bold;-fx-text-fill:#4f46e5;");
            Label suggBadge = new Label(String.format("-%d%%", (int) suggPromo.getPourcentage()));
            suggBadge.getStyleClass().add("promo-badge");
            priceRow.getChildren().addAll(prixBarre, priceLbl, suggBadge);
            if (suggPromo.getDateFin() != null) {
                Label cd = new Label();
                cd.getStyleClass().add("countdown-label");
                final LocalDateTime df = suggPromo.getDateFin();
                Timeline t = new Timeline(new KeyFrame(Duration.seconds(1), ev -> {
                    long sl = ChronoUnit.SECONDS.between(LocalDateTime.now(), df);
                    if (sl <= 0) { cd.setText("Expirée"); return; }
                    cd.setText(String.format("⏱ %dj %02dh %02dm", sl/86400, (sl%86400)/3600, (sl%3600)/60));
                }));
                t.setCycleCount(Timeline.INDEFINITE); t.play();
                priceRow.getChildren().add(cd);
            }
        } else {
            Label priceLbl = new Label(String.format("%.2f DT", prod.getPrix()));
            priceLbl.setStyle("-fx-font-size:12px;-fx-font-weight:bold;-fx-text-fill:#4f46e5;");
            priceRow.getChildren().add(priceLbl);
        }
        info.getChildren().addAll(nameLbl, priceRow);

        Button addBtn = new Button("+ Panier");
        addBtn.setStyle("-fx-background-color:linear-gradient(to right,#4f46e5,#6366f1);-fx-text-fill:white;-fx-font-size:12px;-fx-font-weight:bold;-fx-background-radius:8;-fx-border-width:0;-fx-padding:8 14;-fx-cursor:hand;-fx-focus-color:transparent;-fx-faint-focus-color:transparent;");
        addBtn.setOnAction(e -> {
            if (prod.getStock() <= 0) { showAlert("Stock", "Ce produit est épuisé."); return; }
            try {
                Panier newItem = new Panier(currentUserId, prod.getId(), 1, prod.getPrix());
                newItem.setNomProduit(prod.getNom());
                panierDAO.addToCart(newItem);
                aiExpanded = false;
                if (aiBody != null) { aiBody.setVisible(false); aiBody.setManaged(false); }
                if (aiChevron != null) aiChevron.setText("▾");
                loadData(); bounceNode(addBtn);
            } catch (Exception ex) { showAlert("Erreur", "Impossible d'ajouter au panier."); }
        });
        row.getChildren().addAll(emojiLbl, info, addBtn);
        return row;
    }

    // ── Cart data ─────────────────────────────────────────────────────
    private void loadData() {
        try {
            panierList = panierDAO.getCartItems(currentUserId);
            cartItemsContainer.getChildren().clear();
            if (panierList.isEmpty()) {
                cartItemsContainer.getChildren().add(createEmptyState());
            } else {
                for (Panier p : panierList) cartItemsContainer.getChildren().add(createCartCard(p));
                staggerCards(cartItemsContainer.getChildren());
            }
            updateTotal();
            loadNotifications();
        } catch (SQLException e) { showAlert("Erreur", "Impossible de charger le panier."); }
    }

    private VBox createEmptyState() {
        VBox empty = new VBox(18); empty.setAlignment(Pos.CENTER); empty.setStyle("-fx-padding:80 0;");
        Label icon = new Label("🛒"); icon.setStyle("-fx-font-size:64px;-fx-opacity:0.25;");
        Label title = new Label("Votre panier est vide");
        title.setStyle("-fx-font-size:22px;-fx-font-weight:bold;-fx-text-fill:" + (isDarkMode ? "#e0e7ff" : "#1e1b4b") + ";");
        Label subtitle = new Label("Découvrez nos produits et ajoutez-les à votre panier");
        subtitle.setStyle("-fx-font-size:13px;-fx-text-fill:#64748b;"); subtitle.setWrapText(true);
        subtitle.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
        Button btn = new Button("Commencer vos achats"); btn.getStyleClass().add("checkout-btn");
        btn.setPrefWidth(240); btn.setOnAction(e -> showAlert("Catalogue", "Navigation vers le catalogue."));
        empty.getChildren().addAll(icon, title, subtitle, btn);
        return empty;
    }

    private VBox createCartCard(Panier p) {
        VBox card = new VBox(0); card.getStyleClass().add("m4-card"); card.setStyle("-fx-padding:16 20;");

        HBox top = new HBox(14); top.setAlignment(Pos.CENTER_LEFT);
        StackPane avatar = new StackPane();
        avatar.setPrefSize(64,64); avatar.setMinSize(64,64); avatar.setMaxSize(64,64);
        avatar.setStyle(avatarGradient(p.getNomProduit()) + "-fx-background-radius:12;");
        Label avatarLbl = new Label(p.getNomProduit().substring(0,1).toUpperCase());
        // FIX: avatar text color — dark on light gradient
        avatarLbl.setStyle("-fx-font-size:24px;-fx-font-weight:bold;-fx-text-fill:#4f46e5;");
        avatar.getChildren().add(avatarLbl);

        VBox info = new VBox(3); HBox.setHgrow(info, Priority.ALWAYS);
        Label nomLabel = new Label(p.getNomProduit()); nomLabel.getStyleClass().add("m4-product-name"); nomLabel.setWrapText(true);
        Label refLabel = new Label("Réf: PRD-00" + p.getProduitId()); refLabel.getStyleClass().add("m4-product-ref");
        // ── Promo lookup — check active promotion for this product ──
        Promotion promo = null;
        try { if (promotionDAO != null) promo = promotionDAO.getActivePromoForProduct(p.getProduitId()); }
        catch (SQLException ignored) {}

        if (promo != null) {
            // Discounted price
            double prixReduit = p.getPrixUnitaire() * (1.0 - promo.getPourcentage() / 100.0);

            Label badge = new Label(String.format("-%d%%", (int) promo.getPourcentage()));
            badge.getStyleClass().add("promo-badge");

            Label prixBarre = new Label(String.format("%.2f DT", p.getPrixUnitaire()));
            prixBarre.getStyleClass().add("prix-barre");

            Label prixLabel = new Label(String.format("%.2f DT", prixReduit));
            prixLabel.getStyleClass().add("m4-price");

            HBox priceRow = new HBox(6);
            priceRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            priceRow.getChildren().addAll(prixBarre, prixLabel, badge);

            info.getChildren().addAll(nomLabel, refLabel, priceRow);

            // Countdown if promo has an expiry
            if (promo.getDateFin() != null) {
                Label countdownLabel = new Label();
                countdownLabel.getStyleClass().add("countdown-label");
                info.getChildren().add(countdownLabel);
                final LocalDateTime dateFin = promo.getDateFin();
                Timeline countdown = new Timeline(new KeyFrame(Duration.seconds(1), ev -> {
                    long secondsLeft = ChronoUnit.SECONDS.between(LocalDateTime.now(), dateFin);
                    if (secondsLeft <= 0) {
                        countdownLabel.setText("Promo expir\u00e9e");
                    } else {
                        long days  = secondsLeft / 86400;
                        long hours = (secondsLeft % 86400) / 3600;
                        long mins  = (secondsLeft % 3600) / 60;
                        long secs  = secondsLeft % 60;
                        countdownLabel.setText(String.format("\u23F1 %dj %02dh %02dm %02ds", days, hours, mins, secs));
                    }
                }));
                countdown.setCycleCount(Timeline.INDEFINITE);
                countdown.play();
            }
        } else {
            // No promo — show normal price
            Label prixLabel = new Label(String.format("%.2f DT", p.getPrixUnitaire()));
            prixLabel.getStyleClass().add("m4-price");
            info.getChildren().addAll(nomLabel, refLabel, prixLabel);
        }

        // FIX: bigger delete button
        Button deleteBtn = new Button();
        deleteBtn.setStyle("-fx-background-color:rgba(239,68,68,0.10);-fx-border-color:rgba(239,68,68,0.35);-fx-border-width:1.5;-fx-border-radius:12;-fx-background-radius:12;-fx-cursor:hand;-fx-min-width:44;-fx-min-height:44;-fx-max-width:44;-fx-max-height:44;-fx-focus-color:transparent;-fx-faint-focus-color:transparent;");
        deleteBtn.setGraphic(svgIcon(IC_TRASH, "#ef4444", 18));
        deleteBtn.setOnAction(e -> {
            try {
                panierDAO.removeFromCart(p.getPanierId());
                panierList.remove(p);
                // FIX: reset promo si le nouveau sous-total passe sous le minimum
                checkAndResetPromoAfterChange();
                loadData();
            }
            catch (SQLException ex) { showAlert("Erreur", "Impossible de supprimer l'article."); }
        });
        top.getChildren().addAll(avatar, info, deleteBtn);

        Region divider = new Region(); divider.setPrefHeight(1); divider.setMaxHeight(1);
        divider.setStyle("-fx-background-color:" + (isDarkMode ? "rgba(255,255,255,0.07)" : "#f1f0ff") + ";");
        VBox.setMargin(divider, new Insets(14,0,0,0));

        HBox bottom = new HBox(12); bottom.setAlignment(Pos.CENTER_LEFT);
        VBox.setMargin(bottom, new Insets(12,0,0,0));

        // FIX: Details button — shows styled popup
        Button detailsBtn = new Button("Détails"); detailsBtn.getStyleClass().add("btn-details");
        detailsBtn.setGraphic(svgIcon("M6 9l6 6 6-6", "#4f46e5", 13));
        detailsBtn.setGraphicTextGap(6);
        detailsBtn.setContentDisplay(javafx.scene.control.ContentDisplay.LEFT);
        detailsBtn.setOnAction(e -> showStyledDetails(p));

        HBox qtyContainer = new HBox(0); qtyContainer.setAlignment(Pos.CENTER);
        qtyContainer.setStyle("-fx-background-color:rgba(99,102,241,0.08);-fx-background-radius:10;-fx-padding:4;");
        Button btnMinus = new Button("−"); btnMinus.getStyleClass().add("m4-qty-minus");
        // FIX: qty label always visible
        Label qtyLabel = new Label(String.valueOf(p.getQuantite()));
        qtyLabel.setStyle("-fx-font-size:16px;-fx-font-weight:bold;-fx-min-width:36;-fx-alignment:center;-fx-text-fill:" + (isDarkMode ? "#a5b4fc" : "#4f46e5") + ";");
        Button btnPlus = new Button("+"); btnPlus.getStyleClass().add("m4-qty-plus");
        qtyContainer.getChildren().addAll(btnMinus, qtyLabel, btnPlus);

        Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS);
        VBox subtotalBox = new VBox(2); subtotalBox.setAlignment(Pos.CENTER_RIGHT);
        Label stLbl = new Label("Sous-total"); stLbl.setStyle("-fx-font-size:10px;-fx-text-fill:#94a3b8;");
        Label stVal = new Label(String.format("%.2f DT", p.getSousTotal())); stVal.getStyleClass().add("m4-subtotal");
        subtotalBox.getChildren().addAll(stLbl, stVal);
        bottom.getChildren().addAll(detailsBtn, qtyContainer, spacer, subtotalBox);

        btnMinus.setOnAction(e -> {
            if (p.getQuantite() > 1) {
                try {
                    panierDAO.updateQuantite(p.getPanierId(), p.getQuantite()-1); p.setQuantite(p.getQuantite()-1);
                    qtyLabel.setText(String.valueOf(p.getQuantite()));
                    stVal.setText(String.format("%.2f DT", p.getSousTotal()));
                    updateTotal(); bounceNode(btnMinus);
                } catch (SQLException ex) { showAlert("Erreur", "Impossible de modifier la quantité."); }
            }
        });
        btnPlus.setOnAction(e -> {
            try {
                panierDAO.updateQuantite(p.getPanierId(), p.getQuantite()+1); p.setQuantite(p.getQuantite()+1);
                qtyLabel.setText(String.valueOf(p.getQuantite()));
                stVal.setText(String.format("%.2f DT", p.getSousTotal()));
                updateTotal(); bounceNode(btnPlus);
            } catch (SQLException ex) { showAlert("Erreur", "Impossible de modifier la quantité."); }
        });

        card.getChildren().addAll(top, divider, bottom);
        return card;
    }

    // Styled details dialog
    private void showStyledDetails(Panier p) {
        Stage dialog = new Stage();
        dialog.setTitle("Détails du produit");
        dialog.setResizable(false);

        String bg    = isDarkMode ? "#0f172a" : "#ffffff";
        String cardBg= isDarkMode ? "#1e293b" : "#f8f7ff";
        String txt1  = isDarkMode ? "#e0e7ff" : "#1e1b4b";
        String txt2  = isDarkMode ? "#94a3b8" : "#64748b";
        String accent= isDarkMode ? "#a5b4fc" : "#4f46e5";

        VBox root = new VBox(0);
        root.setPrefWidth(400);
        root.setStyle("-fx-background-color:" + bg + ";");

        // ── Header with product avatar ──────────────────────────────
        HBox header = new HBox(16); header.setAlignment(Pos.CENTER_LEFT);
        header.setStyle("-fx-background-color:linear-gradient(to right,#3730a3,#4f46e5,#6366f1);-fx-padding:20 24 20 24;");

        // Product avatar (same as cart card)
        StackPane avatarSP = new StackPane();
        avatarSP.setPrefSize(56,56); avatarSP.setMinSize(56,56); avatarSP.setMaxSize(56,56);
        avatarSP.setStyle(avatarGradient(p.getNomProduit()) + "-fx-background-radius:14;-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.20),8,0,0,2);");
        Label avatarLbl = new Label(p.getNomProduit().substring(0,1).toUpperCase());
        avatarLbl.setStyle("-fx-font-size:26px;-fx-font-weight:800;-fx-text-fill:#4f46e5;");
        avatarSP.getChildren().add(avatarLbl);

        VBox titleBox = new VBox(5);
        Label dlgTitle = new Label(p.getNomProduit());
        dlgTitle.setStyle("-fx-font-size:16px;-fx-font-weight:bold;-fx-text-fill:white;");
        dlgTitle.setWrapText(true); dlgTitle.setMaxWidth(260);
        Label dlgRef = new Label("Référence : PRD-" + String.format("%04d", p.getProduitId()));
        dlgRef.setStyle("-fx-font-size:12px;-fx-text-fill:rgba(255,255,255,0.65);");
        titleBox.getChildren().addAll(dlgTitle, dlgRef);
        header.getChildren().addAll(avatarSP, titleBox);

        // ── Body ────────────────────────────────────────────────────
        VBox body = new VBox(12);
        body.setStyle("-fx-padding:20 20 12 20;-fx-background-color:" + bg + ";");

        body.getChildren().addAll(
                detailRow("Prix unitaire",  String.format("%.2f DT", p.getPrixUnitaire()), "#10b981", cardBg, txt2, accent),
                detailRow("Quantité",       String.valueOf(p.getQuantite()),               "#6366f1", cardBg, txt2, accent),
                detailRow("Sous-total",     String.format("%.2f DT", p.getSousTotal()),    "#f59e0b", cardBg, txt2, "#f59e0b")
        );

        // ── Footer ──────────────────────────────────────────────────
        HBox footer = new HBox(); footer.setAlignment(Pos.CENTER_RIGHT);
        footer.setStyle("-fx-padding:16 20 20 20;-fx-background-color:" + bg + ";");
        Button close = new Button("Fermer");
        close.setStyle("-fx-background-color:linear-gradient(to right,#4f46e5,#6366f1);-fx-text-fill:white;-fx-font-weight:bold;-fx-font-size:13px;-fx-background-radius:10;-fx-padding:10 28;-fx-cursor:hand;-fx-border-width:0;-fx-focus-color:transparent;-fx-faint-focus-color:transparent;");
        close.setOnAction(e -> dialog.close());
        footer.getChildren().add(close);

        root.getChildren().addAll(header, body, footer);
        Scene scene = new Scene(root, 420, 370);
        scene.setFill(Color.TRANSPARENT);
        dialog.setScene(scene);
        dialog.show();
    }

    private HBox detailRow(String label, String value, String dotColor, String cardBg, String txt2, String valColor) {
        HBox row = new HBox(14); row.setAlignment(Pos.CENTER_LEFT);
        row.setStyle("-fx-background-color:" + cardBg + ";-fx-background-radius:12;-fx-padding:14 16;");

        // Colored dot
        javafx.scene.shape.Circle dot = new javafx.scene.shape.Circle(7);
        dot.setFill(Color.web(dotColor));

        Label lbl = new Label(label);
        lbl.setStyle("-fx-font-size:13px;-fx-text-fill:" + txt2 + ";");
        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        Label val = new Label(value);
        val.setStyle("-fx-font-size:14px;-fx-font-weight:bold;-fx-text-fill:" + valColor + ";");
        row.getChildren().addAll(dot, lbl, sp, val);
        return row;
    }

    private String avatarGradient(String name) {
        int hash = name.hashCode() & 0x7fffffff;
        String[][] g = {{"#fef3c7","#fde68a"},{"#dbeafe","#bfdbfe"},{"#dcfce7","#bbf7d0"},{"#fce7f3","#fbcfe8"},{"#ede9fe","#ddd6fe"},{"#ffedd5","#fed7aa"},{"#f0fdf4","#86efac"},{"#cffafe","#a5f3fc"}};
        return "-fx-background-color:linear-gradient(135deg," + g[hash % g.length][0] + "," + g[hash % g.length][1] + ");";
    }

    private void updateTotal() {
        double sousTotal = panierList.stream().mapToDouble(Panier::getSousTotal).sum();
        int    nb        = panierList.stream().mapToInt(Panier::getQuantite).sum();
        double total     = sousTotal + livraisonFee - discountAmt;
        if (labelSousTotal    != null) labelSousTotal.setText(String.format("%.2f DT", sousTotal));
        if (labelLivraison    != null) labelLivraison.setText(String.format("%.2f DT", livraisonFee));
        if (labelTotal        != null) labelTotal.setText(String.format("%.2f DT", total));
        if (labelArticlesBadge!= null) labelArticlesBadge.setText(nb + " article" + (nb > 1 ? "s" : ""));
        if (cartCountBadge    != null) cartCountBadge.setText(String.valueOf(nb));
    }

    // ── Drag-to-resize: aiResizeHandle ────────────────────────────────────
    private double resizeDragStartY;
    private double resizeStartHeight;

    @FXML private void handleResizeStart(javafx.scene.input.MouseEvent e) {
        resizeDragStartY  = e.getScreenY();
        resizeStartHeight = aiHeight;
        e.consume();
    }

    @FXML private void handleResizeDrag(javafx.scene.input.MouseEvent e) {
        // Handle is at the TOP of the panel (bottom of screen).
        // Dragging UP (negative delta) = panel gets BIGGER.
        // Dragging DOWN (positive delta) = panel gets SMALLER.
        double delta = resizeDragStartY - e.getScreenY();   // inverted
        aiHeight = Math.max(120, Math.min(550, resizeStartHeight + delta));
        if (aiPanel != null) {
            aiPanel.setPrefHeight(aiHeight);
            aiPanel.setMinHeight(aiHeight);
        }
        e.consume();
    }

    /** Highlight on hover */
    @FXML private void handleResizeEnter() {
        if (aiResizeHandle != null)
            aiResizeHandle.setStyle(
                    "-fx-background-color:rgba(99,102,241,0.18);" +
                            "-fx-background-radius:4;-fx-cursor:s-resize;-fx-padding:3 0;");
    }
    /** Remove highlight */
    @FXML private void handleResizeExit() {
        if (aiResizeHandle != null)
            aiResizeHandle.setStyle(
                    "-fx-background-color:rgba(99,102,241,0.07);" +
                            "-fx-background-radius:4;-fx-cursor:s-resize;-fx-padding:3 0;");
    }

    @FXML private void handleAppliquerPromo() {
        if (promoField == null || promoField.getText().isBlank()) { setPromoMsg("❌ Entrez un code promo.", false); return; }
        if (codePromoDAO == null) { setPromoMsg("❌ Service promo indisponible.", false); return; }
        try {
            double sousTotal = panierList.stream().mapToDouble(Panier::getSousTotal).sum();
            CodePromoDAO.ResultatCode res = codePromoDAO.validerCode(promoField.getText().trim());
            if (!res.valide) { setPromoMsg("❌ " + res.message, false); discountAmt = 0; resetReductionRow(); }
            else if (sousTotal < res.montantMin) { setPromoMsg(String.format("❌ Minimum : %.2f DT", res.montantMin), false); discountAmt = 0; resetReductionRow(); }
            else {
                discountAmt  = res.calculerReduction(sousTotal);
                promoMinimum = res.montantMin;
                promoCurrent = res.codeId;
                String lbl = "POURCENTAGE".equals(res.type) ? (int)res.valeur + "%" : String.format("%.2f DT", res.valeur);
                setPromoMsg("✅ Code appliqué ! Réduction : " + lbl, true);
                if (rowReduction  != null) { rowReduction.setVisible(true); rowReduction.setManaged(true); }
                if (labelReduction!= null) labelReduction.setText("-" + String.format("%.2f DT", discountAmt));
            }
            updateTotal();
        } catch (SQLException e) { setPromoMsg("❌ Erreur validation.", false); }
    }

    private void setPromoMsg(String msg, boolean ok) {
        if (labelPromoMessage == null) return;
        labelPromoMessage.setText(msg);
        labelPromoMessage.setStyle(ok ? "-fx-text-fill:#16a34a;-fx-font-size:12px;-fx-font-weight:bold;" : "-fx-text-fill:#ef4444;-fx-font-size:12px;-fx-font-weight:bold;");
    }

    private void checkAndResetPromoAfterChange() {
        if (discountAmt <= 0) return;  // no active promo
        double newSubtotal = panierList.stream().mapToDouble(Panier::getSousTotal).sum();
        if (newSubtotal < promoMinimum || panierList.isEmpty()) {
            discountAmt  = 0;
            promoMinimum = 0;
            promoCurrent = -1;
            resetReductionRow();
            if (promoField != null) promoField.clear();
            setPromoMsg("Code retiré : sous-total insuffisant.", false);
        }
    }

    private void resetReductionRow() {
        if (rowReduction  != null) { rowReduction.setVisible(false); rowReduction.setManaged(false); }
        if (labelReduction!= null) labelReduction.setText("-0.00 DT");
    }

    @FXML private void handleVider() {
        showCustomConfirm(
                "Vider le panier",
                "Tous les articles seront supprimés définitivement. Cette action est irréversible.",
                "Oui, vider", "#dc2626",
                () -> {
                    try { panierDAO.clearCart(currentUserId); panierList.clear(); loadData(); }
                    catch (SQLException e) { showAlert("Erreur", "Impossible de vider."); }
                }
        );
    }

    @FXML private void handlePayer() {
        if (panierList.isEmpty()) { showAlert("Panier vide", "Ajoutez des articles avant de payer."); return; }
        try {
            double total = panierList.stream().mapToDouble(Panier::getSousTotal).sum() + livraisonFee - discountAmt;
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/hanouti/hanoutiem4/Paiement.fxml"));
            Parent root = loader.load(); PaiementController ctrl = loader.getController();
            ctrl.setMontantTotal(Math.max(0, total)); ctrl.setUserEmail(UserSession.getInstance().getUserEmail());
            Stage stage = (Stage) cartItemsContainer.getScene().getWindow();
            stage.setTitle("7anouti-E — Paiement"); stage.setScene(new Scene(root, 1000, 850)); stage.setMaximized(false);
        } catch (IOException e) { showAlert("Erreur", e.getMessage()); }
    }

    private void navigate(String fxml, String title, int w, int h) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/hanouti/hanoutiem4/" + fxml));
            Parent root = loader.load(); Stage stage = (Stage) cartItemsContainer.getScene().getWindow();
            stage.setTitle(title); stage.setScene(new Scene(root, w, h));
        } catch (IOException e) { showAlert("Erreur navigation", e.getMessage()); }
    }

    private void showProfileMenu() {
        AnchorPane overlay = new AnchorPane();
        overlay.setStyle("-fx-background-color:transparent;");
        AnchorPane.setTopAnchor(overlay, 0.0); AnchorPane.setBottomAnchor(overlay, 0.0);
        AnchorPane.setLeftAnchor(overlay, 0.0); AnchorPane.setRightAnchor(overlay, 0.0);

        VBox card = new VBox(0); card.setMaxWidth(256); card.setMinWidth(256);
        card.setStyle("-fx-background-color:#ffffff;-fx-background-radius:16;-fx-border-color:rgba(99,102,241,0.16);-fx-border-width:1;-fx-border-radius:16;-fx-effect:dropshadow(gaussian,rgba(79,70,229,0.22),24,0.12,0,8);");
        card.setOnMouseClicked(javafx.event.Event::consume);

        VBox header = new VBox(8);
        header.setStyle("-fx-background-color:linear-gradient(to bottom right,#1e1b4b,#4f46e5);-fx-background-radius:16 16 0 0;-fx-padding:20 20 18 20;");
        javafx.scene.layout.StackPane avatarSP = new javafx.scene.layout.StackPane();
        javafx.scene.shape.Circle circle = new javafx.scene.shape.Circle(26); circle.setFill(Color.web("#FFFFFF", 0.16));
        Label initLbl = new Label(getInitials(UserSession.getInstance().getUserName()));
        initLbl.setStyle("-fx-text-fill:white;-fx-font-size:16px;-fx-font-weight:bold;");
        avatarSP.getChildren().addAll(circle, initLbl);
        Label nameLbl = new Label(UserSession.getInstance().getUserName()); nameLbl.setStyle("-fx-text-fill:white;-fx-font-size:14px;-fx-font-weight:bold;");
        Label emailLbl = new Label(UserSession.getInstance().getUserEmail()); emailLbl.setStyle("-fx-text-fill:rgba(255,255,255,0.65);-fx-font-size:11px;");
        Label roleLbl = new Label("  Acheteur  "); roleLbl.setStyle("-fx-text-fill:rgba(255,255,255,0.82);-fx-font-size:10px;-fx-font-weight:bold;-fx-background-color:rgba(255,255,255,0.18);-fx-background-radius:20;-fx-padding:3 10;");
        header.getChildren().addAll(avatarSP, nameLbl, emailLbl, roleLbl);

        VBox items = new VBox(0); items.setStyle("-fx-padding:6 0;");
        items.getChildren().add(profileItem("📝", "Modifier mon profil", "#374151", false,
                () -> { rootPane.getChildren().remove(overlay); showAlert("Profil", "Modification du profil."); }));
        items.getChildren().add(profileItem("🕐", "Historique de paiement", "#374151", false,
                () -> { rootPane.getChildren().remove(overlay); navigate("HistoriquePaiement.fxml", "7anouti-E — Historique", 1200, 750); }));
        Region sep = new Region(); sep.setPrefHeight(1); sep.setMaxHeight(1); sep.setStyle("-fx-background-color:rgba(99,102,241,0.10);");
        items.getChildren().add(sep);
        items.getChildren().add(profileItem("⎋", "Déconnexion", "#EF4444", true, () -> {
            rootPane.getChildren().remove(overlay);
            Alert c2 = new Alert(Alert.AlertType.CONFIRMATION); c2.setTitle("Déconnexion"); c2.setContentText("Voulez-vous vous déconnecter ?");
            ButtonType o = new ButtonType("Se déconnecter"); ButtonType n = new ButtonType("Annuler", ButtonBar.ButtonData.CANCEL_CLOSE);
            c2.getButtonTypes().setAll(o, n);
            c2.showAndWait().ifPresent(r -> { if (r == o) { UserSession.getInstance().logout(); ((Stage) rootPane.getScene().getWindow()).close(); } });
        }));

        card.getChildren().addAll(header, items);
        AnchorPane.setTopAnchor(card, 60.0); AnchorPane.setRightAnchor(card, 12.0);
        overlay.getChildren().add(card); rootPane.getChildren().add(overlay);

        overlay.setOnMouseClicked(e -> {
            FadeTransition ft = new FadeTransition(Duration.millis(150), card); ft.setToValue(0);
            ft.setOnFinished(ev -> rootPane.getChildren().remove(overlay)); ft.play();
        });
        card.setOpacity(0); card.setTranslateY(-10);
        FadeTransition ft = new FadeTransition(Duration.millis(200), card); ft.setToValue(1);
        TranslateTransition tt = new TranslateTransition(Duration.millis(200), card);
        tt.setToY(0); tt.setInterpolator(Interpolator.EASE_OUT);
        new ParallelTransition(ft, tt).play();
    }

    private VBox profileItem(String icon, String label, String color, boolean bold, Runnable action) {
        HBox row = new HBox(12); row.setAlignment(Pos.CENTER_LEFT);
        String base  = "-fx-padding:11 20;-fx-background-color:transparent;-fx-cursor:hand;";
        String hover = "#EF4444".equals(color) ? "-fx-padding:11 20;-fx-background-color:rgba(239,68,68,0.07);-fx-cursor:hand;" : "-fx-padding:11 20;-fx-background-color:rgba(99,102,241,0.07);-fx-cursor:hand;";
        row.setStyle(base);
        row.setOnMouseEntered(e -> row.setStyle(hover)); row.setOnMouseExited(e -> row.setStyle(base));
        row.setOnMouseClicked(e -> { e.consume(); action.run(); });
        Label ic = new Label(icon); ic.setStyle("-fx-font-size:14px;-fx-min-width:20;");
        Label tx = new Label(label); tx.setStyle("-fx-font-size:13px;-fx-text-fill:"+color+";-fx-font-weight:"+(bold?"bold":"600")+";");
        row.getChildren().addAll(ic, tx);
        return new VBox(row);
    }

    private String getInitials(String name) {
        if (name == null || name.isBlank()) return "?";
        String[] parts = name.trim().split("\\s+");
        return parts.length == 1 ? parts[0].substring(0, Math.min(2, parts[0].length())).toUpperCase()
                : ("" + parts[0].charAt(0) + parts[parts.length-1].charAt(0)).toUpperCase();
    }

    private SVGPath svgIcon(String d, String strokeColor, double size) {
        SVGPath p = new SVGPath(); p.setContent(d); p.setFill(Color.TRANSPARENT); p.setStroke(Color.web(strokeColor));
        p.setStrokeWidth(1.9); p.setStrokeLineCap(StrokeLineCap.ROUND); p.setStrokeLineJoin(StrokeLineJoin.ROUND);
        double s = size/24.0; p.setScaleX(s); p.setScaleY(s); return p;
    }

    private void playEntrance(HBox h) {
        if (h == null) return; h.setTranslateY(-60); h.setOpacity(0);
        TranslateTransition tt = new TranslateTransition(Duration.millis(450), h); tt.setToY(0);
        FadeTransition ft = new FadeTransition(Duration.millis(450), h); ft.setToValue(1); ft.setInterpolator(Interpolator.EASE_OUT);
        new ParallelTransition(tt, ft).play();
    }

    private void staggerCards(javafx.collections.ObservableList<Node> nodes) {
        for (int i = 0; i < nodes.size(); i++) {
            Node c = nodes.get(i); c.setOpacity(0); c.setTranslateY(20);
            FadeTransition f = new FadeTransition(Duration.millis(360), c); f.setFromValue(0); f.setToValue(1);
            TranslateTransition t = new TranslateTransition(Duration.millis(360), c); t.setFromY(20); t.setToY(0); t.setInterpolator(Interpolator.EASE_OUT);
            ParallelTransition pt = new ParallelTransition(f, t); pt.setDelay(Duration.millis(160 + i*65)); pt.play();
        }
    }

    private void bounceNode(Node n) {
        if (n == null) return;
        ScaleTransition sc = new ScaleTransition(Duration.millis(110), n);
        sc.setFromX(0.88); sc.setFromY(0.88); sc.setToX(1.0); sc.setToY(1.0); sc.setInterpolator(Interpolator.EASE_OUT); sc.play();
    }

    private void showAlert(String title, String message) {
        Alert a = new Alert(Alert.AlertType.INFORMATION); a.setTitle(title); a.setContentText(message); a.showAndWait();
    }
    private void showCustomConfirm(String title, String message,
                                   String confirmLabel, String confirmColor, Runnable onConfirm) {
        boolean dark  = isDarkMode;
        String bgCard = dark ? "#1a1a2e" : "#FFFFFF";
        String t1     = dark ? "#f8fafc"  : "#0f172a";
        String t3     = dark ? "#94a3b8"  : "#64748b";
        String divClr = dark ? "rgba(255,255,255,0.08)" : "#f1f5f9";
        String cancelBg  = dark ? "#252540" : "#f8fafc";
        String cancelTxt = dark ? "#cbd5e1" : "#475569";

        StackPane overlay = new StackPane();
        overlay.setStyle("-fx-background-color:rgba(0,0,0,0.50);");
        AnchorPane.setTopAnchor(overlay,0.0); AnchorPane.setBottomAnchor(overlay,0.0);
        AnchorPane.setLeftAnchor(overlay,0.0); AnchorPane.setRightAnchor(overlay,0.0);

        VBox card = new VBox(20);
        card.setMaxWidth(420); card.setMaxHeight(Region.USE_PREF_SIZE);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setStyle("-fx-background-color:" + bgCard + ";-fx-background-radius:16;" +
                "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.35),60,0,0,20);-fx-padding:32 36 28 36;");
        card.setScaleX(0.88); card.setScaleY(0.88); card.setOpacity(0);
        StackPane.setAlignment(card, Pos.CENTER);

        Label titleLbl = new Label(title);
        titleLbl.setStyle("-fx-font-size:17px;-fx-font-weight:bold;-fx-text-fill:" + t1 + ";-fx-wrap-text:true;");
        titleLbl.setWrapText(true);

        Label msgLbl = new Label(message);
        msgLbl.setStyle("-fx-font-size:13px;-fx-text-fill:" + t3 + ";-fx-wrap-text:true;-fx-line-spacing:4;");
        msgLbl.setWrapText(true); msgLbl.setMaxWidth(360);

        Region div = new Region(); div.setPrefHeight(1); div.setMaxHeight(1);
        div.setStyle("-fx-background-color:" + divClr + ";");

        HBox btnRow = new HBox(10); btnRow.setAlignment(Pos.CENTER_RIGHT);
        String btnReset = "-fx-focus-color:transparent;-fx-faint-focus-color:transparent;-fx-border-width:0;";
        Button cancelBtn = new Button("Annuler");
        cancelBtn.setStyle(btnReset + "-fx-background-color:" + cancelBg + ";-fx-background-radius:10;" +
                "-fx-text-fill:" + cancelTxt + ";-fx-font-size:13px;-fx-font-weight:600;-fx-padding:10 24;-fx-cursor:hand;");
        Button confirmBtn = new Button(confirmLabel);
        confirmBtn.setStyle(btnReset + "-fx-background-color:" + confirmColor + ";-fx-background-radius:10;" +
                "-fx-text-fill:white;-fx-font-size:13px;-fx-font-weight:bold;-fx-padding:10 28;-fx-cursor:hand;");
        btnRow.getChildren().addAll(cancelBtn, confirmBtn);
        card.getChildren().addAll(titleLbl, msgLbl, div, btnRow);
        overlay.getChildren().add(card);
        rootPane.getChildren().add(overlay);

        Runnable dismiss = () -> {
            FadeTransition fo = new FadeTransition(Duration.millis(150), overlay);
            fo.setToValue(0); fo.setOnFinished(ev -> rootPane.getChildren().remove(overlay)); fo.play();
        };
        cancelBtn.setOnAction(e -> dismiss.run());
        confirmBtn.setOnAction(e -> { dismiss.run(); onConfirm.run(); });
        overlay.setOnMouseClicked(e -> { if (e.getTarget() == overlay) dismiss.run(); });

        FadeTransition fi = new FadeTransition(Duration.millis(180), card); fi.setToValue(1);
        ScaleTransition si = new ScaleTransition(Duration.millis(200), card);
        si.setToX(1); si.setToY(1); si.setInterpolator(Interpolator.EASE_OUT);
        new ParallelTransition(fi, si).play();
    }

    // ══════════════════════════════════════════════════════════════════
    // SMART NOTIFICATIONS — uniquement les données du module 4
    // ══════════════════════════════════════════════════════════════════
    private void loadNotifications() {
        if (notifPanel == null) return;
        notifPanel.getChildren().clear();
        java.util.List<HBox> cards = new java.util.ArrayList<>();

        // Helper: skip this notification if user already dismissed it this session
        java.util.function.Predicate<String> notDismissed = id -> !dismissedNotifIds.contains(id);

        // ── 1. Promo active sur un article du panier ──────────────────
        for (Panier p : panierList) {
            try {
                if (promotionDAO == null) break;
                Promotion promo = promotionDAO.getActivePromoForProduct(p.getProduitId());
                if (promo != null) {
                    double economie = p.getPrixUnitaire() * promo.getPourcentage() / 100.0 * p.getQuantite();
                    String msg = promo.getDateFin() != null
                            ? String.format("⏱ Promo -%d%% sur %s — expire bientôt !", (int) promo.getPourcentage(), p.getNomProduit())
                            : String.format("🎉 %s est en promo -%d%% !", p.getNomProduit(), (int) promo.getPourcentage());
                    if (notDismissed.test(msg))
                        cards.add(buildNotifCard(
                                "M9 14l-4-4 4-4M15 10h-4M19 21l-7-4-7 4V5a2 2 0 0 1 2-2h10a2 2 0 0 1 2 2z",
                                "#059669", "#dcfce7", "#065f46",
                                msg,
                                String.format("Tu économises %.2f DT sur ta commande actuelle.", economie)
                        ));
                    break;
                }
            } catch (java.sql.SQLException ignored) {}
        }

        // ── 2. Code promo qui expire dans moins de 3 jours ───────────
        try {
            if (codePromoDAO != null) {
                java.sql.Connection conn = com.hanouti.hanoutiem4.util.DBConnection.getInstance().getConnection();
                String sql = "SELECT code, date_fin FROM codes_promo WHERE actif = 1 " +
                        "AND date_fin IS NOT NULL AND date_fin > NOW() " +
                        "AND date_fin <= DATE_ADD(NOW(), INTERVAL 3 DAY) LIMIT 1";
                try (java.sql.PreparedStatement ps = conn.prepareStatement(sql);
                     java.sql.ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        String code = rs.getString("code");
                        long heures = java.time.temporal.ChronoUnit.HOURS.between(
                                java.time.LocalDateTime.now(),
                                rs.getTimestamp("date_fin").toLocalDateTime());
                        String codeMsg = "⏰ Code promo « " + code + " » expire dans " + heures + "h !";
                        if (notDismissed.test(codeMsg))
                            cards.add(buildNotifCard(
                                    "M12 8v4l3 3M21 12a9 9 0 1 1-18 0 9 9 0 0 1 18 0z",
                                    "#d97706", "#fef3c7", "#78350f",
                                    codeMsg,
                                    "Applique ce code maintenant avant qu'il ne soit plus valable."
                            ));
                    }
                }
            }
        } catch (Exception ignored) {}

        // ── 4. Rappel dernier paiement ────────────────────────────────
        try {
            java.sql.Connection conn = com.hanouti.hanoutiem4.util.DBConnection.getInstance().getConnection();
            String sql = "SELECT montant, methode, date_paiement FROM paiements " +
                    "WHERE user_id = ? AND statut = 'validé' " +
                    "ORDER BY date_paiement DESC LIMIT 1";
            try (java.sql.PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, currentUserId);
                java.sql.ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    double montant = rs.getDouble("montant");
                    String methode = rs.getString("methode");
                    java.time.LocalDateTime date = rs.getTimestamp("date_paiement").toLocalDateTime();
                    long jours = java.time.temporal.ChronoUnit.DAYS.between(date, java.time.LocalDateTime.now());
                    if (jours <= 7) {
                        String payMsg = String.format("Dernier paiement : %.2f DT via %s", montant, methode);
                        if (notDismissed.test(payMsg))
                            cards.add(buildNotifCard(
                                    "M9 5H7a2 2 0 0 0-2 2v12a2 2 0 0 0 2 2h10a2 2 0 0 0 2-2V7a2 2 0 0 0-2-2h-2M9 5a2 2 0 0 0 2 2h2a2 2 0 0 0 2-2M9 5a2 2 0 0 0 2-2h2a2 2 0 0 0 2 2",
                                    "#4f46e5", "#ede9fe", "#3730a3",
                                    payMsg,
                                    jours == 0 ? "Effectué aujourd'hui — voir le reçu dans l'historique."
                                            : String.format("Effectué il y a %d jour%s.", jours, jours > 1 ? "s" : "")
                            ));
                    }
                }
            }
        } catch (Exception ignored) {}

        // ── Afficher ──────────────────────────────────────────────────
        if (!cards.isEmpty()) {
            notifPanel.getChildren().addAll(cards);
            notifPanel.setVisible(true);
            notifPanel.setManaged(true);
        } else {
            notifPanel.setVisible(false);
            notifPanel.setManaged(false);
        }
    }

    private HBox buildNotifCard(String iconPath, String iconColor,
                                String bgColor, String textColor,
                                String title, String subtitle) {
        final String notifId = title; // use title as unique ID for this session
        HBox card = new HBox(12);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setStyle(
                "-fx-background-color:" + bgColor + ";" +
                        "-fx-background-radius:12;" +
                        "-fx-border-color:" + iconColor + ";" +
                        "-fx-border-width:0 0 0 3;" +
                        "-fx-padding:12 16 12 14;"
        );

        StackPane iconBox = new StackPane();
        iconBox.setMinSize(32, 32); iconBox.setMaxSize(32, 32);
        iconBox.setStyle("-fx-background-color:" + iconColor + ";-fx-background-radius:8;");
        iconBox.getChildren().add(svgIcon(iconPath, "#ffffff", 15));

        VBox textBox = new VBox(2); HBox.setHgrow(textBox, Priority.ALWAYS);
        Label titleLbl = new Label(title);
        titleLbl.setStyle("-fx-font-size:13px;-fx-font-weight:bold;-fx-text-fill:" + textColor + ";-fx-wrap-text:true;");
        titleLbl.setWrapText(true);
        Label subtitleLbl = new Label(subtitle);
        subtitleLbl.setStyle("-fx-font-size:11px;-fx-text-fill:" + textColor + ";-fx-opacity:0.75;-fx-wrap-text:true;");
        subtitleLbl.setWrapText(true);
        textBox.getChildren().addAll(titleLbl, subtitleLbl);

        Button dismiss = new Button("×");
        dismiss.setStyle(
                "-fx-background-color:transparent;-fx-border-width:0;" +
                        "-fx-text-fill:" + textColor + ";-fx-font-size:18px;-fx-cursor:hand;" +
                        "-fx-padding:0 4;-fx-focus-color:transparent;-fx-faint-focus-color:transparent;"
        );
        dismiss.setOnAction(e -> {
            dismissedNotifIds.add(notifId);   // remember: don't show again this session
            FadeTransition ft = new FadeTransition(Duration.millis(200), card);
            ft.setToValue(0);
            ft.setOnFinished(ev -> {
                notifPanel.getChildren().remove(card);
                if (notifPanel.getChildren().isEmpty()) {
                    notifPanel.setVisible(false);
                    notifPanel.setManaged(false);
                }
            });
            ft.play();
        });

        card.getChildren().addAll(iconBox, textBox, dismiss);
        card.setOpacity(0); card.setTranslateX(-20);
        FadeTransition ft = new FadeTransition(Duration.millis(300), card); ft.setToValue(1);
        TranslateTransition tt = new TranslateTransition(Duration.millis(300), card);
        tt.setToX(0); tt.setInterpolator(Interpolator.EASE_OUT);
        new ParallelTransition(ft, tt).play();
        return card;
    }

}