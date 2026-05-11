package org.example.Controllers.user.login;

import javafx.animation.*;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import org.example.Services.user.UserCRUD;

import java.sql.SQLException;

public class ResetPasswordController {

    @FXML private AnchorPane rootPane;
    @FXML private ImageView logoView;
    @FXML private Pane logoGlow;
    @FXML private VBox formCard;
    @FXML private ToggleButton themeToggleBtn;

    @FXML private Label emailLabel;
    @FXML private PasswordField passwordField;
    @FXML private TextField passwordVisibleField;
    @FXML private ToggleButton showPasswordBtn;
    @FXML private PasswordField confirmPasswordField;

    @FXML private Label errPass;
    @FXML private Label errConfirm;

    @FXML private Pane str1;
    @FXML private Pane str2;
    @FXML private Pane str3;
    @FXML private Pane str4;
    @FXML private Label strengthLabel;

    @FXML private Button resetBtn;
    @FXML private Label errorLabel;
    @FXML private Label successLabel;
    @FXML private Hyperlink backLink;

    private String email;
    private final UserCRUD userCRUD = new UserCRUD();

    public void setEmail(String email) {
        this.email = email;
        if (emailLabel != null) {
            emailLabel.setText("Compte : " + email);
        }
    }

    @FXML
    public void initialize() {
        setupTheme();
        setupLogo();
        setupPasswordFields();
        setupLiveValidation();
        setupActions();

        hideFieldError(errPass);
        hideFieldError(errConfirm);
        resetStrength();

        applyButtonStyle(resetBtn);
        playEntrance();
        animateGlow();
    }

    private void setupTheme() {
        if (rootPane != null) {
            if (!rootPane.getStyleClass().contains("login-root")) {
                rootPane.getStyleClass().add("login-root");
            }
            setDarkMode(true);
        }

        themeToggleBtn.setSelected(true);
        themeToggleBtn.setText("\u2600  Jour");
        themeToggleBtn.selectedProperty().addListener((obs, old, selected) -> setDarkMode(selected));
    }

    private void setupLogo() {
        try {
            java.io.InputStream s = getClass().getResourceAsStream("/user/image/logo.png");
            if (s != null && logoView != null) {
                logoView.setImage(new Image(s));
            }
        } catch (Exception e) {
            System.err.println("Logo introuvable");
        }
    }

    private void setupPasswordFields() {
        passwordField.setPromptText("Min. 8 caractères");
        passwordVisibleField.setPromptText("Min. 8 caractères");
        confirmPasswordField.setPromptText("Retapez le mot de passe");

        showPasswordBtn.setText("\uD83D\uDC41");

        passwordVisibleField.textProperty().bindBidirectional(passwordField.textProperty());

        passwordVisibleField.setManaged(false);
        passwordVisibleField.setVisible(false);
        passwordField.setManaged(true);
        passwordField.setVisible(true);

        showPasswordBtn.selectedProperty().addListener((obs, old, selected) -> {
            passwordVisibleField.setManaged(selected);
            passwordVisibleField.setVisible(selected);

            passwordField.setManaged(!selected);
            passwordField.setVisible(!selected);

            showPasswordBtn.setText(selected ? "\uD83D\uDE48" : "\uD83D\uDC41");
        });

        addFocusStyle(passwordField);
        addFocusStyle(passwordVisibleField);
        addFocusStyle(confirmPasswordField);
    }

    private void setupLiveValidation() {
        passwordField.textProperty().addListener((obs, old, val) -> {
            validatePasswordLive();
            validateConfirmLive();
            updatePasswordStrength(val);
        });

        confirmPasswordField.textProperty().addListener((obs, old, val) -> validateConfirmLive());
    }

    private void setupActions() {
        resetBtn.setOnAction(e -> onResetPassword());
        backLink.setOnAction(e -> navigateToLogin());
    }

