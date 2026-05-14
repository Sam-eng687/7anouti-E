package tn.hanouti.livreur.controller;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.control.ScrollPane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import tn.hanouti.livreur.dao.LivreurDAO;
import tn.hanouti.livreur.dao.ScoreDAO;
import tn.hanouti.livreur.dao.SuiviLivraisonDAO;
import tn.hanouti.livreur.model.Livreur;
import tn.hanouti.livreur.model.SuiviLivraison;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

public class GestionLivreursController {

    // ── Livreurs Tab ──
    @FXML private ListView<Livreur> listView;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> filtreCombo;
    @FXML private Label lblCountLivreurs;

    // ── Detail Panel ──
    @FXML private VBox detailPanel;
    @FXML private VBox detailEmpty;
    @FXML private ScrollPane detailContent;
    @FXML private Label detailNom;
    @FXML private Label detailTel;
    @FXML private Label detailDispo;
    @FXML private Label detailAge;
    @FXML private Label detailVehicule;
    @FXML private Label detailScore;
    @FXML private ListView<String> detailHistorique;
    @FXML private ImageView detailPhoto;
    @FXML private Label detailPhotoFallback;

    // ── Livraisons Tab ──
    @FXML private ListView<SuiviLivraison> listLivraisons;
    @FXML private VBox affectEmpty;
    @FXML private ScrollPane affectContent;
    @FXML private Label affectCmd;
    @FXML private Label affectAdresse;
    @FXML private ListView<Livreur> listeDispo;

    // ── KPI ──
    @FXML private Label kpiTotal;
    @FXML private Label kpiDisponibles;
    @FXML private Label kpiEnLivraison;
    @FXML private Label kpiEnAttente;

    // ── Message ──
    @FXML private Label messageLabel;
    @FXML private Button btnModeSwitch;
    private boolean isDarkMode = true;
    private LivreurDAO dao = new LivreurDAO();
    private SuiviLivraisonDAO suiviDAO = new SuiviLivraisonDAO();
    private List<Livreur> tousLesLivreurs;
    private Livreur livreurSelectionne;
    private SuiviLivraison livraisonSelectionnee;


