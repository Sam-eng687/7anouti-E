package projet.hanouti.wejden.utils;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import projet.hanouti.wejden.entities.CampagneMarketing;
import projet.hanouti.wejden.entities.StatistiquesVentes;
import projet.hanouti.wejden.services.CampagneMarketingService;
import projet.hanouti.wejden.services.ConseilsIAService;
import projet.hanouti.wejden.services.StatistiquesVentesService;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

public class PDFExportManager {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyyMMdd_HHmm");
    
    // Variable pour stocker le mode thème
    private static boolean darkMode = true;
    
    // Services
    private static final StatistiquesVentesService statsService = new StatistiquesVentesService();
    private static final ConseilsIAService conseilsService = new ConseilsIAService();
    private static final CampagneMarketingService campagnesService = new CampagneMarketingService();
    
    /** Mettre à jour le mode thème */
    public static void setDarkMode(boolean dark) {
        darkMode = dark;
    }

    public static void exportStatistiques(Stage owner) {
        new Thread(() -> {
            try {
                List<StatistiquesVentes> stats = statsService.getData();
                double revenu = statsService.getTotalRevenu();
                int qte = statsService.getTotalVendu();
                double retour = statsService.getTauxRetourMoyen();
                int produits = stats.size();

                StringBuilder sb = new StringBuilder();
                sb.append("{\"revenu\":").append(revenu)
                  .append(",\"qte\":").append(qte)
                  .append(",\"produits\":").append(produits)
                  .append(",\"retour\":").append(retour)
                  .append(",\"produits_list\":[");
                for (int i = 0; i < stats.size(); i++) {
                    StatistiquesVentes s = stats.get(i);
                    if (i > 0) sb.append(",");
                    sb.append("{")
                      .append("\"reference\":\"").append(esc(s.getProduitId())).append("\",")
                      .append("\"periode\":\"").append(esc(s.getPeriode())).append("\",")
                      .append("\"semaine\":\"").append(s.getSemaine()).append("\",")
                      .append("\"quantite_vendue\":").append(s.getTotalVendu()).append(",")
                      .append("\"revenu\":").append(s.getRevenuTotal()).append(",")
                      .append("\"taux_retour\":").append(s.getTauxRetour()).append(",")
                      .append("\"classement\":\"").append(esc(s.getClassement())).append("\"")
                      .append("}");
                }
                sb.append("]}");

                String outPath = getOutputPath("7anouti_stats");
                runPython("stats", sb.toString(), outPath);

                Platform.runLater(() -> openPDF(outPath, owner, "Statistiques exportées avec succès !"));
            } catch (Exception e) {
                Platform.runLater(() -> showError(owner, "Erreur export Stats: " + formatPdfExportError(e)));
            }
        }).start();
    }

    public static void exportCampagnes(Stage owner) {
        new Thread(() -> {
            try {
                List<CampagneMarketing> camps = campagnesService.getData();
                double budget = campagnesService.getBudgetTotal();
                double depense = campagnesService.getDepenseTotal();
                int actives = (int) campagnesService.countByStatut("ACTIVE");

                StringBuilder sb = new StringBuilder();
                sb.append("{\"budget\":").append(budget)
                  .append(",\"depense\":").append(depense)
                  .append(",\"actives\":").append(actives)
                  .append(",\"total\":").append(camps.size())
                  .append(",\"list\":[");
                for (int i = 0; i < camps.size(); i++) {
                    CampagneMarketing c = camps.get(i);
                    if (i > 0) sb.append(",");
                    sb.append("{")
                      .append("\"nom\":\"").append(esc(c.getNomCampagne())).append("\",")
                      .append("\"statut\":\"").append(esc(c.getStatut())).append("\",")
                      .append("\"objectif\":\"").append(esc(c.getObjectif())).append("\",")
                      .append("\"canal\":\"").append(esc(c.getCanal())).append("\",")
                      .append("\"budget_alloue\":").append(c.getBudget()).append(",")
                      .append("\"budget_depense\":").append(c.getDepense()).append(",")
                      .append("\"ia_score\":").append(c.getIaScore()).append(",")
                      .append("\"ia_conseil\":\"").append(esc(c.getIaConseil())).append("\",")
                      .append("\"date_debut\":\"").append(c.getDateDebut()).append("\",")
                      .append("\"date_fin\":\"").append(c.getDateFin()).append("\"")
                      .append("}");
                }
                sb.append("]}");

                String outPath = getOutputPath("7anouti_campagnes");
                runPython("campagnes", sb.toString(), outPath);

                Platform.runLater(() -> openPDF(outPath, owner, "Campagnes exportées avec succès !"));
            } catch (Exception e) {
                Platform.runLater(() -> showError(owner, "Erreur export Campagnes: " + formatPdfExportError(e)));
            }
        }).start();
    }

