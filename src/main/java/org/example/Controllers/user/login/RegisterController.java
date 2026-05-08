package org.example.Controllers.user.login;

import javafx.animation.*;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.shape.Circle;
import javafx.stage.FileChooser;
import javafx.util.Duration;

import org.example.Entites.user.Role;
import org.example.Entites.user.Status;
import org.example.Entites.user.User;
import org.example.Services.user.UserCRUD;
import org.example.Utils.CameraCapture;
import org.example.Utils.MailSender;
import org.example.Utils.PhoneCountryDetector;
import org.example.Utils.WelcomeEmailTemplate;

import java.io.File;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class RegisterController {

    // =================== FXML FIELDS ===================

    @FXML private AnchorPane rootPane;
    @FXML private ImageView logoView;
    @FXML private Pane logoGlow;
    @FXML private VBox formCard;
    @FXML private ToggleButton themeToggleBtn;
    @FXML private Label emailIcon;
    @FXML private Pane emailStatus;
    @FXML private Pane matchStatus;

    @FXML private CheckBox faceIdCheck;

    // Formulaire
    @FXML private TextField nameField;
    @FXML private TextField prenomField;
    @FXML private DatePicker dateNaissField;
    @FXML private TextField emailField;
    @FXML private TextField numTelField;
    @FXML private PasswordField passwordField;
    @FXML private TextField passwordVisibleField;
    @FXML private ToggleButton showPasswordBtn;
    @FXML private PasswordField confirmPasswordField;

    @FXML private Label countryLabel;

    // Labels d'erreur en temps reel (FormValidator)
    @FXML private Label errNom;
    @FXML private Label errPrenom;
    @FXML private Label errDate;
    @FXML private Label errEmail;
    @FXML private Label errTel;
    @FXML private Label errPass;
    @FXML private Label errConfirm;

    // Avatar
    @FXML private StackPane avatarContainer;
    @FXML private ImageView avatarView;

    // Password strength
    @FXML private Pane str1, str2, str3, str4;
    @FXML private Label strengthLabel;

    // Boutons / messages
    @FXML private Button registerBtn;
    @FXML private Label errorLabel;
    @FXML private Label successLabel;
    @FXML private Hyperlink loginLink;

    // =================== LIVE PREVIEW FIELDS ===================

    @FXML private VBox previewCard;
    @FXML private ImageView previewAvatarImg;
    @FXML private Label previewAvatarLetter;
    @FXML private Label previewFullName;
    @FXML private Label previewRole;
    @FXML private Label previewEmail;
    @FXML private Label previewTel;
    @FXML private Label previewDate;
    @FXML private Label previewPass;
    @FXML private Label previewPercent;
    @FXML private Pane  previewProgressBar;
    @FXML private Label previewProgressText;

    @FXML private TextField adresseField;

    // Checklist
    @FXML private Label chkNom;
    @FXML private Label chkDate;
    @FXML private Label chkEmail;
    @FXML private Label chkTel;
    @FXML private Label chkPass;
    @FXML private Label chkConfirm;

    // =================== STATE ===================

    private File selectedImageFile = null;
    private final UserCRUD userCRUD = new UserCRUD();

    // Nombre total de champs a valider pour la barre de progression
    private static final int TOTAL_FIELDS = 6;

    @FXML
    public void initialize() {

        // =================== THEME ===================
        if (rootPane != null) {
            if (!rootPane.getStyleClass().contains("login-root"))
                rootPane.getStyleClass().add("login-root");
            setDarkMode(true);
        }
        themeToggleBtn.setSelected(true);
        themeToggleBtn.setText("\u2600  Jour");
        themeToggleBtn.selectedProperty().addListener((obs, o, sel) -> setDarkMode(sel));

        // =================== LOGO ===================
        try {
            java.io.InputStream s = getClass().getResourceAsStream("/user/image/logo.png");
            if (s != null && logoView != null) logoView.setImage(new Image(s));
        } catch (Exception e) { System.err.println("Logo introuvable"); }

        // =================== PROMPTS ===================
        emailIcon.setText("@");
        emailField.setPromptText("ex: prenom.nom@email.com");
        nameField.setPromptText("Votre nom");
        prenomField.setPromptText("Votre prenom");
        numTelField.setPromptText("+216 XX XXX XXX");
        passwordField.setPromptText("Min. 8 caracteres");
        passwordVisibleField.setPromptText("Min. 8 caracteres");
        confirmPasswordField.setPromptText("Retapez le mot de passe");
        showPasswordBtn.setText("\uD83D\uDC41");

        // =================== AVATAR ===================
        setupAvatar();
        setupDatePicker();

        // =================== TELEPHONE filtre ===================
        numTelField.textProperty().addListener((obs, old, val) -> {
            if (val != null && !val.matches("[0-9+\\- ]*")) {
                numTelField.setText(old);
                return;
            }

            updateCountryBadge(val);
        });

        // =================== PASSWORD TOGGLE ===================
        passwordVisibleField.textProperty().bindBidirectional(passwordField.textProperty());
        showPasswordBtn.selectedProperty().addListener((obs, o, sel) -> {
            passwordVisibleField.setManaged(sel); passwordVisibleField.setVisible(sel);
            passwordField.setManaged(!sel); passwordField.setVisible(!sel);
            showPasswordBtn.setText(sel ? "\uD83D\uDE48" : "\uD83D\uDC41");
        });

        // =================== VALIDATION EN TEMPS REEL via FormValidator ===================
        org.example.Utils.FormValidator.setupRegisterValidation(
                nameField,           errNom,
                prenomField,         errPrenom,
                dateNaissField,      errDate,
                emailField,          errEmail,
                numTelField,         errTel,
                passwordField,       errPass,
                confirmPasswordField, errConfirm
        );

        // Preview update sur chaque champ
        nameField.textProperty().addListener((o,a,b)           -> { updatePreview(); updateEmailStatus(); });
        prenomField.textProperty().addListener((o,a,b)         -> updatePreview());
        numTelField.textProperty().addListener((o,a,b)         -> updatePreview());
        dateNaissField.valueProperty().addListener((o,a,b)     -> updatePreview());
        emailField.textProperty().addListener((o,a,b)          -> { updatePreview(); updateEmailStatus(); });
        passwordField.textProperty().addListener((obs, old, val) -> {
            updatePasswordStrength(val);
            updatePreview();
        });
        confirmPasswordField.textProperty().addListener((o,a,b) -> updatePreview());

        // =================== FOCUS STYLES ===================
        addFocusStyle(nameField);    addFocusStyle(prenomField);
        addFocusStyle(emailField);   addFocusStyle(numTelField);
        addFocusStyle(passwordField); addFocusStyle(passwordVisibleField);
        addFocusStyle(confirmPasswordField);

        // =================== ANIMATIONS ===================
        playEntrance();
        animateGlow();

        // =================== ACTIONS ===================
        registerBtn.setOnAction(e -> onRegister());
        loginLink.setOnAction(e -> navigateToLogin());
        applyRegisterBtnStyle();

        // Init preview
        updatePreview();
    }

    // ============================================================
    // LIVE PREVIEW — mise a jour en temps reel
    // ============================================================

    private void updatePreview() {
        String nom    = getText(nameField);
        String prenom = getText(prenomField);
        String email  = getText(emailField);
        String tel    = getText(numTelField);
        String pass   = passwordField.getText() == null ? "" : passwordField.getText();
        String confirm = confirmPasswordField.getText() == null ? "" : confirmPasswordField.getText();
        LocalDate date = dateNaissField.getValue();

        // --- Nom complet ---
        String fullName = buildFullName(nom, prenom);
        previewFullName.setText(fullName.isBlank() ? "Votre nom" : fullName);

        // --- Lettre avatar ---
        if (selectedImageFile == null) {
            String letter = nom.isEmpty() ? (prenom.isEmpty() ? "?" : prenom.substring(0, 1).toUpperCase())
                    : nom.substring(0, 1).toUpperCase();
            previewAvatarLetter.setText(letter);
            previewAvatarLetter.setVisible(true);
        }

        // --- Email ---
        previewEmail.setText(email.isBlank() ? "\u2014" : email);
        previewEmail.setStyle(isValidEmail(email) ? "-fx-text-fill: #10B981;" : "");

        // --- Telephone ---
        previewTel.setText(tel.isBlank() ? "\u2014" : tel);

        // --- Date ---
        if (date != null) {
            previewDate.setText(date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        } else {
            previewDate.setText("\u2014");
        }

        // --- Mot de passe (masque avec etoiles) ---
        if (pass.isEmpty()) {
            previewPass.setText("\u2014");
        } else {
            StringBuilder masked = new StringBuilder();
            for (int i = 0; i < Math.min(pass.length(), 12); i++) masked.append("\u2022");
            previewPass.setText(masked.toString());
        }

        // --- Checklist + progression ---
        boolean okNom    = !nom.isBlank() && !prenom.isBlank();
        boolean okDate   = date != null;
        boolean okEmail  = isValidEmail(email);
        boolean okTel    = tel.replaceAll("[^0-9]", "").length() >= 8;
        boolean okPass   = pass.length() >= 8;
        boolean okConfirm = !confirm.isBlank() && confirm.equals(pass);

        setCheck(chkNom,    okNom);
        setCheck(chkDate,   okDate);
        setCheck(chkEmail,  okEmail);
        setCheck(chkTel,    okTel);
        setCheck(chkPass,   okPass);
        setCheck(chkConfirm, okConfirm);

        // --- Barre de progression ---
        int done = (okNom ? 1 : 0) + (okDate ? 1 : 0) + (okEmail ? 1 : 0)
                + (okTel ? 1 : 0) + (okPass ? 1 : 0) + (okConfirm ? 1 : 0);
        int pct = (int) ((done / (double) TOTAL_FIELDS) * 100);

        previewPercent.setText(pct + "%");

        // Animer la barre
        animateProgressBar(pct);

        // Couleur de la barre selon l'avancement
        String barColor;
        if (pct <= 30)      barColor = "#EF4444";
        else if (pct <= 60) barColor = "#F59E0B";
        else if (pct < 100) barColor = "#6366F1";
        else                barColor = "#10B981";

        previewProgressBar.setStyle(
                "-fx-background-color: " + barColor + ";" +
                        "-fx-background-radius: 3;"
        );
        previewPercent.setStyle("-fx-text-fill: " + barColor + "; -fx-font-weight: 800;");

        // Message hint
        if (pct == 0) {
            previewProgressText.setText("Remplissez les champs pour continuer");
        } else if (pct < 50) {
            previewProgressText.setText("Continuez, vous etes sur la bonne voie !");
        } else if (pct < 100) {
            previewProgressText.setText("Presque fini, encore quelques champs !");
        } else {
            previewProgressText.setText("Tout est valide ! Vous pouvez creer votre compte.");
            previewProgressText.setStyle("-fx-text-fill: #10B981; -fx-font-weight: 700;");
        }
    }

    private void updateEmailStatus() {
        String email = getText(emailField);
        boolean valid = isValidEmail(email);
        emailStatus.setManaged(valid); emailStatus.setVisible(valid);
    }

    private String buildFullName(String nom, String prenom) {
        if (nom.isBlank() && prenom.isBlank()) return "";
        if (nom.isBlank())   return capitalize(prenom);
        if (prenom.isBlank()) return capitalize(nom);
        return capitalize(nom) + " " + capitalize(prenom);
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return s.substring(0, 1).toUpperCase() + s.substring(1).toLowerCase();
    }

    private void setCheck(Label lbl, boolean ok) {
        lbl.getStyleClass().removeAll("chk-ok", "chk-empty");
        if (ok) {
            lbl.setText("\u2713");
            lbl.getStyleClass().add("chk-ok");
        } else {
            lbl.setText("O");
            lbl.getStyleClass().add("chk-empty");
        }
    }

    private double lastBarPct = 0;

    private void animateProgressBar(int targetPct) {
        double targetWidth = (targetPct / 100.0);

        // Obtenir la largeur du parent (track)
        if (previewProgressBar.getParent() == null) return;
        double trackW = ((javafx.scene.layout.StackPane) previewProgressBar.getParent()).getWidth();
        if (trackW <= 0) trackW = 220; // fallback

        double newW = trackW * targetWidth;
        double oldW = trackW * (lastBarPct / 100.0);
        lastBarPct = targetPct;

        if (Math.abs(newW - oldW) < 1) {
            previewProgressBar.setPrefWidth(newW);
            return;
        }

        Timeline tl = new Timeline(
                new KeyFrame(Duration.ZERO,
                        new KeyValue(previewProgressBar.prefWidthProperty(), oldW, Interpolator.EASE_BOTH)),
                new KeyFrame(Duration.millis(400),
                        new KeyValue(previewProgressBar.prefWidthProperty(), newW, Interpolator.EASE_BOTH))
        );
        tl.play();
    }

    // =================== AVATAR SETUP ===================

    private void setupAvatar() {
        Circle clip = new Circle(43, 43, 43);
        avatarView.setClip(clip);

        // Clip aussi pour la preview
        Circle previewClip = new Circle(43, 43, 43);
        previewAvatarImg.setClip(previewClip);

        avatarContainer.setStyle(
                "-fx-background-color: rgba(99,102,241,0.08);" +
                        "-fx-background-radius: 50;" +
                        "-fx-border-color: rgba(99,102,241,0.20);" +
                        "-fx-border-radius: 50;" +
                        "-fx-border-width: 2;" +
                        "-fx-border-style: dashed;" +
                        "-fx-cursor: hand;"
        );
        avatarContainer.setOnMouseClicked(e -> chooseImage());
        avatarContainer.setOnMouseEntered(e -> avatarContainer.setStyle(
                "-fx-background-color: rgba(99,102,241,0.15);" +
                        "-fx-background-radius: 50;" +
                        "-fx-border-color: rgba(99,102,241,0.35);" +
                        "-fx-border-radius: 50;" +
                        "-fx-border-width: 2;" +
                        "-fx-border-style: dashed;" +
                        "-fx-cursor: hand;"
        ));
        avatarContainer.setOnMouseExited(e -> {
            if (selectedImageFile != null) {
                avatarContainer.setStyle(
                        "-fx-background-color: transparent;" +
                                "-fx-background-radius: 50;" +
                                "-fx-border-color: rgba(16,185,129,0.50);" +
                                "-fx-border-radius: 50;" +
                                "-fx-border-width: 2;" +
                                "-fx-border-style: solid;" +
                                "-fx-cursor: hand;"
                );
            } else {
                avatarContainer.setStyle(
                        "-fx-background-color: rgba(99,102,241,0.08);" +
                                "-fx-background-radius: 50;" +
                                "-fx-border-color: rgba(99,102,241,0.20);" +
                                "-fx-border-radius: 50;" +
                                "-fx-border-width: 2;" +
                                "-fx-border-style: dashed;" +
                                "-fx-cursor: hand;"
                );
            }
        });
    }

    private void chooseImage() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Choisir une photo de profil");
        fc.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.bmp"));
        File file = fc.showOpenDialog(rootPane.getScene().getWindow());
        if (file != null) {
            selectedImageFile = file;
            Image img = new Image(file.toURI().toString(), 86, 86, false, true);
            avatarView.setImage(img);

            // Mise a jour de la preview avatar aussi
            previewAvatarImg.setImage(new Image(file.toURI().toString(), 86, 86, false, true));
            previewAvatarLetter.setVisible(false);

            avatarContainer.setStyle(
                    "-fx-background-color: transparent;" +
                            "-fx-background-radius: 50;" +
                            "-fx-border-color: rgba(16,185,129,0.50);" +
                            "-fx-border-radius: 50;" +
                            "-fx-border-width: 2;" +
                            "-fx-border-style: solid;" +
                            "-fx-cursor: hand;"
            );
            pulseNode(avatarContainer);
            pulseNode(previewAvatarImg);
        }
    }

    // =================== DATE PICKER ===================

    private void setupDatePicker() {
        dateNaissField.setPromptText("jj/mm/aaaa");
        dateNaissField.setEditable(false);
        dateNaissField.setDayCellFactory(picker -> new DateCell() {
            @Override
            public void updateItem(LocalDate d, boolean empty) {
                super.updateItem(d, empty);
                if (d.isAfter(LocalDate.now())) {
                    setDisable(true);
                    setStyle("-fx-background-color: rgba(239,68,68,0.1);");
                }
            }
        });
        dateNaissField.setStyle(
                "-fx-background-color: transparent;" +
                        "-fx-font-size: 13px;" +
                        "-fx-border-color: transparent;" +
                        "-fx-focus-color: transparent;" +
                        "-fx-faint-focus-color: transparent;"
        );
    }

    // =================== THEME ===================

    private void setDarkMode(boolean dark) {
        if (dark) {
            if (!rootPane.getStyleClass().contains("dark")) rootPane.getStyleClass().add("dark");
            themeToggleBtn.setText("\u2600  Jour");
        } else {
            rootPane.getStyleClass().remove("dark");
            themeToggleBtn.setText("\u263D  Nuit");
        }
    }

    // =================== REGISTER ===================

    private void onRegister() {
        // ===== VALIDATION GLOBALE via FormValidator =====
        boolean valid = org.example.Utils.FormValidator.validateAll(
                nameField,           errNom,
                prenomField,         errPrenom,
                dateNaissField,      errDate,
                emailField,          errEmail,
                numTelField,         errTel,
                passwordField,       errPass,
                confirmPasswordField, errConfirm
        );
        if (!valid) {
            shakeNode(formCard);
            return;
        }

        String nom     = getText(nameField);
        String prenom  = getText(prenomField);
        String email   = getText(emailField);
        String numTel  = getText(numTelField);
        String adresse = getText(adresseField);
        String pass    = passwordField.getText() == null ? "" : passwordField.getText();
        LocalDate dateNaiss = dateNaissField.getValue();

        hideError();
        String originalText = registerBtn.getText();
        registerBtn.setText("Creation en cours...");
        registerBtn.setDisable(true);
        registerBtn.setOpacity(0.8);
        PhoneCountryDetector.PhoneInfo phoneInfo =
                PhoneCountryDetector.detect(numTel);

        if (!phoneInfo.valid) {
            showError(phoneInfo.message);
            markFieldError(numTelField);
            shakeNode(numTelField.getParent());
            registerBtn.setText(originalText);
            registerBtn.setDisable(false);
            registerBtn.setOpacity(1.0);
            return;
        }

        PauseTransition delay = new PauseTransition(Duration.millis(100));
        delay.setOnFinished(ev -> {
            try {
                if (userCRUD.emailExists(email)) {
                    showError("Cette adresse email est deja utilisee.");
                    markFieldError(emailField); shakeNode(emailField.getParent()); emailField.requestFocus();
                    registerBtn.setText(originalText); registerBtn.setDisable(false); registerBtn.setOpacity(1.0); return;
                }

                String dateStr   = dateNaiss.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                String imagePath = selectedImageFile != null ? selectedImageFile.getAbsolutePath() : "";

                User newUser = new User(nom, prenom, dateStr, email, numTel, pass,
                        imagePath, Role.acheteur, Status.Unbanned);
                boolean faceEnabled = faceIdCheck != null && faceIdCheck.isSelected();
                String facePath = "";

                if (faceEnabled) {
                    try {
                        facePath = CameraCapture.capturePhoto();
                    } catch (Exception e) {
                        showError("Erreur Face ID");
                        registerBtn.setText(originalText);
                        registerBtn.setDisable(false);
                        registerBtn.setOpacity(1.0);
                        return;
                    }
                }

                newUser.setFaceIdEnabled(faceEnabled);
                newUser.setFaceImagePath(facePath);
                newUser.setAdresse(adresse);
                userCRUD.createUser(newUser);
                new Thread(() -> {
                    try {
                        String bannerUrl = "https://res.cloudinary.com/dgzm2bbkb/image/upload/v1777996341/7anouti-mail_mlld3v.png";
                        String loginUrl = "https://7anouti-e.com/login";
                        String avatarUrl = "";

                        String html = WelcomeEmailTemplate.buildLuxury(
                                nom,
                                prenom,
                                newUser.getRole().name(),
                                avatarUrl,
                                loginUrl,
                                bannerUrl
                        );

                        MailSender.sendHtmlMail(email, "Bienvenue sur 7anouti-E ✨", html);

                    } catch (Exception mailEx) {
                        System.err.println("Compte créé, mais email de bienvenue non envoyé.");
                        mailEx.printStackTrace();
                    }
                }).start();
                new Thread(() -> {
                    try {
                        MailSender.sendMail(
                                email,
                                "Bienvenue sur 7anouti-E 🎉",
                                "Bonjour " + prenom + " " + nom + ",\n\n" +
                                        "Nous sommes ravis de vous accueillir sur 7anouti-E !\n\n" +
                                        "Votre compte a été créé avec succès et vous pouvez dès maintenant profiter de toutes nos fonctionnalités.\n\n" +
                                        "Si vous avez des questions, n'hésitez pas à nous contacter.\n\n" +
                                        "À très bientôt,\n" +
                                        "L'équipe 7anouti-E 🚀"
                        );
                    } catch (Exception mailEx) {
                        System.err.println("Compte créé, mais email de bienvenue non envoyé.");
                        mailEx.printStackTrace();
                    }
                }).start();

                showSuccess("Compte cree avec succes ! Redirection...");
                registerBtn.setText("Cree \u2713");
                registerBtn.setOpacity(1.0);
                registerBtn.setStyle(
                        "-fx-background-color: linear-gradient(to right, #4338CA, #4F46E5, #6366F1);" +
                                "-fx-background-radius: 14; -fx-padding: 16 28;" +
                                "-fx-font-size: 14px; -fx-font-weight: 800;" +
                                "-fx-text-fill: white; -fx-background-insets: 0;" +
                                "-fx-effect: dropshadow(gaussian, rgba(99,102,241,0.40), 24, 0.18, 0, 10);"
                );
                pulseNode(registerBtn);

                PauseTransition redirect = new PauseTransition(Duration.millis(2000));
                redirect.setOnFinished(r -> navigateToLogin());
                redirect.play();

            } catch (SQLException ex) {
                showError("Erreur lors de la creation du compte: " + ex.getMessage());
                registerBtn.setText(originalText); registerBtn.setDisable(false); registerBtn.setOpacity(1.0);
                ex.printStackTrace();
            }
        });
        boolean faceEnabled = faceIdCheck.isSelected();
        String facePath = "";

        if (faceEnabled) {
            try {
                String folder = "faces/" + emailField.getText();

                new java.io.File(folder).mkdirs();

                for (int i = 0; i < 3; i++) {
                    String path = folder + "/img_" + i + ".png";
                    CameraCapture.capturePhoto();
                }

                facePath = folder;            } catch (Exception e) {
                showError("Erreur Face ID");
                return;
            }
        }
        delay.play();
    }

    private String getText(TextField field) {
        return field.getText() == null ? "" : field.getText().trim();
    }

    private void markFieldError(Control field) {
        Parent wrap = findInputWrap(field);
        if (wrap != null) {
            wrap.getStyleClass().removeAll("input-error", "input-ok");
            wrap.getStyleClass().add("input-error");
        }
    }

    // =================== REGISTER BTN STYLE ===================

    private static final String REGISTER_BTN_NORMAL =
            "-fx-background-color: linear-gradient(to right, #4338CA, #4F46E5, #6366F1);" +
                    "-fx-background-radius: 14; -fx-padding: 16 28; -fx-font-size: 14px;" +
                    "-fx-font-weight: 800; -fx-text-fill: white; -fx-cursor: hand;" +
                    "-fx-border-color: transparent; -fx-background-insets: 0;" +
                    "-fx-effect: dropshadow(gaussian, rgba(99,102,241,0.35), 24, 0.18, 0, 10);";

    private static final String REGISTER_BTN_HOVER =
            "-fx-background-color: linear-gradient(to right, #3730A3, #4338CA, #4F46E5);" +
                    "-fx-background-radius: 14; -fx-padding: 16 28; -fx-font-size: 14px;" +
                    "-fx-font-weight: 800; -fx-text-fill: white; -fx-cursor: hand;" +
                    "-fx-border-color: transparent; -fx-background-insets: 0;" +
                    "-fx-effect: dropshadow(gaussian, rgba(99,102,241,0.50), 30, 0.24, 0, 14);";

    private void applyRegisterBtnStyle() {
        registerBtn.setStyle(REGISTER_BTN_NORMAL);
        registerBtn.setOnMouseEntered(e -> registerBtn.setStyle(REGISTER_BTN_HOVER));
        registerBtn.setOnMouseExited(e  -> registerBtn.setStyle(REGISTER_BTN_NORMAL));
        registerBtn.setOnMousePressed(e ->
                registerBtn.setStyle(REGISTER_BTN_HOVER + "-fx-scale-x: 0.985; -fx-scale-y: 0.985;"));
        registerBtn.setOnMouseReleased(e -> registerBtn.setStyle(REGISTER_BTN_NORMAL));
    }

    // =================== PASSWORD STRENGTH ===================

    private void updatePasswordStrength(String pass) {
        String reset = "-fx-background-color: rgba(99,102,241,0.10);";
        str1.setStyle(reset); str2.setStyle(reset); str3.setStyle(reset); str4.setStyle(reset);

        if (pass == null || pass.isEmpty()) {
            strengthLabel.setManaged(false); strengthLabel.setVisible(false); return;
        }

        int score = 0;
        if (pass.length() >= 6)  score++;
        if (pass.length() >= 8)  score++;
        if (pass.matches(".*[A-Z].*") && pass.matches(".*[a-z].*")) score++;
        if (pass.matches(".*[0-9].*") || pass.matches(".*[^A-Za-z0-9].*")) score++;

        String weak = "-fx-background-color: #EF4444;";
        String med  = "-fx-background-color: #F59E0B;";
        String ok   = "-fx-background-color: #10B981;";

        strengthLabel.setManaged(true); strengthLabel.setVisible(true);
        if      (score == 1) { str1.setStyle(weak); strengthLabel.setText("Faible"); strengthLabel.setStyle("-fx-text-fill: #EF4444;"); }
        else if (score == 2) { str1.setStyle(med);  str2.setStyle(med);  strengthLabel.setText("Moyen");    strengthLabel.setStyle("-fx-text-fill: #F59E0B;"); }
        else if (score == 3) { str1.setStyle(ok);   str2.setStyle(ok);   str3.setStyle(ok);  strengthLabel.setText("Fort");      strengthLabel.setStyle("-fx-text-fill: #10B981;"); }
        else if (score >= 4) { str1.setStyle(ok);   str2.setStyle(ok);   str3.setStyle(ok);  str4.setStyle(ok); strengthLabel.setText("Excellent"); strengthLabel.setStyle("-fx-text-fill: #059669;"); }
    }

    // =================== NAVIGATION ===================

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
        } catch (Exception e) { showError("Impossible de charger la page de connexion."); e.printStackTrace(); }
    }

    // =================== VALIDATION ===================

    private boolean isValidEmail(String e) {
        return e != null && e.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    }

    // =================== ERROR / SUCCESS ===================

    private void showError(String msg) {
        hideSuccess();
        errorLabel.setText(msg); errorLabel.setManaged(true); errorLabel.setVisible(true);
        FadeTransition ft = new FadeTransition(Duration.millis(200), errorLabel); ft.setFromValue(0); ft.setToValue(1);
        TranslateTransition tt = new TranslateTransition(Duration.millis(200), errorLabel);
        tt.setFromY(-10); tt.setToY(0); tt.setInterpolator(Interpolator.EASE_OUT);
        new ParallelTransition(ft, tt).play();
    }
    private void hideError() {
        if (errorLabel == null || !errorLabel.isVisible()) return;
        errorLabel.setVisible(false); errorLabel.setManaged(false); errorLabel.setText("");
    }
    private void showSuccess(String msg) {
        hideError();
        successLabel.setText(msg); successLabel.setManaged(true); successLabel.setVisible(true);
        FadeTransition ft = new FadeTransition(Duration.millis(200), successLabel);
        ft.setFromValue(0);
        ft.setToValue(1);
        ft.play();
    }
    private void hideSuccess() {
        if (successLabel == null || !successLabel.isVisible()) return;
        successLabel.setVisible(false); successLabel.setManaged(false); successLabel.setText("");
    }

    // =================== ANIMATIONS ===================

    private void playEntrance() {
        if (formCard != null) {
            formCard.setOpacity(0); formCard.setTranslateX(50);
            FadeTransition fade = new FadeTransition(Duration.millis(700), formCard); fade.setFromValue(0); fade.setToValue(1);
            TranslateTransition slide = new TranslateTransition(Duration.millis(700), formCard);
            slide.setFromX(50); slide.setToX(0); slide.setInterpolator(Interpolator.SPLINE(0.25, 0.1, 0.25, 1.0));
            new ParallelTransition(fade, slide).play();
        }
        if (previewCard != null) {
            previewCard.setOpacity(0); previewCard.setTranslateX(40);
            FadeTransition fp = new FadeTransition(Duration.millis(700), previewCard); fp.setFromValue(0); fp.setToValue(1);
            TranslateTransition sp = new TranslateTransition(Duration.millis(700), previewCard);
            sp.setFromX(40); sp.setToX(0); sp.setInterpolator(Interpolator.SPLINE(0.25, 0.1, 0.25, 1.0));
            ParallelTransition pt = new ParallelTransition(fp, sp);
            pt.setDelay(Duration.millis(150)); pt.play();
        }
        if (logoView != null) {
            logoView.setOpacity(0); logoView.setScaleX(0.5); logoView.setScaleY(0.5);
            FadeTransition lf = new FadeTransition(Duration.millis(600), logoView); lf.setFromValue(0); lf.setToValue(1);
            ScaleTransition ls1 = new ScaleTransition(Duration.millis(500), logoView);
            ls1.setFromX(0.5); ls1.setFromY(0.5); ls1.setToX(1.08); ls1.setToY(1.08);
            ScaleTransition ls2 = new ScaleTransition(Duration.millis(200), logoView);
            ls2.setFromX(1.08); ls2.setFromY(1.08); ls2.setToX(1.0); ls2.setToY(1.0);
            ParallelTransition lp = new ParallelTransition(lf, new SequentialTransition(ls1, ls2));
            lp.setDelay(Duration.millis(200)); lp.play();
        }
    }

    private void animateGlow() {
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
        pulse.setCycleCount(Timeline.INDEFINITE); pulse.setAutoReverse(true); pulse.play();
    }

    private void shakeNode(Node n) {
        if (n == null) return;
        TranslateTransition tt = new TranslateTransition(Duration.millis(50), n);
        tt.setFromX(0); tt.setByX(8); tt.setCycleCount(6); tt.setAutoReverse(true);
        tt.setOnFinished(e -> n.setTranslateX(0)); tt.play();
    }

    private void pulseNode(Node n) {
        if (n == null) return;
        ScaleTransition st = new ScaleTransition(Duration.millis(200), n);
        st.setFromX(1.0); st.setToX(1.05); st.setFromY(1.0); st.setToY(1.05);
        st.setCycleCount(2); st.setAutoReverse(true); st.play();
    }

    private void addFocusStyle(Control field) {
        field.focusedProperty().addListener((obs, o, focused) -> {
            Parent wrap = findInputWrap(field);
            if (wrap == null) return;
            if (focused) { if (!wrap.getStyleClass().contains("is-focused")) wrap.getStyleClass().add("is-focused"); }
            else { wrap.getStyleClass().remove("is-focused"); }
        });
    }

    private Parent findInputWrap(Control field) {
        Parent p = field.getParent();
        while (p != null) {
            if (p.getStyleClass() != null && p.getStyleClass().contains("input-wrap")) return p;
            p = p.getParent();
        }
        return null;
    }
    private void updateCountryBadge(String phone) {
        if (countryLabel == null) return;

        PhoneCountryDetector.PhoneInfo info =
                org.example.Utils.PhoneCountryDetector.detect(phone);

        if (phone == null || phone.isBlank()) {
            countryLabel.setText("");
            countryLabel.setManaged(false);
            countryLabel.setVisible(false);
            return;
        }

        countryLabel.setText(info.flag + " " + info.country);

        if (info.valid) {
            countryLabel.setStyle(
                    "-fx-background-color: rgba(16,185,129,0.12);" +
                            "-fx-text-fill: #059669;" +
                            "-fx-background-radius: 999;" +
                            "-fx-padding: 6 10;" +
                            "-fx-font-size: 11px;" +
                            "-fx-font-weight: 800;"
            );
        } else {
            countryLabel.setStyle(
                    "-fx-background-color: rgba(239,68,68,0.12);" +
                            "-fx-text-fill: #DC2626;" +
                            "-fx-background-radius: 999;" +
                            "-fx-padding: 6 10;" +
                            "-fx-font-size: 11px;" +
                            "-fx-font-weight: 800;"
            );
        }

        countryLabel.setManaged(true);
        countryLabel.setVisible(true);
    }
}