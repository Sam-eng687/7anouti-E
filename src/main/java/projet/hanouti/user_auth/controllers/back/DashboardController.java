package projet.hanouti.user_auth.controllers.back;

import javafx.animation.*;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.shape.Circle;
import javafx.scene.text.Text;
import javafx.util.Duration;
import projet.hanouti.AIachat.controllers.AssistantIAController;
import projet.hanouti.GestionCommandes.entities.Notification;
import projet.hanouti.GestionCommandes.services.NotificationService;
import projet.hanouti.common.utils.SessionManager;
import projet.hanouti.common.utils.UiIcons;
import projet.hanouti.user_auth.entities.User;
import projet.hanouti.user_auth.enums.Role;
import projet.hanouti.user_auth.services.UserCRUD;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

public class DashboardController {

    private static final String ADMIN_USERS_FXML = "/FXML/user_auth/back/tabs/admin_users.fxml";
    private static final String PLACEHOLDER_FXML = "/FXML/user_auth/back/tabs/module_placeholder.fxml";
    private static final String AI_ACHAT_FXML = "/FXML/AIachat/assistant_ia.fxml";
    private static final String ACHETEUR_COMMANDES_FXML = "/FXML/GestionCommandes/AcheteurCommandes.fxml";
    private static final String VENDEUR_COMMANDES_FXML = "/FXML/GestionCommandes/VendeurCommandes.fxml";
    private static final String ADMIN_COMMANDES_FXML = "/FXML/GestionCommandes/AdminCommandes.fxml";

    @FXML private AnchorPane rootPane;
    @FXML private VBox sidebar;
    @FXML private ImageView sidebarLogo;
    @FXML private Button navUsers, navProducts, navOrders, navStats, navSettings, navSupport;
    @FXML private Button themeToggleBtn, logoutBtn, notifBtn, cartBtn, hamburgerBtn;
    @FXML private Label themeIcon, themeLabel;
    @FXML private MenuButton profileMenu;
    @FXML private MenuItem editProfileItem, paymentHistoryItem, logoutProfileItem;
    @FXML private HBox headerBar;
    @FXML private Text headerTitle, headerSubtitle;
    @FXML private Label adminNameLabel, adminRoleLabel, adminAvatarLetter;
    @FXML private ImageView adminAvatar;
    @FXML private StackPane contentContainer;

    @FXML private StackPane profileOverlayPane;
    @FXML private VBox profileDetailCard;
    @FXML private Button profileCloseDetailBtn, profileDetailSaveBtn, profileDetailCancelBtn;
    @FXML private Text profileDetailTitle;
    @FXML private ImageView profileDetailAvatar;
    @FXML private Label profileDetailAvatarLetter, profileDetailMessage;
    @FXML private TextField profileDetailNom, profileDetailPrenom, profileDetailEmail, profileDetailTel, profileDetailDate;
    @FXML private ComboBox<String> profileDetailRole, profileDetailStatus;

    private final UserCRUD userCRUD = new UserCRUD();
    private final NotificationService notificationService = new NotificationService();
    private final List<NavItem> navItems = new ArrayList<>();
    private boolean isDarkMode = true;
    private boolean sidebarOpen = false;
    private User connectedUser;
    private Object currentModuleController;
    private Timeline notificationRefreshTimeline;
    private static final DateTimeFormatter NOTIF_DT_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @FXML
    public void initialize() {
        if (!rootPane.getStyleClass().contains("dash-root")) {
            rootPane.getStyleClass().add("dash-root");
        }

        SessionManager session = SessionManager.getInstance();
        if (!session.isLoggedIn()) {
            redirectToLoginImmediately();
            return;
        }

        connectedUser = session.getConnectedUser();
        if (connectedUser == null || connectedUser.getRole() == null) {
            redirectToLoginImmediately();
            return;
        }

        isDarkMode = session.isDarkMode();
        setDarkMode(isDarkMode);
        updateHeaderUser(connectedUser);
        setupHeaderActions();
        setupProfileOverlay();
        setupShellChrome();
        setupRoleNavigation(connectedUser.getRole());
        startNotificationAutoRefresh();
        refreshHeaderIcons();

        logoutBtn.setVisible(false);
        logoutBtn.setManaged(false);

        playEntrance();
        javafx.application.Platform.runLater(() -> rootPane.requestFocus());
    }