    // ══════════════════════════════════════════
    // EXPORT RAPPORT COMPLET (tous modules)
    // ══════════════════════════════════════════
    /** Demande un emplacement d'enregistrement puis génère le PDF (téléchargement explicite). */
    public static void exportRapportComplet(javafx.stage.Stage owner) {
        Platform.runLater(() -> {
            FileChooser fc = new FileChooser();
            fc.setTitle("Enregistrer le rapport PDF");
            fc.setInitialFileName("7anouti_rapport_" + LocalDateTime.now().format(FMT) + ".pdf");
            fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Fichiers PDF", "*.pdf"));
            File dest = fc.showSaveDialog(owner);
            if (dest == null) {
                return;
            }
            File out;
            if (dest.getName().toLowerCase().endsWith(".pdf")) {
                out = dest;
            } else {
                File par = dest.getParentFile();
                out = par != null ? new File(par, dest.getName() + ".pdf") : new File(dest.getAbsolutePath() + ".pdf");
            }
            new Thread(() -> {
                try {
                    String json = buildJsonRapportComplet();
                    runPython("complet", json, out.getAbsolutePath());
                    Platform.runLater(() -> openPDF(out.getAbsolutePath(), owner,
                            "Rapport PDF enregistré sur votre machine.", false));
                } catch (Exception e) {
                    Platform.runLater(() ->
                            showError(owner, "Erreur export Rapport Complet: " + formatPdfExportError(e)));
                }
            }).start();
        });
    }

    private static String buildJsonRapportComplet() throws Exception {
        List<StatistiquesVentes> stats = statsService.getData();
        double revenu = statsService.getTotalRevenu();
        int qte = statsService.getTotalVendu();
        double retour = statsService.getTauxRetourMoyen();
        int produits = stats.size();

        StringBuilder sbStats = new StringBuilder();
        sbStats.append("{\"revenu\":").append(revenu)
                .append(",\"qte\":").append(qte)
                .append(",\"produits\":").append(produits)
                .append(",\"retour\":").append(retour)
                .append(",\"produits_list\":[");
        for (int i = 0; i < stats.size(); i++) {
            StatistiquesVentes s = stats.get(i);
            if (i > 0) sbStats.append(",");
            sbStats.append("{")
                    .append("\"reference\":\"").append(esc(s.getProduitId())).append("\",")
                    .append("\"periode\":\"").append(esc(s.getPeriode())).append("\",")
                    .append("\"semaine\":\"").append(s.getSemaine()).append("\",")
                    .append("\"quantite_vendue\":").append(s.getTotalVendu()).append(",")
                    .append("\"revenu\":").append(s.getRevenuTotal()).append(",")
                    .append("\"taux_retour\":").append(s.getTauxRetour()).append(",")
                    .append("\"classement\":\"").append(esc(s.getClassement())).append("\"")
                    .append("}");
        }
        sbStats.append("]}");

        List<CampagneMarketing> camps = campagnesService.getData();
        double budget = campagnesService.getBudgetTotal();
        double depense = campagnesService.getDepenseTotal();
        int actives = (int) campagnesService.countByStatut("ACTIVE");

        StringBuilder sbCamps = new StringBuilder();
        sbCamps.append("{\"budget\":").append(budget)
                .append(",\"depense\":").append(depense)
                .append(",\"actives\":").append(actives)
                .append(",\"total\":").append(camps.size())
                .append(",\"list\":[");
        for (int i = 0; i < camps.size(); i++) {
            CampagneMarketing c = camps.get(i);
            if (i > 0) sbCamps.append(",");
            sbCamps.append("{")
                    .append("\"nom\":\"").append(esc(c.getNomCampagne())).append("\",")
                    .append("\"statut\":\"").append(esc(c.getStatut())).append("\",")
                    .append("\"objectif\":\"").append(esc(c.getObjectif())).append("\",")
                    .append("\"canal\":\"").append(esc(c.getCanal())).append("\",")
                    .append("\"budget_alloue\":").append(c.getBudget()).append(",")
                    .append("\"budget_depense\":").append(c.getDepense()).append(",")
                    .append("\"ia_score\":").append(c.getIaScore()).append(",")
                    .append("\"ia_conseil\":\"").append(esc(c.getIaConseil())).append("\",")
                    .append("\"date_debut\":\"").append(c.getDateDebut()).append("\",")
                    .append("\"date_fin\":\"").append(c.getDateFin()).append("\"")
                    .append("}");
        }
        sbCamps.append("]}");

        List<Map<String, Object>> conseilsList = conseilsService.getConseils();
        int totalConseils = conseilsList.size();
        int applique = (int) conseilsList.stream().filter(m -> "ACCEPTE".equals(m.get("etat"))).count();
        int nouveau = (int) conseilsList.stream().filter(m -> "EN_ATTENTE".equals(m.get("etat"))).count();

        StringBuilder sbConseils = new StringBuilder();
        sbConseils.append("{\"total\":").append(totalConseils)
                .append(",\"applique\":").append(applique)
                .append(",\"nouveau\":").append(nouveau)
                .append(",\"confiance\":85")
                .append(",\"list\":[");
        for (int i = 0; i < conseilsList.size(); i++) {
            Map<String, Object> con = conseilsList.get(i);
            if (i > 0) sbConseils.append(",");
            sbConseils.append("{")
                    .append("\"produit\":\"").append(esc(String.valueOf(con.getOrDefault("id_produit", "")))).append("\",")
                    .append("\"produit_nom\":\"").append(esc(String.valueOf(con.getOrDefault("id_produit", "")))).append("\",")
                    .append("\"type\":\"").append(esc(String.valueOf(con.getOrDefault("type", "")))).append("\",")
                    .append("\"urgence\":\"").append(esc(String.valueOf(con.getOrDefault("urgence", "Moyen")))).append("\",")
                    .append("\"description\":\"").append(esc(String.valueOf(con.getOrDefault("description", "")))).append("\",")
                    .append("\"score\":").append(con.getOrDefault("score", 0))
                    .append("}");
        }
        sbConseils.append("]}");

        return "{\"stats\":" + sbStats + ",\"conseils\":" + sbConseils + ",\"campagnes\":" + sbCamps + "}";
    }

