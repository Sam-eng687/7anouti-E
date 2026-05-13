package projet.hanouti.AIachat.controllers;

import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.util.Duration;
import projet.hanouti.AIachat.entities.GeminiResult;
import projet.hanouti.AIachat.entities.HistoriqueIA;
import projet.hanouti.AIachat.entities.Produit;
import projet.hanouti.user_auth.entities.User;
import projet.hanouti.AIachat.services.GeminiService;
import projet.hanouti.AIachat.services.HistoriqueIAServices;
import projet.hanouti.AIachat.services.InteractionUtilisateurServices;
import projet.hanouti.AIachat.services.ProduitServices;
import projet.hanouti.AIachat.services.ScoringService;
import projet.hanouti.AIachat.services.VoiceService;
import projet.hanouti.AIachat.services.WishlistService;
import projet.hanouti.AIachat.tools.ConversationState;
import projet.hanouti.AIachat.tools.KeywordExtractor;
import projet.hanouti.AIachat.tools.RefinementDetector;
import projet.hanouti.AIachat.tools.RefinementIntent;
import projet.hanouti.AIachat.tools.RefinementType;
import projet.hanouti.common.utils.SessionManager;
import projet.hanouti.common.utils.UiIcons;
import projet.hanouti.AIachat.entities.ImageRecognitionResult;
import projet.hanouti.AIachat.entities.ConseilPromo;
import projet.hanouti.AIachat.services.CameraService;
import projet.hanouti.AIachat.services.PromotionService;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Collectors;

public class AssistantIAController {

    // ── FXML - outer root ─────────────────────────────────────────────────────
    @FXML private HBox rootPane;

    // ── FXML - sidebar ────────────────────────────────────────────────────────
    @FXML private Button btnExplore;
    @FXML private Button btnAssistant;

    // ── FXML - navbar ─────────────────────────────────────────────────────────
    @FXML private VBox      rootVBox;
    @FXML private TextField navSearchField;
    @FXML private Button    dayModeBtn;
    @FXML private Button    cartBtn;

    // ── FXML - context / welcome ──────────────────────────────────────────────
    @FXML private Label contextLabel;
    @FXML private Label stateBadge;
    @FXML private Label welcomeLabel;

    // ── FXML - sections ───────────────────────────────────────────────────────
    @FXML private VBox idleSections;
    @FXML private VBox chatSection;
    @FXML private VBox resultsSection;

    // ── FXML - idle rows (plain HBox - no scroll, 6 cards fit side by side) ────
    @FXML private HBox lastRow1;
    @FXML private HBox lastRow2;
    @FXML private HBox lastRow3;
    @FXML private VBox lastExpandRows;
    @FXML private Button showMoreLastBtn;

    @FXML private HBox topRow1;
    @FXML private HBox topRow2;
    @FXML private HBox topRow3;
    @FXML private VBox topExpandRows;
    @FXML private Button showMoreTopBtn;

    // ── FXML - chat & results ─────────────────────────────────────────────────
    @FXML private VBox      chatBubbleBox;
    @FXML private HBox      resultToolbarBox;
    @FXML private FlowPane  resultCardsBox;   // was HBox - now wraps cards vertically
    @FXML private Label     resultsCountLabel;

    // ── FXML - StackPane + scroll ─────────────────────────────────────────────
    @FXML private StackPane  mainArea;
    @FXML private ScrollPane mainScroll;
    @FXML private VBox       mainContent;

    // ── FXML - floating bar ───────────────────────────────────────────────────
    @FXML private HBox      floatingBar;
    @FXML private TextField floatingInput;
    @FXML private Button    floatingMicBtn;
    @FXML private Button    floatingSendBtn;

    // ── FXML - chat bar ───────────────────────────────────────────────────────
    @FXML private HBox       chatBarHBox;
    @FXML private Button     chatMicBtn;
    @FXML private TextField  inputField;

    // ── FXML - send button lock + spinner ────────────────────────────────────
    @FXML private Button            sendButton;
    @FXML private ProgressIndicator progressIndicator;

    // ── FXML - offers section ─────────────────────────────────────────────────
    @FXML private VBox      offersPlaceholder;
    @FXML private HBox      offerRow1;
    @FXML private HBox      offerRow2;
    @FXML private HBox      offerRow3;
    @FXML private VBox      offerExpandRows;
    @FXML private Button    showMoreOffersBtn;

    // ── Services ──────────────────────────────────────────────────────────────
    private final HistoriqueIAServices           iaService          = new HistoriqueIAServices();
    private final ProduitServices                produitSvc         = new ProduitServices();
    private final ScoringService                 scoringSvc         = new ScoringService();
    private final InteractionUtilisateurServices interactionService = new InteractionUtilisateurServices();
    private final GeminiService                  geminiService      = new GeminiService();
    private final VoiceService                   voiceService       = new VoiceService();
    private final WishlistService                wishlistService    = new WishlistService();

    // ── Wishlist in-memory set - preloaded on startup, kept in sync ──────────
    private final Set<Integer> wishlistProductIds = new HashSet<>();

    // ── Promotions - loaded once at startup, used by card builders + scoring ─
    private final PromotionService     promotionService = new PromotionService();
    private       Map<Integer, Double> activePromoMap   = new HashMap<>();
    private       List<ConseilPromo>   activeConseils   = new ArrayList<>();

    // ── Conversation state ────────────────────────────────────────────────────
    private ConversationState                  state                    = ConversationState.IDLE;
    private List<String>                       currentKeywords;
    private String                             currentCategorie;
    private double                             currentBudget;
    private List<ScoringService.ScoredProduct> originalResults;
    private List<ScoringService.ScoredProduct> currentResults;
    private final Set<Integer>                 excludedProductIds       = new HashSet<>();
    private int                                refinementBudgetAttempts = 0;
    private boolean                            isBudgetNegotiation      = false;
    private volatile boolean                   isRecording              = false;
    // ── Camera ────────────────────────────────────────────────────────────────
    @FXML private Button floatingCameraBtn;
    @FXML private Button chatCameraBtn;

    private final CameraService cameraService = new CameraService();
    private volatile boolean isCameraProcessing = false;

    // ── Detail panel (right-side slide-in, programmatic - not FXML) ──────────
    private VBox    detailPanel       = null;
    private boolean detailPanelOpen   = false;
    private Produit detailPanelProduit = null;
    private boolean showingImageResults = false;
    // ── Theme ─────────────────────────────────────────────────────────────────
    private boolean isDarkMode = true;

    // ── Sidebar + floating bar state ──────────────────────────────────────────
    private String  activeSidebarTab    = "explore";
    private boolean floatingBarExpanded = false;

    // ── Bubble typing queue ───────────────────────────────────────────────────
    private static final class BubbleJob {
        final Label  label;
        final String text;
        BubbleJob(Label l, String t) { label = l; text = t; }
    }
    private final Queue<BubbleJob> typingQueue      = new ArrayDeque<>();
    private       boolean          isBubbleTyping   = false;
    private       Timeline         activeTypingLine = null;

    // ── Theme helpers ─────────────────────────────────────────────────────────
    private String cardBg()          { return isDarkMode ? "#111425" : "#FFFFFF"; }
    private String cardBorder()      { return isDarkMode ? "rgba(255,255,255,0.08)" : "#E2E8F0"; }
    private String cardHoverBg()     { return isDarkMode ? "#161830" : "#F8FAFC"; }
    private String textPrimary()     { return isDarkMode ? "#F1F5F9" : "#0F172A"; }
    private String textMuted()       { return isDarkMode ? "#94A3B8" : "#64748B"; }
    private String green()           { return isDarkMode ? "#10B981" : "#16A34A"; }
    private String orange()          { return isDarkMode ? "#F97316" : "#EA580C"; }
    private String accent()          { return isDarkMode ? "#38BDF8" : "#2563EB"; }
    private String accentGlow()      { return isDarkMode ? "rgba(37,99,235,0.30)" : "rgba(37,99,235,0.20)"; }
    private String userBubbleBg()    { return isDarkMode ? "rgba(37,99,235,0.14)" : "#EFF6FF"; }
    private String botBubbleBg()     { return isDarkMode ? "#111425" : "#FFFFFF"; }
    private String botBubbleBorder() { return isDarkMode ? "rgba(255,255,255,0.07)" : "#E2E8F0"; }

    public void applyTheme(boolean dark) {
        isDarkMode = dark;
        applyThemeClasses();
        refreshActionIcons();
        restyleDetailPanel();

        if (state == ConversationState.IDLE) {
            loadIdleCards();
            loadOfferCards();
        } else if (state == ConversationState.SHOWING_RESULTS && currentResults != null) {
            if (showingImageResults) buildImageResultCards();
            else buildResultCards();
        }
    }

    private void applyThemeClasses() {
        for (Region r : new Region[]{rootPane, rootVBox}) {
            if (r == null) continue;
            r.getStyleClass().remove("light-mode");
            if (!isDarkMode) r.getStyleClass().add("light-mode");
        }
        if (dayModeBtn != null) {
            dayModeBtn.setText(isDarkMode ? "Mode Jour" : "Mode Nuit");
        }
    }

    private void refreshActionIcons() {
        String cameraColor = isCameraProcessing ? (isDarkMode ? "#FCD34D" : "#B45309") : (isDarkMode ? "#93C5FD" : "#1D4ED8");
        String micColor = isRecording ? (isDarkMode ? "#FCA5A5" : "#DC2626") : (isDarkMode ? "#A78BFA" : "#7C3AED");
        UiIcons.Icon micIcon = isRecording ? UiIcons.Icon.STOP : UiIcons.Icon.MIC;

        UiIcons.setButtonIcon(floatingMicBtn, micIcon, micColor, 17, isRecording ? "Arreter l'enregistrement" : "Cliquer pour dicter");
        UiIcons.setButtonIcon(chatMicBtn, micIcon, micColor, 17, isRecording ? "Arreter l'enregistrement" : "Cliquer pour dicter");
        UiIcons.setButtonIcon(floatingCameraBtn, UiIcons.Icon.CAMERA, cameraColor, 17, "Recherche par image");
        UiIcons.setButtonIcon(chatCameraBtn, UiIcons.Icon.CAMERA, cameraColor, 17, "Recherche par image");
        UiIcons.setButtonIcon(floatingSendBtn, UiIcons.Icon.SEND, "#FFFFFF", 18, "Envoyer");
        UiIcons.setButtonIcon(sendButton, UiIcons.Icon.SEND, "#FFFFFF", 18, "Envoyer");
    }