    public void setConnectedAdmin(User admin) {
        if (admin == null) {
            admin = SessionManager.getInstance().getConnectedUser();
        }
        connectedUser = admin;
        updateHeaderUser(admin);
    }

    private void setupHeaderActions() {
        profileMenu.setText("Profil");
        if (connectedUser.getPrenom() != null && !connectedUser.getPrenom().isBlank()) {
            profileMenu.setText("Profil " + connectedUser.getPrenom());
        }

        editProfileItem.setOnAction(e -> openProfileEdit(connectedUser));
        logoutProfileItem.setOnAction(e -> navigateToLogin());
        paymentHistoryItem.setOnAction(e ->
                showInfo("Historique", "Historique paiement pret pour integration."));
        notifBtn.setOnAction(e -> openNotificationCenter());

        boolean buyer = connectedUser.getRole() == Role.acheteur;
        cartBtn.setVisible(buyer);
        cartBtn.setManaged(buyer);
        paymentHistoryItem.setVisible(buyer);
        refreshHeaderIcons();
    }

    private void setupProfileOverlay() {
        profileDetailRole.setItems(FXCollections.observableArrayList("admin", "acheteur", "vendeur", "livreur"));
        profileDetailStatus.setItems(FXCollections.observableArrayList("Unbanned", "Banned"));
        profileCloseDetailBtn.setOnAction(e -> closeProfileEdit());
        profileDetailCancelBtn.setOnAction(e -> closeProfileEdit());
        profileDetailSaveBtn.setOnAction(e -> saveProfileEdit());
        profileOverlayPane.setOnMouseClicked(e -> {
            if (e.getTarget() == profileOverlayPane) {
                closeProfileEdit();
            }
        });
    }

    private void setupShellChrome() {
        themeToggleBtn.setOnAction(e -> {
            isDarkMode = !isDarkMode;
            SessionManager.getInstance().setDarkMode(isDarkMode);
            setDarkMode(isDarkMode);
            bounceNode(themeToggleBtn);
        });

        try {
            java.io.InputStream s = getClass().getResourceAsStream("/images/user_auth/logo.png");
            if (s != null) {
                sidebarLogo.setImage(new Image(s));
            }
        } catch (Exception ignored) {}

        sidebar.setVisible(false);
        sidebar.setManaged(false);
        hamburgerBtn.setOnAction(e -> toggleSidebar());
        adminAvatar.setClip(new Circle(19, 19, 19));
    }

    private void setupRoleNavigation(Role role) {
        navItems.clear();
        resetNavVisibility();

        if (role == Role.admin) {
            addNav(navUsers, "Dashboard global", "Vue globale plateforme", ADMIN_USERS_FXML);
            addNav(navProducts, "Gestion acheteurs", "Gestion des comptes acheteurs", PLACEHOLDER_FXML);
            addNav(navOrders, "Gestion vendeurs", "Gestion des comptes vendeurs", PLACEHOLDER_FXML);
            addNav(navStats, "Gestion societes de livraison", "Gestion livraison", PLACEHOLDER_FXML);
            addNav(navSettings, "Gestion commandes", "Supervision commandes", ADMIN_COMMANDES_FXML);
            addNav(navSupport, "Historique IA et interactions", "Audit IA", PLACEHOLDER_FXML);
        } else if (role == Role.acheteur) {
            addNav(navUsers, "Dashboard", "Accueil acheteur", AI_ACHAT_FXML,
                    controller -> ((AssistantIAController) controller).openExploreMode());
            addNav(navProducts, "AI Achats", "Assistant intelligent", AI_ACHAT_FXML,
                    controller -> ((AssistantIAController) controller).openAssistantMode());
            addNav(navOrders, "Catalogue des produits", "Explorer les produits", PLACEHOLDER_FXML);
            addNav(navStats, "Mes favorites", "Produits favoris", PLACEHOLDER_FXML);
            addNav(navSettings, "Mes commandes", "Suivi commandes", ACHETEUR_COMMANDES_FXML);
            hideNav(navSupport);
        } else if (role == Role.vendeur) {
            addNav(navUsers, "Dashboard", "Accueil vendeur", PLACEHOLDER_FXML);
            addNav(navProducts, "Ma boutique", "Gestion boutique", PLACEHOLDER_FXML);
            addNav(navOrders, "Les commandes", "Commandes recues", VENDEUR_COMMANDES_FXML);
            addNav(navStats, "Conseil AI", "Recommandations intelligentes", PLACEHOLDER_FXML);
            addNav(navSettings, "Campagne marketing", "Campagnes commerciales", PLACEHOLDER_FXML);
            addNav(navSupport, "Mes fournisseurs", "Gestion fournisseurs", PLACEHOLDER_FXML);
        } else if (role == Role.livreur) {
            addNav(navUsers, "Dashboard", "Accueil livreur", PLACEHOLDER_FXML);
            addNav(navProducts, "Mes livreurs", "Gestion livreurs", PLACEHOLDER_FXML);
            addNav(navOrders, "Livraisons", "Suivi livraisons", PLACEHOLDER_FXML);
            hideNav(navStats);
            hideNav(navSettings);
            hideNav(navSupport);
        }

        if (!navItems.isEmpty()) {
            loadTab(defaultNavForRole(role));
        }
    }

