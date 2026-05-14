package projet.hanouti.wejden.gui.components;

import projet.hanouti.wejden.services.GeminiMarketingService;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Assistant boutique (Gemini) — fenetre chat lisible sur Windows (viewport ScrollPane + theme dedie).
 */
public final class ShopAssistantChat {

    private static final ExecutorService POOL = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "gemini-chat");
        t.setDaemon(true);
        return t;
    });

    private ShopAssistantChat() {
    }

    public static void show(Stage owner, GeminiMarketingService gemini, boolean darkMode, String accent) {
        // Palette dediee au chat (fort contraste ; ne depend pas du viewport blanc du ScrollPane)
        final String shell = darkMode ? "#070b14" : "#f1f5f9";
        final String headerBg = darkMode ? "#0f172a" : "#e2e8f0";
        final String bodyBg = darkMode ? "#0b1220" : "#ffffff";
        final String scrollTrack = darkMode ? "#0b1220" : "#f8fafc";
        final String fg = darkMode ? "#e2e8f0" : "#0f172a";
        final String fgMuted = darkMode ? "#94a3b8" : "#475569";
        final String bubbleUser = darkMode ? "#1d4ed8" : "#3b82f6";
        final String bubbleUserFg = "#f8fafc";
        final String bubbleBot = darkMode ? "#1e293b" : "#ffffff";
        final String bubbleBotFg = darkMode ? "#e2e8f0" : "#0f172a";
        final String border = darkMode ? "#334155" : "#cbd5e1";
        final String inputBg = darkMode ? "#111827" : "#ffffff";
        final String accentCol = accent != null && !accent.isBlank() ? accent : "#8b5cf6";

        Stage st = new Stage();
        if (owner != null)
            st.initOwner(owner);
        st.initModality(Modality.WINDOW_MODAL);
        st.setTitle("Assistant Hanouti");
        st.initStyle(StageStyle.DECORATED);

        // --- En-tete (toujours visible) ---
        Label headTitle = new Label("Assistant Hanouti");
        headTitle.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: " + fg + ";");
        Label headSub = new Label("Gemini · Conseils, campagnes, promos & e-commerce");
        headSub.setStyle("-fx-font-size: 11px; -fx-text-fill: " + fgMuted + ";");
        VBox headTexts = new VBox(4, headTitle, headSub);
        Region headSp = new Region();
        HBox.setHgrow(headSp, Priority.ALWAYS);
        Button headClose = new Button("\u2715");
        headClose.setStyle(
                "-fx-background-color: transparent; -fx-text-fill: " + fgMuted + "; -fx-font-size: 14px; -fx-cursor: hand;");
        headClose.setOnAction(e -> st.close());
        HBox headRow = new HBox(12, headTexts, headSp, headClose);
        JfxAlign.hbox(headRow, "CENTER_LEFT");
        headRow.setPadding(new Insets(14, 16, 12, 16));
        headRow.setStyle("-fx-background-color: " + headerBg + ";");

        Separator sep1 = new Separator();
        sep1.setStyle("-fx-opacity: 0.35;");

        VBox messages = new VBox(12);
        messages.setPadding(new Insets(14, 12, 14, 12));
        messages.setFillWidth(true);
        messages.setStyle("-fx-background-color: " + bodyBg + ";");

        StackPane scrollBacking = new StackPane(messages);
        scrollBacking.setStyle("-fx-background-color: " + bodyBg + ";");
        JfxAlign.stackChild(messages, "TOP_CENTER");

        ScrollPane scroll = new ScrollPane(scrollBacking);
        scroll.setFitToWidth(true);
        scroll.setPannable(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroll.setMinViewportHeight(260);
        scroll.setPrefViewportHeight(320);
        scroll.setStyle("-fx-background-color: " + scrollTrack + "; -fx-border-color: transparent;");
        scroll.viewportBoundsProperty().addListener((obs, oldV, newV) -> {
            if (newV != null && newV.getHeight() > 0)
                scrollBacking.setMinHeight(newV.getHeight());
        });

        VBox.setVgrow(scroll, Priority.ALWAYS);

        appendBot(messages, scroll, bubbleBot, border, bubbleBotFg,
                "Salut ! Je peux t'aider sur 7anouti Marketing : conseils, campagnes, promos, stats, idees e-commerce. Pose ta question.");

        TextArea input = new TextArea();
        input.setPromptText("Ecris ton message...");
        input.setWrapText(true);
        input.setPrefRowCount(3);
        input.setMinHeight(72);
        input.setStyle("-fx-control-inner-background: " + inputBg + "; -fx-background-color: " + inputBg + ";"
                + "-fx-text-fill: " + fg + "; -fx-font-size: 13px; -fx-prompt-text-fill: " + fgMuted + ";"
                + "-fx-background-radius: 12; -fx-border-color: " + border + "; -fx-border-radius: 12; -fx-border-width: 1;"
                + "-fx-highlight-fill: " + accentCol + "; -fx-highlight-text-fill: white;");

        Label status = new Label(" ");
        status.setMinHeight(18);
        status.setStyle("-fx-text-fill: " + fgMuted + "; -fx-font-size: 11px;");

        Button send = new Button("Envoyer");
        send.setDefaultButton(true);
        send.setStyle("-fx-background-color: " + accentCol + "; -fx-text-fill: white; -fx-font-weight: bold;"
                + "-fx-padding: 10 24; -fx-background-radius: 10; -fx-cursor: hand;");

        Button close = new Button("Fermer");
        close.setStyle("-fx-background-color: " + (darkMode ? "#334155" : "#e2e8f0") + "; -fx-text-fill: " + fg + ";"
                + "-fx-padding: 10 18; -fx-background-radius: 10; -fx-cursor: hand;");

        Region grow = new Region();
        HBox.setHgrow(grow, Priority.ALWAYS);
        HBox btnRow = new HBox(10, close, grow, send);
        JfxAlign.hbox(btnRow, "CENTER_RIGHT");
        btnRow.setPadding(new Insets(4, 4, 12, 4));

        Separator sep2 = new Separator();
        sep2.setStyle("-fx-opacity: 0.25;");

        VBox root = new VBox(0, headRow, sep1, scroll, sep2, input, status, btnRow);
        root.setStyle("-fx-background-color: " + shell + ";");
        root.setPadding(new Insets(0, 12, 8, 12));
        root.setMinWidth(400);
        root.setMinHeight(500);
        root.setPrefWidth(440);
        root.setPrefHeight(560);

        Scene sc = new Scene(root);
        sc.setFill(Color.web(shell));

        Runnable doSend = () -> {
            String q = input.getText() != null ? input.getText().trim() : "";
            if (q.isEmpty())
                return;
            if (!gemini.hasApiKey()) {
                status.setText("Cle API absente ou invalide.");
                return;
            }
            input.clear();
            appendUser(messages, scroll, q, bubbleUser, border, bubbleUserFg);
            status.setText("En cours...");
            send.setDisable(true);
            POOL.submit(() -> {
                try {
                    String ctx = buildContext(messages);
                    String reply = gemini.chatShopAssistant(ctx);
                    Platform.runLater(() -> {
                        appendBot(messages, scroll, bubbleBot, border, bubbleBotFg, reply);
                        status.setText(" ");
                        send.setDisable(false);
                    });
                } catch (Exception ex) {
                    Platform.runLater(() -> {
                        appendBot(messages, scroll, bubbleBot, border, "#fca5a5",
                                "Erreur : " + (ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName()));
                        status.setText(" ");
                        send.setDisable(false);
                    });
                }
            });
        };

        send.setOnAction(e -> doSend.run());
        close.setOnAction(e -> st.close());

        st.setScene(sc);
        st.setMinWidth(400);
        st.setMinHeight(480);
        st.sizeToScene();
        st.show();
        Platform.runLater(() -> {
            root.requestLayout();
            scrollToBottom(scroll);
        });
    }

    private static String buildContext(VBox messages) {
        StringBuilder sb = new StringBuilder();
        for (javafx.scene.Node n : messages.getChildren()) {
            if (n instanceof HBox h && h.getUserData() instanceof String tag) {
                Label l = findLabel(h);
                if (l != null && l.getText() != null) {
                    if ("user".equals(tag))
                        sb.append("Utilisateur: ").append(l.getText()).append("\n");
                    else if ("bot".equals(tag))
                        sb.append("Assistant: ").append(l.getText()).append("\n");
                }
            }
        }
        String s = sb.toString();
        if (s.length() > 12000)
            s = s.substring(s.length() - 12000);
        return s;
    }

    private static Label findLabel(HBox h) {
        for (javafx.scene.Node c : h.getChildren()) {
            if (c instanceof Label lb)
                return lb;
            if (c instanceof javafx.scene.layout.Pane p) {
                for (javafx.scene.Node q : p.getChildren())
                    if (q instanceof Label lb)
                        return lb;
            }
        }
        return null;
    }

    private static void appendUser(VBox messages, ScrollPane scroll, String text, String bg, String border, String fg) {
        Label lb = new Label(text);
        lb.setWrapText(true);
        lb.setMaxWidth(300);
        lb.setStyle("-fx-text-fill: " + fg + "; -fx-font-size: 13px; -fx-padding: 10 14;");
        VBox wrap = new VBox(lb);
        wrap.setStyle("-fx-background-color: " + bg + "; -fx-background-radius: 14 14 4 14;"
                + "-fx-border-color: " + border + "; -fx-border-radius: 14 14 4 14; -fx-border-width: 1;");
        HBox row = new HBox();
        JfxAlign.hbox(row, "CENTER_RIGHT");
        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);
        row.getChildren().addAll(sp, wrap);
        row.setUserData("user");
        messages.getChildren().add(row);
        scrollToBottom(scroll);
    }

    private static void appendBot(VBox messages, ScrollPane scroll, String bg, String border, String fg, String text) {
        Label lb = new Label(text);
        lb.setWrapText(true);
        lb.setMaxWidth(340);
        lb.setStyle("-fx-text-fill: " + fg + "; -fx-font-size: 13px; -fx-padding: 10 14; -fx-line-spacing: 2;");
        VBox wrap = new VBox(lb);
        wrap.setStyle("-fx-background-color: " + bg + "; -fx-background-radius: 14 14 14 4;"
                + "-fx-border-color: " + border + "; -fx-border-radius: 14 14 14 4; -fx-border-width: 1;");
        HBox row = new HBox(wrap);
        JfxAlign.hbox(row, "CENTER_LEFT");
        row.setUserData("bot");
        messages.getChildren().add(row);
        scrollToBottom(scroll);
    }

    private static void scrollToBottom(ScrollPane scroll) {
        Platform.runLater(() -> scroll.setVvalue(1.0));
    }
}
