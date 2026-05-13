package projet.hanouti.produit_fournisseur.controllers;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Modality;
import javafx.stage.Stage;
import netscape.javascript.JSObject;
import projet.hanouti.produit_fournisseur.entities.Fournisseur;
import projet.hanouti.produit_fournisseur.services.FournisseurService;
import projet.hanouti.produit_fournisseur.utils.SessionManager;

import java.util.regex.Pattern;

public class AjouterFournisseurController {

    private ModuleNavigator moduleNavigator;
    public void setModuleNavigator(ModuleNavigator navigator) { this.moduleNavigator = navigator; }

    @FXML private TextField fieldNom;
    @FXML private TextField fieldContact;
    @FXML private TextField fieldEmail;
    @FXML private TextField fieldTelephone;
    @FXML private TextArea  fieldAdresse;
    @FXML private TextArea  fieldConditions;
    @FXML private CheckBox  checkActif;

    @FXML private Label errNom;
    @FXML private Label errContact;
    @FXML private Label errEmail;
    @FXML private Label errTelephone;
    @FXML private Label errAdresse;

    @FXML private Label previewNom;
    @FXML private Label previewContact;
    @FXML private Label previewEmail;
    @FXML private Label previewTel;
    @FXML private Label previewStatut;

    private static final Pattern EMAIL_REGEX =
            Pattern.compile("^[\\w.-]+@[\\w.-]+\\.[a-zA-Z]{2,}$");
    private static final Pattern PHONE_REGEX =
            Pattern.compile("^[0-9+\\-\\s]{8,20}$");

    private final FournisseurService fs = new FournisseurService();
    private String pickedAddress = null;

    // Strong reference to prevent GC
    private JavaBridge javaBridge;

    @FXML
    public void initialize() {
        fieldNom.textProperty().addListener((o, a, b) -> {
            if (b.trim().isEmpty()) show(errNom, " Obligatoire.");
            else hide(errNom);
            previewNom.setText(b.isEmpty() ? "Nom societe" : b);
        });
        fieldContact.textProperty().addListener((o, a, b) ->
                previewContact.setText(b.isEmpty() ? "Contact" : b));
        fieldEmail.textProperty().addListener((o, a, b) -> {
            if (!b.isEmpty()) {
                if (!EMAIL_REGEX.matcher(b.trim()).matches())
                    show(errEmail, " Format invalide.");
                else hide(errEmail);
            } else hide(errEmail);
            previewEmail.setText(b.isEmpty() ? "email@..." : b);
        });
        fieldTelephone.textProperty().addListener((o, a, b) -> {
            if (!b.isEmpty()) {
                if (!PHONE_REGEX.matcher(b.trim()).matches())
                    show(errTelephone, " Format invalide.");
                else hide(errTelephone);
            } else hide(errTelephone);
            previewTel.setText(b.isEmpty() ? "Telephone" : b);
        });
        checkActif.selectedProperty().addListener((o, a, b) ->
                previewStatut.setText(b ? "Actif" : "Inactif"));
    }