    private NavItem defaultNavForRole(Role role) {
        Button defaultButton = switch (role) {
            case admin, acheteur -> navSettings;
            case vendeur -> navOrders;
            default -> navItems.get(0).button();
        };
        return navItems.stream()
                .filter(item -> item.button() == defaultButton)
                .findFirst()
                .orElse(navItems.get(0));
    }

    private void addNav(Button button, String title, String subtitle, String fxmlPath) {
        addNav(button, title, subtitle, fxmlPath, null);
    }

    private void addNav(Button button, String title, String subtitle, String fxmlPath, Consumer<Object> afterLoad) {
        button.setVisible(true);
        button.setManaged(true);
        setNavLabel(button, title);
        button.setTooltip(new Tooltip(subtitle));

        NavItem item = new NavItem(button, title, subtitle, fxmlPath, afterLoad);
        navItems.add(item);
        button.setOnAction(e -> {
            loadTab(item);
            bounceNode(button);
        });
    }

    private void hideNav(Button button) {
        button.setVisible(false);
        button.setManaged(false);
        button.setOnAction(null);
    }

    private void resetNavVisibility() {
        for (Button button : allNavButtons()) {
            button.setVisible(true);
            button.setManaged(true);
            button.getStyleClass().remove("nav-btn-active");
        }
    }

    private void loadTab(NavItem item) {
        setActiveNav(item.button());
        headerTitle.setText(item.title());
        headerSubtitle.setText(item.subtitle());

        try {
            java.net.URL fxml = getClass().getResource(item.fxmlPath());
            if (fxml == null) {
                showContentError(item.title(), "FXML introuvable: " + item.fxmlPath());
                return;
            }

            FXMLLoader loader = new FXMLLoader(fxml);
            Parent content = loader.load();
            currentModuleController = loader.getController();
            if (item.afterLoad() != null) {
                item.afterLoad().accept(currentModuleController);
            }
            applyThemeToContent(content);
            applyThemeToCurrentModule();
            contentContainer.getChildren().setAll(content);
        } catch (Exception ex) {
            showContentError(item.title(), "Impossible de charger le module.");
            ex.printStackTrace();
        }
    }

    private void showContentError(String title, String message) {
        Label heading = new Label(title);
        heading.getStyleClass().add("stat-value");

        Label detail = new Label(message);
        detail.setWrapText(true);
        detail.getStyleClass().add("stat-label");

        VBox card = new VBox(12, heading, detail);
        card.getStyleClass().add("stat-card");
        card.setMaxWidth(720);

        StackPane wrapper = new StackPane(card);
        wrapper.getStyleClass().add("content-area");
        contentContainer.getChildren().setAll(wrapper);
    }

    private void setActiveNav(Button active) {
        for (Button button : allNavButtons()) {
            button.getStyleClass().remove("nav-btn-active");
        }
        if (!active.getStyleClass().contains("nav-btn-active")) {
            active.getStyleClass().add("nav-btn-active");
        }
    }

    private Button[] allNavButtons() {
        return new Button[]{navUsers, navProducts, navOrders, navStats, navSettings, navSupport};
    }

    private void setNavLabel(Button button, String title) {
        if (button.getGraphic() instanceof HBox hbox) {
            for (Node node : hbox.getChildren()) {
                if (node instanceof Label label && label.getStyleClass().contains("nav-label")) {
                    label.setText(title);
                    return;
                }
            }
        }
        button.setText(title);
    }

