package org.example.Controllers.user.login;

import javafx.animation.*;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import org.example.Entites.user.User;
import org.example.Utils.MailSender;
import org.example.Services.user.UserCRUD;

import java.sql.SQLException;

public class ForgotPasswordController {

    @FXML private AnchorPane rootPane;
    @FXML private ImageView logoView;
    @FXML private Pane logoGlow;
    @FXML private VBox formCard;
    @FXML private ToggleButton themeToggleBtn;
    @FXML private Label emailIcon;
    @FXML private Pane emailStatus;

    @FXML private TextField emailField;
    @FXML private Button sendBtn;
    @FXML private Label errorLabel;
    @FXML private Label successLabel;
    @FXML private Hyperlink backLink;

    @FXML private VBox codeBox;
    @FXML private StackPane verifyCodeBox;
    @FXML private TextField codeField;
    @FXML private Button verifyCodeBtn;

    private String generatedCode;
    private final UserCRUD userCRUD = new UserCRUD();

    @FXML
    public void initialize() {

        if (rootPane != null) {
            if (!rootPane.getStyleClass().contains("login-root"))
                rootPane.getStyleClass().add("login-root");
            boolean dark = org.example.Utils.SessionManager.getInstance().isDarkMode();
            setDarkMode(dark);
        }

        themeToggleBtn.setOnAction(e -> {
            boolean newMode = !org.example.Utils.SessionManager.getInstance().isDarkMode();
            org.example.Utils.SessionManager.getInstance().setDarkMode(newMode);
            setDarkMode(newMode);
        });

        try {
            java.io.InputStream s = getClass().getResourceAsStream("/user/image/logo.png");
            if (s != null && logoView != null) logoView.setImage(new Image(s));
        } catch (Exception e) {
            System.err.println("Logo introuvable");
        }

        emailIcon.setText("@");
        emailField.setPromptText("ex: prenom.nom@email.com");

        applyButtonStyle(sendBtn);
        applyButtonStyle(verifyCodeBtn);

        if (codeBox != null) {
            codeBox.setVisible(false);
            codeBox.setManaged(false);
        }

        if (verifyCodeBox != null) {
            verifyCodeBox.setVisible(false);
            verifyCodeBox.setManaged(false);
        }

        emailField.textProperty().addListener((obs, old, val) -> {
            hideError();
            hideSuccess();

            Parent wrap = findInputWrap(emailField);
            if (wrap == null) return;

            wrap.getStyleClass().removeAll("input-error", "input-ok");

            if (val != null && !val.isBlank() && isValidEmail(val.trim())) {
                wrap.getStyleClass().add("input-ok");
                emailStatus.setManaged(true);
                emailStatus.setVisible(true);
            } else {
                emailStatus.setManaged(false);
                emailStatus.setVisible(false);
            }
        });

        addFocusStyle(emailField);

        if (codeField != null) {
            addFocusStyle(codeField);
        }

        playEntrance();
        animateGlow();

        sendBtn.setOnAction(e -> onSend());

        if (verifyCodeBtn != null) {
            verifyCodeBtn.setOnAction(e -> onVerifyCode());
        }

        backLink.setOnAction(e -> navigateToLogin());
    }

