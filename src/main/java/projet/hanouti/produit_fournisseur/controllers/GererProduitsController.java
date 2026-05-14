package projet.hanouti.produit_fournisseur.controllers;

import javafx.animation.TranslateTransition;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.util.Duration;
import projet.hanouti.produit_fournisseur.entities.Fournisseur;
import projet.hanouti.produit_fournisseur.entities.Produit;
import projet.hanouti.produit_fournisseur.services.FournisseurService;
import projet.hanouti.produit_fournisseur.services.ProduitService;
import projet.hanouti.produit_fournisseur.utils.EmailUtil;
import projet.hanouti.produit_fournisseur.utils.PdfExportUtil;
import projet.hanouti.produit_fournisseur.utils.SessionManager;

import java.awt.Desktop;
import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

public class GererProduitsController {

    @FXML private TextField        searchField;
    @FXML private ComboBox<String> filterCategorie;
    @FXML private ComboBox<String> filterStatut;
    @FXML private TextField        filterPrixMin;
    @FXML private TextField        filterPrixMax;
    @FXML private Label            lblCount;
    @FXML private VBox             listContainer;

    @FXML private VBox      editPanel;
    @FXML private TextField editNom;
    @FXML private TextField editPrix;
    @FXML private TextField editStock;
    @FXML private TextField editSeuil;
    @FXML private ComboBox<String> editCat;
    @FXML private ComboBox<String> editStatut;
    @FXML private TextArea  editDescription;
    @FXML private Label     editErrNom;
    @FXML private Label     editErrPrix;
    @FXML private Label     editErrStock;
    @FXML private Label     editErrCat;

    private final ProduitService     ps = new ProduitService();
    private final FournisseurService fs = new FournisseurService();
    private static final Set<Integer> alertedProduits = new HashSet<>();

    private List<Produit> allProduits = new ArrayList<>();
    private List<Produit> filtered    = new ArrayList<>();
    private Produit editingProduit    = null;

    @FXML
    public void initialize() {
        filterCategorie.getItems().addAll(
                "Toutes","ALIMENTAIRE","ELECTRONIQUE","MEDICAMENT",
                "HYGIENE","DECOR","MAKEUP");
        filterCategorie.setValue("Toutes");
        filterStatut.getItems().addAll("Tous","ACTIF","SUSPENDU","SUPPRIME");
        filterStatut.setValue("Tous");
        editCat.getItems().addAll(
                "ALIMENTAIRE","ELECTRONIQUE","MEDICAMENT",
                "HYGIENE","DECOR","MAKEUP");
        editStatut.getItems().addAll("ACTIF","SUSPENDU","SUPPRIME");
        loadData();
    }


    private void loadData() {
        allProduits = ps.getData();
        applyFilters();
    }
    public void onPageShown() {
        javafx.application.Platform.runLater(() -> checkLowStock());
    }


    private void renderList() {
        listContainer.getChildren().clear();
        if (filtered.isEmpty()) {
            Label empty = new Label("Aucun produit trouve.");
            empty.setStyle("-fx-text-fill:#aaa;-fx-font-size:14px;");
            listContainer.getChildren().add(empty);
            return;
        }
        lblCount.setText(filtered.size() + " produit(s)");
        for (Produit p : filtered)
            listContainer.getChildren().add(buildCard(p));
    }

    private HBox buildCard(Produit p) {
        HBox card = new HBox(16);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(16, 20, 16, 20));
        card.setStyle(
                "-fx-background-color:white;" +
                        "-fx-background-radius:12;" +
                        "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.07),8,0,0,2);");

        Node productImage = buildProductThumb(p);

        // Main info  grows to fill space
        VBox info = new VBox(4);
        HBox.setHgrow(info, Priority.ALWAYS);

        Label nom = new Label(p.getNom());
        nom.setStyle("-fx-font-size:14px;-fx-font-weight:bold;-fx-text-fill:#050A38;");

        HBox badges = new HBox(8);
        badges.setAlignment(Pos.CENTER_LEFT);

        Label catBadge = new Label(p.getCategorie());
        catBadge.setStyle(
                "-fx-background-color:#EEF0FF;-fx-text-fill:#192BCC;" +
                        "-fx-font-size:11px;-fx-padding:2 8;-fx-background-radius:20;");

