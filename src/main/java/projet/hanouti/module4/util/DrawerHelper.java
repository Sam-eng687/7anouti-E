package com.hanouti.hanoutiem4.util;

import com.hanouti.hanoutiem4.UserSession;
import com.hanouti.hanoutiem4.dao.PanierDAO;
import javafx.animation.*;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.*;
import javafx.scene.paint.*;
import javafx.scene.shape.SVGPath;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;
import java.sql.SQLException;

/**
 * DrawerHelper — overlay slide-in sidebar.
 * Visual design is kept IDENTICAL to PanierController's inline sidebar.
 */
public class DrawerHelper {

    private final AnchorPane rootPane;
    private boolean          isDark;
    private final String     activePage;
    private Runnable         themeChangeCallback; // notified when theme toggles from drawer footer

    private AnchorPane overlayPane;
    private VBox       drawerPane;
    private boolean    open = false;

    // ── Icon paths (same as PanierController) ─────────────────────────────
    private static final String IC_CART =
            "M6 19m-1 0a1 1 0 1 0 2 0a1 1 0 1 0-2 0" +
                    "M17 19m-1 0a1 1 0 1 0 2 0a1 1 0 1 0-2 0" +
                    "M17 17H6a1 1 0 0 1-1-1V5H3M6 5l1.5 9h9.5l1.5-9H6z";
    private static final String IC_HOME =
            "M3 12L5 10M5 10L12 3L19 10M5 10V20A1 1 0 0 0 6 21H9M19 10L21 12" +
                    "M19 10V20A1 1 0 0 0 18 21H15M9 21A1 1 0 0 0 10 22H14A1 1 0 0 0 15 21M9 21V16" +
                    "A1 1 0 0 1 10 15H14A1 1 0 0 1 15 16V21";
    private static final String IC_PRODUCTS =
            "M5 5m0 2a2 2 0 0 1 2-2h2a2 2 0 0 1 2 2v4a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2z" +
                    "M13 5m0 2a2 2 0 0 1 2-2h2a2 2 0 0 1 2 2v2a2 2 0 0 1-2 2h-2a2 2 0 0 1-2-2z" +
                    "M5 15m0 2a2 2 0 0 1 2-2h2a2 2 0 0 1 2 2v2a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2z" +
                    "M13 13m0 2a2 2 0 0 1 2-2h2a2 2 0 0 1 2 2v4a2 2 0 0 1-2 2h-2a2 2 0 0 1-2-2z";
    private static final String IC_HEART =
            "M19.5 12.572L12 20L4.5 12.572a5 5 0 1 1 7.5-6.566a5 5 0 1 1 7.5 6.572";
    private static final String IC_BOOKMARK =
            "M5 5a2 2 0 0 1 2-2h10a2 2 0 0 1 2 2v16l-7-3.5L5 21V5";
    private static final String IC_HISTORY =
            "M12 22c5.523 0 10-4.477 10-10S17.523 2 12 2 2 6.477 2 12s4.477 10 10 10z M12 6v6l4 2";
    private static final String IC_SETTINGS =
            "M12 15a3 3 0 1 0 0-6 3 3 0 0 0 0 6z" +
                    "M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 0 1 0 2.83 2 2 0 0 1-2.83 0l-.06-.06" +
                    "a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-4 0v-.09" +
                    "A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 0 1-2.83-2.83l.06-.06" +
                    "A1.65 1.65 0 0 0 4.68 15a1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1 0-4h.09" +
                    "A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 0 1 2.83-2.83l.06.06" +
                    "A1.65 1.65 0 0 0 9 4.68a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 4 0v.09" +
                    "a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 0 1 2.83 2.83l-.06.06" +
                    "A1.65 1.65 0 0 0 19.4 9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 0 4h-.09a1.65 1.65 0 0 0-1.51 1z";
    private static final String IC_POWER = "M12 5v4 M17.66 7.34A8 8 0 1 1 6.34 7.34";

