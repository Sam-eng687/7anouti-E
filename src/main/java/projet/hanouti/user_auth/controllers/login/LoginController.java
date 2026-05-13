package projet.hanouti.user_auth.controllers.login;

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
import javafx.scene.shape.Circle;
import javafx.util.Duration;

import projet.hanouti.user_auth.enums.Role;
import projet.hanouti.user_auth.enums.Status;
import projet.hanouti.user_auth.entities.User;
import projet.hanouti.user_auth.services.UserCRUD;
import projet.hanouti.common.utils.CameraCapture;
import projet.hanouti.common.utils.MailSender;
import projet.hanouti.common.utils.SessionManager;

import java.sql.SQLException;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import java.net.URL;

public class LoginController {

    @FXML private AnchorPane rootPane;
    @FXML private ImageView logoView;
    @FXML private Pane logoGlow;
    @FXML private VBox loginCard;
    @FXML private ToggleButton themeToggleBtn;
    @FXML private Label emailIcon;
    @FXML private Pane emailStatus;

    @FXML private Circle blob1, blob2, blob3, blob4;

    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private TextField passwordVisibleField;
    @FXML private ToggleButton showPasswordBtn;

    @FXML private CheckBox rememberMeCheck;
    @FXML private Hyperlink forgotLink;
    @FXML private Hyperlink signupLink;

    @FXML private Button loginBtn;
    @FXML private Button faceLoginBtn;
    @FXML private Label errorLabel;

    private int failedAttempts = 0;

    @FXML
    public void initialize() {

        if (rootPane != null) {
            if (!rootPane.getStyleClass().contains("login-root")) {
                rootPane.getStyleClass().add("login-root");
            }
            boolean dark = projet.hanouti.common.utils.SessionManager.getInstance().isDarkMode();
            setDarkMode(dark);
        }

        themeToggleBtn.setOnAction(e -> {
            boolean newMode = !projet.hanouti.common.utils.SessionManager.getInstance().isDarkMode();
            projet.hanouti.common.utils.SessionManager.getInstance().setDarkMode(newMode);
            setDarkMode(newMode);
        });

        try {
            java.io.InputStream s = getClass().getResourceAsStream("/images/user_auth/logo.png");
            if (s != null && logoView != null) {
                logoView.setImage(new Image(s));
            }
        } catch (Exception e) {
            System.err.println("Logo introuvable");
        }

        if (emailIcon != null) {
            emailIcon.setText("@");
        }

        emailField.setPromptText("ex: prenom.nom@email.com");
        passwordField.setPromptText("Votre mot de passe");

        if (passwordVisibleField != null) {
            passwordVisibleField.setPromptText("Votre mot de passe");
            passwordVisibleField.textProperty().bindBidirectional(passwordField.textProperty());
        }

        if (showPasswordBtn != null) {
            showPasswordBtn.setText("\uD83D\uDC41");
            showPasswordBtn.selectedProperty().addListener((obs, old, selected) -> {
                passwordVisibleField.setManaged(selected);
                passwordVisibleField.setVisible(selected);

                passwordField.setManaged(!selected);
                passwordField.setVisible(!selected);

                showPasswordBtn.setText(selected ? "\uD83D\uDE48" : "\uD83D\uDC41");
            });
        }

        if (forgotLink != null) {
            forgotLink.setText("Mot de passe oublié ?");
            forgotLink.setOnAction(e -> navigateToForgotPassword());
        }

        if (signupLink != null) {
            signupLink.setOnAction(e -> navigateToRegister());
        }

        if (loginBtn != null) {
            loginBtn.setOnAction(e -> onLogin());
        }

        if (faceLoginBtn != null) {
            faceLoginBtn.setOnAction(e -> onFaceLogin());
        }

        emailField.textProperty().addListener((obs, old, val) -> {
            hideError();

            Parent wrap = findInputWrap(emailField);
            if (wrap == null) return;

            wrap.getStyleClass().removeAll("input-error", "input-ok");

            if (val == null || val.isBlank()) {
                if (emailStatus != null) {
                    emailStatus.setManaged(false);
                    emailStatus.setVisible(false);
                }
                return;
            }

            if (isValidEmail(val.trim())) {
                wrap.getStyleClass().add("input-ok");
                if (emailStatus != null) {
                    emailStatus.setManaged(true);
                    emailStatus.setVisible(true);
                }
            } else {
                if (emailStatus != null) {
                    emailStatus.setManaged(false);
                    emailStatus.setVisible(false);
                }
            }
        });

        passwordField.textProperty().addListener((obs, old, val) -> hideError());

        if (passwordVisibleField != null) {
            passwordVisibleField.textProperty().addListener((obs, old, val) -> hideError());
        }

        addFocusStyle(emailField);
        addFocusStyle(passwordField);

        if (passwordVisibleField != null) {
            addFocusStyle(passwordVisibleField);
        }

        playEntranceAnimation();
        animateBlobs();
        animateLogoGlow();
    }

