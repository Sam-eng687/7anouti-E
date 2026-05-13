package projet.hanouti.user_auth.controllers.front;

import javafx.animation.*;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.shape.Circle;
import javafx.stage.FileChooser;
import javafx.util.Duration;

import projet.hanouti.user_auth.entities.User;
import projet.hanouti.user_auth.services.UserCRUD;
import projet.hanouti.common.utils.FormValidator;
import projet.hanouti.common.utils.SessionManager;

import java.io.File;
import java.sql.SQLException;
import java.util.Optional;

public class ProfileController {

    @FXML private AnchorPane rootPane;

    // Topnav
    @FXML private HBox topNav;
    @FXML private ImageView navLogo;
    @FXML private Button navHome, navModule1, navModule2, navModule3;
    @FXML private Button navModule4, navModule5, navModule6;
    @FXML private Button themeBtn, logoutBtn, avatarBtn;
    @FXML private Label themeIcon, themeLabel;
    @FXML private Label userNameLabel, userRoleLabel, userAvatarLetter;
    @FXML private ImageView userAvatar;

    // Form
    @FXML private TextField nomField, prenomField, emailField, telField, dateField;
    @FXML private PasswordField newPassField, confirmPassField;
    @FXML private Label emailIcon;
    @FXML private Label errNom, errPrenom, errEmail, errTel, errPass, errConfirm;
    @FXML private Label successMsg, errorMsg;
    @FXML private Button saveBtn, cancelBtn, changeAvatarBtn;

    // Avatar form
    @FXML private StackPane profileAvatarContainer;
    @FXML private ImageView profileAvatarView;
    @FXML private Label profileAvatarLetter;

    // Preview
    @FXML private ImageView previewAvatar;
    @FXML private Label previewAvatarLetter, previewName, previewRoleBadge;
    @FXML private Label previewEmail, previewTel, previewDate, previewStatus;

    private boolean isDarkMode = true;
    private User currentUser;
    private File selectedImageFile = null;
    private final UserCRUD userCRUD = new UserCRUD();

    @FXML
    public void initialize() {

        if (!rootPane.getStyleClass().contains("front-root"))
            rootPane.getStyleClass().add("front-root");
        setDarkMode(true);

        themeBtn.setOnAction(e -> {
            isDarkMode = !isDarkMode;
            setDarkMode(isDarkMode);
        });

        // Session check
        currentUser = SessionManager.getInstance().getConnectedUser();
        if (currentUser == null) { redirectToLogin(); return; }

        // Logo
        try {
            java.io.InputStream s = getClass().getResourceAsStream("/images/user_auth/logo.png");
            if (s != null) navLogo.setImage(new Image(s));
        } catch (Exception ignored) {}

        // Avatar clips
        userAvatar.setClip(new Circle(17, 17, 17));
        profileAvatarView.setClip(new Circle(41, 41, 41));
        previewAvatar.setClip(new Circle(48, 48, 48));

        // Load data
        loadUserData();

        // Validation temps reel
        FormValidator.setupEditValidation(
                nomField, errNom, prenomField, errPrenom, emailField, errEmail, telField, errTel);

        newPassField.textProperty().addListener((o, a, val) -> {
            if (val == null || val.isBlank()) {
                FormValidator.clearError(errPass);
                FormValidator.clearError(errConfirm);
            } else {
                FormValidator.validatePassword(newPassField, errPass);
                if (!confirmPassField.getText().isBlank())
                    FormValidator.validateConfirmPassword(confirmPassField, newPassField, errConfirm);
            }
        });
        confirmPassField.textProperty().addListener((o, a, val) -> {
            if (!newPassField.getText().isBlank())
                FormValidator.validateConfirmPassword(confirmPassField, newPassField, errConfirm);
        });

        // Live preview
        nomField.textProperty().addListener((o, a, b)    -> updatePreview());
        prenomField.textProperty().addListener((o, a, b) -> updatePreview());
        emailField.textProperty().addListener((o, a, b)  -> updatePreview());
        telField.textProperty().addListener((o, a, b)    -> updatePreview());

        // Nav + actions
        setupNav();
        setupAvatarInteraction();
        saveBtn.setOnAction(e -> onSave());
        cancelBtn.setOnAction(e -> loadUserData());
        changeAvatarBtn.setOnAction(e -> chooseImage());
        logoutBtn.setOnAction(e -> confirmLogout());
        avatarBtn.setOnAction(e -> {}); // already on this page

        applySaveBtnStyle();
        playEntrance();
    }

