package projet.hanouti.common.utils;

import javafx.animation.*;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.util.Duration;

import java.time.LocalDate;
import java.time.Period;

/**
 * FormValidator — Validateur universel en temps reel
 * Utilisable dans : RegisterController, LoginController, DashboardController (edit)
 *
 * Usage :
 *   FormValidator.validateNom(nomField, errorLabel);
 *   FormValidator.validateEmail(emailField, errorLabel, userCRUD); // avec check BD
 *   FormValidator.setupAll(fields...);
 */
public class FormValidator {

    // ============================================================
    // COULEURS
    // ============================================================
    private static final String COLOR_OK    = "#10B981";
    private static final String COLOR_WARN  = "#F59E0B";
    private static final String COLOR_ERROR = "#EF4444";
    private static final String COLOR_INFO  = "#6366F1";

    // ============================================================
    // NOM / PRENOM
    // ============================================================

    public static boolean validateNom(TextField field, Label errorLabel) {
        String val = field.getText() == null ? "" : field.getText().trim();

        if (val.isEmpty()) {
            showError(errorLabel, "Le nom est requis");
            setFieldState(field, "error");
            return false;
        }
        if (val.length() < 2) {
            showError(errorLabel, "Minimum 2 caracteres");
            setFieldState(field, "error");
            return false;
        }
        if (val.length() > 50) {
            showError(errorLabel, "Maximum 50 caracteres");
            setFieldState(field, "error");
            return false;
        }
        if (!val.matches("^[a-zA-Z\u00C0-\u00FF\\s-]+$")) {
            showError(errorLabel, "Caracteres invalides (lettres uniquement)");
            setFieldState(field, "error");
            return false;
        }

        showSuccess(errorLabel, "Valide");
        setFieldState(field, "ok");
        return true;
    }

    public static boolean validatePrenom(TextField field, Label errorLabel) {
        return validateNom(field, errorLabel); // memes regles
    }

    // ============================================================
    // EMAIL
    // ============================================================

    public static boolean validateEmail(TextField field, Label errorLabel) {
        String val = field.getText() == null ? "" : field.getText().trim();

        if (val.isEmpty()) {
            showError(errorLabel, "L'email est requis");
            setFieldState(field, "error");
            return false;
        }
        if (val.length() > 100) {
            showError(errorLabel, "Email trop long (max 100 caracteres)");
            setFieldState(field, "error");
            return false;
        }
        if (!val.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
            showError(errorLabel, "Format invalide — ex: nom@domaine.com");
            setFieldState(field, "error");
            return false;
        }

        showSuccess(errorLabel, "Email valide");
        setFieldState(field, "ok");
        return true;
    }

    /**
     * Validation email avec verif existence en BD (asynchrone)
     */
    public static void validateEmailAsync(TextField field, Label errorLabel,
                                          projet.hanouti.user_auth.services.UserCRUD userCRUD,
                                          Integer excludeUserId) {
        String val = field.getText() == null ? "" : field.getText().trim();

        // Validation format d'abord
        if (!validateEmail(field, errorLabel)) return;

        // Check existance en BD dans un thread
        showInfo(errorLabel, "Verification en cours...");
        new Thread(() -> {
            try {
                Thread.sleep(400); // debounce
                projet.hanouti.user_auth.entities.User existing = userCRUD.getUserByEmail(val);
                javafx.application.Platform.runLater(() -> {
                    boolean taken = existing != null
                            && (excludeUserId == null || existing.getId() != excludeUserId);
                    if (taken) {
                        showError(errorLabel, "Cet email est deja utilise");
                        setFieldState(field, "error");
                    } else {
                        showSuccess(errorLabel, "Email disponible");
                        setFieldState(field, "ok");
                    }
                });
            } catch (Exception e) {
                javafx.application.Platform.runLater(() -> clearError(errorLabel));
            }
        }).start();
    }

    // ============================================================
    // TELEPHONE
    // ============================================================