    @FXML
    public void initialize() {
        // Filtre
        filtreCombo.setItems(FXCollections.observableArrayList(
                "Tous", "Disponible", "Non disponible"));
        filtreCombo.setValue("Tous");
        filtreCombo.setOnAction(e -> filtrer());

        // Cellule liste livreurs
        listView.setCellFactory(lv -> new ListCell<Livreur>() {
            @Override
            protected void updateItem(Livreur l, boolean empty) {
                super.updateItem(l, empty);
                if (empty || l == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    HBox row = new HBox(15);
                    row.setStyle("-fx-padding: 10 20; -fx-alignment: center-left;");

                    Label nom = new Label(l.getNomLivreur());
                    nom.setPrefWidth(200);
                    nom.setStyle("-fx-font-size: 13px; -fx-font-weight: bold;");

                    Label tel = new Label(l.getTelephone());
                    tel.setPrefWidth(120);
                    tel.setStyle("-fx-font-size: 12px;");

                    Label age = new Label(l.getDateNaissance() != null
                            ? l.getAge() + " ans" : "—");
                    age.setPrefWidth(60);
                    age.setStyle("-fx-font-size: 12px;");

                    Label vehicule = new Label(l.getGenreVehicule() != null
                            ? l.getGenreVehicule() : "—");
                    vehicule.setPrefWidth(120);
                    vehicule.setStyle("-fx-font-size: 12px;");

                    Label dispo = new Label(l.isDisponibilite() ? "Oui" : "Non");
                    dispo.setPrefWidth(100);
                    dispo.setStyle(l.isDisponibilite()
                            ? "-fx-background-color: rgba(16,185,129,0.15); " +
                            "-fx-text-fill: #10B981; -fx-background-radius: 6; " +
                            "-fx-padding: 2 10; -fx-font-size: 11px; -fx-font-weight: 700;"
                            : "-fx-background-color: rgba(244,114,182,0.15); " +
                            "-fx-text-fill: #F472B6; -fx-background-radius: 6; " +
                            "-fx-padding: 2 10; -fx-font-size: 11px; -fx-font-weight: 700;");

                    Label score = new Label("⭐ " + l.getScore());
                    score.setPrefWidth(80);
                    score.setStyle("-fx-font-size: 12px;");

                    row.getChildren().addAll(nom, tel, age, vehicule, dispo, score);
                    setGraphic(row);
                }
            }
        });

        // Cellule liste livraisons
        listLivraisons.setCellFactory(lv -> new ListCell<SuiviLivraison>() {
            @Override
            protected void updateItem(SuiviLivraison s, boolean empty) {
                super.updateItem(s, empty);
                if (empty || s == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    HBox row = new HBox(15);
                    row.setStyle("-fx-padding: 10 20; -fx-alignment: center-left;");

                    Label idCmd = new Label("#" + s.getIdCommande());
                    idCmd.setPrefWidth(130);
                    idCmd.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #2563EB;");

                    Label adresse = new Label(s.getAdresseClient() != null
                            ? s.getAdresseClient() : "—");
                    adresse.setPrefWidth(320);
                    adresse.setStyle("-fx-font-size: 12px;");
                    adresse.setWrapText(true);

                    String statutStyle = "EN_ATTENTE".equals(s.getStatut())
                            ? "-fx-background-color: rgba(249,115,22,0.15); " +
                            "-fx-text-fill: #F97316; -fx-background-radius: 6; " +
                            "-fx-padding: 2 10; -fx-font-size: 11px; -fx-font-weight: 700;"
                            : "-fx-background-color: rgba(16,185,129,0.15); " +
                            "-fx-text-fill: #10B981; -fx-background-radius: 6; " +
                            "-fx-padding: 2 10; -fx-font-size: 11px; -fx-font-weight: 700;";
                    Label statut = new Label(s.getStatut() != null ? s.getStatut() : "—");
                    statut.setPrefWidth(130);
                    statut.setStyle(statutStyle);

                    row.getChildren().addAll(idCmd, adresse, statut);
                    setGraphic(row);
                }
            }
        });

        // Cellule liste livreurs disponibles
        listeDispo.setCellFactory(lv -> new ListCell<Livreur>() {
            @Override
            protected void updateItem(Livreur l, boolean empty) {
                super.updateItem(l, empty);
                if (empty || l == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    HBox row = new HBox(12);
                    row.setStyle("-fx-padding: 8 15; -fx-alignment: center-left;");

                    Label nom = new Label(l.getNomLivreur());
                    nom.setPrefWidth(140);
                    nom.setStyle("-fx-font-weight: bold; -fx-font-size: 13px;");

                    Label vehicule = new Label("🚗 " + (l.getGenreVehicule() != null
                            ? l.getGenreVehicule() : "—"));
                    vehicule.setPrefWidth(120);
                    vehicule.setStyle("-fx-font-size: 12px;");

                    Label score = new Label("⭐ " + l.getScore());
                    score.setStyle("-fx-font-size: 12px;");

                    row.getChildren().addAll(nom, vehicule, score);
                    setGraphic(row);
                }
            }
        });

        chargerLivreurs();
        chargerLivraisonsEnAttente();
    }

    // ─────────────────────────────────────────────
    // CHARGER
    // ─────────────────────────────────────────────
    private void chargerLivreurs() {
        try {
            tousLesLivreurs = dao.getAll();
            afficher(tousLesLivreurs);
            mettreAJourKpi();
        } catch (SQLException e) {
            messageLabel.setText("Erreur : " + e.getMessage());
        }
    }

