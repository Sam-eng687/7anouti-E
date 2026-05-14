package tn.hanouti.livreur.controller;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import tn.hanouti.livreur.dao.LivreurDAO;
import tn.hanouti.livreur.model.Livreur;

import java.io.File;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.Period;

public class AjouterLivreurController {

    @FXML private TextField nomField;
    @FXML private TextField telField;
    @FXML private DatePicker dateNaissancePicker;
    @FXML private ComboBox<String> vehiculeCombo;
    @FXML private ImageView photoPreview;
    @FXML private Label labelPhoto;
    @FXML private Label messageLabel;

    private LivreurDAO dao = new LivreurDAO();
    private String cheminPhoto = null; // chemin absolu vers la photo choisie

    // Callback : appelé par GestionLivreursController après ajout réussi
    private Runnable onSuccess;

    public void setOnSuccess(Runnable callback) {
        this.onSuccess = callback;
    }

    @FXML
    public void initialize() {
        vehiculeCombo.setItems(FXCollections.observableArrayList("Voiture", "Petit camion"));
        vehiculeCombo.setValue("Voiture");
    }

    // ─────────────────────────────────────────────
    // CHOISIR UNE PHOTO — FileChooser
    // ─────────────────────────────────────────────
    @FXML
    public void choisirPhoto() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Choisir une photo");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif")
        );

        Stage stage = (Stage) nomField.getScene().getWindow();
        File fichier = chooser.showOpenDialog(stage);

        if (fichier != null) {
            cheminPhoto = fichier.getAbsolutePath();
            labelPhoto.setText(fichier.getName());
            labelPhoto.setStyle("-fx-text-fill: #1D9E75; -fx-font-size: 11;");

            // Afficher la prévisualisation
            try {
                Image img = new Image(fichier.toURI().toString());
                photoPreview.setImage(img);
            } catch (Exception e) {
                labelPhoto.setText("Aperçu non disponible");
            }
        }
    }

    // ─────────────────────────────────────────────
    // AJOUTER — validation + insertion
    // ─────────────────────────────────────────────
    @FXML
    public void ajouter() {
        String nom = nomField.getText().trim();
        String tel = telField.getText().trim();
        LocalDate dateNaissance = dateNaissancePicker.getValue();

        // ── Validations ──
        if (nom.isEmpty()) {
            afficherErreur("Le nom est obligatoire !");
            return;
        }
        if (tel.isEmpty() || !tel.matches("\\d{8}")) {
            afficherErreur("Téléphone : exactement 8 chiffres requis !");
            return;
        }
        if (dateNaissance == null) {
            afficherErreur("La date de naissance est obligatoire !");
            return;
        }
        if (dateNaissance.isAfter(LocalDate.now().minusYears(18))) {
            afficherErreur("Le livreur doit avoir au moins 18 ans !");
            return;
        }

        // ── Calcul de l'âge pour vérification ──
        int age = Period.between(dateNaissance, LocalDate.now()).getYears();

        // ── Créer l'objet Livreur ──
        Livreur l = new Livreur(nom, tel, 1);
        l.setDateNaissance(dateNaissance);
        l.setPhoto(cheminPhoto); // peut être null si pas de photo
        l.setScore(0);
        l.setGenreVehicule(vehiculeCombo.getValue() != null
                ? vehiculeCombo.getValue() : "Voiture");

        // ── Insérer en base ──
        try {
            dao.add(l); // lève une exception si doublon
            fermerFenetre();
            if (onSuccess != null) onSuccess.run(); // rafraîchir la liste
        } catch (SQLException e) {
            afficherErreur(e.getMessage());
        }
    }

    // ─────────────────────────────────────────────
    // ANNULER
    // ─────────────────────────────────────────────
    @FXML
    public void annuler() {
        fermerFenetre();
    }

    // ─────────────────────────────────────────────
    // UTILITAIRES
    // ─────────────────────────────────────────────
    private void afficherErreur(String msg) {
        messageLabel.setStyle("-fx-text-fill: #e74c3c; -fx-font-size: 12;");
        messageLabel.setText(msg);
    }

    private void fermerFenetre() {
        Stage stage = (Stage) nomField.getScene().getWindow();
        stage.close();
    }
}
