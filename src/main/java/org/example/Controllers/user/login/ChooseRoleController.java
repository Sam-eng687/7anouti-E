package org.example.Controllers.user.login;

import javafx.animation.*;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.ToggleButton;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

public class ChooseRoleController {

    @FXML private AnchorPane rootPane;
    @FXML private ImageView logoView;
    @FXML private Pane logoGlow;
    @FXML private VBox roleCard;
    @FXML private ToggleButton themeToggleBtn;

    @FXML private VBox acheteurCard;
    @FXML private VBox vendeurCard;
    @FXML private VBox livreurCard;
    @FXML private Hyperlink backLoginLink;

    @FXML
    public void initialize() {
        if (!rootPane.getStyleClass().contains("login-root")) {
            rootPane.getStyleClass().add("login-root");
        }

        setDarkMode(true);

        themeToggleBtn.setSelected(true);
        themeToggleBtn.setText("\u2600  Jour");
        themeToggleBtn.selectedProperty().addListener((obs, old, selected) -> setDarkMode(selected));

        try {
            java.io.InputStream s = getClass().getResourceAsStream("/user/image/logo.png");
            if (s != null && logoView != null) {
                logoView.setImage(new Image(s));
            }
        } catch (Exception e) {
            System.err.println("Logo introuvable");
        }

        setupRoleCard(acheteurCard, "/user/login/register_view.fxml");
        setupRoleCard(vendeurCard, "/user/login/register_vendeur_view.fxml");
        setupRoleCard(livreurCard, "/user/login/register_livreur_view.fxml");

        backLoginLink.setOnAction(e -> navigate("/user/login/login_view.fxml"));

        playEntrance();
        animateGlow();
    }

    private void setupRoleCard(VBox card, String path) {
        card.setOnMouseClicked(e -> navigate(path));

        card.setOnMouseEntered(e -> {
            card.getStyleClass().add("role-choice-card-hover");
            scale(card, 1.035);
        });

        card.setOnMouseExited(e -> {
            card.getStyleClass().remove("role-choice-card-hover");
            scale(card, 1.0);
        });

        card.setOnMousePressed(e -> scale(card, 0.985));
        card.setOnMouseReleased(e -> scale(card, 1.035));
    }

    private void navigate(String path) {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource(path));
            javafx.scene.Parent root = loader.load();

            javafx.scene.Scene scene = rootPane.getScene();
            scene.setRoot(root);

            java.net.URL css = getClass().getResource("/user/login/login.css");
            if (css != null && !scene.getStylesheets().contains(css.toExternalForm())) {
                scene.getStylesheets().add(css.toExternalForm());
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setDarkMode(boolean dark) {
        if (dark) {
            if (!rootPane.getStyleClass().contains("dark")) {
                rootPane.getStyleClass().add("dark");
            }
            themeToggleBtn.setText("\u2600  Jour");
        } else {
            rootPane.getStyleClass().remove("dark");
            themeToggleBtn.setText("\u263D  Nuit");
        }
    }

    private void scale(Node node, double value) {
        ScaleTransition st = new ScaleTransition(Duration.millis(140), node);
        st.setToX(value);
        st.setToY(value);
        st.setInterpolator(Interpolator.EASE_OUT);
        st.play();
    }

    private void playEntrance() {
        roleCard.setOpacity(0);
        roleCard.setScaleX(0.94);
        roleCard.setScaleY(0.94);

        FadeTransition fade = new FadeTransition(Duration.millis(550), roleCard);
        fade.setFromValue(0);
        fade.setToValue(1);

        ScaleTransition scale = new ScaleTransition(Duration.millis(550), roleCard);
        scale.setFromX(0.94);
        scale.setFromY(0.94);
        scale.setToX(1);
        scale.setToY(1);
        scale.setInterpolator(Interpolator.EASE_OUT);

        new ParallelTransition(fade, scale).play();
    }

    private void animateGlow() {
        if (logoGlow == null) return;

        Timeline pulse = new Timeline(
                new KeyFrame(Duration.ZERO,
                        new KeyValue(logoGlow.scaleXProperty(), 1.0),
                        new KeyValue(logoGlow.scaleYProperty(), 1.0),
                        new KeyValue(logoGlow.opacityProperty(), 0.6)),
                new KeyFrame(Duration.millis(2000),
                        new KeyValue(logoGlow.scaleXProperty(), 1.15),
                        new KeyValue(logoGlow.scaleYProperty(), 1.15),
                        new KeyValue(logoGlow.opacityProperty(), 1.0))
        );

        pulse.setCycleCount(Timeline.INDEFINITE);
        pulse.setAutoReverse(true);
        pulse.play();
    }
}