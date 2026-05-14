package projet.hanouti.module4.controller;
import projet.hanouti.common.utils.SessionManager;

import projet.hanouti.module4.UserSession;
import projet.hanouti.module4.dao.PanierDAO;
import projet.hanouti.module4.dao.PromotionDAO;
import projet.hanouti.module4.dao.WishlistDAO;
import projet.hanouti.module4.model.Panier;
import projet.hanouti.module4.model.Promotion;
import projet.hanouti.module4.model.Wishlist;
import javafx.animation.*;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

public class WishlistController {

    @FXML private AnchorPane rootPane;
    @FXML private HBox       headerBar;
    @FXML private StackPane  logoBoxPlaceholder;
    @FXML private Button     notifBellBtn;
    @FXML private Button     backBtn;
    @FXML private Button     menuBtn;
    @FXML private VBox       wishlistContainer;
    @FXML private Label      labelNbFavoris;
    @FXML private TextField  fieldProduitId;
    @FXML private Button     btnAjouter;
    @FXML private Label      labelNbArticlesText;
    @FXML private Button     cartCountBadge;

    private boolean      isDarkMode = false;
    private WishlistDAO  wishlistDAO;
    private PromotionDAO promotionDAO;
    private PanierDAO    panierDAO;
    private List<Wishlist> wishlistItems;
    private int currentUserId;
    private projet.hanouti.module4.util.DrawerHelper drawerHelper;

    // ── Avatar color gradients — cycles by product ID ─────────────────
    private static final String[] AVATAR_GRADIENTS = {
            "linear-gradient(to bottom right, #10B981, #059669)",   // emerald green
            "linear-gradient(to bottom right, #F97316, #EF4444)",   // orange-red
            "linear-gradient(to bottom right, #8B5CF6, #EC4899)",   // purple-pink
            "linear-gradient(to bottom right, #6366f1, #4f46e5)",   // blue
            "linear-gradient(to bottom right, #F59E0B, #EA580C)",   // amber-orange
    };
    private static final String[] AVATAR_SHADOWS = {
            "rgba(16,185,129,0.40)",
            "rgba(249,115,22,0.40)",
            "rgba(139,92,246,0.40)",
            "rgba(59,130,246,0.40)",
            "rgba(245,158,11,0.40)",
    };


    // Cart SVG path for brand button
    private static final String IC_CART =
            "M6 19m-1 0a1 1 0 1 0 2 0a1 1 0 1 0-2 0" +
                    "M17 19m-1 0a1 1 0 1 0 2 0a1 1 0 1 0-2 0" +
                    "M17 17H6a1 1 0 0 1-1-1V5H3M6 5l1.5 9h9.5l1.5-9H6z";
    // Bookmark SVG (Lucide "Bookmark") — replaces the old red heart
    private static final String IC_STORE =
            "M4 5h16a2 2 0 0 1 2 2v2a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V7a2 2 0 0 1 2-2z"
                    + "M4 11h16v8a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2v-8z"
                    + "M10 21v-5h4v5";
    private static final String IC_BOOKMARK = "M5 5a2 2 0 0 1 2-2h10a2 2 0 0 1 2 2v16l-7-3.5L5 21V5";


    /** SVG icon factory — same as PanierController */
    private SVGPath svgIcon(String d, String strokeColor, double size) {
        SVGPath p = new SVGPath();
        p.setContent(d);
        p.setFill(Color.TRANSPARENT);
        p.setStroke(Color.web(strokeColor));
        p.setStrokeWidth(1.9);
        p.setStrokeLineCap(StrokeLineCap.ROUND);
        p.setStrokeLineJoin(StrokeLineJoin.ROUND);
        double s = size / 24.0; p.setScaleX(s); p.setScaleY(s);
        return p;
    }

    /** Setup topbar icons: hamburger + indigo logo box + bell — same as Panier */
    private void styleBrandTrigger() {
        // Hamburger button
        if (menuBtn != null) {
            menuBtn.setText(null);
            menuBtn.setGraphic(svgIconW("M4 6h16M4 12h16M4 18h16", "#4f46e5", 18));
            menuBtn.setEffect(null);
        }
        // Indigo logo box with cart SVG
        if (logoBoxPlaceholder != null) {
            logoBoxPlaceholder.setStyle(
                    "-fx-background-color:linear-gradient(to bottom right,#4f46e5,#6366f1);"
                            + "-fx-background-radius:13;");
            DropShadow ds = new DropShadow();
            ds.setColor(Color.web("#4f46e5", 0.38)); ds.setRadius(10); ds.setOffsetY(3);
            logoBoxPlaceholder.setEffect(ds);
            logoBoxPlaceholder.getChildren().add(svgIconW(
                    "M6 2L3 6v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2V6l-3-4z M3 6h18 M16 10a4 4 0 0 1-8 0",
                    "#ffffff", 20));
        }
        // Bell icon
        if (notifBellBtn != null) {
            notifBellBtn.setText(null);
            notifBellBtn.setGraphic(svgIconW("M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9 M13.73 21a2 2 0 0 1-3.46 0", "#4f46e5", 16));
        }
    }