    private void onLogin() {
        String email = emailField.getText() == null ? "" : emailField.getText().trim();
        String pass = passwordField.getText() == null ? "" : passwordField.getText();

        if (!isValidEmail(email)) {
            showError("Adresse email invalide.");

            Parent wrap = findInputWrap(emailField);
            if (wrap != null) {
                wrap.getStyleClass().removeAll("input-error", "input-ok");
                wrap.getStyleClass().add("input-error");
            }

            shakeNode(emailField.getParent());
            emailField.requestFocus();
            return;
        }

        if (pass.isBlank()) {
            showError("Mot de passe requis.");

            Parent wrap = findInputWrap(passwordField);
            if (wrap != null) {
                wrap.getStyleClass().removeAll("input-error", "input-ok");
                wrap.getStyleClass().add("input-error");
            }

            shakeNode(passwordField.getParent());
            passwordField.requestFocus();
            return;
        }

        String original = loginBtn.getText();
        loginBtn.setText("Connexion...");
        loginBtn.setDisable(true);
        loginBtn.setOpacity(0.8);

        PauseTransition delay = new PauseTransition(Duration.millis(100));
        delay.setOnFinished(ev -> {
            try {
                UserCRUD userCRUD = new UserCRUD();

                User loginUser = new User();
                loginUser.setE_mail(email);
                loginUser.setMot_de_pass(pass);

                User auth = userCRUD.signIn(loginUser);

                if (auth == null) {
                    failedAttempts++;

                    showError("Email ou mot de passe incorrect.");
                    shakeNode(loginCard);

                    if (failedAttempts >= 3) {
                        showError("Plusieurs tentatives échouées. Une alerte de sécurité va être envoyée.");
                        handleSuspiciousLogin(email);
                        failedAttempts = 0;
                    }

                    resetLoginBtn(original);
                    return;
                }

                failedAttempts = 0;

                if (auth.getStatus() != Status.Unbanned) {
                    showError("Votre compte est banni. Contactez l'administrateur.");
                    resetLoginBtn(original);
                    return;
                }

                SessionManager.getInstance().setConnectedUser(auth);

                hideError();
                loginBtn.setText("Connecté ✓");
                loginBtn.setOpacity(1.0);
                loginBtn.setStyle("-fx-background-color: linear-gradient(to right, #059669, #10B981);");
                pulseNode(loginBtn);

                PauseTransition redirectDelay = new PauseTransition(Duration.millis(700));
                redirectDelay.setOnFinished(re -> redirectByRole(auth));
                redirectDelay.play();

            } catch (SQLException ex) {
                showError(ex.getMessage());
                resetLoginBtn(original);
                ex.printStackTrace();
            }
        });

        delay.play();
    }

    private void redirectByRole(User user) {
        navigateToDashboard(user);
    }

    private void navigateToDashboard(User connectedUser) {
        try {
            java.net.URL fxml = getClass().getResource("/FXML/user_auth/back/dashboard.fxml");

            if (fxml == null) {
                showError("FXML dashboard introuvable: /FXML/user_auth/back/dashboard.fxml");
                return;
            }

            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(fxml);
            javafx.scene.Parent root = loader.load();

            projet.hanouti.user_auth.controllers.back.DashboardController dashCtrl = loader.getController();
            dashCtrl.setConnectedAdmin(connectedUser);

            javafx.scene.Scene scene = rootPane.getScene();
            scene.getStylesheets().clear();

            java.net.URL css = getClass().getResource("/styles/user_auth/back/dashboard.css");
            if (css != null) {
                scene.getStylesheets().add(css.toExternalForm());
            }

            scene.setRoot(root);

        } catch (Exception e) {
            showError("Impossible de charger le dashboard admin.");
            e.printStackTrace();
        }
    }

    private void navigateToAcheteur(User connectedUser) {
        navigateToRoleFront("/FXML/user_auth/front/home_view.fxml");
    }

    private void navigateToVendeur(User connectedUser) {
        navigateToRoleFront("/FXML/user_auth/front/home_vendeur_view.fxml");
    }

    private void navigateToLivreur(User connectedUser) {
        navigateToRoleFront("/FXML/user_auth/front/home_livreur_view.fxml");
    }

