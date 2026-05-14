package projet.hanouti.produit_fournisseur.controllers;

import javafx.animation.TranslateTransition;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.util.Duration;
import projet.hanouti.produit_fournisseur.entities.Fournisseur;
import projet.hanouti.produit_fournisseur.services.FournisseurService;
import projet.hanouti.produit_fournisseur.utils.PdfExportUtil;
import java.awt.Desktop;
import java.util.List;
import java.util.stream.Collectors;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class GererFournisseursController {

    @FXML private TextField        searchField;
    @FXML private ComboBox<String> filterActif;
    @FXML private Label            lblCount;
    @FXML private VBox             listContainer;

    @FXML private VBox      editPanel;
    @FXML private TextField editNom;
    @FXML private TextField editContact;
    @FXML private TextField editEmail;
    @FXML private TextField editTel;
    @FXML private TextArea  editAdresse;
    @FXML private TextArea  editConditions;
    @FXML private CheckBox  editActif;
    @FXML private Label     editErrNom;
    @FXML private Label     editErrContact;
    @FXML private Label     editErrEmail;
    @FXML private Label     editErrTel;
    @FXML private Label     editErrAdresse;

    private static final Pattern EMAIL_REGEX =
            Pattern.compile("^[\\w.-]+@[\\w.-]+\\.[a-zA-Z]{2,}$");
    private static final Pattern PHONE_REGEX =
            Pattern.compile("^[0-9+\\-\\s]{8,20}$");

    private final FournisseurService fs = new FournisseurService();
    private List<Fournisseur> allFournisseurs = new ArrayList<>();
    private List<Fournisseur> filtered        = new ArrayList<>();
    private Fournisseur editingFournisseur    = null;

    @FXML
    public void initialize() {
        filterActif.getItems().addAll("Tous","Actifs","Inactifs");
        filterActif.setValue("Tous");
        loadData();
    }

    private void loadData() {
        allFournisseurs = fs.getData();
        applyFilters();
    }

    //  Render cards 
    private void renderList() {
        listContainer.getChildren().clear();

        if (filtered.isEmpty()) {
            Label empty = new Label("Aucun fournisseur trouve.");
            empty.setStyle("-fx-text-fill:#aaa;-fx-font-size:14px;");
            listContainer.getChildren().add(empty);
            return;
        }

        lblCount.setText(filtered.size() + " fournisseur(s)");

        for (Fournisseur f : filtered) {
            listContainer.getChildren().add(buildCard(f));
        }
    }

    private HBox buildCard(Fournisseur f) {
        HBox card = new HBox(16);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(16, 20, 16, 20));
        card.setStyle(
                "-fx-background-color:white;" +
                        "-fx-background-radius:12;" +
                        "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.07),8,0,0,2);");

        // Avatar circle with first letter
        Label avatar = new Label(f.getNomSociete().substring(0, 1).toUpperCase());
        avatar.setStyle(
                "-fx-font-size:18px;-fx-font-weight:bold;-fx-text-fill:white;" +
                        "-fx-min-width:48px;-fx-min-height:48px;" +
                        "-fx-max-width:48px;-fx-max-height:48px;" +
                        "-fx-alignment:center;" +
                        "-fx-background-color:#192BCC;-fx-background-radius:24;");

        // Main info
        VBox info = new VBox(4);
        HBox.setHgrow(info, Priority.ALWAYS);

        Label nom = new Label(f.getNomSociete());
        nom.setStyle("-fx-font-size:14px;-fx-font-weight:bold;-fx-text-fill:#050A38;");

        Label contact = new Label(" " + f.getContactNom()
                + "    " + f.getTelephone()
                + "    " + f.getEmail());
        contact.setStyle("-fx-font-size:12px;-fx-text-fill:#555;");

        // Adresse  truncated
        String adresse = f.getAdresse() != null && !f.getAdresse().isEmpty()
                ? (f.getAdresse().length() > 60
                ? f.getAdresse().substring(0, 60) + "..." : f.getAdresse())
                : "Adresse non renseignee";
        Label adresseLbl = new Label(" " + adresse);
        adresseLbl.setStyle("-fx-font-size:11px;-fx-text-fill:#888;");
        adresseLbl.setWrapText(true);

        info.getChildren().addAll(nom, contact, adresseLbl);

        // Status badge
        Label statutBadge = new Label(f.isActif() ? "Actif" : "Inactif");
        statutBadge.setStyle(f.isActif()
                ? "-fx-background-color:#CCFFCC;-fx-text-fill:#228822;" +
                "-fx-font-size:11px;-fx-padding:3 10;-fx-background-radius:20;-fx-font-weight:bold;"
                : "-fx-background-color:#FFCCCC;-fx-text-fill:#CC2222;" +
                "-fx-font-size:11px;-fx-padding:3 10;-fx-background-radius:20;-fx-font-weight:bold;");

        // Action buttons
        VBox actions = new VBox(8);
        actions.setAlignment(Pos.CENTER);

        Button editBtn = new Button("Modifier");
        editBtn.getStyleClass().add("icon-btn");
        editBtn.setOnAction(e -> openEdit(f));

        Button delBtn = new Button("Supprimer");
        delBtn.getStyleClass().add("icon-btn-danger");
        delBtn.setOnAction(e -> deleteF(f));

        actions.getChildren().addAll(editBtn, delBtn);

        card.getChildren().addAll(avatar, info, statutBadge, actions);
        return card;
    }

    //  Filters 
    @FXML
    public void applyFilters() {
        String search = searchField.getText() == null ? ""
                : searchField.getText().trim().toLowerCase();
        String statut = filterActif.getValue();

        filtered = allFournisseurs.stream()
                .filter(f -> search.isEmpty()
                        || f.getNomSociete().toLowerCase().contains(search))
                .filter(f -> !"Actifs".equals(statut)   || f.isActif())
                .filter(f -> !"Inactifs".equals(statut) || !f.isActif())
                .collect(Collectors.toList());

        lblCount.setText(filtered.size() + " fournisseur(s)");
        renderList();
    }





    @FXML
    public void exportPdf() {
        try {
            var v = projet.hanouti.produit_fournisseur.utils.SessionManager.getCurrentVendeur();
            String vendeurName = v != null ? v.getPrenom() + " " + v.getNom() : "Vendeur";
            java.io.File pdfFile = PdfExportUtil.exportFournisseurs(filtered, vendeurName);
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(pdfFile);
            } else {
                Alert info = new Alert(Alert.AlertType.INFORMATION);
                info.setTitle("Export reussi"); info.setHeaderText(null);
                info.setContentText("PDF exporte:\n" + pdfFile.getAbsolutePath());
                info.showAndWait();
            }
        } catch (Exception e) {
            Alert err = new Alert(Alert.AlertType.ERROR);
            err.setTitle("Erreur export"); err.setHeaderText(null);
            err.setContentText("Erreur: " + e.getMessage());
            err.showAndWait();
        }
    }

    @FXML
    public void resetFilters() {
        searchField.clear();
        filterActif.setValue("Tous");
        applyFilters();
    }

    //  Edit panel 
    private void openEdit(Fournisseur f) {
        editingFournisseur = f;
        editNom.setText(f.getNomSociete());
        editContact.setText(f.getContactNom());
        editEmail.setText(f.getEmail());
        editTel.setText(f.getTelephone());
        editAdresse.setText(f.getAdresse());
        editConditions.setText(f.getConditionsLivraison() != null
                ? f.getConditionsLivraison() : "");
        editActif.setSelected(f.isActif());
        hideAllErrors();
        slideIn();
    }

    @FXML public void closeEdit() { slideOut(); }

    @FXML
    public void submitEdit() {
        if (!validateEdit()) return;
        editingFournisseur.setNomSociete         (editNom.getText().trim());
        editingFournisseur.setContactNom         (editContact.getText().trim());
        editingFournisseur.setEmail              (editEmail.getText().trim());
        editingFournisseur.setTelephone          (editTel.getText().trim());
        editingFournisseur.setAdresse            (editAdresse.getText().trim());
        editingFournisseur.setConditionsLivraison(editConditions.getText().trim());
        editingFournisseur.setActif              (editActif.isSelected());
        fs.updateEntity(editingFournisseur.getIdFournisseur(), editingFournisseur);
        slideOut(); loadData();
    }

    private void deleteF(Fournisseur f) {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION);
        a.setTitle("Confirmer");
        a.setHeaderText("Supprimer \"" + f.getNomSociete() + "\" ?");
        a.setContentText("Cette action est irreversible.");
        Optional<ButtonType> r = a.showAndWait();
        if (r.isPresent() && r.get() == ButtonType.OK) {
            fs.deleteEntity(f); loadData();
        }
    }

    private boolean validateEdit() {
        boolean ok = true;
        if (editNom.getText().trim().isEmpty()) {
            show(editErrNom, " Obligatoire."); ok = false;
        } else if (editNom.getText().trim().length() < 2) {
            show(editErrNom, " Minimum 2 caracteres."); ok = false;
        } else hide(editErrNom);

        if (editContact.getText().trim().isEmpty()) {
            show(editErrContact, " Obligatoire."); ok = false;
        } else hide(editErrContact);

        String email = editEmail.getText().trim();
        if (email.isEmpty()) { show(editErrEmail, " Obligatoire."); ok = false; }
        else if (!EMAIL_REGEX.matcher(email).matches()) {
            show(editErrEmail, " Format invalide."); ok = false;
        } else hide(editErrEmail);

        String tel = editTel.getText().trim();
        if (tel.isEmpty()) { show(editErrTel, " Obligatoire."); ok = false; }
        else if (!PHONE_REGEX.matcher(tel).matches()) {
            show(editErrTel, " Format invalide."); ok = false;
        } else hide(editErrTel);

        if (editAdresse.getText().trim().isEmpty()) {
            show(editErrAdresse, " Obligatoire."); ok = false;
        } else hide(editErrAdresse);

        return ok;
    }

    private void show(Label l, String m) { l.setText(m); l.setVisible(true); l.setManaged(true); }
    private void hide(Label l) { l.setVisible(false); l.setManaged(false); }
    private void hideAllErrors() {
        hide(editErrNom); hide(editErrContact);
        hide(editErrEmail); hide(editErrTel); hide(editErrAdresse);
    }

    private void slideIn() {
        editPanel.setVisible(true); editPanel.setManaged(true);
        editPanel.setPrefWidth(340);
        TranslateTransition tt = new TranslateTransition(Duration.millis(250), editPanel);
        tt.setFromX(340); tt.setToX(0); tt.play();
    }

    private void slideOut() {
        TranslateTransition tt = new TranslateTransition(Duration.millis(200), editPanel);
        tt.setFromX(0); tt.setToX(340);
        tt.setOnFinished(e -> {
            editPanel.setVisible(false); editPanel.setManaged(false);
            editPanel.setPrefWidth(0);
        });
        tt.play();
    }

    //  Navigation vers Ajouter 
    private ModuleNavigator moduleNavigator;
    public void setModuleNavigator(ModuleNavigator navigator) { this.moduleNavigator = navigator; }

    @FXML
    public void ouvrirAjouter() {
        if (moduleNavigator != null)
            moduleNavigator.navigateToAjouterFournisseur();
    }

}