    private void openProfileEdit(User user) {
        if (user == null) {
            return;
        }

        profileDetailTitle.setText("Modifier mon profil");
        profileDetailNom.setText(value(user.getNom()));
        profileDetailPrenom.setText(value(user.getPrenom()));
        profileDetailEmail.setText(value(user.getE_mail()));
        profileDetailTel.setText(value(user.getNum_tel()));
        profileDetailDate.setText(value(user.getDate_naiss()));
        profileDetailRole.setValue(user.getRole() != null ? user.getRole().name() : "");
        profileDetailStatus.setValue(user.getStatus() != null ? user.getStatus().name() : "");
        profileDetailMessage.setVisible(false);
        profileDetailMessage.setManaged(false);

        String letter = value(user.getNom()).isEmpty() ? "?" : user.getNom().substring(0, 1).toUpperCase();
        profileDetailAvatarLetter.setText(letter);

        if (user.getImage() != null && !user.getImage().isBlank()) {
            try {
                profileDetailAvatar.setClip(new Circle(38, 38, 38));
                profileDetailAvatar.setImage(new Image("file:" + user.getImage(), 76, 76, false, true));
                profileDetailAvatarLetter.setVisible(false);
            } catch (Exception e) {
                profileDetailAvatar.setImage(null);
                profileDetailAvatarLetter.setVisible(true);
            }
        } else {
            profileDetailAvatar.setImage(null);
            profileDetailAvatarLetter.setVisible(true);
        }

        profileOverlayPane.setVisible(true);
        profileOverlayPane.setManaged(true);
        profileOverlayPane.setOpacity(0);
        profileDetailCard.setScaleX(0.9);
        profileDetailCard.setScaleY(0.9);

        FadeTransition fade = new FadeTransition(Duration.millis(200), profileOverlayPane);
        fade.setToValue(1);
        ScaleTransition scale = new ScaleTransition(Duration.millis(250), profileDetailCard);
        scale.setToX(1);
        scale.setToY(1);
        scale.setInterpolator(Interpolator.EASE_OUT);
        new ParallelTransition(fade, scale).play();
    }

    private void closeProfileEdit() {
        FadeTransition fade = new FadeTransition(Duration.millis(150), profileOverlayPane);
        fade.setToValue(0);
        ScaleTransition scale = new ScaleTransition(Duration.millis(150), profileDetailCard);
        scale.setToX(0.9);
        scale.setToY(0.9);
        ParallelTransition transition = new ParallelTransition(fade, scale);
        transition.setOnFinished(e -> {
            profileOverlayPane.setVisible(false);
            profileOverlayPane.setManaged(false);
            profileDetailCard.setScaleX(1);
            profileDetailCard.setScaleY(1);
        });
        transition.play();
    }

    private void saveProfileEdit() {
        if (connectedUser == null) {
            return;
        }

        try {
            connectedUser.setNom(profileDetailNom.getText().trim());
            connectedUser.setPrenom(profileDetailPrenom.getText().trim());
            connectedUser.setE_mail(profileDetailEmail.getText().trim());
            connectedUser.setNum_tel(profileDetailTel.getText().trim());
            connectedUser.setDate_naiss(profileDetailDate.getText().trim());

            userCRUD.updateUserProfile(connectedUser);
            SessionManager.getInstance().setConnectedUser(connectedUser);
            updateHeaderUser(connectedUser);
            showProfileMessage("Profil mis a jour avec succes !", false);

            PauseTransition closeDelay = new PauseTransition(Duration.millis(800));
            closeDelay.setOnFinished(e -> closeProfileEdit());
            closeDelay.play();
        } catch (SQLException ex) {
            showProfileMessage("Erreur: " + ex.getMessage(), true);
        }
    }

    private void showProfileMessage(String msg, boolean isError) {
        profileDetailMessage.setText(msg);
        profileDetailMessage.setVisible(true);
        profileDetailMessage.setManaged(true);
        profileDetailMessage.getStyleClass().removeAll("detail-msg-error", "detail-msg-success");
        profileDetailMessage.getStyleClass().add(isError ? "detail-msg-error" : "detail-msg-success");
    }

    private String value(String val) {
        return val == null ? "" : val;
    }

