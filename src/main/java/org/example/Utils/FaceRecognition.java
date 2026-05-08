package org.example.Utils;

import nu.pattern.OpenCV;
import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

public class FaceRecognition {

    static {
        OpenCV.loadLocally();
    }

    public static boolean recognize(String img1Path, String img2Path) {

        Mat img1 = Imgcodecs.imread(img1Path);
        Mat img2 = Imgcodecs.imread(img2Path);

        if (img1.empty() || img2.empty()) {
            System.out.println("Images invalides");
            return false;
        }

        // grayscale
        Imgproc.cvtColor(img1, img1, Imgproc.COLOR_BGR2GRAY);
        Imgproc.cvtColor(img2, img2, Imgproc.COLOR_BGR2GRAY);

        // resize
        Imgproc.resize(img1, img1, new Size(200, 200));
        Imgproc.resize(img2, img2, new Size(200, 200));

        // difference
        Mat diff = new Mat();
        Core.absdiff(img1, img2, diff);

        double score = Core.sumElems(diff).val[0];

        System.out.println("Score: " + score);

        return score < 900000; // threshold
    }
}