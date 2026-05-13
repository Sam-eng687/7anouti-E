package edu.hanouti.gui.components;

import javafx.animation.*;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Arc;
import javafx.scene.shape.ArcType;
import javafx.scene.shape.Circle;
import javafx.util.Duration;

/**
 * Nav item minimaliste — icone seule + tooltip au survol.
 * Actif : icone coloree + arc neon gauche + fond subtil.
 */
public class SpotlightNavItem extends StackPane {

    private String neon      = "#38bdf8";
    private String bgIcon    = "#0f1a2e";
    private String textIdle  = "#64748b";
    private boolean isDark   = true;

    private final String moduleId;
    private boolean active = false;

    private final VBox  iconBox;
    private final Label iconLbl;
    private final Arc   arcLeft;

    private Runnable onActivate;

    public SpotlightNavItem(String icon, String title, String subtitle, String moduleId) {
        this.moduleId = moduleId;

        // ── Arc neon gauche (indicateur actif) ──
        arcLeft = new Arc(0, 28, 12, 28, -70, 140);
        arcLeft.setType(ArcType.OPEN);
        arcLeft.setFill(Color.TRANSPARENT);
        arcLeft.setStroke(Color.web(neon));
        arcLeft.setStrokeWidth(2.5);
        arcLeft.setOpacity(0);
        arcLeft.setEffect(new DropShadow(10, Color.web(neon)));
        StackPane.setAlignment(arcLeft, Pos.CENTER_LEFT);
        StackPane.setMargin(arcLeft, new Insets(0, 0, 0, -4));

        // ── Icone box ──
        iconBox = new VBox();
        iconBox.setAlignment(Pos.CENTER);
        iconBox.setPrefSize(46, 46);
        iconBox.setMinSize(46, 46);
        iconBox.setMaxSize(46, 46);
        iconBox.setStyle(
            "-fx-background-color:" + bgIcon + ";" +
            "-fx-background-radius:12;");
        iconLbl = new Label(icon);
        iconLbl.setStyle("-fx-font-size:20px;");
        iconBox.getChildren().add(iconLbl);

        // ── Tooltip au survol ──
        Tooltip tip = new Tooltip(title + "\n" + subtitle);
        tip.setStyle(
            "-fx-background-color:#111425;" +
            "-fx-text-fill:#f1f5f9;" +
            "-fx-font-size:12px;" +
            "-fx-border-color:rgba(56,189,248,0.3);" +
            "-fx-border-width:1;" +
            "-fx-border-radius:8;" +
            "-fx-background-radius:8;" +
            "-fx-padding:8 12;");
        tip.setShowDelay(Duration.millis(300));
        Tooltip.install(iconBox, tip);

        // ── Wrapper centre ──
        HBox wrapper = new HBox(iconBox);
        wrapper.setAlignment(Pos.CENTER);
        wrapper.setPadding(new Insets(6, 0, 6, 0));

        this.getChildren().addAll(wrapper, arcLeft);
        this.setAlignment(Pos.CENTER);
        this.setPrefWidth(68);
        this.setPrefHeight(60);

        // ── Hover ──
        this.setOnMouseEntered(e -> {
            if (!active) {
                iconBox.setStyle(
                    "-fx-background-color:rgba(56,189,248,0.08);" +
                    "-fx-background-radius:12;");
                ScaleTransition st = new ScaleTransition(Duration.millis(150), iconBox);
                st.setToX(1.08); st.setToY(1.08); st.play();
            }
        });
        this.setOnMouseExited(e -> {
            if (!active) {
                iconBox.setStyle("-fx-background-color:" + bgIcon + ";-fx-background-radius:12;");
                ScaleTransition st = new ScaleTransition(Duration.millis(150), iconBox);
                st.setToX(1.0); st.setToY(1.0); st.play();
            }
        });
        this.setOnMouseClicked(e -> { if (onActivate != null) onActivate.run(); });
        this.setCursor(javafx.scene.Cursor.HAND);
    }

    public void updateTheme(boolean dark) {
        this.isDark = dark;
        this.bgIcon = dark ? "#0f1a2e" : "#f1f5f9";
        this.textIdle = dark ? "#64748b" : "#94a3b8";
        this.neon = "#38bdf8"; // Garder le bleu neon car il ressort bien dans les deux

        if (!active) {
            iconBox.setStyle("-fx-background-color:" + bgIcon + ";-fx-background-radius:12;");
            iconLbl.setStyle("-fx-font-size:20px; -fx-text-fill:" + textIdle + ";");
        } else {
            setActive(true); // Rafraichir le style actif
        }
        
        // Mettre à jour l'arc
        arcLeft.setStroke(Color.web(neon));
        arcLeft.setEffect(new DropShadow(10, Color.web(neon)));
    }

    public void setActive(boolean val) {
        this.active = val;
        if (val) {
            // Icone box : fond bleu + bordure neon
            String activeBg = isDark ? "#0d2137" : "#e0f2fe";
            iconBox.setStyle(
                "-fx-background-color:" + activeBg + ";" +
                "-fx-background-radius:12;" +
                "-fx-border-color:" + neon + ";" +
                "-fx-border-width:1.5;" +
                "-fx-border-radius:12;");
            iconBox.setEffect(new DropShadow(16, Color.web(neon, 0.6)));

            // Arc gauche apparait
            FadeTransition arcFade = new FadeTransition(Duration.millis(300), arcLeft);
            arcFade.setToValue(1); arcFade.play();

            // Pulse icone
            ScaleTransition pulse = new ScaleTransition(Duration.millis(500), iconBox);
            pulse.setFromX(1); pulse.setFromY(1);
            pulse.setToX(1.1); pulse.setToY(1.1);
            pulse.setAutoReverse(true); pulse.setCycleCount(2);
            pulse.play();

        } else {
            iconBox.setStyle("-fx-background-color:" + bgIcon + ";-fx-background-radius:12;");
            iconBox.setEffect(null);
            FadeTransition arcFade = new FadeTransition(Duration.millis(200), arcLeft);
            arcFade.setToValue(0); arcFade.play();
        }
    }

    public void setOnActivate(Runnable r) { this.onActivate = r; }
    public String getModuleId()           { return moduleId; }
    public boolean isActive()             { return active; }
}
