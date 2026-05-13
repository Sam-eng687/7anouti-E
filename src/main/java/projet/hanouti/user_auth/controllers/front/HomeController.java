package projet.hanouti.user_auth.controllers.front;

import javafx.animation.*;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.shape.Circle;
import javafx.util.Duration;
import projet.hanouti.user_auth.entities.User;

import projet.hanouti.common.utils.SessionManager;

import java.util.Optional;

public class HomeController {

    @FXML private AnchorPane rootPane;

    // Topnav
    @FXML private HBox topNav;
    @FXML private ImageView navLogo;
    @FXML private Label navLogoText;
    @FXML private Button navHome, navModule1, navModule2, navModule3;
    @FXML private Button navModule4, navModule5, navModule6;
    @FXML private Button themeBtn, logoutBtn, avatarBtn, profileShortcut;
    @FXML private Label themeIcon, themeLabel;
    @FXML private Label userNameLabel, userRoleLabel, userAvatarLetter;
    @FXML private ImageView userAvatar;

    // Content
    @FXML private Label heroGreeting, statRoleLabel;
    @FXML private Button heroCta1, heroCta2;
    @FXML private VBox card1, card2, card3, card4, card5, card6;

    private boolean isDarkMode = true;
    private User currentUser;

    @FXML
    public void initialize() {

        if (!rootPane.getStyleClass().contains("front-root"))
            rootPane.getStyleClass().add("front-root");
        setDarkMode(true);

        themeBtn.setOnAction(e -> {
            isDarkMode = !isDarkMode;
            setDarkMode(isDarkMode);
            bounceNode(themeBtn);
        });

        // Session check
        currentUser = SessionManager.getInstance().getConnectedUser();
        if (currentUser == null) { redirectToLogin(); return; }

        // Logo
        try {
            java.io.InputStream s = getClass().getResourceAsStream("/images/user_auth/logo.png");
            if (s != null) {
                navLogo.setImage(new Image(s));
                navLogoText.setVisible(false);
                navLogoText.setManaged(false);
            }
        } catch (Exception ignored) {}

        // Avatar clip - matches 34px fitWidth
        userAvatar.setClip(new Circle(17, 17, 17));

        // Load user info
        loadUserInfo();

        // Nav setup
        setupNav();

        // Actions
        logoutBtn.setOnAction(e -> confirmLogout());
        avatarBtn.setOnAction(e -> navigateToProfile());
        heroCta2.setOnAction(e -> navigateToProfile());
        if (profileShortcut != null) profileShortcut.setOnAction(e -> navigateToProfile());
        heroCta1.setOnAction(e -> bounceNode(heroCta1));

        // Entrance animation
        playEntrance();
    }

    // =================== USER INFO ===================

    private void loadUserInfo() {
        String nom    = currentUser.getNom()    != null ? currentUser.getNom()    : "";
        String prenom = currentUser.getPrenom() != null ? currentUser.getPrenom() : "";
        String full   = (nom + " " + prenom).trim();

        userNameLabel.setText(full.isEmpty() ? "Utilisateur" : full);
        userRoleLabel.setText(currentUser.getRole() != null ? currentUser.getRole().name() : "acheteur");
        if (statRoleLabel != null)
            statRoleLabel.setText(currentUser.getRole() != null ? currentUser.getRole().name() : "Acheteur");

        String letter = nom.isEmpty() ? (prenom.isEmpty() ? "?" : prenom.substring(0, 1).toUpperCase())
                : nom.substring(0, 1).toUpperCase();
        userAvatarLetter.setText(letter);

        if (currentUser.getImage() != null && !currentUser.getImage().isBlank()) {
            try { userAvatar.setImage(new Image("file:" + currentUser.getImage(), 26, 26, false, true)); }
            catch (Exception ignored) {}
        }

        String firstName = prenom.isEmpty() ? nom : prenom;
        heroGreeting.setText("Bonjour, " + capitalize(firstName) + " !");
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return s.substring(0, 1).toUpperCase() + s.substring(1).toLowerCase();
    }

    // =================== THEME ===================

    private void setDarkMode(boolean dark) {
        if (dark) {
            if (!rootPane.getStyleClass().contains("dark")) rootPane.getStyleClass().add("dark");
            themeIcon.setText("\u2600");
            themeLabel.setText("Jour");
        } else {
            rootPane.getStyleClass().remove("dark");
            themeIcon.setText("\u263D");
            themeLabel.setText("Nuit");
        }
    }