    public DrawerHelper(AnchorPane rootPane, boolean isDark, String activePage) {
        this.rootPane   = rootPane;
        this.isDark     = isDark;
        this.activePage = activePage;
        build();
    }

    public void toggle() { if (open) close(); else openDrawer(); }

    public void setThemeChangeCallback(Runnable callback) {
        this.themeChangeCallback = callback;
    }

    public void updateTheme(boolean dark) {
        this.isDark = dark;
        rebuildContent();
        applyShell();
    }

    // ── Build overlay + pane ──────────────────────────────────────────────
    private void build() {
        overlayPane = new AnchorPane();
        overlayPane.setStyle("-fx-background-color:rgba(0,0,0,0.25);");
        overlayPane.setVisible(false);
        overlayPane.setManaged(false);
        AnchorPane.setTopAnchor(overlayPane,    0.0);
        AnchorPane.setBottomAnchor(overlayPane, 0.0);
        AnchorPane.setLeftAnchor(overlayPane,   0.0);
        AnchorPane.setRightAnchor(overlayPane,  0.0);
        overlayPane.setOnMouseClicked(e -> close());

        drawerPane = new VBox(0);
        drawerPane.setPrefWidth(230);
        drawerPane.setMaxWidth(230);
        drawerPane.setTranslateX(-250);
        drawerPane.setOnMouseClicked(javafx.event.Event::consume);
        AnchorPane.setTopAnchor(drawerPane,    0.0);
        AnchorPane.setBottomAnchor(drawerPane, 0.0);
        AnchorPane.setLeftAnchor(drawerPane,   0.0);

        applyShell();
        rebuildContent();
        overlayPane.getChildren().add(drawerPane);
        rootPane.getChildren().add(overlayPane);
    }

    private void applyShell() {
        drawerPane.setStyle(isDark
                ? "-fx-background-color:#0f1117;-fx-border-color:rgba(255,255,255,0.07);-fx-border-width:0 1 0 0;"
                : "-fx-background-color:#ffffff;-fx-border-color:#e8eaf0;-fx-border-width:0 1 0 0;"
        );
    }

    private void rebuildContent() {
        drawerPane.getChildren().clear();
        drawerPane.getChildren().add(buildBrand());
        VBox nav = buildNav();
        VBox.setVgrow(nav, Priority.ALWAYS);
        drawerPane.getChildren().add(nav);
        drawerPane.getChildren().add(buildFooter());
    }

    // ── SVG icon — identical signature to PanierController.svgIcon ───────
    private SVGPath svgIcon(String d, String strokeColor, double size) {
        SVGPath p = new SVGPath();
        p.setContent(d);
        p.setFill(Color.TRANSPARENT);
        p.setStroke(Color.web(strokeColor));
        p.setStrokeWidth(1.9);
        p.setStrokeLineCap(StrokeLineCap.ROUND);
        p.setStrokeLineJoin(StrokeLineJoin.ROUND);
        double s = size / 24.0;
        p.setScaleX(s); p.setScaleY(s);
        return p;
    }