    public static boolean validateTelephone(TextField field, Label errorLabel) {
        String val = field.getText() == null ? "" : field.getText().trim();

        if (val.isEmpty()) {
            showError(errorLabel, "Le telephone est requis");
            setFieldState(field, "error");
            return false;
        }

        String digits = val.replaceAll("[^0-9]", "");
        if (digits.length() < 8) {
            showError(errorLabel, "Minimum 8 chiffres requis");
            setFieldState(field, "error");
            return false;
        }
        if (digits.length() > 15) {
            showError(errorLabel, "Numero trop long");
            setFieldState(field, "error");
            return false;
        }
        if (!val.matches("[0-9+\\-\\s]+")) {
            showError(errorLabel, "Caracteres invalides");
            setFieldState(field, "error");
            return false;
        }

        showSuccess(errorLabel, "Numero valide");
        setFieldState(field, "ok");
        return true;
    }

    // ============================================================
    // DATE DE NAISSANCE
    // ============================================================

    public static boolean validateDateNaissance(DatePicker picker, Label errorLabel) {
        LocalDate val = picker.getValue();

        if (val == null) {
            showError(errorLabel, "La date de naissance est requise");
            return false;
        }
        if (val.isAfter(LocalDate.now())) {
            showError(errorLabel, "La date ne peut pas etre dans le futur");
            return false;
        }

        int age = Period.between(val, LocalDate.now()).getYears();
        if (age < 13) {
            showError(errorLabel, "Vous devez avoir au moins 13 ans");
            return false;
        }
        if (age > 120) {
            showError(errorLabel, "Date invalide");
            return false;
        }

        showSuccess(errorLabel, "Age : " + age + " ans");
        return true;
    }

    // ============================================================
    // MOT DE PASSE
    // ============================================================

    public static boolean validatePassword(PasswordField field, Label errorLabel) {
        String val = field.getText() == null ? "" : field.getText();

        if (val.isEmpty()) {
            showError(errorLabel, "Le mot de passe est requis");
            setFieldState(field, "error");
            return false;
        }
        if (val.length() < 8) {
            showError(errorLabel, "Minimum 8 caracteres");
            setFieldState(field, "error");
            return false;
        }
        if (val.length() > 128) {
            showError(errorLabel, "Mot de passe trop long");
            setFieldState(field, "error");
            return false;
        }
        if (!val.matches(".*[A-Z].*")) {
            showWarn(errorLabel, "Ajoutez une majuscule");
            setFieldState(field, "warn");
            return false;
        }
        if (!val.matches(".*[a-z].*")) {
            showWarn(errorLabel, "Ajoutez une minuscule");
            setFieldState(field, "warn");
            return false;
        }
        if (!val.matches(".*[0-9].*")) {
            showWarn(errorLabel, "Ajoutez un chiffre");
            setFieldState(field, "warn");
            return false;
        }

        showSuccess(errorLabel, "Mot de passe fort");
        setFieldState(field, "ok");
        return true;
    }

    /**
     * Validation "confirmer mot de passe"
     */
    public static boolean validateConfirmPassword(PasswordField confirmField,
                                                  PasswordField originalField,
                                                  Label errorLabel) {
        String confirm  = confirmField.getText() == null ? "" : confirmField.getText();
        String original = originalField.getText() == null ? "" : originalField.getText();

        if (confirm.isEmpty()) {
            showError(errorLabel, "Veuillez confirmer le mot de passe");
            setFieldState(confirmField, "error");
            return false;
        }
        if (!confirm.equals(original)) {
            showError(errorLabel, "Les mots de passe ne correspondent pas");
            setFieldState(confirmField, "error");
            return false;
        }

        showSuccess(errorLabel, "Mots de passe identiques");
        setFieldState(confirmField, "ok");
        return true;
    }

    // ============================================================
    // CHAMP GENERIQUE (TextField non vide)
    // ============================================================