    @FXML
    public void openMapPicker() {
        pickedAddress = null;

        Stage mapStage = new Stage();
        mapStage.initModality(Modality.APPLICATION_MODAL);
        mapStage.setTitle("Choisir l'adresse sur la carte");
        mapStage.setMinWidth(700);
        mapStage.setMinHeight(600);

        // Top bar
        HBox topBar = new HBox(12);
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.setPadding(new Insets(12, 16, 12, 16));
        topBar.setStyle("-fx-background-color:#050A38;");
        Label titleLbl = new Label("  Cliquez sur la carte pour choisir l'adresse");
        titleLbl.setStyle("-fx-text-fill:white;-fx-font-size:13px;-fx-font-weight:bold;");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Button cancelBtn = new Button("  Fermer sans choisir");
        cancelBtn.setStyle(
                "-fx-background-color:#FFEEEE;-fx-text-fill:#CC2222;" +
                        "-fx-background-radius:6;-fx-cursor:hand;" +
                        "-fx-border-color:transparent;-fx-padding:6 14;");
        cancelBtn.setOnAction(e -> mapStage.close());
        topBar.getChildren().addAll(titleLbl, spacer, cancelBtn);

        // Search bar
        HBox searchBar = new HBox(8);
        searchBar.setAlignment(Pos.CENTER_LEFT);
        searchBar.setPadding(new Insets(10, 16, 10, 16));
        searchBar.setStyle("-fx-background-color:#0C145F;");
        TextField searchField = new TextField();
        searchField.setPromptText("  Rechercher une ville ou adresse...");
        searchField.setStyle(
                "-fx-background-color:white;-fx-border-color:#4F61FF;" +
                        "-fx-border-radius:8;-fx-background-radius:8;" +
                        "-fx-padding:7 12;-fx-font-size:12px;");
        HBox.setHgrow(searchField, Priority.ALWAYS);
        Button searchBtn = new Button("Rechercher");
        searchBtn.setStyle(
                "-fx-background-color:#192BCC;-fx-text-fill:white;" +
                        "-fx-background-radius:8;-fx-cursor:hand;" +
                        "-fx-border-color:transparent;-fx-padding:7 16;-fx-font-weight:bold;");
        searchBar.getChildren().addAll(searchField, searchBtn);

        // Address bar
        HBox addrBar = new HBox(10);
        addrBar.setAlignment(Pos.CENTER_LEFT);
        addrBar.setPadding(new Insets(10, 16, 10, 16));
        addrBar.setStyle("-fx-background-color:#F0F2FF;");
        addrBar.setMinHeight(55);
        addrBar.setPrefHeight(55);
        addrBar.setMaxHeight(55);
        Label addrIcon = new Label("");
        addrIcon.setStyle("-fx-font-size:16px;");
        Label addrLabel = new Label("Cliquez sur la carte pour selectionner une adresse");
        addrLabel.setStyle("-fx-text-fill:#888;-fx-font-size:12px;");
        addrLabel.setWrapText(true);
        HBox.setHgrow(addrLabel, Priority.ALWAYS);
        Button confirmBtn = new Button("  Confirmer cette adresse");
        confirmBtn.setStyle(
                "-fx-background-color:#192BCC;-fx-text-fill:white;" +
                        "-fx-background-radius:8;-fx-cursor:hand;" +
                        "-fx-border-color:transparent;-fx-padding:8 16;-fx-font-weight:bold;");
        addrBar.getChildren().addAll(addrIcon, addrLabel, confirmBtn);

        // WebView
        WebView webView = new WebView();
        webView.setPrefHeight(500);
        WebEngine engine = webView.getEngine();
        webView.setContextMenuEnabled(false);

        String html =
                "<!DOCTYPE html><html><head><meta charset='utf-8'/>" +
                        "<style>*{margin:0;padding:0;}html,body{width:100%;height:100%;}" +
                        "#map{width:100%;height:100%;}</style>" +
                        "<link rel='stylesheet' href='https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.9.4/leaflet.min.css'/>" +
                        "<script src='https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.9.4/leaflet.min.js'></script>" +
                        "</head><body><div id='map'></div><script>" +
                        "var map=L.map('map',{preferCanvas:true}).setView([36.8065,10.1815],12);" +
                        "L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png'," +
                        "{attribution:'OSM',maxZoom:19}).addTo(map);" +
                        "var marker=null;" +
                        "var blueIcon=L.divIcon({className:''," +
                        "html:'<div style=\"width:18px;height:18px;background:#192BCC;" +
                        "border:3px solid white;border-radius:50%;" +
                        "box-shadow:0 2px 8px rgba(0,0,0,0.4);\"></div>'," +
                        "iconSize:[18,18],iconAnchor:[9,9]});" +
                        "map.on('click',function(e){" +
                        "var lat=e.latlng.lat;var lng=e.latlng.lng;" +
                        "if(marker){marker.setLatLng([lat,lng]);}else{" +
                        "marker=L.marker([lat,lng],{icon:blueIcon}).addTo(map);}" +
                        "fetch('https://nominatim.openstreetmap.org/reverse?lat='+lat+'&lon='+lng+'&format=json&accept-language=fr')" +
                        ".then(function(r){return r.json();})" +
                        ".then(function(d){" +
                        "var dn=d&&d.display_name?d.display_name:'';" +
                        "if(window.javaBridge){" +
                        "window.javaBridge.onAddressFound(dn?dn:lat+', '+lng);" +
                        "}" +
                        "}).catch(function(){" +
                        "if(window.javaBridge){window.javaBridge.onAddressFound(lat+', '+lng);}" +
                        "});" +
                        "});" +
                        "function searchAddress(q){" +
                        "fetch('https://nominatim.openstreetmap.org/search?q='" +
                        "+encodeURIComponent(q)+'&format=json&limit=1&accept-language=fr')" +
                        ".then(function(r){return r.json();})" +
                        ".then(function(d){if(d&&d.length>0){" +
                        "var lat=parseFloat(d[0].lat);var lon=parseFloat(d[0].lon);" +
                        "var dn=d[0].display_name||'';" +
                        "map.setView([lat,lon],14);" +
                        "if(marker){marker.setLatLng([lat,lon]);}else{" +
                        "marker=L.marker([lat,lon],{icon:blueIcon}).addTo(map);}" +
                        "if(window.javaBridge){" +
                        "window.javaBridge.onAddressFound(dn?dn:lat+', '+lon);" +
                        "}" +
                        "}});}" +
                        "window.searchAddress=searchAddress;" +
                        "</script></body></html>";

        engine.getLoadWorker().stateProperty().addListener(
                (obs, oldState, newState) -> {
                    if (newState == javafx.concurrent.Worker.State.SUCCEEDED) {
                        Platform.runLater(() -> {
                            JSObject win = (JSObject) engine.executeScript("window");
                            // Store in field to prevent garbage collection
                            javaBridge = new JavaBridge(addrLabel, confirmBtn, address -> {
                                pickedAddress = address;
                            });
                            win.setMember("javaBridge", javaBridge);
                        });
                    }
                });

        engine.loadContent(html);

        searchBtn.setOnAction(e -> {
            String q = searchField.getText().trim();
            if (!q.isEmpty())
                engine.executeScript("searchAddress('" + q.replace("'", "\\'") + "')");
        });
        searchField.setOnAction(e -> {
            String q = searchField.getText().trim();
            if (!q.isEmpty())
                engine.executeScript("searchAddress('" + q.replace("'", "\\'") + "')");
        });

        confirmBtn.setOnAction(e -> {
            if (pickedAddress != null) {
                fieldAdresse.setText(pickedAddress);
                hide(errAdresse);
                mapStage.close();
            } else {
                addrLabel.setStyle("-fx-text-fill:#CC2222;-fx-font-size:12px;");
                addrLabel.setText(" Veuillez d'abord cliquer sur la carte.");
            }
        });

        VBox root = new VBox(topBar, searchBar, webView, addrBar);
        VBox.setVgrow(webView, Priority.ALWAYS);
        Scene scene = new Scene(root, 1000, 750);
        mapStage.setScene(scene);
        mapStage.show();
    }