    // ── Brand — identical to PanierController.buildSidebarBrand ──────────
    private HBox buildBrand() {
        HBox brand = new HBox(12);
        brand.setAlignment(Pos.CENTER_LEFT);
        brand.setPadding(new Insets(18, 16, 14, 16));
        brand.setStyle("-fx-cursor:hand;");

        StackPane iconBox = new StackPane();
        iconBox.setPrefSize(42, 42); iconBox.setMinSize(42, 42); iconBox.setMaxSize(42, 42);
        iconBox.setStyle("-fx-background-color:linear-gradient(to bottom right,#4f46e5,#6366f1);-fx-background-radius:13;");
        DropShadow s = new DropShadow();
        s.setColor(Color.web("#4f46e5", 0.35)); s.setRadius(10); s.setOffsetY(3);
        iconBox.setEffect(s);
        iconBox.getChildren().add(svgIcon(IC_CART, "#ffffff", 20));

        VBox text = new VBox(2);
        Label name = new Label("7anouti-E");
        name.setStyle("-fx-font-size:15px;-fx-font-weight:bold;-fx-text-fill:" + (isDark ? "#e0e7ff" : "#1e1b4b") + ";");
        Label tagline = new Label("E-Commerce Tunisia");
        tagline.setStyle("-fx-font-size:11px;-fx-text-fill:" + (isDark ? "#818cf8" : "#6366f1") + ";");
        text.getChildren().addAll(name, tagline);
        brand.getChildren().addAll(iconBox, text);
        brand.setOnMouseClicked(e -> close());
        return brand;
    }

    // ── Nav — identical structure to PanierController.buildSidebarNav ────
    private VBox buildNav() {
        VBox nav = new VBox(2);
        nav.setPadding(new Insets(14, 10, 10, 10));

        nav.getChildren().add(sectionLabel("NAVIGATION", isDark));
        nav.getChildren().add(sidebarRow(IC_HOME,     "Accueil",  false, isDark, null));
        nav.getChildren().add(sidebarRow(IC_PRODUCTS, "Produits", false, isDark, null));

        nav.getChildren().add(sectionLabel("MON COMPTE", isDark));
        nav.getChildren().add(buildCartRow(isDark));
        nav.getChildren().add(buildFavorisRow(isDark));
        nav.getChildren().add(sidebarRow(IC_HISTORY, "Historique", "historique".equals(activePage), isDark,
                e -> { close(); navigate("HistoriquePaiement.fxml", "7anouti-E \u2014 Historique"); }));

        nav.getChildren().add(sectionLabel("PARAM\u00c8TRES", isDark));
        nav.getChildren().add(sidebarRow(IC_SETTINGS, "Param\u00e8tres", false, isDark, null));

        Region spacer = new Region(); VBox.setVgrow(spacer, Priority.ALWAYS);
        nav.getChildren().add(spacer);
        return nav;
    }

    private Label sectionLabel(String text, boolean dark) {
        Label lbl = new Label(text);
        lbl.setStyle("-fx-font-size:9px;-fx-font-weight:bold;-fx-letter-spacing:0.08em;" +
                "-fx-text-fill:" + (dark ? "#4b5563" : "#9ca3af") + ";");
        VBox.setMargin(lbl, new Insets(14, 0, 6, 6));
        return lbl;
    }

    // Cart row — active pill + badge
    private HBox buildCartRow(boolean dark) {
        boolean active = "panier".equals(activePage);
        String pillBg  = dark ? "rgba(99,102,241,0.16)" : "#eef2ff";
        String iconBg  = dark ? "rgba(99,102,241,0.26)" : "#e0e7ff";
        String iconCol = dark ? "#a5b4fc" : "#4f46e5";
        String textCol = dark ? "#e0e7ff" : "#3730a3";

        StackPane box = new StackPane();
        box.setPrefSize(36,36); box.setMinSize(36,36); box.setMaxSize(36,36);
        box.setStyle("-fx-background-color:" + iconBg + ";-fx-background-radius:10;");
        box.getChildren().add(svgIcon(IC_CART, iconCol, 17));
        if (active) {
            DropShadow glow = new DropShadow();
            glow.setColor(Color.web("#6366f1", 0.35)); glow.setRadius(9); glow.setOffsetY(2);
            box.setEffect(glow);
        }

        Label lbl = new Label("Mon Panier");
        lbl.setStyle("-fx-font-size:13px;-fx-font-weight:" + (active ? "bold" : "normal") +
                ";-fx-text-fill:" + textCol + ";");
        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);