    // =================== LOAD DATA ===================

    private void loadUserData() {
        nomField.setText(s(currentUser.getNom()));
        prenomField.setText(s(currentUser.getPrenom()));
        emailField.setText(s(currentUser.getE_mail()));
        telField.setText(s(currentUser.getNum_tel()));
        dateField.setText(s(currentUser.getDate_naiss()));
        emailIcon.setText("@");

        String full = (s(currentUser.getNom()) + " " + s(currentUser.getPrenom())).trim();
        userNameLabel.setText(full.isEmpty() ? "Utilisateur" : full);
        userRoleLabel.setText(currentUser.getRole() != null ? currentUser.getRole().name() : "acheteur");

        String letter = s(currentUser.getNom()).isEmpty() ? "?" : currentUser.getNom().substring(0, 1).toUpperCase();
        userAvatarLetter.setText(letter);
        profileAvatarLetter.setText(letter);

        loadAvatarImages(currentUser.getImage());
        updatePreview();

        boolean active = currentUser.getStatus() != null
                && currentUser.getStatus() == projet.hanouti.user_auth.enums.Status.Unbanned;
        previewStatus.setText(active ? "Actif" : "Banni");
        previewStatus.getStyleClass().removeAll("profile-status-active", "profile-status-banned");
        previewStatus.getStyleClass().add(active ? "profile-status-active" : "profile-status-banned");
        previewRoleBadge.setText(currentUser.getRole() != null ? currentUser.getRole().name() : "acheteur");

        hideMessages();
    }

    private String s(String val) { return val != null ? val : ""; }

    private void loadAvatarImages(String path) {
        if (path != null && !path.isBlank()) {
            try {
                profileAvatarView.setImage(new Image("file:" + path, 82, 82, false, true));
                profileAvatarLetter.setVisible(false);
                userAvatar.setImage(new Image("file:" + path, 26, 26, false, true));
                userAvatarLetter.setVisible(false);
                previewAvatar.setImage(new Image("file:" + path, 96, 96, false, true));
                previewAvatarLetter.setVisible(false);
                return;
            } catch (Exception ignored) {}
        }
        profileAvatarView.setImage(null);  profileAvatarLetter.setVisible(true);
        previewAvatar.setImage(null);       previewAvatarLetter.setVisible(true);
    }

    // =================== LIVE PREVIEW ===================

    private void updatePreview() {
        String nom    = nomField.getText() == null    ? "" : nomField.getText().trim();
        String prenom = prenomField.getText() == null ? "" : prenomField.getText().trim();
        String email  = emailField.getText() == null  ? "" : emailField.getText().trim();
        String tel    = telField.getText() == null    ? "" : telField.getText().trim();

        String full = (nom + " " + prenom).trim();
        previewName.setText(full.isEmpty() ? "Votre nom" : full);

        if (selectedImageFile == null && (s(currentUser.getImage()).isBlank())) {
            String letter = nom.isEmpty() ? "?" : nom.substring(0, 1).toUpperCase();
            previewAvatarLetter.setText(letter);
            profileAvatarLetter.setText(letter);
            userAvatarLetter.setText(letter);
        }

        previewEmail.setText(email.isBlank() ? "\u2014" : email);
        previewEmail.setStyle(isValidEmail(email) ? "-fx-text-fill: #10B981;" : "");
        previewTel.setText(tel.isBlank() ? "\u2014" : tel);
        previewDate.setText(s(dateField.getText()).isBlank() ? "\u2014" : dateField.getText());
        userNameLabel.setText(full.isEmpty() ? "Utilisateur" : full);
    }

    // =================== AVATAR ===================