    @FXML
    public void initialize() {
        // INTEGRATION FIX: hide topbar and drawer
        try {
            javafx.scene.Node tb = rootPane != null ? rootPane.lookup(".topbar") : null;
            if (tb != null) { tb.setVisible(false); ((javafx.scene.layout.Region)tb).setManaged(false); }
        } catch (Exception ignored) {}
        currentUserId = UserSession.getInstance().getUserId();

        try {
            wishlistDAO  = new WishlistDAO();
            promotionDAO = new PromotionDAO();
            panierDAO    = new PanierDAO();
        } catch (SQLException e) {
            showAlert("Erreur DB", "Impossible de se connecter à la base de données.");
        }

        isDarkMode = SessionManager.getInstance().isDarkMode();
        applyDarkMode();
        styleBrandTrigger();


        if (menuBtn != null) {
            menuBtn.setOnAction(e -> { if (drawerHelper != null) drawerHelper.toggle(); });
        }

        javafx.application.Platform.runLater(() -> {
            // Sync dark mode with parent dashboard (Mootez sets .dark on scene root)
            if (rootPane.getScene() != null && rootPane.getScene().getRoot().getStyleClass().contains("dark")) {
                isDarkMode = true; SessionManager.getInstance().setDarkMode(true);
            } else {
                isDarkMode = false; SessionManager.getInstance().setDarkMode(false);
            }
            applyDarkMode();
            // Live theme listener
            try {
                javafx.scene.Parent sceneRoot = rootPane.getScene().getRoot();
                sceneRoot.getStyleClass().addListener((javafx.collections.ListChangeListener<String>) c -> {
                    boolean nowDark = sceneRoot.getStyleClass().contains("dark");
                    if (nowDark != isDarkMode) {
                        isDarkMode = nowDark;
                        SessionManager.getInstance().setDarkMode(isDarkMode);
                        javafx.application.Platform.runLater(() -> { applyDarkMode(); loadData(); });
                    }
                });
            } catch (Exception ignored) {}
            loadData();
            refreshCartBadge();
            playEntrance(headerBar);
            drawerHelper = new projet.hanouti.module4.util.DrawerHelper(rootPane, isDarkMode, "wishlist");
            drawerHelper.setThemeChangeCallback(() -> {
                isDarkMode = SessionManager.getInstance().isDarkMode();
                applyDarkMode();
            });
        });
    }

    private javafx.scene.shape.SVGPath svgIconW(String d, String strokeColor, double size) {
        javafx.scene.shape.SVGPath p = new javafx.scene.shape.SVGPath();
        p.setContent(d);
        p.setFill(Color.TRANSPARENT);
        p.setStroke(Color.web(strokeColor));
        p.setStrokeWidth(1.9);
        p.setStrokeLineCap(StrokeLineCap.ROUND);
        p.setStrokeLineJoin(StrokeLineJoin.ROUND);
        double sc = size / 24.0; p.setScaleX(sc); p.setScaleY(sc);
        return p;
    }

    private void applyDarkMode() {
        if (isDarkMode) {
            if (!rootPane.getStyleClass().contains("dark")) rootPane.getStyleClass().add("dark");
        } else {
            rootPane.getStyleClass().remove("dark");
        }
        // Rebuild cards so inline styles reflect the new theme
        if (wishlistContainer != null && wishlistItems != null) {
            loadData();
        }
    }

    private void loadData() {
        try {
            wishlistItems = wishlistDAO.getWishlistByUser(currentUserId);
            wishlistContainer.getChildren().clear();

            if (wishlistItems.isEmpty()) {
                wishlistContainer.getChildren().add(createEmptyState());
                if (labelNbFavoris != null) labelNbFavoris.setText("0 produit");
            } else {
                // ── Build responsive 3-column FlowPane grid ────────────
                FlowPane grid = new FlowPane();
                grid.setHgap(16);
                grid.setVgap(16);
                grid.setPrefWrapLength(9999);
                grid.setMaxWidth(Double.MAX_VALUE);

                for (Wishlist w : wishlistItems)
                    grid.getChildren().add(createWishlistCard(w));

                wishlistContainer.getChildren().add(grid);
                staggerCards(grid.getChildren());

                if (labelNbFavoris != null)
                    labelNbFavoris.setText(wishlistItems.size() + " produit" + (wishlistItems.size() > 1 ? "s" : ""));
            }
        } catch (SQLException e) {
            showAlert("Erreur", "Impossible de charger la wishlist.");
        }
    }