    private void applySendBtnStyle() {
        String normalStyle =
                "-fx-background-color: linear-gradient(to right, #4338CA, #4F46E5, #6366F1);" +
                        "-fx-background-radius: 14;" +
                        "-fx-padding: 16 28;" +
                        "-fx-font-size: 14px;" +
                        "-fx-font-weight: 800;" +
                        "-fx-text-fill: white;" +
                        "-fx-cursor: hand;" +
                        "-fx-border-color: transparent;" +
                        "-fx-effect: dropshadow(gaussian, rgba(99,102,241,0.35), 24, 0.18, 0, 10);";

        String hoverStyle =
                "-fx-background-color: linear-gradient(to right, #3730A3, #4338CA, #4F46E5);" +
                        "-fx-background-radius: 14;" +
                        "-fx-padding: 16 28;" +
                        "-fx-font-size: 14px;" +
                        "-fx-font-weight: 800;" +
                        "-fx-text-fill: white;" +
                        "-fx-cursor: hand;" +
                        "-fx-border-color: transparent;" +
                        "-fx-effect: dropshadow(gaussian, rgba(99,102,241,0.50), 30, 0.24, 0, 14);";

        sendBtn.setStyle(normalStyle);
        sendBtn.setOnMouseEntered(e -> sendBtn.setStyle(hoverStyle));
        sendBtn.setOnMouseExited(e -> sendBtn.setStyle(normalStyle));
        sendBtn.setOnMousePressed(e -> sendBtn.setStyle(
                "-fx-background-color: linear-gradient(to right, #3730A3, #4338CA, #4F46E5);" +
                        "-fx-background-radius: 14;" +
                        "-fx-padding: 16 28;" +
                        "-fx-font-size: 14px;" +
                        "-fx-font-weight: 800;" +
                        "-fx-text-fill: white;" +
                        "-fx-scale-x: 0.985;" +
                        "-fx-scale-y: 0.985;"
        ));
        sendBtn.setOnMouseReleased(e -> sendBtn.setStyle(normalStyle));
    }

    private void setDarkMode(boolean dark) {
        if (dark) {
            if (!rootPane.getStyleClass().contains("dark"))
                rootPane.getStyleClass().add("dark");
            themeToggleBtn.setText("\u2600  Jour");
        } else {
            rootPane.getStyleClass().remove("dark");
            themeToggleBtn.setText("\u263D  Nuit");
        }
    }