    private void onResetPassword() {
        String pass = passwordField.getText() == null ? "" : passwordField.getText();
        String confirm = confirmPasswordField.getText() == null ? "" : confirmPasswordField.getText();

        hideError();
        hideSuccess();

        boolean valid = validateBeforeSubmit(pass, confirm);
        if (!valid) return;

        try {
            resetBtn.setText("Modification en cours...");
            resetBtn.setDisable(true);

            boolean updated = userCRUD.updatePasswordByEmail(email, pass);

            if (!updated) {
                showError("Aucun compte trouvé avec cet email.");
                resetBtn.setText("Changer le mot de passe");
                resetBtn.setDisable(false);
                return;
            }

            showSuccess("Mot de passe modifié avec succès. Redirection...");
            resetBtn.setText("Mot de passe modifié ✓");
            pulseNode(resetBtn);

            PauseTransition delay = new PauseTransition(Duration.millis(1300));
            delay.setOnFinished(e -> navigateToLogin());
            delay.play();

        } catch (SQLException ex) {
            showError("Erreur lors de la modification du mot de passe.");
            resetBtn.setText("Changer le mot de passe");
            resetBtn.setDisable(false);
            ex.printStackTrace();
        }
    }

    private boolean validateBeforeSubmit(String pass, String confirm) {
        boolean valid = true;

        if (email == null || email.isBlank()) {
            showError("Email introuvable. Veuillez recommencer la réinitialisation.");
            return false;
        }

        if (pass.isBlank()) {
            showFieldError(errPass, "Veuillez entrer un mot de passe.");
            shakeNode(passwordField.getParent());
            valid = false;
        } else if (pass.length() < 8) {
            showFieldError(errPass, "Le mot de passe doit contenir au moins 8 caractères.");
            shakeNode(passwordField.getParent());
            valid = false;
        } else {
            hideFieldError(errPass);
        }

        if (confirm.isBlank()) {
            showFieldError(errConfirm, "Veuillez confirmer le mot de passe.");
            shakeNode(confirmPasswordField.getParent());
            valid = false;
        } else if (!confirm.equals(pass)) {
            showFieldError(errConfirm, "Les mots de passe ne correspondent pas.");
            shakeNode(confirmPasswordField.getParent());
            valid = false;
        } else {
            hideFieldError(errConfirm);
        }

        return valid;
    }

    private void validatePasswordLive() {
        String pass = passwordField.getText() == null ? "" : passwordField.getText();

        if (pass.isBlank()) {
            hideFieldError(errPass);
        } else if (pass.length() < 8) {
            showFieldError(errPass, "Minimum 8 caractères.");
        } else {
            hideFieldError(errPass);
        }
    }

    private void validateConfirmLive() {
        String pass = passwordField.getText() == null ? "" : passwordField.getText();
        String confirm = confirmPasswordField.getText() == null ? "" : confirmPasswordField.getText();

        if (confirm.isBlank()) {
            hideFieldError(errConfirm);
        } else if (!confirm.equals(pass)) {
            showFieldError(errConfirm, "Les mots de passe ne correspondent pas.");
        } else {
            hideFieldError(errConfirm);
        }
    }

    private void resetStrength() {
        String reset = "-fx-background-color: rgba(99,102,241,0.10);";
        str1.setStyle(reset);
        str2.setStyle(reset);
        str3.setStyle(reset);
        str4.setStyle(reset);
        strengthLabel.setManaged(false);
        strengthLabel.setVisible(false);
        strengthLabel.setText("");
    }