    // ══════════════════════════════════════════════════════
    //  WISHLIST CARD — Grid card design matching v0
    // ══════════════════════════════════════════════════════
    private VBox createWishlistCard(Wishlist w) {
        String nom = (w.getNomProduit() != null && !w.getNomProduit().isEmpty())
                ? w.getNomProduit() : "Produit inconnu";

        int colorIdx = Math.abs(w.getProduitId() % AVATAR_GRADIENTS.length);
        boolean dark = isDarkMode;

        // ── Card container ─────────────────────────────────────────────
        VBox card = new VBox(10);
        card.setPrefWidth(270);
        card.setMaxWidth(300);
        card.setMinWidth(220);
        card.setPadding(new Insets(16));
        card.setAlignment(Pos.TOP_CENTER);
        card.setStyle(
                "-fx-background-color: " + (dark ? "#1e1b4b" : "#FFFFFF") + ";" +
                        "-fx-border-color: " + (dark ? "rgba(99,102,241,0.14)" : "rgba(79,70,229,0.14)") + ";" +
                        "-fx-border-width: 1;" +
                        "-fx-border-radius: 16;" +
                        "-fx-background-radius: 16;" +
                        "-fx-effect: dropshadow(gaussian," +
                        (dark ? "rgba(0,0,0,0.35)" : "rgba(79,70,229,0.07)") +
                        ",16,0.08,0,4);" +
                        "-fx-cursor: hand;"
        );

        // ── Top section: avatar + promo badge + heart ─────────────────
        StackPane topSection = new StackPane();
        topSection.setMinHeight(100);
        topSection.setMaxWidth(Double.MAX_VALUE);

        // Avatar — colored gradient square with letter initial
        Label avatar = new Label(nom.substring(0, 1).toUpperCase());
        avatar.setStyle(
                "-fx-background-color: " + AVATAR_GRADIENTS[colorIdx] + ";" +
                        "-fx-background-radius: 18;" +
                        "-fx-min-width: 80; -fx-min-height: 80;" +
                        "-fx-max-width: 80; -fx-max-height: 80;" +
                        "-fx-alignment: center;" +
                        "-fx-text-fill: white;" +
                        "-fx-font-size: 28px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-effect: dropshadow(gaussian," + AVATAR_SHADOWS[colorIdx] + ",12,0.3,0,4);"
        );
        StackPane.setAlignment(avatar, Pos.CENTER);

        // Bookmark button (top-right) — blue, professional
        SVGPath bookmarkIcon = new SVGPath();
        bookmarkIcon.setContent(IC_BOOKMARK);
        bookmarkIcon.setFill(Color.web("#4f46e5"));
        bookmarkIcon.setStroke(Color.web("#4f46e5"));
        bookmarkIcon.setStrokeWidth(1.5);
        bookmarkIcon.setStrokeLineCap(StrokeLineCap.ROUND);
        bookmarkIcon.setStrokeLineJoin(StrokeLineJoin.ROUND);
        double bScale = 15.0 / 24.0;
        bookmarkIcon.setScaleX(bScale); bookmarkIcon.setScaleY(bScale);

        StackPane bookmarkBtn = new StackPane(bookmarkIcon);
        bookmarkBtn.setPrefSize(34, 34); bookmarkBtn.setMinSize(34, 34); bookmarkBtn.setMaxSize(34, 34);
        String bkBase = "-fx-background-color:rgba(79,70,229,0.12);-fx-background-radius:50;-fx-cursor:hand;";
        String bkHov  = "-fx-background-color:#4f46e5;-fx-background-radius:50;-fx-cursor:hand;" +
                "-fx-effect:dropshadow(gaussian,rgba(79,70,229,0.40),10,0.2,0,0);";
        bookmarkBtn.setStyle(bkBase);
        bookmarkBtn.setOnMouseEntered(e -> {
            bookmarkBtn.setStyle(bkHov);
            bookmarkIcon.setFill(Color.WHITE);
            bookmarkIcon.setStroke(Color.WHITE);
        });
        bookmarkBtn.setOnMouseExited(e -> {
            bookmarkBtn.setStyle(bkBase);
            bookmarkIcon.setFill(Color.web("#4f46e5"));
            bookmarkIcon.setStroke(Color.web("#4f46e5"));
        });
        bookmarkBtn.setOnMouseClicked(e -> {
            try {
                wishlistDAO.removeFromWishlist(currentUserId, w.getProduitId());
                loadData();
            } catch (SQLException ex) {
                showAlert("Erreur", "Impossible de supprimer l'article.");
            }
        });
        StackPane.setAlignment(bookmarkBtn, Pos.TOP_RIGHT);

        topSection.getChildren().addAll(avatar, bookmarkBtn);

        // Promo badge (top-left) — added after price box is built
        VBox prixBox = buildPrixBox(w.getProduitId(), w.getPrixProduit());
        boolean hasPromo = !prixBox.getChildren().isEmpty() && prixBox.getChildren().size() > 1;
        if (hasPromo) {
            // Find the badge label from prixBox children
            Node firstChild = prixBox.getChildren().get(0);
            if (firstChild instanceof Label badgeLbl) {
                Label promoBadge = new Label(badgeLbl.getText());
                promoBadge.setStyle(
                        "-fx-background-color: rgba(239,68,68,0.10);" +
                                "-fx-border-color: rgba(239,68,68,0.20);" +
                                "-fx-border-width: 1;" +
                                "-fx-border-radius: 20;" +
                                "-fx-background-radius: 20;" +
                                "-fx-text-fill: #DC2626;" +
                                "-fx-font-size: 10px;" +
                                "-fx-font-weight: bold;" +
                                "-fx-padding: 3 9;"
                );
                StackPane.setAlignment(promoBadge, Pos.TOP_LEFT);
                topSection.getChildren().add(promoBadge);
                // Remove the badge from prixBox so it's not shown twice
                prixBox.getChildren().remove(badgeLbl);
            }
        }

        // ── Product info ──────────────────────────────────────────────
        Label nomLabel = new Label(nom);
        nomLabel.setStyle(
                "-fx-font-size: 14px; -fx-font-weight: bold;" +
                        "-fx-text-fill: " + (dark ? "#e0e7ff" : "#1e1b4b") + ";" +
                        "-fx-wrap-text: true; -fx-text-alignment: center;"
        );
        nomLabel.setMaxWidth(Double.MAX_VALUE);
        nomLabel.setAlignment(Pos.CENTER);
        nomLabel.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
        nomLabel.setWrapText(true);

        String prodRef = "REF-" +
                nom.replaceAll("[^A-Za-z]", "")
                        .substring(0, Math.min(3, nom.replaceAll("[^A-Za-z]", "").length()))
                        .toUpperCase()
                + "-" + String.format("%03d", w.getProduitId());
        Label refLabel = new Label(prodRef);
        refLabel.setStyle(
                "-fx-font-size: 11px;" +
                        "-fx-text-fill: " + (dark ? "#475569" : "#64748B") + ";"
        );
        refLabel.setAlignment(Pos.CENTER);
        refLabel.setMaxWidth(Double.MAX_VALUE);

        // ── Price box centered ─────────────────────────────────────────
        prixBox.setAlignment(Pos.CENTER);
        prixBox.setMaxWidth(Double.MAX_VALUE);
        for (Node n : prixBox.getChildren()) {
            if (n instanceof Label lbl) {
                lbl.setAlignment(Pos.CENTER);
                lbl.setMaxWidth(Double.MAX_VALUE);
            }
        }

        // Separator
        Region sep = new Region();
        sep.setMinHeight(1);
        sep.setMaxWidth(Double.MAX_VALUE);
        sep.setStyle("-fx-background-color: " + (dark ? "rgba(99,102,241,0.08)" : "rgba(79,70,229,0.08)") + ";");

        // ── Action buttons row ─────────────────────────────────────────
        HBox actionRow = new HBox(8);
        actionRow.setAlignment(Pos.CENTER);
        actionRow.setMaxWidth(Double.MAX_VALUE);

        Button btnPanier = new Button("\uD83D\uDED2  Ajouter au panier");
        HBox.setHgrow(btnPanier, Priority.ALWAYS);
        btnPanier.setMaxWidth(Double.MAX_VALUE);
        btnPanier.setStyle(
                "-fx-background-color: #4f46e5;" +
                        "-fx-text-fill: white;" +
                        "-fx-font-size: 12px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-background-radius: 10;" +
                        "-fx-border-width: 0;" +
                        "-fx-padding: 9 0;" +
                        "-fx-cursor: hand;" +
                        "-fx-effect: dropshadow(gaussian,rgba(79,70,229,0.35),10,0.2,0,3);"
        );
        btnPanier.setOnMouseEntered(e -> btnPanier.setStyle(
                "-fx-background-color: #4338ca;" +
                        "-fx-text-fill: white;" +
                        "-fx-font-size: 12px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-background-radius: 10;" +
                        "-fx-border-width: 0;" +
                        "-fx-padding: 9 0;" +
                        "-fx-cursor: hand;" +
                        "-fx-effect: dropshadow(gaussian,rgba(79,70,229,0.50),14,0.25,0,4);"
        ));
        btnPanier.setOnMouseExited(e -> btnPanier.setStyle(
                "-fx-background-color: #4f46e5;" +
                        "-fx-text-fill: white;" +
                        "-fx-font-size: 12px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-background-radius: 10;" +
                        "-fx-border-width: 0;" +
                        "-fx-padding: 9 0;" +
                        "-fx-cursor: hand;" +
                        "-fx-effect: dropshadow(gaussian,rgba(79,70,229,0.35),10,0.2,0,3);"
        ));
        btnPanier.setOnAction(e -> {
            try {
                Panier item = new Panier(currentUserId, w.getProduitId(), 1, w.getPrixProduit());
                panierDAO.addToCart(item);
                bounceNode(btnPanier);
                showAlert("✓ Panier", "\"" + nom + "\" ajouté au panier !");
            } catch (SQLException ex) {
                showAlert("Erreur", "Impossible d'ajouter au panier : " + ex.getMessage());
            }
        });

        // Delete button — SVG trash icon, clearly visible red bordered button
        javafx.scene.shape.SVGPath trashIcon = new javafx.scene.shape.SVGPath();
        trashIcon.setContent("M3 6h18M8 6V4h8v2M19 6l-1 14H6L5 6M10 11v6M14 11v6");
        trashIcon.setFill(Color.TRANSPARENT);
        trashIcon.setStroke(Color.web("#DC2626"));
        trashIcon.setStrokeWidth(1.8);
        trashIcon.setStrokeLineCap(StrokeLineCap.ROUND);
        trashIcon.setStrokeLineJoin(StrokeLineJoin.ROUND);
        double ts = 16.0 / 24.0;
        trashIcon.setScaleX(ts); trashIcon.setScaleY(ts);

        javafx.scene.layout.StackPane trashBox = new javafx.scene.layout.StackPane(trashIcon);
        trashBox.setPrefSize(38, 38); trashBox.setMinSize(38, 38); trashBox.setMaxSize(38, 38);
        String trashBase = "-fx-background-color:rgba(239,68,68,0.08);-fx-border-color:rgba(239,68,68,0.55);" +
                "-fx-border-width:1.5;-fx-border-radius:10;-fx-background-radius:10;-fx-cursor:hand;";
        String trashHov  = "-fx-background-color:#EF4444;-fx-border-color:#EF4444;" +
                "-fx-border-width:1.5;-fx-border-radius:10;-fx-background-radius:10;-fx-cursor:hand;" +
                "-fx-effect:dropshadow(gaussian,rgba(239,68,68,0.50),10,0.2,0,2);";
        trashBox.setStyle(trashBase);
        trashBox.setOnMouseEntered(e -> {
            trashBox.setStyle(trashHov);
            trashIcon.setStroke(Color.WHITE);
        });
        trashBox.setOnMouseExited(e -> {
            trashBox.setStyle(trashBase);
            trashIcon.setStroke(Color.web("#DC2626"));
        });

        javafx.scene.layout.StackPane btnRetirer = trashBox;
        trashBox.setOnMouseClicked(e -> {
            try {
                wishlistDAO.removeFromWishlist(currentUserId, w.getProduitId());
                loadData();
            } catch (SQLException ex) {
                showAlert("Erreur", "Impossible de supprimer l'article.");
            }
        });

        actionRow.getChildren().addAll(btnPanier, btnRetirer);

        // ── Assemble card ──────────────────────────────────────────────
        card.getChildren().addAll(topSection, nomLabel, refLabel, prixBox, sep, actionRow);

        // Hover effect on card
        card.setOnMouseEntered(e -> card.setStyle(card.getStyle()
                .replace("-fx-border-color: " + (dark ? "rgba(99,102,241,0.14)" : "rgba(79,70,229,0.14)"),
                        "-fx-border-color: " + (dark ? "rgba(96,165,250,0.30)" : "rgba(79,70,229,0.30)"))));
        card.setOnMouseExited(e -> card.setStyle(card.getStyle()
                .replace("-fx-border-color: " + (dark ? "rgba(96,165,250,0.30)" : "rgba(79,70,229,0.30)"),
                        "-fx-border-color: " + (dark ? "rgba(99,102,241,0.14)" : "rgba(79,70,229,0.14)"))));

        return card;
    }