        String fourn = "Sans fournisseur";
        if (p.getIdFournisseur() != null) {
            fourn = fs.getData().stream()
                    .filter(f -> f.getIdFournisseur() == p.getIdFournisseur())
                    .map(Fournisseur::getNomSociete)
                    .findFirst()
                    .orElse("Fournisseur #" + p.getIdFournisseur());
        }
        Label fournBadge = new Label(" " + fourn);
        fournBadge.setStyle(
                "-fx-background-color:#F0F2FF;-fx-text-fill:#555;" +
                        "-fx-font-size:11px;-fx-padding:2 8;-fx-background-radius:20;");

        badges.getChildren().addAll(catBadge, fournBadge);

        Label desc = new Label(
                p.getDescription() != null && !p.getDescription().isEmpty()
                        ? p.getDescription() : "Aucune description");
        desc.setStyle("-fx-font-size:11px;-fx-text-fill:#888;");
        desc.setWrapText(true);

        info.getChildren().addAll(nom, badges, desc);

        // Stats  fixed width
        VBox stats = new VBox(6);
        stats.setAlignment(Pos.CENTER_RIGHT);
        stats.setMinWidth(130);
        stats.setPrefWidth(130);
        stats.setMaxWidth(130);

        Label prix = new Label(String.format("%.2f TND", p.getPrix()));
        prix.setStyle("-fx-font-size:16px;-fx-font-weight:bold;-fx-text-fill:#192BCC;");

        String stockColor = p.getQuantiteStock() <= p.getSeuilAlerte()
                ? "#CC2222"
                : p.getQuantiteStock() <= p.getSeuilAlerte() * 2
                ? "#e05555" : "#228822";
        Label stock = new Label("Stock: " + p.getQuantiteStock());
        stock.setStyle("-fx-font-size:12px;-fx-font-weight:bold;-fx-text-fill:" + stockColor + ";");

        String statutBg = switch (p.getStatut()) {
            case "ACTIF"    -> "-fx-background-color:#CCFFCC;-fx-text-fill:#228822;";
            case "SUSPENDU" -> "-fx-background-color:#ffe0e0;-fx-text-fill:#e05555;";
            default         -> "-fx-background-color:#FFCCCC;-fx-text-fill:#CC2222;";
        };
        Label statut = new Label(p.getStatut());
        statut.setStyle(statutBg +
                "-fx-font-size:11px;-fx-padding:2 8;-fx-background-radius:10;-fx-font-weight:bold;");

        stats.getChildren().addAll(prix, stock, statut);

        // Action buttons  fixed width
        VBox actions = new VBox(8);
        actions.setAlignment(Pos.CENTER);
        actions.setMinWidth(110);
        actions.setPrefWidth(110);
        actions.setMaxWidth(110);

        Button editBtn = new Button("Modifier");
        editBtn.getStyleClass().add("icon-btn");
        editBtn.setPrefWidth(95);
        editBtn.setOnAction(e -> openEdit(p));

        Button delBtn = new Button("Supprimer");
        delBtn.getStyleClass().add("icon-btn-danger");
        delBtn.setPrefWidth(95);
        delBtn.setOnAction(e -> deleteProduit(p));

        actions.getChildren().addAll(editBtn, delBtn);