    private void updatePasswordStrength(String pass) {
        resetStrength();

        if (pass == null || pass.isEmpty()) {
            return;
        }

        int score = 0;
        if (pass.length() >= 6) score++;
        if (pass.length() >= 8) score++;
        if (pass.matches(".*[A-Z].*") && pass.matches(".*[a-z].*")) score++;
        if (pass.matches(".*[0-9].*") || pass.matches(".*[^A-Za-z0-9].*")) score++;

        String weak = "-fx-background-color: #EF4444;";
        String med  = "-fx-background-color: #F59E0B;";
        String ok   = "-fx-background-color: #10B981;";

        strengthLabel.setManaged(true);
        strengthLabel.setVisible(true);

        if (score == 1) {
            str1.setStyle(weak);
            strengthLabel.setText("Faible");
            strengthLabel.setStyle("-fx-text-fill: #EF4444;");
        } else if (score == 2) {
            str1.setStyle(med);
            str2.setStyle(med);
            strengthLabel.setText("Moyen");
            strengthLabel.setStyle("-fx-text-fill: #F59E0B;");
        } else if (score == 3) {
            str1.setStyle(ok);
            str2.setStyle(ok);
            str3.setStyle(ok);
            strengthLabel.setText("Fort");
            strengthLabel.setStyle("-fx-text-fill: #10B981;");
        } else {
            str1.setStyle(ok);
            str2.setStyle(ok);
            str3.setStyle(ok);
            str4.setStyle(ok);
            strengthLabel.setText("Excellent");
            strengthLabel.setStyle("-fx-text-fill: #059669;");
        }
    }

    private void navigateToLogin() {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                    getClass().getResource("/user/login/login_view.fxml")
            );

            javafx.scene.Parent root = loader.load();
            javafx.scene.Scene scene = rootPane.getScene();
            scene.setRoot(root);