    // ── Prix box — same logic as PanierController.buildPrixBox ─────────
    private VBox buildPrixBox(int produitId, double prixOriginal) {
        VBox box = new VBox(4);
        box.setAlignment(Pos.CENTER_LEFT);
        box.setPrefWidth(160);

        Promotion promo = null;
        try { promo = promotionDAO.getActivePromoForProduct(produitId); }
        catch (SQLException ignored) {}

        if (promo != null) {
            double prixReduit = prixOriginal - (prixOriginal * promo.getPourcentage() / 100.0);

            Label badge = new Label(String.format("-%d%%", (int) promo.getPourcentage()));
            badge.getStyleClass().add("promo-badge");

            Label prixBarre = new Label(String.format("%.2f TND", prixOriginal));
            prixBarre.getStyleClass().add("prix-barre");

            Label prixLabel = new Label(String.format("%.2f TND", prixReduit));
            prixLabel.getStyleClass().add("m4-price");

            box.getChildren().addAll(badge, prixBarre, prixLabel);

            if (promo.getDateFin() != null) {
                Label countdownLabel = new Label();
                countdownLabel.getStyleClass().add("countdown-label");
                box.getChildren().add(countdownLabel);
                final LocalDateTime dateFin = promo.getDateFin();
                Timeline countdown = new Timeline(new KeyFrame(Duration.seconds(1), ev -> {
                    long secondsLeft = ChronoUnit.SECONDS.between(LocalDateTime.now(), dateFin);
                    if (secondsLeft <= 0) {
                        countdownLabel.setText("Promo expirée");
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
            Label prixLabel = new Label(String.format("%.2f TND", prixOriginal));
            prixLabel.getStyleClass().add("m4-price");
            box.getChildren().add(prixLabel);
        }
        return box;
    }

    // ── Empty state ────────────────────────────────────────────────────
    private VBox createEmptyState() {
        VBox empty = new VBox(18);
        empty.setAlignment(Pos.CENTER);
        empty.setStyle("-fx-padding: 80 0;");

        Label icon = new Label("\uD83D\uDD16");
        icon.setStyle("-fx-font-size: 64px; -fx-text-fill: rgba(79,70,229,0.25);");

        Label title = new Label("Vos favoris sont vides");
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: -t1;");

        Label subtitle = new Label("Ajoute des articles à tes favoris pour les retrouver ici");
        subtitle.setStyle("-fx-font-size: 13px; -fx-text-fill: -t3;");
        subtitle.setWrapText(true);
        subtitle.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);

        Button btnRetour = new Button("← Retour au Panier");
        btnRetour.getStyleClass().add("btn-primary");
        btnRetour.setPrefWidth(280);
        btnRetour.setOnAction(e -> handleRetourPanier());

        empty.getChildren().addAll(icon, title, subtitle, btnRetour);
        return empty;
    }

    @FXML
    private void handleViderFavoris() {
        showCustomConfirm(
                "Vider les favoris",
                "Cette action supprimera définitivement tous vos favoris.\nCette action est irréversible.",
                "Vider", "#ef4444",
                () -> {
                    try {
                        wishlistDAO.clearWishlist(currentUserId);
                        loadData();
                    } catch (SQLException ex) {
                        showAlert("Erreur", "Impossible de vider les favoris : " + ex.getMessage());
                    }
                }
        );
    }

    private void showCustomConfirm(String title, String message,
                                   String confirmLabel, String confirmColor, Runnable onConfirm) {
        boolean dark    = isDarkMode;
        String bgCard   = dark ? "#1a1a2e" : "#FFFFFF";
        String t1       = dark ? "#f8fafc"  : "#0f172a";
        String t3       = dark ? "#94a3b8"  : "#64748b";
        String divClr   = dark ? "rgba(255,255,255,0.08)" : "#f1f5f9";
        String cancelBg = dark ? "#252540"  : "#f8fafc";
        String cancelTxt= dark ? "#cbd5e1"  : "#475569";
        String btnReset = "-fx-background-insets:0;-fx-shadow-highlight-color:transparent;" +
                "-fx-outer-border:transparent;-fx-inner-border:transparent;-fx-body-color:transparent;" +
                "-fx-focus-color:transparent;-fx-faint-focus-color:transparent;";

        StackPane overlay = new StackPane();
        overlay.setStyle("-fx-background-color:rgba(0,0,0,0.55);");
        AnchorPane.setTopAnchor(overlay,0.0);    AnchorPane.setBottomAnchor(overlay,0.0);
        AnchorPane.setLeftAnchor(overlay,0.0);   AnchorPane.setRightAnchor(overlay,0.0);

        VBox card = new VBox(20);
        card.setMaxWidth(420); card.setMaxHeight(Region.USE_PREF_SIZE);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setStyle(
                "-fx-background-color:" + bgCard + ";" +
                        "-fx-background-radius:16;" +
                        "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.35),60,0,0,20);" +
                        "-fx-padding:32 36 28 36;"
        );
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
        Button cancelBtn = new Button("Annuler");
        cancelBtn.setStyle(btnReset +
                "-fx-background-color:" + cancelBg + ";-fx-background-radius:10;" +
                "-fx-text-fill:" + cancelTxt + ";-fx-font-size:13px;-fx-font-weight:600;" +
                "-fx-padding:10 24;-fx-cursor:hand;-fx-border-width:0;");
        Button confirmBtn = new Button(confirmLabel);
        confirmBtn.setStyle(btnReset +
                "-fx-background-color:" + confirmColor + ";-fx-background-radius:10;" +
                "-fx-text-fill:white;-fx-font-size:13px;-fx-font-weight:bold;" +
                "-fx-padding:10 28;-fx-cursor:hand;-fx-border-width:0;");
        btnRow.getChildren().addAll(cancelBtn, confirmBtn);

        card.getChildren().addAll(titleLbl, msgLbl, div, btnRow);
        overlay.getChildren().add(card);
        rootPane.getChildren().add(overlay);

        Runnable dismiss = () -> {
            javafx.animation.FadeTransition fo =
                    new javafx.animation.FadeTransition(Duration.millis(150), overlay);
            fo.setToValue(0); fo.setOnFinished(ev -> rootPane.getChildren().remove(overlay)); fo.play();
        };
        cancelBtn.setOnAction(e -> dismiss.run());
        confirmBtn.setOnAction(e -> { dismiss.run(); onConfirm.run(); });
        overlay.setOnMouseClicked(e -> { if (e.getTarget() == overlay) dismiss.run(); });

        javafx.animation.FadeTransition fi =
                new javafx.animation.FadeTransition(Duration.millis(180), card); fi.setToValue(1);
        javafx.animation.ScaleTransition si =
                new javafx.animation.ScaleTransition(Duration.millis(200), card);
        si.setToX(1); si.setToY(1); si.setInterpolator(javafx.animation.Interpolator.EASE_OUT);
        new javafx.animation.ParallelTransition(fi, si).play();
    }

    @FXML
    private void handleAjouterArticle() {
        String idText = fieldProduitId != null ? fieldProduitId.getText().trim() : "";
        if (idText.isEmpty()) {
            showAlert("Champ manquant", "Veuillez saisir l'ID du produit.");
            return;
        }
        int produitId;
        try {
            produitId = Integer.parseInt(idText);
        } catch (NumberFormatException ex) {
            showAlert("Valeur invalide", "L'ID produit doit être un nombre entier.");
            return;
        }
        try {
            if (wishlistDAO.isInWishlist(currentUserId, produitId)) {
                showAlert("Déjà en favoris", "Ce produit est déjà dans votre wishlist !");
                return;
            }
            wishlistDAO.addToWishlist(currentUserId, produitId);
            if (fieldProduitId != null) fieldProduitId.clear();
            if (btnAjouter     != null) bounceNode(btnAjouter);
            loadData();
        } catch (SQLException ex) {
            showAlert("Erreur", "Impossible d'ajouter l'article : " + ex.getMessage());
        }
    }

    public boolean toggleFavori(int produitId) throws SQLException {
        boolean added = wishlistDAO.toggleWishlist(currentUserId, produitId);
        loadData();
        return added;
    }

    @FXML
    private void handleRetourPanier() {
        try {
            if (backBtn != null) bounceNode(backBtn);
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/projet/hanouti/module4/Panier.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) wishlistContainer.getScene().getWindow();
            stage.setTitle("7anouti-E \u2014 Mon Panier");
            stage.setScene(new Scene(root, 1250, 700));
        } catch (IOException e) {
            showAlert("Erreur navigation", "Impossible de retourner au panier : " + e.getMessage());
        }
    }

    @FXML
    private void handleOpenHistorique() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/projet/hanouti/module4/HistoriquePaiement.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) wishlistContainer.getScene().getWindow();
            stage.setTitle("7anouti-E \u2014 Historique");
            stage.setScene(new Scene(root, 1250, 700));
        } catch (IOException e) { /* ignore */ }
    }