    // =================== NAV ===================

    private void setupNav() {
        Button[] allBtns = {navHome, navModule1, navModule2, navModule3,
                navModule4, navModule5, navModule6};

        for (Button btn : allBtns) {
            btn.setOnAction(e -> {
                for (Button b : allBtns) {
                    b.getStyleClass().remove("topnav-active");
                    b.getStyleClass().remove("button.topnav-active");
                }
                if (!btn.getStyleClass().contains("topnav-active"))
                    btn.getStyleClass().add("topnav-active");
                bounceNode(btn);
            });
        }

        navHome.setOnAction(e -> {
            setActiveNav(allBtns, navHome);
        });
    }

    private void setActiveNav(Button[] all, Button active) {
        for (Button b : all) b.getStyleClass().remove("topnav-active");
        active.getStyleClass().add("topnav-active");
    }

    // =================== NAVIGATION ===================

    private void navigateToProfile() {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                    getClass().getResource("/FXML/user_auth/front/profile_view.fxml"));
            javafx.scene.Parent root = loader.load();
            javafx.scene.Scene scene = rootPane.getScene();
            scene.getStylesheets().clear();
            addStylesheets(scene);
            scene.setRoot(root);
        } catch (Exception ex) { ex.printStackTrace(); }
    }

    private void confirmLogout() {
        Alert c = new Alert(Alert.AlertType.CONFIRMATION);
        c.setTitle("Deconnexion"); c.setHeaderText("Voulez-vous vous deconnecter ?");
        Optional<ButtonType> r = c.showAndWait();
        if (r.isPresent() && r.get() == ButtonType.OK) {
            SessionManager.getInstance().logout();
            redirectToLogin();
        }
    }

    private void redirectToLogin() {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                    getClass().getResource("/FXML/user_auth/login/login_view.fxml"));
            javafx.scene.Parent root = loader.load();
            javafx.scene.Scene scene = rootPane.getScene();
            scene.getStylesheets().clear();
            java.net.URL css = getClass().getResource("/styles/user_auth/login/login.css");
            if (css != null) scene.getStylesheets().add(css.toExternalForm());
            scene.setRoot(root);
        } catch (Exception ex) { ex.printStackTrace(); }
    }

    private void addStylesheets(javafx.scene.Scene scene) {
        java.net.URL frontCss = getClass().getResource("/styles/user_auth/front/front.css");
        if (frontCss != null) scene.getStylesheets().add(frontCss.toExternalForm());
        java.net.URL loginCss = getClass().getResource("/styles/user_auth/login/login.css");
        if (loginCss != null) scene.getStylesheets().add(loginCss.toExternalForm());
    }

    // =================== ANIMATIONS ===================

    private void playEntrance() {
        // Topnav slide down
        topNav.setTranslateY(-60); topNav.setOpacity(0);
        TranslateTransition tt = new TranslateTransition(Duration.millis(450), topNav); tt.setToY(0);
        FadeTransition ft = new FadeTransition(Duration.millis(450), topNav); ft.setToValue(1);
        ft.setInterpolator(Interpolator.EASE_OUT);
        new ParallelTransition(tt, ft).play();

        // Module cards stagger
        VBox[] cards = {card1, card2, card3, card4, card5, card6};
        for (int i = 0; i < cards.length; i++) {
            final VBox card = cards[i];
            card.setOpacity(0); card.setTranslateY(24);
            FadeTransition f = new FadeTransition(Duration.millis(380), card);
            f.setFromValue(0); f.setToValue(1);
            TranslateTransition t = new TranslateTransition(Duration.millis(380), card);
            t.setFromY(24); t.setToY(0); t.setInterpolator(Interpolator.EASE_OUT);
            ParallelTransition pt = new ParallelTransition(f, t);
            pt.setDelay(Duration.millis(250 + i * 60));
            pt.play();
        }
    }

    private void bounceNode(Node n) {
        if (n == null) return;
        ScaleTransition sc = new ScaleTransition(Duration.millis(120), n);
        sc.setFromX(0.90); sc.setFromY(0.90); sc.setToX(1.0); sc.setToY(1.0);
        sc.setInterpolator(Interpolator.EASE_OUT); sc.play();
    }
}