package projet.hanouti.common.utils;

import com.github.sarxos.webcam.Webcam;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;

public class CameraCapture {

    public static String capturePhoto() throws Exception {
        Webcam webcam = Webcam.getDefault();

        if (webcam == null) {
            throw new Exception("Aucune webcam détectée.");
        }

        webcam.open();

        BufferedImage image = webcam.getImage();

        String folderPath = System.getProperty("java.io.tmpdir") + File.separator + "7anouti_security";
        File folder = new File(folderPath);

        if (!folder.exists()) {
            folder.mkdirs();
        }

        String filePath = folderPath + File.separator + "login_attempt_" + System.currentTimeMillis() + ".png";
        File file = new File(filePath);

        ImageIO.write(image, "PNG", file);

        webcam.close();

        return filePath;
    }
}