    // ── Helpers ────────────────────────────────────────────────────────
    private void refreshCartBadge() {
        try {
            java.util.List<projet.hanouti.module4.model.Panier> cartItems =
                    panierDAO.getCartItems(currentUserId);
            int total = cartItems.stream().mapToInt(projet.hanouti.module4.model.Panier::getQuantite).sum();
            double cartTotal = cartItems.stream()
                    .mapToDouble(p -> p.getPrixUnitaire() * p.getQuantite()).sum();
            // label stays as "Panier" (static) — only count badge updates
            if (cartCountBadge != null)
                cartCountBadge.setText(String.valueOf(total));
        } catch (java.sql.SQLException ignored) {}
    }

    private void showAlert(String title, String message) {
        boolean dark    = isDarkMode;
        String bgCard   = dark ? "#1a1a2e" : "#FFFFFF";
        String t1       = dark ? "#f8fafc"  : "#0f172a";
        String t3       = dark ? "#94a3b8"  : "#64748b";
        String divClr   = dark ? "rgba(255,255,255,0.08)" : "#f1f5f9";
        boolean isSuccess = title.contains("✓") || title.contains("Panier");
        String accent = isSuccess ? "#16a34a" : "#4f46e5";
        String btnReset = "-fx-background-insets:0;-fx-shadow-highlight-color:transparent;" +
                "-fx-outer-border:transparent;-fx-inner-border:transparent;-fx-body-color:transparent;" +
                "-fx-focus-color:transparent;-fx-faint-focus-color:transparent;";

        StackPane overlay = new StackPane();
        overlay.setStyle("-fx-background-color:rgba(0,0,0,0.55);");
        javafx.scene.layout.AnchorPane.setTopAnchor(overlay,0.0);
        javafx.scene.layout.AnchorPane.setBottomAnchor(overlay,0.0);
        javafx.scene.layout.AnchorPane.setLeftAnchor(overlay,0.0);
        javafx.scene.layout.AnchorPane.setRightAnchor(overlay,0.0);

        VBox card = new VBox(20);
        card.setMaxWidth(400); card.setMaxHeight(Region.USE_PREF_SIZE);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setStyle(
                "-fx-background-color:" + bgCard + ";" +
                        "-fx-background-radius:16;" +
                        "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.35),60,0,0,20);" +
                        "-fx-padding:32 36 28 36;"
        );
        card.setScaleX(0.88); card.setScaleY(0.88); card.setOpacity(0);
        StackPane.setAlignment(card, Pos.CENTER);

        String cleanTitle = title.replaceAll("^[^\\p{L}\\p{N}]+", "").trim();
        Label titleLbl = new Label(cleanTitle);
        titleLbl.setStyle("-fx-font-size:17px;-fx-font-weight:bold;-fx-text-fill:" + t1 + ";-fx-wrap-text:true;");
        titleLbl.setWrapText(true);

        Label msgLbl = new Label(message);
        msgLbl.setStyle("-fx-font-size:13px;-fx-text-fill:" + t3 + ";-fx-wrap-text:true;-fx-line-spacing:4;");
        msgLbl.setWrapText(true); msgLbl.setMaxWidth(340);

        Region div = new Region(); div.setPrefHeight(1); div.setMaxHeight(1);
        div.setStyle("-fx-background-color:" + divClr + ";");

        Button okBtn = new Button("OK");
        okBtn.setMaxWidth(Double.MAX_VALUE);
        okBtn.setStyle(btnReset +
                "-fx-background-color:" + accent + ";-fx-background-radius:10;" +
                "-fx-text-fill:white;-fx-font-size:13px;-fx-font-weight:bold;" +
                "-fx-padding:11 0;-fx-cursor:hand;-fx-border-width:0;");

        card.getChildren().addAll(titleLbl, msgLbl, div, okBtn);
        overlay.getChildren().add(card);
        rootPane.getChildren().add(overlay);

        Runnable dismiss = () -> {
            javafx.animation.FadeTransition fo =
                    new javafx.animation.FadeTransition(Duration.millis(150), overlay);
            fo.setToValue(0); fo.setOnFinished(ev -> rootPane.getChildren().remove(overlay)); fo.play();
        };
        okBtn.setOnAction(e -> dismiss.run());
        overlay.setOnMouseClicked(e -> { if (e.getTarget() == overlay) dismiss.run(); });

        javafx.animation.FadeTransition fi =
                new javafx.animation.FadeTransition(Duration.millis(180), card); fi.setToValue(1);
        javafx.animation.ScaleTransition si =
                new javafx.animation.ScaleTransition(Duration.millis(200), card);
        si.setToX(1); si.setToY(1); si.setInterpolator(javafx.animation.Interpolator.EASE_OUT);
        new javafx.animation.ParallelTransition(fi, si).play();
    }