    private void restyleDetailPanel() {
        if (detailPanel == null) return;
        detailPanel.setStyle(
                "-fx-background-color:" + (isDarkMode ? "#111425" : "#FFFFFF") + ";" +
                        "-fx-border-color:" + (isDarkMode ? "rgba(255,255,255,0.08)" : "#E2E8F0") + ";" +
                        "-fx-border-width:0 0 0 1;");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Initialize
    // ─────────────────────────────────────────────────────────────────────────

    @FXML
    void initialize() {
        isDarkMode = SessionManager.getInstance().isDarkMode();
        applyThemeClasses();
        refreshActionIcons();

        if (floatingBar   != null) floatingBar.setOnMouseClicked(e  -> { expandFloatingBar(); e.consume(); });
        if (floatingInput != null) floatingInput.setOnMouseClicked(e -> { expandFloatingBar(); e.consume(); });

        // Click anywhere outside the floating bar → animate it back down
        if (mainArea != null) {
            mainArea.addEventFilter(javafx.scene.input.MouseEvent.MOUSE_CLICKED, e -> {
                if (floatingBarExpanded && floatingBar != null) {
                    javafx.geometry.Bounds barBounds =
                            floatingBar.localToScene(floatingBar.getBoundsInLocal());
                    if (!barBounds.contains(e.getSceneX(), e.getSceneY())) {
                        shrinkFloatingBar();
                    }
                }
            });
        }
        if (progressIndicator != null) progressIndicator.setVisible(false);
        activateSidebarTab("explore");

        // ── P4: detail panel — 3rd child of rootPane (outer HBox) ────────────
        detailPanel = new VBox();
        detailPanel.setPrefWidth(0);
        detailPanel.setMinWidth(0);
        detailPanel.setMaxWidth(0);
        detailPanel.setVisible(false);
        detailPanel.setManaged(false);
        restyleDetailPanel();
        if (rootPane != null) rootPane.getChildren().add(detailPanel);

        enterIdle();
        updateMicBtnState();
        showCameraButtons(true);

        // ── Silent auto-purge on startup - deletes stale records older than 7 days ──
        final int idAtStartup = SessionManager.getCurrentUserId();
        Thread purgeThread = new Thread(() -> {
            iaService.deleteOlderThanOneWeek(idAtStartup);
            interactionService.deleteOlderThanOneWeek(idAtStartup);
            // Preload wishlist ids into memory
            List<Integer>        ids      = wishlistService.getByUser(idAtStartup);
            Map<Integer, Double> promoMap = promotionService.loadActivePromoMap();
            List<ConseilPromo>   conseils = promotionService.getActiveConseils();
            Platform.runLater(() -> {
                wishlistProductIds.clear();
                wishlistProductIds.addAll(ids);
                activePromoMap  = promoMap;
                activeConseils  = conseils;
                loadOfferCards();
            });
        }, "startup-purge");
        purgeThread.setDaemon(true);
        purgeThread.start();
    }

    public void openExploreMode() {
        Platform.runLater(this::enterIdle);
    }

    public void openAssistantMode() {
        Platform.runLater(this::enterWaitingNeed);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // FXML handlers
    // ─────────────────────────────────────────────────────────────────────────

    @FXML
    void handleSend(ActionEvent event) {
        String input = inputField.getText() != null ? inputField.getText().trim() : "";
        if (input.isEmpty()) return;
        inputField.clear();
        handleUserInput(input);
    }

    @FXML
    void handleNavSearch(ActionEvent event) {
        String input = navSearchField.getText() != null ? navSearchField.getText().trim() : "";
        if (input.isEmpty()) return;
        navSearchField.clear();
        enterWaitingNeed();
        handleNeedInput(input);
    }

    @FXML
    void handleReset(ActionEvent event) {
        enterIdle();
    }

    @FXML
    void toggleLastExpand(ActionEvent event) {
        boolean expand = !lastExpandRows.isVisible();
        lastExpandRows.setVisible(expand);
        lastExpandRows.setManaged(expand);
        showMoreLastBtn.setText(expand ? "Show less ↑" : "Show more ↓");
    }

    @FXML
    void toggleTopExpand(ActionEvent event) {
        boolean expand = !topExpandRows.isVisible();
        topExpandRows.setVisible(expand);
        topExpandRows.setManaged(expand);
        showMoreTopBtn.setText(expand ? "Show less ↑" : "Show more ↓");
    }

    @FXML
    void toggleOffersExpand(ActionEvent event) {
        if (offerExpandRows == null) return;
        boolean expand = !offerExpandRows.isVisible();
        offerExpandRows.setVisible(expand);
        offerExpandRows.setManaged(expand);
        if (showMoreOffersBtn != null)
            showMoreOffersBtn.setText(expand ? "Show less ↑" : "Show more ↓");
    }

    @FXML
    void toggleDayMode(ActionEvent event) {
        applyTheme(!isDarkMode);
    }

    @FXML
    void handleExploreTab() {
        if ("explore".equals(activeSidebarTab)) return;
        fadeContent(this::enterIdle);
    }

    @FXML
    void handleAssistantTab() {
        if ("assistant".equals(activeSidebarTab)) return;
        if (state == ConversationState.IDLE) {
            fadeContent(this::enterWaitingNeed);
        } else {
            activateSidebarTab("assistant");
            showFloatingBar(false);
            showChatBar(true);
        }
    }

    @FXML
    void handleFloatingSubmit() {
        String input = floatingInput.getText() != null ? floatingInput.getText().trim() : "";
        if (input.isEmpty()) return;
        floatingInput.clear();
        collapseFloatingBar(() -> {
            activateSidebarTab("assistant");
            showChatBar(true);
            handleUserInput(input);
        });
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Voice / STT
    // ─────────────────────────────────────────────────────────────────────────

    @FXML
    void handleMicBtn(ActionEvent event) {
        if (!isRecording) startVoiceRecording();
        else stopVoiceRecording();
    }

    private void startVoiceRecording() {
        Thread worker = new Thread(() -> {
            if (!voiceService.isServerRunning()) {
                Platform.runLater(() -> showMicStatus("Serveur Whisper non démarré. Lancez : python whisper_server.py"));
                return;
            }
            try {
                voiceService.startRecording();
                isRecording = true;
                Platform.runLater(() -> {
                    setMicRecordingStyle(true);
                    if (contextLabel != null)
                        contextLabel.setText("Enregistrement... parlez maintenant. Cliquez sur le micro pour arrêter.");
                });
            } catch (VoiceService.VoiceException e) {
                Platform.runLater(() -> showMicStatus("Impossible de démarrer l'enregistrement."));
            }
        }, "whisper-start");
        worker.setDaemon(true);
        worker.start();
    }

    private void stopVoiceRecording() {
        isRecording = false;
        setMicRecordingStyle(false);
        if (contextLabel != null) contextLabel.setText("Transcription en cours...");
        Thread worker = new Thread(() -> {
            try {
                String text = voiceService.stopAndTranscribe();
                Platform.runLater(() -> applyTranscription(text));
            } catch (VoiceService.VoiceException e) {
                Platform.runLater(() -> showMicStatus("Erreur transcription : " + e.getMessage()));
            }
        }, "whisper-transcribe");
        worker.setDaemon(true);
        worker.start();
    }

    private void applyTranscription(String text) {
        if (text == null || text.trim().isEmpty()) {
            if (contextLabel != null) contextLabel.setText("Rien détecté. Réessayez en parlant plus fort.");
            return;
        }
        if (chatBarHBox != null && chatBarHBox.isVisible() && inputField != null) {
            inputField.setText(text);
            inputField.requestFocus();
        } else if (floatingBar != null && floatingBar.isVisible() && floatingInput != null) {
            floatingInput.setText(text);
            floatingInput.requestFocus();
        }
        if (contextLabel != null) contextLabel.setText("Transcrit : \"" + text + "\"");
    }

    private void setMicRecordingStyle(boolean recording) {
        for (Button btn : new Button[]{floatingMicBtn, chatMicBtn}) {
            if (btn == null) continue;
            if (recording) {
                btn.getStyleClass().remove("mic-btn");
                if (!btn.getStyleClass().contains("mic-btn-recording"))
                    btn.getStyleClass().add("mic-btn-recording");
            } else {
                btn.getStyleClass().remove("mic-btn-recording");
                if (!btn.getStyleClass().contains("mic-btn"))
                    btn.getStyleClass().add("mic-btn");
            }
        }
        refreshActionIcons();
    }

    private void updateMicBtnState() {
        Thread worker = new Thread(() -> {
            boolean up = voiceService.isServerRunning();
            Platform.runLater(() -> {
                for (Button btn : new Button[]{floatingMicBtn, chatMicBtn}) {
                    if (btn == null) continue;
                    btn.setOpacity(up ? 1.0 : 0.38);
                    btn.setDisable(false);
                    Tooltip.install(btn, new Tooltip(up
                            ? "Cliquer pour dicter"
                            : "Serveur Whisper non démarré - lancez whisper_server.py"));
                }
            });
        }, "whisper-health");
        worker.setDaemon(true);
        worker.start();
    }

    private void showMicStatus(String msg) {
        if (contextLabel != null) contextLabel.setText(msg);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Camera / Image search - Step 8b
    // ─────────────────────────────────────────────────────────────────────────

    @FXML
    void handleCameraBtn(ActionEvent event) {
        if (isCameraProcessing) return;
        if (state == ConversationState.WAITING_BUDGET) return;

        // ── Owner window ──────────────────────────────────────────────────────
        javafx.stage.Window owner = (floatingBar != null && floatingBar.isVisible())
                ? floatingBar.getScene().getWindow()
                : (chatBarHBox != null ? chatBarHBox.getScene().getWindow() : null);

        // ── Custom themed popup ───────────────────────────────────────────────
        javafx.stage.Stage popup = new javafx.stage.Stage();
        popup.initModality(javafx.stage.Modality.WINDOW_MODAL);
        if (owner != null) popup.initOwner(owner);
        popup.initStyle(javafx.stage.StageStyle.UNDECORATED);
        popup.setResizable(false);

        // Result holder - null = cancelled
        final boolean[] useWebcam = {false};
        final boolean[] confirmed = {false};

        // ── Background ───────────────────────────────────────────────────────
        String bg      = isDarkMode ? "#111520" : "#FFFFFF";
        String border  = isDarkMode ? "#2A3452" : "#D0D9EE";
        String text1   = isDarkMode ? "#F0F4FF" : "#0F172A";
        String text2   = isDarkMode ? "#8B9DC3" : "#475569";

        VBox root = new VBox(20);
        root.setPadding(new Insets(28, 32, 24, 32));
        root.setAlignment(Pos.CENTER);
        root.setStyle(
                "-fx-background-color:" + bg + ";" +
                        "-fx-border-color:" + border + ";" +
                        "-fx-border-width:1.5;" +
                        "-fx-border-radius:16;" +
                        "-fx-background-radius:16;" +
                        "-fx-effect:dropshadow(three-pass-box,rgba(0,0,0,0.55),32,0,0,8);"
        );

        // ── Title ─────────────────────────────────────────────────────────────
        Label title = new Label("Recherche par image");
        title.setStyle("-fx-text-fill:" + text1 + ";-fx-font-size:15px;-fx-font-weight:bold;");

        Label subtitle = new Label("Comment voulez-vous fournir l'image ?");
        subtitle.setStyle("-fx-text-fill:" + text2 + ";-fx-font-size:12px;");

        // ── Buttons ───────────────────────────────────────────────────────────
        String btnStyle =
                "-fx-background-color:transparent;" +
                        "-fx-border-color:" + border + ";" +
                        "-fx-border-radius:10;" +
                        "-fx-background-radius:10;" +
                        "-fx-text-fill:" + text1 + ";" +
                        "-fx-font-size:13px;" +
                        "-fx-font-weight:bold;" +
                        "-fx-cursor:hand;" +
                        "-fx-padding:12 24 12 24;" +
                        "-fx-min-width:160;";

        String btnHoverStyle =
                "-fx-background-color:rgba(20,184,166,0.14);" +
                        "-fx-border-color:#14B8A6;" +
                        "-fx-border-radius:10;" +
                        "-fx-background-radius:10;" +
                        "-fx-text-fill:" + green() + ";" +
                        "-fx-font-size:13px;" +
                        "-fx-font-weight:bold;" +
                        "-fx-cursor:hand;" +
                        "-fx-padding:12 24 12 24;" +
                        "-fx-min-width:160;";

        String cancelStyle =
                "-fx-background-color:transparent;" +
                        "-fx-border-color:transparent;" +
                        "-fx-text-fill:" + text2 + ";" +
                        "-fx-font-size:12px;" +
                        "-fx-cursor:hand;" +
                        "-fx-padding:6 0 0 0;";

        Button btnWebcam = new Button("Prendre une photo");
        btnWebcam.setGraphic(UiIcons.icon(UiIcons.Icon.CAMERA, text1, 16));
        btnWebcam.setStyle(btnStyle);
        btnWebcam.setOnMouseEntered(e -> btnWebcam.setStyle(btnHoverStyle));
        btnWebcam.setOnMouseExited(e -> btnWebcam.setStyle(btnStyle));
        btnWebcam.setOnAction(e -> {
            useWebcam[0] = true;
            confirmed[0] = true;
            popup.close();
        });

        Button btnFile = new Button("Choisir un fichier");
        btnFile.setGraphic(UiIcons.icon(UiIcons.Icon.FOLDER, text1, 16));
        btnFile.setStyle(btnStyle);
        btnFile.setOnMouseEntered(e -> btnFile.setStyle(btnHoverStyle));
        btnFile.setOnMouseExited(e -> btnFile.setStyle(btnStyle));
        btnFile.setOnAction(e -> {
            useWebcam[0] = false;
            confirmed[0] = true;
            popup.close();
        });

        Button btnCancel = new Button("Annuler");
        btnCancel.setStyle(cancelStyle);
        btnCancel.setOnAction(e -> popup.close());

        HBox btnRow = new HBox(12, btnWebcam, btnFile);
        btnRow.setAlignment(Pos.CENTER);

        root.getChildren().addAll(title, subtitle, btnRow, btnCancel);

        javafx.scene.Scene scene = new javafx.scene.Scene(root);
        scene.setFill(javafx.scene.paint.Color.TRANSPARENT);
        popup.setScene(scene);
        popup.showAndWait();

        // ── After popup closes ────────────────────────────────────────────────
        if (!confirmed[0]) return;

        byte[] imageData = useWebcam[0]
                ? cameraService.captureFromWebcam(owner)
                : cameraService.pickFromFile(owner);

        if (imageData == null) return;
        enterImageSearch(imageData);
    }

    /**
     * Switches both camera buttons to processing style (yellow glow)
     * or back to normal style.
     */
    private void setCameraProcessingStyle(boolean processing) {
        isCameraProcessing = processing;
        for (Button btn : new Button[]{floatingCameraBtn, chatCameraBtn}) {
            if (btn == null) continue;
            if (processing) {
                btn.getStyleClass().remove("camera-btn");
                if (!btn.getStyleClass().contains("camera-btn-processing"))
                    btn.getStyleClass().add("camera-btn-processing");
            } else {
                btn.getStyleClass().remove("camera-btn-processing");
                if (!btn.getStyleClass().contains("camera-btn"))
                    btn.getStyleClass().add("camera-btn");
            }
        }
        refreshActionIcons();
    }

    /**
     * Shows or hides both camera buttons together.
     * Called from state transition methods to enforce the visibility rules.
     */
    private void showCameraButtons(boolean show) {
        for (Button btn : new Button[]{floatingCameraBtn, chatCameraBtn}) {
            if (btn == null) continue;
            btn.setVisible(show);
            btn.setManaged(show);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Conversation routing
    // ─────────────────────────────────────────────────────────────────────────

    private void handleUserInput(String input) {
        if (KeywordExtractor.isResetKeyword(input)) { enterIdle(); return; }
        switch (state) {
            case IDLE:            enterWaitingNeed(); handleNeedInput(input); break;
            case WAITING_NEED:    handleNeedInput(input);   break;
            case WAITING_BUDGET:  handleBudgetInput(input); break;
            case SHOWING_RESULTS: handleRefinement(input);  break;
            case IMAGE_SEARCH:    break; // input disabled during processing, ignore
            default:              enterIdle();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // handleNeedInput - user bubble shown instantly, Gemini async
    // ─────────────────────────────────────────────────────────────────────────

    private void handleNeedInput(String input) {
        appendBubble(input, true);
        setInputEnabled(false);
        showLoading(true);

        Task<GeminiResult> task = new Task<GeminiResult>() {
            @Override
            protected GeminiResult call() {
                List<String> categories = produitSvc.getDistinctCategories();
                return geminiService.analyze(input, categories);
            }
        };

        task.setOnSucceeded(e -> {
            showLoading(false);
            setInputEnabled(true);
            processNeedResult(task.getValue());
        });

        task.setOnFailed(e -> {
            showLoading(false);
            setInputEnabled(true);
            appendBubble("Erreur inattendue. Veuillez réessayer.", false);
        });

        daemonThread(task);
    }

    private void processNeedResult(GeminiResult result) {
        if (!result.isShoppingRequest()) {
            appendBubble(result.getRejectionReason() != null
                    ? result.getRejectionReason() : "Je suis un assistant shopping uniquement.", false);
            appendBubble("Décrivez un produit que vous souhaitez acheter.", false);
            appendBubble("Ex : médicament pour la fièvre ou lait demi-écrémé", false);
            return;
        }
        if (result.getKeywords() == null || result.getKeywords().isEmpty()) {
            appendBubble("Je n'ai pas compris. Pouvez-vous préciser ?", false);
            return;
        }
        if (result.getReformulation() != null && !result.isUsedFallback()) {
            appendBubble("J'ai compris que vous cherchez : " + result.getReformulation(), false);
        } else {
            appendBubble("J'ai compris : " + String.join(", ", result.getKeywords()), false);
        }
        currentKeywords  = result.getKeywords();
        currentCategorie = result.getCategorie();
        enterWaitingBudget();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Budget input
    // ─────────────────────────────────────────────────────────────────────────

    private void handleBudgetInput(String input) {
        appendBubble(input, true);
        if (currentKeywords == null || currentKeywords.isEmpty()) {
            appendBubble("Décrivez d'abord votre besoin.", false);
            enterWaitingNeed();
            return;
        }
        Double budget = KeywordExtractor.parseBudget(input);
        if (budget == null || budget <= 0) {
            appendBubble("Je n'ai pas compris votre budget.", false);
            appendBubble("Entrez un nombre. Ex : \"20\" ou \"50 DT\"", false);
            return;
        }
        currentBudget = budget;
        if (isBudgetNegotiation) {
            isBudgetNegotiation = false;
            refinementBudgetAttempts = 0;
            appendBubble("Nouveau budget : " + budget + " DT. Je relance la recherche...", false);
            runSearchWithExclusions();
        } else {
            appendBubble("Budget : " + budget + " DT. Je recherche...", false);
            runSearch();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // runSearch - DB + scoring on background thread
    // ─────────────────────────────────────────────────────────────────────────

    private void runSearch() {
        if (currentKeywords == null || currentKeywords.isEmpty()) {
            appendBubble("Décrivez votre besoin pour lancer une recherche.", false);
            enterWaitingNeed();
            return;
        }

        setInputEnabled(false);
        showLoading(true);

        final List<String> kwSnap     = List.copyOf(currentKeywords);
        final String       catSnap    = currentCategorie;
        final double       budgetSnap = currentBudget;
        final int          idAcheteur = SessionManager.getCurrentUserId();

        Task<List<ScoringService.ScoredProduct>> task =
                new Task<List<ScoringService.ScoredProduct>>() {
                    @Override
                    protected List<ScoringService.ScoredProduct> call() {
                        List<Produit> allProducts = produitSvc.getAllInStock();
                        if (allProducts.isEmpty()) return null;
                        return scoringSvc.score(kwSnap, catSnap, budgetSnap, allProducts, idAcheteur, activePromoMap);
                    }
                };

        task.setOnSucceeded(e -> {
            showLoading(false);
            setInputEnabled(true);
            List<ScoringService.ScoredProduct> scored = task.getValue();

            if (scored == null) {
                appendBubble("Le catalogue est actuellement vide. Revenez plus tard.", false);
                enterIdle();
                return;
            }
            if (scored.isEmpty()) {
                appendBubble("Aucun produit trouvé pour : " + String.join(", ", kwSnap), false);
                appendBubble("Essayez d'élargir votre budget ou changez vos mots clés.", false);
                iaService.add(new HistoriqueIA(idAcheteur, String.join(" ", kwSnap), null));
                enterWaitingNeed();
                return;
            }

            originalResults = new ArrayList<>(scored);
            currentResults  = new ArrayList<>(originalResults);
            boolean allOver = currentResults.stream().allMatch(r -> r.overBudget);
            appendBubble(allOver
                    ? "Les produits trouvés dépassent votre budget de " + budgetSnap + " DT. Voici les plus proches :"
                    : "J'ai trouvé " + currentResults.size() + " produit(s) pour vous :", false);
            iaService.add(new HistoriqueIA(idAcheteur, String.join(" ", kwSnap),
                    currentResults.get(0).produit.getIdProduit()));
            enterShowingResults();
        });

        task.setOnFailed(e -> {
            showLoading(false);
            setInputEnabled(true);
            appendBubble("Erreur lors de la recherche. Veuillez réessayer.", false);
            enterWaitingNeed();
        });

        daemonThread(task);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // runSearchWithExclusions - also async
    // ─────────────────────────────────────────────────────────────────────────

    private void runSearchWithExclusions() {
        if (currentKeywords == null || currentKeywords.isEmpty()) {
            appendBubble("Aucun mot-clé disponible pour relancer la recherche.", false);
            enterWaitingNeed();
            return;
        }

        setInputEnabled(false);
        showLoading(true);

        final List<String>  kwSnap     = List.copyOf(currentKeywords);
        final String        catSnap    = currentCategorie;
        final double        budgetSnap = currentBudget;
        final int           idAcheteur = SessionManager.getCurrentUserId();
        final Set<Integer>  excluded   = new HashSet<>(excludedProductIds);

        Task<List<ScoringService.ScoredProduct>> task =
                new Task<List<ScoringService.ScoredProduct>>() {
                    @Override
                    protected List<ScoringService.ScoredProduct> call() {
                        List<Produit> allProducts = produitSvc.getAllInStock().stream()
                                .filter(p -> !excluded.contains(p.getIdProduit()))
                                .collect(Collectors.toList());
                        if (allProducts.isEmpty()) return null;
                        return scoringSvc.score(kwSnap, catSnap, budgetSnap, allProducts, idAcheteur, activePromoMap);
                    }
                };

        task.setOnSucceeded(e -> {
            showLoading(false);
            setInputEnabled(true);
            List<ScoringService.ScoredProduct> scored = task.getValue();

            if (scored == null) {
                appendBubble("Aucun produit disponible avec ces critères.", false);
                appendBubble("Tapez un nouveau besoin ou \"recommencer\".", false);
                enterWaitingNeed();
                return;
            }
            if (scored.isEmpty()) {
                appendBubble("Aucun résultat avec les critères actuels.", false);
                appendBubble("Retour aux résultats précédents.", false);
                if (originalResults != null && !originalResults.isEmpty()) {
                    currentResults = new ArrayList<>(originalResults);
                    buildResultCards();
                } else {
                    enterWaitingNeed();
                }
                return;
            }

            originalResults = new ArrayList<>(scored);
            currentResults  = new ArrayList<>(originalResults);
            appendBubble("Voici les nouveaux résultats (" + currentResults.size() + " produit(s)) :", false);

            // ── Silent Update: if previous search had no result (null row),
            //    fill it with the top product found after refinement ──────────
            final int topProductId = currentResults.get(0).produit.getIdProduit();
            Thread updateThread = new Thread(() ->
                    iaService.updateProduitSuggere(idAcheteur, topProductId), "historique-update");
            updateThread.setDaemon(true);
            updateThread.start();

            enterShowingResults();
        });

        task.setOnFailed(e -> {
            showLoading(false);
            setInputEnabled(true);
            appendBubble("Erreur lors de la recherche. Veuillez réessayer.", false);
            enterWaitingNeed();
        });

        daemonThread(task);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Spinner + input lock helpers
    // ─────────────────────────────────────────────────────────────────────────

    private void showLoading(boolean loading) {
        if (progressIndicator != null) progressIndicator.setVisible(loading);
    }

    private void setInputEnabled(boolean enabled) {
        if (inputField != null) inputField.setDisable(!enabled);
        if (sendButton != null) sendButton.setDisable(!enabled);
    }

    private void daemonThread(Task<?> task) {
        Thread t = new Thread(task);
        t.setDaemon(true);
        t.start();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // State transitions
    // ─────────────────────────────────────────────────────────────────────────

    private void enterIdle() {
        state = ConversationState.IDLE;
        showingImageResults = false;
        currentKeywords = null; currentCategorie = null;
        currentBudget   = 0;   currentResults   = null;
        originalResults = null;
        excludedProductIds.clear();
        refinementBudgetAttempts = 0;
        isBudgetNegotiation = false;
        isRecording = false;
        setMicRecordingStyle(false);

        closeDetail();
        stopBubbleQueue();
        showLoading(false);
        setInputEnabled(true);

        if (stateBadge   != null) stateBadge.setText("IDLE");
        if (contextLabel != null) contextLabel.setText("●  Bonjour ! Décrivez votre besoin ci-dessous.");
        if (inputField   != null) inputField.setPromptText("Ex : médicament pour la fièvre…");

        User user = SessionManager.getCurrentUser();
        String prenom = (user != null && user.getPrenom() != null) ? " " + user.getPrenom() : "";
        if (welcomeLabel != null) welcomeLabel.setText("Bonjour" + prenom + " ! Que recherchez-vous aujourd'hui ?");

        if (chatBubbleBox != null) chatBubbleBox.getChildren().clear();
        showSection(Section.IDLE);
        loadIdleCards();

        showFloatingBar(true);
        showChatBar(false);
        showCameraButtons(true);
        activateSidebarTab("explore");
        loadOfferCards();
    }

    private void enterWaitingNeed() {
        state = ConversationState.WAITING_NEED;
        showingImageResults = false;
        closeDetail();
        if (stateBadge   != null) stateBadge.setText("NEED");
        if (contextLabel != null) contextLabel.setText("Décrivez votre besoin en français.");
        if (inputField   != null) inputField.setPromptText("Ex : médicament pour la fièvre…");
        showSection(Section.CHAT);
        showFloatingBar(false);
        showChatBar(true);
        showCameraButtons(true);
        activateSidebarTab("assistant");
    }

    private void enterWaitingBudget() {
        state = ConversationState.WAITING_BUDGET;
        showingImageResults = false;
        if (stateBadge   != null) stateBadge.setText("BUDGET");
        if (contextLabel != null) contextLabel.setText("Quel est votre budget ?");
        if (inputField   != null) inputField.setPromptText("Ex : 20 ou 50 DT");
        showSection(Section.CHAT);
        appendBubble("Quel est votre budget en DT ?", false);
        showFloatingBar(false);
        showChatBar(true);
        showCameraButtons(false);
        activateSidebarTab("assistant");
    }

    private void enterShowingResults() {
        if (currentResults == null || currentResults.isEmpty()) {
            appendBubble("Aucun résultat à afficher.", false);
            enterWaitingNeed();
            return;
        }
        state = ConversationState.SHOWING_RESULTS;
        showingImageResults = false;
        if (stateBadge   != null) stateBadge.setText("RESULTS");
        if (contextLabel != null) contextLabel.setText("Résultats - affinez ou tapez un nouveau besoin.");
        if (inputField   != null) inputField.setPromptText("Ex : moins cher / mieux noté / pas ça...");
        showSection(Section.CHAT_AND_RESULTS);
        showCameraButtons(true);
        buildResultCards();

        final int id = SessionManager.getCurrentUserId();
        final List<ScoringService.ScoredProduct> snap = List.copyOf(currentResults);
        Thread log = new Thread(() -> {
            for (ScoringService.ScoredProduct sp : snap)
                if (sp != null && sp.produit != null)
                    interactionService.logView(id, sp.produit.getIdProduit());
        });
        log.setDaemon(true);
        log.start();
    }
    // ─────────────────────────────────────────────────────────────────────────
    // Image search state - Step 8c
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Transitions to IMAGE_SEARCH state.
     * Called after the user provides an image (webcam or file).
     * Switches to AI Chat tab, shows the chat bar, appends the
     * "Photo reçue" bubble, then calls Gemini analyzeImage() async.
     */
    private void enterImageSearch(byte[] imageData) {
        state = ConversationState.IMAGE_SEARCH;
        showingImageResults = false;
        closeDetail();

        // Reset search state - image search is a fresh start
        currentKeywords  = null;
        currentCategorie = null;
        currentBudget    = 0;
        originalResults  = null;
        currentResults   = null;
        excludedProductIds.clear();
        refinementBudgetAttempts = 0;
        isBudgetNegotiation      = false;

        if (stateBadge   != null) stateBadge.setText("IMG");
        if (contextLabel != null) contextLabel.setText("Analyse de l'image en cours...");

        // If coming from IDLE, switch to AI Chat tab and show chat bar
        showSection(Section.CHAT);
        showFloatingBar(false);
        showChatBar(true);
        activateSidebarTab("assistant");

        // Lock camera + input while processing
        setCameraProcessingStyle(true);
        setInputEnabled(false);
        showLoading(true);

        appendBubble("Photo reçue, analyse en cours...", false);

        final int idAcheteur = SessionManager.getCurrentUserId();

        Task<ImageRecognitionResult> task = new Task<ImageRecognitionResult>() {
            @Override
            protected ImageRecognitionResult call() {
                List<String> categories = produitSvc.getDistinctCategories();
                return geminiService.analyzeImage(imageData, categories);
            }
        };

        task.setOnSucceeded(e -> {
            showLoading(false);
            setInputEnabled(true);
            setCameraProcessingStyle(false);
            handleImageResult(task.getValue(), idAcheteur);
        });

        task.setOnFailed(e -> {
            showLoading(false);
            setInputEnabled(true);
            setCameraProcessingStyle(false);
            appendBubble("Erreur inattendue lors de l'analyse. Veuillez réessayer.", false);
            enterWaitingNeed();
        });

        daemonThread(task);
    }

    /**
     * Handles the ImageRecognitionResult returned by Gemini.
     *
     * If identification failed → shows failure reason, returns to WAITING_NEED.
     * If successful → shows product name bubble, runs scoreForImage() async,
     * then displays image result cards.
     */
    private void handleImageResult(ImageRecognitionResult result, int idAcheteur) {
        if (!result.isIdentified()) {
            appendBubble(result.getFailureReason() != null
                    ? result.getFailureReason()
                    : "Je n'ai pas pu identifier ce produit.", false);
            appendBubble("Essayez avec une photo plus nette ou un angle différent.", false);
            enterWaitingNeed();
            return;
        }

        // Build identification bubble
        String productLine = "J'ai identifié : " + result.getProductName();
        if (result.getBrand() != null) productLine += " (" + result.getBrand() + ")";
        appendBubble(productLine, false);
        appendBubble("Je cherche des produits similaires...", false);

        if (contextLabel != null)
            contextLabel.setText("Produit identifié - recherche en cours...");

        final List<String> kwSnap  = List.copyOf(result.getKeywords());
        final String       catSnap = result.getCategorie();

        setInputEnabled(false);
        showLoading(true);

        Task<List<ScoringService.ScoredProduct>> task =
                new Task<List<ScoringService.ScoredProduct>>() {
                    @Override
                    protected List<ScoringService.ScoredProduct> call() {
                        List<Produit> allProducts = produitSvc.getAllInStock();
                        if (allProducts.isEmpty()) return null;
                        return scoringSvc.scoreForImage(kwSnap, catSnap, allProducts, idAcheteur);
                    }
                };

        task.setOnSucceeded(e -> {
            showLoading(false);
            setInputEnabled(true);

            List<ScoringService.ScoredProduct> scored = task.getValue();

            if (scored == null) {
                appendBubble("Le catalogue est actuellement vide. Revenez plus tard.", false);
                enterIdle();
                return;
            }
            if (scored.isEmpty()) {
                appendBubble("Aucun produit similaire trouvé dans notre catalogue.", false);
                appendBubble("Tapez un besoin ou utilisez à nouveau la caméra.", false);
                enterWaitingNeed();
                return;
            }

            originalResults = new ArrayList<>(scored);
            currentResults  = new ArrayList<>(originalResults);

            appendBubble("J'ai trouvé " + currentResults.size()
                    + " produit(s) similaire(s) :", false);

            // Log to history - no budget so produit_suggere_id = top result
            iaService.add(new HistoriqueIA(idAcheteur,
                    String.join(" ", kwSnap),
                    currentResults.get(0).produit.getIdProduit()));

            enterShowingImageResults();
        });

        task.setOnFailed(e -> {
            showLoading(false);
            setInputEnabled(true);
            appendBubble("Erreur lors de la recherche. Veuillez réessayer.", false);
            enterWaitingNeed();
        });

        daemonThread(task);
    }

    /**
     * Final state after image search - similar to enterShowingResults()
     * but without budget colouring logic.
     * Camera button stays visible so user can do another image search.
     */
    private void enterShowingImageResults() {
        if (currentResults == null || currentResults.isEmpty()) {
            appendBubble("Aucun résultat à afficher.", false);
            enterWaitingNeed();
            return;
        }

        state = ConversationState.SHOWING_RESULTS;
        showingImageResults = true;
        if (stateBadge   != null) stateBadge.setText("RESULTS");
        if (contextLabel != null) contextLabel.setText("Résultats image - tapez un besoin ou prenez une autre photo.");
        if (inputField   != null) inputField.setPromptText("Tapez un besoin ou utilisez la caméra...");

        showSection(Section.CHAT_AND_RESULTS);
        buildImageResultCards();

        // Log VIEW interactions on background thread
        final int id = SessionManager.getCurrentUserId();
        final List<ScoringService.ScoredProduct> snap = List.copyOf(currentResults);
        Thread log = new Thread(() -> {
            for (ScoringService.ScoredProduct sp : snap)
                if (sp != null && sp.produit != null)
                    interactionService.logView(id, sp.produit.getIdProduit());
        });
        log.setDaemon(true);
        log.start();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Result refinement
    // ─────────────────────────────────────────────────────────────────────────

    private void handleRefinement(String input) {
        List<String> categories = produitSvc.getDistinctCategories();
        RefinementIntent intent = RefinementDetector.detect(input, categories);

        if (intent.has(RefinementType.NEW_SEARCH)) {
            closeDetail();
            setInputEnabled(false);
            appendBubble("Lancement d'une nouvelle recherche...", false);
            enterWaitingNeed();
            // Wait for the typing animation to finish before starting the new search.
            // "Lancement d'une nouvelle recherche..." is 38 chars × 22ms = ~836ms.
            String msg = "Lancement d'une nouvelle recherche...";
            long delayMs = msg.length() * 22L + 100; // +100ms buffer
            javafx.animation.PauseTransition pause =
                    new javafx.animation.PauseTransition(
                            javafx.util.Duration.millis(delayMs));
            pause.setOnFinished(e -> {
                setInputEnabled(true);
                handleNeedInput(input);
            });
            pause.play();
            return;
        }

        appendBubble(input, true);
        applyRefinements(intent);
    }

    private void applyRefinements(RefinementIntent intent) {
        if (originalResults == null || originalResults.isEmpty()) {
            if (currentResults == null || currentResults.isEmpty()) {
                appendBubble("Aucun résultat à affiner.", false);
                enterWaitingNeed();
                return;
            }
            originalResults = new ArrayList<>(currentResults);
        }

        boolean needsRerun = false;
        List<ScoringService.ScoredProduct> working = new ArrayList<>(originalResults);

        if (intent.has(RefinementType.EXCLUDE_TOP)) {
            if (currentResults != null && !currentResults.isEmpty()) {
                int topId = currentResults.get(0).produit.getIdProduit();
                excludedProductIds.add(topId);
                working.removeIf(sp -> excludedProductIds.contains(sp.produit.getIdProduit()));
                appendBubble("Produit ignoré. Je recherche d'autres options...", false);
                needsRerun = true;
            }
        }

        if (intent.has(RefinementType.CATEGORY_FILTER)) {
            String category = intent.getParameter(RefinementType.CATEGORY_FILTER);
            if (category != null) {
                working = filterByCategory(working, category);
                appendBubble("Filtrage par catégorie : " + category, false);
            }
        }

        if (intent.has(RefinementType.PRICE_UP)) {
            currentBudget *= 1.3;
            refinementBudgetAttempts = 0;
            appendBubble("Budget élargi à " + String.format("%.0f", currentBudget) + " DT. Je relance...", false);
            needsRerun = true;
        }

        if (intent.has(RefinementType.PRICE_DOWN) && !intent.has(RefinementType.PRICE_UP)) {
            boolean hasInBudget = working.stream().anyMatch(sp -> !sp.overBudget);
            if (hasInBudget) {
                working.sort(Comparator.comparingDouble(sp -> sp.produit.getPrix()));
                appendBubble("Voici les options les moins chères dans votre budget :", false);
            } else if (refinementBudgetAttempts == 0) {
                currentBudget *= 0.75;
                refinementBudgetAttempts++;
                appendBubble("Recherche avec un budget réduit à "
                        + String.format("%.0f", currentBudget) + " DT...", false);
                needsRerun = true;
            } else {
                isBudgetNegotiation = true;
                state = ConversationState.WAITING_BUDGET;
                if (stateBadge   != null) stateBadge.setText("BUDGET");
                if (inputField   != null) inputField.setPromptText("Ex : 30 ou 50 DT");
                if (contextLabel != null) contextLabel.setText("Entrez votre nouveau budget.");
                appendBubble("Quel budget souhaitez-vous définir ?", false);
                return;
            }
        }

        if (intent.has(RefinementType.SORT_RATING)) {
            working.sort((a, b) -> Double.compare(productRating(b), productRating(a)));
            appendBubble("Résultats triés par note.", false);
        }

        if (needsRerun) {
            runSearchWithExclusions();
            return;
        }

        if (working.isEmpty()) {
            appendBubble("Aucun résultat avec ces critères.", false);
            appendBubble("Retour aux résultats précédents.", false);
            currentResults = new ArrayList<>(originalResults);
        } else {
            currentResults = working;
        }

        buildResultCards();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Section visibility
    // ─────────────────────────────────────────────────────────────────────────

    private enum Section { IDLE, CHAT, CHAT_AND_RESULTS }

    private void showSection(Section s) {
        setVisible(idleSections,   s == Section.IDLE);
        setVisible(chatSection,    s == Section.CHAT || s == Section.CHAT_AND_RESULTS);
        setVisible(resultsSection, s == Section.CHAT_AND_RESULTS);
    }

    private void setVisible(VBox node, boolean v) {
        if (node == null) return;
        node.setVisible(v);
        node.setManaged(v);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Sidebar helpers
    // ─────────────────────────────────────────────────────────────────────────

    private void activateSidebarTab(String tab) {
        activeSidebarTab = tab;
        if (btnExplore == null || btnAssistant == null) return;
        final String A = "sidebar-tab-active";
        btnExplore.getStyleClass().remove(A);
        btnAssistant.getStyleClass().remove(A);
        if ("explore".equals(tab)) btnExplore.getStyleClass().add(A);
        else                       btnAssistant.getStyleClass().add(A);
    }

    private void fadeContent(Runnable action) {
        if (mainContent == null) { action.run(); return; }
        FadeTransition out = new FadeTransition(Duration.millis(140), mainContent);
        out.setToValue(0);
        out.setInterpolator(Interpolator.EASE_IN);
        out.setOnFinished(e -> {
            action.run();
            FadeTransition in = new FadeTransition(Duration.millis(180), mainContent);
            in.setToValue(1);
            in.setInterpolator(Interpolator.EASE_OUT);
            in.play();
        });
        out.play();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Bar show/hide + floating bar animation
    // ─────────────────────────────────────────────────────────────────────────

    private void showFloatingBar(boolean show) {
        if (floatingBar == null) return;
        if (show) {
            floatingBarExpanded = false;
            floatingBar.setTranslateY(0);
            floatingBar.setScaleX(1.0);
            floatingBar.setOpacity(0);
            floatingBar.setVisible(true);
            floatingBar.setManaged(true);
            FadeTransition      ft = new FadeTransition(Duration.millis(300), floatingBar);
            TranslateTransition tt = new TranslateTransition(Duration.millis(300), floatingBar);
            ft.setToValue(1); ft.setInterpolator(Interpolator.EASE_OUT);
            tt.setFromY(18);  tt.setToY(0); tt.setInterpolator(Interpolator.EASE_OUT);
            new ParallelTransition(ft, tt).play();
        } else {
            floatingBar.setVisible(false);
            floatingBar.setManaged(false);
            floatingBar.setOpacity(1);
            floatingBar.setTranslateY(0);
            floatingBar.setScaleX(1.0);
        }
    }

    private void showChatBar(boolean show) {
        if (chatBarHBox == null) return;
        chatBarHBox.setVisible(show);
        chatBarHBox.setManaged(show);
    }

    private void expandFloatingBar() {
        if (floatingBarExpanded || floatingBar == null || mainArea == null) return;
        floatingBarExpanded = true;

        double stackH  = mainArea.getHeight();
        double barH    = 58;
        double bottomM = 24;
        double toY     = -(stackH / 2.0 - bottomM - barH / 2.0);

        TranslateTransition tt = new TranslateTransition(Duration.millis(300), floatingBar);
        tt.setToY(toY);
        tt.setInterpolator(Interpolator.EASE_OUT);

        ScaleTransition st = new ScaleTransition(Duration.millis(300), floatingBar);
        st.setToX(1.14);
        st.setInterpolator(Interpolator.EASE_OUT);

        new ParallelTransition(tt, st).play();
        Platform.runLater(() -> floatingInput.requestFocus());
    }

    /**
     * Animates the floating bar back to its resting position at the bottom
     * without hiding it. Called when the user clicks outside after expanding,
     * so they can dismiss it without submitting anything.
     */
    private void shrinkFloatingBar() {
        if (!floatingBarExpanded || floatingBar == null) return;
        floatingBarExpanded = false;

        TranslateTransition tt = new TranslateTransition(Duration.millis(250), floatingBar);
        tt.setToY(0);
        tt.setInterpolator(Interpolator.EASE_IN);

        ScaleTransition st = new ScaleTransition(Duration.millis(250), floatingBar);
        st.setToX(1.0);
        st.setInterpolator(Interpolator.EASE_IN);

        new ParallelTransition(tt, st).play();
        if (floatingInput != null) floatingInput.clear();
    }

    private void collapseFloatingBar(Runnable onDone) {
        if (floatingBar == null) { if (onDone != null) onDone.run(); return; }

        TranslateTransition tt = new TranslateTransition(Duration.millis(200), floatingBar);
        tt.setToY(0);
        tt.setInterpolator(Interpolator.EASE_IN);

        ScaleTransition st = new ScaleTransition(Duration.millis(200), floatingBar);
        st.setToX(1.0);
        st.setInterpolator(Interpolator.EASE_IN);

        FadeTransition ft = new FadeTransition(Duration.millis(200), floatingBar);
        ft.setToValue(0);

        ParallelTransition pt = new ParallelTransition(tt, st, ft);
        pt.setOnFinished(e -> {
            floatingBarExpanded = false;
            floatingBar.setVisible(false);
            floatingBar.setManaged(false);
            floatingBar.setOpacity(1);
            floatingBar.setTranslateY(0);
            floatingBar.setScaleX(1.0);
            if (onDone != null) onDone.run();
        });
        pt.play();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Idle card loading
    // ─────────────────────────────────────────────────────────────────────────

    private void loadIdleCards() {
        clearRow(lastRow1); clearRow(lastRow2); clearRow(lastRow3);
        clearRow(topRow1);  clearRow(topRow2);  clearRow(topRow3);
        if (lastExpandRows != null) { lastExpandRows.setVisible(false); lastExpandRows.setManaged(false); }
        if (topExpandRows  != null) { topExpandRows.setVisible(false);  topExpandRows.setManaged(false);  }
        if (showMoreLastBtn != null) showMoreLastBtn.setText("Show more ↓");
        if (showMoreTopBtn  != null) showMoreTopBtn.setText("Show more ↓");

        int id = SessionManager.getCurrentUserId();
        List<String>  freq = iaService.getCategoriesFromHistoryByFrequency(id);
        List<Produit> last = freq.isEmpty()
                ? produitSvc.getTopRated()
                : produitSvc.getIdleRecommendations(id, freq);
        distributeToRows(last,                     lastRow1, lastRow2, lastRow3, showMoreLastBtn);
        distributeToRows(produitSvc.getTopRated(), topRow1,  topRow2,  topRow3,  showMoreTopBtn);    }

    private void clearRow(HBox row) {
        if (row == null) return;
        for (javafx.scene.Node n : row.getChildren()) {
            if (n instanceof VBox v) {
                v.prefWidthProperty().unbind();
                v.maxWidthProperty().unbind();
            }
        }
        row.getChildren().clear();
    }

    private void distributeToRows(List<Produit> products,
                                  HBox r1, HBox r2, HBox r3,
                                  Button showMoreBtn) {
        int visibleFirstRow = 6;
        int maxProducts     = 18;
        int total = products != null ? Math.min(products.size(), maxProducts) : 0;
        boolean hasMore = total > visibleFirstRow;
        if (showMoreBtn != null) { showMoreBtn.setVisible(hasMore); showMoreBtn.setManaged(hasMore); }

        for (int i = 0; i < total; i++) {
            VBox card = buildIdleCard(products.get(i));
            if      (i < visibleFirstRow)     { if (r1 != null) r1.getChildren().add(card); }
            else if (i < visibleFirstRow * 2) { if (r2 != null) r2.getChildren().add(card); }
            else                              { if (r3 != null) r3.getChildren().add(card); }
        }
        // Bind to 1/6 slot width so all cards match size regardless of count
        bindFixedSlotWidth(r1);
        bindFixedSlotWidth(r2);
        bindFixedSlotWidth(r3);
    }

    /**
     * Binds every card in the row to exactly 1/6 of available row width,
     * regardless of how many cards are in the row.
     * 3 cards stay the same size as 6 — empty space fills to the right.
     * Called via Platform.runLater so the row has a real width before binding.
     */
    private void bindFixedSlotWidth(HBox row) {
        if (row == null || row.getChildren().isEmpty()) return;
        // Unbind first in case this row is being reused
        for (javafx.scene.Node n : row.getChildren()) {
            if (n instanceof VBox v) {
                v.prefWidthProperty().unbind();
                v.maxWidthProperty().unbind();
            }
        }
        Platform.runLater(() -> {
            for (javafx.scene.Node node : row.getChildren()) {
                if (!(node instanceof VBox card)) continue;
                card.prefWidthProperty().bind(
                        row.widthProperty()
                                .subtract(32)   // 16px padding × 2
                                .subtract(50)   // 5 gaps × 10px (max for 6 cards)
                                .divide(6)      // always divide into 6 equal slots
                );
                card.maxWidthProperty().bind(card.prefWidthProperty());
            }
        });
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Offers section (Réductions & Offres)
    // ─────────────────────────────────────────────────────────────────────────

    private void loadOfferCards() {
        if (offersPlaceholder == null) return;

        // Clear rows
        clearRow(offerRow1); clearRow(offerRow2); clearRow(offerRow3);
        if (offerExpandRows != null) { offerExpandRows.setVisible(false); offerExpandRows.setManaged(false); }
        if (showMoreOffersBtn != null) showMoreOffersBtn.setText("Show more ↓");

        if (activeConseils == null || activeConseils.isEmpty()) {
            offersPlaceholder.setVisible(true);
            offersPlaceholder.setManaged(true);
            if (offerRow1 != null) { offerRow1.setVisible(false); offerRow1.setManaged(false); }
            if (showMoreOffersBtn != null) { showMoreOffersBtn.setVisible(false); showMoreOffersBtn.setManaged(false); }
            return;
        }

        offersPlaceholder.setVisible(false);
        offersPlaceholder.setManaged(false);
        if (offerRow1 != null) { offerRow1.setVisible(true); offerRow1.setManaged(true); }

        int visibleFirstRow = 6;
        int total           = Math.min(activeConseils.size(), 18);
        boolean hasMore     = total > visibleFirstRow;

        if (showMoreOffersBtn != null) {
            showMoreOffersBtn.setVisible(hasMore);
            showMoreOffersBtn.setManaged(hasMore);
        }

        for (int i = 0; i < total; i++) {
            ConseilPromo conseil = activeConseils.get(i);
            VBox card = "Promotion".equals(conseil.getType())
                    ? buildPromotionOfferCard(conseil)
                    : buildBundleOfferCard(conseil);

            if      (i < visibleFirstRow)     { if (offerRow1 != null) offerRow1.getChildren().add(card); }
            else if (i < visibleFirstRow * 2) { if (offerRow2 != null) offerRow2.getChildren().add(card); }
            else                              { if (offerRow3 != null) offerRow3.getChildren().add(card); }
        }
        // Bind to 1/6 slot width so offer cards match idle card size
        bindFixedSlotWidth(offerRow1);
        bindFixedSlotWidth(offerRow2);
        bindFixedSlotWidth(offerRow3);
    }

    /**
     * Promotion offer card — identical structure to buildIdleCard with:
     *  - discount badge next to category badge
     *  - product name visible and prominent
     *  - old price with strikethrough
     *  - green effective price
     *  - savings label
     */
    private VBox buildPromotionOfferCard(ConseilPromo conseil) {
        Produit p              = conseil.getProduits().get(0);
        double  originalPrice  = conseil.getOriginalPrice();
        double  effectivePrice = conseil.getEffectivePrice();
        double  savings        = originalPrice - effectivePrice;

        VBox card = new VBox(0);
        card.setMinWidth(0);
        card.setMaxWidth(Double.MAX_VALUE); // overridden by bindFixedSlotWidth
        HBox.setHgrow(card, Priority.NEVER);
        card.setStyle(cardStyle(green()));

        // ── Image zone — inset, 4-sided radius ────────────────────────────────
        VBox imgWrapper = buildImageWrapper(card, p, 20);

        // ── Content zone ──────────────────────────────────────────────────────
        VBox content = new VBox(4);
        content.setPadding(new Insets(8, 8, 8, 8));
        VBox.setVgrow(content, Priority.ALWAYS);

        // Category badge + discount badge
        Label catBadge = new Label(categoryBadge(p.getCategorie()));
        catBadge.setPadding(new Insets(1, 6, 1, 6));
        catBadge.setStyle("-fx-font-size:9px;-fx-font-weight:bold;"
                + "-fx-background-color:" + categoryBadgeColor(p.getCategorie()) + ";"
                + "-fx-text-fill:white;-fx-background-radius:20;");

        Label discBadge = new Label("-" + (int) conseil.getDiscount() + "% 🏷");
        discBadge.setPadding(new Insets(1, 6, 1, 6));
        discBadge.setStyle("-fx-font-size:9px;-fx-font-weight:bold;"
                + "-fx-background-color:linear-gradient(to right,#10B981,#14B8A6);"
                + "-fx-text-fill:white;-fx-background-radius:20;");

        HBox badgeRow = new HBox(4, catBadge, discBadge);
        badgeRow.setAlignment(Pos.CENTER_LEFT);

        // Buyer-facing title (from M7 AI)
        Label titre = new Label(conseil.getTitreAcheteur());
        titre.setWrapText(true);
        titre.setMinWidth(0);
        titre.setMaxWidth(Double.MAX_VALUE);
        titre.setMaxHeight(28);
        titre.setStyle("-fx-font-size:10px;-fx-text-fill:" + textMuted() + ";");

        // Product name — prominent, larger font, explicit fill so it's always visible
        Label name = new Label(safeText(p.getLibelle(), "Produit sans nom"));
        name.setWrapText(true);
        name.setMinWidth(0);
        name.setMaxWidth(Double.MAX_VALUE);
        name.setMaxHeight(36);
        name.setStyle("-fx-font-weight:bold;-fx-font-size:13px;-fx-text-fill:" + textPrimary() + ";");

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        // Old price with strikethrough
        Label oldPrice = new Label(String.format("%.2f DT", originalPrice));
        oldPrice.setStyle("-fx-font-size:10px;-fx-text-fill:" + textMuted()
                + ";-fx-strikethrough:true;");

        // New effective price
        Label newPrice = new Label(String.format("%.2f DT", effectivePrice));
        newPrice.setStyle("-fx-font-weight:bold;-fx-font-size:13px;-fx-text-fill:" + green() + ";");

        // Cart button — full width like idle card
        Button addBtn = new Button("+ Cart");
        addBtn.setMaxWidth(Double.MAX_VALUE);
        addBtn.setPrefHeight(24);
        addBtn.setStyle("-fx-background-color:linear-gradient(to bottom right,#2563EB,#8B5CF6);"
                + "-fx-background-radius:6;-fx-text-fill:white;"
                + "-fx-font-size:10px;-fx-font-weight:bold;-fx-cursor:hand;-fx-padding:0;");
        addBtn.setOnMouseClicked(e -> e.consume());
        addBtn.setOnAction(e -> { e.consume(); logAddToCart(p); });

        // Savings label
        Label savingsLabel = new Label(String.format("✓ Économisez %.2f DT", savings));
        savingsLabel.setStyle("-fx-font-size:9px;-fx-font-weight:bold;-fx-text-fill:" + green() + ";");

        // Bottom row: expiration
        String expText = conseil.getDateExpiration() != null
                ? "Expire " + conseil.getDateExpiration()
                .format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yy"))
                : "";
        Label expLabel = new Label(expText);
        expLabel.setStyle("-fx-font-size:9px;-fx-text-fill:" + textMuted() + ";");

        content.getChildren().addAll(badgeRow, titre, name, spacer, oldPrice, newPrice, addBtn, savingsLabel, expLabel);
        card.getChildren().addAll(imgWrapper, content);
        attachHoverEffect(card, green(), green());
        card.setOnMouseClicked(e -> logProductView(p));
        return card;
    }

    /**
     * Bundle offer card — same row sizing as idle cards with:
     *  - gradient header showing bundle icon + product names
     *  - buyer title
     *  - product list in content
     *  - combined old/new price + savings
     */
    private VBox buildBundleOfferCard(ConseilPromo conseil) {
        double originalPrice  = conseil.getOriginalPrice();
        double effectivePrice = conseil.getEffectivePrice();
        double savings        = originalPrice - effectivePrice;

        VBox card = new VBox(0);
        card.setMinWidth(0);
        card.setMaxWidth(Double.MAX_VALUE); // overridden by bindFixedSlotWidth
        HBox.setHgrow(card, Priority.NEVER);
        card.setStyle(cardStyle(accent()));

        // ── Bundle header — inset 8px, 4-sided radius, height tracks card width like normal cards ──
        VBox header = new VBox(4);
        header.setAlignment(Pos.CENTER);
        header.setMaxWidth(Double.MAX_VALUE);
        header.setStyle("-fx-background-color:linear-gradient(to bottom right,#2563EB,#8B5CF6);"
                + "-fx-background-radius:12;");

        // Wrap header with same 8px inset as normal image tiles
        VBox headerWrapper = new VBox(header);
        headerWrapper.setPadding(new Insets(8, 8, 0, 8));
        headerWrapper.setMaxWidth(Double.MAX_VALUE);

        // Height = 65% of usable width (card width minus 16px L+R padding) — same formula as buildImageWrapper
        card.widthProperty().addListener((obs, o, w) -> {
            double h = (w.doubleValue() - 16) * 0.65;
            header.setPrefHeight(h);
            header.setMinHeight(h);
            header.setMaxHeight(h);
        });

        Label bundleIcon = new Label("📦");
        bundleIcon.setStyle("-fx-font-size:26px;");

        // Product names in the header
        String productNames = conseil.getProduits().stream()
                .map(p -> safeText(p.getLibelle(), "Produit"))
                .collect(java.util.stream.Collectors.joining("  +  "));
        Label namesInHeader = new Label(productNames);
        namesInHeader.setWrapText(true);
        namesInHeader.setMinWidth(0);
        namesInHeader.setMaxWidth(Double.MAX_VALUE);
        namesInHeader.setPadding(new Insets(0, 10, 0, 10));
        namesInHeader.setStyle("-fx-font-size:9px;-fx-text-fill:rgba(255,255,255,0.85);"
                + "-fx-text-alignment:center;");

        header.getChildren().addAll(bundleIcon, namesInHeader);

        // ── Content zone ──────────────────────────────────────────────────────
        VBox content = new VBox(4);
        content.setPadding(new Insets(8, 8, 8, 8));
        VBox.setVgrow(content, Priority.ALWAYS);

        // Bundle + discount badges
        Label bundleBadge = new Label("📦 Bundle");
        bundleBadge.setPadding(new Insets(1, 6, 1, 6));
        bundleBadge.setStyle("-fx-font-size:9px;-fx-font-weight:bold;"
                + "-fx-background-color:linear-gradient(to right,#2563EB,#8B5CF6);"
                + "-fx-text-fill:white;-fx-background-radius:20;");

        Label discBadge = new Label("-" + (int) conseil.getDiscount() + "%");
        discBadge.setPadding(new Insets(1, 6, 1, 6));
        discBadge.setStyle("-fx-font-size:9px;-fx-font-weight:bold;"
                + "-fx-background-color:linear-gradient(to right,#10B981,#14B8A6);"
                + "-fx-text-fill:white;-fx-background-radius:20;");

        HBox badgeRow = new HBox(4, bundleBadge, discBadge);
        badgeRow.setAlignment(Pos.CENTER_LEFT);

        // Buyer title (bold, prominent)
        Label titre = new Label(conseil.getTitreAcheteur());
        titre.setWrapText(true);
        titre.setMinWidth(0);
        titre.setMaxWidth(Double.MAX_VALUE);
        titre.setMaxHeight(28);
        titre.setStyle("-fx-font-weight:bold;-fx-font-size:11px;-fx-text-fill:" + textPrimary() + ";");

        // Product list under title — "• Produit A\n• Produit B"
        VBox productList = new VBox(2);
        for (Produit p : conseil.getProduits()) {
            Label item = new Label("• " + safeText(p.getLibelle(), "Produit"));
            item.setMinWidth(0);
            item.setMaxWidth(Double.MAX_VALUE);
            item.setWrapText(true);
            item.setStyle("-fx-font-size:9px;-fx-text-fill:" + textMuted() + ";");
            productList.getChildren().add(item);
        }

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        // Old combined price - strikethrough
        Label oldPrice = new Label(String.format("%.2f DT", originalPrice));
        oldPrice.setStyle("-fx-font-size:10px;-fx-text-fill:" + textMuted()
                + ";-fx-strikethrough:true;");

        // New combined effective price
        Label newPrice = new Label(String.format("%.2f DT", effectivePrice));
        newPrice.setStyle("-fx-font-weight:bold;-fx-font-size:13px;-fx-text-fill:" + accent() + ";");

        // Add all to cart - full width
        Button addAllBtn = new Button("+ Ajouter le bundle");
        addAllBtn.setMaxWidth(Double.MAX_VALUE);
        addAllBtn.setPrefHeight(24);
        addAllBtn.setStyle("-fx-background-color:linear-gradient(to right,#2563EB,#8B5CF6);"
                + "-fx-background-radius:6;-fx-text-fill:white;"
                + "-fx-font-size:10px;-fx-font-weight:bold;-fx-cursor:hand;-fx-padding:0;");
        addAllBtn.setOnMouseClicked(e -> e.consume());
        addAllBtn.setOnAction(e -> {
            e.consume();
            int userId = SessionManager.getCurrentUserId();
            conseil.getProduits().forEach(p -> interactionService.logAddToCart(userId, p.getIdProduit()));
            if (contextLabel != null)
                contextLabel.setText("Bundle ajouté : " + conseil.getTitreAcheteur());
        });

        // Savings label
        Label savingsLabel = new Label(String.format("✓ Économisez %.2f DT", savings));
        savingsLabel.setStyle("-fx-font-size:9px;-fx-font-weight:bold;-fx-text-fill:" + green() + ";");

        // Expiration
        String expText = conseil.getDateExpiration() != null
                ? "Expire " + conseil.getDateExpiration()
                .format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yy"))
                : "";
        Label expLabel = new Label(expText);
        expLabel.setStyle("-fx-font-size:9px;-fx-text-fill:" + textMuted() + ";");

        content.getChildren().addAll(badgeRow, titre, productList, spacer, oldPrice, newPrice, addAllBtn, savingsLabel, expLabel);
        card.getChildren().addAll(headerWrapper, content);
        attachHoverEffect(card, accent(), accent());
        return card;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Result card building
    // ─────────────────────────────────────────────────────────────────────────

    private void buildResultCards() {
        if (resultCardsBox   != null) resultCardsBox.getChildren().clear();
        if (resultToolbarBox != null) resultToolbarBox.getChildren().clear();
        if (currentResults == null || currentResults.isEmpty()) {
            if (resultsCountLabel != null) resultsCountLabel.setText("0 produit(s)");
            return;
        }
        if (resultsCountLabel != null) resultsCountLabel.setText(currentResults.size() + " produit(s)");
        buildFilterToolbar();
        for (ScoringService.ScoredProduct sp : currentResults)
            if (resultCardsBox != null) resultCardsBox.getChildren().add(buildResultCard(sp));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Image result cards - Step 8d
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Builds result cards for IMAGE_SEARCH results.
     * Identical to buildResultCards() with two differences:
     *  - No budget tag (no green/orange colouring)
     *  - All cards use neutral teal border
     *  - Toolbar only has price and rating sort - no budget-related filters
     */
    private void buildImageResultCards() {
        if (resultCardsBox   != null) resultCardsBox.getChildren().clear();
        if (resultToolbarBox != null) resultToolbarBox.getChildren().clear();

        if (currentResults == null || currentResults.isEmpty()) {
            if (resultsCountLabel != null) resultsCountLabel.setText("0 produit(s)");
            return;
        }

        if (resultsCountLabel != null)
            resultsCountLabel.setText(currentResults.size() + " similaire(s)");

        buildImageFilterToolbar();

        for (ScoringService.ScoredProduct sp : currentResults)
            if (resultCardsBox != null)
                resultCardsBox.getChildren().add(buildImageResultCard(sp));
    }

    /**
     * Simplified toolbar for image results - price sort and rating sort only.
     * No budget filter since there is no budget in image search.
     */
    private void buildImageFilterToolbar() {
        if (resultToolbarBox == null || originalResults == null || originalResults.isEmpty()) return;

        resultToolbarBox.getChildren().add(makeToolbarButton("Prix ↑", () -> {
            List<ScoringService.ScoredProduct> sorted = new ArrayList<>(currentResults);
            sorted.sort(Comparator.comparingDouble(sp -> sp.produit.getPrix()));
            currentResults = sorted;
            buildImageResultCards();
        }));

        resultToolbarBox.getChildren().add(makeToolbarButton("Prix ↓", () -> {
            List<ScoringService.ScoredProduct> sorted = new ArrayList<>(currentResults);
            sorted.sort((a, b) -> Double.compare(b.produit.getPrix(), a.produit.getPrix()));
            currentResults = sorted;
            buildImageResultCards();
        }));

        resultToolbarBox.getChildren().add(makeToolbarButton("Note ↓", () -> {
            List<ScoringService.ScoredProduct> sorted = new ArrayList<>(currentResults);
            sorted.sort((a, b) -> Double.compare(productRating(b), productRating(a)));
            currentResults = sorted;
            buildImageResultCards();
        }));
    }

    /**
     * Builds a single image result card.
     * Same structure as buildResultCard() but:
     *  - Border is always teal (no green/orange budget split)
     *  - No budget tag line
     *  - Small "📷 Similaire" badge instead of budget tag
     */
    private VBox buildImageResultCard(ScoringService.ScoredProduct sp) {
        Produit p = sp.produit;

        VBox card = new VBox(0);
        card.setPrefWidth(230); card.setMinWidth(210); card.setMaxWidth(250);
        card.setStyle(cardStyle(green()));

        // ── Image zone — inset, 4-sided radius ────────────────────────────────
        VBox imgWrapper = buildImageWrapper(card, p, 20);
        // ── Content zone ──────────────────────────────────────────────────────
        VBox content = new VBox(7);
        content.setPadding(new Insets(12, 14, 14, 14));

        // Category badge
        Label badge = buildBadge(p.getCategorie());

        // Product name
        Label name = new Label(safeText(p.getLibelle(), "Produit sans nom"));
        name.setWrapText(true);
        name.setStyle("-fx-font-weight:bold;-fx-font-size:13px;-fx-text-fill:" + textPrimary() + ";");

        // Description
        Label desc = new Label(safeText(p.getDescription(), ""));
        desc.setWrapText(true);
        desc.setStyle("-fx-font-size:11px;-fx-text-fill:" + textMuted() + ";");

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        // Price - always blue/violet accent, no budget colouring
        Label price = new Label(String.format("%.2f DT", p.getPrix()));
        price.setStyle("-fx-font-weight:bold;-fx-font-size:15px;-fx-text-fill:" + accent() + ";");

        // Cart button
        Button addBtn = buildAddToCartButton(p);
        HBox priceLine = new HBox(10, price, addBtn);
        priceLine.setAlignment(Pos.CENTER_LEFT);

        // "Similaire" tag - replaces the budget tag
        Label similarTag = new Label("📷 Similaire");
        similarTag.setStyle(
                "-fx-font-size:10px;-fx-font-weight:bold;" +
                        "-fx-text-fill:" + accent() + ";" +
                        "-fx-background-color:" + (isDarkMode
                        ? "rgba(37,99,235,0.12)"
                        : "rgba(37,99,235,0.08)") + ";" +
                        "-fx-background-radius:6;-fx-padding:2 8 2 8;"
        );

        // Rating
        Label rating = new Label(ratingText(p.getNoteMoyenne()));
        rating.setStyle("-fx-font-size:11px;-fx-text-fill:" + textMuted() + ";");

        content.getChildren().addAll(badge, name, desc, spacer, priceLine, similarTag, rating);
        card.getChildren().addAll(imgWrapper, content);

        attachHoverEffect(card, accent(), accent());
        card.setOnMouseClicked(e -> logProductView(p));

        return card;
    }

    private void buildFilterToolbar() {
        if (resultToolbarBox == null || originalResults == null || originalResults.isEmpty()) return;

        resultToolbarBox.getChildren().add(makeToolbarButton("Prix ↑", () -> {
            List<ScoringService.ScoredProduct> sorted = new ArrayList<>(currentResults);
            sorted.sort(Comparator.comparingDouble(sp -> sp.produit.getPrix()));
            currentResults = sorted;
            buildResultCards();
        }));

        resultToolbarBox.getChildren().add(makeToolbarButton("Prix ↓", () -> {
            List<ScoringService.ScoredProduct> sorted = new ArrayList<>(currentResults);
            sorted.sort((a, b) -> Double.compare(b.produit.getPrix(), a.produit.getPrix()));
            currentResults = sorted;
            buildResultCards();
        }));

        resultToolbarBox.getChildren().add(makeToolbarButton("Note ↓", () -> {
            List<ScoringService.ScoredProduct> sorted = new ArrayList<>(currentResults);
            sorted.sort((a, b) -> Double.compare(productRating(b), productRating(a)));
            currentResults = sorted;
            buildResultCards();
        }));

        Set<String> categories = new LinkedHashSet<>();
        for (ScoringService.ScoredProduct sp : originalResults) {
            if (sp != null && sp.produit != null && sp.produit.getCategorie() != null)
                categories.add(sp.produit.getCategorie());
        }

        for (String category : categories) {
            resultToolbarBox.getChildren().add(makeToolbarButton(category, () -> {
                List<ScoringService.ScoredProduct> filtered = filterByCategory(originalResults, category);
                if (filtered.isEmpty()) {
                    appendBubble("Aucun produit dans cette catégorie.", false);
                    return;
                }
                currentResults = filtered;
                buildResultCards();
            }));
        }

        if (categories.size() > 1 || currentResults.size() != originalResults.size()) {
            resultToolbarBox.getChildren().add(makeToolbarButton("Tous", () -> {
                currentResults = new ArrayList<>(originalResults);
                buildResultCards();
            }));
        }
    }

    private Button makeToolbarButton(String label, Runnable action) {
        Button btn = new Button(label);
        btn.setStyle(toolbarButtonStyle(false));
        btn.setOnAction(e -> action.run());
        btn.setOnMouseEntered(e -> btn.setStyle(toolbarButtonStyle(true)));
        btn.setOnMouseExited(e -> btn.setStyle(toolbarButtonStyle(false)));
        return btn;
    }

    private String toolbarButtonStyle(boolean hover) {
        String border = hover ? "#2563EB" : cardBorder();
        String bg     = hover ? (isDarkMode ? "rgba(37,99,235,0.12)" : "#EFF6FF") : cardBg();
        return "-fx-font-size:11px;" +
                "-fx-background-color:" + bg + ";" +
                "-fx-border-color:" + border + ";" +
                "-fx-border-radius:999;" +
                "-fx-background-radius:999;" +
                "-fx-text-fill:" + (hover ? (isDarkMode ? "#38BDF8" : "#1D4ED8") : textPrimary()) + ";" +
                "-fx-cursor:hand;" +
                "-fx-padding:4 10 4 10;";
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Card builders
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Builds the inner image zone for a product card.
     * Always uses 4-sided rounded corners (12px) so it looks inset inside its
     * wrapper padding. Callers wrap this in a VBox with Insets(8,8,0,8) to
     * create the "floating tile" look — image never bleeds to the card edge.
     *
     * @param p        the product whose image (or category gradient) is displayed
     * @param height   initial pixel height (overridden by the caller's width listener)
     * @param iconSize font-size (px) for the fallback category text icon
     */
    private javafx.scene.layout.Region buildProductImageBox(
            Produit p, double height, int iconSize) {

        // Always 4-sided rounded — the wrapper provides the edge gap
        final double ARC    = 12;
        final String RADIUS = "12";

        // ── resolve image path ────────────────────────────────────────────────
        String raw = p.getImageUrl();
        String cleanPath = null;
        if (raw != null) {
            cleanPath = raw.trim();
            if (cleanPath.startsWith("\"")) cleanPath = cleanPath.substring(1);
            if (cleanPath.endsWith("\""))   cleanPath = cleanPath.substring(0, cleanPath.length() - 1);
            if (cleanPath.isBlank())        cleanPath = null;
        }

        // ── try to build a real-image zone ────────────────────────────────────
        if (cleanPath != null) {
            try {
                String uri = (cleanPath.startsWith("http://")
                        || cleanPath.startsWith("https://")
                        || cleanPath.startsWith("file:"))
                        ? cleanPath
                        : java.nio.file.Paths.get(cleanPath).toUri().toString();

                Image img = new Image(uri, false);

                if (!img.isError()) {
                    ImageView iv = new ImageView(img);
                    iv.setManaged(false);
                    iv.setPreserveRatio(false);
                    iv.setSmooth(true);

                    StackPane pane = new StackPane();
                    pane.setPrefHeight(height);
                    pane.setMinHeight(height);
                    pane.setMaxHeight(height);
                    pane.setMaxWidth(Double.MAX_VALUE);
                    pane.setStyle("-fx-background-color:" + cardBg()
                            + ";-fx-background-radius:" + RADIUS + ";");

                    pane.layoutBoundsProperty().addListener((obs, o, bounds) -> {
                        double w = bounds.getWidth();
                        double h = bounds.getHeight();
                        iv.setX(0); iv.setY(0);
                        iv.setFitWidth(w);
                        iv.setFitHeight(h);
                        javafx.scene.shape.Rectangle clip =
                                new javafx.scene.shape.Rectangle(w, h);
                        clip.setArcWidth(ARC * 2);
                        clip.setArcHeight(ARC * 2);
                        pane.setClip(clip);
                    });

                    pane.getChildren().add(iv);
                    return pane;
                }
            } catch (Exception ignored) {}
        }

        // ── fallback: category gradient + text icon ───────────────────────────
        VBox fallback = new VBox();
        fallback.setPrefHeight(height);
        fallback.setMinHeight(height);
        fallback.setMaxHeight(height);
        fallback.setMaxWidth(Double.MAX_VALUE);
        fallback.setAlignment(Pos.CENTER);
        fallback.setStyle("-fx-background-color:" + categoryGradient(p.getCategorie())
                + ";-fx-background-radius:" + RADIUS + ";");
        fallback.getChildren().add(makeCategoryFallback(p.getCategorie(), iconSize));
        return fallback;
    }

    /**
     * Wraps the inner image box in a VBox with 8px inset padding on left/right/top
     * so the image tile never bleeds to the card edge. The height listener is wired
     * to {@code card.widthProperty()} and accounts for the 16px of horizontal padding.
     *
     * @param card     the parent card VBox (width source for the listener)
     * @param p        product (passed to buildProductImageBox)
     * @param iconSize fallback icon font-size
     * @return the wrapper VBox to add as the FIRST child of the card
     */
    private VBox buildImageWrapper(VBox card, Produit p, int iconSize) {
        javafx.scene.layout.Region inner = buildProductImageBox(p, 130, 28);

        VBox wrapper = new VBox(inner);
        wrapper.setPadding(new Insets(8, 8, 0, 8));
        wrapper.setMaxWidth(Double.MAX_VALUE);

        // Height = 65% of the usable width (card width minus 16px L+R padding)
        card.widthProperty().addListener((obs, o, w) -> {
            double h = (w.doubleValue() - 16) * 0.65;
            inner.setPrefHeight(h);
            inner.setMinHeight(h);
            inner.setMaxHeight(h);
        });

        return wrapper;
    }

    /** Builds the text icon used as a fallback when no product image is available. */
    private Node makeCategoryFallback(String categorie, int iconSize) {
        return UiIcons.icon(categoryIcon(categorie), "#FFFFFF", iconSize);
    }

    private VBox buildIdleCard(Produit p) {
        VBox card = new VBox(0);
        card.setMinWidth(0);
        card.setMaxWidth(Double.MAX_VALUE); // overridden by bindFixedSlotWidth
        HBox.setHgrow(card, Priority.NEVER);
        card.setStyle(cardStyle(cardBorder()));

        // ── Top image zone — inset 8px from card edges, 4-sided rounded corners ──
        VBox imgWrapper = buildImageWrapper(card, p, 20);
        // ── Content zone ────────────────────────────────────────────────────
        VBox content = new VBox(4);
        content.setPadding(new Insets(8, 8, 8, 8));
        VBox.setVgrow(content, Priority.ALWAYS);

        // Category badge - compact
        Label badge = new Label(categoryBadge(p.getCategorie()));
        badge.setPadding(new Insets(1, 6, 1, 6));
        badge.setStyle("-fx-font-size:9px;-fx-font-weight:bold;"
                + "-fx-background-color:" + categoryBadgeColor(p.getCategorie()) + ";"
                + "-fx-text-fill:white;-fx-background-radius:20;");

        // Product name - 2 lines max, 11 px
        // minWidth=0 is critical: without it the label's preferred size can exceed
        // the card's bound width and make the card appear wider than its siblings.
        Label name = new Label(safeText(p.getLibelle(), "Produit sans nom"));
        name.setWrapText(true);
        name.setMinWidth(0);
        name.setMaxWidth(Double.MAX_VALUE);
        name.setMaxHeight(32);
        name.setStyle("-fx-font-weight:bold;-fx-font-size:11px;-fx-text-fill:" + textPrimary() + ";");

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        // Price — check for active promo
        Double promoDiscount = activePromoMap.get(p.getIdProduit());
        VBox priceBox;
        if (promoDiscount != null) {
            double effectivePrice = p.getPrix() * (1.0 - promoDiscount / 100.0);
            Label oldPrice = new Label(String.format("%.2f DT", p.getPrix()));
            oldPrice.setStyle("-fx-font-size:9px;-fx-text-fill:" + textMuted() + ";-fx-strikethrough:true;");
            Label newPrice = new Label(String.format("%.2f DT", effectivePrice));
            newPrice.setStyle("-fx-font-weight:bold;-fx-font-size:13px;-fx-text-fill:" + green() + ";");
            Label discTag = new Label("-" + (int) promoDiscount.doubleValue() + "% 🏷");
            discTag.setPadding(new Insets(1, 5, 1, 5));
            discTag.setStyle("-fx-font-size:8px;-fx-font-weight:bold;"
                    + "-fx-background-color:linear-gradient(to right,#10B981,#14B8A6);"
                    + "-fx-text-fill:white;-fx-background-radius:20;");
            priceBox = new VBox(2, discTag, oldPrice, newPrice);
        } else {
            Label price = new Label(String.format("%.2f DT", p.getPrix()));
            price.setStyle("-fx-font-weight:bold;-fx-font-size:13px;-fx-text-fill:" + accent() + ";");
            priceBox = new VBox(price);
        }

        // Cart button - full width, compact height
        Button addBtn = new Button("+ Cart");
        addBtn.setMaxWidth(Double.MAX_VALUE);
        addBtn.setPrefHeight(24);
        addBtn.setStyle("-fx-background-color:linear-gradient(to bottom right,#2563EB,#8B5CF6);-fx-background-radius:6;"
                + "-fx-text-fill:white;-fx-font-size:10px;-fx-font-weight:bold;"
                + "-fx-cursor:hand;-fx-padding:0;");
        addBtn.setOnMouseClicked(e -> e.consume());
        addBtn.setOnAction(e -> { e.consume(); logAddToCart(p); });

        // Rating
        Label rating = new Label(ratingText(p.getNoteMoyenne()));
        rating.setStyle("-fx-font-size:10px;-fx-text-fill:" + textMuted() + ";");

        // Heart button - red if in wishlist, muted if not
        boolean inWishlist = wishlistProductIds.contains(p.getIdProduit());
        Button heartBtn = new Button(inWishlist ? "❤" : "♡");
        heartBtn.setStyle(heartBtnStyle(inWishlist));
        heartBtn.setOnMouseClicked(e -> e.consume());
        heartBtn.setOnAction(e -> { e.consume(); toggleWishlist(p, heartBtn); });

        // Bottom row: rating + heart aligned left/right
        Region bottomSpacer = new Region();
        HBox.setHgrow(bottomSpacer, Priority.ALWAYS);
        HBox bottomRow = new HBox(rating, bottomSpacer, heartBtn);
        bottomRow.setAlignment(Pos.CENTER_LEFT);

        content.getChildren().addAll(badge, name, spacer, priceBox, addBtn, bottomRow);
        card.getChildren().addAll(imgWrapper, content);
        attachHoverEffect(card, cardBorder(), green());
        card.setOnMouseClicked(e -> logProductView(p));
        return card;
    }

    private VBox buildResultCard(ScoringService.ScoredProduct sp) {
        Produit p             = sp.produit;
        Double  promoDiscount = activePromoMap.get(p.getIdProduit());
        // Use effective price for display and budget colouring
        double  displayPrice  = promoDiscount != null
                ? p.getPrix() * (1.0 - promoDiscount / 100.0)
                : p.getPrix();
        String  accent        = sp.overBudget ? orange() : green();

        VBox card = new VBox(0);
        card.setPrefWidth(230); card.setMinWidth(210); card.setMaxWidth(250);
        card.setStyle(cardStyle(accent));

        // ── Image zone — inset, 4-sided radius ────────────────────────────────
        VBox imgWrapper = buildImageWrapper(card, p, 20);
        VBox content = new VBox(7);
        content.setPadding(new Insets(12, 14, 14, 14));

        Label badge = buildBadge(p.getCategorie());
        Label name  = new Label(safeText(p.getLibelle(), "Produit sans nom"));
        name.setWrapText(true);
        name.setStyle("-fx-font-weight:bold;-fx-font-size:13px;-fx-text-fill:" + textPrimary() + ";");
        Label desc = new Label(safeText(p.getDescription(), ""));
        desc.setWrapText(true);
        desc.setStyle("-fx-font-size:11px;-fx-text-fill:" + textMuted() + ";");

        Region spacer = new Region(); VBox.setVgrow(spacer, Priority.ALWAYS);

        // Price line — promo overlay if applicable
        HBox priceLine;
        if (promoDiscount != null) {
            Label oldPrice = new Label(String.format("%.2f DT", p.getPrix()));
            oldPrice.setStyle("-fx-font-size:11px;-fx-text-fill:" + textMuted() + ";-fx-strikethrough:true;");
            Label newPrice = new Label(String.format("%.2f DT", displayPrice));
            newPrice.setStyle("-fx-font-weight:bold;-fx-font-size:15px;-fx-text-fill:" + green() + ";");
            Label discTag = new Label("-" + (int) promoDiscount.doubleValue() + "% 🏷");
            discTag.setPadding(new Insets(1, 6, 1, 6));
            discTag.setStyle("-fx-font-size:9px;-fx-font-weight:bold;"
                    + "-fx-background-color:linear-gradient(to right,#10B981,#14B8A6);"
                    + "-fx-text-fill:white;-fx-background-radius:20;");
            Button addBtn = buildAddToCartButton(p);
            VBox priceStack = new VBox(2, discTag, oldPrice, newPrice);
            priceLine = new HBox(10, priceStack, addBtn);
        } else {
            Label price = new Label(String.format("%.2f DT", displayPrice));
            price.setStyle("-fx-font-weight:bold;-fx-font-size:15px;-fx-text-fill:" + accent + ";");
            Button addBtn = buildAddToCartButton(p);
            priceLine = new HBox(10, price, addBtn);
        }
        priceLine.setAlignment(Pos.CENTER_LEFT);

        Label budgetTag = new Label(sp.overBudget ? "⚠ dépasse budget" : "✓ dans budget");
        budgetTag.setStyle("-fx-font-size:10px;-fx-text-fill:" + accent + ";");
        Label rating = new Label(ratingText(p.getNoteMoyenne()));
        rating.setStyle("-fx-font-size:11px;-fx-text-fill:" + textMuted() + ";");

        // Heart button - red if in wishlist, muted if not
        boolean inWishlist = wishlistProductIds.contains(p.getIdProduit());
        Button heartBtn = new Button(inWishlist ? "❤" : "♡");
        heartBtn.setStyle(heartBtnStyle(inWishlist));
        heartBtn.setOnMouseClicked(e -> e.consume());
        heartBtn.setOnAction(e -> { e.consume(); toggleWishlist(p, heartBtn); });

        // Bottom row: rating + heart aligned left/right
        Region bottomSpacer = new Region();
        HBox.setHgrow(bottomSpacer, Priority.ALWAYS);
        HBox bottomRow = new HBox(rating, bottomSpacer, heartBtn);
        bottomRow.setAlignment(Pos.CENTER_LEFT);

        content.getChildren().addAll(badge, name, desc, spacer, priceLine, budgetTag, bottomRow);
        card.getChildren().addAll(imgWrapper, content);
        attachHoverEffect(card, accent, accent);
        card.setOnMouseClicked(e -> logProductView(p));
        return card;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Detail panel (P4–P6)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Opens the right-side detail panel for the given product.
     * If the same product is already open, collapses it (toggle).
     */
    private void openDetail(Produit p) {
        if (p == null) return;
        if (detailPanelOpen && p.getIdProduit() == (detailPanelProduit != null ? detailPanelProduit.getIdProduit() : -1)) {
            closeDetail();
            return;
        }
        detailPanelProduit = p;
        buildDetailContent(p);
        slideDetailIn();
    }

    private void closeDetail() {
        if (!detailPanelOpen) return;
        slideDetailOut();
    }

    private void slideDetailIn() {
        if (detailPanel == null) return;
        detailPanel.setTranslateX(340);
        detailPanel.setPrefWidth(340);
        detailPanel.setMinWidth(340);
        detailPanel.setMaxWidth(340);
        detailPanel.setVisible(true);
        detailPanel.setManaged(true);
        detailPanel.setStyle(
                "-fx-background-color:" + (isDarkMode ? "#111425" : "#FFFFFF") + ";" +
                        "-fx-border-color:" + (isDarkMode ? "rgba(255,255,255,0.08)" : "#E2E8F0") + ";" +
                        "-fx-border-width:0 0 0 1;");
        TranslateTransition tt = new TranslateTransition(Duration.millis(260), detailPanel);
        tt.setFromX(340); tt.setToX(0);
        tt.setInterpolator(Interpolator.EASE_OUT);
        tt.play();
        detailPanelOpen = true;
    }

    private void slideDetailOut() {
        if (detailPanel == null) return;
        TranslateTransition tt = new TranslateTransition(Duration.millis(200), detailPanel);
        tt.setFromX(0); tt.setToX(340);
        tt.setInterpolator(Interpolator.EASE_IN);
        tt.setOnFinished(e -> {
            detailPanel.setVisible(false);
            detailPanel.setManaged(false);
            detailPanel.setPrefWidth(0);
            detailPanel.setMinWidth(0);
            detailPanel.setMaxWidth(0);
            detailPanel.setTranslateX(0);
            detailPanelOpen   = false;
            detailPanelProduit = null;
        });
        tt.play();
    }

    /** Rebuilds the detail panel content for the given product. */
    private void buildDetailContent(Produit p) {
        if (detailPanel == null) return;
        detailPanel.getChildren().clear();

        // ── Header row: title + close button ─────────────────────────────────
        Label title = new Label("Détails du produit");
        title.setStyle("-fx-font-size:13px;-fx-font-weight:bold;-fx-text-fill:" + textMuted() + ";");

        Button closeBtn = new Button("✕");
        closeBtn.setStyle(
                "-fx-background-color:transparent;-fx-border-color:transparent;" +
                        "-fx-text-fill:" + textMuted() + ";-fx-font-size:14px;-fx-cursor:hand;-fx-padding:0;");
        closeBtn.setOnMouseEntered(e -> closeBtn.setStyle(
                "-fx-background-color:transparent;-fx-border-color:transparent;" +
                        "-fx-text-fill:" + textPrimary() + ";-fx-font-size:14px;-fx-cursor:hand;-fx-padding:0;"));
        closeBtn.setOnMouseExited(e -> closeBtn.setStyle(
                "-fx-background-color:transparent;-fx-border-color:transparent;" +
                        "-fx-text-fill:" + textMuted() + ";-fx-font-size:14px;-fx-cursor:hand;-fx-padding:0;"));
        closeBtn.setOnAction(e -> closeDetail());

        Region headerSpacer = new Region();
        HBox.setHgrow(headerSpacer, Priority.ALWAYS);
        HBox header = new HBox(title, headerSpacer, closeBtn);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(16, 16, 8, 16));

        // ── Separator ─────────────────────────────────────────────────────────
        Region sep1 = new Region();
        sep1.setPrefHeight(1); sep1.setMaxHeight(1);
        sep1.setMaxWidth(Double.MAX_VALUE);
        sep1.setStyle("-fx-background-color:" + (isDarkMode ? "rgba(255,255,255,0.07)" : "#E2E8F0") + ";");

        // ── Image zone ────────────────────────────────────────────────────────
        javafx.scene.layout.Region imgInner = buildProductImageBox(p, 180, 28);
        VBox imgZone = new VBox(imgInner);
        imgZone.setPadding(new Insets(12, 16, 0, 16));
        imgZone.setMaxWidth(Double.MAX_VALUE);
        imgInner.setPrefHeight(180);
        imgInner.setMinHeight(180);
        imgInner.setMaxHeight(180);
        imgInner.setMaxWidth(Double.MAX_VALUE);

        // ── Scrollable body ───────────────────────────────────────────────────
        VBox body = new VBox(12);
        body.setPadding(new Insets(14, 16, 20, 16));
        body.setMaxWidth(Double.MAX_VALUE);

        // Category badge
        Label catBadge = new Label(categoryBadge(p.getCategorie()));
        catBadge.setPadding(new Insets(3, 10, 3, 10));
        catBadge.setStyle("-fx-font-size:10px;-fx-font-weight:bold;" +
                "-fx-background-color:" + categoryBadgeColor(p.getCategorie()) + ";" +
                "-fx-text-fill:white;-fx-background-radius:20;");

        // Promo badge if applicable
        Double promoDisc = activePromoMap.get(p.getIdProduit());
        HBox badgeRow = new HBox(6, catBadge);
        if (promoDisc != null) {
            Label promoBadge = new Label("-" + (int) promoDisc.doubleValue() + "% 🏷");
            promoBadge.setPadding(new Insets(3, 10, 3, 10));
            promoBadge.setStyle("-fx-font-size:10px;-fx-font-weight:bold;" +
                    "-fx-background-color:linear-gradient(to right,#10B981,#14B8A6);" +
                    "-fx-text-fill:white;-fx-background-radius:20;");
            badgeRow.getChildren().add(promoBadge);
        }
        badgeRow.setAlignment(Pos.CENTER_LEFT);

        // Product name
        Label name = new Label(safeText(p.getLibelle(), "Produit sans nom"));
        name.setWrapText(true);
        name.setMaxWidth(Double.MAX_VALUE);
        name.setStyle("-fx-font-size:16px;-fx-font-weight:bold;-fx-text-fill:" + textPrimary() + ";");

        // Price section
        VBox priceSection = new VBox(4);
        if (promoDisc != null) {
            double effective = p.getPrix() * (1.0 - promoDisc / 100.0);
            Label oldPrice = new Label(String.format("%.2f DT", p.getPrix()));
            oldPrice.setStyle("-fx-font-size:12px;-fx-text-fill:" + textMuted() + ";-fx-strikethrough:true;");
            Label newPrice = new Label(String.format("%.2f DT", effective));
            newPrice.setStyle("-fx-font-size:20px;-fx-font-weight:bold;-fx-text-fill:" + green() + ";");
            Label saving = new Label(String.format("✓ Économisez %.2f DT", p.getPrix() - effective));
            saving.setStyle("-fx-font-size:11px;-fx-text-fill:" + green() + ";-fx-font-weight:bold;");
            priceSection.getChildren().addAll(oldPrice, newPrice, saving);
        } else {
            Label price = new Label(String.format("%.2f DT", p.getPrix()));
            price.setStyle("-fx-font-size:20px;-fx-font-weight:bold;-fx-text-fill:" + accent() + ";");
            priceSection.getChildren().add(price);
        }

        // Separator
        Region sep2 = new Region();
        sep2.setPrefHeight(1); sep2.setMaxHeight(1);
        sep2.setMaxWidth(Double.MAX_VALUE);
        sep2.setStyle("-fx-background-color:" + (isDarkMode ? "rgba(255,255,255,0.07)" : "#E2E8F0") + ";");

        // Description
        Label descTitle = new Label("Description");
        descTitle.setStyle("-fx-font-size:11px;-fx-font-weight:bold;-fx-text-fill:" + textMuted() + ";letter-spacing:0.5;");

        String descText = safeText(p.getDescription(), "Aucune description disponible.");
        Label desc = new Label(descText);
        desc.setWrapText(true);
        desc.setMaxWidth(Double.MAX_VALUE);
        desc.setStyle("-fx-font-size:12px;-fx-text-fill:" + textPrimary() + ";-fx-line-spacing:3;");

        // Separator
        Region sep3 = new Region();
        sep3.setPrefHeight(1); sep3.setMaxHeight(1);
        sep3.setMaxWidth(Double.MAX_VALUE);
        sep3.setStyle("-fx-background-color:" + (isDarkMode ? "rgba(255,255,255,0.07)" : "#E2E8F0") + ";");

        // Stock badge
        String stockStyle, stockText;
        int stock = p.getQuantiteStock();
        if (stock > 10) {
            stockStyle = "-fx-background-color:rgba(16,185,129,0.12);-fx-text-fill:#10B981;";
            stockText = "✓ En stock (" + stock + ")";
        } else if (stock > 0) {
            stockStyle = "-fx-background-color:rgba(249,115,22,0.12);-fx-text-fill:#F97316;";
            stockText = "⚠ Stock limité (" + stock + " restants)";
        } else {
            stockStyle = "-fx-background-color:rgba(239,68,68,0.12);-fx-text-fill:#EF4444;";
            stockText = "✕ Rupture de stock";
        }
        Label stockBadge = new Label(stockText);
        stockBadge.setPadding(new Insets(4, 12, 4, 12));
        stockBadge.setStyle(stockStyle + "-fx-font-size:11px;-fx-font-weight:bold;-fx-background-radius:20;");

        // Rating row
        double rating = p.getNoteMoyenne() != null ? p.getNoteMoyenne() : 0.0;
        Label ratingLabel = new Label(rating > 0 ? "★ " + String.format("%.1f", rating) + " / 5" : "★ Pas encore noté");
        ratingLabel.setStyle("-fx-font-size:12px;-fx-text-fill:" + (rating > 0 ? "#F59E0B" : textMuted()) + ";");

        // Action buttons
        boolean inWishlist = wishlistProductIds.contains(p.getIdProduit());
        Button heartBtn = new Button(inWishlist ? "❤  Retirer des favoris" : "♡  Ajouter aux favoris");
        heartBtn.setMaxWidth(Double.MAX_VALUE);
        heartBtn.setStyle(
                "-fx-background-color:" + (isDarkMode ? "rgba(255,255,255,0.05)" : "#F1F5F9") + ";" +
                        "-fx-border-color:" + (isDarkMode ? "rgba(255,255,255,0.10)" : "#E2E8F0") + ";" +
                        "-fx-border-radius:10;-fx-background-radius:10;" +
                        "-fx-text-fill:" + (inWishlist ? "#E11D48" : textMuted()) + ";" +
                        "-fx-font-size:12px;-fx-cursor:hand;-fx-padding:9 0 9 0;");
        heartBtn.setOnAction(e -> {
            toggleWishlist(p, heartBtn);
            // Refresh heart label after toggle (slight delay to let the thread run)
            javafx.animation.PauseTransition pt = new javafx.animation.PauseTransition(Duration.millis(300));
            pt.setOnFinished(ev -> {
                boolean nowIn = wishlistProductIds.contains(p.getIdProduit());
                heartBtn.setText(nowIn ? "❤  Retirer des favoris" : "♡  Ajouter aux favoris");
                heartBtn.setStyle(
                        "-fx-background-color:" + (isDarkMode ? "rgba(255,255,255,0.05)" : "#F1F5F9") + ";" +
                                "-fx-border-color:" + (isDarkMode ? "rgba(255,255,255,0.10)" : "#E2E8F0") + ";" +
                                "-fx-border-radius:10;-fx-background-radius:10;" +
                                "-fx-text-fill:" + (nowIn ? "#E11D48" : textMuted()) + ";" +
                                "-fx-font-size:12px;-fx-cursor:hand;-fx-padding:9 0 9 0;");
            });
            pt.play();
        });

        Button cartBtn2 = new Button("+ Ajouter au panier");
        cartBtn2.setMaxWidth(Double.MAX_VALUE);
        cartBtn2.setStyle(
                "-fx-background-color:linear-gradient(to bottom right,#2563EB,#8B5CF6);" +
                        "-fx-background-radius:10;-fx-text-fill:white;" +
                        "-fx-font-size:13px;-fx-font-weight:bold;-fx-cursor:hand;-fx-padding:10 0 10 0;");
        cartBtn2.setOnAction(e -> logAddToCart(p));

        body.getChildren().addAll(
                badgeRow, name, priceSection,
                sep2,
                descTitle, desc,
                sep3,
                stockBadge, ratingLabel,
                heartBtn, cartBtn2
        );

        ScrollPane scroll = new ScrollPane(body);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroll.setStyle("-fx-background:transparent;-fx-background-color:transparent;-fx-padding:0;");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        detailPanel.getChildren().addAll(header, sep1, imgZone, scroll);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Hover effect
    // ─────────────────────────────────────────────────────────────────────────

    private void attachHoverEffect(VBox card, String normalBorder, String hoverAccent) {
        DropShadow shadow = new DropShadow();
        shadow.setRadius(6);
        shadow.setSpread(0);
        shadow.setOffsetY(2);
        shadow.setColor(Color.web("#000000", 0.18));
        card.setEffect(shadow);

        ScaleTransition scaleUp   = new ScaleTransition(Duration.millis(180), card);
        ScaleTransition scaleDown = new ScaleTransition(Duration.millis(180), card);
        scaleUp.setToX(1.02);  scaleUp.setToY(1.02);
        scaleDown.setToX(1.0); scaleDown.setToY(1.0);

        card.setOnMouseEntered(e -> {
            scaleDown.stop();
            card.setStyle(cardStyle(hoverAccent) +
                    "-fx-background-color:" + cardHoverBg() + ";");
            shadow.setRadius(14); shadow.setSpread(0.03);
            shadow.setColor(Color.web(isDarkMode ? "#2563EB" : "#1D4ED8", 0.25));
            scaleUp.playFromStart();
        });
        card.setOnMouseExited(e -> {
            scaleUp.stop();
            card.setStyle(cardStyle(normalBorder));
            shadow.setRadius(6);
            shadow.setSpread(0);
            shadow.setOffsetY(2);
            shadow.setColor(Color.web("#000000", 0.18));
            scaleDown.playFromStart();
        });
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Chat bubbles - sequential typewriter queue
    // ─────────────────────────────────────────────────────────────────────────

    private void appendBubble(String message, boolean isUser) {
        if (chatBubbleBox == null) return;

        HBox row = new HBox();
        row.setPadding(new Insets(3, 28, 3, 28));
        row.setAlignment(isUser ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
        row.setOpacity(0);

        Label bubble = new Label(isUser ? message : "");
        bubble.setWrapText(true);
        bubble.setMaxWidth(520);
        bubble.setPadding(new Insets(10, 16, 10, 16));

        if (isUser) {
            bubble.setStyle(
                    "-fx-background-color:" + userBubbleBg() + ";" +
                            "-fx-background-radius:18 18 4 18;" +
                            "-fx-border-color:" + (isDarkMode ? "rgba(37,99,235,0.28)" : "#BFDBFE") + ";" +
                            "-fx-border-radius:18 18 4 18;" +
                            "-fx-font-size:13px;-fx-text-fill:" + textPrimary() + ";");
        } else {
            bubble.setStyle(
                    "-fx-background-color:" + botBubbleBg() + ";" +
                            "-fx-border-color:" + botBubbleBorder() + ";" +
                            "-fx-background-radius:18 18 18 4;" +
                            "-fx-border-radius:18 18 18 4;" +
                            "-fx-font-size:13px;-fx-text-fill:" + textPrimary() + ";");
        }

        row.getChildren().add(bubble);
        chatBubbleBox.getChildren().add(row);

        FadeTransition      fade  = new FadeTransition(Duration.millis(220), row);
        TranslateTransition slide = new TranslateTransition(Duration.millis(220), row);
        fade.setToValue(1);  fade.setInterpolator(Interpolator.EASE_OUT);
        slide.setFromY(10);  slide.setToY(0); slide.setInterpolator(Interpolator.EASE_OUT);
        ParallelTransition entrance = new ParallelTransition(fade, slide);

        if (isUser) {
            entrance.play();
        } else {
            entrance.setOnFinished(e -> {
                typingQueue.add(new BubbleJob(bubble, message));
                if (!isBubbleTyping) drainBubbleQueue();
            });
            entrance.play();
        }

        Platform.runLater(() -> { if (mainScroll != null) mainScroll.setVvalue(1.0); });
    }

    private void drainBubbleQueue() {
        BubbleJob job = typingQueue.poll();
        if (job == null) {
            isBubbleTyping = false;
            activeTypingLine = null;
            return;
        }
        isBubbleTyping = true;
        playTypingEffect(job.label, job.text);
    }

    private void playTypingEffect(Label target, String fullText) {
        if (fullText == null || fullText.isEmpty()) {
            target.setText(fullText != null ? fullText : "");
            drainBubbleQueue();
            return;
        }
        final long MS_PER_CHAR = 22;
        Timeline tl = new Timeline();
        for (int i = 0; i < fullText.length(); i++) {
            final int ci = i + 1;
            tl.getKeyFrames().add(new KeyFrame(
                    Duration.millis(MS_PER_CHAR * ci),
                    e -> target.setText(fullText.substring(0, ci))
            ));
        }
        tl.setOnFinished(e -> {
            Platform.runLater(() -> { if (mainScroll != null) mainScroll.setVvalue(1.0); });
            drainBubbleQueue();
        });
        activeTypingLine = tl;
        tl.play();
    }

    private void stopBubbleQueue() {
        typingQueue.clear();
        isBubbleTyping = false;
        if (activeTypingLine != null) {
            activeTypingLine.stop();
            activeTypingLine = null;
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────

    private String cardStyle(String borderColor) {
        return  "-fx-background-color:" + cardBg() + ";" +
                "-fx-border-color:" + borderColor + ";" +
                "-fx-border-width:1;" +
                "-fx-border-radius:14;" +
                "-fx-background-radius:14;" +
                "-fx-cursor:hand;";
    }

    private Label buildBadge(String categorie) {
        Label b = new Label(categoryBadge(categorie));
        b.setPadding(new Insets(2, 8, 2, 8));
        b.setStyle("-fx-font-size:10px;-fx-font-weight:bold;" +
                "-fx-background-color:" + categoryBadgeColor(categorie) + ";" +
                "-fx-text-fill:white;-fx-background-radius:20;");
        return b;
    }

    private Button buildAddToCartButton(Produit p) {
        Button btn = new Button("+ Cart");
        btn.setMinWidth(70); btn.setPrefWidth(70); btn.setMinHeight(32);
        btn.setStyle("-fx-background-color:linear-gradient(to bottom right,#2563EB,#8B5CF6);-fx-background-radius:8;" +
                "-fx-text-fill:white;-fx-font-size:11px;-fx-font-weight:bold;" +
                "-fx-cursor:hand;-fx-padding:0 10 0 10;");
        btn.setOnMouseClicked(e -> e.consume());
        btn.setOnAction(e -> { e.consume(); logAddToCart(p); });
        return btn;
    }

    private void logProductView(Produit p) {
        if (p == null) return;
        int userId = SessionManager.getCurrentUserId();
        interactionService.logView(userId, p.getIdProduit());
        interactionService.logProductClick(userId, p.getIdProduit());
        if (contextLabel != null)
            contextLabel.setText("Produit consulté : " + safeText(p.getLibelle(), "Produit"));
        openDetail(p);
    }

    private void logAddToCart(Produit p) {
        if (p == null) return;
        interactionService.logAddToCart(SessionManager.getCurrentUserId(), p.getIdProduit());
        if (contextLabel != null)
            contextLabel.setText("Ajouté au panier : " + safeText(p.getLibelle(), "Produit"));
    }

    /**
     * Toggles a product in/out of the wishlist.
     * Updates the in-memory set, DB, interaction log, and the heart button text - all silently.
     * @param p          the product to toggle
     * @param heartBtn   the ♡/❤ button on the card to update visually
     */
    private void toggleWishlist(Produit p, Button heartBtn) {
        if (p == null) return;
        int userId    = SessionManager.getCurrentUserId();
        int produitId = p.getIdProduit();
        boolean alreadyIn = wishlistProductIds.contains(produitId);

        Thread t = new Thread(() -> {
            if (alreadyIn) {
                wishlistService.remove(userId, produitId);
                Platform.runLater(() -> {
                    wishlistProductIds.remove(produitId);
                    heartBtn.setText("♡");
                    heartBtn.setStyle(heartBtnStyle(false));
                    if (contextLabel != null)
                        contextLabel.setText("Retiré des favoris : " + safeText(p.getLibelle(), "Produit"));
                });
            } else {
                wishlistService.add(userId, produitId);
                interactionService.logAddToWishlist(userId, produitId);
                Platform.runLater(() -> {
                    wishlistProductIds.add(produitId);
                    heartBtn.setText("❤");
                    heartBtn.setStyle(heartBtnStyle(true));
                    if (contextLabel != null)
                        contextLabel.setText("Ajouté aux favoris : " + safeText(p.getLibelle(), "Produit"));
                });
            }
        }, "wishlist-toggle");
        t.setDaemon(true);
        t.start();
    }

    /** Style for the heart button depending on wishlist state. */
    private String heartBtnStyle(boolean inWishlist) {
        String color = inWishlist ? "#E11D48" : (isDarkMode ? "#4A5A7A" : "#94A3B8");
        return "-fx-background-color:transparent;" +
                "-fx-border-color:transparent;" +
                "-fx-text-fill:" + color + ";" +
                "-fx-font-size:16px;" +
                "-fx-cursor:hand;" +
                "-fx-padding:0;";
    }

    private List<ScoringService.ScoredProduct> filterByCategory(
            List<ScoringService.ScoredProduct> source, String category) {
        if (source == null) return new ArrayList<>();
        return source.stream()
                .filter(sp -> sp != null && sp.produit != null
                        && category != null
                        && category.equalsIgnoreCase(sp.produit.getCategorie()))
                .collect(Collectors.toList());
    }

    private double productRating(ScoringService.ScoredProduct sp) {
        if (sp == null || sp.produit == null || sp.produit.getNoteMoyenne() == null) return 3.0;
        return sp.produit.getNoteMoyenne();
    }

    private String categoryBadge(String cat) {
        String n = norm(cat);
        if ("medicament".equals(n))   return "MED";
        if ("alimentaire".equals(n))  return "ALI";
        if ("electronique".equals(n)) return "ELE";
        if ("hygiene".equals(n))      return "HYG";
        if ("decor".equals(n))        return "DEC";
        if ("makeup".equals(n))       return "MKP";
        return "AUTRE";
    }

    private UiIcons.Icon categoryIcon(String cat) {
        String n = norm(cat);
        if ("medicament".equals(n))   return UiIcons.Icon.PILL;
        if ("alimentaire".equals(n))  return UiIcons.Icon.BASKET;
        if ("electronique".equals(n)) return UiIcons.Icon.LAPTOP;
        if ("hygiene".equals(n))      return UiIcons.Icon.BOTTLE;
        if ("decor".equals(n))        return UiIcons.Icon.HOME;
        if ("makeup".equals(n))       return UiIcons.Icon.TAG;
        return UiIcons.Icon.BAG;
    }

    private String categoryGradient(String cat) {
        String n = norm(cat);
        if ("medicament".equals(n))   return "linear-gradient(to bottom right,#0EA5E9,#14B8A6)";
        if ("alimentaire".equals(n))  return "linear-gradient(to bottom right,#16A34A,#65A30D)";
        if ("electronique".equals(n)) return "linear-gradient(to bottom right,#2563EB,#0891B2)";
        if ("hygiene".equals(n))      return "linear-gradient(to bottom right,#06B6D4,#0D9488)";
        if ("decor".equals(n))        return "linear-gradient(to bottom right,#F97316,#D97706)";
        if ("makeup".equals(n))       return "linear-gradient(to bottom right,#DB2777,#E11D48)";
        return "linear-gradient(to bottom right,#64748B,#334155)";
    }

    private String categoryBadgeColor(String cat) {
        String n = norm(cat);
        if ("medicament".equals(n))   return "#0284C7";
        if ("alimentaire".equals(n))  return "#16A34A";
        if ("electronique".equals(n)) return "#0E7490";
        if ("hygiene".equals(n))      return "#06B6D4";
        if ("decor".equals(n))        return "#EA580C";
        if ("makeup".equals(n))       return "#DB2777";
        return "#64748B";
    }

    private String norm(String s) {
        return KeywordExtractor.removeAccents(s == null ? "" : s).toLowerCase().trim();
    }

    private String ratingText(Double note) {
        return note != null ? "★ " + String.format("%.1f", note) : "★ -";
    }

    private String safeText(String text, String fallback) {
        return (text != null && !text.trim().isEmpty()) ? text.trim() : fallback;
    }

    @SuppressWarnings("unused")
    private void showErrorAlert(String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Erreur"); alert.setHeaderText(null);
        alert.setContentText(content); alert.showAndWait();
    }
}