    private void setupAvatarInteraction() {
        String baseStyle =
                "-fx-background-color: rgba(99,102,241,0.08);" +
                        "-fx-background-radius: 50;" +
                        "-fx-border-color: rgba(99,102,241,0.20);" +
                        "-fx-border-radius: 50;" +
                        "-fx-border-width: 2;" +
                        "-fx-border-style: dashed;" +
                        "-fx-cursor: hand;";
        String hoverStyle =
                "-fx-background-color: rgba(99,102,241,0.16);" +
                        "-fx-background-radius: 50;" +
                        "-fx-border-color: rgba(99,102,241,0.40);" +
                        "-fx-border-radius: 50;" +
                        "-fx-border-width: 2;" +
                        "-fx-border-style: dashed;" +
                        "-fx-cursor: hand;";
        String selectedStyle =
                "-fx-background-color: transparent;" +
                        "-fx-background-radius: 50;" +
                        "-fx-border-color: rgba(16,185,129,0.50);" +
                        "-fx-border-radius: 50;" +
                        "-fx-border-width: 2;" +
                        "-fx-border-style: solid;" +
                        "-fx-cursor: hand;";

        profileAvatarContainer.setStyle(baseStyle);
        profileAvatarContainer.setOnMouseClicked(e -> chooseImage());
        profileAvatarContainer.setOnMouseEntered(e -> profileAvatarContainer.setStyle(hoverStyle));
        profileAvatarContainer.setOnMouseExited(e ->
                profileAvatarContainer.setStyle(selectedImageFile != null ? selectedStyle : baseStyle));
    }

