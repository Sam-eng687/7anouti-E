package org.example.GUI;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.scene.control.Alert;
import javafx.scene.control.DialogPane;

public class HomePage extends Application {

    @Override
    public void start(Stage stage) {
        try {
            System.out.println("Chargement de login_view.fxml...");

            java.net.URL fxmlUrl = getClass().getResource("/user/login/login_view.fxml");

            if (fxmlUrl == null) {
                throw new Exception("login_view.fxml introuvable");
            }

            // 1. Charger le CSS d'abord
            java.net.URL cssUrl = getClass().getResource("/user/login/login.css");
            if (cssUrl == null) {
                System.err.println("login.css introuvable dans /user/login/");
            } else {
                System.out.println("CSS trouve: " + cssUrl.toExternalForm());
            }

            // 2. Charger le FXML
            Parent root = FXMLLoader.load(fxmlUrl);

            // 3. Creer la Scene
            Scene scene = new Scene(root);

            // 4. Ajouter le CSS a la Scene
            if (cssUrl != null) {
                scene.getStylesheets().clear();
                scene.getStylesheets().add(cssUrl.toExternalForm());
                System.out.println("CSS charge avec succes!");
            }

            stage.setScene(scene);
            stage.setTitle("7anouti-E - Connexion");
            stage.centerOnScreen();
            stage.show();

            System.out.println("Application demarree avec succes!");

        } catch (Exception e) {
            e.printStackTrace();
            showErrorAlert("Erreur de demarrage",
                    "Impossible de charger l'interface",
                    "Erreur: " + e.getMessage());
        }
    }

    private void showErrorAlert(String title, String header, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);

        DialogPane dialogPane = alert.getDialogPane();
        dialogPane.setStyle("-fx-background-color: white; " +
                "-fx-border-color: #F39C12; " +
                "-fx-border-width: 2; " +
                "-fx-border-radius: 15; " +
                "-fx-background-radius: 15;");

        alert.showAndWait();
    }

    public static void main(String[] args) {
        launch(args);
    }
}