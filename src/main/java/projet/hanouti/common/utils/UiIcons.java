package projet.hanouti.common.utils;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;

public final class UiIcons {
    public enum Icon {
        BELL,
        CART,
        CAMERA,
        MIC,
        SEND,
        STOP,
        HEART,
        HEART_OUTLINE,
        STAR,
        PILL,
        BASKET,
        LAPTOP,
        BOTTLE,
        HOME,
        TAG,
        PACKAGE,
        BAG,
        FOLDER
    }

    private UiIcons() {}

    public static StackPane icon(Icon icon, String color, double size) {
        SVGPath path = new SVGPath();
        path.setContent(path(icon));
        path.setFill(Color.web(color));
        path.setScaleX(size / 24.0);
        path.setScaleY(size / 24.0);

        StackPane box = new StackPane(path);
        box.setAlignment(Pos.CENTER);
        box.setMinSize(size, size);
        box.setPrefSize(size, size);
        box.setMaxSize(size, size);
        return box;
    }

    public static void setButtonIcon(Button button, Icon icon, String color, double size, String tooltip) {
        if (button == null) return;
        button.setText("");
        button.setGraphic(icon(icon, color, size));
        button.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        if (tooltip != null && !tooltip.isBlank()) {
            button.setTooltip(new Tooltip(tooltip));
        }
    }

    private static String path(Icon icon) {
        return switch (icon) {
            case BELL -> "M12 22a2.5 2.5 0 0 0 2.45-2h-4.9A2.5 2.5 0 0 0 12 22M18 16v-5.2A6.1 6.1 0 0 0 13 4.1V3a1 1 0 0 0-2 0v1.1a6.1 6.1 0 0 0-5 6.7V16l-1.7 2h15.4z";
            case CART -> "M7 18a2 2 0 1 0 0 4 2 2 0 0 0 0-4m10 0a2 2 0 1 0 0 4 2 2 0 0 0 0-4M3 4h2.2l2.1 9.6A3 3 0 0 0 10.2 16H18a1 1 0 0 0 0-2h-7.8a1 1 0 0 1-1-.8L9 12h7.8a3 3 0 0 0 2.9-2.3L21 5H7.1L6.7 3H3z";
            case CAMERA -> "M8.2 5 10 3h4l1.8 2H19a3 3 0 0 1 3 3v8a3 3 0 0 1-3 3H5a3 3 0 0 1-3-3V8a3 3 0 0 1 3-3zm3.8 3.5a4.5 4.5 0 1 0 0 9 4.5 4.5 0 0 0 0-9m0 2a2.5 2.5 0 1 1 0 5 2.5 2.5 0 0 1 0-5";
            case MIC -> "M12 15a3 3 0 0 0 3-3V5a3 3 0 0 0-6 0v7a3 3 0 0 0 3 3m6-4a1 1 0 0 0-2 0 4 4 0 0 1-8 0 1 1 0 0 0-2 0 6 6 0 0 0 5 5.9V20H8a1 1 0 1 0 0 2h8a1 1 0 1 0 0-2h-3v-3.1A6 6 0 0 0 18 11";
            case SEND -> "M3 20.5 22 12 3 3.5V10l12 2-12 2z";
            case STOP -> "M7 7h10v10H7z";
            case HEART -> "M12 21s-7.5-4.7-9.5-9.1C.8 8.2 3.1 5 6.6 5c2 0 3.4 1.1 4.2 2.2C11.6 6.1 13 5 15 5c3.5 0 5.8 3.2 4.1 6.9C19.5 16.3 12 21 12 21";
            case HEART_OUTLINE -> "M12 21s-7.5-4.7-9.5-9.1C.8 8.2 3.1 5 6.6 5c2 0 3.4 1.1 4.2 2.2C11.6 6.1 13 5 15 5c3.5 0 5.8 3.2 4.1 6.9C19.5 16.3 12 21 12 21M6.8 7C4.7 7 3.4 9 4.4 11.1 5.7 14 10 17.2 12 18.6c2-1.4 6.3-4.6 7.6-7.5C20.6 9 19.3 7 17.2 7c-1.7 0-2.8 1.3-3.5 2.5h-3.4C9.6 8.3 8.5 7 6.8 7";
            case STAR -> "M12 2 15 8.6l7 .7-5.2 4.7 1.6 6.9L12 17.3 5.6 21l1.6-6.9L2 9.3l7-.7z";
            case PILL -> "M10.4 21.2a6 6 0 0 1-8.5-8.5l6.8-6.8a6 6 0 0 1 8.5 8.5zm1.4-1.4 4-4-8.5-8.5-4 4a6 6 0 0 0 8.5 8.5";
            case BASKET -> "M7 9 10 4h4l3 5h3l-2.2 10H6.2L4 9zm2.3 2 .8 6h1.8v-6zm4.8 0v6h1.8l.8-6z";
            case LAPTOP -> "M4 5h16v10H4zm2 2v6h12V7zM2 17h20v2H2z";
            case BOTTLE -> "M9 2h6v3l-1.2 1.4V9l2.2 2v9a2 2 0 0 1-2 2h-4a2 2 0 0 1-2-2v-9l2.2-2V6.4L9 5z";
            case HOME -> "M3 11 12 3l9 8v10h-6v-6H9v6H3z";
            case TAG -> "M3 4h9l9 9-8 8-9-9zm5 5a2 2 0 1 0 0-4 2 2 0 0 0 0 4";
            case PACKAGE -> "M3 7.5 12 3l9 4.5V17l-9 4-9-4zm2.4.7L12 11l6.6-2.8L12 5.4zm5.6 5.1-6-2.5v5l6 2.7zm2 5.2 6-2.7v-5l-6 2.5z";
            case BAG -> "M6 8h12l1 13H5zm3 0a3 3 0 0 1 6 0h-2a1 1 0 0 0-2 0z";
            case FOLDER -> "M3 6h7l2 2h9v10a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z";
        };
    }
}