    @FXML
    public void submitForm() {
        if (!validate()) return;
        Fournisseur f = new Fournisseur();
        f.setIdVendeur          (SessionManager.getCurrentVendeurId());
        f.setNomSociete         (fieldNom.getText().trim());
        f.setContactNom         (fieldContact.getText().trim());
        f.setEmail              (fieldEmail.getText().trim());
        f.setTelephone          (fieldTelephone.getText().trim());
        f.setAdresse            (fieldAdresse.getText().trim());
        f.setConditionsLivraison(fieldConditions.getText().trim());
        f.setActif              (checkActif.isSelected());

        try {
            fs.addEntity(f);
            Alert a = new Alert(Alert.AlertType.INFORMATION);
            a.setTitle("Succes"); a.setHeaderText(null);
            a.setContentText("Fournisseur ajoute avec succes !"); a.showAndWait();
            if (moduleNavigator != null) {
                moduleNavigator.navigateBackToFournisseurs();
            } else {
                resetForm();
            }
        } catch (Exception e) {
            String msg = e.getMessage() != null && e.getMessage().contains("Duplicate")
                    ? detectDuplicateField(e.getMessage())
                    : "Erreur lors de l'ajout du fournisseur.";
            Alert err = new Alert(Alert.AlertType.ERROR);
            err.setTitle("Fournisseur en double");
            err.setHeaderText("Ce fournisseur existe deja !");
            err.setContentText(msg);
            err.showAndWait();
        }
    }



    private String detectDuplicateField(String errorMsg) {
        if (errorMsg.contains("uq_email"))
            return "Un fournisseur avec cet email existe deja dans votre liste.";
        if (errorMsg.contains("uq_tel"))
            return "Un fournisseur avec ce numero de telephone existe deja dans votre liste.";
        if (errorMsg.contains("uq_nom"))
            return "Un fournisseur avec ce nom de societe existe deja dans votre liste.";
        return "Un fournisseur avec ces informations existe deja dans votre liste.";
    }
    @FXML
    public void resetForm() {
        fieldNom.clear(); fieldContact.clear();
        fieldEmail.clear(); fieldTelephone.clear();
        fieldAdresse.clear(); fieldConditions.clear();
        checkActif.setSelected(true);
        pickedAddress = null;
        hideAllErrors();
        previewNom.setText("Nom societe");
        previewContact.setText("Contact");
        previewEmail.setText("email@...");
        previewTel.setText("Telephone");
        previewStatut.setText("Actif");
    }

    private boolean validate() {
        boolean ok = true;
        if (fieldNom.getText().trim().isEmpty()) {
            show(errNom, " Nom obligatoire."); ok = false;
        } else if (fieldNom.getText().trim().length() < 2) {
            show(errNom, " Minimum 2 caracteres."); ok = false;
        } else hide(errNom);
        if (fieldContact.getText().trim().isEmpty()) {
            show(errContact, " Obligatoire."); ok = false;
        } else hide(errContact);
        String email = fieldEmail.getText().trim();
        if (email.isEmpty()) {
            show(errEmail, " Obligatoire."); ok = false;
        } else if (!EMAIL_REGEX.matcher(email).matches()) {
            show(errEmail, " Format invalide."); ok = false;
        } else hide(errEmail);
        String tel = fieldTelephone.getText().trim();
        if (tel.isEmpty()) {
            show(errTelephone, " Obligatoire."); ok = false;
        } else if (!PHONE_REGEX.matcher(tel).matches()) {
            show(errTelephone, " Format invalide."); ok = false;
        } else hide(errTelephone);
        if (fieldAdresse.getText().trim().isEmpty()) {
            show(errAdresse, " Adresse obligatoire."); ok = false;
        } else hide(errAdresse);
        return ok;
    }

    private void show(Label l, String m) {
        l.setText(m); l.setVisible(true); l.setManaged(true);
    }
    private void hide(Label l) {
        l.setVisible(false); l.setManaged(false);
    }
    private void hideAllErrors() {
        hide(errNom); hide(errContact); hide(errEmail);
        hide(errTelephone); hide(errAdresse);
    }
}