    public static boolean validateRequired(TextField field, Label errorLabel, String fieldName) {
        String val = field.getText() == null ? "" : field.getText().trim();
        if (val.isEmpty()) {
            showError(errorLabel, fieldName + " est requis");
            setFieldState(field, "error");
            return false;
        }
        clearError(errorLabel);
        setFieldState(field, "ok");
        return true;
    }

    // ============================================================
    // SETUP LISTENERS — branchement automatique sur les champs
    // ============================================================

    /**
     * Brancher la validation en temps reel sur : nom, prenom, email, tel, date, pass, confirm
     */
    public static void setupRegisterValidation(
            TextField nomField,    Label nomError,
            TextField prenomField, Label prenomError,
            DatePicker datePicker, Label dateError,
            TextField emailField,  Label emailError,
            TextField telField,    Label telError,
            PasswordField passField,    Label passError,
            PasswordField confirmField, Label confirmError
    ) {
        if (nomField    != null) nomField.textProperty().addListener(   (o,a,b) -> validateNom(nomField, nomError));
        if (prenomField != null) prenomField.textProperty().addListener((o,a,b) -> validatePrenom(prenomField, prenomError));
        if (emailField  != null) emailField.textProperty().addListener( (o,a,b) -> validateEmail(emailField, emailError));
        if (telField    != null) telField.textProperty().addListener(   (o,a,b) -> validateTelephone(telField, telError));
        if (datePicker  != null) datePicker.valueProperty().addListener((o,a,b) -> validateDateNaissance(datePicker, dateError));
        if (passField   != null) passField.textProperty().addListener(  (o,a,b) -> {
            validatePassword(passField, passError);
            if (confirmField != null && !confirmField.getText().isEmpty())
                validateConfirmPassword(confirmField, passField, confirmError);
        });
        if (confirmField != null) confirmField.textProperty().addListener((o,a,b) ->
                validateConfirmPassword(confirmField, passField, confirmError));
    }

    /**
     * Brancher la validation en temps reel sur : nom, prenom, email, tel (edit dashboard)
     */
    public static void setupEditValidation(
            TextField nomField,    Label nomError,
            TextField prenomField, Label prenomError,
            TextField emailField,  Label emailError,
            TextField telField,    Label telError
    ) {
        if (nomField    != null) nomField.textProperty().addListener(   (o,a,b) -> validateNom(nomField, nomError));
        if (prenomField != null) prenomField.textProperty().addListener((o,a,b) -> validatePrenom(prenomField, prenomError));
        if (emailField  != null) emailField.textProperty().addListener( (o,a,b) -> validateEmail(emailField, emailError));
        if (telField    != null) telField.textProperty().addListener(   (o,a,b) -> validateTelephone(telField, telError));
    }

    // ============================================================
    // VALIDATION GLOBALE — verifier tous les champs d'un coup
    // ============================================================

    public static boolean validateAll(
            TextField nomField,    Label nomError,
            TextField prenomField, Label prenomError,
            DatePicker datePicker, Label dateError,
            TextField emailField,  Label emailError,
            TextField telField,    Label telError,
            PasswordField passField,    Label passError,
            PasswordField confirmField, Label confirmError
    ) {
        boolean ok = true;
        if (nomField    != null && !validateNom(nomField, nomError))            ok = false;
        if (prenomField != null && !validatePrenom(prenomField, prenomError))   ok = false;
        if (datePicker  != null && !validateDateNaissance(datePicker, dateError)) ok = false;
        if (emailField  != null && !validateEmail(emailField, emailError))      ok = false;
        if (telField    != null && !validateTelephone(telField, telError))      ok = false;
        if (passField   != null && !validatePassword(passField, passError))     ok = false;
        if (confirmField != null && !validateConfirmPassword(confirmField, passField, confirmError)) ok = false;
        return ok;
    }

    public static boolean validateAllEdit(
            TextField nomField,    Label nomError,
            TextField prenomField, Label prenomError,
            TextField emailField,  Label emailError,
            TextField telField,    Label telError
    ) {
        boolean ok = true;
        if (nomField    != null && !validateNom(nomField, nomError))           ok = false;
        if (prenomField != null && !validatePrenom(prenomField, prenomError))  ok = false;
        if (emailField  != null && !validateEmail(emailField, emailError))     ok = false;
        if (telField    != null && !validateTelephone(telField, telError))     ok = false;
        return ok;
    }