    private void onSend() {
        String email = emailField.getText() == null ? "" : emailField.getText().trim();

        if (!isValidEmail(email)) {
            showError("Veuillez entrer une adresse email valide.");
            shakeNode(emailField.getParent());
            emailField.requestFocus();
            return;
        }
        try {
            User user = userCRUD.getUserByEmail(email);

            if (user == null) {
                showError("Aucun compte n'existe avec cet email.");
                shakeNode(emailField.getParent());
                emailField.requestFocus();
                return;
            }

        } catch (SQLException ex) {
            showError("Erreur lors de la vérification de l'email.");
            ex.printStackTrace();
            return;
        }

        hideError();
        hideSuccess();

        sendBtn.setText("Envoi en cours...");
        sendBtn.setDisable(true);

        generatedCode = String.valueOf((int) (Math.random() * 900000) + 100000);

        Task<Void> mailTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                MailSender.sendMail(
                        email,
                        "Réinitialisation du mot de passe",
                        "Bonjour,\n\nVotre code de vérification est : " + generatedCode +
                                "\n\nSi vous n'avez pas demandé cette réinitialisation, ignorez cet email."
                );
                return null;
            }
        };

        mailTask.setOnSucceeded(e -> {
            showSuccess("Un code de vérification a été envoyé à\n" + email);
            sendBtn.setText("Email envoyé ✓");
            sendBtn.setDisable(false);
            pulseNode(sendBtn);

            if (codeBox != null) {
                codeBox.setVisible(true);
                codeBox.setManaged(true);
            }

            if (verifyCodeBox != null) {
                verifyCodeBox.setVisible(true);
                verifyCodeBox.setManaged(true);
            }

            if (codeField != null) {
                codeField.clear();
                codeField.requestFocus();
            }

            System.out.println("Code envoyé = " + generatedCode);
        });

        mailTask.setOnFailed(e -> {
            showError("Erreur lors de l'envoi de l'email.");
            sendBtn.setText("Envoyer le lien");
            sendBtn.setDisable(false);
            mailTask.getException().printStackTrace();
        });

        Thread thread = new Thread(mailTask);
        thread.setDaemon(true);
        thread.start();
    }

    private void onVerifyCode() {
        String enteredCode = codeField.getText() == null ? "" : codeField.getText().trim();

        if (generatedCode == null || generatedCode.isBlank()) {
            showError("Veuillez d'abord demander un code.");
            return;
        }

        if (!enteredCode.matches("\\d{6}")) {
            showError("Le code de vérification doit contenir exactement 6 chiffres.");
            shakeNode(codeField);
            codeField.requestFocus();
            return;
        }

        if (!enteredCode.equals(generatedCode)) {
            showError("Code incorrect. Veuillez réessayer.");
            shakeNode(codeField);
            codeField.requestFocus();
            return;
        }

        hideError();
        showSuccess("Code vérifié avec succès ✔");

        PauseTransition delay = new PauseTransition(Duration.millis(700));
        delay.setOnFinished(e -> navigateToResetPassword(emailField.getText().trim()));
        delay.play();
    }

    private void navigateToLogin() {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                    getClass().getResource("/user/login/login_view.fxml"));
            javafx.scene.Parent root = loader.load();
            javafx.scene.Scene scene = rootPane.getScene();
            scene.setRoot(root);

            java.net.URL css = getClass().getResource("/user/login/login.css");
            if (css != null && !scene.getStylesheets().contains(css.toExternalForm()))
                scene.getStylesheets().add(css.toExternalForm());
        } catch (Exception e) {
            showError("Impossible de charger la page de connexion.");
            e.printStackTrace();
        }
    }
    private void navigateToResetPassword(String email) {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                    getClass().getResource("/user/login/reset_password_view.fxml")
            );

            javafx.scene.Parent root = loader.load();

            ResetPasswordController controller = loader.getController();
            controller.setEmail(email);

            javafx.scene.Scene scene = rootPane.getScene();
            scene.setRoot(root);

            java.net.URL css = getClass().getResource("/user/login/login.css");
            if (css != null && !scene.getStylesheets().contains(css.toExternalForm())) {
                scene.getStylesheets().add(css.toExternalForm());
            }

        } catch (Exception ex) {
            showError("Impossible de charger la page de réinitialisation.");
            ex.printStackTrace();
        }
    }

    private boolean isValidEmail(String e) {
        return e != null && e.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
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
        if (errorLabel == null || !errorLabel.isVisible()) return;
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
        errorLabel.setText("");
    }

    private void showSuccess(String msg) {
        hideError();
        successLabel.setText(msg);
        successLabel.setManaged(true);
        successLabel.setVisible(true);

        FadeTransition ft = new FadeTransition(Duration.millis(300), successLabel);
        ft.setFromValue(0);
        ft.setToValue(1);
        ft.play();
    }

    private void hideSuccess() {
        if (successLabel == null || !successLabel.isVisible()) return;
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
        field.focusedProperty().addListener((obs, o, focused) -> {
            Parent wrap = findInputWrap(field);
            if (wrap == null) return;

            if (focused) {
                if (!wrap.getStyleClass().contains("is-focused"))
                    wrap.getStyleClass().add("is-focused");
            } else {
                wrap.getStyleClass().remove("is-focused");
            }
        });
    }

    private Parent findInputWrap(Control field) {
        Parent p = field.getParent();

        while (p != null) {
            if (p.getStyleClass() != null && p.getStyleClass().contains("input-wrap"))
                return p;
            p = p.getParent();
        }

        return null;
    }
    private void applyButtonStyle(Button btn) {
        String normalStyle =
                "-fx-background-color: linear-gradient(to right, #4338CA, #4F46E5, #6366F1);" +
                        "-fx-background-radius: 14;" +
                        "-fx-padding: 16 28;" +
                        "-fx-font-size: 14px;" +
                        "-fx-font-weight: 800;" +
                        "-fx-text-fill: white;" +
                        "-fx-cursor: hand;" +
                        "-fx-border-color: transparent;" +
                        "-fx-effect: dropshadow(gaussian, rgba(99,102,241,0.35), 24, 0.18, 0, 10);";

        String hoverStyle =
                "-fx-background-color: linear-gradient(to right, #3730A3, #4338CA, #4F46E5);" +
                        "-fx-background-radius: 14;" +
                        "-fx-padding: 16 28;" +
                        "-fx-font-size: 14px;" +
                        "-fx-font-weight: 800;" +
                        "-fx-text-fill: white;" +
                        "-fx-cursor: hand;" +
                        "-fx-border-color: transparent;" +
                        "-fx-effect: dropshadow(gaussian, rgba(99,102,241,0.50), 30, 0.24, 0, 14);";

        btn.setStyle(normalStyle);
        btn.setOnMouseEntered(e -> btn.setStyle(hoverStyle));
        btn.setOnMouseExited(e -> btn.setStyle(normalStyle));
    }
}