    private void chooseImage() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Choisir une photo de profil");
        fc.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.bmp"));
        File file = fc.showOpenDialog(rootPane.getScene().getWindow());
        if (file != null) {
            selectedImageFile = file;
            String uri = file.toURI().toString();
            profileAvatarView.setImage(new Image(uri, 82, 82, false, true));
            profileAvatarLetter.setVisible(false);
            previewAvatar.setImage(new Image(uri, 96, 96, false, true));
            previewAvatarLetter.setVisible(false);
            userAvatar.setImage(new Image(uri, 26, 26, false, true));
            userAvatarLetter.setVisible(false);
            profileAvatarContainer.setStyle(
                    "-fx-background-color: transparent; -fx-background-radius: 50;" +
                            "-fx-border-color: rgba(16,185,129,0.50); -fx-border-radius: 50;" +
                            "-fx-border-width: 2; -fx-border-style: solid; -fx-cursor: hand;");
            pulseNode(profileAvatarContainer);
        }
    }

    // =================== SAVE ===================

    private void onSave() {
        boolean ok = FormValidator.validateAllEdit(
                nomField, errNom, prenomField, errPrenom, emailField, errEmail, telField, errTel);

        String newPass = s(newPassField.getText());
        if (!newPass.isBlank()) {
            boolean passOk    = FormValidator.validatePassword(newPassField, errPass);
            boolean confirmOk = FormValidator.validateConfirmPassword(confirmPassField, newPassField, errConfirm);
            if (!passOk || !confirmOk) ok = false;
        }

        if (!ok) { showError("Veuillez corriger les erreurs ci-dessus."); return; }

        saveBtn.setDisable(true);
        saveBtn.setText("Sauvegarde...");

        try {
            currentUser.setNom(nomField.getText().trim());
            currentUser.setPrenom(prenomField.getText().trim());
            currentUser.setE_mail(emailField.getText().trim());
            currentUser.setNum_tel(telField.getText().trim());

            if (selectedImageFile != null) {
                currentUser.setImage(selectedImageFile.getAbsolutePath());
                userCRUD.updateImageUser(currentUser);
            }

            userCRUD.updateUser(currentUser);

            if (!newPass.isBlank()) {
                currentUser.setMot_de_pass(newPass);
                userCRUD.updatePassword(currentUser);
                newPassField.clear(); confirmPassField.clear();
            }

            SessionManager.getInstance().setConnectedUser(currentUser);
            showSuccess("Profil mis a jour avec succes !");
            saveBtn.setText("Sauvegarde \u2713");
            pulseNode(saveBtn);
            loadUserData();

            PauseTransition delay = new PauseTransition(Duration.millis(2000));
            delay.setOnFinished(e -> { saveBtn.setDisable(false); applySaveBtnStyle(); });
            delay.play();

        } catch (SQLException ex) {
            showError("Erreur: " + ex.getMessage());
            saveBtn.setDisable(false); applySaveBtnStyle();
            ex.printStackTrace();
        }
    }

    // =================== SAVE BTN ===================

    private static final String BTN_NORMAL =
            "-fx-background-color: linear-gradient(to right, #4338CA, #4F46E5, #6366F1);" +
                    "-fx-background-radius: 12; -fx-padding: 12 24; -fx-font-size: 13px;" +
                    "-fx-font-weight: 800; -fx-text-fill: white; -fx-cursor: hand;" +
                    "-fx-border-color: transparent; -fx-background-insets: 0;" +
                    "-fx-effect: dropshadow(gaussian, rgba(99,102,241,0.30), 18, 0.12, 0, 6);";
    private static final String BTN_HOVER =
            "-fx-background-color: linear-gradient(to right, #3730A3, #4338CA, #4F46E5);" +
                    "-fx-background-radius: 12; -fx-padding: 12 24; -fx-font-size: 13px;" +
                    "-fx-font-weight: 800; -fx-text-fill: white; -fx-cursor: hand;" +
                    "-fx-border-color: transparent; -fx-background-insets: 0;" +
                    "-fx-effect: dropshadow(gaussian, rgba(99,102,241,0.50), 24, 0.18, 0, 8);";

    private void applySaveBtnStyle() {
        saveBtn.setStyle(BTN_NORMAL);
        saveBtn.setOnMouseEntered(e -> saveBtn.setStyle(BTN_HOVER));
        saveBtn.setOnMouseExited(e  -> saveBtn.setStyle(BTN_NORMAL));
    }

    // =================== MESSAGES ===================

    private void showSuccess(String msg) {
        hideMessages(); successMsg.setText(msg); successMsg.setManaged(true); successMsg.setVisible(true);
        FadeTransition ft = new FadeTransition(Duration.millis(300), successMsg); ft.setFromValue(0); ft.setToValue(1); ft.play();
    }
    private void showError(String msg) {
        hideMessages(); errorMsg.setText(msg); errorMsg.setManaged(true); errorMsg.setVisible(true);
        FadeTransition ft = new FadeTransition(Duration.millis(200), errorMsg); ft.setFromValue(0); ft.setToValue(1); ft.play();
    }
    private void hideMessages() {
        if (successMsg != null) { successMsg.setVisible(false); successMsg.setManaged(false); }
        if (errorMsg   != null) { errorMsg.setVisible(false);   errorMsg.setManaged(false);   }
    }

    // =================== THEME ===================

    private void setDarkMode(boolean dark) {
        if (dark) {
            if (!rootPane.getStyleClass().contains("dark")) rootPane.getStyleClass().add("dark");
            themeIcon.setText("\u2600"); themeLabel.setText("Jour");
        } else {
            rootPane.getStyleClass().remove("dark");
            themeIcon.setText("\u263D"); themeLabel.setText("Nuit");
        }
    }

    // =================== NAV ===================

    private void setupNav() {
        navHome.setOnAction(e -> navigateToHome());
        Button[] mods = {navModule1, navModule2, navModule3, navModule4, navModule5, navModule6};
        for (Button b : mods) b.setOnAction(e -> bounceNode(b));
    }

    // =================== NAVIGATION ===================

    private void navigateToHome() {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                    getClass().getResource("/FXML/user_auth/front/home_view.fxml"));
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
        java.net.URL f = getClass().getResource("/styles/user_auth/front/front.css");
        if (f != null) scene.getStylesheets().add(f.toExternalForm());
        java.net.URL l = getClass().getResource("/styles/user_auth/login/login.css");
        if (l != null) scene.getStylesheets().add(l.toExternalForm());
    }

    // =================== VALIDATION ===================

    private boolean isValidEmail(String e) {
        return e != null && e.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    }

    // =================== ANIMATIONS ===================

    private void playEntrance() {
        topNav.setTranslateY(-60); topNav.setOpacity(0);
        TranslateTransition tt = new TranslateTransition(Duration.millis(450), topNav); tt.setToY(0);
        FadeTransition ft = new FadeTransition(Duration.millis(450), topNav); ft.setToValue(1);
        new ParallelTransition(tt, ft).play();
    }

    private void bounceNode(Node n) {
        if (n == null) return;
        ScaleTransition sc = new ScaleTransition(Duration.millis(120), n);
        sc.setFromX(0.90); sc.setFromY(0.90); sc.setToX(1.0); sc.setToY(1.0);
        sc.setInterpolator(Interpolator.EASE_OUT); sc.play();
    }

    private void pulseNode(Node n) {
        if (n == null) return;
        ScaleTransition st = new ScaleTransition(Duration.millis(200), n);
        st.setFromX(1.0); st.setToX(1.05); st.setFromY(1.0); st.setToY(1.05);
        st.setCycleCount(2); st.setAutoReverse(true); st.play();
    }
}