    // ============================================================
    // AFFICHAGE DES MESSAGES
    // ============================================================

    public static void showError(Label label, String message) {
        if (label == null) return;
        label.setText("\u26A0 " + message);
        label.setStyle(
                "-fx-text-fill: " + COLOR_ERROR + ";" +
                        "-fx-font-size: 10px;" +
                        "-fx-font-weight: 700;"
        );
        label.setVisible(true);
        label.setManaged(true);
        animateLabel(label);
    }

    public static void showWarn(Label label, String message) {
        if (label == null) return;
        label.setText("\u26A1 " + message);
        label.setStyle(
                "-fx-text-fill: " + COLOR_WARN + ";" +
                        "-fx-font-size: 10px;" +
                        "-fx-font-weight: 700;"
        );
        label.setVisible(true);
        label.setManaged(true);
    }

    public static void showSuccess(Label label, String message) {
        if (label == null) return;
        label.setText("\u2713 " + message);
        label.setStyle(
                "-fx-text-fill: " + COLOR_OK + ";" +
                        "-fx-font-size: 10px;" +
                        "-fx-font-weight: 700;"
        );
        label.setVisible(true);
        label.setManaged(true);
    }

    public static void showInfo(Label label, String message) {
        if (label == null) return;
        label.setText("\u29D7 " + message);
        label.setStyle(
                "-fx-text-fill: " + COLOR_INFO + ";" +
                        "-fx-font-size: 10px;" +
                        "-fx-font-weight: 600;" +
                        "-fx-font-style: italic;"
        );
        label.setVisible(true);
        label.setManaged(true);
    }

    public static void clearError(Label label) {
        if (label == null) return;
        label.setText("");
        label.setVisible(false);
        label.setManaged(false);
    }

    // ============================================================
    // STYLE DES CHAMPS (bordure verte/rouge/amber)
    // ============================================================

    public static void setFieldState(Control field, String state) {
        if (field == null) return;
        Node wrap = findInputWrap(field);
        if (wrap != null) {
            wrap.getStyleClass().removeAll("input-ok", "input-error", "input-warn");
            switch (state) {
                case "ok"    -> wrap.getStyleClass().add("input-ok");
                case "error" -> wrap.getStyleClass().add("input-error");
                case "warn"  -> wrap.getStyleClass().add("input-warn");
            }
        } else {
            switch (state) {
                case "ok"    -> field.setStyle("-fx-border-color: #10B981; -fx-border-width: 1.5;");
                case "error" -> field.setStyle("-fx-border-color: #EF4444; -fx-border-width: 1.5;");
                case "warn"  -> field.setStyle("-fx-border-color: #F59E0B; -fx-border-width: 1.5;");
            }
        }
    }

    public static void clearFieldState(Control field) {
        if (field == null) return;
        Node wrap = findInputWrap(field);
        if (wrap != null) {
            wrap.getStyleClass().removeAll("input-ok", "input-error", "input-warn");
        } else {
            field.setStyle("");
        }
    }

    // ============================================================
    // ANIMATION DU LABEL D'ERREUR (mini shake)
    // ============================================================

    private static void animateLabel(Label label) {
        TranslateTransition tt = new TranslateTransition(Duration.millis(40), label);
        tt.setFromX(0); tt.setByX(5);
        tt.setCycleCount(4); tt.setAutoReverse(true);
        tt.setOnFinished(e -> label.setTranslateX(0));
        tt.play();
    }

    // ============================================================
    // UTILITAIRES
    // ============================================================

    private static Node findInputWrap(Control field) {
        javafx.scene.Parent p = field.getParent();
        while (p != null) {
            if (p.getStyleClass() != null && p.getStyleClass().contains("input-wrap")) return p;
            p = p.getParent();
        }
        return null;
    }
}