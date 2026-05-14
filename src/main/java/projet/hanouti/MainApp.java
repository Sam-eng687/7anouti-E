package projet.hanouti;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.scene.control.Alert;
import javafx.scene.control.DialogPane;
import projet.hanouti.wejden.gui.HanoutiDashboard;

/**
 * Entree produit par defaut : ecran de connexion.
 * <p>
 * Pour tester le dashboard marketing premium (barre LIVE, campagnes, shell complet — code dans
 * {@link HanoutiDashboard}, pas le FXML {@code ressources-wejden/FXML/Dashboard.fxml}),
 * mettre {@link #TEST_START_MARKETING_PREMIUM} a {@code true} : {@code main} lance alors
 * {@link HanoutiDashboard} au lieu de cette classe.
 */
public class MainApp extends Application {

    /**
     * {@code true} : demarrage sur {@link HanoutiDashboard} (titre fenetre
     * {@code 7anouti-E - Dashboard Marketing Premium}, barre d'alertes LIVE en bas).
     * {@code false} : ecran de connexion habituel via {@link MainApp}.
     */
    private static final boolean TEST_START_MARKETING_PREMIUM = false;

    @Override
    public void start(Stage stage) {
        try {
            startLogin(stage);
            System.out.println("Application demarree avec succes!");
        } catch (Exception e) {
            e.printStackTrace();
            showErrorAlert("Erreur de demarrage",
                    "Impossible de charger l'interface",
                    "Erreur: " + e.getMessage());
        }
    }

    private void startLogin(Stage stage) throws Exception {
        System.out.println("Chargement de login_view.fxml...");

        java.net.URL fxmlUrl = getClass().getResource("/FXML/user_auth/login/login_view.fxml");

        if (fxmlUrl == null) {
            throw new Exception("login_view.fxml introuvable");
        }

        java.net.URL cssUrl = getClass().getResource("/styles/user_auth/login/login.css");
        if (cssUrl == null) {
            System.err.println("login.css introuvable dans /styles/user_auth/login/");
        } else {
            System.out.println("CSS trouve: " + cssUrl.toExternalForm());
        }

        Parent root = FXMLLoader.load(fxmlUrl);

        Scene scene = new Scene(root);

        if (cssUrl != null) {
            scene.getStylesheets().clear();
            scene.getStylesheets().add(cssUrl.toExternalForm());
            System.out.println("CSS charge avec succes!");
        }

        stage.setScene(scene);
        stage.setTitle("7anouti-E - Connexion");
        stage.centerOnScreen();
        stage.show();
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
        if (TEST_START_MARKETING_PREMIUM) {
            Application.launch(HanoutiDashboard.class, args);
        } else {
            Application.launch(MainApp.class, args);
        }
    }
}