    private void afficher(List<Livreur> liste) {
        listView.setItems(FXCollections.observableArrayList(liste));
        if (lblCountLivreurs != null)
            lblCountLivreurs.setText(liste.size() + " livreur(s)");
    }

    private void chargerLivraisonsEnAttente() {
        try {
            List<SuiviLivraison> liste = suiviDAO.getEnAttente();
            listLivraisons.setItems(FXCollections.observableArrayList(liste));
            if (kpiEnAttente != null)
                kpiEnAttente.setText(String.valueOf(liste.size()));
        } catch (SQLException e) {
            messageLabel.setText("Erreur : " + e.getMessage());
        }
    }

    private void mettreAJourKpi() {
        if (tousLesLivreurs == null) return;
        long dispo = tousLesLivreurs.stream().filter(Livreur::isDisponibilite).count();
        long enLivraison = tousLesLivreurs.stream().filter(l -> !l.isDisponibilite()).count();
        if (kpiTotal != null) kpiTotal.setText(String.valueOf(tousLesLivreurs.size()));
        if (kpiDisponibles != null) kpiDisponibles.setText(String.valueOf(dispo));
        if (kpiEnLivraison != null) kpiEnLivraison.setText(String.valueOf(enLivraison));
    }

    // ─────────────────────────────────────────────
    // CLIC SUR UN LIVREUR → affiche panel détail
    // ─────────────────────────────────────────────
    @FXML
    public void onLivreurClicked() {
        Livreur selected = listView.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        livreurSelectionne = selected;

        // Remplir les champs
        detailNom.setText(selected.getNomLivreur());
        detailTel.setText("📞 " + selected.getTelephone());
        detailAge.setText(selected.getDateNaissance() != null
                ? selected.getAge() + " ans" : "—");
        detailVehicule.setText(selected.getGenreVehicule() != null
                ? selected.getGenreVehicule() : "—");
        detailScore.setText(selected.getScore() + " / 100");

        // ── Photo ──
        String photoPath = selected.getPhoto();
        if (photoPath != null && !photoPath.isBlank()) {
            try {
                java.io.File f = new java.io.File(photoPath);
                if (f.exists()) {
                    Image img = new Image(f.toURI().toString(),
                            90, 90, true, true);
                    detailPhoto.setImage(img);
                    detailPhoto.setVisible(true);
                    detailPhotoFallback.setVisible(false);
                } else {
                    detailPhoto.setImage(null);
                    detailPhoto.setVisible(false);
                    detailPhotoFallback.setVisible(true);
                }
            } catch (Exception ex) {
                detailPhoto.setImage(null);
                detailPhoto.setVisible(false);
                detailPhotoFallback.setVisible(true);
            }
        } else {
            detailPhoto.setImage(null);
            detailPhoto.setVisible(false);
            detailPhotoFallback.setVisible(true);
        }

        if (selected.isDisponibilite()) {
            detailDispo.setText("✅ Disponible");
            detailDispo.setStyle("-fx-background-color: rgba(16,185,129,0.15); " +
                    "-fx-text-fill: #10B981; -fx-background-radius: 6; -fx-padding: 3 10;");
        } else {
            detailDispo.setText("❌ Indisponible");
            detailDispo.setStyle("-fx-background-color: rgba(244,114,182,0.15); " +
                    "-fx-text-fill: #F472B6; -fx-background-radius: 6; -fx-padding: 3 10;");
        }

        // Historique
        try {
            List<SuiviLivraison> livraisons = suiviDAO.getByLivreur(selected.getIdLivreur());
            if (livraisons.isEmpty()) {
                detailHistorique.setItems(FXCollections.observableArrayList(
                        "Aucune livraison."));
            } else {
                detailHistorique.setItems(FXCollections.observableArrayList(
                        livraisons.stream()
                                .map(s -> "#" + s.getIdCommande() + "  |  "
                                        + (s.getAdresseClient() != null
                                        ? s.getAdresseClient() : "—")
                                        + "  |  " + (s.getStatut() != null
                                        ? s.getStatut() : "—"))
                                .collect(Collectors.toList())
                ));
            }
        } catch (SQLException e) {
            detailHistorique.setItems(FXCollections.observableArrayList("Erreur."));
        }

        // Afficher le panel
        detailEmpty.setVisible(false);
        detailEmpty.setManaged(false);
        detailContent.setVisible(true);
        detailContent.setManaged(true);
    }