    private void playEntrance(HBox header) {
        if (header == null) return;
        header.setTranslateY(-60);
        header.setOpacity(0);
        TranslateTransition tt = new TranslateTransition(Duration.millis(450), header);
        tt.setToY(0);
        FadeTransition ft = new FadeTransition(Duration.millis(450), header);
        ft.setToValue(1);
        ft.setInterpolator(Interpolator.EASE_OUT);
        new ParallelTransition(tt, ft).play();
    }

    private void staggerCards(javafx.collections.ObservableList<Node> nodes) {
        for (int i = 0; i < nodes.size(); i++) {
            Node card = nodes.get(i);
            card.setOpacity(0);
            card.setTranslateY(24);
            FadeTransition f = new FadeTransition(Duration.millis(380), card);
            f.setFromValue(0); f.setToValue(1);
            TranslateTransition t = new TranslateTransition(Duration.millis(380), card);
            t.setFromY(24); t.setToY(0); t.setInterpolator(Interpolator.EASE_OUT);
            ParallelTransition pt = new ParallelTransition(f, t);
            pt.setDelay(Duration.millis(120 + i * 60));
            pt.play();
        }
    }

    private void bounceNode(Node n) {
        if (n == null) return;
        ScaleTransition sc = new ScaleTransition(Duration.millis(120), n);
        sc.setFromX(0.90); sc.setFromY(0.90); sc.setToX(1.0); sc.setToY(1.0);
        sc.setInterpolator(Interpolator.EASE_OUT);
        sc.play();
    }
}