        // Cart count badge
        int count = getCartCount();
        Label badge = new Label(String.valueOf(count));
        badge.setStyle("-fx-background-color:" + (dark ? "#4f46e5" : "#2563EB") + ";" +
                "-fx-text-fill:white;-fx-font-size:10px;-fx-font-weight:bold;" +
                "-fx-background-radius:20;-fx-padding:2 6;" +
                "-fx-min-width:18;-fx-min-height:18;-fx-alignment:center;");

        HBox row = new HBox(12); row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(9,14,9,14)); row.setMaxWidth(Double.MAX_VALUE);
        row.setStyle("-fx-background-color:" + pillBg + ";-fx-background-radius:12;-fx-cursor:hand;");
        row.setOnMouseClicked(e -> { close(); navigate("Panier.fxml", "7anouti-E \u2014 Mon Panier"); });
        row.getChildren().addAll(box, lbl, sp, badge);
        return row;
    }

    // Favoris row — BLUE bookmark (NOT red heart), matching PanierController
    private HBox buildFavorisRow(boolean dark) {
        boolean active = "wishlist".equals(activePage);
        String pillBg  = active
                ? (dark ? "rgba(74,144,226,0.18)" : "rgba(37,99,235,0.08)")
                : (dark ? "rgba(74,144,226,0.10)" : "rgba(37,99,235,0.04)");
        String iconBg  = dark ? "rgba(74,144,226,0.20)" : "rgba(37,99,235,0.10)";
        String iconCol = dark ? "#60A5FA" : "#2563EB";
        String textCol = dark ? "#93C5FD" : "#2563EB";
        String hovBg   = dark ? "rgba(74,144,226,0.24)" : "rgba(37,99,235,0.12)";

        StackPane box = new StackPane();
        box.setPrefSize(36,36); box.setMinSize(36,36); box.setMaxSize(36,36);
        box.setStyle("-fx-background-color:" + iconBg + ";-fx-background-radius:10;");
        box.getChildren().add(svgIcon(IC_BOOKMARK, iconCol, 17));
        if (active) {
            DropShadow glow = new DropShadow();
            glow.setColor(Color.web("#2563EB", 0.30)); glow.setRadius(8); glow.setOffsetY(2);
            box.setEffect(glow);
        }

        Label lbl = new Label("Favoris");
        lbl.setStyle("-fx-font-size:13px;-fx-font-weight:" + (active ? "bold" : "normal") +
                ";-fx-text-fill:" + textCol + ";");

        HBox row = new HBox(12); row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(9,14,9,14)); row.setMaxWidth(Double.MAX_VALUE);
        String base = "-fx-background-color:" + pillBg + ";-fx-background-radius:12;-fx-cursor:hand;";
        String hov  = "-fx-background-color:" + hovBg  + ";-fx-background-radius:12;-fx-cursor:hand;";
        row.setStyle(base);
        row.setOnMouseEntered(e -> { row.setStyle(hov); ScaleTransition st = new ScaleTransition(Duration.millis(120), box); st.setToX(1.10); st.setToY(1.10); st.play(); });
        row.setOnMouseExited(e  -> { row.setStyle(base); ScaleTransition st = new ScaleTransition(Duration.millis(120), box); st.setToX(1.0); st.setToY(1.0); st.play(); });
        row.setOnMouseClicked(e -> { close(); navigate("wishlist.fxml", "7anouti-E \u2014 Mes Favoris"); });
        row.getChildren().addAll(box, lbl);
        return row;
    }

    // Generic sidebar row — same as PanierController.sidebarRow
    private HBox sidebarRow(String svgPath, String label, boolean active, boolean dark,
                            javafx.event.EventHandler<javafx.scene.input.MouseEvent> action) {
        String pillBg  = dark ? "rgba(99,102,241,0.16)" : "#eef2ff";
        String iconBg  = dark ? "rgba(99,102,241,0.26)" : "#e0e7ff";
        String iconCol = dark ? "#a5b4fc" : "#4f46e5";
        String textCol = dark ? (active ? "#e0e7ff" : "#c7d2fe") : (active ? "#3730a3" : "#4f46e5");
        String hovBg   = dark ? "rgba(99,102,241,0.26)" : "#e0e7ff";

        StackPane box = new StackPane();
        box.setPrefSize(36,36); box.setMinSize(36,36); box.setMaxSize(36,36);
        box.setStyle("-fx-background-color:" + iconBg + ";-fx-background-radius:10;");
        box.getChildren().add(svgIcon(svgPath, iconCol, 17));

        Label lbl = new Label(label);
        lbl.setStyle("-fx-font-size:13px;-fx-font-weight:" + (active ? "bold" : "normal") +
                ";-fx-text-fill:" + textCol + ";");

        HBox row = new HBox(12); row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(9,14,9,14)); row.setMaxWidth(Double.MAX_VALUE);
        String base = "-fx-background-color:" + pillBg + ";-fx-background-radius:12;" + (action != null ? "-fx-cursor:hand;" : "");
        String hov  = "-fx-background-color:" + hovBg  + ";-fx-background-radius:12;" + (action != null ? "-fx-cursor:hand;" : "");
        row.setStyle(base);
        if (action != null) {
            row.setOnMouseEntered(e -> { row.setStyle(hov); ScaleTransition st = new ScaleTransition(Duration.millis(120), box); st.setToX(1.10); st.setToY(1.10); st.play(); });
            row.setOnMouseExited (e -> { row.setStyle(base); ScaleTransition st = new ScaleTransition(Duration.millis(120), box); st.setToX(1.0); st.setToY(1.0); st.play(); });
            row.setOnMouseClicked(action);
        }
        row.getChildren().addAll(box, lbl);
        return row;
    }

    // ── Footer — identical to PanierController.buildSidebarFooter ────────
    private HBox buildFooter() {
        HBox footer = new HBox(6);
        footer.setAlignment(Pos.CENTER_LEFT);
        footer.setPadding(new Insets(12, 14, 16, 14));
        footer.setStyle("-fx-border-color:" + (isDark ? "rgba(255,255,255,0.07)" : "rgba(37,99,235,0.10)") + ";-fx-border-width:1 0 0 0;");

        Label ver = new Label("v2.4.1 \u00b7 Tunisia");
        ver.setStyle("-fx-font-size:10px;-fx-text-fill:" + (isDark ? "#374151" : "#94a3b8") + ";");
        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);

        // Theme toggle (moon emoji in light, sun in dark — same as Cart)
        Button themeBtn = new Button(isDark ? "\u2600" : "\uD83C\uDF19");
        themeBtn.setStyle(
                "-fx-background-color:" + (isDark ? "rgba(251,191,36,0.12)" : "transparent") + ";" +
                        "-fx-border-color:" + (isDark ? "rgba(251,191,36,0.55)" : "rgba(37,99,235,0.20)") + ";" +
                        "-fx-border-radius:50;-fx-background-radius:50;-fx-border-width:1.5;" +
                        "-fx-text-fill:" + (isDark ? "#FCD34D" : "#2563EB") + ";" +
                        "-fx-font-size:14px;-fx-min-width:34;-fx-min-height:34;-fx-cursor:hand;" +
                        "-fx-focus-color:transparent;-fx-faint-focus-color:transparent;"
        );
        themeBtn.setFocusTraversable(false);
        themeBtn.setOnAction(e -> {
            isDark = !isDark;
            UserSession.getInstance().setDarkMode(isDark);
            rebuildContent();
            applyShell();
            if (themeChangeCallback != null) themeChangeCallback.run();
        });

        // Power button — same style as Cart (indigo, SVG icon)
        StackPane powerBox = new StackPane(svgIcon(IC_POWER, "#4f46e5", 18));
        powerBox.setPrefSize(34,34); powerBox.setMinSize(34,34); powerBox.setMaxSize(34,34);
        String pwBase = "-fx-background-color:" + (isDark ? "rgba(99,102,241,0.10)" : "#eef2ff") + ";" +
                "-fx-border-color:" + (isDark ? "rgba(99,102,241,0.40)" : "#a5b4fc") + ";" +
                "-fx-border-radius:50;-fx-background-radius:50;-fx-border-width:1.5;-fx-cursor:hand;";
        String pwHov  = "-fx-background-color:" + (isDark ? "rgba(99,102,241,0.22)" : "#e0e7ff") + ";" +
                "-fx-border-color:#6366f1;-fx-border-radius:50;-fx-background-radius:50;-fx-border-width:1.5;-fx-cursor:hand;";
        powerBox.setStyle(pwBase);
        powerBox.setOnMouseEntered(e -> powerBox.setStyle(pwHov));
        powerBox.setOnMouseExited (e -> powerBox.setStyle(pwBase));
        powerBox.setOnMouseClicked(e -> handleLogout());

        footer.getChildren().addAll(ver, sp, themeBtn, powerBox);
        return footer;
    }

    // ── Animations ────────────────────────────────────────────────────────
    private void openDrawer() {
        overlayPane.setVisible(true); overlayPane.setManaged(true);
        FadeTransition fade = new FadeTransition(Duration.millis(180), overlayPane);
        fade.setFromValue(0); fade.setToValue(1);
        TranslateTransition slide = new TranslateTransition(Duration.millis(280), drawerPane);
        slide.setToX(0); slide.setInterpolator(Interpolator.EASE_OUT);
        new ParallelTransition(fade, slide).play();
        open = true;
    }

    private void close() {
        if (!open) return;
        TranslateTransition slide = new TranslateTransition(Duration.millis(220), drawerPane);
        slide.setToX(-250); slide.setInterpolator(Interpolator.EASE_IN);
        FadeTransition fade = new FadeTransition(Duration.millis(200), overlayPane);
        fade.setFromValue(1); fade.setToValue(0);
        ParallelTransition pt = new ParallelTransition(fade, slide);
        pt.setOnFinished(e -> { overlayPane.setVisible(false); overlayPane.setManaged(false); });
        pt.play();
        open = false;
    }

    private void navigate(String fxml, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/hanouti/hanoutiem4/" + fxml));
            Scene scene = new Scene(loader.load(), 1250, 700);
            Stage stage = (Stage) rootPane.getScene().getWindow();
            stage.setTitle(title);
            stage.setScene(scene);
        } catch (IOException ex) {
            Alert a = new Alert(Alert.AlertType.ERROR);
            a.setTitle("Navigation");
            a.setContentText("Impossible d'ouvrir " + fxml + " : " + ex.getMessage());
            a.showAndWait();
        }
    }

    private int getCartCount() {
        try {
            PanierDAO dao = new PanierDAO();
            return dao.getCartItems(UserSession.getInstance().getUserId())
                    .stream().mapToInt(p -> p.getQuantite()).sum();
        } catch (SQLException e) { return 0; }
    }

    private void handleLogout() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("D\u00e9connexion");
        confirm.setHeaderText("Voulez-vous vous d\u00e9connecter ?");
        confirm.setContentText("Vous serez d\u00e9connect\u00e9 de votre compte \u00ab "
                + UserSession.getInstance().getUserName() + " \u00bb.");
        ButtonType btnOui = new ButtonType("Se d\u00e9connecter");
        ButtonType btnNon = new ButtonType("Annuler", ButtonBar.ButtonData.CANCEL_CLOSE);
        confirm.getButtonTypes().setAll(btnOui, btnNon);
        confirm.showAndWait().ifPresent(r -> {
            if (r == btnOui) {
                UserSession.getInstance().logout();
                ((Stage) rootPane.getScene().getWindow()).close();
            }
        });
    }
}