            java.net.URL css = getClass().getResource("/user/login/login.css");
            if (css != null && !scene.getStylesheets().contains(css.toExternalForm())) {
                scene.getStylesheets().add(css.toExternalForm());
            }

        } catch (Exception e) {
            showError("Impossible de charger la page de connexion.");
            e.printStackTrace();
        }
    }

    private void setDarkMode(boolean dark) {
        if (dark) {
            if (!rootPane.getStyleClass().contains("dark")) {
                rootPane.getStyleClass().add("dark");
            }
            themeToggleBtn.setText("\u2600  Jour");
        } else {
            rootPane.getStyleClass().remove("dark");
            themeToggleBtn.setText("\u263D  Nuit");
        }
    }

    private static final String BTN_NORMAL =
            "-fx-background-color: linear-gradient(to right, #4338CA, #4F46E5, #6366F1);" +
                    "-fx-background-radius: 14;" +
                    "-fx-padding: 16 28;" +
                    "-fx-font-size: 14px;" +
                    "-fx-font-weight: 800;" +
                    "-fx-text-fill: white;" +
                    "-fx-cursor: hand;" +
                    "-fx-border-color: transparent;" +
                    "-fx-effect: dropshadow(gaussian, rgba(99,102,241,0.35), 24, 0.18, 0, 10);";

    private static final String BTN_HOVER =
            "-fx-background-color: linear-gradient(to right, #3730A3, #4338CA, #4F46E5);" +
                    "-fx-background-radius: 14;" +
                    "-fx-padding: 16 28;" +
                    "-fx-font-size: 14px;" +
                    "-fx-font-weight: 800;" +
                    "-fx-text-fill: white;" +
                    "-fx-cursor: hand;" +
                    "-fx-border-color: transparent;" +
                    "-fx-effect: dropshadow(gaussian, rgba(99,102,241,0.50), 30, 0.24, 0, 14);";

    private void applyButtonStyle(Button btn) {
        btn.setStyle(BTN_NORMAL);
        btn.setOnMouseEntered(e -> btn.setStyle(BTN_HOVER));
        btn.setOnMouseExited(e -> btn.setStyle(BTN_NORMAL));
        btn.setOnMousePressed(e -> btn.setStyle(BTN_HOVER + "-fx-scale-x: 0.985; -fx-scale-y: 0.985;"));
        btn.setOnMouseReleased(e -> btn.setStyle(BTN_NORMAL));
    }

    private void showFieldError(Label label, String msg) {
        label.setText(msg);
        label.setManaged(true);
        label.setVisible(true);
    }

    private void hideFieldError(Label label) {
        if (label == null) return;
        label.setText("");
        label.setManaged(false);
        label.setVisible(false);
    }

    private void showError(String msg) {
        hideSuccess();
        errorLabel.setText(msg);
        errorLabel.setManaged(true);
        errorLabel.setVisible(true);

        FadeTransition ft = new FadeTransition(Duration.millis(200), errorLabel);
        ft.setFromValue(0);
        ft.setToValue(1);

        TranslateTransition tt = new TranslateTransition(Duration.millis(200), errorLabel);
        tt.setFromY(-10);
        tt.setToY(0);
        tt.setInterpolator(Interpolator.EASE_OUT);

        new ParallelTransition(ft, tt).play();
    }

    private void hideError() {
        if (errorLabel == null) return;
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
        errorLabel.setText("");
    }

    private void showSuccess(String msg) {
        hideError();
        successLabel.setText(msg);
        successLabel.setManaged(true);
        successLabel.setVisible(true);

        FadeTransition ft = new FadeTransition(Duration.millis(250), successLabel);
        ft.setFromValue(0);
        ft.setToValue(1);
        ft.play();
    }

    private void hideSuccess() {
        if (successLabel == null) return;
        successLabel.setVisible(false);
        successLabel.setManaged(false);
        successLabel.setText("");
    }

    private void playEntrance() {
        if (formCard != null) {
            formCard.setOpacity(0);
            formCard.setScaleX(0.92);
            formCard.setScaleY(0.92);

            FadeTransition fade = new FadeTransition(Duration.millis(600), formCard);
            fade.setFromValue(0);
            fade.setToValue(1);

            ScaleTransition scale = new ScaleTransition(Duration.millis(600), formCard);
            scale.setFromX(0.92);
            scale.setFromY(0.92);
            scale.setToX(1);
            scale.setToY(1);
            scale.setInterpolator(Interpolator.EASE_OUT);

            new ParallelTransition(fade, scale).play();
        }
    }

    private void animateGlow() {
        if (logoGlow == null) return;

        Timeline pulse = new Timeline(
                new KeyFrame(Duration.ZERO,
                        new KeyValue(logoGlow.scaleXProperty(), 1.0, Interpolator.EASE_BOTH),
                        new KeyValue(logoGlow.scaleYProperty(), 1.0, Interpolator.EASE_BOTH),
                        new KeyValue(logoGlow.opacityProperty(), 0.6, Interpolator.EASE_BOTH)),
                new KeyFrame(Duration.millis(2000),
                        new KeyValue(logoGlow.scaleXProperty(), 1.15, Interpolator.EASE_BOTH),
                        new KeyValue(logoGlow.scaleYProperty(), 1.15, Interpolator.EASE_BOTH),
                        new KeyValue(logoGlow.opacityProperty(), 1.0, Interpolator.EASE_BOTH))
        );

        pulse.setCycleCount(Timeline.INDEFINITE);
        pulse.setAutoReverse(true);
        pulse.play();
    }

    private void shakeNode(Node n) {
        if (n == null) return;

        TranslateTransition tt = new TranslateTransition(Duration.millis(50), n);
        tt.setFromX(0);
        tt.setByX(8);
        tt.setCycleCount(6);
        tt.setAutoReverse(true);
        tt.setOnFinished(e -> n.setTranslateX(0));
        tt.play();
    }

    private void pulseNode(Node n) {
        if (n == null) return;

        ScaleTransition st = new ScaleTransition(Duration.millis(200), n);
        st.setFromX(1.0);
        st.setToX(1.05);
        st.setFromY(1.0);
        st.setToY(1.05);
        st.setCycleCount(2);
        st.setAutoReverse(true);
        st.play();
    }

    private void addFocusStyle(Control field) {
        field.focusedProperty().addListener((obs, old, focused) -> {
            Parent wrap = findInputWrap(field);
            if (wrap == null) return;

            if (focused) {
                if (!wrap.getStyleClass().contains("is-focused")) {
                    wrap.getStyleClass().add("is-focused");
                }
            } else {
                wrap.getStyleClass().remove("is-focused");
            }
        });
    }

    private Parent findInputWrap(Control field) {
        Parent p = field.getParent();

        while (p != null) {
            if (p.getStyleClass() != null && p.getStyleClass().contains("input-wrap")) {
                return p;
            }
            p = p.getParent();
        }

        return null;
    }
}