    private void setDarkMode(boolean dark) {
        if (dark) {
            if (!rootPane.getStyleClass().contains("dark")) {
                rootPane.getStyleClass().add("dark");
            }
        } else {
            rootPane.getStyleClass().remove("dark");
        }
        if (themeIcon != null) {
            themeIcon.setText(dark ? "\u2600" : "\u263D");
        }
        if (themeLabel != null) {
            themeLabel.setText(dark ? "Mode Jour" : "Mode Nuit");
        }
        refreshHeaderIcons();
        applyThemeToCurrentModule();
    }

    private void applyThemeToCurrentModule() {
        if (contentContainer != null) {
            for (Node child : contentContainer.getChildren()) {
                applyThemeToContent(child);
            }
        }
        if (currentModuleController instanceof AssistantIAController assistant) {
            assistant.applyTheme(isDarkMode);
        }
    }

    private void applyThemeToContent(Node content) {
        if (content == null) {
            return;
        }
        contentContainer.getStyleClass().remove("light-mode");
        content.getStyleClass().remove("light-mode");
        if (!isDarkMode) {
            if (!contentContainer.getStyleClass().contains("light-mode")) {
                contentContainer.getStyleClass().add("light-mode");
            }
            if (!content.getStyleClass().contains("light-mode")) {
                content.getStyleClass().add("light-mode");
            }
        }
    }

    private void refreshHeaderIcons() {
        String iconColor = isDarkMode ? "#A5B4FC" : "#4338CA";
        applyNotificationBell(iconColor);
        UiIcons.setButtonIcon(cartBtn, UiIcons.Icon.CART, iconColor, 18, "Panier");
    }

    private void applyNotificationBell(String iconColor) {
        if (notifBtn == null) return;

        int unread = 0;
        if (connectedUser != null) {
            unread = notificationService.countNonLues(connectedUser.getId());
        }

        StackPane bell = UiIcons.icon(UiIcons.Icon.BELL, iconColor, 18);
        StackPane graphic = new StackPane(bell);
        graphic.setPickOnBounds(false);

        if (unread > 0) {
            Label badge = new Label(unread > 99 ? "99+" : String.valueOf(unread));
            badge.getStyleClass().add("notif-counter-badge");
            StackPane.setAlignment(badge, javafx.geometry.Pos.TOP_RIGHT);
            graphic.getChildren().add(badge);
        }

        notifBtn.setText("");
        notifBtn.setGraphic(graphic);
        notifBtn.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        notifBtn.setTooltip(new Tooltip(
                unread > 0 ? "Notifications (" + unread + " non lue(s))" : "Notifications"
        ));
    }

    private void startNotificationAutoRefresh() {
        if (notificationRefreshTimeline != null) {
            notificationRefreshTimeline.stop();
        }
        notificationRefreshTimeline = new Timeline(
                new KeyFrame(Duration.seconds(15), e -> refreshHeaderIcons())
        );
        notificationRefreshTimeline.setCycleCount(Animation.INDEFINITE);
        notificationRefreshTimeline.play();
    }

    private void stopNotificationAutoRefresh() {
        if (notificationRefreshTimeline != null) {
            notificationRefreshTimeline.stop();
            notificationRefreshTimeline = null;
        }
    }