    private static void runPython(String mode, String jsonData, String outPath) throws Exception {
        File tmpJson = File.createTempFile("7anouti_", ".json");
        tmpJson.deleteOnExit();
        Files.writeString(tmpJson.toPath(), jsonData, java.nio.charset.StandardCharsets.UTF_8);

        // Résoudre pdf_export.py (classpath ressources-wejden, puis emplacements legacy)
        File script = resolvePdfExportScript();
        File workDir = new File(System.getProperty("user.dir"));

        System.out.println("[PDF] Script: " + script.getAbsolutePath());
        System.out.println("[PDF] Output: " + outPath);

        // Essayer python, python3, py dans l'ordre
        String[] pythonCmds = {"python", "python3", "py"};
        Exception lastEx = null;

        for (String pyCmd : pythonCmds) {
            try {
                ProcessBuilder pb = new ProcessBuilder(
                    pyCmd, script.getAbsolutePath(),
                    "--mode", mode,
                    "--data", tmpJson.getAbsolutePath(),
                    "--output", outPath
                );
                pb.directory(workDir);
                pb.redirectErrorStream(true);
                Process proc = pb.start();
                String output = new String(proc.getInputStream().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
                int exit = proc.waitFor();
                System.out.println("[PDF] Cmd=" + pyCmd + " Exit=" + exit + " Output=" + output);
                if (exit == 0) return; // Succès
                lastEx = new Exception("Python error (" + pyCmd + "): " + output);
            } catch (Exception e) {
                lastEx = e;
                System.out.println("[PDF] " + pyCmd + " non trouve: " + e.getMessage());
            }
        }
        throw lastEx != null ? lastEx : new Exception("Python introuvable");
    }

    /**
     * Script embarqué : {@code /ressources-wejden/scripts/pdf_export.py} (JAR ou IDE).
     * Secours : {@code pdf_export.py} à la racine du répertoire de travail (legacy Premium).
     */
    private static File resolvePdfExportScript() throws IOException {
        URL url = PDFExportManager.class.getResource("/ressources-wejden/scripts/pdf_export.py");
        if (url != null) {
            if ("file".equalsIgnoreCase(url.getProtocol())) {
                try {
                    File f = new File(url.toURI());
                    if (f.isFile()) {
                        return f;
                    }
                } catch (URISyntaxException ignored) {
                    // copie via flux
                }
            }
            Path tmp = Files.createTempFile("pdf_export_", ".py");
            tmp.toFile().deleteOnExit();
            try (InputStream in = url.openStream()) {
                Files.copy(in, tmp, StandardCopyOption.REPLACE_EXISTING);
            }
            return tmp.toFile();
        }
        File cwd = new File(System.getProperty("user.dir"));
        File legacy = new File(cwd, "pdf_export.py");
        if (legacy.isFile()) {
            return legacy;
        }
        File parent = cwd.getParentFile();
        if (parent != null) {
            File legacyParent = new File(parent, "pdf_export.py");
            if (legacyParent.isFile()) {
                return legacyParent;
            }
        }
        throw new IOException(
                "pdf_export.py introuvable. Attendu sur le classpath : /ressources-wejden/scripts/pdf_export.py");
    }

    private static void openPDF(String path, Stage owner, String msg) {
        openPDF(path, owner, msg, true);
    }

    private static void openPDF(String path, Stage owner, String msg, boolean autoOpenPdf) {
        // ── Style 1 : Checkmark Explosion Premium ──
        Stage dialog = new Stage();
        dialog.initOwner(owner);
        dialog.initModality(javafx.stage.Modality.APPLICATION_MODAL);
        dialog.initStyle(javafx.stage.StageStyle.UNDECORATED);

        // Nom du fichier
        String fileName = new File(path).getName();

        // Couleurs adaptatives pour le dialogue PDF (pastel jaune/orange en mode clair)
        String overlayBg = darkMode ? "#0a0f1e" : "#fffbeb";
        String ringColor = darkMode ? "#38bdf8" : "#f59e0b";
        String ring3Fill = darkMode ? "#0d2137" : "#fef3c7";
        String checkColor = darkMode ? "#38bdf8" : "#d97706";
        String titleColor = darkMode ? "#f1f5f9" : "#78350f";
        String fileColor = darkMode ? "#475569" : "#92400e";
        String btnOpenBg = darkMode ? "linear-gradient(to right,#2563eb,#38bdf8)" : "linear-gradient(to right,#f59e0b,#fbbf24)";
        String btnCloseBg = darkMode ? "#1a2035" : "#fef3c7";
        String btnCloseText = darkMode ? "#64748b" : "#92400e";
        String btnCloseBorder = darkMode ? "#1e2a40" : "#fde68a";
        String dialogBorder = darkMode ? "#38bdf8" : "#f59e0b";

        javafx.scene.layout.StackPane overlay = new javafx.scene.layout.StackPane();
        overlay.setStyle("-fx-background-color:" + overlayBg + ";-fx-background-radius:18;");
        overlay.setPrefSize(460, 340);

        // ── Anneaux pulsants (3 cercles) ──
        javafx.scene.shape.Circle ring1 = new javafx.scene.shape.Circle(80);
        ring1.setFill(javafx.scene.paint.Color.TRANSPARENT);
        ring1.setStroke(javafx.scene.paint.Color.web(ringColor, 0.15));
        ring1.setStrokeWidth(2);

        javafx.scene.shape.Circle ring2 = new javafx.scene.shape.Circle(60);
        ring2.setFill(javafx.scene.paint.Color.TRANSPARENT);
        ring2.setStroke(javafx.scene.paint.Color.web(ringColor, 0.25));
        ring2.setStrokeWidth(2);

        javafx.scene.shape.Circle ring3 = new javafx.scene.shape.Circle(42);
        ring3.setFill(javafx.scene.paint.Color.web(ring3Fill));
        ring3.setStroke(javafx.scene.paint.Color.web(ringColor, 0.6));
        ring3.setStrokeWidth(2);

        // Checkmark ✓
        javafx.scene.control.Label check = new javafx.scene.control.Label("✓");
        check.setStyle("-fx-text-fill:" + checkColor + ";-fx-font-size:32px;-fx-font-weight:900;");
        check.setTranslateY(-60);

        // Texte sous le cercle
        javafx.scene.layout.VBox textBox = new javafx.scene.layout.VBox(6);
        textBox.setAlignment(javafx.geometry.Pos.CENTER);
        textBox.setTranslateY(60);

        javafx.scene.control.Label titleLbl = new javafx.scene.control.Label("Fichier généré");
        titleLbl.setStyle("-fx-text-fill:" + titleColor + ";-fx-font-size:18px;-fx-font-weight:bold;");

        String locLine = autoOpenPdf ? "Emplacement par défaut (ouverture auto)" : "Enregistré ici :";
        File parentDir = new File(path).getParentFile();
        if (!autoOpenPdf && parentDir != null) {
            String ap = parentDir.getAbsolutePath();
            locLine = ap.length() > 56 ? ("…" + ap.substring(ap.length() - 54)) : ap;
        }
        javafx.scene.control.Label fileLbl = new javafx.scene.control.Label(fileName + "\n" + locLine);
        fileLbl.setWrapText(true);
        fileLbl.setStyle("-fx-text-fill:" + fileColor + ";-fx-font-size:11px;-fx-text-alignment:center;");

        javafx.scene.control.Label msgLbl = new javafx.scene.control.Label(msg);
        msgLbl.setWrapText(true);
        msgLbl.setMaxWidth(400);
        msgLbl.setStyle("-fx-text-fill:" + titleColor + ";-fx-font-size:12px;-fx-opacity:0.92;");

        textBox.getChildren().addAll(titleLbl, msgLbl, fileLbl);

        // Boutons
        javafx.scene.layout.HBox btnRow = new javafx.scene.layout.HBox(12);
        btnRow.setAlignment(javafx.geometry.Pos.CENTER);
        btnRow.setTranslateY(120);

        javafx.scene.control.Button btnOuvrir = new javafx.scene.control.Button("OUVRIR");
        btnOuvrir.setPrefWidth(160); btnOuvrir.setPrefHeight(42);
        btnOuvrir.setStyle(
            "-fx-background-color:" + btnOpenBg + ";" +
            "-fx-text-fill:white;-fx-font-weight:bold;-fx-font-size:13px;" +
            "-fx-background-radius:10;-fx-cursor:hand;-fx-letter-spacing:2px;");

        javafx.scene.control.Button btnFermer = new javafx.scene.control.Button("FERMER");
        btnFermer.setPrefWidth(160); btnFermer.setPrefHeight(42);
        btnFermer.setStyle(
            "-fx-background-color:" + btnCloseBg + ";-fx-text-fill:" + btnCloseText + ";-fx-font-size:13px;" +
            "-fx-background-radius:10;-fx-cursor:hand;-fx-letter-spacing:2px;" +
            "-fx-border-color:" + btnCloseBorder + ";-fx-border-width:1;-fx-border-radius:10;");

        btnOuvrir.setOnAction(e -> {
            dialog.close();
            try { java.awt.Desktop.getDesktop().open(new File(path)); } catch (Exception ignored) {}
        });
        btnFermer.setOnAction(e -> dialog.close());
        btnRow.getChildren().addAll(btnOuvrir, btnFermer);

        overlay.getChildren().addAll(ring1, ring2, ring3, check, textBox, btnRow);

        // ── Animations Checkmark Explosion ──
        // Cercles : scale 0 → 1 avec rebond
        ring1.setScaleX(0); ring1.setScaleY(0);
        ring2.setScaleX(0); ring2.setScaleY(0);
        ring3.setScaleX(0); ring3.setScaleY(0);
        check.setOpacity(0); check.setScaleX(0.3); check.setScaleY(0.3);
        textBox.setOpacity(0); btnRow.setOpacity(0);

        javafx.animation.ScaleTransition s1 = new javafx.animation.ScaleTransition(javafx.util.Duration.millis(500), ring1);
        s1.setToX(1); s1.setToY(1); s1.setInterpolator(javafx.animation.Interpolator.EASE_OUT);

        javafx.animation.ScaleTransition s2 = new javafx.animation.ScaleTransition(javafx.util.Duration.millis(450), ring2);
        s2.setToX(1); s2.setToY(1); s2.setDelay(javafx.util.Duration.millis(80));
        s2.setInterpolator(javafx.animation.Interpolator.EASE_OUT);

        javafx.animation.ScaleTransition s3 = new javafx.animation.ScaleTransition(javafx.util.Duration.millis(400), ring3);
        s3.setToX(1); s3.setToY(1); s3.setDelay(javafx.util.Duration.millis(160));
        s3.setInterpolator(javafx.animation.Interpolator.EASE_OUT);

        // Checkmark pop
        javafx.animation.ScaleTransition sc = new javafx.animation.ScaleTransition(javafx.util.Duration.millis(350), check);
        sc.setToX(1); sc.setToY(1); sc.setDelay(javafx.util.Duration.millis(300));
        sc.setInterpolator(javafx.animation.Interpolator.EASE_OUT);
        javafx.animation.FadeTransition fc = new javafx.animation.FadeTransition(javafx.util.Duration.millis(300), check);
        fc.setToValue(1); fc.setDelay(javafx.util.Duration.millis(300));

        // Texte + boutons fade in
        javafx.animation.FadeTransition ft = new javafx.animation.FadeTransition(javafx.util.Duration.millis(400), textBox);
        ft.setToValue(1); ft.setDelay(javafx.util.Duration.millis(550));
        javafx.animation.FadeTransition fb = new javafx.animation.FadeTransition(javafx.util.Duration.millis(400), btnRow);
        fb.setToValue(1); fb.setDelay(javafx.util.Duration.millis(650));

        // Pulse continu sur ring1
        javafx.animation.ScaleTransition pulse = new javafx.animation.ScaleTransition(javafx.util.Duration.seconds(1.5), ring1);
        pulse.setFromX(1); pulse.setFromY(1); pulse.setToX(1.08); pulse.setToY(1.08);
        pulse.setAutoReverse(true); pulse.setCycleCount(javafx.animation.Animation.INDEFINITE);
        pulse.setDelay(javafx.util.Duration.millis(600));

        new javafx.animation.ParallelTransition(s1, s2, s3, sc, fc, ft, fb).play();
        pulse.play();

        // Contour néon du dialogue
        javafx.scene.layout.VBox root = new javafx.scene.layout.VBox();
        root.setStyle("-fx-background-color:" + overlayBg + ";-fx-border-color:" + dialogBorder + ";" +
                      "-fx-border-width:2;-fx-border-radius:18;-fx-background-radius:18;");
        root.getChildren().add(overlay);

        javafx.scene.Scene scene = new javafx.scene.Scene(root);
        scene.setFill(javafx.scene.paint.Color.TRANSPARENT);
        dialog.setScene(scene);

        // Ouvrir automatiquement le PDF (exports stats / campagnes uniquement)
        if (autoOpenPdf) {
            javafx.animation.PauseTransition autoOpen = new javafx.animation.PauseTransition(javafx.util.Duration.seconds(1.5));
            autoOpen.setOnFinished(e -> {
                try { java.awt.Desktop.getDesktop().open(new File(path)); } catch (Exception ignored) {}
            });
            dialog.setOnShown(ev -> autoOpen.play());
        } else {
            dialog.setOnShown(null);
        }
        dialog.showAndWait();
    }

    private static String getOutputPath(String name) {
        String ts = LocalDateTime.now().format(FMT);
        String home = System.getProperty("user.home");

        // Ordre de priorité : OneDrive\Desktop (Windows FR/EN), Desktop, Bureau, Documents
        String[] candidates = {
            home + File.separator + "OneDrive" + File.separator + "Desktop",
            home + File.separator + "OneDrive" + File.separator + "Bureau",
            home + File.separator + "Desktop",
            home + File.separator + "Bureau",
            home + File.separator + "Documents",
            home
        };

        String dir = home;
        for (String candidate : candidates) {
            File f = new File(candidate);
            if (f.exists() && f.isDirectory()) {
                dir = candidate;
                break;
            }
        }

        System.out.println("[PDF] Dossier sortie : " + dir);
        return dir + File.separator + name + "_" + ts + ".pdf";
    }

    private static String esc(String s) {
        if (s == null) return "";
        return s.replace("\"", "\\\"").replace("\n", " ");
    }

    private static void showError(Stage owner, String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK);
        a.initOwner(owner);
        a.setHeaderText(null);
        a.getDialogPane().setPrefWidth(520);
        a.showAndWait();
    }

    /** Message lisible quand Python / reportlab manquent (évite seulement la stack brute). */
    private static String formatPdfExportError(Throwable e) {
        String raw = e.getMessage();
        if (raw == null) {
            raw = String.valueOf(e);
        }
        if (raw.contains("reportlab") || raw.contains("ModuleNotFoundError")) {
            return "Le module Python « reportlab » est requis pour générer le PDF.\n\n"
                    + "À la racine du projet 7anouti-E :\n"
                    + "    python -m pip install -r requirements_pdf.txt\n\n"
                    + "Puis réessayez l’export.\n\n— Détails —\n" + raw;
        }
        if (raw.contains("9009") || raw.toLowerCase().contains("python est introuvable")
                || raw.contains("Python introuvable")) {
            return "La commande « python » n’a pas été trouvée (PATH / alias Microsoft Store).\n\n"
                    + "Installez Python 3 depuis https://www.python.org/downloads/ "
                    + "ou désactivez l’alias Store (Paramètres → Applications → paramètres avancés).\n\n"
                    + "— Détails —\n" + raw;
        }
        return raw;
    }
}