        card.getChildren().addAll(productImage, info, stats, actions);
        return card;
    }

    @FXML
    public void applyFilters() {
        String search  = searchField.getText() == null ? ""
                : searchField.getText().trim().toLowerCase();
        String cat     = filterCategorie.getValue();
        String statut  = filterStatut.getValue();
        double prixMin = parseDouble(filterPrixMin.getText(), 0);
        double prixMax = parseDouble(filterPrixMax.getText(), Double.MAX_VALUE);

        filtered = allProduits.stream()
                .filter(p -> search.isEmpty()
                        || p.getNom().toLowerCase().contains(search))
                .filter(p -> cat == null || "Toutes".equals(cat)
                        || cat.equals(p.getCategorie()))
                .filter(p -> statut == null || "Tous".equals(statut)
                        || statut.equals(p.getStatut()))
                .filter(p -> p.getPrix() >= prixMin && p.getPrix() <= prixMax)
                .collect(Collectors.toList());

        lblCount.setText(filtered.size() + " produit(s)");
        renderList();
    }

    @FXML
    public void resetFilters() {
        searchField.clear();
        filterCategorie.setValue("Toutes");
        filterStatut.setValue("Tous");
        filterPrixMin.clear();
        filterPrixMax.clear();
        applyFilters();
    }

    @FXML
    public void exportPdf() {
        try {
            var v = SessionManager.getCurrentVendeur();
            String vendeurName = v != null ? v.getPrenom() + " " + v.getNom() : "Vendeur";
            java.io.File pdfFile = PdfExportUtil.export(filtered, vendeurName);
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

    private void checkLowStock() {
        List<Produit> lowStockList = allProduits.stream()
                .filter(p -> p.getQuantiteStock() <= p.getSeuilAlerte())
                .filter(p -> !alertedProduits.contains(p.getIdProduit()))
                .collect(Collectors.toList());

        if (lowStockList.isEmpty()) return;

        lowStockList.forEach(p -> alertedProduits.add(p.getIdProduit()));

        var vendeur = SessionManager.getCurrentVendeur();

        Map<Integer, List<Produit>> byFournisseur = new LinkedHashMap<>();
        for (Produit p : lowStockList) {
            int key = p.getIdFournisseur() != null ? p.getIdFournisseur() : -1;
            byFournisseur.computeIfAbsent(key, k -> new ArrayList<>()).add(p);
        }

        for (Map.Entry<Integer, List<Produit>> entry : byFournisseur.entrySet()) {
            int idF = entry.getKey();
            List<Produit> produitsDuFournisseur = entry.getValue();

            if (idF == -1) {
                Alert warn = new Alert(Alert.AlertType.WARNING);
                warn.setTitle(" Stock Critique");
                warn.setHeaderText(produitsDuFournisseur.size()
                        + " produit(s) en stock critique sans fournisseur assigne");
                StringBuilder sb = new StringBuilder();
                for (Produit p : produitsDuFournisseur)
                    sb.append("   ").append(p.getNom())
                            .append("  ").append(p.getQuantiteStock()).append(" unite(s)\n");
                warn.setContentText(sb.toString()
                        + "\nAssignez un fournisseur a ces produits pour envoyer un email.");
                warn.showAndWait();
                continue;
            }

            var fournisseurOpt = fs.getData().stream()
                    .filter(f -> f.getIdFournisseur() == idF).findFirst();
            if (fournisseurOpt.isEmpty()) continue;
            var fournisseur = fournisseurOpt.get();

            Dialog<Map<Produit, Integer>> dialog = new Dialog<>();
            dialog.setTitle(" Stock Critique");
            dialog.setHeaderText(
                    produitsDuFournisseur.size() + " produit(s) en stock critique\n"
                            + "Remplissez les quantites souhaitees (au moins une).\n"
                            + " Email sera envoye a : " + fournisseur.getEmail()
                            + " (" + fournisseur.getNomSociete() + ")");

            ButtonType sendBtn   = new ButtonType("Envoyer l'email", ButtonBar.ButtonData.OK_DONE);
            ButtonType cancelBtn = new ButtonType("Annuler", ButtonBar.ButtonData.CANCEL_CLOSE);
            dialog.getDialogPane().getButtonTypes().addAll(sendBtn, cancelBtn);

            javafx.scene.layout.GridPane grid = new javafx.scene.layout.GridPane();
            grid.setHgap(12); grid.setVgap(10);
            grid.setPadding(new Insets(20));

            Label h1 = new Label("Produit");       h1.setStyle("-fx-font-weight:bold;");
            Label h2 = new Label("Stock actuel");  h2.setStyle("-fx-font-weight:bold;");
            Label h3 = new Label("Quantite a commander (optionnel)");
            h3.setStyle("-fx-font-weight:bold;");
            grid.add(h1, 0, 0); grid.add(h2, 1, 0); grid.add(h3, 2, 0);

            Map<Produit, TextField> quantityFields = new LinkedHashMap<>();
            int row = 1;
            for (Produit p : produitsDuFournisseur) {
                Label nomLbl = new Label(p.getNom());
                nomLbl.setStyle("-fx-font-weight:bold;");
                Label stockLbl = new Label(p.getQuantiteStock() + " unite(s)");
                stockLbl.setStyle("-fx-text-fill:#CC2222;-fx-font-weight:bold;");
                TextField qtyField = new TextField();
                qtyField.setPromptText("Laisser vide pour ignorer");
                qtyField.setPrefWidth(160);
                quantityFields.put(p, qtyField);
                grid.add(nomLbl, 0, row);
                grid.add(stockLbl, 1, row);
                grid.add(qtyField, 2, row);
                row++;
            }

            dialog.getDialogPane().setContent(grid);

            Node sendButton = dialog.getDialogPane().lookupButton(sendBtn);
            sendButton.setDisable(true);
            Runnable validate = () -> {
                boolean anyValid = quantityFields.values().stream().anyMatch(tf -> {
                    try { return Integer.parseInt(tf.getText().trim()) > 0; }
                    catch (Exception ex) { return false; }
                });
                sendButton.setDisable(!anyValid);
            };
            quantityFields.values().forEach(tf ->
                    tf.textProperty().addListener((o, a, b) -> validate.run()));

            final var fournisseurFinal = fournisseur;
            dialog.setResultConverter(btn -> {
                if (btn == sendBtn) {
                    Map<Produit, Integer> result = new LinkedHashMap<>();
                    quantityFields.forEach((p, tf) -> {
                        try {
                            int qty = Integer.parseInt(tf.getText().trim());
                            if (qty > 0) result.put(p, qty);
                        } catch (Exception ignored) {}
                    });
                    return result;
                }
                return null;
            });

            dialog.showAndWait().ifPresent(quantities -> {
                if (!quantities.isEmpty())
                    EmailUtil.sendRestockAlert(fournisseurFinal, quantities, vendeur);
            });
        }
    }

    private void openEdit(Produit p) {
        editingProduit = p;
        editNom.setText(p.getNom());
        editPrix.setText(String.valueOf(p.getPrix()));
        editStock.setText(String.valueOf(p.getQuantiteStock()));
        editSeuil.setText(String.valueOf(p.getSeuilAlerte()));
        editCat.setValue(p.getCategorie());
        editStatut.setValue(p.getStatut());
        editDescription.setText(p.getDescription() != null ? p.getDescription() : "");
        hideAllErrors();
        slideIn();
    }

    @FXML public void closeEdit() { slideOut(); }

    @FXML
    public void submitEdit() {
        if (!validateEdit()) return;

        String nom       = editNom.getText().trim();
        String categorie = editCat.getValue();
        double prix      = Double.parseDouble(editPrix.getText().trim());
        int    stock     = Integer.parseInt(editStock.getText().trim());

        Produit existing = ps.findExistingExcluding(
                nom, categorie,
                editingProduit.getIdFournisseur(),
                SessionManager.getCurrentVendeurId(),
                editingProduit.getIdProduit(), prix);

        if (existing != null) {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Produit deja existant");
            confirm.setHeaderText("\"" + nom + "\" existe deja avec le meme prix !");
            confirm.setContentText(
                    "Stock actuel: " + existing.getQuantiteStock() + " unites.\n\n" +
                            "Voulez-vous fusionner et ajouter " + stock +
                            " unites au stock existant ?\nNouveau stock: "
                            + (existing.getQuantiteStock() + stock));
            ButtonType btnFusionner = new ButtonType("Fusionner le stock");
            ButtonType btnAnnuler   = new ButtonType("Annuler",
                    ButtonBar.ButtonData.CANCEL_CLOSE);
            confirm.getButtonTypes().setAll(btnFusionner, btnAnnuler);
            confirm.showAndWait().ifPresent(response -> {
                if (response == btnFusionner) {
                    ps.increaseStock(existing.getIdProduit(), stock);
                    ps.deleteEntity(editingProduit);
                    slideOut(); loadData();
                    Alert ok = new Alert(Alert.AlertType.INFORMATION);
                    ok.setTitle("Succes"); ok.setHeaderText(null);
                    ok.setContentText("Stock fusionne !\nNouveau stock: "
                            + (existing.getQuantiteStock() + stock) + " unites.");
                    ok.showAndWait();
                }
            });
            return;
        }

        editingProduit.setNom          (nom);
        editingProduit.setPrix         (prix);
        editingProduit.setQuantiteStock(stock);
        editingProduit.setSeuilAlerte  (editSeuil.getText().trim().isEmpty() ? 5
                : Integer.parseInt(editSeuil.getText().trim()));
        editingProduit.setCategorie    (categorie);
        editingProduit.setStatut       (editStatut.getValue());
        editingProduit.setDescription  (editDescription.getText().trim());
        ps.updateEntity(editingProduit.getIdProduit(), editingProduit);
        slideOut(); loadData();
    }

    private void deleteProduit(Produit p) {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION);
        a.setTitle("Confirmer");
        a.setHeaderText("Supprimer \"" + p.getNom() + "\" ?");
        a.setContentText("Cette action est irreversible.");
        Optional<ButtonType> r = a.showAndWait();
        if (r.isPresent() && r.get() == ButtonType.OK) {
            ps.deleteEntity(p); loadData();
        }
    }

    private boolean validateEdit() {
        boolean ok = true;
        String nom = editNom.getText().trim();
        if (nom.isEmpty()) { show(editErrNom, " Nom obligatoire."); ok = false; }
        else if (nom.length() < 2) { show(editErrNom, " Minimum 2 caracteres."); ok = false; }
        else hide(editErrNom);

        try {
            double v = Double.parseDouble(editPrix.getText().trim());
            if (v <= 0) { show(editErrPrix, " Prix doit etre > 0."); ok = false; }
            else hide(editErrPrix);
        } catch (NumberFormatException e) {
            show(editErrPrix, " Prix invalide (ex: 12.50)."); ok = false;
        }

        try {
            int v = Integer.parseInt(editStock.getText().trim());
            if (v < 0) { show(editErrStock, " Stock >= 0."); ok = false; }
            else hide(editErrStock);
        } catch (NumberFormatException e) {
            show(editErrStock, " Entier uniquement."); ok = false;
        }

        if (editCat.getValue() == null) {
            show(editErrCat, " Categorie obligatoire."); ok = false;
        } else hide(editErrCat);

        return ok;
    }

    private String getCatEmoji(String cat) {
        return switch (cat) {
            case "ALIMENTAIRE"  -> "\u2615";
            case "ELECTRONIQUE" -> "\u26A1";
            case "MEDICAMENT"   -> "+";
            case "HYGIENE"      -> "\u2726";
            case "DECOR"        -> "\u25C6";
            case "MAKEUP"       -> "\u2665";
            default             -> "\u25CF";
        };
    }

    private Node buildProductThumb(Produit p) {
        StackPane holder = new StackPane();
        holder.setMinSize(64, 64);
        holder.setPrefSize(64, 64);
        holder.setMaxSize(64, 64);
        holder.setStyle(
                "-fx-background-color:" + getCatColor(p.getCategorie()) + ";" +
                        "-fx-background-radius:12;" +
                        "-fx-border-color:rgba(99,102,241,0.14);" +
                        "-fx-border-radius:12;-fx-border-width:1;");

        String imagePath = p.getImage();
        if (imagePath != null && !imagePath.trim().isEmpty()) {
            try {
                Image img = imagePath.startsWith("http") || imagePath.startsWith("file:")
                        ? new Image(imagePath, 64, 64, true, true)
                        : new Image(new File(imagePath).toURI().toString(), 64, 64, true, true);
                if (!img.isError()) {
                    ImageView iv = new ImageView(img);
                    iv.setFitWidth(58);
                    iv.setFitHeight(58);
                    iv.setPreserveRatio(true);
                    iv.setSmooth(true);
                    holder.getChildren().add(iv);
                    return holder;
                }
            } catch (Exception ignored) {
            }
        }

        Label fallback = new Label(getCatEmoji(p.getCategorie()));
        fallback.setStyle("-fx-font-size:24px;-fx-text-fill:#192BCC;-fx-font-weight:bold;");
        holder.getChildren().add(fallback);
        return holder;
    }

    private String getCatColor(String cat) {
        return switch (cat) {
            case "ALIMENTAIRE"  -> "#EEFFEE";
            case "ELECTRONIQUE" -> "#FFF8EE";
            case "MEDICAMENT"   -> "#EEF0FF";
            case "HYGIENE"      -> "#E8F8FF";
            case "DECOR"        -> "#F0FFE8";
            case "MAKEUP"       -> "#FFF0F8";
            default             -> "#F5F5F5";
        };
    }

    private double parseDouble(String s, double def) {
        try { return Double.parseDouble(s.trim()); }
        catch (Exception e) { return def; }
    }

    private void show(Label l, String m) {
        l.setText(m); l.setVisible(true); l.setManaged(true);
    }
    private void hide(Label l) { l.setVisible(false); l.setManaged(false); }
    private void hideAllErrors() {
        hide(editErrNom); hide(editErrPrix); hide(editErrStock); hide(editErrCat);
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
            moduleNavigator.navigateToAjouterProduit();
    }

}
