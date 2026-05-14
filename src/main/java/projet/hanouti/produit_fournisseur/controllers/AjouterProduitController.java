package projet.hanouti.produit_fournisseur.controllers;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.json.JSONArray;
import org.json.JSONObject;
import projet.hanouti.produit_fournisseur.entities.Fournisseur;
import projet.hanouti.produit_fournisseur.entities.Produit;
import projet.hanouti.produit_fournisseur.services.FournisseurService;
import projet.hanouti.produit_fournisseur.services.ProduitService;
import projet.hanouti.produit_fournisseur.utils.SessionManager;

import java.io.File;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.time.Duration;
import java.util.Base64;
import java.util.Properties;

public class AjouterProduitController {

    private ModuleNavigator moduleNavigator;
    public void setModuleNavigator(ModuleNavigator navigator) { this.moduleNavigator = navigator; }

    @FXML private TextField  fieldNom;
    @FXML private TextArea   fieldDescription;
    @FXML private TextField  fieldPrix;
    @FXML private TextField  fieldStock;
    @FXML private TextField  fieldSeuil;
    @FXML private TextField  fieldImage;
    @FXML private ComboBox<String>      comboCat;
    @FXML private ComboBox<String>      comboStatut;
    @FXML private ComboBox<Fournisseur> comboFournisseur;

    @FXML private Label errNom;
    @FXML private Label errPrix;
    @FXML private Label errStock;
    @FXML private Label errCat;

    @FXML private Label previewNom;
    @FXML private Label previewCat;
    @FXML private Label previewPrix;
    @FXML private Label previewStock;

    @FXML private Button    btnScanAI;
    @FXML private ImageView imagePreview;
    @FXML private Label     lblAiStatus;

    private final ProduitService     ps = new ProduitService();
    private final FournisseurService fs = new FournisseurService();

    private File pickedImageFile = null;

    private static final String DEFAULT_GEMINI_MODEL = "gemini-flash-latest";

    @FXML
    public void initialize() {
        comboCat.getItems().addAll(
                "ALIMENTAIRE","ELECTRONIQUE","MEDICAMENT","HYGIENE","DECOR","MAKEUP","AUTRE");
        comboStatut.getItems().addAll("ACTIF","SUSPENDU");
        comboStatut.setValue("ACTIF");
        comboFournisseur.getItems().addAll(fs.getData());

        fieldNom.textProperty().addListener((o, a, b) -> {
            if (b.trim().isEmpty())
                show(errNom, " Nom obligatoire.");
            else if (b.trim().length() < 2)
                show(errNom, " Minimum 2 caracteres.");
            else
                hide(errNom);
            previewNom.setText(b.isEmpty() ? "Nom du produit" : b);
        });

        fieldPrix.textProperty().addListener((o, a, b) -> {
            if (!b.isEmpty()) {
                try {
                    double v = Double.parseDouble(b);
                    if (v <= 0) show(errPrix, " Prix doit etre > 0.");
                    else hide(errPrix);
                } catch (NumberFormatException e) {
                    show(errPrix, " Chiffres uniquement (ex: 12.50).");
                }
            } else hide(errPrix);
            previewPrix.setText(b.isEmpty() ? "0.00 TND" : b + " TND");
        });

        fieldStock.textProperty().addListener((o, a, b) -> {
            if (!b.isEmpty()) {
                try {
                    int v = Integer.parseInt(b);
                    if (v < 0) show(errStock, " Stock >= 0.");
                    else hide(errStock);
                } catch (NumberFormatException e) {
                    show(errStock, " Entier uniquement (ex: 10).");
                }
            } else hide(errStock);
            previewStock.setText("Stock: " + (b.isEmpty() ? "0" : b));
        });

        comboCat.valueProperty().addListener((o, a, b) -> {
            if (b == null) show(errCat, " Categorie obligatoire.");
            else hide(errCat);
            previewCat.setText(b == null ? "Categorie" : b);
        });
    }