    private void navigateToRoleFront(String fxmlPath) {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource(fxmlPath));
            javafx.scene.Parent root = loader.load();

            javafx.scene.Scene scene = rootPane.getScene();
            scene.getStylesheets().clear();

            java.net.URL frontCss = getClass().getResource("/styles/user_auth/front/front.css");
            if (frontCss != null) {
                scene.getStylesheets().add(frontCss.toExternalForm());
            }

            java.net.URL loginCss = getClass().getResource("/styles/user_auth/login/login.css");
            if (loginCss != null) {
                scene.getStylesheets().add(loginCss.toExternalForm());
            }

            scene.setRoot(root);

        } catch (Exception e) {
            showError("Impossible de charger l'interface.");
            e.printStackTrace();
        }
    }

    private void navigateToRegister() {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                    getClass().getResource("/FXML/user_auth/login/choose_role_view.fxml")
            );

            javafx.scene.Parent root = loader.load();
            javafx.scene.Scene scene = rootPane.getScene();

            scene.setRoot(root);

            java.net.URL css = getClass().getResource("/styles/user_auth/login/login.css");
            if (css != null && !scene.getStylesheets().contains(css.toExternalForm())) {
                scene.getStylesheets().add(css.toExternalForm());
            }

        } catch (Exception e) {
            showError("Impossible de charger la page de choix du rôle.");
            e.printStackTrace();
        }
    }

    private void navigateToForgotPassword() {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                    getClass().getResource("/FXML/user_auth/login/forgot_password_view.fxml")
            );

            javafx.scene.Parent root = loader.load();
            javafx.scene.Scene scene = rootPane.getScene();

            scene.setRoot(root);

            java.net.URL css = getClass().getResource("/styles/user_auth/login/login.css");
            if (css != null && !scene.getStylesheets().contains(css.toExternalForm())) {
                scene.getStylesheets().add(css.toExternalForm());
            }

        } catch (Exception e) {
            showError("Impossible de charger la page mot de passe oublié.");
            e.printStackTrace();
        }
    }

    private void onFaceLogin() {

        String email = emailField.getText().trim();

        if (!isValidEmail(email)) {
            showError("Entrer votre email d'abord");
            return;
        }

        new Thread(() -> {
            try {
                UserCRUD crud = new UserCRUD();
                User user = crud.getUserByEmail(email);

                if (user == null) {
                    javafx.application.Platform.runLater(() ->
                            showError("Compte introuvable"));
                    return;
                }

                if (!user.isFaceIdEnabled()) {
                    javafx.application.Platform.runLater(() ->
                            showError("Face ID non activé"));
                    return;
                }


                String newCapture = CameraCapture.capturePhoto();


                boolean match = projet.hanouti.common.utils.FaceRecognition.recognize(
                        newCapture,
                        user.getFaceImagePath()
                );

                javafx.application.Platform.runLater(() -> {
                    if (match) {

                        SessionManager.getInstance().setConnectedUser(user);
                        showError("Face reconnue ✔");

                        redirectByRole(user);

                    } else {
                        showError("Face incorrecte ❌");
                    }
                });

            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void handleSuspiciousLogin(String email) {
        new Thread(() -> {
            try {
                String imagePath = CameraCapture.capturePhoto();

                MailSender.sendMailWithAttachment(
                        email,
                        "Alerte de sécurité - 7anouti-E",
                        "Bonjour,\n\n" +
                                "Nous avons détecté plusieurs tentatives de connexion échouées sur votre compte 7anouti-E.\n\n" +
                                "Est-ce que c'était vous ?\n\n" +
                                "Si ce n'était pas vous, nous vous recommandons de changer votre mot de passe immédiatement.\n\n" +
                                "Une photo de la tentative est jointe à cet email.\n\n" +
                                "L'équipe 7anouti-E",
                        imagePath
                );

                System.out.println("Alerte sécurité envoyée à : " + email);

            } catch (Exception e) {
                System.err.println("Impossible d'envoyer l'alerte sécurité.");
                e.printStackTrace();
            }
        }).start();
    }

    private void resetLoginBtn(String text) {
        loginBtn.setText(text);
        loginBtn.setDisable(false);
        loginBtn.setOpacity(1.0);
        loginBtn.setStyle("");
    }

    private void setDarkMode(boolean dark) {
        if (rootPane == null) return;

        if (dark) {
            if (!rootPane.getStyleClass().contains("dark")) {
                rootPane.getStyleClass().add("dark");
            }
            if (themeToggleBtn != null) {
                themeToggleBtn.setText("\u2600  Jour");
            }
        } else {
            rootPane.getStyleClass().remove("dark");
            if (themeToggleBtn != null) {
                themeToggleBtn.setText("\u263D  Nuit");
            }
        }
    }

    private boolean isValidEmail(String e) {
        return e != null && e.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    }

    private void showError(String msg) {
        if (errorLabel == null) return;

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

    private void playEntranceAnimation() {
        if (loginCard != null) {
            loginCard.setOpacity(0);
            loginCard.setTranslateX(50);

            FadeTransition fade = new FadeTransition(Duration.millis(700), loginCard);
            fade.setFromValue(0);
            fade.setToValue(1);

            TranslateTransition slide = new TranslateTransition(Duration.millis(700), loginCard);
            slide.setFromX(50);
            slide.setToX(0);
            slide.setInterpolator(Interpolator.SPLINE(0.25, 0.1, 0.25, 1.0));

            new ParallelTransition(fade, slide).play();
        }

        if (loginBtn != null) {
            loginBtn.setOpacity(0);

            FadeTransition bf = new FadeTransition(Duration.millis(400), loginBtn);
            bf.setFromValue(0);
            bf.setToValue(1);
            bf.setDelay(Duration.millis(600));
            bf.play();
        }
    }

    private void animateLogoGlow() {
        if (logoGlow == null) return;

        Timeline pulse = new Timeline(
                new KeyFrame(Duration.ZERO,
                        new KeyValue(logoGlow.scaleXProperty(), 1.0, Interpolator.EASE_BOTH),
                        new KeyValue(logoGlow.scaleYProperty(), 1.0, Interpolator.EASE_BOTH),
                        new KeyValue(logoGlow.opacityProperty(), 0.65, Interpolator.EASE_BOTH)),
                new KeyFrame(Duration.millis(2200),
                        new KeyValue(logoGlow.scaleXProperty(), 1.12, Interpolator.EASE_BOTH),
                        new KeyValue(logoGlow.scaleYProperty(), 1.12, Interpolator.EASE_BOTH),
                        new KeyValue(logoGlow.opacityProperty(), 1.0, Interpolator.EASE_BOTH))
        );

        pulse.setCycleCount(Timeline.INDEFINITE);
        pulse.setAutoReverse(true);
        pulse.play();
    }

    private void animateBlobs() {
        floatBlob(blob1, 14, 10, 6500);
        floatBlob(blob2, -12, 16, 8000);
        floatBlob(blob3, 10, -12, 5800);
        floatBlob(blob4, -16, 8, 7200);
    }

    private void floatBlob(Circle b, double dx, double dy, double ms) {
        if (b == null) return;

        TranslateTransition tt = new TranslateTransition(Duration.millis(ms), b);
        tt.setByX(dx);
        tt.setByY(dy);
        tt.setCycleCount(TranslateTransition.INDEFINITE);
        tt.setAutoReverse(true);
        tt.setInterpolator(Interpolator.EASE_BOTH);
        tt.play();
    }

    private void shakeNode(Node n) {
        if (n == null) return;

        TranslateTransition tt = new TranslateTransition(Duration.millis(50), n);
        tt.setFromX(0);
        tt.setByX(8);
        tt.setCycleCount(6);
        tt.setAutoReverse(true);
        tt.setInterpolator(Interpolator.EASE_BOTH);
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

    private void bounceNode(Node n, double from) {
        if (n == null) return;

        ScaleTransition sc = new ScaleTransition(Duration.millis(130), n);
        sc.setFromX(from);
        sc.setFromY(from);
        sc.setToX(1.0);
        sc.setToY(1.0);
        sc.setInterpolator(Interpolator.EASE_OUT);
        sc.play();
    }

    private void addFocusStyle(Control field) {
        if (field == null) return;

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
    private void navigateToMainDashboard() {
        try {
            java.net.URL fxmlUrl = getClass().getResource("/FXML/user_auth/back/main_dashboard.fxml");

            if (fxmlUrl == null) {
                showError("main_dashboard.fxml introuvable dans /FXML/user_auth/back/");
                return;
            }

            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(fxmlUrl);
            javafx.scene.Parent root = loader.load();

            javafx.scene.Scene scene = emailField.getScene();
            scene.setRoot(root);

            scene.getStylesheets().clear();

            java.net.URL css = getClass().getResource("/styles/user_auth/back/main_dashboard.css");
            if (css != null) {
                scene.getStylesheets().add(css.toExternalForm());
            }

        } catch (Exception e) {
            e.printStackTrace();
            showError("Impossible de charger le dashboard principal.");
        }
    }
}