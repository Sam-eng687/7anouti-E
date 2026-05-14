package projet.hanouti.AIachat.services;

import com.github.sarxos.webcam.Webcam;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.concurrent.atomic.AtomicBoolean;

public class CameraService {

    public byte[] pickFromFile(Window ownerWindow) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Choisir une image produit");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Images", "*.jpg", "*.jpeg", "*.png", "*.webp"));
        File file = chooser.showOpenDialog(ownerWindow);
        if (file == null) return null;
        try {
            return Files.readAllBytes(file.toPath());
        } catch (IOException e) {
            return null;
        }
    }

    public byte[] captureFromWebcam(Window ownerWindow) {
        Webcam webcam = Webcam.getDefault();
        if (webcam == null) return null;

        byte[][] result = {null};
        // This flag tells the background thread when to stop fetching images
        AtomicBoolean isRunning = new AtomicBoolean(true);

        Stage modal = new Stage();
        modal.initModality(Modality.WINDOW_MODAL);
        modal.initOwner(ownerWindow);
        modal.setTitle("Prendre une photo");
        modal.setResizable(false);

        ImageView preview = new ImageView();
        preview.setFitWidth(480);
        preview.setFitHeight(360);
        preview.setPreserveRatio(true);

        Label statusLabel = new Label("Démarrage de la caméra...");
        statusLabel.setStyle("-fx-font-size:12px; -fx-text-fill:#8B9DC3;");

        Button captureBtn = new Button("📷  Capturer");
        captureBtn.setDisable(true);
        captureBtn.setStyle("-fx-background-color:linear-gradient(to bottom right,#14B8A6,#0EA5E9);-fx-text-fill:white;-fx-font-size:13px;-fx-font-weight:bold;-fx-background-radius:10;-fx-padding:8 20 8 20;-fx-cursor:hand;");

        Button cancelBtn = new Button("Annuler");
        cancelBtn.setStyle("-fx-background-color:transparent;-fx-border-color:#2A3452;-fx-border-radius:10;-fx-background-radius:10;-fx-text-fill:#8B9DC3;-fx-font-size:13px;-fx-padding:8 20 8 20;-fx-cursor:hand;");

        HBox buttons = new HBox(12, captureBtn, cancelBtn);
        buttons.setAlignment(javafx.geometry.Pos.CENTER);

        VBox root = new VBox(14, preview, statusLabel, buttons);
        root.setAlignment(javafx.geometry.Pos.CENTER);
        root.setPadding(new Insets(20));
        root.setStyle("-fx-background-color:#0C0F18;");

        modal.setScene(new Scene(root));

        // ── BACKGROUND THREAD ──────────────────────────────────────────────────
        Thread cameraWorker = new Thread(() -> {
            try {
                webcam.open();

                Platform.runLater(() -> {
                    statusLabel.setText("Caméra prête - cliquez sur Capturer.");
                    captureBtn.setDisable(false);
                });

                // Instead of Timeline, we use a while loop on this background thread
                while (isRunning.get()) {
                    // 1. This is the blocking call. It happens OFF the UI thread.
                    BufferedImage frame = webcam.getImage();

                    if (frame != null) {
                        // 2. Convert to FX Image (Fast with SwingFXUtils)
                        javafx.scene.image.Image fxImage = SwingFXUtils.toFXImage(frame, null);

                        // 3. Only the final update happens on the UI thread
                        Platform.runLater(() -> {
                            if (isRunning.get()) {
                                preview.setImage(fxImage);
                            }
                        });
                    }
                    // Throttle to ~20 FPS to avoid overloading the CPU
                    Thread.sleep(50);
                }
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> statusLabel.setText("Erreur caméra : " + e.getMessage()));
            } finally {
                closeWebcam(webcam);
            }
        }, "Camera-Worker-Thread");

        cameraWorker.setDaemon(true);
        cameraWorker.start();

        // ── Button Actions ──────────────────────────────────────────────────────
        captureBtn.setOnAction(ev -> {
            isRunning.set(false); // Stop the loop
            BufferedImage frame = webcam.getImage();
            if (frame != null) result[0] = toJpegBytes(frame);
            modal.close();
        });

        cancelBtn.setOnAction(ev -> {
            isRunning.set(false); // Stop the loop
            modal.close();
        });

        modal.setOnCloseRequest(ev -> isRunning.set(false));

        modal.showAndWait();
        return result[0];
    }

    private byte[] toJpegBytes(BufferedImage image) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            ImageIO.write(image, "jpg", out);
            return out.toByteArray();
        } catch (IOException e) {
            return null;
        }
    }

    private void closeWebcam(Webcam webcam) {
        try {
            if (webcam != null && webcam.isOpen()) webcam.close();
        } catch (Exception e) {
            System.out.println("[WARN] Close webcam error: " + e.getMessage());
        }
    }
}