    @FXML
    public void browseImage() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Choisir une image");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter(
                "Images", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.bmp"));
        Stage stage = (Stage) fieldImage.getScene().getWindow();
        File file = fc.showOpenDialog(stage);
        if (file != null) {
            pickedImageFile = file;
            fieldImage.setText(file.getAbsolutePath());
            if (imagePreview != null)
                imagePreview.setImage(
                        new Image(file.toURI().toString(), 200, 200, true, true));
            if (lblAiStatus != null)
                lblAiStatus.setText("Image chargee. Cliquez sur Analyser avec IA.");
        }
    }

    @FXML
    public void scanWithAI() {
        String apiKey = loadGeminiApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            showAlert("Cle Gemini manquante", "Definissez GEMINI_API_KEY ou gemini.api.key dans config.properties pour activer l'analyse IA.");
            return;
        }
        if (pickedImageFile == null) {
            showAlert("Aucune image", "Veuillez d'abord choisir une image.");
            return;
        }

        btnScanAI.setDisable(true);
        if (lblAiStatus != null)
            lblAiStatus.setText(" Analyse en cours...");

        new Thread(() -> {
            try {
                byte[] imageBytes = Files.readAllBytes(pickedImageFile.toPath());
                String base64Image = Base64.getEncoder().encodeToString(imageBytes);
                String mimeType = pickedImageFile.getName().toLowerCase().endsWith(".png")
                        ? "image/png" : "image/jpeg";
                String requestBody = buildGeminiRequestBody(base64Image, mimeType);

                HttpClient client = HttpClient.newHttpClient();
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(loadGeminiApiUrl()))
                        .timeout(Duration.ofSeconds(30))
                        .header("Content-Type", "application/json")
                        .header("X-goog-api-key", apiKey)
                        .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                        .build();

                HttpResponse<String> response = client.send(
                        request, HttpResponse.BodyHandlers.ofString());

                String body = response.body();
                if (response.statusCode() != 200) {
                    throw new Exception("Gemini a retourne le statut " + response.statusCode()
                            + ": " + body.substring(0, Math.min(200, body.length())));
                }

                String text = extractGeminiText(body);
                if (text == null || text.isBlank())
                    throw new Exception("Reponse vide de l'IA.");

                text = extractJsonObject(text);
                JSONObject json = new JSONObject(text);

                String nom         = json.optString("nom", "");
                String description = json.optString("description", "");
                String prixRaw     = json.opt("prix") == null ? "0" : String.valueOf(json.opt("prix"));
                String categorie   = json.optString("categorie", "AUTRE").trim().toUpperCase();

                if (!isValidCategorie(categorie)) categorie = "AUTRE";

                final String fNom         = nom;
                final String fDescription = description;
                final String fPrix        = prixRaw;
                final String fCategorie   = categorie;

                Platform.runLater(() -> {
                    fieldNom.setText(fNom);
                    fieldDescription.setText(fDescription);
                    fieldPrix.setText(fPrix);
                    comboCat.setValue(fCategorie);
                    if (lblAiStatus != null)
                        lblAiStatus.setText("Produit analyse avec succes !");
                    btnScanAI.setDisable(false);
                });

            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    if (lblAiStatus != null)
                        lblAiStatus.setText(" Erreur: " + e.getMessage());
                    btnScanAI.setDisable(false);
                    showAlert("Erreur IA", e.getMessage());
                });
            }
        }).start();
    }

    private static String loadGeminiApiKey() {
        String envKey = System.getenv("GEMINI_API_KEY");
        if (envKey != null && !envKey.trim().isEmpty()) return envKey.trim();
        return loadGeminiProperty("gemini.api.key", "");
    }

    private static String loadGeminiApiUrl() {
        String configuredUrl = loadGeminiProperty("gemini.api.url", "");
        if (configuredUrl != null && !configuredUrl.isBlank()) return configuredUrl.trim();
        String model = loadGeminiProperty("gemini.api.model", DEFAULT_GEMINI_MODEL);
        if (model == null || model.isBlank()) model = DEFAULT_GEMINI_MODEL;
        return "https://generativelanguage.googleapis.com/v1beta/models/" + model.trim() + ":generateContent";
    }

    private static String loadGeminiProperty(String key, String fallback) {
        try (InputStream input = AjouterProduitController.class.getResourceAsStream("/config.properties")) {
            if (input == null) return fallback;
            Properties properties = new Properties();
            properties.load(input);
            return properties.getProperty(key, fallback).trim();
        } catch (Exception e) {
            return fallback;
        }
    }

    private String buildGeminiRequestBody(String base64Image, String mimeType) {
        String prompt = "You are a product catalog assistant for a Tunisian e-commerce platform called 7anouti-E.\n"
                + "Analyze this product image and respond ONLY with valid JSON, no markdown and no explanation.\n"
                + "Use exactly these fields: nom, description, prix, categorie.\n"
                + "nom: short French product name.\n"
                + "description: professional French description in 1 or 2 sentences.\n"
                + "prix: estimated Tunisian market price in TND as a plain number.\n"
                + "categorie: one of ALIMENTAIRE, ELECTRONIQUE, MEDICAMENT, HYGIENE, DECOR, MAKEUP, AUTRE.\n"
                + "If the image is a known packaged product, use its real name and realistic Tunisia price.";

        JSONObject imagePart = new JSONObject()
                .put("inline_data", new JSONObject()
                        .put("mime_type", mimeType)
                        .put("data", base64Image));
        JSONObject textPart = new JSONObject().put("text", prompt);
        JSONArray parts = new JSONArray().put(imagePart).put(textPart);
        JSONObject content = new JSONObject().put("parts", parts);
        JSONObject generationConfig = new JSONObject()
                .put("temperature", 0.2)
                .put("maxOutputTokens", 1024);

        return new JSONObject()
                .put("contents", new JSONArray().put(content))
                .put("generationConfig", generationConfig)
                .toString();
    }

    private String extractGeminiText(String body) {
        JSONObject root = new JSONObject(body);
        return root.getJSONArray("candidates")
                .getJSONObject(0)
                .getJSONObject("content")
                .getJSONArray("parts")
                .getJSONObject(0)
                .getString("text");
    }

    private String extractJsonObject(String text) throws Exception {
        String cleaned = text.replace("```json", "").replace("```", "").trim();
        int start = cleaned.indexOf('{');
        int end = cleaned.lastIndexOf('}');
        if (start < 0 || end < start)
            throw new Exception("L'IA n'a pas retourne un objet JSON valide.");
        return cleaned.substring(start, end + 1);
    }

    private boolean isValidCategorie(String categorie) {
        String[] validCats =
                {"ALIMENTAIRE","ELECTRONIQUE","MEDICAMENT","HYGIENE","DECOR","MAKEUP","AUTRE"};
        for (String c : validCats)
            if (c.equals(categorie)) return true;
        return false;
    }

    private String extractJsonString(String json, String key) {
        try {
            int keyIdx = json.indexOf(key);
            if (keyIdx < 0) return null;
            int quoteStart = json.indexOf("\"", keyIdx + key.length());
            if (quoteStart < 0) return null;
            int valueStart = quoteStart + 1;
            StringBuilder sb = new StringBuilder();
            for (int i = valueStart; i < json.length(); i++) {
                char c = json.charAt(i);
                if (c == '\\' && i + 1 < json.length()) {
                    char next = json.charAt(i + 1);
                    if (next == '"')       { sb.append('"');  i++; }
                    else if (next == 'n')  { sb.append('\n'); i++; }
                    else if (next == '\\') { sb.append('\\'); i++; }
                    else { sb.append(c); }
                } else if (c == '"') {
                    break;
                } else {
                    sb.append(c);
                }
            }
            return sb.toString();
        } catch (Exception e) { return null; }
    }

    private String extractJsonValue(String json, String key) {
        try {
            int keyIdx = json.indexOf(key);
            if (keyIdx < 0) return null;
            int start = keyIdx + key.length();
            while (start < json.length() &&
                    (json.charAt(start) == ' ' || json.charAt(start) == '\n'))
                start++;
            if (json.charAt(start) == '"')
                return extractJsonString(json, key);
            int end = start;
            while (end < json.length() && (Character.isDigit(json.charAt(end))
                    || json.charAt(end) == '.' || json.charAt(end) == '-'))
                end++;
            return json.substring(start, end);
        } catch (Exception e) { return null; }
    }

    @FXML
    public void submitForm() {
        if (!validate()) return;

        String nom       = fieldNom.getText().trim();
        String categorie = comboCat.getValue();
        int    stock     = Integer.parseInt(fieldStock.getText().trim());
        Fournisseur f    = comboFournisseur.getValue();
        Integer idFourn  = f.getIdFournisseur();
        int idVendeur    = SessionManager.getCurrentVendeurId();

        Produit existing = ps.findExisting(nom, categorie, idFourn, idVendeur,
                Double.parseDouble(fieldPrix.getText().trim()));

        if (existing != null) {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Produit deja existant");
            confirm.setHeaderText("\"" + nom + "\" existe deja dans votre catalogue !");
            confirm.setContentText(
                    "Stock actuel: " + existing.getQuantiteStock() + " unites.\n\n" +
                            "Voulez-vous ajouter " + stock + " unites au stock existant ?\n" +
                            " Nouveau stock: " + (existing.getQuantiteStock() + stock));
            ButtonType btnAjouter = new ButtonType("Augmenter le stock");
            ButtonType btnAnnuler = new ButtonType("Annuler",
                    javafx.scene.control.ButtonBar.ButtonData.CANCEL_CLOSE);
            confirm.getButtonTypes().setAll(btnAjouter, btnAnnuler);
            confirm.showAndWait().ifPresent(response -> {
                if (response == btnAjouter) {
                    ps.increaseStock(existing.getIdProduit(), stock);
                    Alert ok = new Alert(Alert.AlertType.INFORMATION);
                    ok.setTitle("Succes"); ok.setHeaderText(null);
                    ok.setContentText("Stock mis a jour !\nNouveau stock: "
                            + (existing.getQuantiteStock() + stock) + " unites.");
                    ok.showAndWait();
                    resetForm();
                }
            });
            return;
        }

        Produit p = new Produit();
        p.setIdVendeur    (idVendeur);
        p.setNom          (nom);
        p.setDescription  (fieldDescription.getText().trim());
        p.setPrix         (Double.parseDouble(fieldPrix.getText().trim()));
        p.setQuantiteStock(stock);
        p.setSeuilAlerte  (fieldSeuil.getText().trim().isEmpty() ? 5
                : Integer.parseInt(fieldSeuil.getText().trim()));
        p.setCategorie    (categorie);
        p.setStatut       (comboStatut.getValue() != null
                ? comboStatut.getValue() : "ACTIF");
        p.setImage        (fieldImage.getText().trim());
        p.setIdFournisseur(idFourn);

        ps.addEntity(p);
        if (moduleNavigator != null) {
            moduleNavigator.navigateBackToBoutique();
        } else {
            Alert a = new Alert(Alert.AlertType.INFORMATION);
            a.setTitle("Succes"); a.setHeaderText(null);
            a.setContentText("Produit ajoute avec succes !"); a.showAndWait();
            resetForm();
        }
    }

    @FXML
    public void resetForm() {
        fieldNom.clear(); fieldDescription.clear();
        fieldPrix.clear(); fieldStock.clear();
        fieldSeuil.clear(); fieldImage.clear();
        comboCat.setValue(null);
        comboStatut.setValue("ACTIF");
        comboFournisseur.setValue(null);
        pickedImageFile = null;
        if (imagePreview != null) imagePreview.setImage(null);
        if (lblAiStatus != null) lblAiStatus.setText("");
        hideAllErrors();
        previewNom.setText("Nom du produit");
        previewCat.setText("Categorie");
        previewPrix.setText("0.00 TND");
        previewStock.setText("Stock: 0");
    }

    private boolean validate() {
        boolean ok = true;
        String nom = fieldNom.getText().trim();
        if (nom.isEmpty()) {
            show(errNom, " Nom obligatoire."); ok = false;
        } else if (nom.length() < 2) {
            show(errNom, " Minimum 2 caracteres."); ok = false;
        } else hide(errNom);

        try {
            double prix = Double.parseDouble(fieldPrix.getText().trim());
            if (prix <= 0) { show(errPrix, " Prix doit etre > 0."); ok = false; }
            else hide(errPrix);
        } catch (NumberFormatException e) {
            show(errPrix, " Prix invalide (ex: 12.50)."); ok = false;
        }

        try {
            int stock = Integer.parseInt(fieldStock.getText().trim());
            if (stock < 0) { show(errStock, " Stock >= 0."); ok = false; }
            else hide(errStock);
        } catch (NumberFormatException e) {
            show(errStock, " Entier uniquement."); ok = false;
        }

        if (comboCat.getValue() == null) {
            show(errCat, " Categorie obligatoire."); ok = false;
        } else hide(errCat);

        if (comboFournisseur.getValue() == null) {
            showAlert("Fournisseur obligatoire",
                    "Veuillez selectionner un fournisseur avant d'enregistrer le produit.");
            return false;
        }

        return ok;
    }


    private void showAlert(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle(title); a.setHeaderText(null);
        a.setContentText(msg); a.showAndWait();
    }
    private void show(Label l, String m) {
        l.setText(m); l.setVisible(true); l.setManaged(true);
    }
    private void hide(Label l) { l.setVisible(false); l.setManaged(false); }
    private void hideAllErrors() {
        hide(errNom); hide(errPrix); hide(errStock); hide(errCat);
    }
}