    private void openNotificationCenter() {
        if (connectedUser == null) return;

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Centre de notifications");
        dialog.setHeaderText("Notifications de " + connectedUser.getRole().name());

        DialogPane pane = dialog.getDialogPane();
        if (!pane.getStyleClass().contains("dashboard-dialog")) {
            pane.getStyleClass().add("dashboard-dialog");
        }
        java.net.URL css = getClass().getResource("/styles/user_auth/back/dashboard.css");
        if (css != null && !pane.getStylesheets().contains(css.toExternalForm())) {
            pane.getStylesheets().add(css.toExternalForm());
        }
        if (isDarkMode) {
            pane.setStyle("-card: #14122E; -card-b: rgba(165,180,252,0.18); -inp: rgba(255,255,255,0.06); -inp-b: rgba(165,180,252,0.16); -t1: #F1F0FF; -t2: #A5B4FC; -b600: #6366F1; -b700: #4F46E5;");
        } else {
            pane.setStyle("-card: #FFFFFF; -card-b: rgba(99,102,241,0.16); -inp: #EEF2FF; -inp-b: rgba(99,102,241,0.16); -t1: #1E1B4B; -t2: #4F46E5; -b600: #4F46E5; -b700: #4338CA;");
        }

        ListView<Notification> notifList = new ListView<>();
        notifList.getStyleClass().add("notif-center-list");
        notifList.setPrefHeight(380);
        notifList.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Notification n, boolean empty) {
                super.updateItem(n, empty);
                if (empty || n == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }

                Label title = new Label((n.isRead() ? "" : "● ") + n.getTitre());
                title.getStyleClass().add("notif-center-title");

                Label message = new Label(n.getMessage());
                message.getStyleClass().add("notif-center-message");
                message.setWrapText(true);

                Label time = new Label(formatNotificationDate(n.getDateCreation()));
                time.getStyleClass().add("notif-center-time");

                VBox box = new VBox(3, title, message, time);
                box.getStyleClass().add("notif-center-item");
                setGraphic(box);
            }
        });

        Runnable reload = () -> notifList.setItems(FXCollections.observableArrayList(
                notificationService.getByUser(connectedUser.getId())
        ));
        reload.run();

        notifList.setOnMouseClicked(e -> {
            Notification selected = notifList.getSelectionModel().getSelectedItem();
            if (selected == null) return;
            if (!selected.isRead()) {
                notificationService.marquerCommeLue(selected.getNotificationId());
                reload.run();
                refreshHeaderIcons();
            }
        });

        Button btnRefresh = new Button("Actualiser");
        Button btnMarkAll = new Button("Tout marquer comme lu");
        btnRefresh.getStyleClass().add("detail-cancel-btn");
        btnMarkAll.getStyleClass().add("detail-save-btn");
        btnRefresh.setOnAction(e -> reload.run());
        btnMarkAll.setOnAction(e -> {
            notificationService.marquerToutesCommeLues(connectedUser.getId());
            reload.run();
            refreshHeaderIcons();
        });

        HBox actions = new HBox(10, btnRefresh, btnMarkAll);
        VBox content = new VBox(12, notifList, actions);
        content.getStyleClass().add("notif-center-wrap");
        pane.setContent(content);
        pane.getButtonTypes().setAll(ButtonType.CLOSE);

        dialog.showAndWait();
        refreshHeaderIcons();
    }

    private String formatNotificationDate(LocalDateTime dateTime) {
        if (dateTime == null) return "";
        java.time.Duration diff = java.time.Duration.between(dateTime, LocalDateTime.now());
        if (diff.toMinutes() < 1) return "à l'instant";
        if (diff.toHours() < 1) return "il y a " + diff.toMinutes() + " min";
        if (diff.toDays() < 1) return "il y a " + diff.toHours() + " h";
        return dateTime.format(NOTIF_DT_FMT);
    }

    private void navigateToLogin() {
        ButtonType confirm = new ButtonType("Se deconnecter", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancel = new ButtonType("Annuler", ButtonBar.ButtonData.CANCEL_CLOSE);
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Deconnexion");
        alert.setHeaderText("Confirmer la deconnexion");
        alert.setContentText("Voulez-vous vous deconnecter ?");
        alert.getButtonTypes().setAll(confirm, cancel);
        styleDashboardDialog(alert);

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == confirm) {
            stopNotificationAutoRefresh();
            SessionManager.getInstance().logout();
            redirectToLoginImmediately();
        }
    }

    private void redirectToLoginImmediately() {
        stopNotificationAutoRefresh();
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/FXML/user_auth/login/login_view.fxml"));
            Parent root = loader.load();
            javafx.scene.Scene scene = rootPane.getScene();
            scene.getStylesheets().clear();
            java.net.URL css = getClass().getResource("/styles/user_auth/login/login.css");
            if (css != null) {
                scene.getStylesheets().add(css.toExternalForm());
            }
            scene.setRoot(root);
        } catch (Exception ex) {
            showError("Erreur", "Impossible de charger la page de connexion.");
            ex.printStackTrace();
        }
    }

    private void showInfo(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setContentText(content);
        styleDashboardDialog(alert);
        alert.showAndWait();
    }

    private void showError(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setContentText(content);
        styleDashboardDialog(alert);
        alert.showAndWait();
    }

    private void styleDashboardDialog(Alert alert) {
        alert.setGraphic(null);
        DialogPane pane = alert.getDialogPane();
        if (!pane.getStyleClass().contains("dashboard-dialog")) {
            pane.getStyleClass().add("dashboard-dialog");
        }
        java.net.URL css = getClass().getResource("/styles/user_auth/back/dashboard.css");
        if (css != null && !pane.getStylesheets().contains(css.toExternalForm())) {
            pane.getStylesheets().add(css.toExternalForm());
        }
        if (isDarkMode) {
            pane.setStyle("-card: #14122E; -card-b: rgba(165,180,252,0.18); -inp: rgba(255,255,255,0.06); -inp-b: rgba(165,180,252,0.16); -t1: #F1F0FF; -t2: #A5B4FC; -b600: #6366F1; -b700: #4F46E5;");
        } else {
            pane.setStyle("-card: #FFFFFF; -card-b: rgba(99,102,241,0.16); -inp: #EEF2FF; -inp-b: rgba(99,102,241,0.16); -t1: #1E1B4B; -t2: #4F46E5; -b600: #4F46E5; -b700: #4338CA;");
        }
    }

    private void playEntrance() {
        sidebar.setTranslateX(-260);
        sidebar.setOpacity(0);
        TranslateTransition sidebarSlide = new TranslateTransition(Duration.millis(500), sidebar);
        sidebarSlide.setToX(0);
        FadeTransition sidebarFade = new FadeTransition(Duration.millis(500), sidebar);
        sidebarFade.setToValue(1);
        sidebarFade.setInterpolator(Interpolator.EASE_OUT);
        new ParallelTransition(sidebarSlide, sidebarFade).play();

        headerBar.setOpacity(0);
        FadeTransition headerFade = new FadeTransition(Duration.millis(600), headerBar);
        headerFade.setFromValue(0);
        headerFade.setToValue(1);
        headerFade.setDelay(Duration.millis(200));
        headerFade.play();
    }

    private void bounceNode(Node node) {
        if (node == null) {
            return;
        }
        ScaleTransition scale = new ScaleTransition(Duration.millis(130), node);
        scale.setFromX(0.88);
        scale.setFromY(0.88);
        scale.setToX(1.0);
        scale.setToY(1.0);
        scale.setInterpolator(Interpolator.EASE_OUT);
        scale.play();
    }

    private void toggleSidebar() {
        sidebarOpen = !sidebarOpen;

        if (sidebarOpen) {
            sidebar.setVisible(true);
            sidebar.setManaged(true);
            javafx.application.Platform.runLater(() -> {
                for (Button button : allNavButtons()) {
                    if (button.getStyleClass().contains("nav-btn-active")) {
                        button.getStyleClass().remove("nav-btn-active");
                        button.getStyleClass().add("nav-btn-active");
                    }
                }
                rootPane.requestFocus();
            });

            TranslateTransition slide = new TranslateTransition(Duration.millis(260), sidebar);
            slide.setFromX(-260);
            slide.setToX(0);
            slide.setInterpolator(Interpolator.EASE_OUT);
            slide.play();
        } else {
            TranslateTransition slide = new TranslateTransition(Duration.millis(220), sidebar);
            slide.setFromX(0);
            slide.setToX(-260);
            slide.setInterpolator(Interpolator.EASE_IN);
            slide.setOnFinished(e -> {
                sidebar.setVisible(false);
                sidebar.setManaged(false);
            });
            slide.play();
        }
    }

    private void updateHeaderUser(User user) {
        if (user == null) {
            return;
        }

        String nom = value(user.getNom());
        String prenom = value(user.getPrenom());
        String fullName = (nom + " " + prenom).trim();
        adminNameLabel.setText(fullName.isEmpty() ? "Utilisateur" : fullName);
        adminRoleLabel.setText(user.getRole() != null ? user.getRole().name() : "");
        adminAvatarLetter.setText(nom.isEmpty() ? "U" : nom.substring(0, 1).toUpperCase());

        if (profileMenu != null) {
            profileMenu.setText(prenom.isBlank() ? "Profil" : "Profil " + prenom);
        }

        if (user.getImage() != null && !user.getImage().isBlank()) {
            try {
                adminAvatar.setImage(new Image("file:" + user.getImage(), 38, 38, false, true));
                adminAvatarLetter.setVisible(false);
            } catch (Exception ignored) {
                adminAvatarLetter.setVisible(true);
            }
        } else {
            adminAvatar.setImage(null);
            adminAvatarLetter.setVisible(true);
        }
    }

    private record NavItem(Button button, String title, String subtitle, String fxmlPath, Consumer<Object> afterLoad) {}
}