    // ─────────────────────────────────────────────
    // CLIC SUR UNE LIVRAISON EN ATTENTE → affiche panel affectation
    // ─────────────────────────────────────────────
    @FXML
    public void onLivraisonClicked() {
        SuiviLivraison selected = listLivraisons.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        if (!"EN_ATTENTE".equals(selected.getStatut())) {
            messageLabel.setStyle("-fx-text-fill: #F97316;");
            messageLabel.setText("Cette livraison est déjà " + selected.getStatut());
            return;
        }
        livraisonSelectionnee = selected;

        affectCmd.setText("Commande #" + selected.getIdCommande());
        affectAdresse.setText(selected.getAdresseClient() != null
                ? selected.getAdresseClient() : "—");

        // Charger livreurs disponibles
        try {
            List<Livreur> disponibles = dao.getDisponibles();
            listeDispo.setItems(FXCollections.observableArrayList(disponibles));
        } catch (SQLException e) {
            messageLabel.setStyle("-fx-text-fill: #F472B6;");
            messageLabel.setText("Erreur : " + e.getMessage());
            return;
        }

        affectEmpty.setVisible(false);
        affectEmpty.setManaged(false);
        affectContent.setVisible(true);
        affectContent.setManaged(true);
    }

    // ─────────────────────────────────────────────
    // CONFIRMER AFFECTATION
    // ─────────────────────────────────────────────
    @FXML
    public void onConfirmerAffectation() {
        Livreur livreurChoisi = listeDispo.getSelectionModel().getSelectedItem();
        if (livreurChoisi == null) {
            messageLabel.setStyle("-fx-text-fill: #F97316;");
            messageLabel.setText("Sélectionne un livreur dans la liste !");
            return;
        }
        if (livraisonSelectionnee == null) return;

        try {
            String heureEstimee = LocalTime.now().plusMinutes(30).getHour()
                    + "h" + String.format("%02d",
                    LocalTime.now().plusMinutes(30).getMinute());

            suiviDAO.affecterLivreur(livraisonSelectionnee.getIdSuivi(),
                    livreurChoisi.getIdLivreur(), heureEstimee);
            dao.assignerLivraison(livreurChoisi.getIdLivreur());

            messageLabel.setStyle("-fx-text-fill: #10B981;");
            messageLabel.setText("✅ Livraison #" + livraisonSelectionnee.getIdCommande()
                    + " affectée à " + livreurChoisi.getNomLivreur() + " !");

            // Reset panel
            affectEmpty.setVisible(true);
            affectEmpty.setManaged(true);
            affectContent.setVisible(false);
            affectContent.setManaged(false);
            livraisonSelectionnee = null;

            chargerLivreurs();
            chargerLivraisonsEnAttente();

        } catch (SQLException e) {
            messageLabel.setStyle("-fx-text-fill: #F472B6;");
            messageLabel.setText("Erreur : " + e.getMessage());
        }
    }

    // ─────────────────────────────────────────────
    // BOUTONS DU PANEL DÉTAIL
    // ─────────────────────────────────────────────
    @FXML
    public void onAssignerLivraison() {
        if (livreurSelectionne == null) return;
        // Switcher vers l'onglet livraisons
        messageLabel.setStyle("-fx-text-fill: #38BDF8;");
        messageLabel.setText("Allez dans l'onglet 'Livraisons en attente' pour affecter.");
    }

