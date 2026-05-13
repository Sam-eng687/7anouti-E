package edu.hanouti.gui;

import edu.hanouti.entities.CampagneMarketing;
import edu.hanouti.entities.ConseilsMarketing;
import edu.hanouti.entities.StatistiquesVentes;
import edu.hanouti.services.CampagneMarketingService;
import edu.hanouti.services.ConseilsMarketingService;
import edu.hanouti.services.ClaudeAIService;
import edu.hanouti.services.ConseilsIAService;
import edu.hanouti.services.StatistiquesVentesService;
import javafx.animation.*;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.geometry.*;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.ImageView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.*;
import javafx.scene.shape.*;
import javafx.scene.text.*;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import edu.hanouti.gui.components.ShopAssistantChat;
import edu.hanouti.services.GeminiMarketingService;
import edu.hanouti.utils.ConseilShareServer;
import edu.hanouti.utils.GoogleCalendarPromoLink;
import edu.hanouti.utils.MyConnection;
import edu.hanouti.utils.QrCodeFx;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.io.File;
import java.awt.Desktop;

public class HanoutiDashboard extends Application {

    // ==============================================
    // [1] THEME — dark / light
    // ==============================================
    private static boolean darkMode = true;

    // Couleurs dynamiques (changent selon le thème)
    static String BG_DEEP = "#0a0d1a";
    static String BG_CARD = "#111425";
    static String BG_SIDEBAR = "#111425";
    static String TEXT_1 = "#F1F5F9";
    static String TEXT_2 = "#94A3B8";
    static String TEXT_3 = "#475569";

    // Couleurs accent — identiques dans les deux thèmes
    static final String ROYAL = "#2563EB";
    static final String ORANGE = "#F97316";
    static final String VIOLET = "#8B5CF6";
    static final String NEON_GREEN = "#10B981";
    static final String PASTEL_ROSE = "#F472B6";
    static final String PASTEL_SKY = "#38BDF8";
    static final String GOLD = "#FBBF24";

    /** Cartes campagne : dépenses (indigo doux, remplace orange vif) */
    static final String CAMP_SPEND_1 = "#6366f1";
    static final String CAMP_SPEND_2 = "#818cf8";
    /** Cartes campagne : revenu (cyan / sky, aligné sur le thème) */
    static final String CAMP_REV_1 = "#0ea5e9";
    static final String CAMP_REV_2 = "#38bdf8";
    /** Badge statut ACTIVE — même famille que le template */
    static final String CAMP_ACTIVE = "#38bdf8";

    /** Retourne les couleurs des barres du graphique selon le thème */
    private String[] getBarColors() {
        if (darkMode) {
            return new String[] { "#38bdf8", "#10b981", "#8b5cf6", "#f97316", "#fbbf24", "#f472b6", "#06b6d4",
                    "#a78bfa", "#34d399", "#fb923c" };
        } else {
            // Mode clair : couleurs douces et désaturées
            return new String[] { "#60a5fa", "#34d399", "#a78bfa", "#fb923c", "#fbbf24", "#f472b6", "#38bdf8",
                    "#a78bfa", "#34d399", "#fb923c" };
        }
    }

    /** Retourne les couleurs des hexagones selon le thème */
    private String[] getHexColors() {
        if (darkMode) {
            return new String[] { "#38bdf8", "#10b981", "#fbbf24", "#64748b" };
        } else {
            // Mode clair : couleurs pastel douces
            return new String[] { "#60a5fa", "#34d399", "#fbbf24", "#94a3b8" };
        }
    }

    /** Retourne les couleurs des cartes produit selon le thème */
    private String[] getCardColors() {
        if (darkMode) {
            return new String[] { "#38bdf8", "#10b981", "#8b5cf6", "#f97316", "#fbbf24", "#f472b6" };
        } else {
            // Mode clair : couleurs douces
            return new String[] { "#60a5fa", "#34d399", "#a78bfa", "#fb923c", "#fbbf24", "#f472b6" };
        }
    }

    /** Applique le thème dark ou light */
    private void applyTheme(boolean dark) {
        darkMode = dark;
        // Synchroniser avec PDFExportManager
        edu.hanouti.utils.PDFExportManager.setDarkMode(dark);

        if (dark) {
            BG_DEEP = "#0a0d1a";
            BG_CARD = "#111425";
            BG_SIDEBAR = "#111425";
            TEXT_1 = "#F1F5F9";
            TEXT_2 = "#94A3B8";
            TEXT_3 = "#475569";
        } else {
            BG_DEEP = "#f0f4f8"; // Fond doux, pas blanc pur
            BG_CARD = "#ffffff";
            BG_SIDEBAR = "#ffffff";
            TEXT_1 = "#0f172a";
            TEXT_2 = "#334155";
            TEXT_3 = "#64748b";
        }
    }

    /** Met à jour les styles sans recharger l'interface */
    private void updateThemeStyles() {
        // Mettre à jour le fond principal
        if (rootRef != null) {
            rootRef.setStyle("-fx-background-color:" + BG_DEEP + ";");
        }

        if (primaryStage != null && primaryStage.getScene() != null) {
            javafx.scene.Parent root = primaryStage.getScene().getRoot();
            if (root instanceof StackPane) {
                // StackPane rootWithMenu
                Node mainVBox = ((StackPane) root).getChildren().get(0);
                if (mainVBox instanceof VBox) {
                    mainVBox.setStyle("-fx-background-color:" + BG_DEEP + ";");
                }
            }
        }

        // Mettre à jour la TopBar
        if (topBarRef != null) {
            String topBarBg = darkMode ? BG_SIDEBAR : "#ffffff";
            String topBarBorder = darkMode ? "rgba(255,255,255,0.07)" : "rgba(0,0,0,0.1)";
            topBarRef.setStyle(
                    "-fx-background-color: " + topBarBg + ";" +
                            "-fx-border-color: " + topBarBorder + ";" +
                            "-fx-border-width: 0 0 1 0;" +
                            (darkMode ? "" : "-fx-effect: dropshadow(gaussian,rgba(0,0,0,0.04),8,0,0,2);"));
        }

        // Mettre à jour la Sidebar
        if (sidebarRef != null) {
            String sidebarBg = darkMode ? BG_SIDEBAR : "#ffffff";
            String sidebarBorder = darkMode ? "rgba(255,255,255,0.07)" : "rgba(0,0,0,0.1)";
            sidebarRef.setStyle(
                    "-fx-background-color: " + sidebarBg + ";" +
                            "-fx-border-color: " + sidebarBorder + ";" +
                            "-fx-border-width: 0 1 0 0;" +
                            (darkMode ? "" : "-fx-effect: dropshadow(gaussian,rgba(0,0,0,0.04),8,0,2,0);"));
        }

        // Mettre à jour LogoZone et DbZone
        if (logoZoneRef != null) {
            String logoZoneBorder = darkMode ? "rgba(255,255,255,0.07)" : "rgba(0,0,0,0.08)";
            logoZoneRef.setStyle("-fx-border-color: " + logoZoneBorder + "; -fx-border-width: 0 0 1 0;");
        }
        if (dbZoneRef != null) {
            String dbZoneBorder = darkMode ? "rgba(255,255,255,0.07)" : "rgba(0,0,0,0.08)";
            dbZoneRef.setStyle("-fx-border-color: " + dbZoneBorder + "; -fx-border-width: 1 0 0 0;");
        }

        // Mettre à jour la barre de recherche
        if (searchBoxRef != null) {
            String searchBg = darkMode ? "rgba(13,21,38,0.8)" : "rgba(0,0,0,0.03)";
            String searchBorder = darkMode ? "rgba(56,189,248,0.15)" : "rgba(0,0,0,0.08)";
            searchBoxRef.setStyle(
                    "-fx-background-color:" + searchBg + ";" +
                            "-fx-border-color:" + searchBorder + ";" +
                            "-fx-border-width:1;" +
                            "-fx-border-radius:99;" +
                            "-fx-background-radius:99;" +
                            "-fx-padding:0 14 0 14;");
        }

        // Mettre à jour le bouton de thème
        if (themeBtnRef != null) {
            themeBtnRef.setStyle(
                    "-fx-background-color:" + (darkMode ? "rgba(255,255,255,0.1)" : "rgba(59,130,246,0.1)") + ";" +
                            "-fx-background-radius:10;" +
                            "-fx-border-color:" + (darkMode ? "rgba(255,255,255,0.2)" : "rgba(59,130,246,0.3)") + ";" +
                            "-fx-border-width:2;-fx-border-radius:10;" +
                            "-fx-cursor:hand;-fx-padding:8;");
        }

        // Mettre à jour la navigation sidebar
        if (sidebarNavRef != null) {
            sidebarNavRef.updateTheme(darkMode);
        }

        // Recharger le module actif pour appliquer les nouveaux styles
        Platform.runLater(() -> naviguerVers(currentModule));
    }

    // Services
    private final StatistiquesVentesService statsService = new StatistiquesVentesService();
    private final ConseilsMarketingService conseilsService = new ConseilsMarketingService();
    private final CampagneMarketingService campagnesService = new CampagneMarketingService();

    /**
     * Affiche une notification premium de style Toast en haut à droite
     */
    public void showNotification(String title, String message, String type) {
        javafx.application.Platform.runLater(() -> {
            VBox toast = new VBox(4);
            toast.setMaxSize(320, 80);
            toast.setPadding(new Insets(15, 20, 15, 20));

            String color = type.equalsIgnoreCase("success") ? NEON_GREEN
                    : type.equalsIgnoreCase("delete") ? PASTEL_ROSE : PASTEL_SKY;
            String icon = type.equalsIgnoreCase("success") ? "✓" : type.equalsIgnoreCase("delete") ? "🗑" : "ℹ";

            String bgColor = darkMode ? "rgba(17, 20, 37, 0.85)" : "rgba(255, 255, 255, 0.9)";
            toast.setStyle(
                    "-fx-background-color: " + bgColor + ";" +
                            "-fx-background-radius: 12;" +
                            "-fx-border-color: " + color + "44;" + // 44 is transparency
                            "-fx-border-width: 1;" +
                            "-fx-effect: dropshadow(gaussian, " + color + "33, 15, 0, 0, 0);");

            HBox header = new HBox(10);
            header.setAlignment(Pos.CENTER_LEFT);

            Label iconLbl = new Label(icon);
            iconLbl.setStyle("-fx-text-fill: " + color + "; -fx-font-size: 18px; -fx-font-weight: bold;");

            Label titleLbl = new Label(title);
            titleLbl.setStyle("-fx-text-fill: " + TEXT_1 + "; -fx-font-size: 14px; -fx-font-weight: 900;");
            header.getChildren().addAll(iconLbl, titleLbl);

            Label msgLbl = new Label(message);
            msgLbl.setStyle("-fx-text-fill: " + TEXT_2 + "; -fx-font-size: 12px;");
            msgLbl.setWrapText(true);

            toast.getChildren().addAll(header, msgLbl);

            globalOverlay.getChildren().add(toast);
            StackPane.setAlignment(toast, Pos.TOP_RIGHT);
            StackPane.setMargin(toast, new Insets(80, 25, 0, 0)); // Under TopBar

            // Animation entrée
            toast.setTranslateX(400);
            toast.setOpacity(0);

            TranslateTransition slideIn = new TranslateTransition(Duration.millis(500), toast);
            slideIn.setToX(0);
            slideIn.setInterpolator(Interpolator.EASE_OUT);

            FadeTransition fadeIn = new FadeTransition(Duration.millis(400), toast);
            fadeIn.setToValue(1);

            ParallelTransition entry = new ParallelTransition(slideIn, fadeIn);
            entry.play();

            // Auto-dismiss après 3.5s
            entry.setOnFinished(e -> {
                new Thread(() -> {
                    try {
                        Thread.sleep(3000);
                    } catch (InterruptedException ex) {
                    }
                    Platform.runLater(() -> {
                        TranslateTransition slideOut = new TranslateTransition(Duration.millis(400), toast);
                        slideOut.setToX(400);
                        FadeTransition fadeOut = new FadeTransition(Duration.millis(300), toast);
                        fadeOut.setToValue(0);
                        ParallelTransition exit = new ParallelTransition(slideOut, fadeOut);
                        exit.setOnFinished(ev -> globalOverlay.getChildren().remove(toast));
                        exit.play();
                    });
                }).start();
            });
        });
    }

    private final ClaudeAIService claudeService = new ClaudeAIService();
    private final ConseilsIAService conseilsIaService = new ConseilsIAService();
    private final edu.hanouti.services.NewsService newsService = new edu.hanouti.services.NewsService();
    private final edu.hanouti.services.PrayerTimeService prayerService = new edu.hanouti.services.PrayerTimeService();
    private final edu.hanouti.services.WeatherService weatherService = new edu.hanouti.services.WeatherService();
    private final GeminiMarketingService geminiService = new GeminiMarketingService();

    // ==============================================
    // [2] ÉTAT DE L'APPLICATION — champs et services
    // ==============================================
    // Module actif
    private Stage primaryStage;
    private StackPane contentArea;
    private Button activeNavBtn = null;
    private String currentFilter = "Toutes les periodes";
    private String currentModule = "stats"; // Module actuellement affiché
    private String initialConseilFilter = "Tous";
    private Button btnStats;
    private Button btnConseils;
    private Button btnCamp;
    private Button btnMarketing;
    private edu.hanouti.gui.components.SidebarNav sidebarNavRef;

    // Référence au label du module actif dans le topBar
    private Label topBarModuleLabel;
    private TextField globalSearchField;
    private VBox sidebarRef; // référence pour dissolution/réapparition
    private Button menuBtnRef; // bouton ☰ pour réafficher la sidebar
    private HBox topBarRef;
    private HBox searchBoxRef;
    private Button themeBtnRef;
    private VBox logoZoneRef;
    private VBox dbZoneRef;
    private HBox liveBarRef;
    private VBox rootRef;
    private StackPane globalOverlay;

    @Override
    public void start(Stage stage) {
        this.primaryStage = stage;
        stage.setTitle("7anouti-E - Dashboard Marketing Premium");

        // -- Verification connexion MySQL avec dialogue d'erreur creatif --
        if (!checkDbStatus()) {
            showDbErrorDialog(stage);
            return;
        }

        // Zone de contenu
        contentArea = new StackPane();
        contentArea.setStyle("-fx-background-color: " + BG_DEEP + ";");
        HBox.setHgrow(contentArea, Priority.ALWAYS);

        VBox sidebar = buildSidebar();
        sidebarRef = sidebar;

        // Barre d'alertes en bas avec animation
        liveBarRef = buildLiveAlertBar();
        HBox.setHgrow(liveBarRef, Priority.ALWAYS);

        // Groupe de droite : Contenu + Barre d'alertes
        VBox centerPanel = new VBox(contentArea, liveBarRef);
        VBox.setVgrow(contentArea, Priority.ALWAYS);
        HBox.setHgrow(centerPanel, Priority.ALWAYS);
        centerPanel.setMinWidth(0);

        // Layout principal : sidebar + centerPanel
        HBox mainRow = new HBox(sidebar, centerPanel);
        mainRow.setStyle("-fx-background-color: " + BG_DEEP + ";");
        VBox.setVgrow(mainRow, Priority.ALWAYS);

        // TopBar global
        topBarRef = buildTopBar();

        // Root = topBar + mainRow
        rootRef = new VBox(topBarRef, mainRow);
        rootRef.setStyle("-fx-background-color: " + BG_DEEP + ";");

        // Overlay pour les notifications et dialogues
        globalOverlay = new StackPane(rootRef);

        Scene scene = new Scene(globalOverlay, 1280, 720);
        stage.setScene(scene);
        stage.setMaximized(true);
        stage.show();

        Button chatFab = new Button("\uD83E\uDD16");
        chatFab.setTooltip(new Tooltip("Assistant Hanouti (Gemini) — boutique & app"));
        chatFab.setStyle(
                "-fx-font-size: 20px; -fx-background-radius: 99; -fx-min-width: 54; -fx-min-height: 54; -fx-max-width: 54; -fx-max-height: 54;"
                        + "-fx-background-color: linear-gradient(to bottom right,#8b5cf6,#6366f1); -fx-text-fill: white; -fx-cursor: hand;"
                        + "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.45), 14, 0.25, 0, 3);");
        chatFab.setOnAction(e -> ShopAssistantChat.show(primaryStage, geminiService, darkMode, VIOLET));
        globalOverlay.getChildren().add(chatFab);
        StackPane.setAlignment(chatFab, Pos.BOTTOM_RIGHT);
        StackPane.setMargin(chatFab, new Insets(0, 24, 96, 0));

        // Charger le module initial
        Platform.runLater(() -> naviguerVers("stats"));
    }

    /** Dialogue d'erreur MySQL creatif */
    private void showDbErrorDialog(Stage owner) {
        Stage errStage = new Stage();
        errStage.initOwner(owner);
        errStage.initStyle(javafx.stage.StageStyle.UNDECORATED);
        errStage.initModality(javafx.stage.Modality.APPLICATION_MODAL);

        // Couleurs adaptatives - Thème rouge pastel en mode clair
        String errBg = darkMode ? "#0a0f1e" : "#fef2f2"; // Rouge très clair
        String errBorder = darkMode ? "#f97316" : "#f87171"; // Rouge pastel
        String errTitle = darkMode ? "#f97316" : "#dc2626"; // Rouge foncé
        String errMsg = darkMode ? "#94a3b8" : "#64748b"; // Gris
        String errBtn = darkMode ? "#f97316" : "#ef4444"; // Rouge
        String errBtnCancel = darkMode ? "#1a2035" : "#e5e7eb"; // Gris clair
        String errBtnCancelText = darkMode ? "#64748b" : "#6b7280";

        javafx.scene.layout.VBox root = new javafx.scene.layout.VBox(18);
        root.setStyle(
                "-fx-background-color:" + errBg + ";" +
                        "-fx-border-color:" + errBorder + ";" +
                        "-fx-border-width:2;-fx-border-radius:16;-fx-background-radius:16;" +
                        "-fx-padding:32 36 28 36;" +
                        "-fx-effect:dropshadow(gaussian," + (darkMode ? "rgba(0,0,0,0.6)" : "rgba(0,0,0,0.15)")
                        + ",20,0,0,6);");
        root.setPrefWidth(420);
        root.setAlignment(javafx.geometry.Pos.CENTER);

        javafx.scene.control.Label icon = new javafx.scene.control.Label("\u26A0");
        icon.setStyle("-fx-font-size:42px;");

        javafx.scene.control.Label title = new javafx.scene.control.Label("Connexion MySQL impossible");
        title.setStyle("-fx-text-fill:" + errTitle + ";-fx-font-size:17px;-fx-font-weight:bold;");

        javafx.scene.control.Label msg = new javafx.scene.control.Label(
                "Impossible de se connecter a la base de donnees.\n\n" +
                        "Verifiez que :\n" +
                        "  . WAMP ou XAMPP est lance\n" +
                        "  . MySQL tourne sur le port 3306\n" +
                        "  . La base '7anouti' existe");
        msg.setStyle("-fx-text-fill:" + errMsg + ";-fx-font-size:13px;-fx-text-alignment:center;");
        msg.setWrapText(true);
        msg.setMaxWidth(350);
        msg.setAlignment(javafx.geometry.Pos.CENTER);

        javafx.scene.layout.HBox btnRow = new javafx.scene.layout.HBox(10);
        btnRow.setAlignment(javafx.geometry.Pos.CENTER);

        javafx.scene.control.Button btnRetry = new javafx.scene.control.Button("\uD83D\uDD04  Reessayer");
        btnRetry.setPrefWidth(150);
        btnRetry.setPrefHeight(40);
        btnRetry.setStyle(
                "-fx-background-color:" + errBtn + ";" +
                        "-fx-text-fill:white;-fx-font-weight:bold;" +
                        "-fx-font-size:13px;-fx-background-radius:9;-fx-cursor:hand;");
        btnRetry.setOnAction(e -> {
            errStage.close();
            // Reessayer le demarrage
            if (checkDbStatus()) {
                contentArea = new StackPane();
                contentArea.setStyle("-fx-background-color:" + BG_DEEP + ";");
                HBox.setHgrow(contentArea, Priority.ALWAYS);
                VBox sidebar = buildSidebar();
                HBox rootBox = new HBox(sidebar, contentArea);
                rootBox.setStyle("-fx-background-color:" + BG_DEEP + ";");
                Scene scene = new Scene(rootBox, 1280, 720);
                owner.setScene(scene);
                owner.show();
            } else {
                showDbErrorDialog(owner);
            }
        });

        javafx.scene.control.Button btnQuit = new javafx.scene.control.Button("Quitter");
        btnQuit.setPrefWidth(150);
        btnQuit.setPrefHeight(40);
        btnQuit.setStyle(
                "-fx-background-color:" + errBtnCancel + ";" +
                        "-fx-text-fill:" + errBtnCancelText + ";" +
                        "-fx-font-size:13px;-fx-background-radius:9;-fx-cursor:hand;");
        btnQuit.setOnAction(e -> {
            errStage.close();
            javafx.application.Platform.exit();
        });

        btnRow.getChildren().addAll(btnRetry, btnQuit);
        root.getChildren().addAll(icon, title, msg, btnRow);

        javafx.scene.Scene errScene = new javafx.scene.Scene(root);
        errScene.setFill(javafx.scene.paint.Color.TRANSPARENT);
        errStage.setScene(errScene);
        owner.show();
        errStage.showAndWait();
    }

    // ==============================================
    // [3] TOP BAR GLOBAL
    // ==============================================
    private HBox buildTopBar() {
        HBox bar = new HBox(16);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPrefHeight(52);
        bar.setMinHeight(52);
        bar.setMaxHeight(52);
        bar.setPadding(new Insets(0, 20, 0, 20));

        // Couleurs adaptatives pour la topBar
        String topBarBg = darkMode ? BG_SIDEBAR : "#ffffff";
        String topBarBorder = darkMode ? "rgba(255,255,255,0.07)" : "rgba(0,0,0,0.1)";

        bar.setStyle(
                "-fx-background-color: " + topBarBg + ";" +
                        "-fx-border-color: " + topBarBorder + ";" +
                        "-fx-border-width: 0 0 1 0;" +
                        (darkMode ? "" : "-fx-effect: dropshadow(gaussian,rgba(0,0,0,0.04),8,0,0,2);"));

        this.topBarRef = bar;

        // ── Logo compact ──
        javafx.scene.text.TextFlow logoFlow = new javafx.scene.text.TextFlow();
        Text t1 = new Text("7");
        t1.setStyle("-fx-font-size:18px;-fx-font-weight:900;");
        t1.setFill(Color.web("#38bdf8"));
        Text t2 = new Text("anouti");
        t2.setStyle("-fx-font-size:18px;-fx-font-weight:900;");
        t2.setFill(Color.web(TEXT_1));
        Text t3 = new Text("-E");
        t3.setStyle("-fx-font-size:18px;-fx-font-weight:900;");
        t3.setFill(Color.web("#38bdf8"));
        logoFlow.getChildren().addAll(t1, t2, t3);
        logoFlow.setEffect(new DropShadow(10, Color.web("#38bdf8", darkMode ? 0.5 : 0.3)));
        logoFlow.setTranslateY(8); // Nudge down for perfect centering in the TopBar/ Nudge down slightly for
                                   // better baseline alignment

        // ── Séparateur ──
        javafx.scene.shape.Rectangle sep = new javafx.scene.shape.Rectangle(1, 28);
        sep.setFill(Color.web(darkMode ? "rgba(255,255,255,0.1)" : "rgba(0,0,0,0.1)"));

        // ── Module actif label (glisse depuis la gauche) ──
        topBarModuleLabel = new Label("Dashboard");
        topBarModuleLabel.setStyle(
                "-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: " + PASTEL_SKY + ";" +
                        "-fx-opacity: 0.85;");

        // ── Spacer ──
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // ── Barre de recherche globale ──
        HBox searchBox = new HBox(8);
        searchBox.setAlignment(Pos.CENTER_LEFT);
        searchBox.setPrefWidth(340);
        searchBox.setPrefHeight(36);

        // Couleurs adaptatives pour la recherche - FOND PLUS SOMBRE
        String searchBg = darkMode ? "rgba(13,21,38,0.8)" : "rgba(0,0,0,0.03)"; // Plus sombre en dark
        String searchBorder = darkMode ? "rgba(56,189,248,0.15)" : "rgba(0,0,0,0.08)"; // Bordure plus visible
        String searchFocusBg = darkMode ? "rgba(56,189,248,0.08)" : "rgba(59,130,246,0.06)";
        String searchFocusBorder = darkMode ? PASTEL_SKY : "#3b82f6";
        String searchIconColor = darkMode ? "#64748b" : "#94a3b8"; // Icône plus visible
        String searchPromptColor = darkMode ? "#64748b" : "#94a3b8";

        searchBox.setStyle(
                "-fx-background-color:" + searchBg + ";" +
                        "-fx-border-color:" + searchBorder + ";" +
                        "-fx-border-width:1;" +
                        "-fx-border-radius:99;" +
                        "-fx-background-radius:99;" +
                        "-fx-padding:0 14 0 14;");
        this.searchBoxRef = searchBox;

        Label searchIcon = new Label("\uD83D\uDD0D");
        searchIcon.setStyle("-fx-font-size:13px;-fx-text-fill:" + searchIconColor + ";");

        globalSearchField = new TextField();
        globalSearchField.setPromptText("Rechercher...");
        globalSearchField.setStyle(
                "-fx-background-color:transparent;" +
                        "-fx-text-fill:" + TEXT_1 + ";" +
                        "-fx-prompt-text-fill:" + searchPromptColor + ";" +
                        "-fx-font-size:13px;" +
                        "-fx-border-color:transparent;" +
                        "-fx-padding:0;");
        globalSearchField.setPrefWidth(260);
        HBox.setHgrow(globalSearchField, Priority.ALWAYS);

        // Focus : border devient bleue - adapté au thème
        globalSearchField.focusedProperty().addListener((obs, old, focused) -> {
            if (focused) {
                searchBox.setStyle(
                        "-fx-background-color:" + searchFocusBg + ";" +
                                "-fx-border-color:" + searchFocusBorder + ";" +
                                "-fx-border-width:1.5;" +
                                "-fx-border-radius:99;" +
                                "-fx-background-radius:99;" +
                                "-fx-padding:0 14 0 14;");
            } else {
                searchBox.setStyle(
                        "-fx-background-color:" + searchBg + ";" +
                                "-fx-border-color:" + searchBorder + ";" +
                                "-fx-border-width:1;" +
                                "-fx-border-radius:99;" +
                                "-fx-background-radius:99;" +
                                "-fx-padding:0 14 0 14;");
            }
        });

        // Popup résultats — géré par searchPopup ci-dessous

        // Logique de recherche — données chargées immédiatement
        final java.util.List<edu.hanouti.entities.CampagneMarketing> campsCache;
        final java.util.List<edu.hanouti.entities.StatistiquesVentes> statsCache;
        final java.util.List<edu.hanouti.entities.ConseilsMarketing> conseilsCache;
        java.util.List<edu.hanouti.entities.CampagneMarketing> _c = null;
        java.util.List<edu.hanouti.entities.StatistiquesVentes> _s = null;
        java.util.List<edu.hanouti.entities.ConseilsMarketing> _k = null;
        try {
            _c = campagnesService.getData();
        } catch (Exception ignored) {
        }
        try {
            _s = statsService.getData();
        } catch (Exception ignored) {
        }
        try {
            _k = conseilsService.getData();
        } catch (Exception ignored) {
        }
        campsCache = _c != null ? _c : new java.util.ArrayList<>();
        statsCache = _s != null ? _s : new java.util.ArrayList<>();
        conseilsCache = _k != null ? _k : new java.util.ArrayList<>();

        // Popup JavaFX
        javafx.stage.Popup searchPopup = new javafx.stage.Popup();
        searchPopup.setAutoHide(true);
        searchPopup.setHideOnEscape(true);

        javafx.scene.layout.VBox resultsBox = new javafx.scene.layout.VBox(2);
        resultsBox.setPrefWidth(360);
        // Style sera mis à jour dynamiquement dans le listener
        searchPopup.getContent().add(resultsBox);

        globalSearchField.textProperty().addListener((obs, old, query) -> {
            if (query == null || query.trim().isEmpty()) {
                searchPopup.hide();
                return;
            }
            String q = query.trim().toLowerCase();
            // Normaliser les accents pour la recherche
            q = java.text.Normalizer.normalize(q, java.text.Normalizer.Form.NFD)
                    .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
            // Une seule lettre/chiffre : uniquement "commence par" (évite le bruit des contenus)
            final boolean strictPrefixOnly = q.length() < 2;
            resultsBox.getChildren().clear();

            // Mettre à jour le style du popup selon le thème actuel
            resultsBox.setStyle(
                    "-fx-background-color:" + (darkMode ? "#111425" : "#ffffff") + ";" +
                            "-fx-border-color:" + (darkMode ? "rgba(56,189,248,0.35)" : "rgba(59,130,246,0.3)") + ";" +
                            "-fx-border-width:1;" +
                            "-fx-border-radius:10;" +
                            "-fx-background-radius:10;" +
                            "-fx-padding:8;" +
                            "-fx-effect:dropshadow(gaussian," + (darkMode ? "rgba(0,0,0,0.6)" : "rgba(0,0,0,0.15)")
                            + ",20,0,0,6);");

            // ── Campagnes : commence par (nom / objectif / canal), puis contient si requête assez longue ──
            java.util.List<edu.hanouti.entities.CampagneMarketing> campStarts = new java.util.ArrayList<>();
            java.util.List<edu.hanouti.entities.CampagneMarketing> campContains = new java.util.ArrayList<>();
            for (edu.hanouti.entities.CampagneMarketing c : campsCache) {
                String nom = norm(c.getNomCampagne());
                String obj = norm(c.getObjectif());
                String can = norm(c.getCanal());
                if (nom.startsWith(q) || obj.startsWith(q) || can.startsWith(q))
                    campStarts.add(c);
                else if (!strictPrefixOnly && (nom.contains(q) || obj.contains(q) || can.contains(q)))
                    campContains.add(c);
            }
            java.util.List<edu.hanouti.entities.CampagneMarketing> campsSorted = new java.util.ArrayList<>(campStarts);
            campsSorted.addAll(campContains);
            int foundC = 0;
            for (edu.hanouti.entities.CampagneMarketing c : campsSorted) {
                if (foundC >= 5)
                    break;
                resultsBox.getChildren().add(buildSearchResultRow(
                        "\uD83D\uDCE2", c.getNomCampagne(),
                        "Campagne  \u00B7  " + c.getStatut() + "  \u00B7  " + c.getCanal(),
                        PASTEL_SKY,
                        () -> {
                            naviguerVers("campagnes");
                            globalSearchField.clear();
                            searchPopup.hide();
                        }));
                foundC++;
            }

            // ── Produits (stats) : même règle (préfixe sur le nom affiché, sans REF-) ──
            java.util.List<String> prodStarts = new java.util.ArrayList<>();
            java.util.List<String> prodContains = new java.util.ArrayList<>();
            java.util.Set<String> seenRefs = new java.util.LinkedHashSet<>();
            for (edu.hanouti.entities.StatistiquesVentes s : statsCache) {
                String ref = s.getProduitId() != null ? s.getProduitId() : "";
                String refNorm = norm(ref);
                // Nom propre = sans préfixe REF-
                String nomProp = norm(ref.startsWith("REF-") ? ref.substring(4) : ref);
                if (!seenRefs.contains(ref)) {
                    if (nomProp.startsWith(q) || refNorm.startsWith(q)) {
                        prodStarts.add(ref);
                        seenRefs.add(ref);
                    } else if (!strictPrefixOnly && (nomProp.contains(q) || refNorm.contains(q))) {
                        prodContains.add(ref);
                        seenRefs.add(ref);
                    }
                }
            }
            java.util.List<String> prodsSorted = new java.util.ArrayList<>(prodStarts);
            prodsSorted.addAll(prodContains);
            int foundS = 0;
            for (String ref : prodsSorted) {
                if (foundS >= 4)
                    break;
                String refClean = ref.replace("REF-", "");
                double totalRev = statsCache.stream()
                        .filter(x -> ref.equals(x.getProduitId()))
                        .mapToDouble(edu.hanouti.entities.StatistiquesVentes::getRevenuTotal)
                        .sum();
                resultsBox.getChildren().add(buildSearchResultRow(
                        "\uD83D\uDCE6", refClean,
                        String.format("Produit  \u00B7  %.0f TND  \u00B7  %s  \u00B7  %s",
                                totalRev,
                                edu.hanouti.services.ExchangeRateService.format(totalRev, "EUR"),
                                edu.hanouti.services.ExchangeRateService.format(totalRev, "USD")),
                        NEON_GREEN,
                        () -> {
                            naviguerVers("stats");
                            globalSearchField.clear();
                            searchPopup.hide();
                        }));
                foundS++;
            }

            // ── Conseils : préfixe sur type, description, produit ; contient seulement si requête >= 3 car. ──
            java.util.List<edu.hanouti.entities.ConseilsMarketing> conseilStarts = new java.util.ArrayList<>();
            java.util.List<edu.hanouti.entities.ConseilsMarketing> conseilContains = new java.util.ArrayList<>();
            for (edu.hanouti.entities.ConseilsMarketing c : conseilsCache) {
                String type = norm(c.getTypeConseil());
                String desc = norm(c.getDescription());
                String pid = norm(c.getProduitId());
                String pnom = norm(c.getProduitNom());
                if (type.startsWith(q) || desc.startsWith(q) || pid.startsWith(q) || pnom.startsWith(q))
                    conseilStarts.add(c);
                else if (!strictPrefixOnly && (type.contains(q) || desc.contains(q) || pid.contains(q) || pnom.contains(q)))
                    conseilContains.add(c);
            }
            java.util.List<edu.hanouti.entities.ConseilsMarketing> conseilsSorted = new java.util.ArrayList<>(
                    conseilStarts);
            conseilsSorted.addAll(conseilContains);
            int foundK = 0;
            for (edu.hanouti.entities.ConseilsMarketing c : conseilsSorted) {
                if (foundK >= 2)
                    break;
                String statut = c.isApplique() ? "Applique" : "En attente";
                String raw = c.getTypeConseil() != null ? c.getTypeConseil() : "";
                String lbl = raw;
                resultsBox.getChildren().add(buildSearchResultRow(
                        "\uD83D\uDCA1", lbl,
                        "Conseil  \u00B7  " + statut, GOLD,
                        () -> {
                            naviguerVers("conseils");
                            globalSearchField.clear();
                            searchPopup.hide();
                        }));
                foundK++;
            }

            // ── Aucun résultat ──
            if (resultsBox.getChildren().isEmpty()) {
                Label noRes = new Label(strictPrefixOnly
                        ? "Rien ne commence par « " + query.trim()
                                + " ». Ajoutez une 2e lettre pour chercher aussi à l'intérieur des noms."
                        : "Aucun resultat pour \"" + query + "\"");
                noRes.setWrapText(true);
                noRes.setMaxWidth(340);
                noRes.setStyle("-fx-text-fill:#475569;-fx-font-size:12px;-fx-padding:6 8;");
                resultsBox.getChildren().add(noRes);
            }

            // Afficher le popup sous la barre de recherche
            if (!searchPopup.isShowing()) {
                Platform.runLater(() -> {
                    javafx.geometry.Bounds b = searchBox.localToScreen(searchBox.getBoundsInLocal());
                    if (b != null)
                        searchPopup.show(primaryStage, b.getMinX(), b.getMaxY() + 6);
                });
            }
        });

        // Fermer au Enter
        globalSearchField.setOnAction(e -> searchPopup.hide());

        searchBox.getChildren().addAll(searchIcon, globalSearchField);

        // ── Badge IA Active ──
        Label iaBadge = new Label("\u26A1 IA Active");
        iaBadge.setStyle(
                "-fx-font-size: 11px; -fx-font-weight: bold;" +
                        "-fx-text-fill: " + NEON_GREEN + ";" +
                        "-fx-background-color: rgba(16,185,129,0.12);" +
                        "-fx-padding: 5 12 5 12;" +
                        "-fx-border-color: rgba(16,185,129,0.3);" +
                        "-fx-border-width: 1;" +
                        "-fx-border-radius: 99;" +
                        "-fx-background-radius: 99;");
        FadeTransition iaPulse = new FadeTransition(Duration.seconds(2), iaBadge);
        iaPulse.setFromValue(1.0);
        iaPulse.setToValue(0.6);
        iaPulse.setAutoReverse(true);
        iaPulse.setCycleCount(Timeline.INDEFINITE);
        iaPulse.play();

        // ── Toggle Dark/Light ──
        Label themeIcon = new Label(darkMode ? "\uD83C\uDF19" : "\u2600");
        themeIcon.setStyle("-fx-font-size:20px;"); // Agrandi de 16px à 20px
        Button themeBtn = new Button();
        themeBtn.setGraphic(themeIcon);
        themeBtn.setPrefSize(44, 44); // Taille fixe plus grande
        themeBtn.setStyle(
                "-fx-background-color:" + (darkMode ? "rgba(255,255,255,0.1)" : "rgba(59,130,246,0.1)") + ";" +
                        "-fx-background-radius:10;" +
                        "-fx-border-color:" + (darkMode ? "rgba(255,255,255,0.2)" : "rgba(59,130,246,0.3)") + ";" +
                        "-fx-border-width:2;-fx-border-radius:10;" +
                        "-fx-cursor:hand;-fx-padding:8;");
        this.themeBtnRef = themeBtn;
        themeBtn.setOnAction(e -> {
            // Changer le thème SANS recharger l'interface
            applyTheme(!darkMode);
            themeIcon.setText(darkMode ? "\uD83C\uDF19" : "\u2600");

            // Mettre à jour uniquement les styles sans reconstruire
            updateThemeStyles();
        });
        javafx.scene.control.Tooltip.install(themeBtn,
                new javafx.scene.control.Tooltip(darkMode ? "Passer en mode clair" : "Passer en mode sombre"));

        // ── Bouton ☰ (Toggle Sidebar) ──
        Button menuBtn = new Button("\u2630");
        menuBtn.setStyle(
                "-fx-background-color: transparent;" +
                        "-fx-text-fill: " + PASTEL_SKY + ";" +
                        "-fx-font-size: 20px;" +
                        "-fx-cursor: hand;" +
                        "-fx-padding: 0 10 0 0;");
        menuBtn.setOnAction(e -> toggleSidebar(true));
        this.menuBtnRef = menuBtn;

        bar.getChildren().addAll(menuBtn, logoFlow, sep, topBarModuleLabel, spacer, searchBox, iaBadge, themeBtn);

        // Par défaut, si la sidebar est affichée au début, on cache le bouton
        menuBtn.setVisible(false);
        menuBtn.setManaged(false);

        return bar;
    }

    /** Basculer la visibilité de la sidebar avec animation */
    private void toggleSidebar(boolean show) {
        if (sidebarRef == null || menuBtnRef == null)
            return;

        if (show) {
            // AFFICHER la sidebar
            menuBtnRef.setVisible(false);
            menuBtnRef.setManaged(false);

            sidebarRef.setVisible(true);
            sidebarRef.setManaged(true);
            sidebarRef.setPrefWidth(68);
            sidebarRef.setMinWidth(68);
            sidebarRef.setTranslateX(-68);
            sidebarRef.setOpacity(0);

            TranslateTransition slideIn = new TranslateTransition(Duration.millis(300), sidebarRef);
            slideIn.setToX(0);
            FadeTransition fadeIn = new FadeTransition(Duration.millis(300), sidebarRef);
            fadeIn.setToValue(1);
            new ParallelTransition(slideIn, fadeIn).play();
        } else {
            // CACHER la sidebar
            TranslateTransition slideOut = new TranslateTransition(Duration.millis(250), sidebarRef);
            slideOut.setToX(-70);
            FadeTransition fadeOut = new FadeTransition(Duration.millis(250), sidebarRef);
            fadeOut.setToValue(0);

            ParallelTransition hideAnim = new ParallelTransition(slideOut, fadeOut);
            hideAnim.setOnFinished(ev -> {
                sidebarRef.setVisible(false);
                sidebarRef.setManaged(false);

                menuBtnRef.setVisible(true);
                menuBtnRef.setManaged(true);
            });
            hideAnim.play();
        }
    }

    /** Normalise une chaine : minuscules + supprime accents */
    private static String norm(String s) {
        if (s == null)
            return "";
        String lower = s.toLowerCase();
        return java.text.Normalizer.normalize(lower, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
    }

    /** Ligne de résultat de recherche */
    private HBox buildSearchResultRow(String icon, String title, String sub, String color, Runnable onClick) {
        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(8, 10, 8, 10));
        row.setStyle("-fx-background-radius: 7; -fx-cursor: hand;");

        Label iconLbl = new Label(icon);
        iconLbl.setStyle("-fx-font-size: 14px;");

        VBox texts = new VBox(2);
        Label titleLbl = new Label(title);
        titleLbl.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: " + TEXT_1 + ";");
        Label subLbl = new Label(sub);
        subLbl.setStyle("-fx-font-size: 11px; -fx-text-fill: " + color + ";");
        texts.getChildren().addAll(titleLbl, subLbl);

        row.getChildren().addAll(iconLbl, texts);

        // Couleurs hover adaptées au thème
        String hoverBg = darkMode ? "rgba(56,189,248,0.08)" : "rgba(59,130,246,0.06)";
        row.setOnMouseEntered(e -> row.setStyle(
                "-fx-background-color: " + hoverBg + "; -fx-background-radius: 7; -fx-cursor: hand;"));
        row.setOnMouseExited(e -> row.setStyle(
                "-fx-background-radius: 7; -fx-cursor: hand;"));
        row.setOnMouseClicked(e -> {
            if (onClick != null)
                onClick.run();
        });
        return row;
    }

    /**
     * Animation dissolution sidebar ligne par ligne (haut → bas, 320ms total)
     * puis cache la sidebar et affiche le bouton ☰
     */
    private void dissolveSidebar(VBox sidebar, Runnable onDone) {
        if (sidebar == null) {
            if (onDone != null)
                onDone.run();
            return;
        }
        int nbLines = 8; // nombre de "lignes" simulées
        int totalMs = 320;
        int stepMs = totalMs / nbLines;

        // Créer des rectangles de dissolution par-dessus la sidebar
        StackPane overlay = new StackPane();
        overlay.setPrefWidth(sidebar.getWidth() > 0 ? sidebar.getWidth() : 68);
        overlay.setPrefHeight(sidebar.getHeight() > 0 ? sidebar.getHeight() : 720);
        overlay.setStyle("-fx-background-color:transparent;");
        overlay.setMouseTransparent(true);

        VBox lines = new VBox(0);
        double lineH = overlay.getPrefHeight() / nbLines;
        // Couleur de dissolution adaptée au thème
        String dissolveColor = darkMode ? "#0a0d1a" : "#f0f4f8";
        for (int i = 0; i < nbLines; i++) {
            javafx.scene.shape.Rectangle line = new javafx.scene.shape.Rectangle(0, lineH);
            line.setFill(Color.web(dissolveColor));
            lines.getChildren().add(line);
        }
        overlay.getChildren().add(lines);

        // Insérer l'overlay dans le parent de la sidebar
        if (sidebar.getParent() instanceof javafx.scene.layout.HBox) {
            javafx.scene.layout.HBox parent = (javafx.scene.layout.HBox) sidebar.getParent();
            StackPane sideWrap = new StackPane(sidebar, overlay);
            sideWrap.setPrefWidth(sidebar.getPrefWidth());
            int idx = parent.getChildren().indexOf(sidebar);
            if (idx >= 0) {
                parent.getChildren().set(idx, sideWrap);
            }
        }

        // Animer chaque ligne de gauche à droite en séquence
        Timeline tl = new Timeline();
        double maxW = overlay.getPrefWidth() > 0 ? overlay.getPrefWidth() : 68;
        for (int i = 0; i < nbLines; i++) {
            javafx.scene.shape.Rectangle line = (javafx.scene.shape.Rectangle) lines.getChildren().get(i);
            int delay = i * stepMs;
            tl.getKeyFrames().addAll(
                    new KeyFrame(Duration.millis(delay),
                            new KeyValue(line.widthProperty(), 0)),
                    new KeyFrame(Duration.millis(delay + stepMs),
                            new KeyValue(line.widthProperty(), maxW, Interpolator.EASE_IN)));
        }
        tl.setOnFinished(e -> {
            // Cacher la sidebar complètement
            sidebar.setVisible(false);
            sidebar.setManaged(false);
            // Restaurer le parent si on a wrappé
            if (sidebar.getParent() instanceof StackPane) {
                StackPane wrap = (StackPane) sidebar.getParent();
                if (wrap.getParent() instanceof javafx.scene.layout.HBox) {
                    javafx.scene.layout.HBox parent = (javafx.scene.layout.HBox) wrap.getParent();
                    int idx = parent.getChildren().indexOf(wrap);
                    if (idx >= 0)
                        parent.getChildren().set(idx, sidebar);
                }
            }
            // Le bouton menu est toujours visible maintenant, pas besoin de le montrer
            if (onDone != null)
                onDone.run();
        });
        tl.play();
    }

    /** Barre d'alertes en bas avec animation de défilement */
    private HBox buildLiveAlertBar() {
        HBox bar = new HBox();
        bar.setPrefHeight(42);
        bar.setMinHeight(42);
        bar.setMaxHeight(42);
        // Gradient rouge vif
        bar.setStyle(
                "-fx-background-color: linear-gradient(to right, #dc2626, #ef4444);" +
                        "-fx-border-color: rgba(0,0,0,0.3);" +
                        "-fx-border-width: 1 0 0 0;");

        // Badge LIVE
        HBox liveBadge = new HBox(6);
        liveBadge.setAlignment(Pos.CENTER);
        liveBadge.setPadding(new Insets(0, 16, 0, 16));
        liveBadge.setStyle("-fx-background-color: rgba(0,0,0,0.25);");

        Label liveIcon = new Label("⚡");
        liveIcon.setStyle("-fx-font-size: 14px; -fx-text-fill: #fef08a;"); // Jaune clair

        Label liveText = new Label("LIVE");
        liveText.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 12px;");

        // Animation pulse pour LIVE
        FadeTransition pulse = new FadeTransition(Duration.seconds(1), liveText);
        pulse.setFromValue(1.0);
        pulse.setToValue(0.6);
        pulse.setAutoReverse(true);
        pulse.setCycleCount(Timeline.INDEFINITE);
        pulse.play();

        liveBadge.getChildren().addAll(liveIcon, liveText);

        // Zone de défilement des alertes
        StackPane scrollContainer = new StackPane();
        scrollContainer.setStyle("-fx-background-color: transparent;");
        scrollContainer.setMinWidth(0); // CRUCIAL: Empêche le ticker de pousser la largeur de la fenêtre
        scrollContainer.setPrefWidth(100);
        HBox.setHgrow(scrollContainer, Priority.ALWAYS);

        // Empêcher le ticker de déborder sur la largeur de la page
        Rectangle clip = new Rectangle();
        clip.widthProperty().bind(scrollContainer.widthProperty());
        clip.heightProperty().bind(scrollContainer.heightProperty());
        scrollContainer.setClip(clip);

        // Créer les alertes avec couleurs qui matchent le rouge
        HBox alertsBox = new HBox(40);
        alertsBox.setAlignment(Pos.CENTER_LEFT);
        alertsBox.setMinWidth(javafx.scene.layout.Region.USE_PREF_SIZE); // EMPÊCHE LES "..."
        alertsBox.setPadding(new Insets(0, 20, 0, 20));

        // Récupérer les vraies alertes depuis les données
        try {
            List<Map<String, Object>> produits = statsService.getDataAsMaps();
            int alertCount = 0;

            for (Map<String, Object> p : produits) {
                if (alertCount >= 5)
                    break;

                String ref = (String) p.get("produitId");
                if (ref == null)
                    continue;
                ref = ref.replace("REF-", "");

                Number stockNum = (Number) p.get("stock");
                int stock = stockNum != null ? stockNum.intValue() : 0;

                if (stock < 10) {
                    HBox alert = new HBox(8);
                    alert.setAlignment(Pos.CENTER);

                    Label dot = new Label("●");
                    dot.setStyle("-fx-text-fill: #fef08a; -fx-font-size: 10px;"); // Jaune clair

                    Label alertText = new Label("Stock critique — " + ref + " #" + (alertCount + 1) + " · " + stock
                            + " restant" + (stock <= 5 ? " · 0 vente ce mois" : ""));
                    alertText.setStyle("-fx-text-fill: #fef3c7; -fx-font-size: 13px; -fx-font-weight: 600;"); // Jaune
                                                                                                              // très
                                                                                                              // clair

                    alert.getChildren().addAll(dot, alertText);
                    alertsBox.getChildren().add(alert);
                    alertCount++;
                }
            }

            // Si pas assez d'alertes, ajouter des alertes génériques
            if (alertCount < 3) {
                HBox alert1 = createAlert("●", "Fidelite VIP — ROI +340% · EMAIL performant", "#fef08a", "#fef3c7");
                HBox alert2 = createAlert("●", "RAMADHAN — Budget 899 TND · Lancement imminent", "#fef08a", "#fef3c7");
                alertsBox.getChildren().addAll(alert1, alert2);
            }

        } catch (Exception e) {
            // Alertes par défaut en cas d'erreur
            HBox alert1 = createAlert("●", "Stock critique — CHARGEUR #21 · 0 vente ce mois", "#fef08a", "#fef3c7");
            HBox alert2 = createAlert("●", "Fidelite VIP — ROI +340% · EMAIL performant", "#fef08a", "#fef3c7");
            HBox alert3 = createAlert("●", "RAMADHAN — Budget 899 TND · Lancement imminent", "#fef08a", "#fef3c7");
            alertsBox.getChildren().addAll(alert1, alert2, alert3);
        }

        scrollContainer.getChildren().add(alertsBox);

        // Animation de défilement infini - On initialise avec les alertes de base
        baseAlerts.clear();
        baseAlerts.addAll(alertsBox.getChildren());
        restartTickerAnimation(alertsBox, scrollContainer);

        // -- Intégration News Externes (Asynchrone) --
        new Thread(() -> {
            try {
                Thread.sleep(2000);
            } catch (Exception e) {
            }
            List<String> news = newsService.getLatestNewsTitles();
            Platform.runLater(() -> {
                if (news != null && !news.isEmpty()) {
                    // On ne garde que les alertes de stock (les 5 premières max)
                    int stockCount = Math.min(baseAlerts.size(), 5);
                    List<Node> stocksOnly = new ArrayList<>(baseAlerts.subList(0, stockCount));

                    baseAlerts.clear();
                    baseAlerts.addAll(stocksOnly);

                    for (String title : news) {
                        baseAlerts.add(createAlert("\uD83D\uDDFE", title, "#fef08a", "#fef3c7"));
                    }
                    // Relancer avec la liste complète (stocks + news)
                    restartTickerAnimation(alertsBox, scrollContainer);
                }
            });
        }).start();

        bar.getChildren().addAll(liveBadge, scrollContainer);
        return bar;
    }

    private TranslateTransition currentTickerAnim;
    private List<Node> baseAlerts = new ArrayList<>();

    private void restartTickerAnimation(HBox alertsBox, StackPane container) {
        if (currentTickerAnim != null)
            currentTickerAnim.stop();
        if (baseAlerts.isEmpty())
            return;

        // On reconstruit l'alertsBox proprement : [Base] + [Base] pour la boucle
        alertsBox.getChildren().clear();
        alertsBox.getChildren().addAll(baseAlerts);

        // On clone manuellement pour la boucle
        for (Node n : baseAlerts) {
            if (n instanceof HBox) {
                HBox orig = (HBox) n;
                Label icon = (Label) orig.getChildren().get(0);
                Label text = (Label) orig.getChildren().get(1);
                alertsBox.getChildren().add(createAlert(icon.getText(), text.getText(),
                        icon.getStyle().contains("#fef08a") ? "#fef08a" : "#38bdf8",
                        text.getStyle().contains("#fef3c7") ? "#fef3c7" : "#e0f2fe"));
            }
        }

        // Calculer la largeur réelle après layout
        alertsBox.applyCss();
        alertsBox.layout();

        double totalWidth = 0;
        for (Node n : alertsBox.getChildren()) {
            totalWidth += n.getBoundsInLocal().getWidth() + alertsBox.getSpacing();
        }

        final double finalWidth = totalWidth / 2;
        if (finalWidth <= 0)
            return;

        double duration = 20 + (alertsBox.getChildren().size() * 1.5);

        currentTickerAnim = new TranslateTransition(Duration.seconds(duration), alertsBox);
        currentTickerAnim.setFromX(0);
        currentTickerAnim.setToX(-finalWidth);
        currentTickerAnim.setCycleCount(Timeline.INDEFINITE);
        currentTickerAnim.setInterpolator(Interpolator.LINEAR);
        currentTickerAnim.play();
    }

    /** Créer une alerte pour la barre live avec couleurs personnalisées */
    private HBox createAlert(String icon, String text, String dotColor, String textColor) {
        HBox alert = new HBox(8);
        alert.setAlignment(Pos.CENTER);

        Label dot = new Label(icon);
        dot.setStyle("-fx-text-fill: " + dotColor + "; -fx-font-size: 10px;");

        Label alertText = new Label(text);
        alertText.setStyle("-fx-text-fill: " + textColor + "; -fx-font-size: 13px; -fx-font-weight: 600;");

        alert.getChildren().addAll(dot, alertText);
        return alert;
    }

    /** Animation dissolution (simplifiée pour stabilité) */
    private void dissolveSidebarScanlines(Runnable onDone) {
        if (sidebarRef == null) {
            if (onDone != null)
                onDone.run();
            return;
        }

        FadeTransition fade = new FadeTransition(Duration.millis(200), sidebarRef);
        fade.setToValue(0);
        fade.setOnFinished(e -> {
            if (onDone != null)
                onDone.run();
        });
        fade.play();
    }

    /** Animation créative d'apparition de la sidebar (inverse de dissolution) */
    private void appearSidebar(VBox sidebar) {
        if (sidebar == null)
            return;

        // Rendre la sidebar visible mais transparente
        sidebar.setVisible(true);
        sidebar.setManaged(true);
        sidebar.setOpacity(0);

        int nbLines = 8;
        int totalMs = 320;
        int stepMs = totalMs / nbLines;

        // Créer l'overlay avec des rectangles
        StackPane overlay = new StackPane();
        overlay.setPrefWidth(sidebar.getWidth() > 0 ? sidebar.getWidth() : 68);
        overlay.setPrefHeight(sidebar.getHeight() > 0 ? sidebar.getHeight() : 720);
        overlay.setStyle("-fx-background-color:transparent;");
        overlay.setMouseTransparent(true);

        VBox lines = new VBox(0);
        double lineH = overlay.getPrefHeight() / nbLines;
        String dissolveColor = darkMode ? "#0a0d1a" : "#f0f4f8";

        for (int i = 0; i < nbLines; i++) {
            javafx.scene.shape.Rectangle line = new javafx.scene.shape.Rectangle(
                    overlay.getPrefWidth(), lineH);
            line.setFill(Color.web(dissolveColor));
            lines.getChildren().add(line);
        }
        overlay.getChildren().add(lines);

        // Insérer l'overlay
        if (sidebar.getParent() instanceof javafx.scene.layout.HBox) {
            javafx.scene.layout.HBox parent = (javafx.scene.layout.HBox) sidebar.getParent();
            StackPane sideWrap = new StackPane(sidebar, overlay);
            sideWrap.setPrefWidth(sidebar.getPrefWidth());
            int idx = parent.getChildren().indexOf(sidebar);
            if (idx >= 0) {
                parent.getChildren().set(idx, sideWrap);
            }
        }

        // Animer l'apparition : lignes disparaissent de droite à gauche
        Timeline tl = new Timeline();
        double maxW = overlay.getPrefWidth() > 0 ? overlay.getPrefWidth() : 68;

        for (int i = 0; i < nbLines; i++) {
            javafx.scene.shape.Rectangle line = (javafx.scene.shape.Rectangle) lines.getChildren().get(i);
            int delay = i * stepMs;
            tl.getKeyFrames().addAll(
                    new KeyFrame(Duration.millis(delay),
                            new KeyValue(line.widthProperty(), maxW),
                            new KeyValue(sidebar.opacityProperty(), 0)),
                    new KeyFrame(Duration.millis(delay + stepMs),
                            new KeyValue(line.widthProperty(), 0, Interpolator.EASE_OUT),
                            new KeyValue(sidebar.opacityProperty(), 1)));
        }

        tl.setOnFinished(e -> {
            // Restaurer le parent
            if (sidebar.getParent() instanceof StackPane) {
                StackPane wrap = (StackPane) sidebar.getParent();
                if (wrap.getParent() instanceof javafx.scene.layout.HBox) {
                    javafx.scene.layout.HBox parent = (javafx.scene.layout.HBox) wrap.getParent();
                    int idx = parent.getChildren().indexOf(wrap);
                    if (idx >= 0)
                        parent.getChildren().set(idx, sidebar);
                }
            }
            sidebar.setOpacity(1);
        });
        tl.play();
    }

    // ==============================================
    // [4] SIDEBAR + NAVIGATION
    // ==============================================
    private VBox buildSidebar() {
        VBox sidebar = new VBox(0);
        sidebar.setPrefWidth(68);
        sidebar.setMinWidth(68);
        sidebar.setMaxWidth(68);

        // Couleurs adaptatives pour la sidebar
        String sidebarBg = darkMode ? BG_SIDEBAR : "#ffffff";
        String sidebarBorder = darkMode ? "rgba(255,255,255,0.07)" : "rgba(0,0,0,0.1)";

        sidebar.setStyle(
                "-fx-background-color: " + sidebarBg + ";" +
                        "-fx-border-color: " + sidebarBorder + ";" +
                        "-fx-border-width: 0 1 0 0;" +
                        (darkMode ? "" : "-fx-effect: dropshadow(gaussian,rgba(0,0,0,0.04),8,0,2,0);"));

        // -- Logo compact avec panier et titre --
        VBox logoZone = new VBox(6);
        logoZone.setAlignment(Pos.CENTER);
        logoZone.setPrefHeight(100);
        logoZone.setMinHeight(100);

        String logoZoneBorder = darkMode ? "rgba(255,255,255,0.07)" : "rgba(0,0,0,0.08)";
        logoZone.setStyle("-fx-border-color: " + logoZoneBorder + "; -fx-border-width: 0 0 1 0;");
        this.logoZoneRef = logoZone;

        // Icône panier 🛒
        Label logoIcon = new Label("\uD83D\uDED2");
        logoIcon.setStyle("-fx-font-size:32px;");
        javafx.scene.effect.DropShadow logoGlow = new javafx.scene.effect.DropShadow(14,
                Color.web("#38bdf8", darkMode ? 1.0 : 0.6));
        logoIcon.setEffect(logoGlow);
        Timeline glowAnim = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(logoGlow.radiusProperty(), 14)),
                new KeyFrame(Duration.seconds(1.8), new KeyValue(logoGlow.radiusProperty(), 3)));
        glowAnim.setAutoReverse(true);
        glowAnim.setCycleCount(Timeline.INDEFINITE);
        glowAnim.play();

        // Titre "Votre marché en ligne"
        Label logoTitle = new Label("Votre marché");
        logoTitle.setStyle("-fx-font-size: 9px; -fx-font-weight: 600; -fx-text-fill: " + PASTEL_SKY + ";");
        Label logoSubtitle = new Label("en ligne");
        logoSubtitle.setStyle("-fx-font-size: 8px; -fx-font-weight: 500; -fx-text-fill: " + TEXT_3 + ";");

        logoZone.getChildren().addAll(logoIcon, logoTitle, logoSubtitle);

        // -- Bouton Fermer (X) en haut de la sidebar --
        Button closeBtn = new Button("×");
        closeBtn.setStyle(
                "-fx-background-color: transparent;" +
                        "-fx-text-fill: " + TEXT_3 + ";" +
                        "-fx-font-size: 24px;" +
                        "-fx-padding: 0 4 0 0;" +
                        "-fx-cursor: hand;");
        closeBtn.setOnAction(e -> toggleSidebar(false));

        StackPane sidebarHeader = new StackPane(logoZone, closeBtn);
        StackPane.setAlignment(closeBtn, Pos.TOP_RIGHT);
        StackPane.setMargin(closeBtn, new Insets(2, 5, 0, 0));

        sidebar.getChildren().add(sidebarHeader);
        edu.hanouti.gui.components.SidebarNav sidebarNav = new edu.hanouti.gui.components.SidebarNav();
        this.sidebarNavRef = sidebarNav;
        sidebarNav.setOnNavigate(module -> {
            // Animation dissolution (scanlines) avant de naviguer
            dissolveSidebarScanlines(() -> {
                // Après dissolution, naviguer et cacher sidebar via la méthode centralisée
                naviguerVers(module);
                toggleSidebar(false);
            });
        });

        btnStats = new Button();
        btnConseils = new Button();
        btnCamp = new Button();

        sidebarNav.activate("statistiques");

        // Spacer
        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        // -- Zone boutons bas (icones seulement) --
        VBox dbZone = new VBox(8);
        dbZone.setAlignment(Pos.CENTER);
        dbZone.setPadding(new Insets(10, 0, 14, 0));

        String dbZoneBorder = darkMode ? "rgba(255,255,255,0.07)" : "rgba(0,0,0,0.08)";
        dbZone.setStyle("-fx-border-color: " + dbZoneBorder + "; -fx-border-width: 1 0 0 0;");
        this.dbZoneRef = dbZone;

        // Bouton PDF (icone) - couleurs adaptatives
        Button btnPdf = new Button("\u2B07");
        btnPdf.setOnAction(e -> edu.hanouti.utils.PDFExportManager.exportRapportComplet(primaryStage));
        btnPdf.setPrefSize(42, 42);
        btnPdf.setMinSize(42, 42);

        String pdfBg = darkMode ? "rgba(56,189,248,0.08)" : "rgba(56,189,248,0.1)";
        String pdfBorder = darkMode ? "rgba(56,189,248,0.2)" : "rgba(56,189,248,0.3)";

        btnPdf.setStyle(
                "-fx-background-color:" + pdfBg + ";" +
                        "-fx-text-fill:" + PASTEL_SKY + ";" +
                        "-fx-font-size:16px;" +
                        "-fx-background-radius:10;" +
                        "-fx-border-color:" + pdfBorder + ";" +
                        "-fx-border-width:1;-fx-border-radius:10;-fx-cursor:hand;");
        javafx.scene.control.Tooltip.install(btnPdf,
                new javafx.scene.control.Tooltip("Télécharger le rapport PDF (choix du dossier)"));

        // Bouton Deconnexion (icone) - couleurs adaptatives
        Button btnLogout = new Button("\u23FB");
        btnLogout.setPrefSize(42, 42);
        btnLogout.setMinSize(42, 42);

        String logoutBg = darkMode ? "rgba(220,38,38,0.08)" : "rgba(239,68,68,0.1)";
        String logoutText = darkMode ? "#f87171" : "#dc2626";
        String logoutBorder = darkMode ? "rgba(220,38,38,0.2)" : "rgba(239,68,68,0.3)";

        btnLogout.setStyle(
                "-fx-background-color:" + logoutBg + ";" +
                        "-fx-text-fill:" + logoutText + ";" +
                        "-fx-font-size:16px;" +
                        "-fx-background-radius:10;" +
                        "-fx-border-color:" + logoutBorder + ";" +
                        "-fx-border-width:1;-fx-border-radius:10;-fx-cursor:hand;");
        javafx.scene.control.Tooltip.install(btnLogout,
                new javafx.scene.control.Tooltip("Deconnexion"));
        btnLogout.setOnAction(e -> {
            javafx.application.Platform.exit();
            System.exit(0);
        });

        dbZone.getChildren().addAll(btnPdf, btnLogout);
        sidebar.getChildren().addAll(logoZone, sidebarNav, spacer, dbZone);
        return sidebar;
    }

    private boolean checkDbStatus() {
        try {
            Connection cnx = edu.hanouti.utils.MyConnection.getInstance().getCnx();
            return cnx != null && !cnx.isClosed();
        } catch (Exception e) {
            return false;
        }
    }

    private Button makeNavBtn(String icon, String title, String sub) {
        HBox content = new HBox(12);
        content.setAlignment(Pos.CENTER_LEFT);
        Label iconLbl = new Label(icon);
        iconLbl.setStyle("-fx-font-size: 18px;");
        VBox texts = new VBox(1);
        Label titleLbl = new Label(title);
        titleLbl.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: " + TEXT_1 + ";");
        Label subLbl = new Label(sub);
        subLbl.setStyle("-fx-font-size: 11px; -fx-text-fill: " + TEXT_3 + ";");
        texts.getChildren().addAll(titleLbl, subLbl);
        content.getChildren().addAll(iconLbl, texts);
        Button btn = new Button();
        btn.setGraphic(content);
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setStyle(
                "-fx-background-color: transparent;" +
                        "-fx-cursor: hand;" +
                        "-fx-padding: 11 20 11 20;" +
                        "-fx-background-radius: 10;");
        btn.setOnMouseEntered(e -> {
            if (btn != activeNavBtn)
                btn.setStyle(btn.getStyle().replace("transparent", "rgba(255,255,255,0.05)"));
        });
        btn.setOnMouseExited(e -> {
            if (btn != activeNavBtn)
                btn.setStyle(btn.getStyle().replace("rgba(255,255,255,0.05)", "transparent"));
        });
        VBox.setMargin(btn, new Insets(2, 10, 2, 10));
        return btn;
    }

    private void naviguerVers(String module) {
        // Mémoriser le module actif
        currentModule = module.toLowerCase();

        javafx.application.Platform.runLater(() -> {
            Button targetBtn;
            javafx.scene.Node nextView;
            String navId;
            String moduleDisplayName;

            switch (currentModule) {
                case "conseils":
                    targetBtn = btnConseils;
                    nextView = buildModuleConseils();
                    navId = "conseils";
                    moduleDisplayName = "Conseils";
                    break;
                case "campagnes":
                    targetBtn = btnCamp;
                    nextView = buildModuleCampagnes();
                    navId = "campagnes";
                    moduleDisplayName = "Campagnes";
                    break;
                case "marketing":
                    targetBtn = btnMarketing;
                    nextView = new edu.hanouti.modules.marketing.MarketingDashboardView()
                            .buildView(darkMode);
                    navId = "marketing";
                    moduleDisplayName = "Marketing Vendeur";
                    break;
                default:
                    currentModule = "stats"; // Normaliser
                    targetBtn = btnStats;
                    nextView = buildModuleStats();
                    navId = "statistiques";
                    moduleDisplayName = "Dashboard";
                    break;
            }

            // Mettre a jour le label module dans le topBar avec animation glisse
            if (topBarModuleLabel != null) {
                topBarModuleLabel.setTranslateX(-12);
                topBarModuleLabel.setOpacity(0);
                topBarModuleLabel.setText(moduleDisplayName);
                TranslateTransition ttt = new TranslateTransition(Duration.millis(250), topBarModuleLabel);
                ttt.setToX(0);
                ttt.setInterpolator(Interpolator.EASE_OUT);
                FadeTransition tft = new FadeTransition(Duration.millis(250), topBarModuleLabel);
                tft.setToValue(0.85);
                new ParallelTransition(ttt, tft).play();
            }

            // Synchroniser le SidebarNav
            if (sidebarNavRef != null)
                sidebarNavRef.activate(navId);

            // Afficher le contenu avec animation créative
            if (contentArea.getChildren().isEmpty()) {
                // Premier chargement : fade in simple
                contentArea.getChildren().setAll(nextView);
                nextView.setOpacity(0);
                nextView.setTranslateY(20);
                FadeTransition fadeIn = new FadeTransition(Duration.millis(400), nextView);
                fadeIn.setFromValue(0.0);
                fadeIn.setToValue(1.0);
                TranslateTransition slideIn = new TranslateTransition(Duration.millis(400), nextView);
                slideIn.setFromY(20);
                slideIn.setToY(0);
                slideIn.setInterpolator(Interpolator.EASE_OUT);
                new ParallelTransition(fadeIn, slideIn).play();
            } else {
                // Changement de module : animation slide créative
                javafx.scene.Node oldContent = contentArea.getChildren().get(0);

                // Animation sortie : slide vers la gauche + fade out
                TranslateTransition slideOut = new TranslateTransition(Duration.millis(300), oldContent);
                slideOut.setFromX(0);
                slideOut.setToX(-50);
                slideOut.setInterpolator(Interpolator.EASE_IN);
                FadeTransition fadeOut = new FadeTransition(Duration.millis(300), oldContent);
                fadeOut.setFromValue(1.0);
                fadeOut.setToValue(0.0);

                ParallelTransition exitAnim = new ParallelTransition(slideOut, fadeOut);
                final javafx.scene.Node fNextView = nextView;

                exitAnim.setOnFinished(e -> {
                    contentArea.getChildren().setAll(fNextView);

                    // Animation entrée : slide depuis la droite + fade in
                    fNextView.setOpacity(0.0);
                    fNextView.setTranslateX(50);
                    TranslateTransition slideIn = new TranslateTransition(Duration.millis(400), fNextView);
                    slideIn.setFromX(50);
                    slideIn.setToX(0);
                    slideIn.setInterpolator(Interpolator.EASE_OUT);
                    FadeTransition fadeIn = new FadeTransition(Duration.millis(400), fNextView);
                    fadeIn.setFromValue(0.0);
                    fadeIn.setToValue(1.0);

                    new ParallelTransition(slideIn, fadeIn).play();
                });
                exitAnim.play();
            }
        });
    }

    private void setActive(Button btn, javafx.scene.Node newContent) {
        if (activeNavBtn != null)
            activeNavBtn.setStyle(
                    "-fx-background-color: transparent;" +
                            "-fx-cursor: hand; -fx-padding: 11 20 11 20; -fx-background-radius: 10;");
        activeNavBtn = btn;
        btn.setStyle(
                "-fx-background-color: linear-gradient(to right, rgba(37,99,235,0.18), rgba(139,92,246,0.1));" +
                        "-fx-border-color: rgba(37,99,235,0.3); -fx-border-width: 1;" +
                        "-fx-border-radius: 10; -fx-background-radius: 10;" +
                        "-fx-cursor: hand; -fx-padding: 11 20 11 20;");

        // ANIMATION "VIEW-TRANSITION"
        if (contentArea.getChildren().isEmpty()) {
            contentArea.getChildren().setAll(newContent);
        } else {
            javafx.scene.Node oldContent = contentArea.getChildren().get(0);
            FadeTransition fadeOut = new FadeTransition(Duration.seconds(0.12), oldContent);
            fadeOut.setFromValue(1.0);
            fadeOut.setToValue(0.0);

            fadeOut.setOnFinished(e -> {
                contentArea.getChildren().setAll(newContent);
                newContent.setOpacity(0.0);

                FadeTransition fadeIn = new FadeTransition(Duration.seconds(0.2), newContent);
                fadeIn.setFromValue(0.0);
                fadeIn.setToValue(1.0);
                fadeIn.play();
            });
            fadeOut.play();
        }
    }

    private ScrollPane styledScroll(javafx.scene.Node content) {
        ScrollPane sp = new ScrollPane(content);
        sp.setFitToWidth(true);
        sp.setMinWidth(0); // Permet de réduire la taille si la fenêtre est petite
        sp.setPrefWidth(100); // Force l'utilisation de HGrow
        sp.setStyle(
                "-fx-background-color: transparent; -fx-background: " + BG_DEEP + "; -fx-border-color: transparent;");
        return sp;
    }

    // ==============================================
    // [5] MODULE STATISTIQUES — Dashboard principal
    // ==============================================
    private ScrollPane buildModuleStats() {
        VBox view = new VBox(24);
        view.setPadding(new Insets(28));
        view.setStyle("-fx-background-color: transparent;");

        // -- Donnees reelles depuis la DB --
        List<StatistiquesVentes> statsData = statsService.getData();
        int totalVendu = statsService.getTotalVendu();
        double totalRevenu = statsService.getTotalRevenu();
        double avgRetour = statsService.getTauxRetourMoyen();
        int nbProduits = statsData.size();

        String moisAnnee = java.time.LocalDate.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("MMMM yyyy", java.util.Locale.FRENCH));
        moisAnnee = moisAnnee.substring(0, 1).toUpperCase() + moisAnnee.substring(1);

        // -- 1. Header style "Centre de Decision" --
        HBox header = buildModHeader(
                "Temps Reel", "Dashboard",
                "Performances produits \u00B7 " + moisAnnee + " \u00B7 Donnees directes depuis la base",
                null, null);

        // -- 2. KPI Cards dynamiques --
        HBox kpiRow = new HBox(16);
        String bestProd = statsData.stream()
                .max(java.util.Comparator.comparingDouble(StatistiquesVentes::getRevenuTotal))
                .map(s -> s.getProduitId() != null ? s.getProduitId() : "N/A").orElse("N/A");
        String bestRetour = statsData.stream()
                .min(java.util.Comparator.comparingDouble(StatistiquesVentes::getTauxRetour))
                .map(s -> String.format("Meilleur: %s %.1f%%", s.getProduitId(), s.getTauxRetour()))
                .orElse("Moy: " + String.format("%.1f%%", avgRetour));

        // Calcul des tendances depuis la DB
        double revenuMoisPrecedent = statsService.getRevenuMoisPrecedent();
        double revenuMoisCourant = statsService.getRevenuMoisCourant();

        // Nom du mois precedent pour affichage
        String moisPrecedentNom = java.time.LocalDate.now().minusMonths(1)
                .format(java.time.format.DateTimeFormatter.ofPattern("MMMM", java.util.Locale.FRENCH));
        moisPrecedentNom = moisPrecedentNom.substring(0, 1).toUpperCase() + moisPrecedentNom.substring(1);

        int venduMoisPrecedent = statsService.getVenduMoisPrecedent();
        int venduMoisCourant = statsService.getVenduMoisCourant();

        // Tendance TOTAL VENDU avec fleche + % + nom du mois
        String tendanceVentesDisplay;
        if (venduMoisPrecedent > 0) {
            double pctV = ((double) (venduMoisCourant - venduMoisPrecedent) / venduMoisPrecedent) * 100;
            String arrow = pctV >= 0 ? "\u25B2 +" : "\u25BC ";
            tendanceVentesDisplay = String.format("%s%.1f%% vs %s (%d)", arrow, Math.abs(pctV), moisPrecedentNom,
                    venduMoisPrecedent);
        } else {
            tendanceVentesDisplay = venduMoisCourant + " unites ce mois";
        }
        // Tendance REVENU avec fleche + % + nom du mois
        String tendanceRevenuDisplay;
        if (revenuMoisPrecedent > 0) {
            double pctR = ((revenuMoisCourant - revenuMoisPrecedent) / revenuMoisPrecedent) * 100;
            String arrow = pctR >= 0 ? "\u25B2 +" : "\u25BC ";
            tendanceRevenuDisplay = String.format("%s%.1f%% vs %s", arrow, Math.abs(pctR), moisPrecedentNom);
        } else {
            tendanceRevenuDisplay = "ce mois";
        }
        // Sous-ligne EUR/USD pour REVENU TOTAL
        String revenuEurUsd = edu.hanouti.services.ExchangeRateService.format(totalRevenu, "EUR")
                + "  \u2022  "
                + edu.hanouti.services.ExchangeRateService.format(totalRevenu, "USD");
        // TAUX DE RETOUR : libelle clair
        String retourExplain = "Taux retour produit (remboursements)";

        // Couleurs des KPI selon le screenshot : Bleu, Vert, Violet, Rose
        String kpi1Color = darkMode ? "#60a5fa" : "#3b82f6"; // Bleu
        String kpi2Color = darkMode ? "#34d399" : "#10b981"; // Vert
        String kpi3Color = darkMode ? "#a78bfa" : "#8b5cf6"; // Violet
        String kpi4Color = darkMode ? "#f472b6" : "#ec4899"; // Rose

        VBox kpi1 = makeScreenshotKpi("TOTAL VENDU", String.valueOf(totalVendu), null, tendanceVentesDisplay,
                kpi1Color);
        VBox kpi2 = makeScreenshotKpi("REVENU TOTAL", String.format("%,.0f", totalRevenu), "TND", tendanceRevenuDisplay,
                kpi2Color);
        VBox kpi3 = makeScreenshotKpi("PRODUITS ACTIFS", String.valueOf(nbProduits), null,
                "\u2705 " + nbProduits + " actifs", kpi3Color);
        VBox kpi4 = makeScreenshotKpi("TAUX DE RETOUR", String.format("%.1f%%", avgRetour), null, retourExplain,
                kpi4Color);
        // Ajouter sous-ligne EUR/USD sur kpi2
        Label kpi2EurUsd = new Label(revenuEurUsd);
        kpi2EurUsd.setStyle("-fx-text-fill:#64748b; -fx-font-size:11px;");
        kpi2.getChildren().add(2, kpi2EurUsd); // apres la valeur TND
        kpiRow.getChildren().addAll(kpi1, kpi2, kpi3, kpi4);
        for (javafx.scene.Node k : kpiRow.getChildren())
            HBox.setHgrow(k, Priority.ALWAYS);
        for (int ki = 0; ki < kpiRow.getChildren().size(); ki++) {
            javafx.scene.Node card = kpiRow.getChildren().get(ki);
            card.setOpacity(0);
            card.setTranslateY(20);
            FadeTransition kft = new FadeTransition(Duration.millis(400), card);
            kft.setToValue(1.0);
            kft.setDelay(Duration.millis(ki * 80));
            TranslateTransition ktt = new TranslateTransition(Duration.millis(400), card);
            ktt.setToY(0);
            ktt.setDelay(Duration.millis(ki * 80));
            new ParallelTransition(kft, ktt).play();
        }

        // -- 3. Bande insight --
        HBox iaStrip = new HBox(12);
        iaStrip.setAlignment(Pos.CENTER_LEFT);
        iaStrip.setPadding(new Insets(12, 20, 12, 20));
        iaStrip.setStyle("-fx-background-color: rgba(56,189,248,0.06); -fx-background-radius: 12;" +
                "-fx-border-color: rgba(56,189,248,0.2); -fx-border-width: 1;");
        Circle pulse = new Circle(4, Color.web(PASTEL_SKY));
        applyPulse(pulse);
        Label iaTxt = new Label("Produit phare : " + bestProd +
                String.format("  .  Revenu total : %,.0f TND  .  Taux retour moyen : %.1f%%", totalRevenu, avgRetour));
        iaTxt.setStyle("-fx-text-fill: " + TEXT_2 + "; -fx-font-size: 13px;");
        iaStrip.getChildren().addAll(pulse, iaTxt);

        // =====================================================================
        // -- PRAYER TIMES & WEATHER WIDGETS (Premium Glassmorphism Design) --
        // =====================================================================
        HBox prayerWeatherRow = new HBox(18);
        prayerWeatherRow.setMinHeight(220);

        // ── PRAYER TIMES WIDGET ──
        VBox prayerWidget = buildPrayerWidget();
        HBox.setHgrow(prayerWidget, Priority.ALWAYS);

        // ── WEATHER WIDGET ──
        VBox weatherWidget = buildWeatherWidget();
        HBox.setHgrow(weatherWidget, Priority.ALWAYS);

        prayerWeatherRow.getChildren().addAll(prayerWidget, weatherWidget);

        // Animate entry
        prayerWidget.setOpacity(0);
        prayerWidget.setTranslateY(25);
        weatherWidget.setOpacity(0);
        weatherWidget.setTranslateY(25);
        FadeTransition pft = new FadeTransition(Duration.millis(500), prayerWidget);
        pft.setToValue(1);
        pft.setDelay(Duration.millis(200));
        TranslateTransition ptt = new TranslateTransition(Duration.millis(500), prayerWidget);
        ptt.setToY(0);
        ptt.setDelay(Duration.millis(200));
        FadeTransition wft = new FadeTransition(Duration.millis(500), weatherWidget);
        wft.setToValue(1);
        wft.setDelay(Duration.millis(350));
        TranslateTransition wtt = new TranslateTransition(Duration.millis(500), weatherWidget);
        wtt.setToY(0);
        wtt.setDelay(Duration.millis(350));
        new ParallelTransition(pft, ptt, wft, wtt).play();

        // -- 4. Filtres Pill Glowing --
        String[] filterLabels = { "Par Produit", "Ce Mois", "Par Revenu" };
        HBox pillRow = new HBox(10);
        pillRow.setAlignment(Pos.CENTER_LEFT);
        Button[] pillBtns = new Button[filterLabels.length];

        // Couleurs adaptatives pour les pills - CONTRASTE AMÉLIORÉ
        String pillActiveBg = darkMode ? "#1e3a5f" : "#3b82f6"; // Plus saturé
        String pillActiveText = darkMode ? "#38bdf8" : "#ffffff"; // Blanc en mode clair
        String pillActiveBorder = darkMode ? "#38bdf8" : "#2563eb";
        String pillActiveGlow = darkMode ? "dropshadow(gaussian,#38bdf8,12,0.5,0,0)"
                : "dropshadow(gaussian,rgba(59,130,246,0.4),10,0.4,0,0)";
        String pillInactiveBg = darkMode ? "rgba(13,21,38,0.5)" : "rgba(248,250,252,0.6)"; // Plus transparent
        String pillInactiveText = darkMode ? "#64748b" : "#94a3b8";
        String pillInactiveBorder = darkMode ? "rgba(30,42,64,0.6)" : "rgba(226,232,240,0.8)";

        for (int pi = 0; pi < filterLabels.length; pi++) {
            Button pill = new Button(filterLabels[pi]);
            pill.setPrefHeight(34);
            pill.setPadding(new Insets(0, 20, 0, 20));
            boolean isFirst = pi == 0;
            pill.setStyle(isFirst
                    ? "-fx-background-color:" + pillActiveBg + ";-fx-text-fill:" + pillActiveText
                            + ";-fx-font-weight:900;-fx-font-size:13px;" +
                            "-fx-background-radius:99;-fx-border-color:" + pillActiveBorder
                            + ";-fx-border-width:2;-fx-border-radius:99;-fx-cursor:hand;" +
                            "-fx-effect:" + pillActiveGlow + ";"
                    : "-fx-background-color:" + pillInactiveBg + ";-fx-text-fill:" + pillInactiveText
                            + ";-fx-font-size:12px;-fx-font-weight:500;" +
                            "-fx-background-radius:99;-fx-border-color:" + pillInactiveBorder
                            + ";-fx-border-width:1;-fx-border-radius:99;-fx-cursor:hand;-fx-opacity:0.7;");
            pillBtns[pi] = pill;
        }

        // Graphe + hex
        VBox barCont = new VBox(12);
        barCont.setStyle("-fx-background-color: " + BG_CARD + "; -fx-background-radius: 16; -fx-padding: 22;");
        HBox.setHgrow(barCont, Priority.ALWAYS);
        Label barTit = new Label("REVENUS PAR PRODUIT (TND)");
        barTit.setStyle("-fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold;");
        StackPane chartHolder = new StackPane(buildRealBarChart(statsData, "Par Produit"));
        barCont.getChildren().addAll(barTit, chartHolder);

        VBox hexCont = new VBox(12);
        hexCont.setPrefWidth(340);
        hexCont.setStyle("-fx-background-color: " + BG_CARD + "; -fx-background-radius: 16; -fx-padding: 22;");
        Label hexTit = new Label("REPARTITION STRATEGIQUE");
        hexTit.setStyle("-fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold;");
        StackPane hexHolder = new StackPane();
        updateHexFromDB(hexHolder, statsData);
        hexCont.getChildren().addAll(hexTit, hexHolder);

        HBox chartsRow = new HBox(18, barCont, hexCont);

        // Actions filtres - graphe + hexagones changent ensemble
        for (int pi = 0; pi < filterLabels.length; pi++) {
            final String lbl = filterLabels[pi];
            final int idx = pi;
            pillBtns[pi].setOnAction(ev -> {
                // Style pills avec couleurs adaptatives
                for (int j = 0; j < pillBtns.length; j++) {
                    pillBtns[j].setStyle(j == idx
                            ? "-fx-background-color:" + pillActiveBg + ";-fx-text-fill:" + pillActiveText
                                    + ";-fx-font-weight:900;-fx-font-size:13px;" +
                                    "-fx-background-radius:99;-fx-border-color:" + pillActiveBorder
                                    + ";-fx-border-width:2;-fx-border-radius:99;-fx-cursor:hand;" +
                                    "-fx-effect:" + pillActiveGlow + ";"
                            : "-fx-background-color:" + pillInactiveBg + ";-fx-text-fill:" + pillInactiveText
                                    + ";-fx-font-size:12px;-fx-font-weight:500;" +
                                    "-fx-background-radius:99;-fx-border-color:" + pillInactiveBorder
                                    + ";-fx-border-width:1;-fx-border-radius:99;-fx-cursor:hand;-fx-opacity:0.7;");
                }
                // Mettre a jour le graphe
                FadeTransition fOut = new FadeTransition(Duration.millis(150), chartHolder);
                fOut.setToValue(0);
                fOut.setOnFinished(done -> {
                    chartHolder.getChildren().setAll(buildRealBarChart(statsData, lbl));
                    barTit.setText(lbl.equals("Par Produit") ? "REVENUS PAR PRODUIT (TND)"
                            : lbl.equals("Par Semaine") ? "REVENUS PAR SEMAINE (TND)"
                                    : "CLASSEMENT PAR REVENU (TND)");
                    FadeTransition fIn = new FadeTransition(Duration.millis(250), chartHolder);
                    fIn.setToValue(1);
                    fIn.play();
                });
                fOut.play();

                // Mettre a jour les hexagones selon le filtre
                new Thread(() -> {
                    List<java.util.Map<String, Object>> hexData;
                    if (lbl.equals("Ce Mois")) {
                        hexData = statsService.getTopProduitsMoisCourant();
                    } else if (lbl.equals("Par Revenu")) {
                        hexData = statsService.getTopProduitsByRevenuDesc();
                    } else {
                        hexData = statsService.getTopProduitsByRevenu();
                    }
                    final List<java.util.Map<String, Object>> finalData = hexData;
                    Platform.runLater(() -> updateHexFromMapList(hexHolder, finalData));
                }).start();
            });
            pillRow.getChildren().add(pillBtns[pi]);
        }

        // -- 5. Tableau performances --
        VBox tableSection = new VBox(12);
        Label tableTitle = new Label("Détails des Performances par Produit");
        tableTitle.setStyle("-fx-text-fill: " + PASTEL_SKY + "; -fx-font-weight: bold; -fx-font-size: 14px;");
        javafx.scene.layout.FlowPane cardsPane = new javafx.scene.layout.FlowPane(14, 14);
        String[] cardColors = getCardColors(); // Utilise les couleurs adaptées au thème
        for (int i = 0; i < statsData.size() && i < 6; i++) {
            StatistiquesVentes s = statsData.get(i);
            String ref = s.getProduitId() != null ? s.getProduitId() : "REF-" + i;
            String rev = String.format("%,.0f TND", s.getRevenuTotal());
            String ret = String.format("%.1f%%", s.getTauxRetour());
            String cls = s.getClassement() != null && !s.getClassement().isEmpty() ? s.getClassement() : "-";
            cardsPane.getChildren().add(buildPerfCard(ref, rev, ret, String.valueOf(s.getTotalVendu()), cls,
                    cardColors[i % cardColors.length]));
        }
        for (int i = 0; i < cardsPane.getChildren().size(); i++) {
            javafx.scene.Node card = cardsPane.getChildren().get(i);
            card.setOpacity(0);
            card.setTranslateY(20);
            FadeTransition ft = new FadeTransition(Duration.millis(450), card);
            ft.setToValue(1);
            ft.setDelay(Duration.millis(i * 100));
            TranslateTransition tt = new TranslateTransition(Duration.millis(450), card);
            tt.setToY(0);
            tt.setDelay(Duration.millis(i * 100));
            new ParallelTransition(ft, tt).play();
        }
        tableSection.getChildren().addAll(tableTitle, cardsPane);

        // -- 6. Alertes dynamiques depuis la DB --
        VBox alertSection = buildAlertsSection();

        view.getChildren().addAll(header, kpiRow, iaStrip, prayerWeatherRow, pillRow, chartsRow, tableSection,
                alertSection);
        return styledScroll(view);
    }

    // ── [5.1] Alertes IA dynamiques ──────────────
    /** Alertes dynamiques depuis alerte_ia, conseils_ia, campagne_marketing */
    private VBox buildAlertsSection() {
        VBox section = new VBox(10);

        // ── En-tête style screenshot ──
        HBox titleRow = new HBox(10);
        titleRow.setAlignment(Pos.CENTER_LEFT);

        Label title = new Label("Alertes");
        title.setStyle("-fx-text-fill: " + TEXT_1 + "; -fx-font-weight: bold; -fx-font-size: 16px;");
        title.setOpacity(0);
        FadeTransition titleFt = new FadeTransition(Duration.millis(500), title);
        titleFt.setToValue(1);
        titleFt.play();

        // Badge rouge avec le nombre
        javafx.scene.layout.StackPane badge = new javafx.scene.layout.StackPane();
        badge.setStyle("-fx-background-color:#ef4444;-fx-background-radius:99;");
        badge.setPrefSize(22, 22);
        badge.setMinSize(22, 22);
        Label badgeNb = new Label("1");
        badgeNb.setStyle("-fx-text-fill:white;-fx-font-size:11px;-fx-font-weight:bold;");
        badge.getChildren().add(badgeNb);

        Region hSp = new Region();
        HBox.setHgrow(hSp, Priority.ALWAYS);

        Button btnVoirTous = new Button("Voir tous \u2192");
        String btnColor = darkMode ? "#38bdf8" : "#2563eb";
        btnVoirTous.setStyle("-fx-background-color:transparent;-fx-text-fill:" + btnColor + ";" +
                "-fx-font-size:13px;-fx-cursor:hand;-fx-border-color:transparent;");
        btnVoirTous.setOnAction(e -> naviguerVers("conseils"));

        titleRow.getChildren().addAll(title, badge, hSp, btnVoirTous);
        section.getChildren().add(titleRow);

        // Collecter toutes les alertes
        java.util.List<HBox> allAlerts = new java.util.ArrayList<>();

        // ── Alerte 1 : depuis alerte_ia ──
        try {
            String sql = "SELECT message, niveau, score_sante, created_at " +
                    "FROM alerte_ia WHERE id_vendeur = 1 " +
                    "ORDER BY created_at DESC LIMIT 3";
            java.sql.Statement st = edu.hanouti.utils.MyConnection.getConnection().createStatement();
            java.sql.ResultSet rs = st.executeQuery(sql);
            while (rs.next()) {
                String msg = rs.getString("message");
                String niveau = rs.getString("niveau");
                int score = rs.getInt("score_sante");
                java.sql.Timestamp ts = rs.getTimestamp("created_at");
                String temps = "-";
                if (ts != null) {
                    long diff = System.currentTimeMillis() - ts.getTime();
                    long heures = diff / 3600000;
                    if (heures < 1)
                        temps = "a l'instant";
                    else if (heures < 24)
                        temps = "il y a " + heures + "h";
                    else
                        temps = "il y a " + (heures / 24) + "j";
                }
                String col = niveau != null && niveau.toUpperCase().contains("CRIT") ? PASTEL_ROSE
                        : niveau != null && niveau.toUpperCase().contains("WARN") ? GOLD : NEON_GREEN;
                String detail = msg != null ? msg : "Score sante : " + score;
                allAlerts.add(buildAlertRow("Score " + score + " \u2014 " + detail, temps, col, allAlerts.size()));
            }
            if (allAlerts.isEmpty()) {
                String sql2 = "SELECT reference, taux_retour, quantite_vendue FROM statistiques_ventes ORDER BY taux_retour DESC LIMIT 1";
                java.sql.ResultSet rs2 = st.executeQuery(sql2);
                if (rs2.next()) {
                    String ref = rs2.getString("reference");
                    double ret = rs2.getDouble("taux_retour");
                    int qte = rs2.getInt("quantite_vendue");
                    allAlerts.add(
                            buildAlertRow(ref + " taux retour " + String.format("%.1f%%", ret) + ", " + qte + " ventes",
                                    "recent", GOLD, 0));
                }
            }
        } catch (Exception e) {
            System.err.println("Erreur alerte_ia: " + e.getMessage());
        }

        // ── Alerte 2 : Conseils non appliques ──
        try {
            String sql = "SELECT COUNT(*) as nb, MIN(date_genere) as plus_ancien FROM conseils_ia WHERE etat = 'NOUVEAU'";
            java.sql.Statement st = edu.hanouti.utils.MyConnection.getConnection().createStatement();
            java.sql.ResultSet rs = st.executeQuery(sql);
            if (rs.next()) {
                int nb = rs.getInt("nb");
                String plusAncien = rs.getString("plus_ancien");
                int jours = 0;
                if (plusAncien != null) {
                    try {
                        java.time.LocalDate d = java.time.LocalDate.parse(plusAncien.substring(0, 10));
                        jours = (int) java.time.temporal.ChronoUnit.DAYS.between(d, java.time.LocalDate.now());
                    } catch (Exception ignored) {
                    }
                }
                String temps = jours < 1 ? "aujourd'hui" : "il y a " + jours + "j";
                String msg = nb + " conseil" + (nb > 1 ? "s" : "") + " non applique" + (nb > 1 ? "s" : "") + " depuis "
                        + jours + " jours";
                String col = nb > 5 ? PASTEL_ROSE : nb > 0 ? GOLD : NEON_GREEN;
                allAlerts.add(buildAlertRow(msg, temps, col, allAlerts.size()));
            }
        } catch (Exception e) {
            System.err.println("Erreur conseils_ia: " + e.getMessage());
        }

        // ── Alerte 3 : Meilleure campagne active ──
        try {
            String sql = "SELECT nom, score_ia, budget_alloue, budget_depense FROM campagne_marketing WHERE statut = 'ACTIVE' ORDER BY score_ia DESC LIMIT 1";
            java.sql.Statement st = edu.hanouti.utils.MyConnection.getConnection().createStatement();
            java.sql.ResultSet rs = st.executeQuery(sql);
            if (rs.next()) {
                String nom = rs.getString("nom");
                double score = rs.getDouble("score_ia");
                double bud = rs.getDouble("budget_alloue");
                double dep = rs.getDouble("budget_depense");
                double roi = bud > 0 ? (dep / bud) * 100 : 0;
                String msg = nom + " \u2014 Score " + String.format("%.1f", score) + "/10, budget "
                        + String.format("%.0f%%", roi);
                allAlerts.add(buildAlertRow(msg, "Active", NEON_GREEN, allAlerts.size()));
            }
        } catch (Exception e) {
            System.err.println("Erreur campagne: " + e.getMessage());
        }

        // Afficher seulement la 1ère alerte
        if (!allAlerts.isEmpty()) {
            section.getChildren().add(allAlerts.get(0));
        }

        // Mettre à jour le badge
        badgeNb.setText(String.valueOf(allAlerts.size()));

        // "Voir tous" affiche les alertes restantes inline
        final boolean[] expanded = { false };
        btnVoirTous.setOnAction(e -> {
            if (!expanded[0]) {
                expanded[0] = true;
                btnVoirTous.setText("Masquer \u2191");
                for (int i = 1; i < allAlerts.size(); i++) {
                    HBox row = allAlerts.get(i);
                    row.setOpacity(0);
                    row.setTranslateX(-20);
                    section.getChildren().add(row);
                    FadeTransition ft = new FadeTransition(Duration.millis(350), row);
                    ft.setToValue(1);
                    ft.setDelay(Duration.millis((i - 1) * 80));
                    TranslateTransition tt = new TranslateTransition(Duration.millis(350), row);
                    tt.setToX(0);
                    tt.setDelay(Duration.millis((i - 1) * 80));
                    new ParallelTransition(ft, tt).play();
                }
            } else {
                expanded[0] = false;
                btnVoirTous.setText("Voir tous \u2192");
                for (int i = allAlerts.size() - 1; i >= 1; i--) {
                    section.getChildren().remove(allAlerts.get(i));
                }
            }
        });

        return section;
    }

    /** Ligne d'alerte style screenshot — point coloré + texte + temps */
    // ── [5.2] Widgets Prière & Météo ─────────────
    /**
     * Widget des horaires de prière — design 7anouti Premium avec animations
     */
    private VBox buildPrayerWidget() {
        VBox widget = new VBox(0);
        widget.setPadding(new Insets(18));
        widget.setStyle(
            "-fx-background-color: " + BG_CARD + ";" +
            "-fx-background-radius: 16;" +
            "-fx-border-color: " + (darkMode ? "#1e2a38" : "#dde3ec") + ";" +
            "-fx-border-width: 1;" +
            "-fx-border-radius: 16;"
        );

        // Titre avec icône lune
        HBox titleRow = new HBox(8);
        titleRow.setAlignment(Pos.CENTER_LEFT);
        Label moonIcon = new Label("\uD83C\uDF19");
        moonIcon.setStyle("-fx-font-size: 14px;");
        Label title = new Label("Horaires de Prière");
        title.setStyle("-fx-text-fill: #2eb87a; -fx-font-size: 13px; -fx-font-weight: 800;");
        titleRow.getChildren().addAll(moonIcon, title);

        Label hijriLbl = new Label("Chargement...");
        hijriLbl.setStyle("-fx-text-fill: #2eb87a; -fx-font-size: 11px; -fx-padding: 2 0 12 0;");

        VBox timesContainer = new VBox(4);

        widget.getChildren().addAll(titleRow, hijriLbl, timesContainer);

        new Thread(() -> {
            Map<String, String> times = prayerService.getTodayPrayerTimes();
            String nextPrayer = prayerService.getNextPrayer(times);
            String hijri = times.getOrDefault("_hijriDate", "1445") + " · " + times.getOrDefault("_hijriMonth", "");

            // Déterminer la prière ACTIVE (celle en cours = la précédente à la suivante)
            String[] allPrayers = { "Fajr", "Dhuhr", "Asr", "Maghrib", "Isha" };
            String activePrayer = allPrayers[allPrayers.length - 1]; // défaut = Isha
            for (int i = 0; i < allPrayers.length; i++) {
                if (allPrayers[i].equalsIgnoreCase(nextPrayer)) {
                    activePrayer = i > 0 ? allPrayers[i - 1] : allPrayers[allPrayers.length - 1];
                    break;
                }
            }
            final String activeP = activePrayer;
            final String nextP = nextPrayer;

            Platform.runLater(() -> {
                hijriLbl.setText(hijri);
                timesContainer.getChildren().clear();

                String[] prayerOrder = { "Fajr", "Dhuhr", "Asr", "Maghrib", "Isha" };
                int[] delay = {0};
                for (String p : prayerOrder) {
                    if (!times.containsKey(p)) continue;

                    boolean isActive = p.equalsIgnoreCase(activeP);
                    boolean isNext   = p.equalsIgnoreCase(nextP);

                    HBox row = new HBox();
                    row.setAlignment(Pos.CENTER_LEFT);
                    row.setPadding(new Insets(7, 10, 7, isActive ? 7 : 10));

                    if (isActive) {
                        row.setStyle(
                            "-fx-background-color: " + (darkMode ? "#0f2a1e" : "#e6f7f0") + ";" +
                            "-fx-background-radius: 0 8 8 0;" +
                            "-fx-border-color: #2eb87a;" +
                            "-fx-border-width: 0 0 0 3;" +
                            "-fx-border-radius: 0 8 8 0;"
                        );
                        // Point animé clignotant
                        javafx.scene.shape.Circle dot = new javafx.scene.shape.Circle(4, Color.web("#2eb87a"));
                        FadeTransition ftDot = new FadeTransition(Duration.millis(1200), dot);
                        ftDot.setFromValue(1.0);
                        ftDot.setToValue(0.15);
                        ftDot.setCycleCount(Animation.INDEFINITE);
                        ftDot.setAutoReverse(true);
                        ftDot.play();
                        row.getChildren().add(dot);
                        HBox.setMargin(dot, new Insets(0, 6, 0, 0));
                    } else {
                        row.setStyle("-fx-background-radius: 8;");
                    }

                    Label name = new Label(p);
                    name.setStyle(
                        "-fx-text-fill: " + (isActive ? (darkMode ? "#e0f5ec" : "#0f5132") : TEXT_2) + ";" +
                        "-fx-font-weight: " + (isActive ? "800" : "normal") + ";" +
                        "-fx-font-size: 13px;"
                    );

                    Region sp = new Region();
                    HBox.setHgrow(sp, Priority.ALWAYS);

                    Label timeLabel = new Label(times.get(p));
                    timeLabel.setStyle(
                        "-fx-text-fill: " + (isActive ? "#2eb87a" : TEXT_3) + ";" +
                        "-fx-font-weight: " + (isActive ? "800" : "normal") + ";" +
                        "-fx-font-size: 13px;"
                    );

                    // Badge countdown pour la prochaine prière uniquement
                    if (isNext) {
                        try {
                            java.time.LocalTime now = java.time.LocalTime.now();
                            String[] parts = times.get(p).split(":");
                            java.time.LocalTime pTime = java.time.LocalTime.of(
                                Integer.parseInt(parts[0]), Integer.parseInt(parts[1]));
                            long diffMin = java.time.Duration.between(now, pTime).toMinutes();
                            if (diffMin < 0) diffMin += 1440;
                            String cdText = diffMin >= 60
                                ? String.format("dans %dh%02d", diffMin / 60, diffMin % 60)
                                : String.format("dans %d min", diffMin);
                            Label cd = new Label(cdText);
                            cd.setStyle(
                                "-fx-background-color: " + (darkMode ? "#0d1f30" : "#dbeafe") + ";" +
                                "-fx-text-fill: " + PASTEL_SKY + ";" +
                                "-fx-font-size: 10px;" +
                                "-fx-font-weight: 800;" +
                                "-fx-background-radius: 99;" +
                                "-fx-padding: 2 8 2 8;"
                            );
                            FadeTransition cdFade = new FadeTransition(Duration.millis(1400), cd);
                            cdFade.setFromValue(1.0);
                            cdFade.setToValue(0.3);
                            cdFade.setCycleCount(Animation.INDEFINITE);
                            cdFade.setAutoReverse(true);
                            cdFade.play();
                            row.getChildren().addAll(name, sp, timeLabel, cd);
                            HBox.setMargin(cd, new Insets(0, 0, 0, 8));
                        } catch (Exception ex) {
                            row.getChildren().addAll(name, sp, timeLabel);
                        }
                    } else {
                        row.getChildren().addAll(name, sp, timeLabel);
                    }

                    // Animation slide-in décalée
                    row.setOpacity(0);
                    row.setTranslateX(-10);
                    FadeTransition ft = new FadeTransition(Duration.millis(350), row);
                    ft.setFromValue(0); ft.setToValue(1);
                    ft.setDelay(Duration.millis(delay[0] * 60));
                    TranslateTransition tt = new TranslateTransition(Duration.millis(350), row);
                    tt.setFromX(-10); tt.setToX(0);
                    tt.setDelay(Duration.millis(delay[0] * 60));
                    ft.play(); tt.play();
                    delay[0]++;

                    timesContainer.getChildren().add(row);
                }
            });
        }).start();

        return widget;
    }

    /**
     * Widget météo — design 7anouti Premium avec animations
     */
    private VBox buildWeatherWidget() {
        VBox widget = new VBox(12);
        widget.setPadding(new Insets(18));
        widget.setStyle(
            "-fx-background-color: " + BG_CARD + ";" +
            "-fx-background-radius: 16;" +
            "-fx-border-color: " + (darkMode ? "#1e2a38" : "#dde3ec") + ";" +
            "-fx-border-width: 1;" +
            "-fx-border-radius: 16;"
        );

        // Titre
        HBox titleRow = new HBox(8);
        titleRow.setAlignment(Pos.CENTER_LEFT);
        Label cloudIcon = new Label("\u26C5");
        cloudIcon.setStyle("-fx-font-size: 14px;");
        Label title = new Label("Météo Tunis");
        title.setStyle("-fx-text-fill: " + PASTEL_SKY + "; -fx-font-size: 13px; -fx-font-weight: 800;");
        titleRow.getChildren().addAll(cloudIcon, title);

        VBox content = new VBox(12);
        Label loading = new Label("Récupération de la météo...");
        loading.setStyle("-fx-text-fill: " + TEXT_3 + "; -fx-font-size: 12px;");
        content.getChildren().add(loading);

        widget.getChildren().addAll(titleRow, content);

        new Thread(() -> {
            Map<String, Object> data = weatherService.getCurrentWeather();
            Platform.runLater(() -> {
                content.getChildren().clear();
                if (data.isEmpty()) {
                    content.getChildren().add(new Label("Météo indisponible"));
                    return;
                }

                double temp = (double) data.get("temp");
                int code = (int) data.get("code");
                double hum = (double) data.get("humidity");
                double wind = (double) data.get("wind");
                double apparent = data.containsKey("apparent_temp") ? (double) data.get("apparent_temp") : temp - 1;

                // Icône météo avec animation flottante
                Label icon = new Label(edu.hanouti.services.WeatherService.getWeatherEmoji(code));
                icon.setStyle("-fx-font-size: 38px;");
                TranslateTransition floatAnim = new TranslateTransition(Duration.millis(2200), icon);
                floatAnim.setByY(-6);
                floatAnim.setCycleCount(Animation.INDEFINITE);
                floatAnim.setAutoReverse(true);
                floatAnim.play();

                VBox tempBox = new VBox(2);
                tempBox.setAlignment(Pos.CENTER_LEFT);
                Label tempLbl = new Label(String.format("%.1f°C", temp));
                tempLbl.setStyle("-fx-text-fill: " + (darkMode ? "#e8f4ff" : "#0f172a") + "; -fx-font-size: 28px; -fx-font-weight: 900;");
                Label desc = new Label(edu.hanouti.services.WeatherService.getWeatherDescription(code)
                    + " · ressenti " + String.format("%.0f°C", apparent));
                desc.setStyle("-fx-text-fill: " + PASTEL_SKY + "; -fx-font-size: 11px;");
                tempBox.getChildren().addAll(tempLbl, desc);

                HBox mainRow = new HBox(14);
                mainRow.setAlignment(Pos.CENTER_LEFT);
                mainRow.getChildren().addAll(icon, tempBox);

                // Grille stats 2x2
                GridPane statsGrid = new GridPane();
                statsGrid.setHgap(8);
                statsGrid.setVgap(8);

                // Sunrise/sunset si dispo
                String sunriseVal = data.containsKey("sunrise") ? (String) data.get("sunrise") : "--:--";
                String sunsetVal  = data.containsKey("sunset")  ? (String) data.get("sunset")  : "--:--";

                String[][] statItems = {
                    { "💧", "Humidité",   String.format("%.0f%%", hum) },
                    { "💨", "Vent",       String.format("%.1f km/h", wind) },
                    { "🌅", "Lever",      sunriseVal },
                    { "🌇", "Coucher",    sunsetVal }
                };

                String statBg     = darkMode ? "#0d1520" : "#f1f5f9";
                String statBorder = darkMode ? "#1e2a38" : "#dde3ec";

                for (int i = 0; i < statItems.length; i++) {
                    VBox cell = new VBox(2);
                    cell.setPadding(new Insets(7, 10, 7, 10));
                    cell.setStyle(
                        "-fx-background-color: " + statBg + ";" +
                        "-fx-border-color: " + statBorder + ";" +
                        "-fx-border-width: 1;" +
                        "-fx-background-radius: 8;" +
                        "-fx-border-radius: 8;"
                    );
                    Label lbl = new Label(statItems[i][0] + " " + statItems[i][1]);
                    lbl.setStyle("-fx-text-fill: " + TEXT_3 + "; -fx-font-size: 10px;");
                    Label val = new Label(statItems[i][2]);
                    val.setStyle("-fx-text-fill: " + (darkMode ? "#c8dff0" : "#1e3a5c") + "; -fx-font-size: 14px; -fx-font-weight: 800;");
                    cell.getChildren().addAll(lbl, val);
                    GridPane.setHgrow(cell, Priority.ALWAYS);
                    statsGrid.add(cell, i % 2, i / 2);
                }

                // Animation fade-in sur content
                content.setOpacity(0);
                content.getChildren().addAll(mainRow, statsGrid);
                FadeTransition fadeContent = new FadeTransition(Duration.millis(500), content);
                fadeContent.setFromValue(0);
                fadeContent.setToValue(1);
                fadeContent.play();
            });
        }).start();

        return widget;
    }

    private VBox buildWeatherDetail(String label, String value, String icon) {
        VBox box = new VBox(2);
        box.setAlignment(Pos.CENTER);
        Label l = new Label(label);
        l.setStyle("-fx-text-fill: " + TEXT_3
                + "; -fx-font-size: 10px; -fx-font-weight: 900; -fx-text-transform: uppercase;");
        Label v = new Label(icon + " " + value);
        v.setStyle("-fx-text-fill: " + TEXT_1 + "; -fx-font-size: 13px; -fx-font-weight: 900;");
        box.getChildren().addAll(v, l);
        return box;
    }

    private Region spacer() {
        Region r = new Region();
        HBox.setHgrow(r, Priority.ALWAYS);
        return r;
    }

    private HBox buildAlertRow(String detail, String temps, String color, int delay) {
        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(12, 16, 12, 16));

        // Style adapté au thème
        String bgColor = darkMode ? "#0d1526" : "#f8fafc";
        String borderColor = darkMode ? "rgba(255,255,255,0.05)" : "rgba(0,0,0,0.07)";
        row.setStyle(
                "-fx-background-color: " + bgColor + ";" +
                        "-fx-background-radius: 10;" +
                        "-fx-border-color: " + borderColor + ";" +
                        "-fx-border-width: 1;" +
                        "-fx-border-radius: 10;");

        // Point coloré avec effet adapté au thème
        Circle dot = new Circle(5, Color.web(color));
        if (darkMode) {
            dot.setEffect(new DropShadow(8, Color.web(color)));
        } else {
            dot.setEffect(new DropShadow(4, Color.web(color, 0.4)));
        }
        applyPulse(dot);

        // Texte principal — tronque a 60 caracteres
        String detailTronque = detail != null && detail.length() > 60
                ? detail.substring(0, 60)
                : detail;
        Label lbl = new Label(detailTronque);
        lbl.setStyle("-fx-text-fill: " + TEXT_1 + "; -fx-font-size: 13px; -fx-font-weight: bold;");
        lbl.setWrapText(false);
        // Tooltip avec texte complet si tronque
        if (detail != null && detail.length() > 60) {
            Tooltip tip = new Tooltip(detail);
            tip.setStyle("-fx-font-size:12px;-fx-background-color:" + (darkMode ? "#0d1117" : "#ffffff")
                    + ";-fx-text-fill:" + TEXT_1 + ";");
            tip.setWrapText(true);
            tip.setMaxWidth(400);
            Tooltip.install(lbl, tip);
        }
        HBox.setHgrow(lbl, Priority.ALWAYS);

        // Temps
        Label tps = new Label(temps);
        tps.setStyle("-fx-text-fill: " + TEXT_3 + "; -fx-font-size: 12px;");

        row.getChildren().addAll(dot, lbl, tps);

        // Animation slide depuis la gauche
        row.setOpacity(0);
        row.setTranslateX(-20);
        FadeTransition ft = new FadeTransition(Duration.millis(400), row);
        ft.setToValue(1);
        ft.setDelay(Duration.millis(delay * 100));
        TranslateTransition tt = new TranslateTransition(Duration.millis(400), row);
        tt.setToX(0);
        tt.setDelay(Duration.millis(delay * 100));
        tt.setInterpolator(Interpolator.EASE_OUT);
        new ParallelTransition(ft, tt).play();

        // Hover avec couleurs adaptées
        String hoverBg = darkMode ? "rgba(56,189,248,0.08)" : "rgba(56,189,248,0.06)";
        String hoverBorder = darkMode ? "rgba(56,189,248,0.25)" : "rgba(56,189,248,0.2)";
        row.setOnMouseEntered(e -> row.setStyle(
                "-fx-background-color:" + hoverBg + ";-fx-background-radius:10;" +
                        "-fx-border-color:" + hoverBorder + ";-fx-border-width:1;-fx-border-radius:10;"));
        row.setOnMouseExited(e -> row.setStyle(
                "-fx-background-color:" + bgColor + ";-fx-background-radius:10;" +
                        "-fx-border-color:" + borderColor + ";-fx-border-width:1;-fx-border-radius:10;"));

        return row;
    }

    // ── [5.3] Graphiques & Charts ────────────────
    /** Graphe barres avec VRAIES donnees DB */
    private VBox buildRealBarChart(List<StatistiquesVentes> data, String mode) {
        javafx.scene.chart.CategoryAxis xAxis = new javafx.scene.chart.CategoryAxis();
        javafx.scene.chart.NumberAxis yAxis = new javafx.scene.chart.NumberAxis();
        xAxis.setTickLabelFill(Color.web(TEXT_3));
        yAxis.setTickLabelFill(Color.web(TEXT_3));
        xAxis.setStyle("-fx-tick-label-fill:" + TEXT_3 + ";-fx-border-color:transparent;");
        yAxis.setStyle("-fx-tick-label-fill:" + TEXT_3 + ";-fx-border-color:transparent;");
        xAxis.setTickLabelRotation(-30);
        yAxis.setTickLabelFormatter(new javafx.util.StringConverter<Number>() {
            @Override
            public String toString(Number n) {
                double v = n.doubleValue();
                return v == 0 ? "0" : v >= 1000 ? String.format("%.0fK", v / 1000) : String.format("%.0f", v);
            }

            @Override
            public Number fromString(String s) {
                return null;
            }
        });
        javafx.scene.chart.BarChart<String, Number> chart = new javafx.scene.chart.BarChart<>(xAxis, yAxis);
        chart.setLegendVisible(false);
        chart.setAnimated(false);
        chart.setStyle(
                "-fx-background-color:transparent;-fx-plot-background-color:transparent;-fx-bar-gap:4;-fx-category-gap:14;");

        // Construire les données selon le mode
        java.util.Map<String, Double> revByProd = new java.util.LinkedHashMap<>();
        if (mode.equals("Par Semaine")) {
            for (StatistiquesVentes s : data)
                revByProd.merge("S" + s.getSemaine(), s.getRevenuTotal(), Double::sum);
        } else {
            for (StatistiquesVentes s : data) {
                String ref = s.getProduitId() != null ? s.getProduitId().replace("REF-", "") : "?";
                revByProd.merge(ref, s.getRevenuTotal(), Double::sum);
            }
            if (mode.equals("Par Revenu")) {
                List<java.util.Map.Entry<String, Double>> sorted = new java.util.ArrayList<>(revByProd.entrySet());
                sorted.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));
                revByProd = new java.util.LinkedHashMap<>();
                for (java.util.Map.Entry<String, Double> e : sorted)
                    revByProd.put(e.getKey(), e.getValue());
            }
        }

        // Largeur dynamique selon nb de barres
        int nbBars = revByProd.size();
        double barWidth = Math.max(50, Math.min(90, 600.0 / Math.max(nbBars, 1)));
        double chartWidth = Math.max(500, nbBars * (barWidth + 18) + 80);
        chart.setPrefWidth(chartWidth);
        chart.setPrefHeight(260);

        javafx.scene.chart.XYChart.Series<String, Number> series = new javafx.scene.chart.XYChart.Series<>();
        String[] barColors = getBarColors(); // Utilise les couleurs adaptées au thème
        for (java.util.Map.Entry<String, Double> e : revByProd.entrySet())
            series.getData().add(new javafx.scene.chart.XYChart.Data<>(e.getKey(), e.getValue()));
        chart.getData().add(series);

        final String[] colors = barColors;
        final java.util.Map<String, Double> finalRevByProd = revByProd;

        Platform.runLater(() -> {
            for (int i = 0; i < series.getData().size(); i++) {
                javafx.scene.chart.XYChart.Data<String, Number> d = series.getData().get(i);
                if (d.getNode() != null) {
                    String col = colors[i % colors.length];
                    d.getNode().setStyle("-fx-bar-fill:" + col + ";-fx-background-radius:4 4 0 0;");
                    // Tooltip avec valeur TND
                    double val = d.getYValue().doubleValue();
                    String tipText = d.getXValue() + "\n"
                            + String.format("%,.0f TND", val) + "\n"
                            + edu.hanouti.services.ExchangeRateService.format(val, "EUR") + "  "
                            + edu.hanouti.services.ExchangeRateService.format(val, "USD");
                    Tooltip t = new Tooltip(tipText);
                    t.setStyle("-fx-background-color:#0d1117;-fx-text-fill:white;-fx-font-size:12px;" +
                            "-fx-font-weight:bold;-fx-border-color:" + col
                            + ";-fx-border-radius:5;-fx-background-radius:5;");
                    Tooltip.install(d.getNode(), t);
                    // Animation cascade
                    d.getNode().setScaleY(0);
                    d.getNode().setTranslateY(20);
                    Timeline tl = new Timeline(
                            new KeyFrame(Duration.ZERO,
                                    new KeyValue(d.getNode().scaleYProperty(), 0),
                                    new KeyValue(d.getNode().translateYProperty(), 20)),
                            new KeyFrame(Duration.millis(600 + i * 80),
                                    new KeyValue(d.getNode().scaleYProperty(), 1, Interpolator.EASE_OUT),
                                    new KeyValue(d.getNode().translateYProperty(), 0, Interpolator.EASE_OUT)));
                    tl.setDelay(Duration.millis(i * 60));
                    tl.play();
                    // Hover glow + highlight hexagone
                    d.getNode().setOnMouseEntered(ev -> d.getNode().setStyle(
                            "-fx-bar-fill:" + col + ";-fx-background-radius:4 4 0 0;" +
                                    "-fx-effect:dropshadow(gaussian," + col + ",14,0.7,0,0);"));
                    d.getNode().setOnMouseExited(ev -> d.getNode().setStyle(
                            "-fx-bar-fill:" + col + ";-fx-background-radius:4 4 0 0;"));
                }
            }
        });

        // ScrollPane horizontal si trop de barres
        ScrollPane sp = new ScrollPane(chart);
        sp.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        sp.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        sp.setFitToHeight(true);
        sp.setStyle("-fx-background-color:transparent;-fx-background:transparent;" +
                "-fx-border-color:transparent;-fx-padding:0;");
        sp.setPrefHeight(280);

        VBox wrapper = new VBox(4);
        // Légende couleurs sous le graphique — noms lisibles
        HBox legend = new HBox(12);
        legend.setAlignment(Pos.CENTER_LEFT);
        legend.setPadding(new Insets(6, 0, 0, 8));
        legend.setStyle("-fx-flex-wrap:wrap;");
        int li = 0;
        for (String key : finalRevByProd.keySet()) {
            String col = colors[li % colors.length];
            // Nom lisible : première lettre majuscule, reste minuscule
            String displayName = key.length() > 0
                    ? key.substring(0, 1).toUpperCase() + key.substring(1).toLowerCase()
                    : key;
            HBox item = new HBox(5);
            item.setAlignment(Pos.CENTER_LEFT);
            javafx.scene.shape.Rectangle rect = new javafx.scene.shape.Rectangle(10, 10);
            rect.setFill(Color.web(col));
            rect.setArcWidth(3);
            rect.setArcHeight(3);
            Label lbl = new Label(displayName);
            lbl.setStyle("-fx-font-size:11px;-fx-text-fill:#94a3b8;");
            item.getChildren().addAll(rect, lbl);
            legend.getChildren().add(item);
            li++;
        }
        wrapper.getChildren().addAll(sp, legend);
        return wrapper;
    }

    /** Hexagones avec vraies donnees DB - depuis List<StatistiquesVentes> */
    private void updateHexFromDB(StackPane holder, List<StatistiquesVentes> data) {
        java.util.Map<String, Double> revByProd = new java.util.LinkedHashMap<>();
        for (StatistiquesVentes s : data) {
            String ref = s.getProduitId() != null ? s.getProduitId() : "?";
            revByProd.merge(ref, s.getRevenuTotal(), Double::sum);
        }
        // Top 3 + "Autres"
        List<java.util.Map.Entry<String, Double>> sorted = new java.util.ArrayList<>(revByProd.entrySet());
        sorted.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));

        List<java.util.Map<String, Object>> mapList = new java.util.ArrayList<>();
        double autresTotal = 0;
        for (int i = 0; i < sorted.size(); i++) {
            if (i < 3) {
                java.util.Map<String, Object> m = new java.util.LinkedHashMap<>();
                m.put("ref", sorted.get(i).getKey());
                m.put("total", sorted.get(i).getValue());
                mapList.add(m);
            } else {
                autresTotal += sorted.get(i).getValue();
            }
        }
        if (autresTotal > 0) {
            java.util.Map<String, Object> autres = new java.util.LinkedHashMap<>();
            autres.put("ref", "Autres (" + (sorted.size() - 3) + ")");
            autres.put("total", autresTotal);
            mapList.add(autres);
        }
        updateHexFromMapList(holder, mapList);
    }

    /** Hexagones depuis une liste Map {ref, total} avec animation fade + entree */
    private void updateHexFromMapList(StackPane holder, List<java.util.Map<String, Object>> list) {
        if (list.isEmpty())
            return;
        double totalRev = list.stream().mapToDouble(m -> ((Number) m.get("total")).doubleValue()).sum();
        if (totalRev == 0)
            totalRev = 1;
        // Couleurs adaptées au thème
        String[] hexColors = getHexColors();
        double[] hexSizes = { 120, 96, 78, 64 }; // Tailles augmentées pour meilleure visibilité
        HBox hexBox = new HBox(12);
        hexBox.setAlignment(Pos.CENTER);
        hexBox.setPadding(new Insets(8, 0, 8, 0));
        for (int i = 0; i < list.size(); i++) {
            java.util.Map<String, Object> e = list.get(i);
            String ref = ((String) e.get("ref")).replace("REF-", "");
            double val = ((Number) e.get("total")).doubleValue();
            double pct = (val / totalRev) * 100;
            String col = hexColors[i % hexColors.length];
            double sz = i < hexSizes.length ? hexSizes[i] : 50;
            StackPane hexWrap = new StackPane();
            javafx.scene.shape.Polygon poly = new javafx.scene.shape.Polygon();
            for (int j = 0; j < 6; j++)
                poly.getPoints().addAll(sz / 2 + (sz / 2) * Math.cos(j * 2 * Math.PI / 6),
                        sz / 2 + (sz / 2) * Math.sin(j * 2 * Math.PI / 6));

            // Style adapté au thème
            if (darkMode) {
                poly.setFill(Color.web("#0d1526"));
                poly.setStroke(Color.web(col));
                poly.setStrokeWidth(2.0);
                poly.setEffect(new DropShadow(12, Color.web(col, 0.45)));
            } else {
                // Mode clair : fond pastel, bordure douce
                String bgColor = i == 0 ? "#dbeafe" : i == 1 ? "#d1fae5" : i == 2 ? "#fef3c7" : "#f3f4f6";
                poly.setFill(Color.web(bgColor));
                poly.setStroke(Color.web(col));
                poly.setStrokeWidth(2.0);
                poly.setEffect(new DropShadow(8, Color.web("rgba(0,0,0,0.1)")));
            }

            VBox txt = new VBox(2);
            txt.setAlignment(Pos.CENTER);
            Label lPct = new Label(String.format("%.0f%%", pct));
            lPct.setStyle("-fx-font-size:" + (sz / 3.8) + "px;-fx-font-weight:900;-fx-text-fill:" + col + ";");
            // Nom court lisible
            String refDisplay = ref.replace("REF-", "");
            if (refDisplay.length() > 9)
                refDisplay = refDisplay.substring(0, 9);
            Label lRef = new Label(refDisplay);
            String refColor = darkMode ? "#cbd5e1"
                    : (i == 0 ? "#1d4ed8" : i == 1 ? "#065f46" : i == 2 ? "#92400e" : "#374151");
            lRef.setStyle("-fx-font-size:" + Math.max(sz / 7.5, 9) + "px;-fx-text-fill:" + refColor
                    + ";-fx-font-weight:bold;");
            Label lRev = new Label(String.format("%,.0f TND", val));
            lRev.setStyle("-fx-font-size:" + Math.max(sz / 9.0, 8) + "px;-fx-text-fill:"
                    + (darkMode ? "#94a3b8" : "#64748b") + ";");
            txt.getChildren().addAll(lPct, lRef, lRev);
            hexWrap.getChildren().addAll(poly, txt);
            hexWrap.setScaleX(0);
            hexWrap.setScaleY(0);
            hexWrap.setRotate(-30);
            ScaleTransition st = new ScaleTransition(Duration.millis(600), hexWrap);
            st.setDelay(Duration.millis(i * 100));
            st.setToX(1);
            st.setToY(1);
            RotateTransition rt = new RotateTransition(Duration.millis(700), hexWrap);
            rt.setDelay(Duration.millis(i * 100));
            rt.setToAngle(0);
            rt.setInterpolator(Interpolator.EASE_OUT);
            new ParallelTransition(st, rt).play();
            Timeline floatAni = new Timeline(
                    new KeyFrame(Duration.ZERO, new KeyValue(hexWrap.translateYProperty(), 0)),
                    new KeyFrame(Duration.seconds(2 + i * 0.4),
                            new KeyValue(hexWrap.translateYProperty(), -6, Interpolator.EASE_BOTH)));
            floatAni.setAutoReverse(true);
            floatAni.setCycleCount(Timeline.INDEFINITE);
            floatAni.setDelay(Duration.millis(i * 300));
            floatAni.play();
            FadeTransition gp = new FadeTransition(Duration.seconds(1.8), poly);
            gp.setFromValue(1.0);
            gp.setToValue(0.5);
            gp.setAutoReverse(true);
            gp.setCycleCount(Timeline.INDEFINITE);
            gp.setDelay(Duration.millis(i * 400));
            gp.play();
            hexBox.getChildren().add(hexWrap);
        }
        // Transition fade pour le changement
        FadeTransition fadeOut = new FadeTransition(Duration.millis(150), holder);
        fadeOut.setToValue(0);
        fadeOut.setOnFinished(ev -> {
            holder.getChildren().setAll(hexBox);
            FadeTransition fadeIn = new FadeTransition(Duration.millis(300), holder);
            fadeIn.setToValue(1);
            fadeIn.play();
        });
        fadeOut.play();
    }

    /** Chart with period-specific bar values */
    private VBox createNeonBarChart(String period) {
        javafx.scene.chart.CategoryAxis xAxis = new javafx.scene.chart.CategoryAxis();
        javafx.scene.chart.NumberAxis yAxis = new javafx.scene.chart.NumberAxis();
        xAxis.setTickLabelFill(Color.web(TEXT_3));
        yAxis.setTickLabelFill(Color.web(TEXT_3));
        xAxis.setStyle("-fx-tick-label-fill: " + TEXT_3 + "; -fx-border-color: transparent;");
        yAxis.setStyle("-fx-tick-label-fill: " + TEXT_3 + "; -fx-border-color: transparent;");

        yAxis.setTickLabelFormatter(new javafx.util.StringConverter<Number>() {
            @Override
            public String toString(Number object) {
                if (object.doubleValue() == 0)
                    return "0";
                return String.format("%.0fK", object.doubleValue() / 1000);
            }

            @Override
            public Number fromString(String string) {
                return null;
            }
        });

        javafx.scene.chart.BarChart<String, Number> chart = new javafx.scene.chart.BarChart<>(xAxis, yAxis);
        chart.setLegendVisible(false);
        chart.setAnimated(false);
        chart.setStyle(
                "-fx-background-color: transparent; -fx-plot-background-color: transparent; -fx-bar-gap: 8; -fx-category-gap: 30;");
        chart.setPrefHeight(260);

        javafx.scene.chart.XYChart.Series<String, Number> sBud = new javafx.scene.chart.XYChart.Series<>();
        javafx.scene.chart.XYChart.Series<String, Number> sRev = new javafx.scene.chart.XYChart.Series<>();

        switch (period) {
            case "Semaine":
                sBud.getData().addAll(new javafx.scene.chart.XYChart.Data<>("Lun", 300),
                        new javafx.scene.chart.XYChart.Data<>("Mar", 400),
                        new javafx.scene.chart.XYChart.Data<>("Mer", 350),
                        new javafx.scene.chart.XYChart.Data<>("Jeu", 500),
                        new javafx.scene.chart.XYChart.Data<>("Ven", 600),
                        new javafx.scene.chart.XYChart.Data<>("Sam", 800),
                        new javafx.scene.chart.XYChart.Data<>("Dim", 200));
                sRev.getData().addAll(new javafx.scene.chart.XYChart.Data<>("Lun", 320),
                        new javafx.scene.chart.XYChart.Data<>("Mar", 480),
                        new javafx.scene.chart.XYChart.Data<>("Mer", 410),
                        new javafx.scene.chart.XYChart.Data<>("Jeu", 560),
                        new javafx.scene.chart.XYChart.Data<>("Ven", 720),
                        new javafx.scene.chart.XYChart.Data<>("Sam", 890),
                        new javafx.scene.chart.XYChart.Data<>("Dim", 230));
                break;
            case "Mois":
                sBud.getData().addAll(new javafx.scene.chart.XYChart.Data<>("S1", 1800),
                        new javafx.scene.chart.XYChart.Data<>("S2", 3000),
                        new javafx.scene.chart.XYChart.Data<>("S3", 2500),
                        new javafx.scene.chart.XYChart.Data<>("S4", 3800));
                sRev.getData().addAll(new javafx.scene.chart.XYChart.Data<>("S1", 2100),
                        new javafx.scene.chart.XYChart.Data<>("S2", 3400),
                        new javafx.scene.chart.XYChart.Data<>("S3", 2800),
                        new javafx.scene.chart.XYChart.Data<>("S4", 4320));
                break;
            case "Ann\u00e9e":
                sBud.getData().addAll(new javafx.scene.chart.XYChart.Data<>("Jan", 8000),
                        new javafx.scene.chart.XYChart.Data<>("Fev", 7000),
                        new javafx.scene.chart.XYChart.Data<>("Mar", 9000),
                        new javafx.scene.chart.XYChart.Data<>("Avr", 10000),
                        new javafx.scene.chart.XYChart.Data<>("Mai", 12000),
                        new javafx.scene.chart.XYChart.Data<>("Jun", 10000),
                        new javafx.scene.chart.XYChart.Data<>("Jul", 9000),
                        new javafx.scene.chart.XYChart.Data<>("Aou", 8000),
                        new javafx.scene.chart.XYChart.Data<>("Sep", 10000),
                        new javafx.scene.chart.XYChart.Data<>("Oct", 13000),
                        new javafx.scene.chart.XYChart.Data<>("Nov", 15000),
                        new javafx.scene.chart.XYChart.Data<>("Dec", 20000));
                sRev.getData().addAll(new javafx.scene.chart.XYChart.Data<>("Jan", 8200),
                        new javafx.scene.chart.XYChart.Data<>("Fev", 7100),
                        new javafx.scene.chart.XYChart.Data<>("Mar", 9500),
                        new javafx.scene.chart.XYChart.Data<>("Avr", 11200),
                        new javafx.scene.chart.XYChart.Data<>("Mai", 13500),
                        new javafx.scene.chart.XYChart.Data<>("Jun", 10800),
                        new javafx.scene.chart.XYChart.Data<>("Jul", 9200),
                        new javafx.scene.chart.XYChart.Data<>("Aou", 8400),
                        new javafx.scene.chart.XYChart.Data<>("Sep", 11000),
                        new javafx.scene.chart.XYChart.Data<>("Oct", 14200),
                        new javafx.scene.chart.XYChart.Data<>("Nov", 16800),
                        new javafx.scene.chart.XYChart.Data<>("Dec", 21000));
                break;
            default: // Trimestre
                sBud.getData().addAll(new javafx.scene.chart.XYChart.Data<>("Trim 1", 16000),
                        new javafx.scene.chart.XYChart.Data<>("Trim 2", 22000),
                        new javafx.scene.chart.XYChart.Data<>("Trim 3", 18000),
                        new javafx.scene.chart.XYChart.Data<>("Trim 4", 30000));
                sRev.getData().addAll(new javafx.scene.chart.XYChart.Data<>("Trim 1", 18500),
                        new javafx.scene.chart.XYChart.Data<>("Trim 2", 24200),
                        new javafx.scene.chart.XYChart.Data<>("Trim 3", 19800),
                        new javafx.scene.chart.XYChart.Data<>("Trim 4", 32100));
                break;
        }
        chart.getData().addAll(sBud, sRev);

        Platform.runLater(() -> {
            // Couleurs adaptées au thème
            String budgetColor = darkMode ? "#f6ab00" : "#fbbf24";
            String revenusColor = darkMode ? "#00d4aa" : "#34d399";

            for (int i = 0; i < sBud.getData().size(); i++) {
                javafx.scene.chart.XYChart.Data<String, Number> db = sBud.getData().get(i);
                javafx.scene.chart.XYChart.Data<String, Number> dr = sRev.getData().get(i);
                if (db.getNode() != null) {
                    db.getNode().setStyle("-fx-bar-fill: " + budgetColor + "; -fx-background-radius: 4 4 0 0;");
                    Tooltip tb = new Tooltip(db.getYValue() + " TND");
                    tb.setStyle("-fx-background-color: " + (darkMode ? "#0d1117" : "#ffffff") + "; -fx-text-fill: "
                            + TEXT_1 + "; -fx-font-weight: bold; -fx-border-color: " + budgetColor
                            + "; -fx-border-radius: 5; -fx-background-radius: 5;");
                    Tooltip.install(db.getNode(), tb);
                    applyCascadeAnimation(db.getNode(), i * 100);
                }
                if (dr.getNode() != null) {
                    dr.getNode().setStyle("-fx-bar-fill: " + revenusColor + "; -fx-background-radius: 4 4 0 0;");
                    Tooltip tr = new Tooltip(dr.getYValue() + " TND");
                    tr.setStyle("-fx-background-color: " + (darkMode ? "#0d1117" : "#ffffff") + "; -fx-text-fill: "
                            + TEXT_1 + "; -fx-font-weight: bold; -fx-border-color: " + revenusColor
                            + "; -fx-border-radius: 5; -fx-background-radius: 5;");
                    Tooltip.install(dr.getNode(), tr);
                    applyCascadeAnimation(dr.getNode(), i * 100 + 50);
                }
            }
        });

        HBox legend = new HBox(20);
        legend.setAlignment(Pos.CENTER);
        String budgetColor = darkMode ? "#f6ab00" : "#fbbf24";
        String revenusColor = darkMode ? "#00d4aa" : "#34d399";
        legend.getChildren().addAll(makeLegendItem("Budget", budgetColor),
                makeLegendItem("Revenus r\u00e9els", revenusColor));
        VBox wrapper = new VBox(10);
        wrapper.getChildren().addAll(chart, legend);
        return wrapper;
    }

    private void applyCascadeAnimation(javafx.scene.Node node, int delayMs) {
        node.setScaleY(0);
        node.setTranslateY(node.getBoundsInParent().getHeight() / 2);
        ScaleTransition st = new ScaleTransition(Duration.millis(400), node);
        st.setToY(1);
        st.setDelay(Duration.millis(delayMs));
        TranslateTransition tt = new TranslateTransition(Duration.millis(400), node);
        tt.setToY(0);
        tt.setDelay(Duration.millis(delayMs));
        new ParallelTransition(st, tt).play();
    }

    /** Fallback no-arg version */
    private VBox createNeonBarChart() {
        return createNeonBarChart("Trimestre");
    }

    /** Récupère le nombre de vues (nb_interaction) pour un produit depuis interaction_utilisateur */
    private int getVuesProduit(String produitId) {
        try {
            java.sql.Connection cnx = edu.hanouti.utils.MyConnection.getConnection();
            String sql = "SELECT COALESCE(SUM(nb_interaction), 0) FROM interaction_utilisateur WHERE id_produit = ? AND type_interaction = 'vue'";
            java.sql.PreparedStatement ps = cnx.prepareStatement(sql);
            ps.setString(1, produitId);
            java.sql.ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1);
        } catch (Exception e) {
            // Si la colonne type_interaction n'existe pas, compter toutes les interactions
            try {
                java.sql.Connection cnx = edu.hanouti.utils.MyConnection.getConnection();
                String sql2 = "SELECT COALESCE(SUM(nb_interaction), 0) FROM interaction_utilisateur WHERE id_produit = ?";
                java.sql.PreparedStatement ps2 = cnx.prepareStatement(sql2);
                ps2.setString(1, produitId);
                java.sql.ResultSet rs2 = ps2.executeQuery();
                if (rs2.next()) return rs2.getInt(1);
            } catch (Exception ex) { /* ignore */ }
        }
        return 0;
    }

    /** Neon performance card for card-view */
    private VBox buildPerfCard(String ref, String sales, String rev, String ret, String status, String color) {
        VBox card = new VBox(7);
        card.setPadding(new Insets(12));
        card.setPrefWidth(175);
        card.setPrefHeight(130);

        // Style adapté au thème
        if (darkMode) {
            card.setStyle("-fx-background-color: " + BG_CARD + "; -fx-background-radius: 12;" +
                    "-fx-border-color: " + color + "; -fx-border-width: 1; -fx-border-radius: 12;");
            card.setEffect(new DropShadow(10, Color.web(color + "33")));
        } else {
            // Mode clair : fond blanc, bordure top colorée
            card.setStyle("-fx-background-color: #ffffff; -fx-background-radius: 12;" +
                    "-fx-border-color: " + color + " #e2e8f0 #e2e8f0 #e2e8f0;" +
                    "-fx-border-width: 3 1 1 1; -fx-border-radius: 12;");
            card.setEffect(new DropShadow(8, Color.web("rgba(0,0,0,0.08)")));
        }

        // Nom produit (sans REF-)
        Label lRef = new Label(ref.replace("REF-", ""));
        lRef.setStyle("-fx-text-fill: " + TEXT_1 + "; -fx-font-weight: bold; -fx-font-size: 13px;");

        // Ventes avec label clair
        Label lSales = new Label("\uD83D\uDED2 " + sales + " ventes");
        lSales.setStyle("-fx-text-fill: " + color + "; -fx-font-size: 11px; -fx-font-weight: bold;");

        // Revenu avec label
        Label lRev = new Label("\uD83D\uDCB0 " + rev);
        String revColor = darkMode ? PASTEL_SKY : "#60a5fa";
        lRev.setStyle("-fx-text-fill: " + revColor + "; -fx-font-size: 11px;");

        // Taux de retour avec label explicite
        Label lRet = new Label("\uD83D\uDD04 Retour : " + ret);
        lRet.setStyle("-fx-text-fill: " + TEXT_2 + "; -fx-font-size: 10px;");

        // Badge vues — récupéré depuis interaction_utilisateur
        int nbVues = getVuesProduit(ref);
        String badgeText = "👁 " + nbVues + " vues";
        String badgeBg;
        String badgeColor;
        if (nbVues >= 100) {
            badgeBg = darkMode ? "rgba(16,185,129,0.12)" : "#d1fae5";
            badgeColor = darkMode ? NEON_GREEN : "#065f46";
        } else if (nbVues >= 10) {
            badgeBg = darkMode ? "rgba(56,189,248,0.12)" : "#dbeafe";
            badgeColor = darkMode ? PASTEL_SKY : "#1d4ed8";
        } else {
            badgeBg = darkMode ? "rgba(100,116,139,0.12)" : "#f3f4f6";
            badgeColor = darkMode ? "#64748b" : "#6b7280";
        }

        Label badge = new Label(badgeText);
        badge.setStyle("-fx-text-fill: " + badgeColor + "; -fx-font-size: 10px; -fx-font-weight: bold;" +
                "-fx-background-color: " + badgeBg + ";" +
                "-fx-padding: 2 8; -fx-background-radius: 99;");

        card.getChildren().addAll(lRef, lSales, lRev, lRet, badge);
        card.setOnMouseEntered(e -> {
            TranslateTransition t = new TranslateTransition(Duration.millis(150), card);
            t.setToY(-4);
            t.play();
        });
        card.setOnMouseExited(e -> {
            TranslateTransition t = new TranslateTransition(Duration.millis(150), card);
            t.setToY(0);
            t.play();
        });
        return card;
    }

    private VBox makeScreenshotKpi(String title, String val, String valLine2, String trend, String color) {
        VBox card = new VBox(8);
        card.setPadding(new Insets(20, 22, 20, 22));
        if (darkMode) {
            card.setStyle("-fx-background-color: " + BG_CARD + "; -fx-background-radius: 16;" +
                    "-fx-border-color: " + color + "; -fx-border-width: 1.5; -fx-border-radius: 16;");
            card.setEffect(new DropShadow(18, Color.web(color + "55")));
        } else {
            card.setStyle("-fx-background-color: #ffffff; -fx-background-radius: 16;" +
                    "-fx-border-color: " + color + " transparent transparent transparent;" +
                    "-fx-border-width: 3 0 0 0; -fx-border-radius: 16;");
            card.setEffect(new DropShadow(10, Color.web("rgba(0,0,0,0.08)")));
        }
        Label lTitle = new Label(title);
        lTitle.setStyle("-fx-text-fill: " + TEXT_3
                + "; -fx-font-size: 10px; -fx-font-weight: bold; -fx-letter-spacing: 0.1em;");
        Label lVal = new Label(val);
        lVal.setStyle("-fx-text-fill: " + color + "; -fx-font-size: 32px; -fx-font-weight: 900;");
        lVal.setEffect(new DropShadow(20, Color.web(color)));
        card.getChildren().addAll(lTitle, lVal);
        if (valLine2 != null) {
            Label lVal2 = new Label(valLine2);
            lVal2.setStyle("-fx-text-fill: " + color + "; -fx-font-size: 24px; -fx-font-weight: 900;");
            card.getChildren().add(lVal2);
        }
        Label lTrend = new Label(trend);
        lTrend.setStyle("-fx-text-fill: " + color + "; -fx-font-size: 11px;");
        card.getChildren().add(lTrend);
        // Hover glow pulse
        card.setOnMouseEntered(e -> {
            ScaleTransition s = new ScaleTransition(Duration.millis(150), card);
            s.setToX(1.02);
            s.setToY(1.02);
            s.play();
        });
        card.setOnMouseExited(e -> {
            ScaleTransition s = new ScaleTransition(Duration.millis(150), card);
            s.setToX(1.0);
            s.setToY(1.0);
            s.play();
        });
        return card;
    }

    private void updateHexGridFull(StackPane holder, String p1, String p2, String rev1, String rev2) {
        HBox hexBox = new HBox(30);
        hexBox.setAlignment(Pos.CENTER);
        hexBox.setPadding(new Insets(10, 0, 0, 0));

        StackPane hex1 = buildAdvancedHexBadge(p1, "CHARGEUR", rev1, "#f6ab00");
        StackPane hex2 = buildAdvancedHexBadge(p2, "CABLE", rev2, "#00d4aa");
        hexBox.getChildren().addAll(hex1, hex2);

        VBox container = new VBox(25);
        container.setAlignment(Pos.CENTER);

        double pct1 = 66.0;
        try {
            pct1 = Double.parseDouble(p1.replace("%", "").trim());
        } catch (Exception e) {
        }

        HBox pBar = new HBox(0);
        pBar.setPrefHeight(10);
        pBar.setPrefWidth(280);
        pBar.setMaxWidth(280);
        pBar.setStyle("-fx-background-color: rgba(255,255,255,0.05); -fx-background-radius: 5;");

        Region fill1 = new Region();
        fill1.setStyle("-fx-background-color: #f6ab00; -fx-background-radius: 5 0 0 5;");
        Region fill2 = new Region();
        fill2.setStyle("-fx-background-color: #00d4aa; -fx-background-radius: 0 5 5 0;");

        fill1.setPrefWidth(0);
        fill2.setPrefWidth(0);
        double finalPct1 = pct1;
        Platform.runLater(() -> {
            new Timeline(
                    new KeyFrame(Duration.ZERO, new KeyValue(fill1.prefWidthProperty(), 0),
                            new KeyValue(fill2.prefWidthProperty(), 0)),
                    new KeyFrame(Duration.millis(800),
                            new KeyValue(fill1.prefWidthProperty(), 280 * (finalPct1 / 100.0)),
                            new KeyValue(fill2.prefWidthProperty(), 280 * ((100 - finalPct1) / 100.0))))
                    .play();
        });

        pBar.getChildren().addAll(fill1, fill2);

        HBox leg = new HBox(20);
        leg.setAlignment(Pos.CENTER);
        leg.getChildren().addAll(makeLegendItem("Chargeur " + p1, "#f6ab00"),
                makeLegendItem("C\u00e2ble " + p2, "#00d4aa"));

        container.getChildren().addAll(hexBox, pBar, leg);
        holder.getChildren().setAll(container);
    }

    private void updateHexGrid(StackPane holder, String p1, String p2) {
        updateHexGridFull(holder, p1, p2, "10K TND", "2K TND");
    }

    private StackPane buildAdvancedHexBadge(String pct, String title, String val, String color) {
        double width = 110, height = 125;

        // Glow layer (No DropShadow, just translucent thick stroke)
        javafx.scene.shape.Polygon glow = new javafx.scene.shape.Polygon();
        glow.getPoints().addAll(width / 2, 0.0, width, height / 4, width, height * 3 / 4, width / 2, height, 0.0,
                height * 3 / 4, 0.0, height / 4);
        glow.setFill(Color.TRANSPARENT);
        glow.setStroke(Color.web(color));
        glow.setStrokeWidth(6);
        glow.setOpacity(0.4);

        javafx.scene.shape.Polygon outer = new javafx.scene.shape.Polygon();
        outer.getPoints().addAll(width / 2, 0.0, width, height / 4, width, height * 3 / 4, width / 2, height, 0.0,
                height * 3 / 4, 0.0, height / 4);
        outer.setFill(Color.web("#0d1117"));
        outer.setStroke(Color.web(color));
        outer.setStrokeWidth(2.5);

        javafx.scene.shape.Polygon inner = new javafx.scene.shape.Polygon();
        double iw = width - 16, ih = height - 16;
        inner.getPoints().addAll(iw / 2, 0.0, iw, ih / 4, iw, ih * 3 / 4, iw / 2, ih, 0.0, ih * 3 / 4, 0.0, ih / 4);
        inner.setFill(Color.TRANSPARENT);
        inner.setStroke(Color.web(color));
        inner.setStrokeWidth(1.5);
        inner.getStrokeDashArray().addAll(4d, 4d);

        VBox content = new VBox(2);
        content.setAlignment(Pos.CENTER);
        Label lPct = new Label("0%");
        lPct.setStyle("-fx-text-fill: " + color + "; -fx-font-size: 28px; -fx-font-weight: 900;");
        Label lTitle = new Label(title);
        lTitle.setStyle("-fx-text-fill: " + color + "; -fx-font-size: 11px; -fx-font-weight: bold;");
        Label lVal = new Label(val);
        lVal.setStyle("-fx-text-fill: " + TEXT_3 + "; -fx-font-size: 10px;");
        content.getChildren().addAll(lPct, lTitle, lVal);

        double targetPct = 0;
        try {
            targetPct = Double.parseDouble(pct.replace("%", "").trim());
        } catch (Exception e) {
        }
        double fTarget = targetPct;
        Platform.runLater(() -> {
            Timeline tl = new Timeline(new KeyFrame(Duration.millis(800),
                    new KeyValue(new javafx.beans.property.SimpleDoubleProperty(0), fTarget)));
            tl.currentTimeProperty().addListener((obs, oldV, newV) -> {
                double current = (newV.toMillis() / 800.0) * fTarget;
                lPct.setText(Math.round(current) + "%");
            });
            tl.play();
        });

        StackPane pane = new StackPane(glow, outer, inner, content);
        // Continuous glow pulse on the glow polygon
        FadeTransition ft = new FadeTransition(Duration.seconds(1.5), glow);
        ft.setFromValue(0.5);
        ft.setToValue(0.1);
        ft.setCycleCount(javafx.animation.Animation.INDEFINITE);
        ft.setAutoReverse(true);
        ft.play();

        return pane;
    }

    private void applyNeonScrollbar(ScrollPane sp) {
        sp.getStylesheets().add("data:text/css," +
                ".scroll-bar:vertical .thumb { -fx-background-color: " + ROYAL + "; -fx-background-radius: 10; }" +
                ".scroll-bar:vertical .track { -fx-background-color: rgba(255,255,255,0.02); -fx-background-radius: 10; }"
                +
                ".scroll-bar:vertical .increment-button, .scroll-bar:vertical .decrement-button { -fx-opacity: 0; }");
    }

    private void animateUpdate(Node n) {
        // Creative: pulse glow + scale
        ScaleTransition st = new ScaleTransition(Duration.millis(250), n);
        st.setFromX(1.0);
        st.setFromY(1.0);
        st.setToX(1.03);
        st.setToY(1.03);
        st.setCycleCount(2);
        st.setAutoReverse(true);
        FadeTransition ft = new FadeTransition(Duration.millis(200), n);
        ft.setFromValue(1.0);
        ft.setToValue(0.5);
        ft.setCycleCount(2);
        ft.setAutoReverse(true);
        DropShadow glow = new DropShadow(30, Color.web(ROYAL));
        n.setEffect(glow);
        new ParallelTransition(st, ft).play();
        // Remove glow after animation
        new Timeline(new KeyFrame(Duration.millis(600), ev -> n.setEffect(null))).play();
    }

    private HBox buildDetailedRow(String name, String period, String sem, String sales, String rev, String ret,
            String status, String color) {
        HBox row = new HBox(0);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(15, 20, 15, 20));
        row.setStyle("-fx-border-color: rgba(255,255,255,0.03); -fx-border-width: 0 0 1 0;");

        Label lN = new Label(name);
        lN.setPrefWidth(200);
        lN.setStyle("-fx-text-fill: white; -fx-font-weight: bold;");
        Label lP = new Label(period);
        lP.setPrefWidth(150);
        lP.setStyle("-fx-text-fill: " + TEXT_3 + ";");
        Label lS = new Label(sem);
        lS.setPrefWidth(100);
        lS.setStyle("-fx-text-fill: " + TEXT_3 + ";");
        Label lV = new Label(sales);
        lV.setPrefWidth(100);
        lV.setStyle("-fx-text-fill: " + color + "; -fx-font-weight: bold;");
        Label lR = new Label(rev);
        lR.setPrefWidth(150);
        lR.setStyle("-fx-text-fill: " + TEXT_2 + ";");
        Label lRt = new Label(ret);
        lRt.setPrefWidth(120);
        lRt.setStyle("-fx-text-fill: " + (ret.contains("4") || ret.contains("6") ? PASTEL_ROSE : NEON_GREEN) + ";");

        // Calcul du Revenu / Unite
        double revVal = 0;
        try {
            revVal = Double.parseDouble(rev.replace("TND", "").trim());
        } catch (Exception e) {
        }
        int qtyVal = 1;
        try {
            qtyVal = Integer.parseInt(sales.replace("\uD83D\uDCC8", "").trim());
        } catch (Exception e) {
        }
        Label lUnit = new Label(String.format("%.1f/u", revVal / Math.max(1, qtyVal)));
        lUnit.setPrefWidth(80);
        lUnit.setStyle("-fx-text-fill: white; -fx-font-size: 11px;");

        StackPane badge = makeBadge(status, color);
        HBox bBox = new HBox(badge);
        bBox.setPrefWidth(100);
        bBox.setAlignment(Pos.CENTER_LEFT);

        row.getChildren().addAll(lN, lP, lS, lV, lR, lUnit, lRt, bBox);
        row.setOnMouseEntered(e -> row.setStyle(
                "-fx-background-color: rgba(255,255,255,0.03); -fx-border-color: rgba(255,255,255,0.03); -fx-border-width: 0 0 1 0;"));
        row.setOnMouseExited(e -> row.setStyle(
                "-fx-background-color: transparent; -fx-border-color: rgba(255,255,255,0.03); -fx-border-width: 0 0 1 0;"));
        return row;
    }

    private HBox buildStatRow(String name, String sub, String vStatus, String rStatus, String cStatus, String sStatus,
            int score, String color) {
        HBox row = new HBox(0);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(18, 25, 18, 25));
        row.setStyle("-fx-border-color: rgba(255,255,255,0.03); -fx-border-width: 0 0 1 0;");

        VBox pBox = new VBox(2);
        pBox.setPrefWidth(220);
        Label nL = new Label(name);
        nL.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 15px;");
        Label sL = new Label(sub);
        sL.setStyle("-fx-text-fill: " + TEXT_3 + "; -fx-font-size: 11px;");
        pBox.getChildren().addAll(nL, sL);

        StackPane vPill = makeBadge(vStatus, color);
        HBox vBox = new HBox(vPill);
        vBox.setPrefWidth(140);
        vBox.setAlignment(Pos.CENTER_LEFT);

        StackPane rPill = makeBadge(rStatus, color.equals(PASTEL_ROSE) ? PASTEL_ROSE : NEON_GREEN);
        HBox rBox = new HBox(rPill);
        rBox.setPrefWidth(120);
        rBox.setAlignment(Pos.CENTER_LEFT);

        StackPane cPill = makeBadge(cStatus, color.equals(PASTEL_ROSE) ? PASTEL_ROSE : PASTEL_SKY);
        HBox cBox = new HBox(cPill);
        cBox.setPrefWidth(120);
        cBox.setAlignment(Pos.CENTER_LEFT);

        StackPane sPill = makeBadge(sStatus,
                sStatus.equals("Bas") ? ORANGE : (sStatus.equals("Vide") ? PASTEL_ROSE : NEON_GREEN));
        HBox sBox = new HBox(sPill);
        sBox.setPrefWidth(120);
        sBox.setAlignment(Pos.CENTER_LEFT);

        VBox scoreBox = new VBox(5);
        scoreBox.setPrefWidth(100);
        scoreBox.setAlignment(Pos.CENTER_RIGHT);
        Label scoreL = new Label(String.valueOf(score));
        scoreL.setStyle("-fx-text-fill: " + color + "; -fx-font-size: 22px; -fx-font-weight: 900;");
        ProgressBar pb = new ProgressBar(score / 100.0);
        pb.setPrefWidth(80);
        pb.setMaxHeight(4);
        pb.setStyle("-fx-accent: " + color + ";");
        scoreBox.getChildren().addAll(scoreL, pb);

        row.getChildren().addAll(pBox, vBox, rBox, cBox, sBox, scoreBox);
        row.setOnMouseEntered(e -> row.setStyle(
                "-fx-background-color: rgba(255,255,255,0.02); -fx-border-color: rgba(255,255,255,0.03); -fx-border-width: 0 0 1 0;"));
        row.setOnMouseExited(e -> row.setStyle(
                "-fx-background-color: transparent; -fx-border-color: rgba(255,255,255,0.03); -fx-border-width: 0 0 1 0;"));
        return row;
    }

    private Label makeHeadLbl(String txt, double w) {
        Label l = new Label(txt);
        l.setPrefWidth(w);
        l.setStyle(
                "-fx-text-fill: " + TEXT_3 + "; -fx-font-size: 11px; -fx-font-weight: bold; -fx-letter-spacing: 1px;");
        return l;
    }

    private StackPane makeBadge(String txt, String color) {
        Label l = new Label(txt);
        l.setStyle("-fx-text-fill: " + color + "; -fx-font-weight: bold; -fx-font-size: 12px;");
        StackPane p = new StackPane(l);
        p.setPadding(new Insets(5, 12, 5, 12));
        if (darkMode) {
            p.setStyle("-fx-background-color: rgba(" + hexToRgb(color) + ", 0.1);" +
                    "-fx-border-color: " + color + ";" +
                    "-fx-border-radius: 8; -fx-background-radius: 8; -fx-border-width: 1;");
        } else {
            p.setStyle("-fx-background-color: rgba(" + hexToRgb(color) + ", 0.12);" +
                    "-fx-border-color: rgba(" + hexToRgb(color) + ", 0.4);" +
                    "-fx-border-radius: 8; -fx-background-radius: 8; -fx-border-width: 1;");
        }
        return p;
    }

    private HBox makeLegendItem(String txt, String color) {
        Circle c = new Circle(5, Color.web(color));
        Label l = new Label(txt);
        l.setStyle("-fx-text-fill: " + TEXT_2 + "; -fx-font-size: 12px;");
        HBox h = new HBox(8, c, l);
        h.setAlignment(Pos.CENTER);
        return h;
    }

    private String hexToRgb(String hex) {
        Color c = Color.web(hex);
        return (int) (c.getRed() * 255) + "," + (int) (c.getGreen() * 255) + "," + (int) (c.getBlue() * 255);
    }

    // ==============================================
    // [6] MODULE CONSEILS IA
    // ==============================================
    private ScrollPane buildModuleConseils() {
        VBox view = new VBox(22);
        view.setPadding(new Insets(28, 32, 32, 32));
        view.setStyle("-fx-background-color: transparent;");

        // ── Header amélioré ──
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(0, 0, 20, 0));
        header.setStyle("-fx-border-color: rgba(255,255,255,0.06); -fx-border-width: 0 0 1 0;");

        // Barre colorée à gauche
        javafx.scene.shape.Rectangle accentBar = new javafx.scene.shape.Rectangle(4, 52);
        accentBar.setFill(Color.web(VIOLET));
        accentBar.setArcWidth(4);
        accentBar.setArcHeight(4);
        accentBar.setEffect(new DropShadow(10, Color.web(VIOLET)));

        VBox headerTexts = new VBox(4);
        Label headerTitle = new Label("Centre de Decision");
        headerTitle.setStyle("-fx-font-size:26px;-fx-font-weight:900;-fx-text-fill:" + TEXT_1 + ";");
        Label headerSub = new Label("Opportunites detectees \u00B7 Historique d'interaction");
        headerSub.setStyle("-fx-font-size:13px;-fx-text-fill:" + TEXT_3 + ";");
        headerTexts.getChildren().addAll(headerTitle, headerSub);

        Region hSp = new Region();
        HBox.setHgrow(hSp, Priority.ALWAYS);

        // Bouton Historique compact et discret - couleurs adaptatives
        Button histBtn = new Button("\uD83D\uDD52  Historique");
        String histBtnBg = darkMode ? "rgba(17,20,37,0.5)" : "rgba(248,250,252,0.6)"; // Plus transparent
        String histBtnText = darkMode ? "#94a3b8" : "#64748b";
        String histBtnBorder = darkMode ? "rgba(51,65,85,0.5)" : "rgba(226,232,240,0.6)"; // Plus subtil
        String histBtnHoverBg = darkMode ? "#1e2a40" : "#e0f2fe";
        String histBtnHoverText = darkMode ? "#38bdf8" : "#0284c7";
        String histBtnHoverBorder = darkMode ? "#38bdf8" : "#60a5fa";

        histBtn.setStyle(
                "-fx-background-color:" + histBtnBg + ";-fx-text-fill:" + histBtnText + ";" +
                        "-fx-font-size:11px;-fx-cursor:hand;-fx-padding:6 12;" + // Réduit
                        "-fx-background-radius:8;-fx-border-color:" + histBtnBorder + ";" +
                        "-fx-border-width:1;-fx-border-radius:8;-fx-opacity:0.85;"); // Légèrement transparent
        histBtn.setOnMouseEntered(e -> histBtn.setStyle(
                "-fx-background-color:" + histBtnHoverBg + ";-fx-text-fill:" + histBtnHoverText + ";" +
                        "-fx-font-size:12px;-fx-cursor:hand;-fx-padding:7 14;" +
                        "-fx-background-radius:8;-fx-border-color:" + histBtnHoverBorder + ";" +
                        "-fx-border-width:1;-fx-border-radius:8;"));
        histBtn.setOnMouseExited(e -> histBtn.setStyle(
                "-fx-background-color:" + histBtnBg + ";-fx-text-fill:" + histBtnText + ";" +
                        "-fx-font-size:12px;-fx-cursor:hand;-fx-padding:7 14;" +
                        "-fx-background-radius:8;-fx-border-color:" + histBtnBorder + ";" +
                        "-fx-border-width:1;-fx-border-radius:8;"));
        histBtn.setOnAction(e -> showHistoriquePopup());

        header.getChildren().addAll(accentBar, headerTexts, hSp, histBtn);
        view.getChildren().add(header);

        List<Map<String, Object>> conseils = conseilsIaService.getConseils();

        // ── KPI Row ──
        HBox kpiRow = new HBox(14);
        int totalC = conseils.size();
        int appliques = conseilsIaService.compter("ACCEPTE");
        int nouveaux = conseilsIaService.compter("NOUVEAU");
        String confianceStr = totalC > 0
                ? String.format("%.0f%%", (appliques * 100.0 / totalC))
                : "0%";
        kpiRow.getChildren().addAll(
                makeKpi("TOTAL CONSEILS", String.valueOf(totalC), "generes ce mois", PASTEL_SKY),
                makeKpi("APPLIQUES", String.valueOf(appliques), "taux d'action", NEON_GREEN),
                makeKpi("NOUVEAUX", String.valueOf(nouveaux), "en attente d'action", ORANGE),
                makeKpi("CONFIANCE", confianceStr, "taux d'application", VIOLET));
        for (javafx.scene.Node k : kpiRow.getChildren())
            HBox.setHgrow(k, Priority.ALWAYS);

        // ── Filtres Tabs — style pill visible ──
        HBox filtresTabs = new HBox(6);
        filtresTabs.setAlignment(Pos.CENTER_LEFT);
        filtresTabs.setPadding(new Insets(4, 0, 4, 0));
        String[] tabLabels = { "Actifs", "Appliques", "Ignores" };
        Button[] tabBtns = new Button[3];
        final String[] activeTab = { "Actifs" };

        // Couleurs adaptatives pour les tabs
        String tabActiveBg = darkMode ? "rgba(56,189,248,0.15)" : "#dbeafe";
        String tabActiveText = darkMode ? "#38bdf8" : "#0284c7";
        String tabActiveBorder = darkMode ? "#38bdf8" : "#60a5fa";
        String tabInactiveBg = darkMode ? "rgba(255,255,255,0.04)" : "#f8fafc";
        String tabInactiveText = darkMode ? "#64748b" : "#94a3b8";
        String tabInactiveBorder = darkMode ? "rgba(255,255,255,0.08)" : "#e2e8f0";

        for (int ti = 0; ti < tabLabels.length; ti++) {
            Button tab = new Button(tabLabels[ti]);
            boolean first = ti == 0;
            tab.setStyle(first
                    ? "-fx-background-color:" + tabActiveBg + ";" +
                            "-fx-text-fill:" + tabActiveText + ";-fx-font-weight:bold;-fx-font-size:13px;" +
                            "-fx-cursor:hand;-fx-padding:8 20;-fx-background-radius:8;" +
                            "-fx-border-color:" + tabActiveBorder + ";-fx-border-width:1.5;-fx-border-radius:8;"
                    : "-fx-background-color:" + tabInactiveBg + ";" +
                            "-fx-text-fill:" + tabInactiveText + ";-fx-font-size:13px;" +
                            "-fx-cursor:hand;-fx-padding:8 20;-fx-background-radius:8;" +
                            "-fx-border-color:" + tabInactiveBorder + ";-fx-border-width:1;-fx-border-radius:8;");
            tabBtns[ti] = tab;
        }

        final javafx.scene.layout.FlowPane cardsContainer = new javafx.scene.layout.FlowPane(18, 18);
        cardsContainer.setPadding(new Insets(8, 0, 32, 0));

        for (int ti = 0; ti < tabLabels.length; ti++) {
            final String lbl = tabLabels[ti];
            final int idx = ti;
            tabBtns[ti].setOnAction(e -> {
                activeTab[0] = lbl;
                for (int j = 0; j < tabBtns.length; j++) {
                    tabBtns[j].setStyle(j == idx
                            ? "-fx-background-color:" + tabActiveBg + ";" +
                                    "-fx-text-fill:" + tabActiveText + ";-fx-font-weight:bold;-fx-font-size:13px;" +
                                    "-fx-cursor:hand;-fx-padding:8 20;-fx-background-radius:8;" +
                                    "-fx-border-color:" + tabActiveBorder + ";-fx-border-width:1.5;-fx-border-radius:8;"
                            : "-fx-background-color:" + tabInactiveBg + ";" +
                                    "-fx-text-fill:" + tabInactiveText + ";-fx-font-size:13px;" +
                                    "-fx-cursor:hand;-fx-padding:8 20;-fx-background-radius:8;" +
                                    "-fx-border-color:" + tabInactiveBorder
                                    + ";-fx-border-width:1;-fx-border-radius:8;");
                }
                String filterKey = lbl.equals("Actifs") ? "Tous"
                        : lbl.equals("Appliques") ? "Appliques" : "Ignores";
                refreshConseilsCards(cardsContainer, conseils, filterKey);
            });
            filtresTabs.getChildren().add(tabBtns[ti]);
        }

        // ── Filter Bar type conseil ──
        HBox filterBox = new HBox(10);
        filterBox.setAlignment(Pos.CENTER_LEFT);
        Label fLbl = new Label("TYPE :");
        fLbl.setStyle("-fx-text-fill:" + TEXT_3 + ";-fx-font-weight:bold;-fx-font-size:11px;");
        HBox filterBar = new HBox(8);
        final List<Button> pills = new java.util.ArrayList<>();
        String[] filters = { "Tous", "Promotion", "Destockage", "Bundle" };
        for (String f : filters) {
            Button p = makePill(f, f.equals("Tous"));
            pills.add(p);
            p.setOnAction(e -> {
                pills.forEach(b -> b.setStyle(
                        "-fx-background-color:transparent;-fx-text-fill:" + TEXT_3 + ";" +
                                "-fx-border-color:rgba(255,255,255,0.1);-fx-border-radius:20;" +
                                "-fx-background-radius:20;-fx-padding:6 16;-fx-cursor:hand;"));
                p.setStyle(
                        "-fx-background-color:rgba(56,189,248,0.15);-fx-border-color:" + PASTEL_SKY + ";" +
                                "-fx-text-fill:white;-fx-font-weight:bold;-fx-background-radius:20;" +
                                "-fx-border-radius:20;-fx-padding:6 16;-fx-cursor:hand;");
                refreshConseilsCards(cardsContainer, conseils, f);
            });
            filterBar.getChildren().add(p);
        }
        filterBox.getChildren().addAll(fLbl, filterBar);

        String startFilter = initialConseilFilter;
        initialConseilFilter = "Tous"; // Reset
        view.getChildren().addAll(kpiRow, filtresTabs, filterBox, cardsContainer);
        refreshConseilsCards(cardsContainer, conseils, startFilter);
        return styledScroll(view);
    }

    private void refreshConseilsCards(javafx.scene.layout.FlowPane container, List<Map<String, Object>> allData,
            String filter) {
        container.getChildren().clear();
        List<Map<String, Object>> filtered = allData.stream().filter(c -> {
            if (filter.equals("Tous"))
                return true;
            if (filter.equals("Actifs"))
                return "NOUVEAU".equals(c.get("etat"));
            if (filter.equals("Appliques"))
                return "ACCEPTE".equals(c.get("etat"));
            if (filter.equals("Ignores"))
                return "IGNORE".equals(c.get("etat"));
            String ctype = (String) c.get("type");
            if ("Promotion".equals(filter))
                return ctype != null && ctype.toLowerCase().contains("promo");
            if ("Destockage".equals(filter))
                return ctype != null && ctype.toLowerCase().contains("destock");
            if ("Bundle".equals(filter))
                return ctype != null && ctype.toLowerCase().contains("bundle");
            return filter.equalsIgnoreCase(ctype);
        }).collect(Collectors.toList());

        if (filtered.isEmpty()) {
            // Message vide stylisé avec icône
            VBox emptyBox = new VBox(12);
            emptyBox.setAlignment(Pos.CENTER);
            emptyBox.setPadding(new Insets(60, 0, 60, 0));
            emptyBox.setStyle(
                    "-fx-background-color:" + (darkMode ? "rgba(17,20,37,0.4)" : "rgba(248,250,252,0.6)") + ";" +
                            "-fx-background-radius:16;-fx-border-color:"
                            + (darkMode ? "rgba(56,189,248,0.1)" : "rgba(226,232,240,0.5)") + ";" +
                            "-fx-border-width:1;-fx-border-radius:16;-fx-padding:40;");
            emptyBox.setPrefWidth(600);

            Label emptyIcon = new Label("💡");
            emptyIcon.setStyle("-fx-font-size:48px;-fx-opacity:0.5;");

            Label emptyTitle = new Label("Aucun conseil disponible");
            emptyTitle.setStyle("-fx-text-fill:" + TEXT_1 + ";-fx-font-size:18px;-fx-font-weight:bold;");

            Label emptyMsg = new Label(
                    "Aucun conseil " + (filter.equals("Tous") ? "actif" : "dans cette catégorie") + " pour le moment.");
            emptyMsg.setStyle("-fx-text-fill:" + TEXT_3 + ";-fx-font-size:14px;");

            emptyBox.getChildren().addAll(emptyIcon, emptyTitle, emptyMsg);
            container.getChildren().add(emptyBox);
        } else {
            for (int i = 0; i < filtered.size(); i++) {
                Map<String, Object> c = filtered.get(i);
                int id = (int) c.get("id");
                String type = (String) c.get("type");
                String desc = (String) c.get("description");
                String urgence = (String) c.get("urgence");
                Object idProdObj = c.get("id_produit");
                int idProd = idProdObj instanceof Number ? ((Number) idProdObj).intValue() : -1;
                // Utilise le nom du produit récupéré directement par JOIN
                String nomProdJoin = (String) c.get("produit_nom");
                String nomProd;
                if (nomProdJoin != null && !nomProdJoin.isBlank()) {
                    // Nom récupéré par JOIN avec table produit
                    nomProd = nomProdJoin;
                } else if (idProd > 0) {
                    String n = conseilsIaService.getNomProduit(idProd);
                    nomProd = (n != null) ? n : ("Produit #" + idProd);
                } else {
                    // Pas de produit lié - extraire un titre depuis la description
                    nomProd = extraireNomDepuisDesc(desc, type);
                }
                int score = (int) c.get("score");
                String etat = (String) c.get("etat");
                boolean isApp = "ACCEPTE".equals(etat);

                VBox card = buildConseilCard(id, idProd, nomProd, type, PASTEL_SKY, PASTEL_SKY,
                        String.valueOf(score), desc, "", "", urgence, score, isApp);
                container.getChildren().add(card);

                // Animation entree decalee
                card.setOpacity(0);
                card.setTranslateY(20);
                FadeTransition ft = new FadeTransition(Duration.millis(350), card);
                ft.setToValue(1);
                ft.setDelay(Duration.millis(i * 60));
                TranslateTransition tt = new TranslateTransition(Duration.millis(350), card);
                tt.setToY(0);
                tt.setDelay(Duration.millis(i * 60));
                tt.setInterpolator(Interpolator.EASE_OUT);
                new ParallelTransition(ft, tt).play();
            }
        }
    }

    private String mapTypeToIcon(String type) {
        String t = type == null ? "" : type.toLowerCase();
        if (t.contains("promo"))
            return "\uD83D\uDE80";
        if (t.contains("destock"))
            return "\uD83D\uDCE6";
        if (t.contains("bundle"))
            return "\uD83D\uDD17";
        if (t.contains("mise en avant"))
            return "\u2B50"; // Star icon
        if (t.contains("stock"))
            return "\u26A0";
        return "\uD83D\uDCA1";
    }

    private String mapConseilColor(String type) {
        String t = type == null ? "" : type.toLowerCase();
        if (t.contains("promo"))
            return ROYAL;
        if (t.contains("destock"))
            return PASTEL_ROSE;
        if (t.contains("bundle"))
            return VIOLET;
        if (t.contains("mise en avant"))
            return GOLD; // Gold for Featured
        if (t.contains("stock"))
            return ORANGE;
        return NEON_GREEN;
    }

    // ==============================================
    // [7] MODULE CAMPAGNES MARKETING
    // ==============================================
    private ScrollPane buildModuleCampagnes() {
        VBox view = new VBox(0);
        view.setStyle("-fx-background-color: " + BG_DEEP + ";");

        HBox header = buildModHeader(
                "IA Active", "Campagnes Marketing",
                "Gerez vos actions . L'IA analyse les resultats automatiquement",
                null,
                new String[] { "+ Ajouter Campagne" });
        Button btnAdd = (Button) ((HBox) header.getChildren().get(2)).getChildren().get(0);
        btnAdd.setOnAction(e -> openAjouterCampagneDialog());

        // Bouton Email Cible Client dans le module campagnes - Palette Ocean Pro
        Button btnEmailCamp = new Button("\u2709  Email Vendeur");
        btnEmailCamp.setOnAction(e -> openEmailTargetDialog());
        btnEmailCamp.setPrefHeight(38);
        // Changé en gradient bleu Ocean Pro
        btnEmailCamp.setStyle(
                "-fx-background-color: linear-gradient(to right, #0ea5e9, #38bdf8);" +
                        "-fx-text-fill:white;-fx-font-weight:bold;" +
                        "-fx-font-size:13px;-fx-background-radius:9;-fx-cursor:hand;-fx-padding:0 16;");
        ((HBox) header.getChildren().get(2)).getChildren().add(0, btnEmailCamp);

        view.getChildren().add(header);

        List<CampagneMarketing> camps = campagnesService.getData();

        // KPI
        HBox kpiRow = new HBox(14);
        kpiRow.setPadding(new Insets(20, 32, 0, 32));
        double totalBudget = campagnesService.getBudgetTotal();
        double totalDepense = campagnesService.getDepenseTotal();

        // KPI avec sous-ligne EUR/USD
        String budgetSub = String.format("%.0f TND  \u2022  %s  \u2022  %s",
                totalBudget,
                edu.hanouti.services.ExchangeRateService.format(totalBudget, "EUR"),
                edu.hanouti.services.ExchangeRateService.format(totalBudget, "USD"));
        String depenseSub = String.format("%.0f TND  \u2022  %s  \u2022  %s",
                totalDepense,
                edu.hanouti.services.ExchangeRateService.format(totalDepense, "EUR"),
                edu.hanouti.services.ExchangeRateService.format(totalDepense, "USD"));
        kpiRow.getChildren().addAll(
                makeKpi("BUDGET TOTAL", String.format("%.0f TND", totalBudget), budgetSub, PASTEL_SKY),
                makeKpi("ACTIVES", String.valueOf(campagnesService.countByStatut("ACTIVE")), "en cours d'execution",
                        NEON_GREEN),
                makeKpi("TOTAL", String.valueOf(camps.size()), "Actions marketing enregistrees", ORANGE),
                makeKpi("DEPENSE", String.format("%.0f TND", totalDepense), depenseSub, VIOLET));

        // IA Conseils Auto (DYNAMIQUE)
        VBox iaSection = new VBox(12);
        iaSection.setPadding(new Insets(20, 32, 0, 32));
        iaSection.getChildren().add(makeSectionTitle("Conseils IA Automatiques"));
        HBox iaGrid = new HBox(12);

        List<Map<String, Object>> latestAdvice = conseilsIaService.getLatestAdvice(3);
        if (latestAdvice.isEmpty()) {
            iaGrid.getChildren().add(buildIaAutoCard("\uD83D\uDCA1 En attente",
                    "L'IA analyse vos donnees. Revenez bientot.", PASTEL_SKY));
        } else {
            for (Map<String, Object> advice : latestAdvice) {
                String type = (String) advice.get("type");
                String desc = (String) advice.get("description");
                String color = mapConseilColor(type);
                iaGrid.getChildren().add(buildIaAutoCard(mapTypeToIcon(type) + " " + type, desc, color));
            }
        }
        iaSection.getChildren().add(iaGrid);

        // Campagnes list - FILTRER les campagnes TERMINÉES
        VBox campSection = new VBox(12);
        campSection.setPadding(new Insets(20, 32, 32, 32));
        campSection.getChildren().add(makeSectionTitle("Vos Campagnes Actives"));

        // Palette creative - couleurs differentes par card
        String[] cardPalette = { PASTEL_SKY, NEON_GREEN, VIOLET, ORANGE, GOLD, PASTEL_ROSE, "#06b6d4" };
        int ci = 0;
        for (CampagneMarketing c : camps) {
            // Filtrer les campagnes TERMINÉES
            if ("TERMINEE".equalsIgnoreCase(c.getStatut())) {
                continue; // Sauter les campagnes terminées
            }
            String color = cardPalette[ci % cardPalette.length];
            int pct = c.getBudget() > 0 ? (int) ((c.getDepense() / c.getBudget()) * 100) : 0;
            campSection.getChildren().add(buildCampCard(c, color, pct));
            ci++;
        }

        view.getChildren().addAll(kpiRow, iaSection, campSection);
        return styledScroll(view);
    }

    /** Bouton compact : corrige le titre campagne via Gemini (cle GEMINI_API_KEY ou ~/.hanouti/gemini.properties). */
    private Button buildAiCampagneTitleButton(TextField titre, ComboBox<String> objectifCombo,
            ComboBox<String> canalCombo, Stage ownerDialog) {
        Button ai = new Button("IA");
        ai.setMinSize(40, 40);
        ai.setPrefSize(42, 40);
        ai.setMaxHeight(42);
        ai.setTooltip(new Tooltip("Corriger / peaufiner le titre avec Gemini"));
        ai.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: " + TEXT_1 + ";"
                + "-fx-background-color: rgba(139,92,246,0.35); -fx-background-radius: 10; -fx-cursor: hand;"
                + "-fx-border-color: rgba(139,92,246,0.65); -fx-border-radius: 10; -fx-border-width: 1;");
        ai.setOnAction(e -> {
            if (!geminiService.hasApiKey()) {
                showNotification("Gemini",
                        "Cle absente : variable GEMINI_API_KEY ou fichier .hanouti\\gemini.properties (apiKey=...).",
                        "error");
                return;
            }
            String draft = titre.getText() != null ? titre.getText().trim() : "";
            if (draft.isEmpty()) {
                showNotification("Titre", "Saisis un brouillon de titre avant l'IA.", "delete");
                return;
            }
            ai.setDisable(true);
            CompletableFuture.runAsync(() -> {
                try {
                    String obj = objectifCombo != null && objectifCombo.getValue() != null ? objectifCombo.getValue() : "";
                    String can = canalCombo != null && canalCombo.getValue() != null ? canalCombo.getValue() : "";
                    String fixed = geminiService.polishCampaignTitle(draft, obj, can);
                    Platform.runLater(() -> {
                        if (fixed != null && !fixed.isBlank())
                            titre.setText(fixed);
                        showNotification("IA", "Titre corrige.", "success");
                        ai.setDisable(false);
                    });
                } catch (Exception ex) {
                    Platform.runLater(() -> {
                        showNotification("IA", ex.getMessage() != null ? ex.getMessage() : "Erreur Gemini", "error");
                        ai.setDisable(false);
                    });
                }
            });
        });
        return ai;
    }

    // ── [7.1] Dialogues Campagnes (Ajouter / Modifier) ──
    private void openAjouterCampagneDialog() {
        Stage dialog = new Stage();
        dialog.initOwner(primaryStage);
        dialog.initModality(javafx.stage.Modality.APPLICATION_MODAL);
        dialog.initStyle(javafx.stage.StageStyle.UNDECORATED);

        // Couleurs adaptatives selon le thème
        String dialogBg = darkMode ? "#0a0f1e" : "#f8fafc";
        String dialogBorder = darkMode ? "#38bdf8" : "#60a5fa";
        String cardBg = darkMode ? "#0d1526" : "#ffffff";
        String iconBoxBg = darkMode ? "#0d2137" : "#dbeafe";
        String iconBoxBorder = darkMode ? "#38bdf8" : "#60a5fa";
        String labelColor = darkMode ? "#38bdf8" : "#60a5fa";
        String fieldBg = darkMode ? "#0d1526" : "#ffffff";
        String fieldText = darkMode ? "#f1f5f9" : "#0f172a";
        String fieldBorder = darkMode ? "#1e3a5f" : "#e2e8f0";
        String fieldPrompt = darkMode ? "#334155" : "#94a3b8";
        String tndBg = darkMode ? "transparent" : "#e0f2fe";
        String tndText = darkMode ? "#38bdf8" : "#0284c7";
        String btnGradient = darkMode ? "linear-gradient(to right,#2563eb,#38bdf8)"
                : "linear-gradient(to right,#60a5fa,#34d399)";
        String cancelBg = darkMode ? "#374151" : "#e5e7eb"; // Plus visible
        String cancelText = darkMode ? "#d1d5db" : "#4b5563"; // Texte plus contrasté
        String cancelBorder = darkMode ? "#4b5563" : "#cbd5e1"; // Bordure plus visible
        String sepColor = darkMode ? "#1e2a40" : "#e2e8f0";
        String hTitleColor = darkMode ? "#f1f5f9" : "#0f172a";
        String hSubColor = darkMode ? "#334155" : "#64748b";

        String microLbl = "-fx-text-fill:" + labelColor + ";-fx-font-size:10px;-fx-font-weight:bold;";
        String fieldStyle = "-fx-background-color:" + fieldBg + ";-fx-text-fill:" + fieldText + ";-fx-font-size:14px;" +
                "-fx-border-color:" + fieldBorder + ";-fx-border-width:1;" +
                "-fx-background-radius:8;-fx-border-radius:8;-fx-padding:10 14;" +
                "-fx-prompt-text-fill:" + fieldPrompt + ";";
        String comboStyle = "-fx-background-color:" + fieldBg + ";-fx-text-fill:" + fieldText + ";-fx-font-size:14px;" +
                "-fx-border-color:" + fieldBorder + ";-fx-border-width:1;" +
                "-fx-background-radius:8;-fx-border-radius:8;";
        // Style spécifique pour DatePicker - texte toujours visible
        String datePickerStyle = comboStyle +
                "-fx-control-inner-background:" + fieldBg + ";" +
                "-fx-control-inner-background-alt:" + fieldBg + ";" +
                "-fx-background:" + fieldBg + ";" +
                "-fx-text-fill:" + fieldText + ";";

        // -- Root --
        javafx.scene.layout.VBox root = new javafx.scene.layout.VBox(0);
        root.setStyle("-fx-background-color:" + dialogBg + ";-fx-border-color:" + dialogBorder + ";" +
                "-fx-border-width:2;-fx-border-radius:16;-fx-background-radius:16;");
        root.setPrefWidth(480);

        // -- Header --
        javafx.scene.layout.HBox header = new javafx.scene.layout.HBox(14);
        header.setStyle("-fx-padding:20 22 16 22;");
        header.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        javafx.scene.layout.StackPane iconBox = new javafx.scene.layout.StackPane();
        iconBox.setPrefSize(46, 46);
        iconBox.setMinSize(46, 46);
        iconBox.setStyle("-fx-background-color:" + iconBoxBg + ";-fx-background-radius:12;" +
                "-fx-border-color:" + iconBoxBorder + ";-fx-border-width:1;-fx-border-radius:12;");
        javafx.scene.control.Label iconLbl = new javafx.scene.control.Label("+");
        iconLbl.setStyle("-fx-font-size:18px;-fx-text-fill:" + labelColor + ";");
        iconBox.getChildren().add(iconLbl);
        javafx.scene.layout.VBox hTexts = new javafx.scene.layout.VBox(2);
        javafx.scene.control.Label hTitle = new javafx.scene.control.Label("Creer une campagne");
        hTitle.setStyle("-fx-text-fill:" + hTitleColor + ";-fx-font-size:17px;-fx-font-weight:bold;");
        javafx.scene.control.Label hSub = new javafx.scene.control.Label("ACTIONS MARKETING");
        hSub.setStyle("-fx-text-fill:" + hSubColor + ";-fx-font-size:10px;-fx-letter-spacing:1px;");
        hTexts.getChildren().addAll(hTitle, hSub);
        header.getChildren().addAll(iconBox, hTexts);

        javafx.scene.layout.HBox sep = new javafx.scene.layout.HBox();
        sep.setStyle("-fx-background-color:" + sepColor + ";-fx-pref-height:1;-fx-max-height:1;");

        // -- Corps --
        javafx.scene.layout.VBox body = new javafx.scene.layout.VBox(10);
        body.setStyle("-fx-padding:16 22 14 22;");

        // TITRE (+ combos objectif / canal declares tot pour le bouton IA)
        javafx.scene.control.Label lTitre = new javafx.scene.control.Label(".  TITRE");
        lTitre.setStyle(microLbl);
        javafx.scene.control.TextField titre = new javafx.scene.control.TextField();
        titre.setPromptText("Ex: Promo Ramadan");
        titre.setStyle(fieldStyle);
        titre.setPrefHeight(42);
        javafx.scene.control.ComboBox<String> type = new javafx.scene.control.ComboBox<>(
                FXCollections.observableArrayList("VENTES", "VISIBILITE", "FIDELISATION", "NOTORIETE"));
        type.setValue("VENTES");
        type.setStyle(comboStyle);
        type.setPrefHeight(42);
        type.setMaxWidth(Double.MAX_VALUE);
        javafx.scene.control.ComboBox<String> canal = new javafx.scene.control.ComboBox<>(
                FXCollections.observableArrayList("EMAIL", "BANNIERE", "SOCIAL", "PROMOTION", "SMS"));
        canal.setValue("EMAIL");
        canal.setStyle(comboStyle);
        canal.setPrefHeight(42);
        canal.setMaxWidth(Double.MAX_VALUE);
        javafx.scene.layout.HBox titreRow = new javafx.scene.layout.HBox(8);
        titreRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        javafx.scene.layout.HBox.setHgrow(titre, javafx.scene.layout.Priority.ALWAYS);
        titreRow.getChildren().addAll(titre, buildAiCampagneTitleButton(titre, type, canal, dialog));

        // OBJECTIF
        javafx.scene.control.Label lObj = new javafx.scene.control.Label(".  OBJECTIF");
        lObj.setStyle(microLbl);

        // CANAL
        javafx.scene.control.Label lCanal = new javafx.scene.control.Label("o  CANAL");
        lCanal.setStyle(microLbl);

        // BUDGET avec TND prefixe
        javafx.scene.control.Label lBudget = new javafx.scene.control.Label(".  BUDGET");
        lBudget.setStyle(microLbl);
        javafx.scene.layout.HBox budgetWrap = new javafx.scene.layout.HBox(0);
        budgetWrap.setStyle("-fx-background-color:" + fieldBg + ";-fx-border-color:" + fieldBorder + ";" +
                "-fx-border-width:1;-fx-background-radius:8;-fx-border-radius:8;");
        budgetWrap.setPrefHeight(42);
        budgetWrap.setMaxWidth(Double.MAX_VALUE);
        budgetWrap.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        javafx.scene.control.Label tndPfx = new javafx.scene.control.Label("TND");
        tndPfx.setMinWidth(42);
        tndPfx.setStyle("-fx-text-fill:" + tndText + ";-fx-font-size:12px;-fx-font-weight:bold;" +
                "-fx-padding:0 8 0 14;-fx-background-color:" + tndBg + ";");
        tndPfx.setAlignment(javafx.geometry.Pos.CENTER);
        javafx.scene.control.TextField budgetField = new javafx.scene.control.TextField();
        budgetField.setPromptText("0");
        budgetField.setStyle("-fx-background-color:transparent;-fx-text-fill:" + fieldText + ";" +
                "-fx-font-size:14px;-fx-border-color:transparent;-fx-prompt-text-fill:" + fieldPrompt + ";");
        javafx.scene.layout.HBox.setHgrow(budgetField, javafx.scene.layout.Priority.ALWAYS);
        budgetWrap.getChildren().addAll(tndPfx, budgetField);

        // DATES cote a cote
        javafx.scene.layout.HBox datesLblRow = new javafx.scene.layout.HBox(10);
        javafx.scene.control.Label lDD = new javafx.scene.control.Label(">  DATE DEBUT");
        lDD.setStyle(microLbl);
        lDD.setPrefWidth(210);
        javafx.scene.control.Label lDF = new javafx.scene.control.Label(">  DATE FIN");
        lDF.setStyle(microLbl);
        datesLblRow.getChildren().addAll(lDD, lDF);

        javafx.scene.layout.HBox datesRow = new javafx.scene.layout.HBox(10);
        javafx.scene.control.DatePicker dateDebut = new javafx.scene.control.DatePicker();
        dateDebut.setStyle(datePickerStyle);
        dateDebut.setPrefHeight(42);
        dateDebut.setPrefWidth(210);
        javafx.scene.control.DatePicker dateFin = new javafx.scene.control.DatePicker();
        dateFin.setStyle(datePickerStyle);
        dateFin.setPrefHeight(42);
        javafx.scene.layout.HBox.setHgrow(dateFin, javafx.scene.layout.Priority.ALWAYS);
        datesRow.getChildren().addAll(dateDebut, dateFin);

        // STATUT boutons cliquables — seulement ACTIVE et BROUILLON à la création
        javafx.scene.control.Label lStatut = new javafx.scene.control.Label("o  STATUT");
        lStatut.setStyle(microLbl);
        final String[] selStatut = { "ACTIVE" };
        String[] statOpts = { "ACTIVE", "BROUILLON" };
        String[] statDB = { "ACTIVE", "BROUILLON" };

        String btnActiveBg = darkMode ? "#1e3a5f" : "#dbeafe";
        String btnActiveText = darkMode ? "#38bdf8" : "#0284c7";
        String btnActiveBorder = darkMode ? "#38bdf8" : "#60a5fa";
        String btnInactiveBg = darkMode ? "#0d1526" : "#f8fafc";
        String btnInactiveText = darkMode ? "#334155" : "#94a3b8";
        String btnInactiveBorder = darkMode ? "#1e2a40" : "#e2e8f0";

        javafx.scene.layout.HBox statutRow = new javafx.scene.layout.HBox(8);
        javafx.scene.control.Button[] sBtns = new javafx.scene.control.Button[statOpts.length];
        for (int i = 0; i < statOpts.length; i++) {
            final String db = statDB[i];
            final int idx = i;
            javafx.scene.control.Button sb = new javafx.scene.control.Button(statOpts[i]);
            sb.setPrefHeight(34);
            javafx.scene.layout.HBox.setHgrow(sb, javafx.scene.layout.Priority.ALWAYS);
            boolean sel = i == 0;
            sb.setStyle(sel
                    ? "-fx-background-color:" + btnActiveBg + ";-fx-text-fill:" + btnActiveText
                            + ";-fx-font-weight:bold;-fx-font-size:11px;-fx-background-radius:8;-fx-border-color:"
                            + btnActiveBorder + ";-fx-border-width:1;-fx-border-radius:8;-fx-cursor:hand;"
                    : "-fx-background-color:" + btnInactiveBg + ";-fx-text-fill:" + btnInactiveText
                            + ";-fx-font-size:11px;-fx-background-radius:8;-fx-border-color:" + btnInactiveBorder
                            + ";-fx-border-width:1;-fx-border-radius:8;-fx-cursor:hand;");
            sBtns[i] = sb;
            sb.setOnAction(ev -> {
                selStatut[0] = db;
                for (int j = 0; j < sBtns.length; j++)
                    sBtns[j].setStyle(j == idx
                            ? "-fx-background-color:" + btnActiveBg + ";-fx-text-fill:" + btnActiveText
                                    + ";-fx-font-weight:bold;-fx-font-size:11px;-fx-background-radius:8;-fx-border-color:"
                                    + btnActiveBorder + ";-fx-border-width:1;-fx-border-radius:8;-fx-cursor:hand;"
                            : "-fx-background-color:" + btnInactiveBg + ";-fx-text-fill:" + btnInactiveText
                                    + ";-fx-font-size:11px;-fx-background-radius:8;-fx-border-color:"
                                    + btnInactiveBorder + ";-fx-border-width:1;-fx-border-radius:8;-fx-cursor:hand;");
            });
            statutRow.getChildren().add(sb);
        }

        body.getChildren().addAll(
                lTitre, titreRow,
                lObj, type,
                lCanal, canal,
                lBudget, budgetWrap,
                datesLblRow, datesRow,
                lStatut, statutRow);

        // -- Boutons --
        javafx.scene.layout.HBox btnBar = new javafx.scene.layout.HBox(10);
        btnBar.setStyle("-fx-padding:14 22 20 22;");
        btnBar.setAlignment(javafx.geometry.Pos.CENTER);

        javafx.scene.control.Button btnSave = new javafx.scene.control.Button("\uD83D\uDCBE  Sauvegarder");
        btnSave.setPrefHeight(44);
        btnSave.setPrefWidth(200);
        btnSave.setStyle("-fx-background-color:" + btnGradient + ";" +
                "-fx-text-fill:white;-fx-font-weight:bold;-fx-font-size:14px;" +
                "-fx-background-radius:10;-fx-cursor:hand;");

        javafx.scene.control.Button btnCancel = new javafx.scene.control.Button("Annuler");
        btnCancel.setPrefHeight(44);
        btnCancel.setPrefWidth(200);
        btnCancel.setStyle("-fx-background-color:" + cancelBg + ";-fx-text-fill:" + cancelText
                + ";-fx-font-size:14px;-fx-font-weight:600;" +
                "-fx-background-radius:10;-fx-cursor:hand;-fx-border-color:" + cancelBorder + ";" +
                "-fx-border-width:1.5;-fx-border-radius:10;");

        btnSave.setOnAction(e -> {
            // ── Validation complète ──
            String titreVal = titre.getText().trim();
            String canalVal = canal.getValue();
            String objectifVal = type.getValue();
            double budgetVal = 0;
            try {
                budgetVal = Double.parseDouble(budgetField.getText().trim());
            } catch (Exception ex) {
            }

            // 1. Titre obligatoire
            if (titreVal.isEmpty()) {
                showValidationError(dialog, "\u26A0 Titre obligatoire", "Veuillez saisir un titre pour la campagne.");
                return;
            }

            // 2. Budget > 0
            if (budgetVal <= 0) {
                showValidationError(dialog, "\u26A0 Budget invalide", "Le budget doit etre superieur a 0 TND.");
                return;
            }

            // 3. Dates obligatoires
            if (dateDebut.getValue() == null || dateFin.getValue() == null) {
                showValidationError(dialog, "\u26A0 Dates manquantes",
                        "Veuillez selectionner une date de debut et une date de fin.");
                return;
            }
            if (dateFin.getValue().isBefore(dateDebut.getValue())) {
                showValidationError(dialog, "\u26A0 Dates invalides",
                        "La date de fin doit etre apres la date de debut.");
                return;
            }

            // 4. Vérifier doublon exact (même titre)
            try {
                java.sql.PreparedStatement chk = edu.hanouti.utils.MyConnection.getConnection()
                        .prepareStatement("SELECT COUNT(*) FROM campagne_marketing WHERE nom=? AND vendor_id=1");
                chk.setString(1, titreVal);
                java.sql.ResultSet rs = chk.executeQuery();
                if (rs.next() && rs.getInt(1) > 0) {
                    showValidationError(dialog, "\u26A0 Campagne existante",
                            "Une campagne avec ce titre existe deja.\nModifiez le titre ou modifiez la campagne existante.");
                    return;
                }
            } catch (Exception ex) {
                System.err.println("Erreur check doublon: " + ex.getMessage());
            }

            // 5. Vérifier conflit canal + période
            try {
                java.sql.PreparedStatement chk2 = edu.hanouti.utils.MyConnection.getConnection()
                        .prepareStatement(
                                "SELECT nom FROM campagne_marketing WHERE type_action=? AND statut='ACTIVE' " +
                                        "AND date_debut <= ? AND date_fin >= ? AND vendor_id=1 LIMIT 1");
                chk2.setString(1, canalVal);
                chk2.setDate(2, java.sql.Date.valueOf(dateFin.getValue()));
                chk2.setDate(3, java.sql.Date.valueOf(dateDebut.getValue()));
                java.sql.ResultSet rs2 = chk2.executeQuery();
                if (rs2.next()) {
                    String conflictNom = rs2.getString("nom");
                    // Avertissement (pas bloquant)
                    javafx.scene.control.Alert warn = new javafx.scene.control.Alert(
                            javafx.scene.control.Alert.AlertType.WARNING);
                    warn.initOwner(dialog);
                    warn.setTitle("Conflit detecte");
                    warn.setHeaderText("\u26A0 Conflit de canal");
                    warn.setContentText("La campagne \"" + conflictNom + "\" utilise deja le canal " + canalVal +
                            " sur cette periode.\nVoulez-vous continuer quand meme ?");
                    javafx.scene.control.ButtonType btnContinuer = new javafx.scene.control.ButtonType("Continuer");
                    javafx.scene.control.ButtonType btnAnnulerW = new javafx.scene.control.ButtonType("Annuler");
                    warn.getButtonTypes().setAll(btnContinuer, btnAnnulerW);
                    java.util.Optional<javafx.scene.control.ButtonType> result = warn.showAndWait();
                    if (result.isEmpty() || result.get() == btnAnnulerW)
                        return;
                }
            } catch (Exception ex) {
                System.err.println("Erreur check conflit: " + ex.getMessage());
            }

            // ── Sauvegarder ──
            CampagneMarketing camp = new CampagneMarketing();
            camp.setNomCampagne(titreVal);
            camp.setObjectif(objectifVal);
            camp.setCanal(canalVal);
            camp.setBudget(budgetVal);
            camp.setDateDebut(java.sql.Date.valueOf(dateDebut.getValue()));
            camp.setDateFin(java.sql.Date.valueOf(dateFin.getValue()));
            camp.setStatut(selStatut[0]);
            camp.setDepense(0);
            camp.setIaScore(0);
            camp.setIaConseil("");
            campagnesService.addEntity(camp);
            dialog.close();

            // Afficher confirmation de succès
            showNotification("Campagne créée", "\"" + titreVal + "\" est maintenant active.", "success");

            naviguerVers("campagnes");
        });
        btnCancel.setOnAction(e -> dialog.close());

        btnBar.getChildren().addAll(btnSave, btnCancel);
        root.getChildren().addAll(header, sep, body, btnBar);

        javafx.scene.Scene scene = new javafx.scene.Scene(root);
        scene.setFill(javafx.scene.paint.Color.TRANSPARENT);
        dialog.setScene(scene);
        dialog.showAndWait();
    }

    /** Dialogue d'erreur de validation compact */
    private void showValidationError(Stage owner, String title, String msg) {
        Stage err = new Stage();
        err.initOwner(owner);
        err.initModality(javafx.stage.Modality.APPLICATION_MODAL);
        err.initStyle(javafx.stage.StageStyle.UNDECORATED);

        // Couleurs adaptatives
        String errorBg = darkMode ? "#0a0f1e" : "#fff7ed";
        String errorBorder = darkMode ? "#f97316" : "#fb923c";
        String errorTitle = darkMode ? "#f97316" : "#ea580c";
        String errorMsg = darkMode ? "#94a3b8" : "#64748b";
        String errorBtnBg = darkMode ? "#f97316" : "#fb923c";

        javafx.scene.layout.VBox root = new javafx.scene.layout.VBox(14);
        root.setStyle("-fx-background-color:" + errorBg + ";-fx-border-color:" + errorBorder + ";" +
                "-fx-border-width:2;-fx-border-radius:12;-fx-background-radius:12;-fx-padding:22 26 18 26;");
        root.setPrefWidth(340);
        root.setAlignment(javafx.geometry.Pos.CENTER);
        javafx.scene.control.Label t = new javafx.scene.control.Label(title);
        t.setStyle("-fx-text-fill:" + errorTitle + ";-fx-font-size:15px;-fx-font-weight:bold;");
        javafx.scene.control.Label m = new javafx.scene.control.Label(msg);
        m.setStyle("-fx-text-fill:" + errorMsg + ";-fx-font-size:13px;-fx-wrap-text:true;-fx-text-alignment:center;");
        m.setMaxWidth(290);
        m.setAlignment(javafx.geometry.Pos.CENTER);
        javafx.scene.control.Button ok = new javafx.scene.control.Button("OK");
        ok.setPrefWidth(100);
        ok.setPrefHeight(36);
        ok.setStyle("-fx-background-color:" + errorBtnBg + ";-fx-text-fill:white;-fx-font-weight:bold;" +
                "-fx-font-size:13px;-fx-background-radius:8;-fx-cursor:hand;");
        ok.setOnAction(ev -> err.close());
        root.getChildren().addAll(t, m, ok);
        javafx.scene.Scene s = new javafx.scene.Scene(root);
        s.setFill(javafx.scene.paint.Color.TRANSPARENT);
        err.setScene(s);
        err.showAndWait();
    }

    /** Dialogue de confirmation de succès avec auto-fermeture */
    private void showSuccessConfirmation(String title, String campagneName) {
        Stage success = new Stage();
        success.initOwner(primaryStage);
        success.initStyle(javafx.stage.StageStyle.UNDECORATED);

        // Couleurs adaptatives - Vert pastel
        String successBg = darkMode ? "#0a1410" : "#f0fdf4";
        String successBorder = darkMode ? "#10b981" : "#34d399";
        String successTitle = darkMode ? "#10b981" : "#059669";
        String successMsg = darkMode ? "#94a3b8" : "#64748b";
        String successIcon = darkMode ? "#10b981" : "#10b981";

        javafx.scene.layout.VBox root = new javafx.scene.layout.VBox(16);
        root.setStyle("-fx-background-color:" + successBg + ";-fx-border-color:" + successBorder + ";" +
                "-fx-border-width:2;-fx-border-radius:16;-fx-background-radius:16;-fx-padding:28 32 24 32;" +
                "-fx-effect:dropshadow(gaussian," + (darkMode ? "rgba(0,0,0,0.6)" : "rgba(0,0,0,0.15)")
                + ",20,0,0,6);");
        root.setPrefWidth(380);
        root.setAlignment(javafx.geometry.Pos.CENTER);

        // Icône checkmark animée
        javafx.scene.control.Label icon = new javafx.scene.control.Label("✓");
        icon.setStyle("-fx-font-size:48px;-fx-text-fill:" + successIcon + ";");
        icon.setEffect(new javafx.scene.effect.DropShadow(12, Color.web(successIcon, 0.5)));

        // Animation scale pour l'icône
        icon.setScaleX(0);
        icon.setScaleY(0);
        ScaleTransition scaleIcon = new ScaleTransition(Duration.millis(400), icon);
        scaleIcon.setToX(1);
        scaleIcon.setToY(1);
        scaleIcon.setInterpolator(Interpolator.EASE_OUT);
        scaleIcon.play();

        javafx.scene.control.Label t = new javafx.scene.control.Label(title);
        t.setStyle("-fx-text-fill:" + successTitle + ";-fx-font-size:17px;-fx-font-weight:bold;");

        javafx.scene.control.Label m = new javafx.scene.control.Label("« " + campagneName + " »");
        m.setStyle("-fx-text-fill:" + successMsg + ";-fx-font-size:14px;-fx-wrap-text:true;-fx-text-alignment:center;");
        m.setMaxWidth(320);
        m.setAlignment(javafx.geometry.Pos.CENTER);

        root.getChildren().addAll(icon, t, m);
        javafx.scene.Scene s = new javafx.scene.Scene(root);
        s.setFill(javafx.scene.paint.Color.TRANSPARENT);
        success.setScene(s);
        success.show();

        // Auto-fermeture après 2 secondes avec fade out
        PauseTransition pause = new PauseTransition(Duration.seconds(2));
        pause.setOnFinished(e -> {
            FadeTransition fade = new FadeTransition(Duration.millis(300), root);
            fade.setToValue(0);
            fade.setOnFinished(ev -> success.close());
            fade.play();
        });
        pause.play();
    }

    private void openModifierCampagneDialog(CampagneMarketing c) {
        Stage dialog = new Stage();
        dialog.initOwner(primaryStage);
        dialog.initModality(javafx.stage.Modality.APPLICATION_MODAL);
        dialog.initStyle(javafx.stage.StageStyle.UNDECORATED);

        // Couleurs adaptatives - Thème bleu pastel en mode clair
        String dialogBg = darkMode ? "#0a0f1e" : "#f8fafc"; // Gris clair
        String dialogBorder = darkMode ? "#38bdf8" : "#60a5fa"; // Bleu pastel
        String labelColor = darkMode ? "#38bdf8" : "#3b82f6"; // Bleu
        String fieldBg = darkMode ? "#ffffff" : "#ffffff"; // Blanc dans les deux
        String fieldText = darkMode ? "#1e3a5f" : "#0f172a"; // Texte foncé
        String fieldBorder = darkMode ? "#38bdf8" : "#60a5fa"; // Bleu pastel

        String microLbl2 = "-fx-text-fill:" + labelColor + ";-fx-font-size:10px;-fx-font-weight:bold;";
        String fieldStyle2 = "-fx-background-color:" + fieldBg + ";-fx-text-fill:" + fieldText
                + ";-fx-font-size:14px;-fx-font-weight:bold;" +
                "-fx-border-color:" + fieldBorder
                + ";-fx-border-width:1;-fx-background-radius:8;-fx-border-radius:8;-fx-padding:9 12;";
        String comboStyle2 = "-fx-background-color:" + fieldBg + ";-fx-text-fill:" + fieldText
                + ";-fx-font-size:14px;-fx-font-weight:bold;" +
                "-fx-border-color:" + fieldBorder + ";-fx-border-width:1;-fx-background-radius:8;-fx-border-radius:8;";
        // Style spécifique pour DatePicker - texte toujours visible
        String datePickerStyle2 = comboStyle2 +
                "-fx-control-inner-background:" + fieldBg + ";" +
                "-fx-control-inner-background-alt:" + fieldBg + ";" +
                "-fx-background:" + fieldBg + ";" +
                "-fx-text-fill:" + fieldText + ";";

        javafx.scene.layout.VBox root = new javafx.scene.layout.VBox(0);
        root.setStyle(
                "-fx-background-color:" + dialogBg + ";" +
                        "-fx-border-color:" + dialogBorder + ";" +
                        "-fx-border-width:2;-fx-border-radius:16;-fx-background-radius:16;" +
                        "-fx-effect:dropshadow(gaussian," + (darkMode ? "rgba(0,0,0,0.6)" : "rgba(0,0,0,0.15)")
                        + ",20,0,0,6);");
        root.setPrefWidth(500);

        // -- Header --
        String iconBg = darkMode ? "#0d2137" : "#dbeafe"; // Bleu très clair
        String titleColor = darkMode ? "#f1f5f9" : "#0f172a"; // Noir doux
        String subColor = darkMode ? "#334155" : "#64748b"; // Gris
        String sepColor = darkMode ? "#1e2a40" : "#e2e8f0"; // Séparateur

        javafx.scene.layout.HBox header = new javafx.scene.layout.HBox(14);
        header.setStyle("-fx-padding:20 22 16 22;");
        header.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        javafx.scene.layout.StackPane iconBox = new javafx.scene.layout.StackPane();
        iconBox.setPrefSize(48, 48);
        iconBox.setMinSize(48, 48);
        iconBox.setStyle(
                "-fx-background-color:" + iconBg + ";" +
                        "-fx-background-radius:12;" +
                        "-fx-border-color:" + fieldBorder + ";" +
                        "-fx-border-width:1;-fx-border-radius:12;");
        javafx.scene.control.Label iconLbl2 = new javafx.scene.control.Label("\u270F");
        iconLbl2.setStyle("-fx-font-size:20px;");
        iconBox.getChildren().add(iconLbl2);
        javafx.scene.layout.VBox hTexts = new javafx.scene.layout.VBox(2);
        javafx.scene.control.Label hTitle = new javafx.scene.control.Label("Modifier Campagne");
        hTitle.setStyle("-fx-text-fill:" + titleColor + ";-fx-font-size:18px;-fx-font-weight:bold;");
        javafx.scene.control.Label hSub = new javafx.scene.control.Label(
                c.getNomCampagne().toUpperCase() + "  .  ACTIONS MARKETING");
        hSub.setStyle("-fx-text-fill:" + subColor + ";-fx-font-size:10px;");
        hTexts.getChildren().addAll(hTitle, hSub);
        header.getChildren().addAll(iconBox, hTexts);

        javafx.scene.layout.HBox sep = new javafx.scene.layout.HBox();
        sep.setStyle("-fx-background-color:" + sepColor + ";-fx-pref-height:1;-fx-max-height:1;");

        // -- Corps --
        javafx.scene.layout.VBox body = new javafx.scene.layout.VBox(10);
        body.setStyle("-fx-padding:16 22 14 22;");

        // Barre budget dynamique
        String cardBg = darkMode ? "#0d1526" : "#ffffff"; // Blanc
        String cardBorder = darkMode ? "#1e3a5f" : "#cbd5e1"; // Gris
        String barBg = darkMode ? "#0d1a2e" : "#e0f2fe"; // Bleu clair
        String barFill = darkMode ? "#38bdf8" : "#60a5fa"; // Bleu pastel
        String tndBg = darkMode ? "#e8f4fd" : "#e0f2fe"; // Bleu clair
        String tndText = darkMode ? "#38bdf8" : "#0284c7"; // Bleu moyen

        javafx.scene.layout.VBox budgetCard = new javafx.scene.layout.VBox(8);
        budgetCard.setStyle(
                "-fx-background-color:" + cardBg + ";" +
                        "-fx-background-radius:10;" +
                        "-fx-border-color:" + cardBorder + ";" +
                        "-fx-border-width:1;-fx-border-radius:10;-fx-padding:12 14;");
        javafx.scene.layout.HBox bTopRow = new javafx.scene.layout.HBox();
        bTopRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        javafx.scene.control.Label bTopLbl = new javafx.scene.control.Label("BUDGET CONSOMME");
        bTopLbl.setStyle("-fx-text-fill:" + subColor + ";-fx-font-size:10px;-fx-font-weight:bold;");
        javafx.scene.layout.Region bSp = new javafx.scene.layout.Region();
        javafx.scene.layout.HBox.setHgrow(bSp, javafx.scene.layout.Priority.ALWAYS);
        javafx.scene.control.Label pctLbl = new javafx.scene.control.Label("0%");
        pctLbl.setStyle("-fx-text-fill:" + labelColor + ";-fx-font-size:13px;-fx-font-weight:bold;");
        bTopRow.getChildren().addAll(bTopLbl, bSp, pctLbl);
        javafx.scene.layout.StackPane progTrack = new javafx.scene.layout.StackPane();
        progTrack.setStyle(
                "-fx-background-color:" + barBg + ";-fx-background-radius:99;-fx-pref-height:6;-fx-max-height:6;");
        javafx.scene.layout.HBox progFill = new javafx.scene.layout.HBox();
        progFill.setStyle("-fx-background-color:" + barFill + ";-fx-background-radius:99;");
        progFill.setPrefHeight(6);
        progFill.setPrefWidth(0);
        javafx.scene.layout.StackPane.setAlignment(progFill, javafx.geometry.Pos.CENTER_LEFT);
        progTrack.getChildren().add(progFill);
        javafx.scene.layout.HBox bBotRow = new javafx.scene.layout.HBox();
        bBotRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        javafx.scene.control.Label depLbl = new javafx.scene.control.Label(
                String.format("%.0f TND depense", c.getDepense()));
        depLbl.setStyle("-fx-text-fill:" + labelColor + ";-fx-font-size:12px;-fx-font-weight:bold;");
        javafx.scene.layout.Region bSp2 = new javafx.scene.layout.Region();
        javafx.scene.layout.HBox.setHgrow(bSp2, javafx.scene.layout.Priority.ALWAYS);
        javafx.scene.control.Label surLbl = new javafx.scene.control.Label(
                String.format("sur %.0f TND", c.getBudget()));
        surLbl.setStyle("-fx-text-fill:" + subColor + ";-fx-font-size:11px;");
        bBotRow.getChildren().addAll(depLbl, bSp2, surLbl);
        budgetCard.getChildren().addAll(bTopRow, progTrack, bBotRow);

        // Titre (combos declares ici pour le bouton IA ; affichage : titre puis ligne type/canal)
        javafx.scene.control.ComboBox<String> typeCombo = new javafx.scene.control.ComboBox<>(
                FXCollections.observableArrayList("VENTES", "VISIBILITE", "FIDELISATION", "NOTORIETE"));
        typeCombo.setValue(c.getObjectif() != null ? c.getObjectif() : "VENTES");
        typeCombo.setStyle(comboStyle2);
        typeCombo.setPrefHeight(40);
        typeCombo.setPrefWidth(220);
        javafx.scene.control.ComboBox<String> canalCombo = new javafx.scene.control.ComboBox<>(
                FXCollections.observableArrayList("EMAIL", "BANNIERE", "SOCIAL", "PROMOTION", "SMS"));
        canalCombo.setValue(c.getCanal() != null ? c.getCanal() : "EMAIL");
        canalCombo.setStyle(comboStyle2);
        canalCombo.setPrefHeight(40);
        javafx.scene.layout.HBox.setHgrow(canalCombo, javafx.scene.layout.Priority.ALWAYS);

        javafx.scene.control.Label lTitre = new javafx.scene.control.Label(".  TITRE");
        lTitre.setStyle(microLbl2);
        javafx.scene.control.TextField titre = new javafx.scene.control.TextField(c.getNomCampagne());
        titre.setStyle(fieldStyle2);
        titre.setPrefHeight(40);
        javafx.scene.layout.HBox titreRow = new javafx.scene.layout.HBox(8);
        titreRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        javafx.scene.layout.HBox.setHgrow(titre, javafx.scene.layout.Priority.ALWAYS);
        titreRow.getChildren().addAll(titre, buildAiCampagneTitleButton(titre, typeCombo, canalCombo, dialog));

        // Type + Canal
        javafx.scene.layout.HBox r1L = new javafx.scene.layout.HBox(10);
        javafx.scene.control.Label lType = new javafx.scene.control.Label(".  TYPE");
        lType.setStyle(microLbl2);
        lType.setPrefWidth(220);
        javafx.scene.control.Label lCanal = new javafx.scene.control.Label("o  CANAL");
        lCanal.setStyle(microLbl2);
        r1L.getChildren().addAll(lType, lCanal);
        javafx.scene.layout.HBox r1F = new javafx.scene.layout.HBox(10);
        r1F.getChildren().addAll(typeCombo, canalCombo);

        // Budget + Depense avec TND
        javafx.scene.layout.HBox r2L = new javafx.scene.layout.HBox(10);
        javafx.scene.control.Label lBudget = new javafx.scene.control.Label(".  BUDGET");
        lBudget.setStyle(microLbl2);
        lBudget.setPrefWidth(220);
        javafx.scene.control.Label lDep = new javafx.scene.control.Label(".  DEPENSE");
        lDep.setStyle(microLbl2);
        r2L.getChildren().addAll(lBudget, lDep);
        javafx.scene.layout.HBox r2F = new javafx.scene.layout.HBox(10);

        // Budget - label TND separe a gauche du champ
        javafx.scene.layout.VBox bVBox = new javafx.scene.layout.VBox(0);
        bVBox.setPrefWidth(220);
        javafx.scene.layout.HBox bWrap = new javafx.scene.layout.HBox(0);
        bWrap.setStyle("-fx-background-color:" + fieldBg + ";-fx-border-color:" + fieldBorder
                + ";-fx-border-width:1;-fx-background-radius:8;-fx-border-radius:8;");
        bWrap.setPrefHeight(40);
        bWrap.setMaxWidth(Double.MAX_VALUE);
        bWrap.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        javafx.scene.control.Label tnd1 = new javafx.scene.control.Label(" TND ");
        tnd1.setPrefWidth(50);
        tnd1.setMinWidth(50);
        tnd1.setStyle(
                "-fx-text-fill:" + tndText + ";-fx-font-size:12px;-fx-font-weight:bold;" +
                        "-fx-background-color:" + tndBg + ";-fx-border-color:" + fieldBorder + ";" +
                        "-fx-border-width:0 1 0 0;-fx-padding:0 6 0 8;");
        tnd1.setAlignment(javafx.geometry.Pos.CENTER);
        javafx.scene.control.TextField budgetField = new javafx.scene.control.TextField(
                String.valueOf((int) c.getBudget()));
        budgetField.setStyle("-fx-background-color:transparent;-fx-text-fill:" + fieldText + ";" +
                "-fx-font-size:14px;-fx-font-weight:bold;-fx-border-color:transparent;-fx-padding:0 8;");
        javafx.scene.layout.HBox.setHgrow(budgetField, javafx.scene.layout.Priority.ALWAYS);
        bWrap.getChildren().addAll(tnd1, budgetField);
        bVBox.getChildren().add(bWrap);

        // Depense - label TND separe a gauche du champ
        javafx.scene.layout.HBox dWrap = new javafx.scene.layout.HBox(0);
        dWrap.setStyle("-fx-background-color:" + fieldBg + ";-fx-border-color:" + fieldBorder
                + ";-fx-border-width:1;-fx-background-radius:8;-fx-border-radius:8;");
        dWrap.setPrefHeight(40);
        javafx.scene.layout.HBox.setHgrow(dWrap, javafx.scene.layout.Priority.ALWAYS);
        dWrap.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        javafx.scene.control.Label tnd2 = new javafx.scene.control.Label(" TND ");
        tnd2.setPrefWidth(50);
        tnd2.setMinWidth(50);
        tnd2.setStyle(
                "-fx-text-fill:" + tndText + ";-fx-font-size:12px;-fx-font-weight:bold;" +
                        "-fx-background-color:" + tndBg + ";-fx-border-color:" + fieldBorder + ";" +
                        "-fx-border-width:0 1 0 0;-fx-padding:0 6 0 8;");
        tnd2.setAlignment(javafx.geometry.Pos.CENTER);
        javafx.scene.control.TextField depenseField = new javafx.scene.control.TextField(
                String.valueOf((int) c.getDepense()));
        depenseField.setStyle("-fx-background-color:transparent;-fx-text-fill:" + fieldText + ";" +
                "-fx-font-size:14px;-fx-font-weight:bold;-fx-border-color:transparent;-fx-padding:0 8;");
        javafx.scene.layout.HBox.setHgrow(depenseField, javafx.scene.layout.Priority.ALWAYS);
        dWrap.getChildren().addAll(tnd2, depenseField);
        r2F.getChildren().addAll(bVBox, dWrap);

        // Mise a jour barre en temps reel
        Runnable updateBar = () -> {
            try {
                double dep = Double.parseDouble(depenseField.getText().trim());
                double bud = Double.parseDouble(budgetField.getText().trim());
                if (bud > 0) {
                    double ratio = Math.min(dep / bud, 1.0);
                    pctLbl.setText((int) (ratio * 100) + "%");
                    progFill.setPrefWidth(ratio * 440);
                    depLbl.setText((int) dep + " TND depense");
                    surLbl.setText("sur " + (int) bud + " TND");
                }
            } catch (Exception ignored) {
            }
        };
        budgetField.textProperty().addListener((obs, o, n) -> updateBar.run());
        depenseField.textProperty().addListener((obs, o, n) -> updateBar.run());

        // Dates
        javafx.scene.layout.HBox r3L = new javafx.scene.layout.HBox(10);
        javafx.scene.control.Label lDD = new javafx.scene.control.Label(">  DATE DEBUT");
        lDD.setStyle(microLbl2);
        lDD.setPrefWidth(220);
        javafx.scene.control.Label lDF = new javafx.scene.control.Label(">  DATE FIN");
        lDF.setStyle(microLbl2);
        r3L.getChildren().addAll(lDD, lDF);
        javafx.scene.layout.HBox r3F = new javafx.scene.layout.HBox(10);
        javafx.scene.control.DatePicker dateDebutPicker = new javafx.scene.control.DatePicker();
        if (c.getDateDebut() != null)
            dateDebutPicker.setValue(c.getDateDebut().toLocalDate());
        dateDebutPicker.setStyle(datePickerStyle2);
        dateDebutPicker.setPrefHeight(40);
        dateDebutPicker.setPrefWidth(220);
        javafx.scene.control.DatePicker dateFinPicker = new javafx.scene.control.DatePicker();
        if (c.getDateFin() != null)
            dateFinPicker.setValue(c.getDateFin().toLocalDate());
        dateFinPicker.setStyle(datePickerStyle2);
        dateFinPicker.setPrefHeight(40);
        javafx.scene.layout.HBox.setHgrow(dateFinPicker, javafx.scene.layout.Priority.ALWAYS);
        r3F.getChildren().addAll(dateDebutPicker, dateFinPicker);

        // Statut boutons
        String btnActiveBg = darkMode ? "#1e3a5f" : "#dbeafe"; // Bleu clair
        String btnActiveText = darkMode ? "#38bdf8" : "#0284c7"; // Bleu moyen
        String btnInactiveBg = darkMode ? "#0d1526" : "#f1f5f9"; // Gris clair
        String btnInactiveText = darkMode ? "#334155" : "#64748b"; // Gris
        String btnInactiveBorder = darkMode ? "#1e2a40" : "#cbd5e1"; // Gris

        javafx.scene.control.Label lStatut = new javafx.scene.control.Label("o  STATUT");
        lStatut.setStyle(microLbl2);
        final String[] selStatut = { c.getStatut() != null ? c.getStatut() : "ACTIVE" };
        String[] statOpts = { "ACTIVE", "EN PAUSE", "TERMINEE", "BROUILLON" };
        String[] statDB = { "ACTIVE", "ACTIVE", "TERMINEE", "BROUILLON" };
        javafx.scene.layout.HBox statutRow = new javafx.scene.layout.HBox(8);
        javafx.scene.control.Button[] sBtns = new javafx.scene.control.Button[statOpts.length];
        for (int i = 0; i < statOpts.length; i++) {
            final String opt = statOpts[i];
            final String db = statDB[i];
            final int idx = i;
            javafx.scene.control.Button sb = new javafx.scene.control.Button(opt);
            sb.setPrefHeight(34);
            javafx.scene.layout.HBox.setHgrow(sb, javafx.scene.layout.Priority.ALWAYS);
            boolean sel = db.equals(selStatut[0]) || opt.equals(selStatut[0]);
            sb.setStyle(sel
                    ? "-fx-background-color:" + btnActiveBg + ";-fx-text-fill:" + btnActiveText
                            + ";-fx-font-weight:bold;-fx-font-size:11px;-fx-background-radius:8;-fx-border-color:"
                            + fieldBorder + ";-fx-border-width:1;-fx-border-radius:8;-fx-cursor:hand;"
                    : "-fx-background-color:" + btnInactiveBg + ";-fx-text-fill:" + btnInactiveText
                            + ";-fx-font-size:11px;-fx-background-radius:8;-fx-border-color:" + btnInactiveBorder
                            + ";-fx-border-width:1;-fx-border-radius:8;-fx-cursor:hand;");
            sBtns[i] = sb;
            sb.setOnAction(ev -> {
                selStatut[0] = db;
                for (int j = 0; j < sBtns.length; j++)
                    sBtns[j].setStyle(j == idx ? "-fx-background-color:" + btnActiveBg + ";-fx-text-fill:"
                            + btnActiveText
                            + ";-fx-font-weight:bold;-fx-font-size:11px;-fx-background-radius:8;-fx-border-color:"
                            + fieldBorder + ";-fx-border-width:1;-fx-border-radius:8;-fx-cursor:hand;"
                            : "-fx-background-color:" + btnInactiveBg + ";-fx-text-fill:" + btnInactiveText
                                    + ";-fx-font-size:11px;-fx-background-radius:8;-fx-border-color:"
                                    + btnInactiveBorder + ";-fx-border-width:1;-fx-border-radius:8;-fx-cursor:hand;");
            });
            statutRow.getChildren().add(sb);
        }

        body.getChildren().addAll(budgetCard, lTitre, titreRow, r1L, r1F, r2L, r2F, r3L, r3F, lStatut, statutRow);

        // -- Boutons --
        String btnSaveBg = darkMode ? "linear-gradient(to right,#2563eb,#38bdf8)" : "#3b82f6"; // Bleu
        String btnCancelBg = darkMode ? "#374151" : "#e5e7eb"; // Plus visible
        String btnCancelText = darkMode ? "#d1d5db" : "#4b5563"; // Texte plus contrasté
        String btnCancelBorder = darkMode ? "#4b5563" : "#cbd5e1"; // Bordure plus visible

        javafx.scene.layout.HBox btnBar = new javafx.scene.layout.HBox(10);
        btnBar.setStyle("-fx-padding:14 22 20 22;");
        btnBar.setAlignment(javafx.geometry.Pos.CENTER);

        javafx.scene.control.Button btnSave = new javafx.scene.control.Button("\u2705  Mettre a jour");
        btnSave.setPrefHeight(44);
        btnSave.setPrefWidth(200);
        btnSave.setStyle("-fx-background-color:" + btnSaveBg
                + ";-fx-text-fill:white;-fx-font-weight:bold;-fx-font-size:14px;-fx-background-radius:10;-fx-cursor:hand;");

        javafx.scene.control.Button btnCancel = new javafx.scene.control.Button("Annuler");
        btnCancel.setPrefHeight(44);
        btnCancel.setPrefWidth(200);
        btnCancel.setStyle(
                "-fx-background-color:" + btnCancelBg + ";" +
                        "-fx-text-fill:" + btnCancelText + ";" +
                        "-fx-font-size:14px;-fx-font-weight:600;-fx-background-radius:10;-fx-cursor:hand;" +
                        "-fx-border-color:" + btnCancelBorder + ";" +
                        "-fx-border-width:1.5;-fx-border-radius:10;");

        btnSave.setOnAction(e -> {
            c.setNomCampagne(titre.getText().trim());
            c.setObjectif(typeCombo.getValue());
            c.setCanal(canalCombo.getValue());
            try {
                c.setBudget(Double.parseDouble(budgetField.getText()));
            } catch (Exception ex) {
            }
            try {
                c.setDepense(Double.parseDouble(depenseField.getText()));
            } catch (Exception ex) {
            }
            if (dateDebutPicker.getValue() != null)
                c.setDateDebut(java.sql.Date.valueOf(dateDebutPicker.getValue()));
            if (dateFinPicker.getValue() != null)
                c.setDateFin(java.sql.Date.valueOf(dateFinPicker.getValue()));
            c.setStatut(selStatut[0]);
            campagnesService.updateEntity(c.getCampagneId(), c);
            dialog.close();

            // Afficher confirmation de succès
            showSuccessConfirmation("Campagne mise à jour !", c.getNomCampagne());

            naviguerVers("campagnes");
        });
        btnCancel.setOnAction(e -> dialog.close());
        btnBar.getChildren().addAll(btnSave, btnCancel);

        javafx.application.Platform.runLater(updateBar);
        root.getChildren().addAll(header, sep, body, btnBar);
        javafx.scene.Scene scene = new javafx.scene.Scene(root);
        scene.setFill(javafx.scene.paint.Color.TRANSPARENT);
        dialog.setScene(scene);
        dialog.showAndWait();
    }

    // ==============================================
    // [8] COMPOSANTS RÉUTILISABLES — KPI, Charts, Badges
    // ==============================================

    private HBox buildModHeader(String badgeText, String title, String sub,
            String[] filterOptions, String[] btnLabels) {
        HBox header = new HBox();
        header.setPadding(new Insets(28, 32, 20, 32));
        header.setAlignment(Pos.CENTER_LEFT);
        header.setStyle(
                "-fx-background-color: " + (darkMode ? "rgba(13,16,32,0.95)" : BG_SIDEBAR) + ";" +
                        "-fx-border-color: " + (darkMode ? "rgba(255,255,255,0.07)" : "rgba(0,0,0,0.08)") + ";" +
                        "-fx-border-width: 0 0 1 0;");

        VBox left = new VBox(6);
        HBox badge = new HBox(6);
        badge.setAlignment(Pos.CENTER_LEFT);
        Circle badgeDot = new Circle(3, Color.web(VIOLET));
        FadeTransition ft = new FadeTransition(Duration.seconds(1.5), badgeDot);
        ft.setFromValue(1);
        ft.setToValue(0.3);
        ft.setAutoReverse(true);
        ft.setCycleCount(Timeline.INDEFINITE);
        ft.play();
        Label badgeLbl = new Label(badgeText);
        badgeLbl.setStyle(
                "-fx-font-size: 10px; -fx-font-weight: bold;" +
                        "-fx-text-fill: " + VIOLET + ";" +
                        "-fx-background-color: rgba(139,92,246,0.18);" +
                        "-fx-padding: 3 10 3 10; -fx-background-radius: 99;");
        badge.getChildren().addAll(badgeDot, badgeLbl);

        Text titleLbl = new Text(title);
        titleLbl.setFont(Font.font("System", FontWeight.BOLD, 28));
        titleLbl.setFill(new LinearGradient(0, 0, 1, 0, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web(TEXT_1)), new Stop(1, Color.web(TEXT_3))));

        Label subLbl = new Label(sub);
        subLbl.setStyle("-fx-font-size: 13px; -fx-text-fill: " + TEXT_3 + ";");
        left.getChildren().addAll(badge, titleLbl, subLbl);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox right = new HBox(10);
        right.setAlignment(Pos.CENTER_RIGHT);
        if (filterOptions != null) {
            ComboBox<String> cb = new ComboBox<>();
            cb.getItems().addAll(filterOptions);
            cb.setValue(filterOptions[0]);
            cb.setStyle(
                    "-fx-background-color: " + BG_CARD + ";" +
                            "-fx-text-fill: " + TEXT_1 + ";" +
                            "-fx-border-color: rgba(255,255,255,0.07);" +
                            "-fx-border-radius: 10; -fx-background-radius: 10; -fx-font-size: 13px;");
            right.getChildren().add(cb);
        }
        if (btnLabels != null) {
            for (int i = 0; i < btnLabels.length; i++) {
                String lblText = btnLabels[i];
                Button b = new Button(lblText);
                String style;
                if (lblText.contains("Excel") || lblText.contains("Export")) {
                    style = "-fx-background-color: linear-gradient(to right, " + NEON_GREEN + ", #059669);" +
                            "-fx-text-fill: white; -fx-font-weight: bold;";
                } else if (lblText.startsWith("+") || lblText.contains("Ajouter") || lblText.contains("Actualiser")) {
                    style = "-fx-background-color: linear-gradient(to right, " + ROYAL + ", " + VIOLET + ");" +
                            "-fx-text-fill: white; -fx-font-weight: bold;";
                } else {
                    style = "-fx-background-color: rgba(255,255,255,0.06);" +
                            "-fx-text-fill: " + TEXT_2 + ";" +
                            "-fx-border-color: rgba(255,255,255,0.07); -fx-border-radius: 10;";
                }
                b.setStyle(style
                        + " -fx-padding: 10 18 10 18; -fx-background-radius: 10; -fx-cursor: hand; -fx-font-size: 13px;");
                right.getChildren().add(b);
            }
        }
        header.getChildren().addAll(left, spacer, right);
        return header;
    }

    private VBox makeKpi(String label, String value, String trend, String color) {
        VBox card = new VBox(6);
        card.setPadding(new Insets(22, 24, 22, 24));
        if (darkMode) {
            card.setStyle(
                    "-fx-background-color: " + BG_CARD + ";" +
                            "-fx-background-radius: 16; -fx-border-radius: 16;" +
                            "-fx-border-color: rgba(" + hexToRgb(color) + ", 0.4); -fx-border-width: 1.5;" +
                            "-fx-effect: dropshadow(gaussian, rgba(" + hexToRgb(color) + ", 0.15), 15, 0, 0, 0);");
        } else {
            card.setStyle(
                    "-fx-background-color: #ffffff;" +
                            "-fx-background-radius: 16; -fx-border-radius: 16;" +
                            "-fx-border-color: " + color
                            + " transparent transparent transparent; -fx-border-width: 3 0 0 0;" +
                            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 12, 0, 0, 2);");
        }
        HBox.setHgrow(card, Priority.ALWAYS);

        Label lbl = new Label(label);
        lbl.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: " + TEXT_3
                + "; -fx-letter-spacing: 0.1em; -fx-text-transform: uppercase;");

        Label val = new Label(value);
        val.setStyle("-fx-font-size: 32px; -fx-font-weight: bold; -fx-text-fill: " + color + ";");
        val.setEffect(new javafx.scene.effect.DropShadow(12, Color.web(color, 0.6)));

        boolean isUp = trend.contains("^") || trend.contains("+") || trend.contains("Tous");
        Label trendLbl = new Label(trend);
        // Couleur du trend = couleur de la card (pas de rouge)
        trendLbl.setStyle("-fx-font-size: 13px; -fx-text-fill: " + color + "; -fx-font-weight: 500; -fx-opacity: 0.8;");

        card.getChildren().addAll(lbl, val, trendLbl);
        card.setCursor(javafx.scene.Cursor.HAND);

        card.setOnMouseEntered(e -> {
            card.setStyle(card.getStyle().replace("0.4", "0.8").replace("1.5", "2.0"));
            card.setTranslateY(-5);
        });
        card.setOnMouseExited(e -> {
            card.setStyle(card.getStyle().replace("0.8", "0.4").replace("2.0", "1.5"));
            card.setTranslateY(0);
        });

        return card;
    }

    private HBox buildIaStrip(String text, String btnText) {
        HBox strip = new HBox(12);
        strip.setAlignment(Pos.CENTER_LEFT);
        strip.setPadding(new Insets(12, 18, 12, 18));
        strip.setStyle(
                "-fx-background-color: linear-gradient(to right, rgba(139,92,246,0.15), rgba(56,189,248,0.1), rgba(249,115,22,0.08));"
                        +
                        "-fx-border-color: " + VIOLET + ";" +
                        "-fx-border-width: 1.5;" +
                        "-fx-background-radius: 12; -fx-border-radius: 12; -fx-effect: dropshadow(gaussian, rgba(139,92,246,0.3), 10, 0, 0, 0);");
        Circle dot = new Circle(4, Color.web(ORANGE));
        dot.setEffect(new DropShadow(12, Color.web(ORANGE)));
        FadeTransition ft = new FadeTransition(Duration.seconds(1.5), dot);
        ft.setFromValue(1);
        ft.setToValue(0.3);
        ft.setAutoReverse(true);
        ft.setCycleCount(Timeline.INDEFINITE);
        ft.play();
        Label lbl = new Label(text);
        lbl.setStyle("-fx-font-size: 13px; -fx-text-fill: " + TEXT_2 + ";");
        lbl.setWrapText(true);
        HBox.setHgrow(lbl, Priority.ALWAYS);
        Button btn = new Button(btnText);
        btn.setOnAction(e -> naviguerVers("conseils"));

        btn.setStyle(
                "-fx-background-color: linear-gradient(to bottom right, " + VIOLET + ", " + ROYAL + ");" +
                        "-fx-text-fill: white;" +
                        "-fx-border-radius: 8; -fx-background-radius: 8;" +
                        "-fx-font-size: 12px; -fx-font-weight: bold; -fx-padding: 6 14 6 14; -fx-cursor: hand;");

        strip.getChildren().addAll(dot, lbl, btn);
        return strip;
    }

    private VBox buildBarChart(List<StatistiquesVentes> stats, String filter) {
        String title = "Revenus par " + (filter.equals("Toutes les periodes") ? "Semaine" : filter) + " (TND)";
        VBox card = makeChartCard(title);
        HBox bars = new HBox(16);
        bars.setAlignment(Pos.BOTTOM_LEFT);
        bars.setPrefHeight(160);
        bars.setPadding(new Insets(0, 0, 8, 0));

        String[] palette1 = { PASTEL_SKY, ROYAL, VIOLET, ORANGE, NEON_GREEN, PASTEL_ROSE, GOLD };
        String[] palette2 = { ROYAL, NEON_GREEN, PASTEL_ROSE, GOLD, PASTEL_SKY, VIOLET, ORANGE };

        Map<String, Double> weeklyRev = statsService.getRevenueByPeriod(filter);
        List<String> labels = new java.util.ArrayList<>(weeklyRev.keySet());
        if (labels.isEmpty()) {
            labels.add("Sem 1");
            labels.add("Sem 2");
            labels.add("Sem 3");
            labels.add("Sem 4");
        }
        int count = labels.size();

        double maxRev = weeklyRev.values().stream().mapToDouble(Double::doubleValue).max().orElse(2000);
        if (maxRev < 2000)
            maxRev = 2000;

        for (int i = 0; i < count; i++) {
            String labelText = labels.get(i);
            double rev = weeklyRev.getOrDefault(labelText, 0.0);
            if (rev == 0)
                rev = 1000 * (0.5 + Math.sin(i) * 0.3);

            VBox group = new VBox(6);
            group.setAlignment(Pos.BOTTOM_CENTER);
            HBox pair = new HBox(4);
            pair.setAlignment(Pos.BOTTOM_CENTER);

            double budget = rev * 0.7;
            double hRev = (rev / maxRev) * 120 + 10;
            double hBud = (budget / maxRev) * 120 + 10;

            String c1 = PASTEL_SKY; // Budget Color
            String c2 = NEON_GREEN; // Real Revenue Color

            Rectangle barBudget = makeBar(18, 0, c1, c1);
            Rectangle barRev = makeBar(18, 0, c2, c2);

            Timeline tl = new Timeline(
                    new KeyFrame(Duration.ZERO, new KeyValue(barBudget.heightProperty(), 0, Interpolator.EASE_BOTH),
                            new KeyValue(barRev.heightProperty(), 0, Interpolator.EASE_BOTH),
                            new KeyValue(group.opacityProperty(), 0)),
                    new KeyFrame(Duration.seconds(0.8),
                            new KeyValue(barBudget.heightProperty(), hBud, Interpolator.EASE_OUT),
                            new KeyValue(barRev.heightProperty(), hRev, Interpolator.EASE_OUT),
                            new KeyValue(group.opacityProperty(), 1)));
            tl.setDelay(Duration.seconds(i * 0.1));
            tl.play();

            Label lbl = new Label(labelText);
            lbl.setStyle("-fx-font-size: 10px; -fx-text-fill: " + TEXT_3
                    + "; -fx-border-color: rgba(255,255,255,0.05); -fx-border-width: 1 0 0 0; -fx-padding: 4 0 0 0;");
            pair.getChildren().addAll(barBudget, barRev);
            group.getChildren().addAll(pair, lbl);
            bars.getChildren().add(group);
        }
        HBox legend = new HBox(16);
        legend.setPadding(new Insets(8, 0, 0, 0));
        legend.getChildren().addAll(
                makeLegendItem(PASTEL_SKY, "Budget"),
                makeLegendItem(NEON_GREEN, "Revenus Reels"));
        HBox.setHgrow(card, Priority.ALWAYS);
        card.getChildren().addAll(bars, legend);
        return card;
    }

    private Rectangle makeBar(double w, double h, String c1, String c2) {
        Rectangle r = new Rectangle(w, h);
        r.setFill(new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web(c1)), new Stop(1, Color.web(c2 + "66"))));
        r.setArcWidth(4);
        r.setArcHeight(4);
        return r;
    }

    private VBox buildPieChart(String filter) {
        VBox card = makeChartCard("Repartition Strategique");
        card.setPrefWidth(280);
        card.setMaxWidth(280);

        HBox hexContainer = new HBox(8);
        hexContainer.setAlignment(Pos.CENTER);
        hexContainer.setPadding(new Insets(24, 0, 24, 0));

        List<javafx.scene.chart.PieChart.Data> dbData = statsService.getStrategicDistribution(filter);
        double total = dbData.stream().mapToDouble(javafx.scene.chart.PieChart.Data::getPieValue).sum();
        if (total == 0)
            total = 1;

        // Border colors from screenshot: Orange, Blue, Emerald, Pink
        String[] borderColors = { "#D97706", "#3B82F6", "#10B981", "#F472B6" };
        double[] hexSizes = { 120, 96, 78, 64 }; // Tailles augmentées

        int count = 0;
        for (javafx.scene.chart.PieChart.Data d : dbData) {
            if (count >= 4)
                break;

            StackPane hexWrap = new StackPane();
            double size = hexSizes[count];
            String color = borderColors[count % borderColors.length];

            javafx.scene.shape.Polygon poly = new javafx.scene.shape.Polygon();
            for (int i = 0; i < 6; i++) {
                poly.getPoints().addAll(
                        size / 2 + (size / 2) * Math.cos(i * 2 * Math.PI / 6),
                        size / 2 + (size / 2) * Math.sin(i * 2 * Math.PI / 6));
            }
            poly.setFill(Color.web("#1A2537")); // Dark fill as in screenshot
            poly.setStroke(Color.web(color));
            poly.setStrokeWidth(2.5);
            poly.setEffect(new javafx.scene.effect.DropShadow(12, Color.rgb(0, 0, 0, 0.6)));

            VBox txt = new VBox(-1);
            txt.setAlignment(Pos.CENTER);

            // Parse name|revenue
            String[] parts = d.getName().split("\\|");
            String ref = parts[0];
            String rev = parts.length > 1 ? parts[1] : "0";

            Label pct = new Label((int) ((d.getPieValue() / total) * 100) + "%");
            pct.setStyle("-fx-font-size: " + (size / 3.2) + "px; -fx-font-weight: bold; -fx-text-fill: " + color + ";");

            if (ref.startsWith("REF-"))
                ref = ref.substring(4);
            Label name = new Label(ref);
            name.setStyle("-fx-font-size: " + (size / 7.5) + "px; -fx-text-fill: #94A3B8; -fx-font-weight: bold;");

            Label revLbl = new Label(String.format("%,.0f TND", Double.parseDouble(rev)));
            revLbl.setStyle("-fx-font-size: " + (size / 8.5) + "px; -fx-text-fill: white; -fx-font-weight: 500;");

            txt.getChildren().addAll(pct, name, revLbl);
            hexWrap.getChildren().addAll(poly, txt);

            // CREATIVE ANIMATION: Scale + Rotate + Delay
            hexWrap.setScaleX(0);
            hexWrap.setScaleY(0);
            ScaleTransition st = new ScaleTransition(Duration.seconds(0.8), hexWrap);
            st.setDelay(Duration.seconds(count * 0.15));
            st.setToX(1);
            st.setToY(1);

            RotateTransition rt = new RotateTransition(Duration.seconds(1.2), hexWrap);
            rt.setDelay(Duration.seconds(count * 0.15));
            rt.setFromAngle(-45);
            rt.setToAngle(0);

            ParallelTransition pt = new ParallelTransition(st, rt);
            pt.play();

            // Floating effect
            Timeline floatAni = new Timeline(
                    new KeyFrame(Duration.ZERO, new KeyValue(hexWrap.translateYProperty(), 0)),
                    new KeyFrame(Duration.seconds(2 + count * 0.5),
                            new KeyValue(hexWrap.translateYProperty(), -8, Interpolator.EASE_BOTH)));
            floatAni.setAutoReverse(true);
            floatAni.setCycleCount(Timeline.INDEFINITE);
            floatAni.play();

            hexContainer.getChildren().add(hexWrap);
            count++;
        }

        card.getChildren().add(hexContainer);
        return card;
    }

    private VBox buildStatsTable(List<StatistiquesVentes> stats) {
        VBox wrap = new VBox(0);
        wrap.setStyle("-fx-background-color: " + BG_CARD + ";" +
                "-fx-background-radius: 14; -fx-border-color: rgba(255,255,255,0.07);" +
                "-fx-border-radius: 14;");
        HBox toolbar = new HBox(12);
        toolbar.setPadding(new Insets(12, 16, 12, 16));
        toolbar.setStyle("-fx-border-color: rgba(255,255,255,0.07); -fx-border-width: 0 0 1 0;");
        TextField search = new TextField();
        search.setPromptText("\uD83D\uDD0D  Recherche par Reference ou Periode...");
        search.setStyle("-fx-background-color: " + BG_DEEP + "; -fx-text-fill: " + TEXT_1 + ";" +
                "-fx-prompt-text-fill: " + TEXT_3 + "; -fx-border-color: rgba(255,255,255,0.07);" +
                "-fx-border-radius: 10; -fx-background-radius: 10; -fx-font-size: 13px; -fx-padding: 9 14 9 14;");
        HBox.setHgrow(search, Priority.ALWAYS);
        toolbar.getChildren().add(search);

        TableView<StatistiquesVentes> table = new TableView<>();
        table.setItems(FXCollections.observableArrayList(stats));
        table.setStyle("-fx-background-color: transparent; -fx-control-inner-background: transparent;" +
                "-fx-table-cell-border-color: rgba(255,255,255,0.04);");
        table.getStylesheets().add("data:text/css," +
                ".table-view .column-header { -fx-background-color: rgba(139,92,246,0.12); -fx-border-color: transparent transparent "
                + VIOLET + " transparent; -fx-border-width: 0 0 2 0; }" +
                ".table-view .column-header .label { -fx-text-fill: " + VIOLET
                + "; -fx-font-weight: bold; -fx-font-size: 11px; -fx-alignment: center-left; }" +
                ".table-view .corner { -fx-background-color: transparent; } " +
                ".table-row-cell:hover { -fx-background-color: rgba(255,255,255,0.05); } " +
                ".table-view .scroll-bar:vertical { -fx-background-color: transparent; } " +
                ".table-view .scroll-bar:vertical .thumb { -fx-background-color: #38bdf8; -fx-background-radius: 4; }");
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        search.textProperty().addListener((obs, old, val) -> {
            String kw = val.toLowerCase().trim();
            if (kw.isEmpty()) {
                table.setItems(FXCollections.observableArrayList(statsService.getData()));
                return;
            }
            // Normaliser la recherche (supprimer accents)
            String kwNorm = java.text.Normalizer.normalize(kw, java.text.Normalizer.Form.NFD)
                    .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");

            // Séparer résultats : commence par vs contient
            List<StatistiquesVentes> allStats = statsService.getData();
            List<StatistiquesVentes> startsWith = new java.util.ArrayList<>();
            List<StatistiquesVentes> contains = new java.util.ArrayList<>();

            for (StatistiquesVentes s : allStats) {
                String ref = s.getProduitId() != null ? s.getProduitId() : "";
                String refNorm = java.text.Normalizer.normalize(ref.toLowerCase(), java.text.Normalizer.Form.NFD)
                        .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
                // Enlever le préfixe REF- pour la recherche
                String refClean = refNorm.startsWith("ref-") ? refNorm.substring(4) : refNorm;

                if (refClean.startsWith(kwNorm))
                    startsWith.add(s);
                else if (refClean.contains(kwNorm))
                    contains.add(s);
            }

            // Combiner : priorité aux résultats qui commencent par la recherche
            List<StatistiquesVentes> filtered = new java.util.ArrayList<>(startsWith);
            filtered.addAll(contains);
            table.setItems(FXCollections.observableArrayList(filtered));
        });

        String[] headers = { "Produit / Ref", "Periode", "Sem", "Ventes", "Revenu TND", "Retour", "Statut" };
        for (int i = 0; i < headers.length; i++) {
            TableColumn<StatistiquesVentes, String> col = new TableColumn<>(headers[i]);
            final int idx = i;
            col.setCellValueFactory(cd -> {
                StatistiquesVentes s = cd.getValue();
                String val = "";
                if (idx == 0) {
                    val = (s.getProduitNom() != null ? s.getProduitNom() : s.getProduitId());
                    if (val != null && val.matches("\\d+"))
                        val = "REF-PROD-" + val;
                } else if (idx == 1)
                    val = s.getPeriode();
                else if (idx == 2)
                    val = String.valueOf(s.getSemaine());
                else if (idx == 3)
                    val = String.valueOf(s.getTotalVendu());
                else if (idx == 4)
                    val = String.format("%.0f", s.getRevenuTotal()).replace(",", " ");
                else if (idx == 5)
                    val = s.getTauxRetour() + "%";
                else if (idx == 6) {
                    // Vues depuis interaction_utilisateur
                    val = String.valueOf(getVuesProduit(s.getProduitId()));
                }
                return new javafx.beans.property.SimpleStringProperty(val);
            });
            col.setCellFactory(column -> new TableCell<StatistiquesVentes, String>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    setGraphic(null);
                    if (item == null || empty) {
                        setText(null);
                        setStyle("");
                    } else {
                        StatistiquesVentes s = getTableRow() != null ? getTableRow().getItem() : null;
                        if (idx == 0) {
                            setText(item);
                            setStyle("-fx-text-fill: white; -fx-font-weight: bold;");
                        } else if (idx == 3) {
                            setText(item);
                            setStyle("-fx-text-fill: " + (s != null && s.getTotalVendu() > 30 ? NEON_GREEN : PASTEL_SKY)
                                    + "; -fx-font-weight: bold;");
                        } else if (idx == 4) {
                            setText(item);
                            setStyle("-fx-text-fill: " + TEXT_1 + ";");
                        } else if (idx == 5) {
                            if (s != null && s.getTauxRetour() > 4) {
                                setText("\u26A0 " + item);
                                setStyle("-fx-text-fill: " + PASTEL_ROSE + ";");
                            } else {
                                setText(item);
                                setStyle("-fx-text-fill: "
                                        + (s != null && s.getTauxRetour() < 2 ? NEON_GREEN : PASTEL_SKY) + ";");
                            }
                        } else if (idx == 6) {
                            setText("");
                            int vues = 0;
                            try { vues = Integer.parseInt(item); } catch (Exception ex) {}
                            Label badge = new Label("\uD83D\uDC41 " + vues + " vues");
                            String bg = "rgba(255,255,255,0.1)";
                            String textC = TEXT_2;
                            if (vues >= 100) {
                                bg = "rgba(16,185,129,0.2)";
                                textC = NEON_GREEN;
                            } else if (vues >= 10) {
                                bg = "rgba(56,189,248,0.2)";
                                textC = PASTEL_SKY;
                            }
                            badge.setStyle("-fx-background-color: " + bg + "; -fx-text-fill: " + textC
                                    + "; -fx-padding: 2 8 2 8; -fx-background-radius: 6; -fx-font-size: 11px; -fx-font-weight: bold;");
                            setGraphic(badge);
                            setStyle("");
                        } else {
                            setText(item);
                            setStyle("-fx-text-fill: " + TEXT_2 + ";");
                        }
                    }
                }
            });
            col.setStyle("-fx-font-size: 12px;");
            table.getColumns().add(col);
        }

        table.setPrefHeight(250);
        VBox.setVgrow(table, Priority.ALWAYS);
        wrap.getChildren().addAll(toolbar, table);
        return wrap;
    }

    private HBox buildAlertCard(String icon, String boldMsg, String normalMsg, String time, String color) {
        HBox card = new HBox(12);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(12, 20, 12, 20));

        // Style adapté au thème
        String bgColor = darkMode ? "#0d1020" : "#f8fafc";
        String borderColor = darkMode ? "rgba(" + hexToRgb(color) + ", 0.25)" : "rgba(" + hexToRgb(color) + ", 0.2)";
        card.setStyle(
                "-fx-background-color: " + bgColor + ";" +
                        "-fx-border-color: " + borderColor + ";" +
                        "-fx-border-radius: 10; -fx-background-radius: 10;" +
                        "-fx-border-width: 1;");

        Circle dot = new Circle(4);
        dot.setFill(Color.web(color));
        if (darkMode) {
            dot.setEffect(new javafx.scene.effect.DropShadow(12, Color.web(color, 0.8)));
        } else {
            dot.setEffect(new javafx.scene.effect.DropShadow(6, Color.web(color, 0.4)));
        }

        Label main = new Label(boldMsg);
        main.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: " + color + ";");

        Label sub = new Label(normalMsg);
        sub.setStyle("-fx-font-size: 13px; -fx-text-fill: " + TEXT_2 + ";");
        sub.setWrapText(false);

        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);

        Label tL = new Label(time);
        tL.setStyle("-fx-font-size: 12px; -fx-text-fill: " + color + "; -fx-font-weight: bold;");

        card.getChildren().addAll(dot, main, sub, sp, tL);

        // CREATIVE MOVEMENT: Constant Floating
        TranslateTransition floating = new TranslateTransition(Duration.seconds(2.5 + Math.random()), card);
        floating.setByY(-4);
        floating.setAutoReverse(true);
        floating.setCycleCount(Timeline.INDEFINITE);
        floating.setInterpolator(Interpolator.EASE_BOTH);
        floating.play();

        // Pulsing glow on the dot
        ScaleTransition pulse = new ScaleTransition(Duration.seconds(1.5), dot);
        pulse.setFromX(1);
        pulse.setFromY(1);
        pulse.setToX(1.3);
        pulse.setToY(1.3);
        pulse.setAutoReverse(true);
        pulse.setCycleCount(Timeline.INDEFINITE);
        pulse.play();

        return card;
    }

    /** Extrait un titre lisible depuis la description quand aucun produit n'est lié */
    private String extraireNomDepuisDesc(String desc, String type) {
        if (desc == null || desc.isBlank())
            return type != null ? type : "Conseil";
        // Cherche "id=XX" pour récupérer le nom depuis la DB
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("id[=\\s]*(\\d+)").matcher(desc);
        if (m.find()) {
            try {
                int idExtrait = Integer.parseInt(m.group(1));
                String nom = conseilsIaService.getNomProduit(idExtrait);
                if (nom != null && !nom.isBlank()) return nom;
            } catch (NumberFormatException ignored) {}
        }
        // Cherche le premier mot significatif en majuscule (nom propre produit)
        String[] mots = desc.split("[\\s,\\.]+");
        for (String mot : mots) {
            if (mot.length() >= 4 && Character.isUpperCase(mot.charAt(0))
                    && !mot.equalsIgnoreCase("Lance") && !mot.equalsIgnoreCase("Acheteur")
                    && !mot.equalsIgnoreCase("Offre") && !mot.equalsIgnoreCase("Les")
                    && !mot.equalsIgnoreCase("Le") && !mot.equalsIgnoreCase("La")) {
                return mot;
            }
        }
        // Fallback : les 4 premiers mots de la description
        String[] w = desc.split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < Math.min(4, w.length); i++) {
            if (i > 0) sb.append(" ");
            sb.append(w[i]);
        }
        return sb.toString();
    }

    private VBox buildConseilCard(int id, int idProduit, String prodName, String type, String nameColor,
            String accentColor, String progress, String desc,
            String impact, String expiry, String urgency, int recommendedPct, boolean done) {

        // Couleur du contour selon le type - creative et variee
        String borderCol;
        String t = type != null ? type.toLowerCase() : "";
        if (t.contains("promo"))
            borderCol = "#38bdf8"; // bleu neon
        else if (t.contains("destock"))
            borderCol = "#f472b6"; // rose
        else if (t.contains("bundle"))
            borderCol = "#8b5cf6"; // violet
        else if (t.contains("mise"))
            borderCol = GOLD; // Gold
        else if (t.contains("stock"))
            borderCol = "#f97316"; // orange
        else
            borderCol = "#38bdf8"; // bleu neon par defaut

        VBox card = new VBox(12);
        card.setPrefWidth(330);
        card.setPadding(new Insets(18));
        card.setStyle(
                "-fx-background-color: " + (darkMode ? "#0d1526" : "#ffffff") + ";" +
                        "-fx-background-radius: 14;" +
                        "-fx-border-color: " + borderCol + ";" +
                        "-fx-border-width: 1.5;" +
                        "-fx-border-radius: 14;" +
                        "-fx-effect: dropshadow(gaussian," + borderCol + ",12,0.25,0,0);");

        // -- En-tete --
        HBox head = new HBox(10);
        head.setAlignment(Pos.CENTER_LEFT);
        Label titleL = new Label(prodName);
        titleL.setStyle("-fx-font-size: 15px; -fx-font-weight: 800; -fx-text-fill: " + TEXT_1 + ";");
        titleL.setWrapText(true);
        titleL.setMinWidth(0);
        HBox.setHgrow(titleL, Priority.ALWAYS);

        Region hs = new Region();
        HBox.setHgrow(hs, Priority.ALWAYS);

        Label typeB = new Label(type);
        typeB.setStyle(
                "-fx-font-size: 10px; -fx-font-weight: 900;" +
                        "-fx-text-fill: " + borderCol + ";" +
                        "-fx-background-color: " + borderCol + "1A;" + // 10% opacity
                        "-fx-padding: 3 12; -fx-background-radius: 99;" +
                        "-fx-border-color: " + borderCol + "; -fx-border-width: 1; -fx-border-radius: 99;");

        // Hide "Mise en avant" text as requested
        if (t.contains("mise")) {
            typeB.setVisible(false);
            typeB.setManaged(false);
        }

        Button btnQrShare = new Button("QR");
        btnQrShare.setMinSize(32, 26);
        btnQrShare.setPrefSize(34, 26);
        btnQrShare.setMaxSize(34, 26);
        btnQrShare.setTooltip(new Tooltip("Téléphone sur le même Wi‑Fi : QR ouvre la page conseil (serveur local)."));
        btnQrShare.setStyle(
                "-fx-font-size: 9px; -fx-font-weight: bold;"
                        + "-fx-background-color: rgba(148,163,184,0.2);"
                        + "-fx-text-fill: " + TEXT_2 + ";"
                        + "-fx-background-radius: 6; -fx-padding: 2 6; -fx-cursor: hand;");
        btnQrShare.setOnAction(e -> openConseilQrSharePopup(id));

        head.getChildren().addAll(titleL, hs, btnQrShare, typeB);

        // -- Description --
        Label descL = new Label(desc);
        descL.setWrapText(true);
        descL.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 13px; -fx-line-spacing: 2;");

        // -- Indicateurs - sans repetition --
        HBox indicators = new HBox(8);
        // Urgence seulement (pas "Impact Impact Eleve")
        String urgCol = urgency != null && urgency.toLowerCase().contains("urgent") ? PASTEL_ROSE
                : urgency != null && urgency.toLowerCase().contains("elev") ? GOLD
                        : "#94a3b8";
        indicators.getChildren().addAll(
                makeSmallTag("\u26A1 " + (urgency != null ? urgency : "Moyen"), urgCol),
                makeSmallTag(
                        "\uD83D\uDCCA " + String.format("%.0f%%", Double.parseDouble(progress.replace("%", "").trim())),
                        borderCol));

        // -- Actions --
        HBox actions = new HBox(8);
        actions.setAlignment(Pos.CENTER);

        Button btnApp = new Button(done ? "\u2705 Applique" : "\u2705 Accepter");
        btnApp.setPrefWidth(100);
        btnApp.setStyle(
                "-fx-background-color: " + (done ? "rgba(16,185,129,0.12)" : "#2563eb") + ";" +
                        "-fx-text-fill: " + (done ? "#10b981" : "white") + ";" +
                        "-fx-font-weight: bold; -fx-background-radius: 8; -fx-padding: 9; -fx-cursor: hand;");
        btnApp.setOnAction(e -> {
            if (!done) {
                String tl = type == null ? "" : type.toLowerCase();
                boolean isPromo = tl.contains("promo");
                boolean isDestock = tl.contains("destock");
                boolean isBundle = tl.contains("bundle");
                if (isPromo) {
                    openPromotionConfigDialog(id, prodName, type, recommendedPct, card);
                } else if (isDestock) {
                    openDestockageConfirmDialog(id, idProduit, prodName, card);
                } else if (isBundle) {
                    openBundleConfigDialog(id, prodName);
                } else {
                    conseilsIaService.accepter(id);
                    showNotification("Conseil Appliqué", "Action activée pour " + prodName, "success");
                    animateExit(card);
                }
            }
        });

        Button btnIgn = new Button("\uD83D\uDEAB Ignorer");
        btnIgn.setPrefWidth(100);
        btnIgn.setStyle(
                "-fx-background-color: rgba(244,114,182,0.1);" +
                        "-fx-text-fill: #f472b6;" +
                        "-fx-font-weight: bold; -fx-background-radius: 8; -fx-padding: 9; -fx-cursor: hand;");
        btnIgn.setOnAction(e -> {
            conseilsIaService.ignorer(id);
            showNotification("Élément Retiré", "Le conseil a été ignoré et supprimé.", "delete");
            // Animation creative : la card disparait et les voisines se rapprochent
            animateIgnore(card);
        });

        Button btnPlus = new Button("\uD83D\uDCAC Plus");
        btnPlus.setPrefWidth(75);
        btnPlus.setStyle(
                "-fx-background-color: rgba(56,189,248,0.1);" +
                        "-fx-text-fill: #38bdf8;" +
                        "-fx-font-weight: bold; -fx-background-radius: 8; -fx-padding: 9; -fx-cursor: hand;");
        btnPlus.setOnAction(e -> showSimilarProductsPopup(null));

        actions.getChildren().addAll(btnApp, btnIgn, btnPlus);
        card.getChildren().addAll(head, descL, indicators, actions);
        return card;
    }

    /** QR + mini serveur HTTP LAN (ZXing + JDK HttpServer) pour ouvrir le conseil sur le téléphone. */
    private void openConseilQrSharePopup(int idConseil) {
        if (primaryStage == null) {
            showNotification("QR", "Fenêtre principale indisponible.", "error");
            return;
        }
        try {
            int p = ConseilShareServer.ensureStarted(conseilsIaService);
            List<String> ips = ConseilShareServer.rankedLanIpv4Addresses();
            String ip = ips.isEmpty() ? "127.0.0.1" : ips.get(0);
            String url = "http://" + ip + ":" + p + "/c?id=" + idConseil;
            javafx.scene.image.Image qrImg = QrCodeFx.toImage(url, 240);

            Stage dlg = new Stage();
            dlg.initModality(Modality.WINDOW_MODAL);
            dlg.initOwner(primaryStage);
            dlg.setTitle("Partager le conseil");

            ImageView iv = new ImageView(qrImg);
            iv.setPreserveRatio(true);
            iv.setSmooth(true);

            Label hint = new Label(
                    "Telephone sur le meme Wi-Fi que ce PC. Si Safari affiche une page blanche, le telephone "
                            + "n'atteint pas cette IP (souvent 192.168.146.x = carte VMware, pas le Wi-Fi du salon). "
                            + "Essaie un des autres liens ci-dessous, ou verifie l'IP du PC dans Parametres reseau Windows.");
            hint.setWrapText(true);
            hint.setMaxWidth(380);
            hint.setStyle("-fx-text-fill: " + TEXT_2 + "; -fx-font-size: 12px;");

            Label urlLbl = new Label(url);
            urlLbl.setWrapText(true);
            urlLbl.setMaxWidth(380);
            urlLbl.setStyle("-fx-font-family: 'Consolas','monospace'; -fx-font-size: 11px; -fx-text-fill: " + TEXT_1 + ";");

            Button copy = new Button("Copier le lien");
            copy.setStyle("-fx-cursor: hand;");
            copy.setOnAction(ev -> {
                ClipboardContent cc = new ClipboardContent();
                cc.putString(url);
                Clipboard.getSystemClipboard().setContent(cc);
                showNotification("Copié", "Lien dans le presse-papiers.", "success");
            });

            Button close = new Button("Fermer");
            close.setStyle("-fx-cursor: hand;");
            close.setOnAction(ev -> dlg.close());

            HBox btns = new HBox(10, copy, close);
            btns.setAlignment(Pos.CENTER);

            VBox root = new VBox(14, iv, hint, urlLbl);
            root.setAlignment(Pos.CENTER);
            if (ips.size() > 1) {
                StringBuilder sb = new StringBuilder("Autres IP sur ce PC (copie-colle dans Safari si besoin) :\n");
                for (int i = 1; i < Math.min(ips.size(), 8); i++) {
                    sb.append("\nhttp://").append(ips.get(i)).append(":").append(p).append("/c?id=").append(idConseil);
                }
                Label alt = new Label(sb.toString());
                alt.setWrapText(true);
                alt.setMaxWidth(380);
                alt.setStyle("-fx-text-fill: " + TEXT_3 + "; -fx-font-size: 11px; -fx-font-family: 'Consolas','monospace';");
                root.getChildren().add(alt);
            }
            root.getChildren().add(btns);
            root.setPadding(new Insets(22));
            root.setStyle("-fx-background-color: " + (darkMode ? BG_CARD : "#f1f5f9") + ";");

            ScrollPane sp = new ScrollPane(root);
            sp.setFitToWidth(true);
            sp.setPrefViewportHeight(480);
            sp.setPrefViewportWidth(420);
            sp.setStyle("-fx-background: transparent; -fx-background-color: transparent;");

            Scene sc = new Scene(sp);
            dlg.setScene(sc);
            dlg.setResizable(true);
            dlg.show();
        } catch (Throwable ex) {
            showNotification("QR", "Impossible de démarrer le partage : " + ex.getMessage(), "error");
        }
    }

    /**
     * Animation ignore creative : shrink + fade + les voisines glissent doucement
     */
    private void animateIgnore(VBox card) {
        // Phase 1 : scale down + fade out (200ms)
        ScaleTransition st = new ScaleTransition(Duration.millis(220), card);
        st.setToX(0.8);
        st.setToY(0.8);
        FadeTransition ft = new FadeTransition(Duration.millis(220), card);
        ft.setToValue(0);
        ParallelTransition phase1 = new ParallelTransition(st, ft);

        phase1.setOnFinished(e -> {
            javafx.scene.layout.Pane parent = (javafx.scene.layout.Pane) card.getParent();
            if (parent == null)
                return;

            // Recuperer les voisines APRES la card supprimee
            int idx = parent.getChildren().indexOf(card);
            List<javafx.scene.Node> after = new java.util.ArrayList<>(
                    parent.getChildren().subList(Math.min(idx + 1, parent.getChildren().size()),
                            parent.getChildren().size()));

            // Animer les voisines : elles glissent vers le haut lentement (600ms)
            for (int i = 0; i < after.size(); i++) {
                javafx.scene.Node neighbor = after.get(i);
                // Decaler legerement chaque voisine pour un effet cascade
                TranslateTransition slide = new TranslateTransition(Duration.millis(600), neighbor);
                slide.setFromY(neighbor.getTranslateY());
                slide.setToY(0);
                slide.setDelay(Duration.millis(i * 30));
                slide.setInterpolator(Interpolator.SPLINE(0.25, 0.1, 0.25, 1.0));
                // Legere rotation pour effet creatif
                RotateTransition rot = new RotateTransition(Duration.millis(400), neighbor);
                rot.setFromAngle(1.5);
                rot.setToAngle(0);
                rot.setDelay(Duration.millis(i * 30));
                new ParallelTransition(slide, rot).play();
            }

            // Phase 2 : collapse hauteur de la card (500ms lent)
            double startH = card.getHeight();
            card.setMinHeight(0);
            Timeline collapse = new Timeline(
                    new KeyFrame(Duration.ZERO,
                            new KeyValue(card.prefHeightProperty(), startH),
                            new KeyValue(card.minHeightProperty(), startH),
                            new KeyValue(card.spacingProperty(), 12.0)),
                    new KeyFrame(Duration.millis(500),
                            new KeyValue(card.prefHeightProperty(), 0, Interpolator.SPLINE(0.4, 0, 0.6, 1)),
                            new KeyValue(card.minHeightProperty(), 0, Interpolator.SPLINE(0.4, 0, 0.6, 1)),
                            new KeyValue(card.spacingProperty(), 0.0)));
            collapse.currentTimeProperty().addListener((obs, o, n) -> {
                double ratio = Math.max(0, 1.0 - n.toMillis() / 500.0);
                card.setPadding(new Insets(18 * ratio, 18 * ratio, 18 * ratio, 18 * ratio));
            });
            collapse.setOnFinished(ev -> parent.getChildren().remove(card));
            collapse.play();
        });
        phase1.play();
    }

    private void showSimilarProductsPopup(ConseilsMarketing c) {
        Stage popup = new Stage();
        popup.initStyle(StageStyle.TRANSPARENT);
        VBox root = new VBox(15);
        root.setPadding(new Insets(25));

        // Couleurs adaptatives - Thème cyan pastel en mode clair
        String popupBg = darkMode ? "#0f172a" : "#ecfeff"; // Cyan très clair
        String popupBorder = darkMode ? PASTEL_SKY : "#06b6d4"; // Cyan pastel
        String titleColor = darkMode ? PASTEL_SKY : "#0e7490"; // Cyan foncé
        String cardBg = darkMode ? "#1e293b" : "#ffffff"; // Blanc pour les cards
        String textColor = darkMode ? "white" : "#0f172a"; // Noir doux
        String badgeBg = darkMode ? "rgba(139,92,246,0.15)" : "rgba(6,182,212,0.15)";
        String badgeText = darkMode ? VIOLET : "#0891b2";
        String btnBg = darkMode ? "rgba(139,92,246,0.15)" : "rgba(6,182,212,0.12)";
        String btnText = darkMode ? VIOLET : "#0891b2";

        root.setStyle(
                "-fx-background-color: " + popupBg + ";" +
                        "-fx-border-color: " + popupBorder + ";" +
                        "-fx-border-width: 2;" +
                        "-fx-background-radius: 15;" +
                        "-fx-border-radius: 15;" +
                        "-fx-effect: dropshadow(gaussian," + (darkMode ? "rgba(0,0,0,0.6)" : "rgba(0,0,0,0.15)")
                        + ",20,0,0,6);");

        Label title = new Label("\uD83D\uDD17 Produits similaires concernes");
        title.setStyle("-fx-text-fill: " + titleColor + "; -fx-font-size: 16px; -fx-font-weight: bold;");

        VBox list = new VBox(8);
        String[] similars = { "REF-CABLE-18", "REF-HUB-09", "REF-ADAPT-04" };
        for (String s : similars) {
            HBox row = new HBox(10);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setStyle("-fx-background-color: " + cardBg + "; -fx-padding: 8; -fx-background-radius: 8;");
            Label name = new Label(s);
            name.setStyle("-fx-text-fill: " + textColor + "; -fx-font-weight: bold;");
            Region sp = new Region();
            HBox.setHgrow(sp, Priority.ALWAYS);
            Label badge = new Label("meme probleme");
            badge.setStyle("-fx-background-color: " + badgeBg + "; -fx-text-fill: " + badgeText
                    + "; -fx-font-size: 10px; -fx-padding: 2 8; -fx-background-radius: 4;");
            row.getChildren().addAll(name, sp, badge);
            list.getChildren().add(row);
        }

        Button btnAll = new Button("Appliquer a tous les " + (similars.length + 1) + " produits ->");
        btnAll.setMaxWidth(Double.MAX_VALUE);
        btnAll.setStyle("-fx-background-color: " + btnBg + "; -fx-text-fill: " + btnText
                + "; -fx-font-weight: bold; -fx-padding: 10; -fx-background-radius: 8; -fx-cursor: hand;");
        btnAll.setOnAction(e -> popup.close());

        root.getChildren().addAll(title, list, btnAll);

        Scene scene = new Scene(root);
        scene.setFill(Color.TRANSPARENT);
        popup.setScene(scene);
        popup.show();
    }

    private void showHistoriquePopup() {
        Stage popup = new Stage();
        popup.initStyle(StageStyle.TRANSPARENT);
        VBox root = new VBox(15);
        root.setPadding(new Insets(25));
        root.setMinWidth(450);

        // Couleurs adaptatives pour le popup historique (violet pastel en mode clair)
        String histBg = darkMode ? "#0f172a" : "#faf5ff"; // Violet très clair
        String histBorder = darkMode ? VIOLET : "#a78bfa"; // Violet pastel
        String histTitleColor = darkMode ? "white" : "#6b21a8"; // Violet foncé
        String histCloseBg = darkMode ? "rgba(255,255,255,0.05)" : "#e9d5ff"; // Violet clair
        String histCloseText = darkMode ? "white" : "#7c3aed"; // Violet moyen
        String histCloseHoverBg = darkMode ? "rgba(255,255,255,0.1)" : "#c4b5fd"; // Violet plus saturé

        root.setStyle(
                "-fx-background-color: " + histBg + ";" +
                        "-fx-border-color: " + histBorder + ";" +
                        "-fx-border-width: 2;" +
                        "-fx-background-radius: 15;" +
                        "-fx-border-radius: 15;" +
                        "-fx-effect: dropshadow(gaussian," + (darkMode ? "rgba(0,0,0,0.6)" : "rgba(0,0,0,0.15)")
                        + ",20,0,0,6);");

        Label title = new Label("\uD83D\uDCDC Historique des Conseils");
        title.setStyle("-fx-text-fill: " + histTitleColor + "; -fx-font-size: 18px; -fx-font-weight: bold;");

        VBox list = new VBox(10);
        // Adapter les couleurs des icônes pour le mode clair
        String greenColor = darkMode ? NEON_GREEN : "#10b981";
        String roseColor = darkMode ? PASTEL_ROSE : "#ec4899";
        String goldColor = darkMode ? GOLD : "#f59e0b";

        list.getChildren().addAll(
                buildHistRow("\u2705 Promo Ramadan", "Applique -> ROI 340%", greenColor),
                buildHistRow("\uD83D\uDEAB Bundle Ete", "Ignore -> manque a gagner 800 TND", roseColor),
                buildHistRow("\u23F3 Promo CHARGEUR", "En attente - expire dans 14j", goldColor),
                buildHistRow("\u2705 Restock LAMPE", "Applique -> rupture evitee", greenColor));

        Button btnClose = new Button("Fermer");
        btnClose.setMaxWidth(Double.MAX_VALUE);
        btnClose.setStyle(
                "-fx-background-color: " + histCloseBg + ";" +
                        "-fx-text-fill: " + histCloseText + ";" +
                        "-fx-padding: 10;" +
                        "-fx-background-radius: 8;" +
                        "-fx-border-color: " + histBorder + ";" +
                        "-fx-border-width: 1;" +
                        "-fx-border-radius: 8;" +
                        "-fx-cursor: hand;" +
                        "-fx-font-weight: bold;");
        btnClose.setOnMouseEntered(e -> btnClose.setStyle(
                "-fx-background-color: " + histCloseHoverBg + ";" +
                        "-fx-text-fill: " + histCloseText + ";" +
                        "-fx-padding: 10;" +
                        "-fx-background-radius: 8;" +
                        "-fx-border-color: " + histBorder + ";" +
                        "-fx-border-width: 1;" +
                        "-fx-border-radius: 8;" +
                        "-fx-cursor: hand;" +
                        "-fx-font-weight: bold;"));
        btnClose.setOnMouseExited(e -> btnClose.setStyle(
                "-fx-background-color: " + histCloseBg + ";" +
                        "-fx-text-fill: " + histCloseText + ";" +
                        "-fx-padding: 10;" +
                        "-fx-background-radius: 8;" +
                        "-fx-border-color: " + histBorder + ";" +
                        "-fx-border-width: 1;" +
                        "-fx-border-radius: 8;" +
                        "-fx-cursor: hand;" +
                        "-fx-font-weight: bold;"));
        btnClose.setOnAction(e -> popup.close());

        root.getChildren().addAll(title, list, btnClose);
        Scene scene = new Scene(root);
        scene.setFill(Color.TRANSPARENT);
        popup.setScene(scene);
        popup.show();
    }

    private HBox buildHistRow(String title, String status, String color) {
        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(8, 0, 8, 0));

        // Couleurs adaptatives pour les lignes d'historique
        String rowBorder = darkMode ? "rgba(255,255,255,0.05)" : "rgba(0,0,0,0.08)";
        String rowTitleColor = darkMode ? "white" : "#1e293b";

        row.setStyle("-fx-border-color: " + rowBorder + "; -fx-border-width: 0 0 1 0;");
        Label tL = new Label(title);
        tL.setStyle("-fx-text-fill: " + rowTitleColor + "; -fx-font-weight: bold;");
        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);
        Label sL = new Label(status);
        sL.setStyle("-fx-text-fill: " + color + "; -fx-font-size: 12px; -fx-font-weight: 600;");
        row.getChildren().addAll(tL, sp, sL);
        return row;
    }

    private VBox buildCampCard(CampagneMarketing c, String accentColor, int pct) {
        // ── Calculs financiers ──
        double revenuAttribue = campagnesService.getRevenuAttribue(c.getDateDebut(), c.getDateFin());
        boolean isBrouillon = "BROUILLON".equals(c.getStatut());
        boolean hasRevenu = revenuAttribue > 0 && !isBrouillon;
        double roi = (c.getDepense() > 0 && hasRevenu)
                ? ((revenuAttribue - c.getDepense()) / c.getDepense()) * 100
                : 0;

        // ── Carte principale ──
        VBox card = new VBox(8);
        card.setPadding(new Insets(16, 20, 14, 20));
        card.setStyle(
                "-fx-background-color: " + BG_CARD + ";" +
                        "-fx-background-radius: 14; -fx-border-radius: 14;" +
                        "-fx-border-color: rgba(255,255,255,0.07), " + accentColor + ";" +
                        "-fx-border-width: 1, 0 0 0 5;");

        // ── LIGNE 1 : Nom + Score IA + Statut ──
        HBox top = new HBox(8);
        top.setAlignment(Pos.CENTER_LEFT);
        Label nameL = new Label(c.getNomCampagne());
        nameL.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: " + TEXT_1 + ";");
        nameL.setWrapText(true);
        nameL.setMinWidth(0);
        HBox.setHgrow(nameL, Priority.ALWAYS);
        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);

        if (c.getIaScore() > 0) {
            Label scoreB = new Label("\u2605 " + c.getIaScore() + "/10");
            scoreB.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: " + GOLD + ";" +
                    "-fx-background-color: rgba(251,191,36,0.1); -fx-padding: 3 8 3 8;" +
                    "-fx-border-color: rgba(251,191,36,0.2); -fx-border-width:1;" +
                    "-fx-background-radius: 8; -fx-border-radius: 8;");

            // Tooltip explicatif pour le score IA
            Tooltip scoreTooltip = new Tooltip(
                    "Score IA basé sur :\n" +
                            "• ROI et performance\n" +
                            "• Taux d'engagement\n" +
                            "• Conversion client\n" +
                            "• Efficacité du canal\n" +
                            "• Budget vs résultats");
            scoreTooltip.setStyle(
                    "-fx-background-color:" + (darkMode ? "#0d1117" : "#ffffff") + ";" +
                            "-fx-text-fill:" + TEXT_1 + ";" +
                            "-fx-font-size:12px;" +
                            "-fx-padding:8 12;" +
                            "-fx-background-radius:8;" +
                            "-fx-border-color:" + GOLD + ";" +
                            "-fx-border-width:1;" +
                            "-fx-border-radius:8;");
            scoreTooltip.setShowDelay(Duration.millis(200));
            Tooltip.install(scoreB, scoreTooltip);

            top.getChildren().addAll(nameL, sp, scoreB);
        } else {
            Label waitB = new Label("\uD83E\uDD16 IA");
            waitB.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: " + VIOLET + ";" +
                    "-fx-background-color: rgba(139,92,246,0.12); -fx-padding: 3 8 3 8;" +
                    "-fx-border-color: rgba(139,92,246,0.2); -fx-border-width:1;" +
                    "-fx-background-radius: 8; -fx-border-radius: 8;");

            // Tooltip pour IA en attente
            Tooltip waitTooltip = new Tooltip("Score IA en cours de calcul...");
            waitTooltip.setStyle(
                    "-fx-background-color:" + (darkMode ? "#0d1117" : "#ffffff") + ";" +
                            "-fx-text-fill:" + TEXT_1 + ";" +
                            "-fx-font-size:12px;");
            Tooltip.install(waitB, waitTooltip);

            top.getChildren().addAll(nameL, sp, waitB);
        }

        String statColor = c.getStatut().equals("ACTIVE") ? CAMP_ACTIVE
                : c.getStatut().equals("TERMINEE") ? PASTEL_ROSE : VIOLET;
        Label statBadge = new Label(c.getStatut());
        statBadge.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: " + statColor + ";" +
                "-fx-background-color: rgba(" + hexToRgb(statColor) + ",0.12);" +
                "-fx-padding: 3 10 3 10; -fx-background-radius: 99;" +
                "-fx-border-color: rgba(" + hexToRgb(statColor) + ",0.25);" +
                "-fx-border-width:1; -fx-border-radius: 99;");
        top.getChildren().add(statBadge);

        // ── LIGNE 2 : Meta (3 infos séparées) ──
        HBox metaRow = new HBox(16);
        metaRow.setAlignment(Pos.CENTER_LEFT);
        Label metaObj = new Label("\uD83C\uDFAF " + c.getObjectif());
        Label metaCan = new Label("\uD83D\uDCE1 " + c.getCanal());
        String dateStr = (c.getDateDebut() != null ? c.getDateDebut().toString().substring(0, 7) : "-")
                + " \u2192 "
                + (c.getDateFin() != null ? c.getDateFin().toString().substring(0, 7) : "-");
        Label metaDate = new Label("\uD83D\uDCC5 " + dateStr);
        for (Label ml : new Label[] { metaObj, metaCan, metaDate }) {
            ml.setStyle("-fx-font-size: 11px; -fx-text-fill: " + TEXT_3 + ";");
        }
        // Séparateurs visuels
        Label sep1 = new Label("|");
        sep1.setStyle("-fx-text-fill:#334155;-fx-font-size:11px;");
        Label sep2 = new Label("|");
        sep2.setStyle("-fx-text-fill:#334155;-fx-font-size:11px;");
        metaRow.getChildren().addAll(metaObj, sep1, metaCan, sep2, metaDate);

        // ── LIGNE 3 : IA Insight (dynamique) ──
        String iaText;
        if (c.getIaConseil() != null && !c.getIaConseil().isEmpty()
                && !c.getIaConseil().equals("Analyse IA en cours...")) {
            iaText = c.getIaConseil();
        } else if (isBrouillon) {
            iaText = "\uD83D\uDCDD Brouillon \u2014 campagne non encore lancee";
        } else if (c.getIaScore() >= 7) {
            iaText = "\uD83D\uDCC8 ROI " + String.format("%+.0f%%", roi)
                    + " \u00B7 Canal " + c.getCanal() + " performant \u00B7 Score " + c.getIaScore() + "/10";
        } else if (c.getIaScore() > 0) {
            iaText = "\u26A0 Score " + c.getIaScore() + "/10 \u2014 optimiser le canal ou le budget";
        } else if (c.getDepense() > 0 && c.getBudget() > 0 && pct >= 80) {
            iaText = "\uD83D\uDD14 Budget consomme a " + pct + "% \u2014 surveiller les depenses";
        } else {
            iaText = "\uD83D\uDCCA Canal : " + c.getCanal()
                    + " \u00B7 Objectif : " + c.getObjectif()
                    + " \u00B7 Budget : " + String.format("%.0f TND", c.getBudget());
        }
        HBox iaStrip = new HBox(6);
        iaStrip.setPadding(new Insets(6, 10, 6, 10));
        iaStrip.setStyle("-fx-background-color: rgba(139,92,246,0.07);" +
                "-fx-border-color: rgba(139,92,246,0.13); -fx-border-width:1;" +
                "-fx-background-radius: 8; -fx-border-radius: 8;");
        Label iaL = new Label("\uD83E\uDD16  " + iaText);
        iaL.setStyle("-fx-font-size: 11px; -fx-text-fill: " + TEXT_2 + ";");
        iaL.setWrapText(true);
        iaStrip.getChildren().add(iaL);

        // ── LIGNE 4 : 2 blocs financiers côte à côte ──
        HBox financialRow = new HBox(10);
        financialRow.setAlignment(Pos.CENTER_LEFT);

        // Bloc Budget consommé (tons indigo / slate)
        VBox budgetBloc = new VBox(3);
        budgetBloc.setPadding(new Insets(8, 12, 8, 12));
        String spendBg = darkMode ? "rgba(129,140,248,0.10)" : "rgba(99,102,241,0.07)";
        String spendBr = darkMode ? "rgba(129,140,248,0.22)" : "rgba(99,102,241,0.16)";
        budgetBloc.setStyle("-fx-background-color: " + spendBg + ";" +
                "-fx-border-color: " + spendBr + "; -fx-border-width:1;" +
                "-fx-background-radius: 8; -fx-border-radius: 8;");
        Label budTitle = new Label("\uD83D\uDCCA Budget consomme");
        budTitle.setStyle("-fx-font-size: 10px; -fx-text-fill:#94a3b8;");
        Label budTND = new Label(String.format("%.0f / %.0f TND", c.getDepense(), c.getBudget()));
        budTND.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: " + CAMP_SPEND_2 + ";");
        Label budDevises = new Label(
                edu.hanouti.services.ExchangeRateService.format(c.getDepense(), "EUR")
                        + "  \u2022  "
                        + edu.hanouti.services.ExchangeRateService.format(c.getDepense(), "USD"));
        budDevises.setStyle("-fx-font-size: 10px; -fx-text-fill:#64748b;");
        budgetBloc.getChildren().addAll(budTitle, budTND, budDevises);
        HBox.setHgrow(budgetBloc, Priority.ALWAYS);

        // Bloc Revenu généré (cyan / sky) — masqué si BROUILLON
        VBox revenuBloc = new VBox(3);
        revenuBloc.setPadding(new Insets(8, 12, 8, 12));
        String revBg = darkMode ? "rgba(14,165,233,0.10)" : "rgba(14,165,233,0.07)";
        String revBr = darkMode ? "rgba(56,189,248,0.22)" : "rgba(14,165,233,0.18)";
        revenuBloc.setStyle("-fx-background-color: " + revBg + ";" +
                "-fx-border-color: " + revBr + "; -fx-border-width:1;" +
                "-fx-background-radius: 8; -fx-border-radius: 8;");
        Label revTitle = new Label("\uD83D\uDCC8 Revenu genere");
        revTitle.setStyle("-fx-font-size: 10px; -fx-text-fill:#94a3b8;");
        Label revTND = new Label(hasRevenu ? String.format("%.0f TND", revenuAttribue) : "N/A");
        revTND.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: " + CAMP_REV_2 + ";");
        Label revDevises = new Label(hasRevenu
                ? edu.hanouti.services.ExchangeRateService.format(revenuAttribue, "EUR")
                        + "  \u2022  "
                        + edu.hanouti.services.ExchangeRateService.format(revenuAttribue, "USD")
                : isBrouillon ? "Campagne non lancee" : "Aucune vente sur cette periode");
        revDevises.setStyle("-fx-font-size: 10px; -fx-text-fill:#64748b;");
        revenuBloc.getChildren().addAll(revTitle, revTND, revDevises);
        HBox.setHgrow(revenuBloc, Priority.ALWAYS);

        financialRow.getChildren().addAll(budgetBloc, revenuBloc);

        // ── LIGNE 5 : 2 barres de progression ──
        VBox barsWrap = new VBox(5);

        // Barre budget (indigo)
        HBox budBarRow = new HBox(8);
        budBarRow.setAlignment(Pos.CENTER_LEFT);
        Label budBarLbl = new Label("Budget  " + pct + "%");
        budBarLbl.setStyle("-fx-font-size: 10px; -fx-text-fill:#94a3b8; -fx-min-width:80;");
        StackPane budTrack = new StackPane();
        budTrack.setPrefHeight(6);
        budTrack.setStyle("-fx-background-color: rgba(255,255,255,0.06); -fx-background-radius: 99;");
        HBox budFill = new HBox();
        budFill.setStyle("-fx-background-color: linear-gradient(to right," + CAMP_SPEND_1 + "," + CAMP_SPEND_2 + ");" +
                "-fx-background-radius: 99;");
        budFill.setPrefHeight(6);
        budFill.setPrefWidth(0);
        StackPane.setAlignment(budFill, Pos.CENTER_LEFT);
        budTrack.getChildren().add(budFill);
        HBox.setHgrow(budTrack, Priority.ALWAYS);
        budBarRow.getChildren().addAll(budBarLbl, budTrack);

        // Barre revenu (cyan / sky)
        int revPct = (c.getBudget() > 0 && hasRevenu)
                ? (int) Math.min((revenuAttribue / c.getBudget()) * 100, 200)
                : 0;
        HBox revBarRow = new HBox(8);
        revBarRow.setAlignment(Pos.CENTER_LEFT);
        Label revBarLbl = new Label("Revenu  " + revPct + "%");
        revBarLbl.setStyle("-fx-font-size: 10px; -fx-text-fill:#94a3b8; -fx-min-width:80;");
        StackPane revTrack = new StackPane();
        revTrack.setPrefHeight(6);
        revTrack.setStyle("-fx-background-color: rgba(255,255,255,0.06); -fx-background-radius: 99;");
        HBox revFill = new HBox();
        revFill.setStyle("-fx-background-color: linear-gradient(to right," + CAMP_REV_1 + "," + CAMP_REV_2 + ");" +
                "-fx-background-radius: 99;");
        revFill.setPrefHeight(6);
        revFill.setPrefWidth(0);
        StackPane.setAlignment(revFill, Pos.CENTER_LEFT);
        revTrack.getChildren().add(revFill);
        HBox.setHgrow(revTrack, Priority.ALWAYS);
        revBarRow.getChildren().addAll(revBarLbl, revTrack);

        barsWrap.getChildren().addAll(budBarRow, revBarRow);

        // Animation barres
        Platform.runLater(() -> {
            double maxW = budTrack.getWidth() > 0 ? budTrack.getWidth() : 260;
            Timeline tl = new Timeline(
                    new KeyFrame(Duration.millis(900),
                            new KeyValue(budFill.prefWidthProperty(), maxW * Math.min(pct / 100.0, 1.0)),
                            new KeyValue(revFill.prefWidthProperty(), maxW * Math.min(revPct / 100.0, 1.0))));
            tl.play();
        });

        // ── LIGNE 6 : ROI badge + Actions ──
        HBox bottomRow = new HBox(10);
        bottomRow.setAlignment(Pos.CENTER_LEFT);

        // ROI badge (masqué si BROUILLON ou revenu=0)
        if (hasRevenu) {
            String roiColor = roi >= 0 ? CAMP_REV_2 : PASTEL_ROSE;
            Label roiBadge = new Label(String.format("ROI %+.0f%%", roi));
            roiBadge.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: " + roiColor + ";" +
                    "-fx-background-color: rgba(" + hexToRgb(roiColor) + ",0.12);" +
                    "-fx-padding: 4 12 4 12; -fx-background-radius: 99;" +
                    "-fx-border-color: rgba(" + hexToRgb(roiColor) + ",0.25);" +
                    "-fx-border-width:1; -fx-border-radius: 99;");
            bottomRow.getChildren().add(roiBadge);
        }

        // Indicateur taux avec tooltip
        Label freshLbl = new Label(edu.hanouti.services.ExchangeRateService.isFresh() ? "\u26A1" : "\u26A1");
        freshLbl.setStyle("-fx-font-size: 11px; -fx-text-fill:#334155; -fx-cursor:hand;");
        javafx.scene.control.Tooltip freshTip = new javafx.scene.control.Tooltip(
                edu.hanouti.services.ExchangeRateService.isFresh()
                        ? "Taux mis a jour a " + edu.hanouti.services.ExchangeRateService.getLastUpdateTime()
                        : "Taux de reference fixes (EUR=0.30, USD=0.32)");
        freshTip.setStyle("-fx-font-size:11px;");
        javafx.scene.control.Tooltip.install(freshLbl, freshTip);
        bottomRow.getChildren().add(freshLbl);

        Region actSp = new Region();
        HBox.setHgrow(actSp, Priority.ALWAYS);
        bottomRow.getChildren().add(actSp);

        // Bouton Modifier (neutre)
        Button edit = makeActionBtn("\u270F Modifier", TEXT_2, "rgba(255,255,255,0.06)", "rgba(255,255,255,0.07)");
        edit.setOnAction(e -> openModifierCampagneDialog(c));

        // Bouton Supprimer (discret → rouge au survol)
        Button del = new Button("\uD83D\uDDD1 Supprimer");
        del.setStyle("-fx-background-color: transparent; -fx-text-fill: #475569;" +
                "-fx-font-size: 12px; -fx-cursor: hand; -fx-background-radius: 8;" +
                "-fx-padding: 6 14 6 14; -fx-border-color: rgba(255,255,255,0.06);" +
                "-fx-border-width:1; -fx-border-radius:8;");
        del.setOnMouseEntered(ev -> del.setStyle(
                "-fx-background-color: rgba(244,114,182,0.1); -fx-text-fill: " + PASTEL_ROSE + ";" +
                        "-fx-font-size: 12px; -fx-cursor: hand; -fx-background-radius: 8;" +
                        "-fx-padding: 6 14 6 14; -fx-border-color: rgba(244,114,182,0.3);" +
                        "-fx-border-width:1; -fx-border-radius:8;"));
        del.setOnMouseExited(ev -> del.setStyle(
                "-fx-background-color: transparent; -fx-text-fill: #475569;" +
                        "-fx-font-size: 12px; -fx-cursor: hand; -fx-background-radius: 8;" +
                        "-fx-padding: 6 14 6 14; -fx-border-color: rgba(255,255,255,0.06);" +
                        "-fx-border-width:1; -fx-border-radius:8;"));

        del.setOnAction(e -> {
            Stage confirm = new Stage();
            confirm.initOwner(primaryStage);
            confirm.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            confirm.initStyle(javafx.stage.StageStyle.UNDECORATED);

            // Couleurs adaptatives pour le dialogue de suppression
            String dialogBg = darkMode ? "#0d1020" : "#fef2f2";
            String dialogBorder = darkMode ? "#f472b6" : "#fca5a5";
            String iconBoxBg = darkMode ? "rgba(220,38,38,0.18)" : "rgba(239,68,68,0.12)";
            String titleColor = darkMode ? "#f1f5f9" : "#7f1d1d";
            String subColor = darkMode ? "#6b7280" : "#991b1b";
            String trackBg = darkMode ? "rgba(220,38,38,0.18)" : "rgba(254,202,202,0.5)";
            String trackLblColor = darkMode ? "#ef4444" : "#dc2626";
            String thumbBg = darkMode ? "#ef4444" : "#ef4444";
            String cancelBg = darkMode ? "#374151" : "#e5e7eb";
            String cancelText = darkMode ? "#9ca3af" : "#6b7280";

            javafx.scene.layout.VBox cRoot = new javafx.scene.layout.VBox(18);
            cRoot.setStyle(
                    "-fx-background-color: " + dialogBg + ";" +
                            "-fx-border-color: " + dialogBorder + ";" +
                            "-fx-border-width: 2;" +
                            "-fx-border-radius: 18;" +
                            "-fx-background-radius: 18;" +
                            "-fx-padding: 28 32 24 32;");
            cRoot.setPrefWidth(380);
            cRoot.setAlignment(javafx.geometry.Pos.CENTER);

            // Icone poubelle stylisee
            javafx.scene.layout.StackPane iconBox = new javafx.scene.layout.StackPane();
            iconBox.setStyle("-fx-background-color: " + iconBoxBg + "; -fx-background-radius: 16;");
            iconBox.setPrefSize(64, 64);
            javafx.scene.control.Label iconLbl = new javafx.scene.control.Label("\uD83D\uDDD1");
            iconLbl.setStyle("-fx-font-size: 28px;");
            iconBox.getChildren().add(iconLbl);

            javafx.scene.control.Label msg = new javafx.scene.control.Label(
                    "Supprimer \"" + c.getNomCampagne() + "\" ?");
            msg.setStyle("-fx-text-fill: " + titleColor
                    + "; -fx-font-size: 16px; -fx-font-weight: bold; -fx-text-alignment: center;");
            msg.setWrapText(true);
            msg.setMaxWidth(320);
            msg.setAlignment(javafx.geometry.Pos.CENTER);

            javafx.scene.control.Label sub = new javafx.scene.control.Label("Glissez vers la droite pour confirmer");
            sub.setStyle("-fx-text-fill: " + subColor + "; -fx-font-size: 12px;");

            // -- Slider de confirmation --
            double TRACK_W = 300;
            double THUMB_SIZE = 44;

            javafx.scene.layout.StackPane track = new javafx.scene.layout.StackPane();
            track.setPrefSize(TRACK_W, THUMB_SIZE);
            track.setMaxWidth(TRACK_W);
            track.setStyle("-fx-background-color: " + trackBg + "; -fx-background-radius: 99;");

            javafx.scene.control.Label trackLbl = new javafx.scene.control.Label("GLISSER ->");
            trackLbl.setStyle("-fx-text-fill: " + trackLblColor + "; -fx-font-size: 13px; -fx-font-weight: bold;");
            javafx.scene.layout.StackPane.setAlignment(trackLbl, javafx.geometry.Pos.CENTER_RIGHT);
            javafx.scene.layout.StackPane.setMargin(trackLbl, new Insets(0, 18, 0, 0));

            javafx.scene.layout.StackPane thumb = new javafx.scene.layout.StackPane();
            thumb.setPrefSize(THUMB_SIZE, THUMB_SIZE);
            thumb.setStyle("-fx-background-color: " + thumbBg + "; -fx-background-radius: 99;");
            javafx.scene.control.Label arrowLbl = new javafx.scene.control.Label("->");
            arrowLbl.setStyle("-fx-text-fill: white; -fx-font-size: 18px; -fx-font-weight: bold;");
            thumb.getChildren().add(arrowLbl);
            javafx.scene.layout.StackPane.setAlignment(thumb, javafx.geometry.Pos.CENTER_LEFT);

            track.getChildren().addAll(trackLbl, thumb);

            // Drag logic
            final double[] dragStart = { 0 };
            final double[] thumbX = { 0 };
            double maxSlide = TRACK_W - THUMB_SIZE;

            thumb.setOnMousePressed(ev -> dragStart[0] = ev.getSceneX() - thumbX[0]);
            thumb.setOnMouseDragged(ev -> {
                double newX = ev.getSceneX() - dragStart[0];
                newX = Math.max(0, Math.min(newX, maxSlide));
                thumbX[0] = newX;
                thumb.setTranslateX(newX);
                double ratio = newX / maxSlide;
                String dynamicTrackBg = darkMode
                        ? "rgba(220,38,38," + (0.18 + ratio * 0.5) + ")"
                        : "rgba(254,202,202," + (0.5 + ratio * 0.4) + ")";
                track.setStyle("-fx-background-color: " + dynamicTrackBg + "; -fx-background-radius: 99;");
            });
            thumb.setOnMouseReleased(ev -> {
                if (thumbX[0] >= maxSlide * 0.85) {
                    campagnesService.deleteEntity(c);
                    confirm.close();
                    animateExit(card);
                } else {
                    // Reset
                    javafx.animation.TranslateTransition reset = new javafx.animation.TranslateTransition(
                            Duration.millis(200), thumb);
                    reset.setToX(0);
                    reset.play();
                    thumbX[0] = 0;
                    track.setStyle("-fx-background-color: " + trackBg + "; -fx-background-radius: 99;");
                }
            });

            // Bouton Annuler
            javafx.scene.control.Button btnNon = new javafx.scene.control.Button("Annuler");
            btnNon.setPrefWidth(160);
            btnNon.setPrefHeight(40);
            btnNon.setStyle("-fx-background-color:" + cancelBg + ";-fx-text-fill:" + cancelText + ";" +
                    "-fx-font-size:14px;-fx-background-radius:10;-fx-cursor:hand;");
            btnNon.setOnAction(ev -> confirm.close());

            cRoot.getChildren().addAll(iconBox, msg, sub, track, btnNon);

            javafx.scene.Scene cs = new javafx.scene.Scene(cRoot);
            cs.setFill(javafx.scene.paint.Color.TRANSPARENT);
            confirm.setScene(cs);
            confirm.showAndWait();
        });

        bottomRow.getChildren().addAll(edit, del);
        card.getChildren().addAll(top, metaRow, iaStrip, financialRow, barsWrap, bottomRow);

        // ANIMATION: Slide in from right
        card.setTranslateX(50);
        card.setOpacity(0);
        TranslateTransition tt = new TranslateTransition(Duration.millis(400), card);
        tt.setToX(0);
        FadeTransition ft = new FadeTransition(Duration.millis(400), card);
        ft.setToValue(1);
        new ParallelTransition(tt, ft).play();

        return card;
    }

    private VBox buildIaAutoCard(String title, String msg, String color) {
        VBox card = new VBox(8);
        card.setPadding(new Insets(14));
        HBox.setHgrow(card, Priority.ALWAYS);
        card.setStyle(
                "-fx-background-color: rgba(" + hexToRgb(color) + ",0.07);" +
                        "-fx-border-color: rgba(" + hexToRgb(color) + ",0.2); -fx-border-width:1;" +
                        "-fx-background-radius: 12; -fx-border-radius: 12;");
        Label t = new Label(title);
        t.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: " + color + ";");
        Label m = new Label(msg);
        m.setStyle("-fx-font-size: 12px; -fx-text-fill: " + TEXT_2 + ";");
        m.setWrapText(true);
        card.getChildren().addAll(t, m);

        card.setOnMouseEntered(e -> {
            card.setStyle(card.getStyle().replace("0.07", "0.15").replace("0.2", "0.5"));
            card.setTranslateY(-3);
        });
        card.setOnMouseExited(e -> {
            card.setStyle(card.getStyle().replace("0.15", "0.07").replace("0.5", "0.2"));
            card.setTranslateY(0);
        });

        return card;
    }

    private VBox makeChartCard(String title) {
        VBox card = new VBox(12);
        card.setPadding(new Insets(20));
        card.setStyle("-fx-background-color: " + BG_CARD + ";" +
                "-fx-background-radius: 14;" +
                "-fx-border-color: " + (darkMode ? "rgba(255,255,255,0.07)" : "rgba(0,0,0,0.07)") + ";" +
                "-fx-border-width: 1; -fx-border-radius: 14;" +
                (darkMode ? "" : "-fx-effect: dropshadow(gaussian,rgba(0,0,0,0.06),10,0,0,2);"));
        Label t = new Label(title);
        t.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: " + TEXT_1 + ";");
        card.getChildren().add(t);
        HBox.setHgrow(card, Priority.ALWAYS);
        return card;
    }

    private Label makeSectionTitle(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-letter-spacing: 0.12em;" +
                "-fx-text-fill: " + ROYAL + "; -fx-text-transform: uppercase;");
        return l;
    }

    private Label makeTag(String text, String color) {
        Label l = new Label(text);
        l.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: " + color + ";" +
                "-fx-background-color: rgba(" + hexToRgb(color) + ",0.1);" +
                "-fx-padding: 2 8 2 8; -fx-background-radius: 99;");
        return l;
    }

    private Button makeActionBtn(String text, String textColor, String bg, String border) {
        Button b = new Button(text);
        b.setStyle("-fx-background-color: " + bg + "; -fx-text-fill: " + textColor + ";" +
                "-fx-border-color: " + border + "; -fx-border-width:1;" +
                "-fx-border-radius: 9; -fx-background-radius: 9;" +
                "-fx-font-size: 12px; -fx-padding: 8 10 8 10; -fx-cursor: hand;");
        return b;
    }

    private String formatKpiValue(double val, boolean isCurrency) {
        String s = String.format("%,d", (long) val).replace(",", " ").replace("\u00A0", " ").replace(" ", "\n");
        if (isCurrency) {
            s += "\nTND";
        }
        return s;
    }

    private ScrollPane styledScroll(VBox content) {
        ScrollPane sp = new ScrollPane(content);
        sp.setFitToWidth(true);
        sp.setStyle("-fx-background-color: " + BG_DEEP + "; -fx-background: " + BG_DEEP + ";" +
                "-fx-border-color: transparent;");
        sp.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        return sp;
    }

    private Button makePill(String text, boolean active) {
        Button b = new Button(text);

        // Couleurs adaptatives selon le thème
        String activeBg = darkMode ? "rgba(37,99,235,0.18)" : "#dbeafe";
        String activeText = darkMode ? PASTEL_SKY : "#0284c7";
        String activeBorder = darkMode ? "rgba(37,99,235,0.4)" : "#60a5fa";
        String inactiveBg = darkMode ? "transparent" : "#f8fafc";
        String inactiveText = darkMode ? TEXT_3 : "#94a3b8";
        String inactiveBorder = darkMode ? "rgba(255,255,255,0.07)" : "#e2e8f0";

        b.setStyle(active
                ? "-fx-background-color:" + activeBg + ";-fx-text-fill:" + activeText + ";" +
                        "-fx-border-color:" + activeBorder + ";-fx-border-width:1;" +
                        "-fx-border-radius:99;-fx-background-radius:99;" +
                        "-fx-font-size:12px;-fx-font-weight:500;-fx-padding:6 14 6 14;-fx-cursor:hand;"
                : "-fx-background-color:" + inactiveBg + ";-fx-text-fill:" + inactiveText + ";" +
                        "-fx-border-color:" + inactiveBorder + ";-fx-border-width:1;" +
                        "-fx-border-radius:99;-fx-background-radius:99;" +
                        "-fx-font-size:12px;-fx-font-weight:500;-fx-padding:6 14 6 14;-fx-cursor:hand;");
        return b;
    }

    private void exportToPDF(List<Map<String, Object>> data) {
        FileChooser fc = new FileChooser();
        fc.setTitle("Sauvegarder le Rapport PDF");
        fc.setInitialFileName("Rapport_Hanouti_" + java.time.LocalDate.now() + ".pdf");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));

        File file = fc.showSaveDialog(null);
        if (file != null) {
            try {
                edu.hanouti.services.ReportingService.generatePDF(data, file);

                Alert al = new Alert(Alert.AlertType.INFORMATION);
                al.setTitle("OK Succes");
                al.setHeaderText("Exportation reussie");
                al.setContentText("Le fichier a ete enregistre : " + file.getName());

                ButtonType openBtn = new ButtonType("Ouvrir le fichier");
                al.getButtonTypes().add(openBtn);

                al.showAndWait().ifPresent(response -> {
                    if (response == openBtn) {
                        try {
                            java.awt.Desktop.getDesktop().open(file);
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    }
                });
            } catch (Exception e) {
                Alert err = new Alert(Alert.AlertType.ERROR, "Erreur lors de l'exportation : " + e.getMessage());
                err.show();
            }
        }
    }

    private void showEmailDialog(List<Map<String, Object>> data) {
        TextInputDialog dialog = new TextInputDialog("client@example.com");
        dialog.setTitle("\uD83D\uDCE7 Envoyer par Email");
        dialog.setHeaderText("Partager le rapport de performance");
        dialog.setContentText("Veuillez saisir l'adresse email :");
        dialog.getDialogPane().setStyle("-fx-background-color: " + BG_CARD + ";");
        dialog.getDialogPane().lookup(".label").setStyle("-fx-text-fill: white;");

        dialog.showAndWait().ifPresent(email -> {
            edu.hanouti.services.ReportingService.sendEmail(email, data);
            Alert al = new Alert(Alert.AlertType.INFORMATION, "Email envoye avec succes a " + email);
            al.show();
        });
    }

    private void applyPulse(Circle c) {
        ScaleTransition st = new ScaleTransition(Duration.seconds(1.2), c);
        st.setFromX(1.0);
        st.setFromY(1.0);
        st.setToX(1.4);
        st.setToY(1.4);
        st.setAutoReverse(true);
        st.setCycleCount(Animation.INDEFINITE);
        st.play();
    }

    private void addHealthRow(GridPane grid, int row, String ref, String line, String status, String ret, String stock,
            String urg, int score, String color) {
        Label lRef = new Label(ref);
        lRef.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 13px;");
        Label lLine = new Label(line);
        lLine.setStyle("-fx-text-fill: " + TEXT_2 + "; -fx-font-size: 12px;");
        Label lStatus = new Label(status);
        lStatus.setStyle("-fx-text-fill: " + color + "; -fx-font-weight: bold; -fx-font-size: 12px;");
        Label lRet = new Label(ret);
        lRet.setStyle("-fx-text-fill: " + (ret.contains("\u2713") || ret.contains("OK") ? NEON_GREEN : PASTEL_ROSE)
                + "; -fx-font-size: 12px;");
        Label lStock = new Label(stock);
        lStock.setStyle("-fx-text-fill: " + TEXT_2 + "; -fx-font-size: 12px;");
        Label lUrg = new Label(urg);
        lUrg.setStyle("-fx-text-fill: " + (urg.equals("Bas") ? NEON_GREEN : GOLD) + "; -fx-font-size: 12px;");

        ProgressBar pb = new ProgressBar(score / 100.0);
        pb.setPrefWidth(100);
        pb.setStyle("-fx-accent: " + color + ";");

        Label lScore = new Label(score + "%");
        lScore.setStyle("-fx-text-fill: " + color + "; -fx-font-weight: bold; -fx-font-size: 12px;");

        grid.add(lRef, 0, row);
        grid.add(lLine, 1, row);
        grid.add(lStatus, 2, row);
        grid.add(lRet, 3, row);
        grid.add(lStock, 4, row);
        grid.add(lUrg, 5, row);
        grid.add(pb, 6, row);
        grid.add(lScore, 7, row);
    }

    private Label makeSmallTag(String text, String color) {
        Label l = new Label(text);
        l.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: " + color + ";" +
                "-fx-background-color: rgba(" + hexToRgb(color) + ",0.1);" +
                "-fx-padding: 2 8 2 8; -fx-background-radius: 4;");
        return l;
    }

    private void updateHealthUI(GridPane hGrid) {
        // Logique de mise a jour
    }

    private void animateExit(Node node) {
        FadeTransition ft = new FadeTransition(Duration.millis(300), node);
        ft.setToValue(0);
        ScaleTransition st = new ScaleTransition(Duration.millis(300), node);
        st.setToX(0.8);
        st.setToY(0.8);
        ParallelTransition pt = new ParallelTransition(ft, st);
        pt.setOnFinished(e -> {
            if (node.getParent() instanceof Pane) {
                ((Pane) node.getParent()).getChildren().remove(node);
            }
        });
        pt.play();
    }

    private HBox makeTableBadge(String txt, String color) {
        Label l = new Label(txt);
        l.setStyle("-fx-text-fill: " + color + "; -fx-font-weight: bold; -fx-font-size: 12px;");
        HBox b = new HBox(l);
        b.setAlignment(Pos.CENTER);
        b.setPadding(new Insets(8, 18, 8, 18));
        b.setStyle("-fx-background-color: rgba(255,255,255,0.02); -fx-border-color: rgba(" + hexToRgb(color)
                + ", 0.4); -fx-border-width: 1.5; -fx-border-radius: 12; -fx-background-radius: 12;");
        return b;
    }

    // ── [8.1] Dialogues Email & Utilitaires ──────
    private final edu.hanouti.services.VendeurService vendeurService = new edu.hanouti.services.VendeurService();

    private void openEmailTargetDialog() {
        Stage dialog = new Stage();
        dialog.initOwner(primaryStage);
        dialog.initModality(javafx.stage.Modality.APPLICATION_MODAL);
        dialog.initStyle(javafx.stage.StageStyle.UNDECORATED);

        javafx.scene.layout.VBox root = new javafx.scene.layout.VBox(0);
        String dialogBg = darkMode ? "#0d1020" : "#f1f5f9";
        String dialogBorder = darkMode ? "#38bdf8" : "#cbd5e1";
        String titleColor = darkMode ? "#38bdf8" : "#334155";

        root.setStyle("-fx-background-color:" + dialogBg + ";-fx-border-color:" + dialogBorder + ";" +
                "-fx-border-width:2;-fx-border-radius:12;-fx-background-radius:12;");
        root.setPrefWidth(480);

        javafx.scene.control.Label titleLbl = new javafx.scene.control.Label(
                "\uD83D\uDCE7  Envoyer le rapport au vendeur");
        titleLbl.setStyle("-fx-text-fill:" + titleColor + ";-fx-font-size:16px;-fx-font-weight:bold;" +
                "-fx-padding:22 28 14 28;");
        titleLbl.setMaxWidth(Double.MAX_VALUE);
        titleLbl.setAlignment(javafx.geometry.Pos.CENTER);

        javafx.scene.layout.VBox body = new javafx.scene.layout.VBox(12);
        body.setStyle("-fx-padding:0 28 22 28;");

        String fieldBg = darkMode ? "#111425" : "#ffffff";
        String fieldText = darkMode ? "#f1f5f9" : "#0f172a";
        String fieldBorder = darkMode ? "#38bdf8" : "#cbd5e1";
        String fieldPrompt = darkMode ? "#6b7280" : "#94a3b8";
        String labelColor = darkMode ? "#94a3b8" : "#64748b";

        String fieldStyle = "-fx-background-color:" + fieldBg + ";-fx-text-fill:" + fieldText + ";" +
                "-fx-border-color:" + fieldBorder + ";-fx-border-width:1;-fx-background-radius:8;-fx-border-radius:8;" +
                "-fx-font-size:14px;-fx-padding:8 12;-fx-prompt-text-fill:" + fieldPrompt + ";";
        String lblStyle = "-fx-text-fill:" + labelColor + ";-fx-font-weight:bold;-fx-font-size:13px;";

        javafx.scene.control.Label lNom = new javafx.scene.control.Label("Nom du vendeur :");
        lNom.setStyle(lblStyle);
        javafx.scene.control.TextField tfNom = new javafx.scene.control.TextField();
        tfNom.setPromptText("Tapez le nom...");
        tfNom.setPrefHeight(40);
        tfNom.setStyle(fieldStyle);

        javafx.scene.control.ListView<String> suggestions = new javafx.scene.control.ListView<>();
        String suggestionBg = darkMode ? "#0d1526" : "#ffffff";
        String suggestionBorder = darkMode ? "#38bdf8" : "#e2e8f0";
        String suggestionText = darkMode ? "#f1f5f9" : "#0f172a";
        String suggestionCellBg = darkMode ? "#0d1526" : "#f8fafc";

        suggestions.setStyle("-fx-background-color:" + suggestionBg + ";-fx-border-color:" + suggestionBorder + ";" +
                "-fx-border-width:1;-fx-border-radius:8;-fx-background-radius:8;");
        suggestions.setPrefHeight(110);
        suggestions.setVisible(false);
        suggestions.setManaged(false);
        suggestions.setCellFactory(lv -> new javafx.scene.control.ListCell<String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    setStyle("-fx-text-fill:" + suggestionText + ";-fx-font-size:13px;-fx-background-color:"
                            + suggestionCellBg + ";-fx-padding:8 12;");
                }
            }
        });

        java.util.Map<String, String> nomEmailMap = new java.util.LinkedHashMap<>();
        try {
            List<Map<String, String>> allVendors = vendeurService.getAll();
            for (Map<String, String> v : allVendors) {
                nomEmailMap.put(v.get("nom"), v.get("email"));
            }
        } catch (Exception ignored) {
        }

        javafx.scene.control.Label lEmail = new javafx.scene.control.Label("Adresse email :");
        lEmail.setStyle(lblStyle);
        javafx.scene.control.TextField tfEmail = new javafx.scene.control.TextField();
        tfEmail.setPromptText("Selectionnez un vendeur ci-dessus");
        tfEmail.setPrefHeight(40);
        tfEmail.setStyle(fieldStyle);

        javafx.scene.control.Label lJours = new javafx.scene.control.Label("Jours avant rupture :");
        lJours.setStyle(lblStyle);
        javafx.scene.control.TextField tfJours = new javafx.scene.control.TextField("8");
        tfJours.setPrefHeight(40);
        tfJours.setStyle(fieldStyle);

        tfNom.textProperty().addListener((obs, old, val) -> {
            if (val == null || val.isBlank()) {
                suggestions.setVisible(false);
                suggestions.setManaged(false);
                return;
            }
            java.util.List<String> matches = vendeurService.searchByNom(val)
                    .stream().map(v -> v.get("nom")).collect(java.util.stream.Collectors.toList());
            if (matches.isEmpty()) {
                suggestions.setVisible(false);
                suggestions.setManaged(false);
            } else {
                suggestions.getItems().setAll(matches);
                suggestions.setVisible(true);
                suggestions.setManaged(true);
            }
        });

        suggestions.setOnMouseClicked(e -> {
            String selected = suggestions.getSelectionModel().getSelectedItem();
            if (selected != null) {
                tfNom.setText(selected);
                tfEmail.setText(nomEmailMap.getOrDefault(selected, ""));
                suggestions.setVisible(false);
                suggestions.setManaged(false);
            }
        });

        body.getChildren().addAll(lNom, tfNom, suggestions, lEmail, tfEmail, lJours, tfJours);

        javafx.scene.layout.HBox btnBar = new javafx.scene.layout.HBox(12);
        btnBar.setAlignment(javafx.geometry.Pos.CENTER);
        btnBar.setPadding(new javafx.geometry.Insets(10, 0, 0, 0));

        javafx.scene.control.Button btnEnvoyer = new javafx.scene.control.Button("ENVOYER");
        btnEnvoyer.setPrefWidth(160);
        btnEnvoyer.setPrefHeight(44);
        String btnEnvoyerBg = darkMode ? "linear-gradient(to right,#2563eb,#38bdf8)"
                : "linear-gradient(to right,#64748b,#94a3b8)";
        btnEnvoyer.setStyle("-fx-background-color:" + btnEnvoyerBg + ";" +
                "-fx-text-fill:white;-fx-font-weight:bold;-fx-font-size:14px;" +
                "-fx-background-radius:10;-fx-cursor:hand;");

        javafx.scene.control.Button btnAnnuler = new javafx.scene.control.Button("ANNULER");
        btnAnnuler.setPrefWidth(160);
        btnAnnuler.setPrefHeight(44);
        String btnAnnulerBg = darkMode ? "#374151" : "#e5e7eb";
        String btnAnnulerText = darkMode ? "#f1f5f9" : "#4b5563";
        btnAnnuler.setStyle("-fx-background-color:" + btnAnnulerBg + ";-fx-text-fill:" + btnAnnulerText + ";" +
                "-fx-font-size:14px;-fx-background-radius:10;-fx-cursor:hand;");

        btnEnvoyer.setOnAction(e -> {
            String nom = tfNom.getText();
            String email = tfEmail.getText();
            int jours = 8;
            try {
                jours = Integer.parseInt(tfJours.getText());
            } catch (Exception ex) {
            }

            if (email == null || email.isBlank()) {
                // Dialogue d'erreur
                Stage errDlg = new Stage();
                errDlg.initOwner(dialog);
                errDlg.initModality(javafx.stage.Modality.APPLICATION_MODAL);
                errDlg.initStyle(javafx.stage.StageStyle.UNDECORATED);

                javafx.scene.layout.VBox eRoot = new javafx.scene.layout.VBox(14);
                String errBg = darkMode ? "#0a0f1e" : "#fff7ed";
                String errBorder = darkMode ? "#f97316" : "#fb923c";
                eRoot.setStyle("-fx-background-color:" + errBg + ";-fx-border-color:" + errBorder + ";" +
                        "-fx-border-width:2;-fx-border-radius:14;-fx-background-radius:14;-fx-padding:24 28 20 28;");
                eRoot.setAlignment(javafx.geometry.Pos.CENTER);

                javafx.scene.control.Label eIcon = new javafx.scene.control.Label("\u26A0");
                eIcon.setStyle("-fx-font-size:36px;");
                javafx.scene.control.Label eTitle = new javafx.scene.control.Label("Email manquant");
                eTitle.setStyle("-fx-text-fill:" + (darkMode ? "#f97316" : "#ea580c") + ";-fx-font-size:16px;-fx-font-weight:bold;");
                javafx.scene.control.Button eOk = new javafx.scene.control.Button("Corriger");
                eOk.setStyle("-fx-background-color:#f97316;-fx-text-fill:white;-fx-font-weight:bold;");
                eOk.setOnAction(ev -> errDlg.close());

                eRoot.getChildren().addAll(eIcon, eTitle, eOk);
                errDlg.setScene(new javafx.scene.Scene(eRoot));
                errDlg.showAndWait();
                return;
            }

            dialog.close();
            final int joursFinaux = jours;
            new Thread(() -> {
                try {
                    List<Map<String, Object>> produits = statsService.getDataAsMaps();
                    edu.hanouti.services.ReportingService.sendVendeurEmail(email, nom, produits, joursFinaux);
                    final String heure = java.time.LocalTime.now()
                            .format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"));
                    javafx.application.Platform.runLater(() -> showEmailSuccessDialog(nom, heure));
                } catch (Exception ex) {
                    javafx.application.Platform.runLater(() -> {
                        javafx.scene.control.Alert error = new javafx.scene.control.Alert(
                                javafx.scene.control.Alert.AlertType.ERROR, "Erreur envoi: " + ex.getMessage());
                        error.showAndWait();
                    });
                }
            }).start();
        });

        btnAnnuler.setOnAction(e -> dialog.close());
        btnBar.getChildren().addAll(btnEnvoyer, btnAnnuler);
        body.getChildren().add(btnBar);
        root.getChildren().addAll(titleLbl, body);

        javafx.scene.Scene scene = new javafx.scene.Scene(root);
        scene.setFill(javafx.scene.paint.Color.TRANSPARENT);
        dialog.setScene(scene);
        dialog.showAndWait();
    }


    /** Dialogue succès email compact */
    private void showEmailSuccessDialog(String nom, String heure) {
        Stage dlg = new Stage();
        dlg.initOwner(primaryStage);
        dlg.initModality(javafx.stage.Modality.APPLICATION_MODAL);
        dlg.initStyle(javafx.stage.StageStyle.UNDECORATED);

        // Couleurs adaptatives — mode clair gris clair (plus de carte verte)
        String successBg = darkMode ? "#0a0f1e" : "#f1f5f9";
        String successBorder = darkMode ? "#38bdf8" : "#cbd5e1";
        String successTitle = darkMode ? "#38bdf8" : "#334155";
        String successSub = darkMode ? "#94a3b8" : "#64748b";
        String successTime = darkMode ? "#475569" : "#94a3b8";
        String successBtn = darkMode ? "#2563eb" : "#64748b";

        javafx.scene.layout.VBox root = new javafx.scene.layout.VBox(14);
        root.setStyle(
                "-fx-background-color:" + successBg + ";" +
                        "-fx-border-color:" + successBorder + ";" +
                        "-fx-border-width:2;-fx-border-radius:16;-fx-background-radius:16;" +
                        "-fx-padding:28 32 22 32;" +
                        "-fx-effect:dropshadow(gaussian," + (darkMode ? "rgba(0,0,0,0.6)" : "rgba(0,0,0,0.15)")
                        + ",20,0,0,6);");
        root.setPrefWidth(400);
        root.setAlignment(javafx.geometry.Pos.CENTER);

        javafx.scene.control.Label icon = new javafx.scene.control.Label("\u2705");
        icon.setStyle("-fx-font-size:38px;");
        javafx.animation.ScaleTransition iconPulse = new javafx.animation.ScaleTransition(
                javafx.util.Duration.millis(600), icon);
        iconPulse.setFromX(0.3);
        iconPulse.setFromY(0.3);
        iconPulse.setToX(1);
        iconPulse.setToY(1);
        iconPulse.setInterpolator(javafx.animation.Interpolator.EASE_OUT);
        iconPulse.play();

        javafx.scene.control.Label title = new javafx.scene.control.Label("Email Envoye !");
        title.setStyle("-fx-text-fill:" + successTitle + ";-fx-font-size:18px;-fx-font-weight:bold;");
        javafx.scene.control.Label sub = new javafx.scene.control.Label("A " + nom);
        sub.setStyle("-fx-text-fill:" + successSub + ";-fx-font-size:13px;");
        javafx.scene.control.Label time = new javafx.scene.control.Label("Heure : " + heure);
        time.setStyle("-fx-text-fill:" + successTime + ";-fx-font-size:12px;");

        javafx.scene.control.Button ok = new javafx.scene.control.Button("OK");
        ok.setPrefWidth(120);
        ok.setPrefHeight(38);
        ok.setStyle(
                "-fx-background-color:" + successBtn + ";" +
                        "-fx-text-fill:white;-fx-font-weight:bold;" +
                        "-fx-font-size:13px;-fx-background-radius:9;-fx-cursor:hand;");
        ok.setOnAction(ev -> dlg.close());

        root.getChildren().addAll(icon, title, sub, time, ok);
        javafx.scene.Scene s = new javafx.scene.Scene(root);
        s.setFill(javafx.scene.paint.Color.TRANSPARENT);
        dlg.setScene(s);
        dlg.showAndWait();
    }

    // ── [6.1] Dialogues Conseils (Ajouter / Promotions / Bundle) ──
    /** Dialogue pour ajouter manuellement un conseil marketing */
    private void openAjouterConseilDialog() {
        Stage dialog = new Stage();
        dialog.initOwner(primaryStage);
        dialog.initStyle(StageStyle.UNDECORATED);
        dialog.initModality(javafx.stage.Modality.APPLICATION_MODAL);

        VBox root = new VBox(20);
        root.setPadding(new Insets(30));
        root.setStyle("-fx-background-color: " + (darkMode ? "#0a0d1a" : "#ffffff") + ";" +
                "-fx-border-color: " + VIOLET + "; -fx-border-width: 2; -fx-border-radius: 16; -fx-background-radius: 16;");
        root.setPrefWidth(450);

        Label title = new Label("Nouveau Conseil Manuel");
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: 900; -fx-text-fill: " + TEXT_1 + ";");

        VBox form = new VBox(15);
        TextField tfProduit = new TextField();
        tfProduit.setPromptText("Nom du produit (ex: iPhone 15)");
        tfProduit.setStyle("-fx-background-color: " + (darkMode ? "rgba(255,255,255,0.05)" : "#f8fafc") + ";" +
                "-fx-text-fill: " + TEXT_1 + "; -fx-padding: 10; -fx-background-radius: 8;");

        ComboBox<String> cbType = new ComboBox<>(
                FXCollections.observableArrayList("Promotion", "Destockage", "Mise en avant", "Bundle"));
        cbType.setValue("Promotion");
        cbType.setMaxWidth(Double.MAX_VALUE);

        TextArea taDesc = new TextArea();
        taDesc.setPromptText("Description de votre conseil...");
        taDesc.setPrefHeight(100);

        form.getChildren().addAll(new Label("Produit"), tfProduit, new Label("Type"), cbType, new Label("Description"),
                taDesc);
        for (Node n : form.getChildren())
            if (n instanceof Label)
                ((Label) n).setStyle("-fx-text-fill: " + TEXT_2 + "; -fx-font-size: 12px;");

        HBox btns = new HBox(12);
        btns.setAlignment(Pos.CENTER_RIGHT);
        Button cancel = new Button("Annuler");
        cancel.setOnAction(e -> dialog.close());

        Button save = new Button("Enregistrer");
        save.setStyle("-fx-background-color: " + VIOLET
                + "; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 10 25; -fx-background-radius: 8; -fx-cursor: hand;");
        save.setOnAction(e -> {
            if (tfProduit.getText().isBlank() || taDesc.getText().isBlank()) {
                showNotification("Champs requis", "Veuillez remplir tous les champs.", "delete");
                return;
            }
            try {
                showNotification("Succès", "Conseil ajouté avec succès !", "success");
                dialog.close();
                naviguerVers("conseils");
            } catch (Exception ex) {
                showNotification("Erreur", "Impossible de sauvegarder.", "delete");
            }
        });

        btns.getChildren().addAll(cancel, save);
        root.getChildren().addAll(title, form, btns);
        Scene scene = new Scene(root);
        scene.setFill(Color.TRANSPARENT);
        dialog.setScene(scene);
        dialog.showAndWait();
    }

    /** Confirmation déstockage — même famille visuelle que les dialogs promo (sans Alert système). */
    private void openDestockageConfirmDialog(int idConseil, int idProduit, String prodName, VBox card) {
        Stage dialog = new Stage();
        dialog.initOwner(primaryStage);
        dialog.initStyle(StageStyle.UNDECORATED);
        dialog.initModality(javafx.stage.Modality.APPLICATION_MODAL);

        final String rose = "#f472b6";
        final String roseDeep = "#db2777";
        String bg = darkMode ? "#0d1526" : "#fff1f2";
        String glow = darkMode ? "rgba(244,114,182,0.45)" : "rgba(244,114,182,0.35)";

        VBox root = new VBox(16);
        root.setPadding(new Insets(28, 30, 26, 30));
        root.setStyle(
                "-fx-background-color:" + bg + ";" +
                        "-fx-background-radius:18;-fx-border-radius:18;-fx-border-width:2;-fx-border-color:" + rose + ";" +
                        "-fx-effect:dropshadow(gaussian," + glow + ",22,0.25,0,4);");
        root.setPrefWidth(430);

        Label icon = new Label("\uD83D\uDCE6");
        icon.setStyle("-fx-font-size:34px;");
        Label title = new Label("Déstockage");
        title.setStyle("-fx-font-size:21px;-fx-font-weight:900;-fx-text-fill:" + TEXT_1 + ";");
        Label warn = new Label("Action irréversible");
        warn.setStyle("-fx-font-size:12px;-fx-font-weight:bold;-fx-text-fill:" + roseDeep + ";");
        Label body = new Label(
                "La quantité en stock sera mise à 0 dans la table produit (quantite_stock) pour :\n\n« "
                        + prodName
                        + " »\n\nLe conseil sera marqué comme accepté.");
        body.setWrapText(true);
        body.setStyle("-fx-font-size:13px;-fx-text-fill:" + TEXT_2 + ";-fx-line-spacing:3;");

        Region gap = new Region();
        gap.setMinHeight(6);

        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER_RIGHT);
        Button cancel = new Button("Annuler");
        cancel.setStyle(
                "-fx-background-color:transparent;-fx-text-fill:" + TEXT_2 + ";"
                        + "-fx-border-color:rgba(148,163,184,0.45);-fx-border-radius:10;-fx-border-width:1;"
                        + "-fx-padding:10 20;-fx-background-radius:10;-fx-cursor:hand;");
        cancel.setOnAction(e -> dialog.close());

        Button ok = new Button("Confirmer");
        ok.setStyle(
                "-fx-background-color:linear-gradient(to bottom right," + rose + "," + roseDeep + ");"
                        + "-fx-text-fill:white;-fx-font-weight:bold;-fx-padding:10 24;-fx-background-radius:10;-fx-cursor:hand;");
        ok.setOnAction(e -> {
            if (idProduit > 0) {
                conseilsIaService.destocker(idProduit);
            }
            conseilsIaService.accepter(idConseil);
            showNotification("Déstockage", "Stock mis à 0 pour « " + prodName + " ».", "success");
            dialog.close();
            animateExit(card);
        });

        row.getChildren().addAll(cancel, ok);
        root.getChildren().addAll(icon, title, warn, body, gap, row);

        Scene sc = new Scene(root);
        sc.setFill(Color.TRANSPARENT);
        dialog.setScene(sc);
        dialog.showAndWait();
    }

    private void openPromotionConfigDialog(int idConseil, String produit, String type, int defaultPct,
            VBox card) {
        Stage dialog = new Stage();
        dialog.initOwner(primaryStage);
        dialog.initStyle(StageStyle.UNDECORATED);
        dialog.initModality(javafx.stage.Modality.APPLICATION_MODAL);

        VBox root = new VBox(25);
        root.setPadding(new Insets(30));
        root.setStyle("-fx-background-color: " + (darkMode ? "#0a1020" : "#ffffff") + ";" +
                "-fx-border-color: " + PASTEL_SKY + "; -fx-border-width: 2; -fx-border-radius: 20; -fx-background-radius: 20;");
        root.setPrefWidth(480);

        Label title = new Label("\uD83D\uDCE3 Configurer la Promotion");
        title.setStyle("-fx-font-size: 22px; -fx-font-weight: 900; -fx-text-fill: " + TEXT_1 + ";");
        Label subTitle = new Label("Pour : " + produit);
        subTitle.setStyle("-fx-text-fill: " + TEXT_2 + "; -fx-font-size: 14px;");

        VBox form = new VBox(20);
        
        // -- Dates --
        HBox dateRow = new HBox(20);
        VBox startCol = new VBox(8);
        Label lblStart = new Label("\uD83D\uDCC5 Date de Début");
        lblStart.setStyle("-fx-text-fill: " + TEXT_2 + "; -fx-font-weight: bold;");
        DatePicker dpStart = new DatePicker(java.time.LocalDate.now());
        String dpStyle = "-fx-background-color: " + (darkMode ? "#1e293b" : "#ffffff") + ";"
                + "-fx-text-fill: " + (darkMode ? "#f1f5f9" : "#0f172a") + ";"
                + "-fx-border-color: " + (darkMode ? "#334155" : "#cbd5e1") + ";"
                + "-fx-border-radius: 8; -fx-background-radius: 8; -fx-border-width: 1;"
                + "-fx-font-size: 13px;";
        dpStart.setStyle(dpStyle);
        startCol.getChildren().addAll(lblStart, dpStart);

        VBox endCol = new VBox(8);
        Label lblEnd = new Label("\u231B Date de Fin");
        lblEnd.setStyle("-fx-text-fill: " + TEXT_2 + "; -fx-font-weight: bold;");
        DatePicker dpEnd = new DatePicker(java.time.LocalDate.now().plusDays(7));
        dpEnd.setStyle(dpStyle);
        endCol.getChildren().addAll(lblEnd, dpEnd);
        dateRow.getChildren().addAll(startCol, endCol);

        // -- Remise --
        VBox pctCol = new VBox(10);
        int safePct = Math.min(90, Math.max(5, defaultPct));
        Label pctLabel = new Label("\uD83C\uDFF7\uFE0F Remise appliquée : " + safePct + "%");
        pctLabel.setStyle("-fx-text-fill: " + PASTEL_SKY + "; -fx-font-size: 16px; -fx-font-weight: 900;");
        Slider slider = new Slider(5, 90, safePct);
        slider.setShowTickLabels(true);
        slider.setShowTickMarks(true);
        slider.setStyle("-fx-cursor: hand;");
        TextField tfPct = new TextField(String.valueOf(safePct));
        tfPct.setPromptText("%");
        tfPct.setMaxWidth(72);
        tfPct.setStyle("-fx-text-fill: " + TEXT_1 + "; -fx-background-color: " + (darkMode ? "#111827" : "#f1f5f9")
                + "; -fx-background-radius: 8;");
        slider.valueProperty().addListener((obs, oldVal, newVal) -> {
            int v = newVal.intValue();
            pctLabel.setText("\uD83C\uDFF7\uFE0F Remise appliquée : " + v + "%");
            tfPct.setText(String.valueOf(v));
        });
        Label pctHint = new Label("Ou saisir le % :");
        pctHint.setStyle("-fx-text-fill: " + TEXT_2 + "; -fx-font-size: 12px;");
        pctCol.getChildren().addAll(pctLabel, slider, pctHint, tfPct);

        form.getChildren().addAll(dateRow, pctCol);

        HBox btns = new HBox(15);
        btns.setAlignment(Pos.CENTER_RIGHT);
        Button cancel = new Button("Annuler");
        cancel.setStyle("-fx-background-color: transparent; -fx-text-fill: " + TEXT_2 + "; -fx-cursor: hand;");
        cancel.setOnAction(e -> dialog.close());

        Button confirm = new Button("Lancer la Campagne \uD83D\uDE80");
        confirm.setStyle("-fx-background-color: " + PASTEL_SKY
                + "; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 12 30; -fx-background-radius: 10; -fx-cursor: hand;");
        confirm.setOnAction(e -> {
            if (dpStart.getValue() == null || dpEnd.getValue() == null) {
                showNotification("Dates manquantes", "Choisissez la date de début et de fin.", "delete");
                return;
            }
            if (dpEnd.getValue().isBefore(dpStart.getValue())) {
                showNotification("Dates invalides", "La date de fin doit être après la date de début.", "delete");
                return;
            }
            int pct;
            try {
                String raw = tfPct.getText() == null ? "" : tfPct.getText().trim().replaceAll("[^0-9]", "");
                pct = raw.isEmpty() ? (int) Math.round(slider.getValue()) : Integer.parseInt(raw);
            } catch (NumberFormatException ex) {
                pct = (int) Math.round(slider.getValue());
            }
            pct = Math.min(90, Math.max(5, pct));
            java.time.LocalDate dStart = dpStart.getValue();
            java.time.LocalDate dEnd = dpEnd.getValue();
            String start = dStart.toString();
            String end = dEnd.toString();
            conseilsIaService.accepterAvecConfig(idConseil, start, end, pct);
            showNotification("\uD83D\uDE80 Promotion Active", "Le produit " + produit + " est maintenant en promotion !", "success");
            dialog.close();
            offerAddPromoToGoogleCalendar(produit, type, dStart, dEnd, pct);
            if (card != null) {
                animateExit(card);
            }
            naviguerVers("conseils");
        });

        btns.getChildren().addAll(cancel, confirm);
        root.getChildren().addAll(title, subTitle, form, btns);
        Scene scene = new Scene(root);
        scene.setFill(Color.TRANSPARENT);
        dialog.setScene(scene);
        dialog.showAndWait();
    }

    /**
     * Après acceptation d’une promo : propose d’ouvrir Google Calendar (navigateur) avec les dates / remise préremplies.
     */
    private void offerAddPromoToGoogleCalendar(String produit, String type, java.time.LocalDate dateDebut,
            java.time.LocalDate dateFin, int pct) {
        if (primaryStage == null)
            return;
        Stage ask = new Stage();
        ask.initOwner(primaryStage);
        ask.initModality(Modality.WINDOW_MODAL);
        ask.setTitle("Google Calendar");

        VBox root = new VBox(18);
        root.setPadding(new Insets(24));
        root.setPrefWidth(440);
        root.setStyle("-fx-background-color: " + (darkMode ? BG_CARD : "#ffffff") + "; -fx-background-radius: 14;");

        Label head = new Label("Ajouter au Google Calendar ?");
        head.setStyle("-fx-font-size: 17px; -fx-font-weight: bold; -fx-text-fill: " + TEXT_1 + ";");

        Label body = new Label("Promo « " + produit + " » (-" + pct + "%)\nDu " + dateDebut + " au " + dateFin
                + " inclus.\n\nSi tu choisis Oui, le navigateur s'ouvre sur la creation d'un evenement Google "
                + "(compte Google requis ; tu peux encore modifier avant d'enregistrer).");
        body.setWrapText(true);
        body.setStyle("-fx-text-fill: " + TEXT_2 + "; -fx-font-size: 13px;");

        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER_RIGHT);
        Button no = new Button("Non merci");
        no.setStyle("-fx-background-color: transparent; -fx-text-fill: " + TEXT_2 + "; -fx-cursor: hand;");
        no.setOnAction(e -> ask.close());
        Button yes = new Button("Oui, ouvrir Google Calendar");
        yes.setDefaultButton(true);
        yes.setStyle("-fx-background-color: " + PASTEL_SKY
                + "; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 10 18; -fx-background-radius: 8; -fx-cursor: hand;");
        yes.setOnAction(e -> {
            try {
                String url = GoogleCalendarPromoLink.buildPromoTemplateUrl(produit, type, dateDebut, dateFin, pct);
                if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                    Desktop.getDesktop().browse(java.net.URI.create(url));
                } else {
                    getHostServices().showDocument(url);
                }
            } catch (Exception ex) {
                showNotification("Calendrier", "Impossible d'ouvrir le navigateur : " + ex.getMessage(), "error");
            }
            ask.close();
        });
        row.getChildren().addAll(no, yes);
        root.getChildren().addAll(head, body, row);

        Scene sc = new Scene(root);
        ask.setScene(sc);
        ask.showAndWait();
    }

    private void openBundleConfigDialog(int idConseil, String produitPrincipal) {
        Stage bundleDialog = new Stage();
        bundleDialog.initOwner(primaryStage);
        bundleDialog.initStyle(StageStyle.UNDECORATED);
        bundleDialog.initModality(javafx.stage.Modality.APPLICATION_MODAL);

        VBox bundleRoot = new VBox(25);
        bundleRoot.setPadding(new Insets(30));
        bundleRoot.setStyle("-fx-background-color: " + (darkMode ? "#0a1020" : "#ffffff") + ";" +
                "-fx-border-color: " + VIOLET + "; -fx-border-width: 2; -fx-border-radius: 20; -fx-background-radius: 20;");
        bundleRoot.setPrefWidth(500);

        Label bundleTitle = new Label("Composer le Bundle : " + produitPrincipal);
        bundleTitle.setStyle("-fx-font-size: 20px; -fx-font-weight: 900; -fx-text-fill: " + TEXT_1 + ";");

        VBox bundleForm = new VBox(20);
        
        List<Map<String, String>> prods = conseilsIaService.getProduits();
        List<String> prodNames = prods.stream().map(m -> m.get("nom")).collect(java.util.stream.Collectors.toList());

        VBox prod1Col = new VBox(8);
        Label bl1 = new Label("Produit Complémentaire 1");
        bl1.setStyle("-fx-text-fill: " + TEXT_2 + "; -fx-font-size: 12px;");
        ComboBox<String> cb1 = new ComboBox<>(FXCollections.observableArrayList(prodNames));
        cb1.setPromptText("Sélectionner un produit...");
        cb1.setMaxWidth(Double.MAX_VALUE);
        cb1.setStyle("-fx-background-color: " + (darkMode ? "#111827" : "#f1f5f9") + "; -fx-text-fill: " + TEXT_1 + ";");
        prod1Col.getChildren().addAll(bl1, cb1);

        VBox prod2Col = new VBox(8);
        Label bl2 = new Label("Produit Complémentaire 2");
        bl2.setStyle("-fx-text-fill: " + TEXT_2 + "; -fx-font-size: 12px;");
        ComboBox<String> cb2 = new ComboBox<>(FXCollections.observableArrayList(prodNames));
        cb2.setPromptText("Sélectionner un produit...");
        cb2.setMaxWidth(Double.MAX_VALUE);
        cb2.setStyle("-fx-background-color: " + (darkMode ? "#111827" : "#f1f5f9") + "; -fx-text-fill: " + TEXT_1 + ";");
        prod2Col.getChildren().addAll(bl2, cb2);

        VBox promoCol = new VBox(8);
        Label bl3 = new Label("Remise du Bundle (%)");
        bl3.setStyle("-fx-text-fill: " + TEXT_2 + "; -fx-font-size: 12px;");
        Slider bundleSlider = new Slider(5, 50, 15);
        bundleSlider.setShowTickLabels(true);
        Label valL = new Label("Remise : 15%");
        valL.setStyle("-fx-text-fill: " + VIOLET + "; -fx-font-weight: bold;");
        bundleSlider.valueProperty().addListener((o, ov, nv) -> valL.setText("Remise : " + nv.intValue() + "%"));
        promoCol.getChildren().addAll(bl3, valL, bundleSlider);

        bundleForm.getChildren().addAll(prod1Col, prod2Col, promoCol);

        HBox bundleBtns = new HBox(15);
        bundleBtns.setAlignment(Pos.CENTER_RIGHT);
        Button bundleCancel = new Button("Annuler");
        bundleCancel.setOnAction(e -> bundleDialog.close());

        Button bundleConfirm = new Button("Créer le Bundle");
        bundleConfirm.setStyle("-fx-background-color: " + VIOLET
                + "; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 12 30; -fx-background-radius: 10; -fx-cursor: hand;");
        bundleConfirm.setOnAction(e -> {
            String p1 = cb1.getValue();
            String p2 = cb2.getValue();
            if (p1 == null || p2 == null) {
                showNotification("Champs requis", "Veuillez sélectionner 2 produits.", "error");
                return;
            }
            conseilsIaService.accepter(idConseil);
            showNotification("🎁 Bundle Créé", "Le pack avec " + p1 + " et " + p2 + " est lancé !", "success");
            bundleDialog.close();
            naviguerVers("conseils");
        });

        bundleBtns.getChildren().addAll(bundleCancel, bundleConfirm);
        bundleRoot.getChildren().addAll(bundleTitle, bundleForm, bundleBtns);
        Scene bundleScene = new Scene(bundleRoot);
        bundleScene.setFill(Color.TRANSPARENT);
        bundleDialog.setScene(bundleScene);
        bundleDialog.showAndWait();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