    @FXML
    public void onModifier() {
        if (livreurSelectionne == null) return;
        modifierLivreur(livreurSelectionne);
    }

    @FXML
    public void onSupprimer() {
        if (livreurSelectionne == null) return;

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmation");
        confirm.setContentText("Supprimer " + livreurSelectionne.getNomLivreur() + " ?");
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                try {
                    dao.delete(livreurSelectionne.getIdLivreur());
                    messageLabel.setStyle("-fx-text-fill: #10B981;");
                    messageLabel.setText("Livreur supprimé.");

                    // Reset panel
                    detailEmpty.setVisible(true);
                    detailEmpty.setManaged(true);
                    detailContent.setVisible(false);
                    detailContent.setManaged(false);
                    livreurSelectionne = null;

                    chargerLivreurs();
                } catch (SQLException e) {
                    messageLabel.setStyle("-fx-text-fill: #F472B6;");
                    messageLabel.setText("Erreur : " + e.getMessage());
                }
            }
        });
    }

    // ─────────────────────────────────────────────
    // MODIFIER LIVREUR
    // ─────────────────────────────────────────────
    private void modifierLivreur(Livreur selected) {
        Dialog<Livreur> dialog = new Dialog<>();
        dialog.setTitle("Modifier livreur");

        TextField nomF = new TextField(selected.getNomLivreur());
        TextField telF = new TextField(selected.getTelephone());
        ComboBox<String> dispoC = new ComboBox<>(
                FXCollections.observableArrayList("Disponible", "Non disponible"));
        dispoC.setValue(selected.isDisponibilite() ? "Disponible" : "Non disponible");
        ComboBox<String> vehiculeC = new ComboBox<>(
                FXCollections.observableArrayList("Voiture", "Petit camion"));
        vehiculeC.setValue(selected.getGenreVehicule() != null
                ? selected.getGenreVehicule() : "Voiture");

        String fieldStyle = "-fx-background-color: #2a2a4a; -fx-text-fill: white; " +
                "-fx-border-color: #4f46e5; -fx-border-radius: 8; " +
                "-fx-background-radius: 8; -fx-padding: 8;";
        nomF.setStyle(fieldStyle);
        telF.setStyle(fieldStyle);
        dispoC.setPrefWidth(280);
        vehiculeC.setPrefWidth(280);

        Label titre = new Label("Modifier — " + selected.getNomLivreur());
        titre.setStyle("-fx-text-fill: white; -fx-font-size: 15px; -fx-font-weight: bold;");
        Label errL = new Label("");
        errL.setStyle("-fx-text-fill: #F472B6; -fx-font-size: 12px;");

        VBox box = new VBox(10, titre,
                new Label("Nom :") {{ setStyle("-fx-text-fill: #94A3B8;"); }}, nomF,
                new Label("Téléphone :") {{ setStyle("-fx-text-fill: #94A3B8;"); }}, telF,
                new Label("Disponibilité :") {{ setStyle("-fx-text-fill: #94A3B8;"); }}, dispoC,
                new Label("Véhicule :") {{ setStyle("-fx-text-fill: #94A3B8;"); }}, vehiculeC,
                errL);
        box.setStyle("-fx-background-color: #111425; -fx-padding: 25; -fx-min-width: 320;");

        dialog.getDialogPane().setContent(box);
        dialog.getDialogPane().setStyle("-fx-background-color: #111425;");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        Button okBtn = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        okBtn.setText("Modifier");
        okBtn.setStyle("-fx-background-color: linear-gradient(to right, #2563EB, #8B5CF6); " +
                "-fx-text-fill: white; -fx-background-radius: 8; -fx-padding: 8 20;");

        Button cancelBtn = (Button) dialog.getDialogPane().lookupButton(ButtonType.CANCEL);
        cancelBtn.setText("Annuler");
        cancelBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #94A3B8; " +
                "-fx-border-color: rgba(255,255,255,0.07); -fx-border-radius: 8; -fx-padding: 8 20;");

        Platform.runLater(() -> { nomF.requestFocus(); nomF.selectAll(); });

        dialog.setResultConverter(btn -> {
            if (btn == ButtonType.OK) {
                selected.setNomLivreur(nomF.getText().trim());
                selected.setTelephone(telF.getText().trim());
                selected.setDisponibilite(dispoC.getValue().equals("Disponible"));
                selected.setGenreVehicule(vehiculeC.getValue());
                return selected;
            }
            return null;
        });

        dialog.showAndWait().ifPresent(l -> {
            if (l.getNomLivreur().isEmpty()) {
                messageLabel.setStyle("-fx-text-fill: #F472B6;");
                messageLabel.setText("Le nom est obligatoire !");
                return;
            }
            if (!l.getTelephone().matches("\\d{8}")) {
                messageLabel.setStyle("-fx-text-fill: #F472B6;");
                messageLabel.setText("Téléphone : 8 chiffres requis !");
                return;
            }
            try {
                dao.update(l);
                messageLabel.setStyle("-fx-text-fill: #10B981;");
                messageLabel.setText("Livreur modifié !");
                chargerLivreurs();
                onLivreurClicked(); // rafraîchir le panel
            } catch (SQLException e) {
                messageLabel.setStyle("-fx-text-fill: #F472B6;");
                messageLabel.setText("Erreur : " + e.getMessage());
            }
        });
    }

    // ─────────────────────────────────────────────
    // AJOUTER
    // ─────────────────────────────────────────────
    @FXML
    public void ouvrirAjout() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/livreur/AjouterLivreur.fxml"));
            Parent root = loader.load();
            AjouterLivreurController controller = loader.getController();
            controller.setOnSuccess(this::chargerLivreurs);
            Stage stage = new Stage();
            stage.setTitle("Nouveau livreur");
            stage.setScene(new Scene(root));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.showAndWait();
        } catch (IOException e) {
            messageLabel.setStyle("-fx-text-fill: #F472B6;");
            messageLabel.setText("Erreur : " + e.getMessage());
        }
    }

    // ─────────────────────────────────────────────
    // RECHERCHE
    // ─────────────────────────────────────────────
    @FXML
    public void rechercherLivreur() {
        String terme = searchField.getText().toLowerCase();
        List<Livreur> filtre = tousLesLivreurs.stream()
                .filter(l -> l.getNomLivreur().toLowerCase().contains(terme)
                        || l.getTelephone().contains(terme))
                .collect(Collectors.toList());
        afficher(filtre);
    }

    // ─────────────────────────────────────────────
    // FILTRE
    // ─────────────────────────────────────────────
    private void filtrer() {
        String val = filtreCombo.getValue();
        if ("Tous".equals(val)) {
            afficher(tousLesLivreurs);
        } else if ("Disponible".equals(val)) {
            afficher(tousLesLivreurs.stream()
                    .filter(Livreur::isDisponibilite)
                    .collect(Collectors.toList()));
        } else {
            afficher(tousLesLivreurs.stream()
                    .filter(l -> !l.isDisponibilite())
                    .collect(Collectors.toList()));
        }
    }

    // ─────────────────────────────────────────────
    // NAVIGATION
    // ─────────────────────────────────────────────
    @FXML
    public void switchMode() {
        Scene scene = listView.getScene();
        scene.getStylesheets().clear();
        if (isDarkMode) {
            scene.getStylesheets().add(
                    getClass().getResource("/css/livreur/Light.css").toExternalForm());
            btnModeSwitch.setText("🌙  Mode nuit");
            isDarkMode = false;
        } else {
            scene.getStylesheets().add(
                    getClass().getResource("/css/livreur/Dark.css").toExternalForm());
            btnModeSwitch.setText("☀️  Mode jour");
            isDarkMode = true;
        }
    }
}
