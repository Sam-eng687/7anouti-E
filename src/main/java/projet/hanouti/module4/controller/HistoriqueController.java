package com.hanouti.hanoutiem4.controller;

import com.hanouti.hanoutiem4.UserSession;
import com.hanouti.hanoutiem4.dao.PaiementDAO;
import com.hanouti.hanoutiem4.model.Paiement;
import javafx.animation.*;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.stage.FileChooser;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpExchange;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;
import javafx.scene.image.ImageView;
import javafx.scene.effect.DropShadow;
import javafx.scene.shape.SVGPath;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;
import javafx.scene.paint.Color;
import java.nio.charset.StandardCharsets;
import java.io.IOException;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.text.NumberFormat;
import java.util.Locale;

public class HistoriqueController {

    /** French locale currency formatter: 1 670,00 DT */
    private static String fmtDT(double amount) {
        // French grouping: space as thousands separator, comma as decimal
        NumberFormat nf = NumberFormat.getNumberInstance(Locale.FRANCE);
        nf.setMinimumFractionDigits(2);
        nf.setMaximumFractionDigits(3);
        return nf.format(amount) + " DT";
    }

    /** Short version with exactly 2 decimals */
    private static String fmtDT2(double amount) {
        NumberFormat nf = NumberFormat.getNumberInstance(Locale.FRANCE);
        nf.setMinimumFractionDigits(2);
        nf.setMaximumFractionDigits(2);
        return nf.format(amount) + " DT";
    }

    // ── SVG path constants ─────────────────────────────────────────────────
    private static final String IC_CART =
            "M6 2L3 6v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2V6l-3-4z" +
                    "M3 6h18M16 10a4 4 0 0 1-8 0";
    private static final String IC_RECEIPT =
            "M14 2H6a2 2 0 0 0-2 2v16l4-2 4 2 4-2 4 2V4a2 2 0 0 0-2-2z" +
                    "M8 11h8M8 7h8M8 15h5";
    private static final String IC_MENU =
            "M3 6h18M3 12h18M3 18h18";
    private static final String IC_BELL =
            "M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9" +
                    "M13.73 21a2 2 0 0 1-3.46 0";
    private static final String IC_MOON =
            "M21 12.79A9 9 0 1 1 11.21 3 7 7 0 0 0 21 12.79z";
    private static final String IC_SUN =
            "M12 1v2M12 21v2M4.22 4.22l1.42 1.42M18.36 18.36l1.42 1.42" +
                    "M1 12h2M21 12h2M4.22 19.78l1.42-1.42M18.36 5.64l1.42-1.42" +
                    "M12 7a5 5 0 1 0 0 10A5 5 0 0 0 12 7z";
    private static final String IC_CHART =
            "M21.21 15.89A10 10 0 1 1 8 2.83M22 12A10 10 0 0 0 12 2v10z";
    private static final String IC_CARD =
            "M2 6a2 2 0 0 1 2-2h16a2 2 0 0 1 2 2v12a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V6zM2 10h20";
    private static final String IC_WALLET =
            "M3 7h18a2 2 0 0 1 2 2v10a2 2 0 0 1-2 2H3a2 2 0 0 1-2-2V9a2 2 0 0 1 2-2z" +
                    "M7 7V5a2 2 0 0 1 2-2h6a2 2 0 0 1 2 2v2M15 14a1 1 0 1 0 2 0 1 1 0 0 0-2 0z";
    private static final String IC_PHONE =
            "M22 16.92v3a2 2 0 0 1-2.18 2 19.79 19.79 0 0 1-8.63-3.07" +
                    "A19.5 19.5 0 0 1 4.69 13.5 19.79 19.79 0 0 1 1.62 4.85 2 2 0 0 1 3.62 2h3" +
                    "a2 2 0 0 1 2 1.72c.127.96.361 1.903.7 2.81a2 2 0 0 1-.45 2.11L7.91 9.64" +
                    "a16 16 0 0 0 6.29 6.29l.94-.94a2 2 0 0 1 2.11-.45c.907.339 1.85.573 2.81.7" +
                    "A2 2 0 0 1 22 16.92z";

    // ── FXML injections ───────────────────────────────────────────────────
    @FXML private AnchorPane rootPane;
    @FXML private HBox       headerBar;
    @FXML private Button     menuBtn;
    @FXML private StackPane  logoBoxPlaceholder;
    @FXML private Button     themeBtn;
    @FXML private Button     notifBellBtn;
    @FXML private Label      cartAmountLabel;
    @FXML private Button     cartCountBadge;

    // Page header
    @FXML private StackPane  pageIconBox;
    @FXML private Label      transactionsBadge;
    @FXML private Button     exportBtn;
    @FXML private Button     deleteAllBtn;
    @FXML private HBox       statsBar;
    @FXML private TextField  searchField;
    @FXML private HBox       searchBar;
    @FXML private HBox       filterBar;
    @FXML private HBox       sortBar;

    // Cards list
    @FXML private ScrollPane cardsScrollPane;
    @FXML private VBox       cardsContainer;

    // Sidebar
    @FXML private VBox       sidebarPanel;
    @FXML private StackPane  resumeIconBox;
    @FXML private Label      resumeTitle;
    @FXML private VBox       resumeRows;
    @FXML private StackPane  pmIconBox;
    @FXML private Label      moyensTitle;
    @FXML private StackPane  qrHintIconBox;

    // ── State ─────────────────────────────────────────────────────────────
    private String  currentFilter = "Tous";
    private String  currentSearch = "";
    private String  currentSort   = "DATE_DESC"; // DATE_DESC, DATE_ASC, AMT_DESC, AMT_ASC
    private boolean isDarkMode    = false;
    private PaiementDAO dao;
    private List<Paiement> allData;
    private int currentUserId;
    private com.hanouti.hanoutiem4.util.DrawerHelper drawerHelper;
    // Mini local HTTP server for QR scan
    private HttpServer activeQrServer;
    private static final int QR_SERVER_PORT = 7472;

    // ── Initialize ────────────────────────────────────────────────────────
    @FXML
    public void initialize() {
        currentUserId = UserSession.getInstance().getUserId();
        isDarkMode    = UserSession.getInstance().isDarkMode();
        applyDarkMode();

        try {
            dao = new PaiementDAO();
            loadData();
        } catch (SQLException e) {
            showAlert("Erreur", "Connexion base de données impossible.");
        }

        Platform.runLater(() -> {
            buildTopbarIcons();
            buildSidebarIcons();
            setupFilterTabs();
            setupSortBar();
            setupSearchListener();
            setupExportButton();
            setupDeleteAllButton();
            updateCartPill();

            drawerHelper = new com.hanouti.hanoutiem4.util.DrawerHelper(
                    rootPane, isDarkMode, "historique");
            drawerHelper.setThemeChangeCallback(() -> {
                isDarkMode = UserSession.getInstance().isDarkMode();
                applyDarkMode();
                setupFilterTabs();
                applyCurrentFilter();
                updateSidebar(allData != null ? allData : List.of());
            });
        });
    }

    // ── Build icons programmatically ──────────────────────────────────────

    private void buildTopbarIcons() {
        // Logo box
        if (logoBoxPlaceholder != null) {
            logoBoxPlaceholder.setStyle(
                    "-fx-background-color:linear-gradient(to bottom right,#4f46e5,#6366f1);" +
                            "-fx-background-radius:13;");
            DropShadow ds = new DropShadow();
            ds.setColor(Color.web("#4f46e5", 0.38)); ds.setRadius(10); ds.setOffsetY(3);
            logoBoxPlaceholder.setEffect(ds);
            logoBoxPlaceholder.getChildren().add(svgIcon(IC_CART, "#ffffff", 20));
        }
        // Hamburger
        if (menuBtn != null) {
            menuBtn.setGraphic(svgIcon(IC_MENU, "#4f46e5", 18));
            menuBtn.setText(null);
            menuBtn.getStyleClass().setAll("topbar-ham-btn");
            menuBtn.setOnAction(e -> { if (drawerHelper != null) drawerHelper.toggle(); });
        }
        // Theme button
        updateThemeIcon();
        if (themeBtn != null) {
            themeBtn.setOnAction(e -> {
                isDarkMode = !isDarkMode;
                UserSession.getInstance().setDarkMode(isDarkMode);
                if (isDarkMode) rootPane.getStyleClass().add("dark");
                else            rootPane.getStyleClass().remove("dark");
                updateThemeIcon();
                setupFilterTabs();
                applyCurrentFilter();
                buildSidebarIcons();
            });
        }
        // Bell
        if (notifBellBtn != null) {
            notifBellBtn.setGraphic(svgIcon(IC_BELL, "#4f46e5", 16));
            notifBellBtn.setText(null);
        }
        // Page receipt icon
        if (pageIconBox != null) {
            pageIconBox.setStyle(
                    "-fx-background-color:linear-gradient(to bottom right,#4f46e5,#6366f1);" +
                            "-fx-background-radius:14;");
            DropShadow ds = new DropShadow();
            ds.setColor(Color.web("#4f46e5", 0.30)); ds.setRadius(10); ds.setOffsetY(2);
            pageIconBox.setEffect(ds);
            pageIconBox.getChildren().add(svgIcon(IC_RECEIPT, "#ffffff", 22));
        }
    }

    private void updateThemeIcon() {
        if (themeBtn == null) return;
        themeBtn.setGraphic(svgIcon(isDarkMode ? IC_SUN : IC_MOON,
                isDarkMode ? "#f59e0b" : "#4f46e5", 16));
        themeBtn.setText(null);
    }

    private void buildSidebarIcons() {
        // Résumé icon
        if (resumeIconBox != null) {
            resumeIconBox.setStyle(
                    "-fx-background-color:rgba(99,102,241,0.10);" +
                            "-fx-border-color:rgba(99,102,241,0.22);" +
                            "-fx-border-width:1;-fx-border-radius:7;-fx-background-radius:7;");
            resumeIconBox.getChildren().add(svgIcon(IC_CHART, "#4f46e5", 15));
        }
        // Payment methods icon
        if (pmIconBox != null) {
            pmIconBox.setStyle(
                    "-fx-background-color:rgba(99,102,241,0.10);" +
                            "-fx-border-color:rgba(99,102,241,0.22);" +
                            "-fx-border-width:1;-fx-border-radius:7;-fx-background-radius:7;");
            pmIconBox.getChildren().add(svgIcon(IC_CARD, "#4f46e5", 15));
        }
        // QR hint icon — use filled SVG for proper QR appearance
        if (qrHintIconBox != null) {
            qrHintIconBox.setStyle(
                    "-fx-background-color:#4f46e5;" +
                            "-fx-background-radius:12;");
            DropShadow qrDs = new DropShadow();
            qrDs.setColor(Color.web("#4f46e5", 0.35)); qrDs.setRadius(10); qrDs.setOffsetY(2);
            qrHintIconBox.setEffect(qrDs);
            // Filled QR code: three corner squares + dots — FILL not stroke
            String qrFilled =
                    "M2 2h8v8H2zM3 3v6h6V3z" +               // TL outer
                            "M4 4h4v4H4z" +                            // TL inner
                            "M14 2h8v8h-8zM15 3v6h6V3z" +             // TR outer
                            "M16 4h4v4h-4z" +                          // TR inner
                            "M2 14h8v8H2zM3 15v6h6v-6z" +             // BL outer
                            "M4 16h4v4H4z" +                           // BL inner
                            "M14 14h2v2h-2zM16 14h2v2h-2z" +          // BR dots
                            "M14 16h2v2h-2zM16 18h2v2h-2z" +
                            "M18 14h4v4h-4zM18 18h4v4h-4z" +
                            "M10 2h2v2h-2zM12 4h2v2h-2z" +            // extras
                            "M10 6h2v2h-2zM2 10h2v2H2zM4 10h4v2H4z";
            SVGPath qrPath = buildBtnIcon(qrFilled, "#ffffff", 22);
            qrHintIconBox.getChildren().add(qrPath);
        }
    }

    // ── Search listener ───────────────────────────────────────────────────

    private void setupSearchListener() {
        if (searchField == null) return;
        searchField.textProperty().addListener((obs, oldV, newV) -> {
            currentSearch = newV == null ? "" : newV.trim().toLowerCase();
            applyCurrentFilter();
        });
    }

    // ── Export PDF button ─────────────────────────────────────────────────

    private void setupExportButton() {
        if (exportBtn == null) return;
        exportBtn.setOnAction(e -> exportAllToPDF());
    }

    private void exportAllToPDF() {
        List<Paiement> toExport = (allData != null) ? allData : List.of();
        if (toExport.isEmpty()) { showAlert("Export", "Aucune transaction à exporter."); return; }

        // Save as .html → user opens in browser → Ctrl+P → Save as PDF
        FileChooser fc = new FileChooser();
        fc.setTitle("Exporter l'historique des paiements");
        fc.setInitialFileName("historique_7anouti_" +
                new SimpleDateFormat("yyyyMMdd_HHmm").format(new java.util.Date()) + ".html");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Page web (*.html)", "*.html"));
        java.io.File file = fc.showSaveDialog(rootPane.getScene().getWindow());
        if (file == null) return;

        try (java.io.PrintWriter pw = new java.io.PrintWriter(file, StandardCharsets.UTF_8)) {
            double totalValide = toExport.stream()
                    .filter(p -> "validé".equalsIgnoreCase(p.getStatut()))
                    .mapToDouble(Paiement::getMontant).sum();
            long nValidees  = toExport.stream().filter(p -> "validé".equalsIgnoreCase(p.getStatut())).count();
            long nAttente   = toExport.stream().filter(p -> "en attente".equalsIgnoreCase(p.getStatut())).count();
            String genDate  = new SimpleDateFormat("dd MMMM yyyy à HH:mm", java.util.Locale.FRANCE)
                    .format(new java.util.Date());
            SimpleDateFormat sdfRow = new SimpleDateFormat("dd MMM yyyy HH:mm", java.util.Locale.FRANCE);

            StringBuilder rows = new StringBuilder();
            for (Paiement p : toExport) {
                String sc = "valid".equals(p.getStatut() != null ? p.getStatut().toLowerCase().substring(0,5) : "x")
                        ? "badge-v" : "en attente".equalsIgnoreCase(p.getStatut()) ? "badge-a" : "badge-x";
                String sLabel = p.getStatut() != null ? capitalize(p.getStatut()) : "—";
                String meth = p.getMethode() != null ? p.getMethode() : "—";
                String date = p.getDatePaiement() != null ? sdfRow.format(p.getDatePaiement()) : "—";
                String methIcon = switch (meth) {
                    case "Espèces" -> "&#128176;";
                    case "CIB"    -> "&#128179;";
                    case "D17"    -> "&#128241;";
                    default        -> "&#128179;";
                };
                rows.append("<tr>")
                        .append("<td><span class=\"mono\">TXN-").append(txnId(p)).append("</span></td>")
                        .append("<td>").append(date).append("</td>")
                        .append("<td>").append(methIcon).append(" ").append(meth).append("</td>")
                        .append("<td class=\"amount\">").append(fmtDT(p.getMontant())).append("</td>")
                        .append("<td><span class=\"badge ").append(sc).append("\">").append(sLabel).append("</span></td>")
                        .append("</tr>\n");
            }

            pw.println(buildHistoriqueHtml(
                    UserSession.getInstance().getUserName(),
                    UserSession.getInstance().getUserEmail(),
                    genDate, toExport.size(), nValidees, nAttente,
                    fmtDT(totalValide), rows.toString()
            ));

            // Open in default browser
            try { java.awt.Desktop.getDesktop().browse(file.toURI()); }
            catch (Exception ignored) {}

            showStyledAlert("✓ Export ouvert !",
                    "Le document s'est ouvert dans votre navigateur.\nFaites Ctrl+P puis \"Enregistrer en PDF\".",
                    "#16a34a");
        } catch (Exception ex) {
            showAlert("Erreur export", ex.getMessage());
        }
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    private String buildHistoriqueHtml(String name, String email, String genDate,
                                       int total, long validees, long attente, String totalFormate, String rows) {
        return "<!DOCTYPE html><html lang=\"fr\"><head><meta charset=\"UTF-8\">"
                + "<meta name=\"viewport\" content=\"width=device-width,initial-scale=1\">"
                + "<title>Historique des paiements · 7anouti-E</title>"
                + "<style>" + getPdfCssBase()
                + ".stats{display:grid;grid-template-columns:repeat(4,1fr);gap:0;border-bottom:1px solid #f0f0ff;}"
                + ".stat-cell{padding:14px 18px;border-right:1px solid #f0f0ff;}"
                + ".stat-cell:last-child{border-right:none;}"
                + ".stat-num{font-size:22px;font-weight:700;color:#4f46e5;}"
                + ".stat-lbl{font-size:11px;color:#94a3b8;margin-top:2px;}"
                + "table{width:100%;border-collapse:collapse;margin:0;}"
                + "th{font-size:11px;font-weight:600;color:#64748b;text-align:left;padding:10px 16px;"
                + "background:#fafafe;border-bottom:1px solid #e0e7ff;}"
                + "td{padding:11px 16px;font-size:13px;color:#374151;border-bottom:1px solid #f8f7ff;}"
                + "tr:hover td{background:#fafafe;}"
                + ".amount{font-weight:700;color:#4f46e5;text-align:right;}"
                + "th:last-child,td:last-child{text-align:center;}"
                + "th:nth-child(4),td:nth-child(4){text-align:right;}"
                + "</style></head><body>"
                + "<div class=\"page\">"
                + "<div class=\"header\">"
                + "<div class=\"logo-row\"><div class=\"logo-box\">&#x1F6CD;</div>"
                + "<div><div class=\"brand\">7anouti-E</div><div class=\"brand-tag\">Votre marché en ligne</div></div>"
                + "<div style=\"margin-left:auto;text-align:right\">"
                + "<div style=\"font-size:10px;opacity:.65;\">Généré le</div>"
                + "<div style=\"font-size:12px;font-weight:600;\">" + genDate + "</div></div></div>"
                + "<div class=\"doc-title\">Historique des paiements</div>"
                + "<div class=\"doc-sub\">" + name + " · " + email + "</div>"
                + "<div class=\"meta-row\">"
                + "<div class=\"meta-item\"><div class=\"meta-val\">" + total + "</div><div class=\"meta-lbl\">Total</div></div>"
                + "<div class=\"meta-item\"><div class=\"meta-val\">" + validees + "</div><div class=\"meta-lbl\">Validées</div></div>"
                + "<div class=\"meta-item\"><div class=\"meta-val\">" + attente + "</div><div class=\"meta-lbl\">En attente</div></div>"
                + "<div class=\"meta-item\"><div class=\"meta-val\">" + totalFormate + "</div><div class=\"meta-lbl\">Total payé</div></div>"
                + "</div></div>"
                + "<div class=\"card\" style=\"overflow:hidden;\">"
                + "<table><thead><tr><th>Transaction</th><th>Date</th><th>Méthode</th><th>Montant</th><th>Statut</th></tr></thead>"
                + "<tbody>" + rows + "</tbody></table></div>"
                + "<div class=\"footer\"><span>&#x1F512; Document confidentiel · 7anouti-E</span>"
                + "<span>Imprimez depuis votre navigateur pour obtenir un PDF</span></div>"
                + "</div></body></html>";
    }

    private void setupDeleteAllButton() {
        if (deleteAllBtn == null) return;
        String trashPath = "M3 6h18M19 6l-1 14a2 2 0 0 1-2 2H8a2 2 0 0 1-2-2L5 6" +
                "M10 11v6M14 11v6M9 6V4a1 1 0 0 1 1-1h4a1 1 0 0 1 1 1v2";
        SVGPath trashIcRed   = svgIcon(trashPath, "#ef4444", 15);
        SVGPath trashIcWhite = svgIcon(trashPath, "#ffffff", 15);
        deleteAllBtn.setGraphic(trashIcRed);
        deleteAllBtn.setGraphicTextGap(7);
        deleteAllBtn.setContentDisplay(javafx.scene.control.ContentDisplay.LEFT);
        String base = BTN_RESET +
                "-fx-background-color:transparent;-fx-background-radius:8;" +
                "-fx-border-color:rgba(239,68,68,0.45);-fx-border-width:1;-fx-border-radius:8;" +
                "-fx-text-fill:#94a3b8;-fx-font-size:12px;-fx-font-weight:600;" +
                "-fx-padding:8 16;-fx-cursor:hand;";
        String hover = BTN_RESET +
                "-fx-background-color:#ef4444;-fx-background-radius:8;" +
                "-fx-border-color:#ef4444;-fx-border-width:1;-fx-border-radius:8;" +
                "-fx-text-fill:white;-fx-font-size:12px;-fx-font-weight:600;" +
                "-fx-padding:8 16;-fx-cursor:hand;" +
                "-fx-effect:dropshadow(gaussian,rgba(239,68,68,0.30),10,0.2,0,2);";
        deleteAllBtn.setText("Supprimer tout");
        deleteAllBtn.setStyle(base);
        deleteAllBtn.setOnMouseEntered(e -> { deleteAllBtn.setStyle(hover); deleteAllBtn.setGraphic(trashIcWhite); });
        deleteAllBtn.setOnMouseExited(e  -> { deleteAllBtn.setStyle(base);  deleteAllBtn.setGraphic(trashIcRed); });
        deleteAllBtn.setOnAction(e -> showDeleteAllDialog());
    }
    private void showDeleteAllDialog() {
        // Step 1 — amber warning
        showCustomConfirm(
                "⚠️  Supprimer tout l'historique",
                "Cette action supprimera définitivement toutes vos transactions.\nÊtes-vous sûr(e) de vouloir continuer ?",
                "Oui, continuer", "#d97706", "rgba(217,119,6,0.08)",
                () -> {
                    // Step 2 — red final confirmation
                    showCustomConfirm(
                            "🗑  Dernière confirmation",
                            "Confirmer la suppression définitive de TOUTES les transactions ?",
                            "Supprimer définitivement", "#ef4444", "rgba(239,68,68,0.08)",
                            () -> {
                                try {
                                    String sql = "DELETE FROM paiements WHERE user_id = ?";
                                    try (java.sql.PreparedStatement ps =
                                                 com.hanouti.hanoutiem4.util.DBConnection.getInstance()
                                                         .getConnection().prepareStatement(sql)) {
                                        ps.setInt(1, currentUserId);
                                        ps.executeUpdate();
                                    }
                                    allData = new java.util.ArrayList<>();
                                    renderCards(allData);
                                    updateSidebar(allData);
                                } catch (SQLException ex) {
                                    showAlert("Erreur", "Impossible de supprimer : " + ex.getMessage());
                                }
                            });
                });
    }

    private void supprimerTransaction(Paiement p) {
        showCustomConfirm(
                "🗑  Supprimer cette transaction",
                "TXN-" + txnId(p) + " — " + fmtDT(p.getMontant()) +
                        "\nCette action est irréversible.",
                "Supprimer", "#ef4444", "rgba(239,68,68,0.08)",
                () -> {
                    try {
                        String sql = "DELETE FROM paiements WHERE paiement_id = ?";
                        try (java.sql.PreparedStatement ps =
                                     com.hanouti.hanoutiem4.util.DBConnection.getInstance()
                                             .getConnection().prepareStatement(sql)) {
                            ps.setInt(1, p.getPaiementId());
                            ps.executeUpdate();
                        }
                        allData.remove(p);
                        applyCurrentFilter();
                        updateSidebar(allData);
                    } catch (SQLException ex) {
                        showAlert("Erreur", "Impossible de supprimer : " + ex.getMessage());
                    }
                });
    }

    // ── Data loading ──────────────────────────────────────────────────────
    private void loadData() throws SQLException {
        allData = dao.getHistoriqueByUser(currentUserId);
        renderCards(allData);
        updateSidebar(allData);
    }

    // ── Sort bar ─────────────────────────────────────────────────────────
    private void setupSortBar() {
        if (sortBar == null) return;
        sortBar.getChildren().clear();
        sortBar.setSpacing(0);
        sortBar.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        String t3 = isDarkMode ? "#94A3B8" : "#64748b";
        Label sortLbl = new Label("Trier par");
        sortLbl.setStyle("-fx-font-size:11px;-fx-font-weight:600;-fx-text-fill:" + t3 + ";-fx-padding:0 10 0 0;");
        sortBar.getChildren().add(sortLbl);
        // Segmented control
        HBox seg = new HBox(6);
        seg.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        seg.setStyle("-fx-background-color:transparent;-fx-border-width:0;");
        String[][] opts = {
                {"DATE_DESC","Date ↓"},{"DATE_ASC","Date ↑"},
                {"AMT_DESC","Montant ↓"},{"AMT_ASC","Montant ↑"}
        };
        for (int i = 0; i < opts.length; i++) {
            final String key = opts[i][0];
            // No separator — pills stand alone
            Button sb = new Button(opts[i][1]);
            boolean first = (i == 0), last = (i == opts.length - 1);
            applySortStyle(sb, key.equals(currentSort), first, last);
            sb.setOnAction(e -> {
                currentSort = key;
                int k = 0;
                for (javafx.scene.Node n : seg.getChildren()) {
                    if (n instanceof Button b) {
                        int pos = seg.getChildren().indexOf(b);
                        applySortStyle(b, b.getUserData().equals(currentSort), pos == 0, pos == seg.getChildren().size()-1);
                    }
                }
                applyCurrentFilter();
            });
            sb.setUserData(key);
            seg.getChildren().add(sb);
        }
        sortBar.getChildren().add(seg);
    }

    private void applySortStyle(Button btn, boolean active, boolean first, boolean last) {
        // Pill shape — same as filter pills (radius 999, no segmented corners)
        String bg   = isDarkMode ? "rgba(255,255,255,0.06)" : "#ffffff";
        String text = isDarkMode ? "rgba(255,255,255,0.80)" : "#374151";
        String bdr  = isDarkMode ? "rgba(255,255,255,0.20)" : "#e0e7ff";
        btn.setStyle(BTN_RESET +
                (active
                        ? "-fx-background-color:#4f46e5;-fx-text-fill:white;-fx-border-width:0;"
                          + "-fx-effect:dropshadow(gaussian,rgba(79,70,229,0.30),8,0.2,0,2);"
                        : "-fx-background-color:" + bg + ";-fx-text-fill:" + text + ";"
                          + "-fx-border-color:" + bdr + ";-fx-border-width:1.5;") +
                "-fx-font-size:12px;-fx-font-weight:bold;-fx-background-radius:999;" +
                "-fx-border-radius:999;-fx-padding:7 18;-fx-cursor:hand;");
    }
    private void setupFilterTabs() {
        if (filterBar == null) return;
        filterBar.getChildren().clear();
        String[] labels = {"Tous", "Validé", "En attente", "Annulé", "Échec"};
        String[] values = {"Tous", "validé", "en attente", "annulé", "échec"};
        for (int i = 0; i < labels.length; i++) {
            final String val = values[i];
            Button tab = new Button(labels[i]);
            tab.setUserData(val);
            applyTabStyle(tab, val.equals(currentFilter) || (i == 0 && "Tous".equals(currentFilter)));
            tab.setOnAction(e -> {
                currentFilter = val;
                filterBar.getChildren().forEach(n -> {
                    if (n instanceof Button b) applyTabStyle(b, b.getUserData().equals(currentFilter));
                });
                applyCurrentFilter();
            });
            filterBar.getChildren().add(tab);
        }
    }

    private void applyTabStyle(Button tab, boolean active) {
        if (active) {
            tab.setStyle(BTN_RESET +
                    "-fx-background-color:#4f46e5;-fx-text-fill:white;" +
                    "-fx-font-size:12px;-fx-font-weight:bold;-fx-background-radius:999;" +
                    "-fx-border-width:0;-fx-padding:7 18;-fx-cursor:hand;" +
                    "-fx-effect:dropshadow(gaussian,rgba(79,70,229,0.30),8,0.2,0,2);");
        } else {
            String bg   = isDarkMode ? "rgba(255,255,255,0.06)" : "#ffffff";
            String text = isDarkMode ? "rgba(255,255,255,0.80)" : "#374151";
            String bdr  = isDarkMode ? "rgba(255,255,255,0.20)" : "#e0e7ff";
            String base = BTN_RESET +
                    "-fx-background-color:" + bg + ";-fx-text-fill:" + text + ";" +
                    "-fx-font-size:12px;-fx-font-weight:bold;-fx-background-radius:999;" +
                    "-fx-border-color:" + bdr + ";-fx-border-width:1.5;-fx-border-radius:999;" +
                    "-fx-padding:7 18;-fx-cursor:hand;";
            String hov = BTN_RESET +
                    "-fx-background-color:rgba(99,102,241,0.10);-fx-text-fill:#4f46e5;" +
                    "-fx-font-size:12px;-fx-font-weight:bold;-fx-background-radius:999;" +
                    "-fx-border-color:rgba(99,102,241,0.40);-fx-border-width:1.5;-fx-border-radius:999;" +
                    "-fx-padding:7 18;-fx-cursor:hand;";
            tab.setStyle(base);
            tab.setOnMouseEntered(e -> tab.setStyle(hov));
            tab.setOnMouseExited(e  -> tab.setStyle(base));
        }
    }

    private void applyCurrentFilter() {
        if (allData == null) return;
        List<Paiement> filtered = "Tous".equals(currentFilter)
                ? allData
                : allData.stream()
                  .filter(p -> currentFilter.equalsIgnoreCase(p.getStatut()))
                  .collect(Collectors.toList());
        if (!currentSearch.isEmpty()) {
            filtered = filtered.stream().filter(p -> {
                String ref = p.getReferenceTransaction() != null
                        ? p.getReferenceTransaction().toLowerCase() : "";
                String amt = String.valueOf(p.getMontant());
                String mth = p.getMethode() != null ? p.getMethode().toLowerCase() : "";
                return ref.contains(currentSearch) || amt.contains(currentSearch)
                        || mth.contains(currentSearch)
                        || ("txn-" + txnId(p)).contains(currentSearch);
            }).collect(Collectors.toList());
        }
        // Apply sort
        filtered.sort((a, b) -> switch (currentSort) {
            case "DATE_ASC"  -> a.getDatePaiement() != null && b.getDatePaiement() != null
                    ? a.getDatePaiement().compareTo(b.getDatePaiement()) : 0;
            case "AMT_DESC" -> Double.compare(b.getMontant(), a.getMontant());
            case "AMT_ASC"  -> Double.compare(a.getMontant(), b.getMontant());
            default           -> a.getDatePaiement() != null && b.getDatePaiement() != null
                    ? b.getDatePaiement().compareTo(a.getDatePaiement()) : 0;
        });
        renderCards(filtered);
    }

    // ── timeAgo helper ────────────────────────────────────────────────────
    private String timeAgo(java.util.Date date) {
        if (date == null) return "";
        long diffMs  = System.currentTimeMillis() - date.getTime();
        long diffMin = diffMs / 60_000;
        if (diffMin <  1)  return "a l instant";
        if (diffMin < 60)  return "il y a " + diffMin + "min";
        long diffH = diffMin / 60;
        if (diffH   < 24)  return "il y a " + diffH + "h";
        long diffD = diffH / 24;
        if (diffD   ==  1) return "hier";
        if (diffD   <   7) return "il y a " + diffD + "j";
        return new SimpleDateFormat("dd/MM/yyyy").format(date);
    }

    /** Short readable TXN id from the database reference */
    private String txnId(Paiement p) {
        if (p.getReferenceTransaction() == null)
            return String.format("%06d", p.getPaiementId());
        String raw = p.getReferenceTransaction().replaceAll("[^0-9]", "");
        return raw.isEmpty() ? String.format("%06d", p.getPaiementId())
                : (raw.length() > 6 ? raw.substring(raw.length() - 6) : raw);
    }

    // ── Render cards ─────────────────────────────────────────────────────

    private void renderCards(List<Paiement> list) {
        cardsContainer.getChildren().clear();
        if (list.isEmpty()) {
            VBox empty = new VBox(14);
            empty.setAlignment(Pos.CENTER);
            empty.setStyle("-fx-padding:70 0;");
            Label icon = new Label("🕐");
            icon.setStyle("-fx-font-size:52px;-fx-opacity:0.20;");
            Label msg = new Label("Aucune transaction trouvée");
            msg.setStyle("-fx-font-size:18px;-fx-font-weight:bold;-fx-text-fill:" +
                    (isDarkMode ? "#E2F0FF" : "#1e1b4b") + ";");
            Label sub = new Label("Ajustez vos filtres ou effectuez votre premier paiement");
            sub.setStyle("-fx-font-size:13px;-fx-text-fill:#94a3b8;");
            empty.getChildren().addAll(icon, msg, sub);
            cardsContainer.getChildren().add(empty);
        } else {
            for (Paiement p : list) cardsContainer.getChildren().add(buildCard(p));
            staggerCards(cardsContainer.getChildren());
        }
        updateSidebar(list);
    }

    // ── Build transaction card ────────────────────────────────────────────

    private VBox buildCard(Paiement p) {
        String statut = p.getStatut() != null ? p.getStatut() : "";
        String methode = p.getMethode() != null ? p.getMethode() : "";

        // Status colors
        String statusColor, statusBg, statusBorder, statusLabel, statusIcon;
        switch (statut.toLowerCase()) {
            case "validé"     -> { statusColor="#16A34A"; statusBg="#dcfce7"; statusBorder="#86efac"; statusLabel="Validé";     statusIcon="✓"; }
            case "en attente" -> { statusColor="#D97706"; statusBg="#fef9c3"; statusBorder="#fde68a"; statusLabel="En attente"; statusIcon="⏳"; }
            case "annulé"     -> { statusColor="#6B7280"; statusBg="#f3f4f6"; statusBorder="#d1d5db"; statusLabel="Annulé";     statusIcon="○"; }
            default           -> { statusColor="#DC2626"; statusBg="#fee2e2"; statusBorder="#fca5a5"; statusLabel="Échec";      statusIcon="✕"; }
        }

        boolean isEnAttente = "en attente".equalsIgnoreCase(statut);

        // Card VBox
        VBox card = new VBox(0);
        card.setStyle(
                "-fx-background-color:" + (isDarkMode ? "#1A2E4A" : "#ffffff") + ";" +
                        "-fx-border-color:" + (isDarkMode ? "rgba(96,165,250,0.13)" : "rgba(99,102,241,0.13)") + ";" +
                        "-fx-border-width:1;-fx-border-radius:16;-fx-background-radius:16;" +
                        "-fx-effect:dropshadow(gaussian," + (isDarkMode ? "rgba(0,0,0,0.25)" : "rgba(79,70,229,0.07)") + ",16,0.08,0,4);");
        card.setOnMouseEntered(e -> card.setStyle(
                "-fx-background-color:" + (isDarkMode ? "#1E3A5F" : "#f5f3ff") + ";" +
                        "-fx-border-color:rgba(99,102,241,0.28);" +
                        "-fx-border-width:1;-fx-border-radius:16;-fx-background-radius:16;" +
                        "-fx-effect:dropshadow(gaussian,rgba(79,70,229,0.14),20,0.12,0,6);"));
        card.setOnMouseExited(e -> card.setStyle(
                "-fx-background-color:" + (isDarkMode ? "#1A2E4A" : "#ffffff") + ";" +
                        "-fx-border-color:" + (isDarkMode ? "rgba(96,165,250,0.13)" : "rgba(99,102,241,0.13)") + ";" +
                        "-fx-border-width:1;-fx-border-radius:16;-fx-background-radius:16;" +
                        "-fx-effect:dropshadow(gaussian," + (isDarkMode ? "rgba(0,0,0,0.25)" : "rgba(79,70,229,0.07)") + ",16,0.08,0,4);"));

        // ── Status bar (top colored strip) ──
        Region statusBar = new Region();
        statusBar.setPrefHeight(3); statusBar.setMaxHeight(3); statusBar.setMinHeight(3);
        statusBar.setStyle("-fx-background-color:" + statusColor + ";-fx-background-radius:16 16 0 0;");
        statusBar.setMaxWidth(Double.MAX_VALUE);

        // ── Card body ──
        VBox body = new VBox(0);
        body.setStyle("-fx-padding:14 18;");

        // TOP ROW: icon + info + status badge + amount
        HBox topRow = new HBox(12);
        topRow.setAlignment(Pos.CENTER_LEFT);
        topRow.setStyle("-fx-padding:0 0 12 0;");

        // Method icon
        StackPane methodIcon = buildMethodIconPane(methode);

        // Center info
        VBox infoBox = new VBox(3);
        HBox.setHgrow(infoBox, Priority.ALWAYS);

        String txnRef = "Transaction #TXN-" + txnId(p);
        Label refLbl = new Label(txnRef);
        refLbl.setStyle("-fx-font-size:13px;-fx-font-weight:bold;-fx-text-fill:" +
                (isDarkMode ? "#E2F0FF" : "#1e1b4b") + ";");

        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy 'à' HH:mm");
        String dateStr = p.getDatePaiement() != null ? sdf.format(p.getDatePaiement()) : "Date inconnue";
        List<com.hanouti.hanoutiem4.model.Panier> itemsTmp;
        try { itemsTmp = getItemsForPaiement(p); } catch (Exception ignored) { itemsTmp = new java.util.ArrayList<>(); }
        final List<com.hanouti.hanoutiem4.model.Panier> items = itemsTmp;
        int nbItems = items.stream().mapToInt(com.hanouti.hanoutiem4.model.Panier::getQuantite).sum();

        Label metaLbl = new Label(dateStr + (nbItems > 0 ? "  •  " + nbItems + " article" + (nbItems > 1 ? "s" : "") : ""));
        metaLbl.setStyle("-fx-font-size:11px;-fx-text-fill:#94a3b8;");

        if (!items.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < Math.min(items.size(), 3); i++) {
                if (i > 0) sb.append(", ");
                sb.append(items.get(i).getNomProduit()).append(" × ").append(items.get(i).getQuantite());
            }
            if (items.size() > 3) sb.append("…");
            Label prodLbl = new Label(sb.toString());
            prodLbl.setStyle("-fx-font-size:11px;-fx-text-fill:" + (isDarkMode ? "rgba(255,255,255,0.40)" : "#94a3b8") + ";");
            prodLbl.setWrapText(true);
            infoBox.getChildren().addAll(refLbl, metaLbl, prodLbl);
        } else {
            infoBox.getChildren().addAll(refLbl, metaLbl);
        }

        // Right column: status badge + amount + method badge
        VBox rightCol = new VBox(6);
        rightCol.setAlignment(Pos.CENTER_RIGHT);
        rightCol.setMinWidth(170);

        Label statusBadge = new Label(statusIcon + "  " + statusLabel);
        statusBadge.setStyle(
                "-fx-font-size:10px;-fx-font-weight:bold;-fx-text-fill:" + statusColor + ";" +
                        "-fx-background-color:" + statusBg + ";-fx-background-radius:20;" +
                        "-fx-border-color:" + statusBorder + ";-fx-border-width:1;-fx-border-radius:20;" +
                        "-fx-padding:3 12;");
        if (isDarkMode) statusBadge.setStyle(
                "-fx-font-size:10px;-fx-font-weight:bold;-fx-text-fill:" + statusColor + ";" +
                        "-fx-background-color:" + statusColor + "22;-fx-background-radius:20;" +
                        "-fx-border-color:" + statusColor + "55;-fx-border-width:1;-fx-border-radius:20;" +
                        "-fx-padding:3 12;");

        Label amtLbl = new Label(fmtDT(p.getMontant()));
        amtLbl.setStyle("-fx-font-size:18px;-fx-font-weight:800;-fx-text-fill:" +
                (isDarkMode ? "#818cf8" : "#4f46e5") + ";");

        String methLabel = switch (methode) {
            case "Espèces" -> "Espèces (Cash)";
            case "Carte", "VISA" -> "Carte VISA";
            case "CIB"  -> "Carte CIB";
            case "D17"  -> "D17 Mobile";
            default -> methode;
        };
        // Payment badge (VISA / CIB / D17 / CASH coloured pill)
        String pmText, pmBg, pmFg;
        switch (methode) {
            case "CIB"    -> { pmText="CIB";  pmBg="#00447c"; pmFg="white"; }
            case "D17"    -> { pmText="D17";  pmBg="#e31837"; pmFg="white"; }
            case "Espèces"-> { pmText="CASH"; pmBg="#16a34a"; pmFg="white"; }
            default       -> { pmText="VISA"; pmBg="#1a1f71"; pmFg="white"; }
        }
        Label pmBadge = new Label(pmText);
        pmBadge.setStyle(
                "-fx-background-color:" + pmBg + ";-fx-text-fill:" + pmFg + ";" +
                        "-fx-font-size:10px;-fx-font-weight:bold;-fx-background-radius:5;" +
                        "-fx-padding:2 9;");

        Label methLbl = new Label(methLabel);
        methLbl.setStyle("-fx-font-size:11px;-fx-text-fill:#94a3b8;");

        HBox methRow = new HBox(6);
        methRow.setAlignment(Pos.CENTER_RIGHT);
        methRow.getChildren().addAll(pmBadge, methLbl);

        rightCol.getChildren().addAll(statusBadge, amtLbl, methRow);
        topRow.getChildren().addAll(methodIcon, infoBox, rightCol);

        // PRODUCTS EMOJI ROW (if items available)
        HBox emojiRow = null;
        if (!items.isEmpty()) {
            emojiRow = new HBox(0);
            emojiRow.setAlignment(Pos.CENTER_LEFT);
            emojiRow.setStyle("-fx-padding:0 0 12 0;-fx-border-color:" +
                    (isDarkMode ? "rgba(255,255,255,0.06)" : "#f1f0ff") +
                    ";-fx-border-width:0 0 1 0;");
            int max = Math.min(items.size(), 4);
            for (int i = 0; i < max; i++) {
                String emoji = emojiForProduct(items.get(i).getNomProduit());
                Label box = new Label(emoji);
                String emBg = isDarkMode ? "#1e3a5f" : "#e0e7ff";
                String emTx = isDarkMode ? "#93c5fd" : "#3730a3";
                box.setStyle("-fx-font-size:18px;-fx-min-width:36;-fx-min-height:36;" +
                        "-fx-max-width:36;-fx-max-height:36;-fx-alignment:center;" +
                        "-fx-background-color:" + emBg + ";-fx-background-radius:9;" +
                        "-fx-border-color:#fff;-fx-border-width:2.5;-fx-border-radius:9;" +
                        "-fx-translate-x:" + (-10 * i) + ";");
                emojiRow.getChildren().add(box);
            }
            if (items.size() > 4) {
                Label more = new Label("+" + (items.size() - 4));
                String moreBg = isDarkMode ? "#1e3a5f" : "#c7d2fe";
                more.setStyle("-fx-font-size:11px;-fx-font-weight:700;-fx-text-fill:" +
                        (isDarkMode ? "#93c5fd" : "#3730a3") + ";" +
                        "-fx-min-width:36;-fx-min-height:36;-fx-max-width:36;-fx-max-height:36;" +
                        "-fx-alignment:center;-fx-background-color:" + moreBg + ";" +
                        "-fx-background-radius:9;-fx-border-color:#fff;-fx-border-width:2.5;-fx-border-radius:9;" +
                        "-fx-translate-x:" + (-10 * max) + ";");
                emojiRow.getChildren().add(more);
            }
            Label countLbl = new Label(nbItems + " article" + (nbItems > 1 ? "s" : ""));
            countLbl.setStyle("-fx-font-size:11px;-fx-text-fill:#94a3b8;-fx-padding:0 0 0 12;");
            emojiRow.getChildren().add(countLbl);
        }

        // BUTTONS ROW
        HBox btnsRow = new HBox(8);
        btnsRow.setAlignment(Pos.CENTER_LEFT);
        btnsRow.setStyle("-fx-padding:12 0 0 0;");

        SVGPath receiptIco = buildBtnIcon("M14 2H6a2 2 0 0 0-2 2v16l4-2 4 2 4-2 4 2V4a2 2 0 0 0-2-2zM9 13h6M9 9h6M9 17h4", "#ffffff", 15);
        Button factureBtn = buildBlackBtnIcon("Voir facture", receiptIco);
        factureBtn.setOnAction(ev -> exportInvoiceHtml(p));

        // Delete button (trash)
        // Stroked trash SVG — same icon as "Supprimer tout"
        SVGPath trashIco = svgIcon(
                "M3 6h18M19 6l-1 14a2 2 0 0 1-2 2H8a2 2 0 0 1-2-2L5 6" +
                        "M10 11v6M14 11v6M9 6V4a1 1 0 0 1 1-1h4a1 1 0 0 1 1 1v2",
                "#dc2626", 15);
        // Red delete button — outline style matching "Vider le panier"
        Button deleteBtn = new Button("Supprimer");
        deleteBtn.setGraphic(trashIco);
        deleteBtn.setGraphicTextGap(7);
        deleteBtn.setContentDisplay(javafx.scene.control.ContentDisplay.LEFT);
        String delBase = BTN_RESET +
                "-fx-background-color:transparent;-fx-background-radius:999;" +
                "-fx-border-color:#dc2626;-fx-border-width:1.5;-fx-border-radius:999;" +
                "-fx-text-fill:#dc2626;-fx-font-size:12px;-fx-font-weight:bold;" +
                "-fx-padding:7 18;-fx-cursor:hand;";
        String delHov = BTN_RESET +
                "-fx-background-color:#fef2f2;-fx-background-radius:999;" +
                "-fx-border-color:#b91c1c;-fx-border-width:1.5;-fx-border-radius:999;" +
                "-fx-text-fill:#b91c1c;-fx-font-size:12px;-fx-font-weight:bold;" +
                "-fx-padding:7 18;-fx-cursor:hand;";
        deleteBtn.setStyle(delBase);
        deleteBtn.setOnMouseEntered(ev -> deleteBtn.setStyle(delHov));
        deleteBtn.setOnMouseExited(ev  -> deleteBtn.setStyle(delBase));
        deleteBtn.setOnAction(ev -> supprimerTransaction(p));

        if (isEnAttente && "Espèces".equals(methode)) {
            Button cancelBtn = buildCancelBtn();
            cancelBtn.setOnAction(ev -> showCancelDialog(p));
            btnsRow.getChildren().addAll(factureBtn, cancelBtn, deleteBtn);
        } else {
            btnsRow.getChildren().addAll(factureBtn, deleteBtn);
        }

        // Details expand button — pushed to the right with a spacer
        Region btnSpacer = new Region();
        HBox.setHgrow(btnSpacer, Priority.ALWAYS);
        SVGPath chevIco = svgIcon("M6 9l6 6 6-6", "#64748b", 14);
        Button detailsBtn = buildGhostBtn("Détails");
        detailsBtn.setGraphic(chevIco);
        detailsBtn.setGraphicTextGap(6);
        detailsBtn.setContentDisplay(javafx.scene.control.ContentDisplay.LEFT);
        btnsRow.getChildren().addAll(btnSpacer, detailsBtn);

        // Expanded details panel (toggle on click)
        VBox expandPanel = new VBox(0);
        expandPanel.setVisible(false); expandPanel.setManaged(false);

        detailsBtn.setOnAction(ev -> {
            boolean open = expandPanel.isVisible();
            expandPanel.setVisible(!open); expandPanel.setManaged(!open);
            detailsBtn.setText(open ? "Détails" : "Masquer détails");
            if (!open) buildExpandedDetails(expandPanel, p, items);
        });

        body.getChildren().addAll(topRow);
        if (emojiRow != null) body.getChildren().add(emojiRow);
        body.getChildren().addAll(btnsRow, expandPanel);
        card.getChildren().addAll(statusBar, body);
        return card;
    }

    private void buildExpandedDetails(VBox panel, Paiement p,
                                      List<com.hanouti.hanoutiem4.model.Panier> items) {
        panel.getChildren().clear();
        panel.setStyle("-fx-padding:12 0 0 0;-fx-border-color:" +
                (isDarkMode ? "rgba(255,255,255,0.06)" : "#f1f0ff") +
                ";-fx-border-width:1 0 0 0;-fx-spacing:8;");

        boolean dark = isDarkMode;
        String t1   = dark ? "#E2F0FF" : "#1e1b4b";
        String t3   = dark ? "rgba(255,255,255,0.60)" : "#475569";
        String rowBg= dark ? "rgba(255,255,255,0.05)" : "#eef2ff";

        // Products list
        if (!items.isEmpty()) {
            for (com.hanouti.hanoutiem4.model.Panier item : items) {
                HBox row = new HBox(10); row.setAlignment(Pos.CENTER_LEFT);
                row.setStyle("-fx-padding:7 10;-fx-background-color:" + rowBg +
                        ";-fx-background-radius:8;-fx-margin:2 0;");
                String eBg = dark ? "rgba(99,102,241,0.18)" : "#e0e7ff";
                Label emoji = new Label(emojiForProduct(item.getNomProduit()));
                emoji.setStyle("-fx-font-size:18px;" +
                        "-fx-min-width:36;-fx-min-height:36;-fx-max-width:36;-fx-max-height:36;" +
                        "-fx-alignment:center;-fx-background-color:" + eBg + ";" +
                        "-fx-background-radius:9;");
                VBox info = new VBox(1); HBox.setHgrow(info, Priority.ALWAYS);
                Label nom = new Label(item.getNomProduit());
                nom.setStyle("-fx-font-size:12px;-fx-font-weight:bold;-fx-text-fill:" + t1 + ";");
                Label qty = new Label("Qté : " + item.getQuantite());
                qty.setStyle("-fx-font-size:10px;-fx-text-fill:" + t3 + ";");
                info.getChildren().addAll(nom, qty);
                Label total = new Label(fmtDT2(item.getSousTotal()));
                total.setStyle("-fx-font-size:12px;-fx-font-weight:bold;-fx-text-fill:#4f46e5;");
                row.getChildren().addAll(emoji, info, total);
                panel.getChildren().add(row);
                VBox.setMargin(row, new Insets(2, 0, 2, 0));
            }
        }

        // Total summary row
        HBox totalRow = new HBox(); totalRow.setAlignment(Pos.CENTER_LEFT);
        totalRow.setStyle("-fx-padding:10 0 0 0;");
        Label totalLbl = new Label("Total payé");
        totalLbl.setStyle("-fx-font-size:13px;-fx-font-weight:bold;-fx-text-fill:" + t1 + ";");
        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        Label totalAmt = new Label(fmtDT(p.getMontant()));
        totalAmt.setStyle("-fx-font-size:15px;-fx-font-weight:800;-fx-text-fill:#4f46e5;");
        totalRow.getChildren().addAll(totalLbl, sp, totalAmt);
        panel.getChildren().add(totalRow);
    }

    // ── Method icon pane ──────────────────────────────────────────────────

    private StackPane buildMethodIconPane(String methode) {
        String iconColor, bgColor, svgD;
        switch (methode) {
            case "Espèces" -> { iconColor="#16A34A"; bgColor= isDarkMode ? "rgba(22,163,74,0.18)" : "rgba(22,163,74,0.10)"; svgD=IC_WALLET; }
            case "CIB"    -> { iconColor="#DC2626"; bgColor= isDarkMode ? "rgba(220,38,38,0.18)"  : "rgba(220,38,38,0.09)";  svgD=IC_CARD; }
            case "D17"    -> { iconColor="#EA580C"; bgColor= isDarkMode ? "rgba(234,88,12,0.18)"  : "rgba(234,88,12,0.09)";  svgD=IC_PHONE; }
            default       -> { iconColor="#4f46e5"; bgColor= isDarkMode ? "rgba(99,102,241,0.18)" : "rgba(99,102,241,0.10)"; svgD=IC_CARD; }
        }
        StackPane container = new StackPane();
        container.setMinWidth(44); container.setMinHeight(44);
        container.setMaxWidth(44); container.setMaxHeight(44);
        container.setStyle("-fx-background-color:" + bgColor + ";-fx-background-radius:12;");
        container.getChildren().add(svgIcon(svgD, iconColor, 18));
        return container;
    }

    // ── Button builders ───────────────────────────────────────────────────

    private Button buildBlackBtn(String text) {
        return buildBlackBtnIcon(text, null);
    }

    private Button buildBlackBtnIcon(String text, SVGPath graphic) {
        Button btn = new Button(text);
        if (graphic != null) {
            btn.setGraphic(graphic);
            btn.setGraphicTextGap(7);
            btn.setContentDisplay(javafx.scene.control.ContentDisplay.LEFT);
        }
        String base = BTN_RESET +
                "-fx-background-color:#4f46e5;-fx-background-radius:999;" +
                "-fx-text-fill:white;-fx-font-size:12px;-fx-font-weight:bold;" +
                "-fx-border-width:0;-fx-padding:8 18;-fx-cursor:hand;";
        String hover = BTN_RESET +
                "-fx-background-color:#4338ca;-fx-background-radius:999;" +
                "-fx-text-fill:white;-fx-font-size:12px;-fx-font-weight:bold;" +
                "-fx-border-width:0;-fx-padding:8 18;-fx-cursor:hand;";
        btn.setStyle(base);
        btn.setOnMouseEntered(e -> btn.setStyle(hover));
        btn.setOnMouseExited(e  -> btn.setStyle(base));
        return btn;
    }

    private static final String BTN_RESET =
            "-fx-background-insets:0;-fx-shadow-highlight-color:transparent;" +
                    "-fx-outer-border:transparent;-fx-inner-border:transparent;-fx-body-color:transparent;" +
                    "-fx-focus-color:transparent;-fx-faint-focus-color:transparent;";

    private Button buildActionBtn(String text, String color, String bg, String border) {
        Button btn = new Button(text);
        String base = BTN_RESET +
                "-fx-background-color:" + bg + ";-fx-background-radius:8;" +
                "-fx-text-fill:" + color + ";-fx-font-size:11px;-fx-font-weight:bold;" +
                "-fx-border-color:" + border + ";-fx-border-width:1;-fx-border-radius:8;" +
                "-fx-padding:7 14;-fx-cursor:hand;";
        String hover = BTN_RESET +
                "-fx-background-color:" + bg.replace("0.10","0.22") + ";-fx-background-radius:8;" +
                "-fx-text-fill:" + color + ";-fx-font-size:11px;-fx-font-weight:bold;" +
                "-fx-border-color:" + color + ";-fx-border-width:1;-fx-border-radius:8;" +
                "-fx-padding:7 14;-fx-cursor:hand;";
        btn.setStyle(base);
        btn.setOnMouseEntered(e -> btn.setStyle(hover));
        btn.setOnMouseExited(e  -> btn.setStyle(base));
        return btn;
    }

    private Button buildPayBtn() {
        Button btn = new Button("Marquer payé");
        SVGPath icon = buildBtnIcon(
                "M9 12l2 2 4-4M7 2H5a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2V9l-5-7H7z",
                "#ffffff", 14);
        btn.setGraphic(icon);
        btn.setGraphicTextGap(6);
        btn.setContentDisplay(javafx.scene.control.ContentDisplay.LEFT);
        String base = BTN_RESET +
                "-fx-background-color:#16a34a;-fx-background-radius:999;" +
                "-fx-text-fill:white;-fx-font-size:12px;-fx-font-weight:bold;" +
                "-fx-border-width:0;-fx-padding:8 18;-fx-cursor:hand;";
        String hover = BTN_RESET +
                "-fx-background-color:#15803d;-fx-background-radius:999;" +
                "-fx-text-fill:white;-fx-font-size:12px;-fx-font-weight:bold;" +
                "-fx-border-width:0;-fx-padding:8 18;-fx-cursor:hand;";
        btn.setStyle(base);
        btn.setOnMouseEntered(e -> btn.setStyle(hover));
        btn.setOnMouseExited(e  -> btn.setStyle(base));
        return btn;
    }

    private Button buildCancelBtn() {
        Button btn = new Button("Annuler");
        SVGPath icon = buildBtnIcon(
                "M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2z" +
                        "M8 11h8v2H8z", "#d97706", 14);
        btn.setGraphic(icon);
        btn.setGraphicTextGap(6);
        btn.setContentDisplay(javafx.scene.control.ContentDisplay.LEFT);
        // Amber outline to match design harmony
        String base = BTN_RESET +
                "-fx-background-color:transparent;-fx-background-radius:999;" +
                "-fx-border-color:#d97706;-fx-border-width:1.5;-fx-border-radius:999;" +
                "-fx-text-fill:#d97706;-fx-font-size:12px;-fx-font-weight:bold;" +
                "-fx-padding:7 18;-fx-cursor:hand;";
        String hover = BTN_RESET +
                "-fx-background-color:#fffbeb;-fx-background-radius:999;" +
                "-fx-border-color:#b45309;-fx-border-width:1.5;-fx-border-radius:999;" +
                "-fx-text-fill:#b45309;-fx-font-size:12px;-fx-font-weight:bold;" +
                "-fx-padding:7 18;-fx-cursor:hand;";
        btn.setStyle(base);
        btn.setOnMouseEntered(e -> btn.setStyle(hover));
        btn.setOnMouseExited(e  -> btn.setStyle(base));
        return btn;
    }

    private Button buildGhostBtn(String text) {
        Button btn = new Button(text);
        String base = BTN_RESET +
                "-fx-background-color:transparent;-fx-background-radius:999;" +
                "-fx-text-fill:#4f46e5;-fx-font-size:12px;-fx-font-weight:700;" +
                "-fx-border-color:rgba(99,102,241,0.40);-fx-border-width:1.5;-fx-border-radius:999;" +
                "-fx-padding:8 18;-fx-cursor:hand;";
        String hover = BTN_RESET +
                "-fx-background-color:#eef2ff;-fx-background-radius:999;" +
                "-fx-text-fill:#4338ca;-fx-font-size:12px;-fx-font-weight:700;" +
                "-fx-border-color:#6366f1;-fx-border-width:1.5;-fx-border-radius:999;" +
                "-fx-padding:8 18;-fx-cursor:hand;";
        btn.setStyle(base);
        HBox.setHgrow(btn, Priority.NEVER);
        Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS);
        btn.setUserData(spacer);
        btn.setOnMouseEntered(e -> btn.setStyle(hover));
        btn.setOnMouseExited(e  -> btn.setStyle(base));
        return btn;
    }

    // ── Emoji guesser for products ────────────────────────────────────────

    private String emojiForProduct(String name) {
        if (name == null) return "📦";
        String n = name.toLowerCase();
        // Electronics & tech
        if (n.contains("laptop") || n.contains("ordinateur") || n.contains("pc") || n.contains("macbook")) return "💻";
        if (n.contains("souris") || n.contains("mouse")) return "🖱";
        if (n.contains("clavier") || n.contains("keyboard")) return "⌨";
        if (n.contains("écran") || n.contains("ecran") || n.contains("moniteur") || n.contains("screen")) return "🖥";
        if (n.contains("téléphone") || n.contains("telephone") || n.contains("smartphone") || n.contains("iphone") || n.contains("samsung")) return "📱";
        if (n.contains("casque") || n.contains("ecouteur") || n.contains("headphone") || n.contains("airpod")) return "🎧";
        if (n.contains("imprimante") || n.contains("printer")) return "🖨";
        if (n.contains("tablette") || n.contains("tablet") || n.contains("ipad")) return "📟";
        if (n.contains("camera") || n.contains("webcam") || n.contains("caméra")) return "📷";
        if (n.contains("usb") || n.contains("cable") || n.contains("câble") || n.contains("chargeur")) return "🔌";
        if (n.contains("disque") || n.contains("ssd") || n.contains("stockage")) return "💾";
        if (n.contains("processeur") || n.contains("cpu") || n.contains("ram") || n.contains("mémoire")) return "🔧";
        if (n.contains("sans fil") || n.contains("wifi") || n.contains("wireless") || n.contains("bluetooth")) return "📡";
        if (n.contains("dell") || n.contains("hp") || n.contains("lenovo") || n.contains("asus") || n.contains("acer")) return "💻";
        // Food
        if (n.contains("couscous") || n.contains("blé") || n.contains("semoule")) return "🌾";
        if (n.contains("thon") || n.contains("poisson") || n.contains("sardine")) return "🐟";
        if (n.contains("huile") || n.contains("olive")) return "🫒";
        if (n.contains("datte") || n.contains("deglet")) return "🌴";
        if (n.contains("harissa") || n.contains("piment") || n.contains("poivre")) return "🌶";
        if (n.contains("café") || n.contains("touba")) return "☕";
        if (n.contains("thé") || n.contains("tisane")) return "🍵";
        if (n.contains("miel")) return "🍯";
        if (n.contains("riz") || n.contains("basmati")) return "🍚";
        if (n.contains("makroudh") || n.contains("bsisa")) return "🍪";
        if (n.contains("lentille") || n.contains("haricot") || n.contains("pois")) return "🫘";
        if (n.contains("epice") || n.contains("épice") || n.contains("tajine")) return "🧂";
        return "📦";
    }

    // ── Sidebar stats update ──────────────────────────────────────────────

    private void updateSidebar(List<Paiement> list) {
        if (resumeRows == null) return;
        resumeRows.getChildren().clear();
        resumeRows.setSpacing(0);

        long total    = list.size();
        long reussies = list.stream().filter(p -> "validé".equalsIgnoreCase(p.getStatut())).count();
        long attente  = list.stream().filter(p -> "en attente".equalsIgnoreCase(p.getStatut())).count();
        long echecs   = list.stream().filter(p ->
                "échec".equalsIgnoreCase(p.getStatut()) || "echoué".equalsIgnoreCase(p.getStatut())).count();
        double totalPaye = list.stream()
                .filter(p -> "validé".equalsIgnoreCase(p.getStatut()))
                .mapToDouble(Paiement::getMontant).sum();

        String t1 = isDarkMode ? "#E2F0FF" : "#1e1b4b";
        String t3 = isDarkMode ? "#94A3B8"  : "#64748b";

        addResumeRow("Total transactions", String.valueOf(total),    "#4f46e5", t3);
        addResumeRow("Réussies",            String.valueOf(reussies), "#16A34A",  t3);
        addResumeRow("En attente",          String.valueOf(attente),  "#D97706",  t3);
        addResumeRow("Échouées",            String.valueOf(echecs),   "#DC2626",  t3);

        // Total payé row (bigger + divider)
        Region divider = new Region(); divider.setPrefHeight(1); divider.setMaxHeight(1);
        divider.setStyle("-fx-background-color:" + (isDarkMode ? "rgba(255,255,255,0.06)" : "#f1f0ff") + ";");
        VBox.setMargin(divider, new Insets(6, 0, 8, 0));
        resumeRows.getChildren().add(divider);

        HBox totalRow = new HBox(); totalRow.setAlignment(Pos.CENTER_LEFT);
        Label totalLbl = new Label("Total payé");
        totalLbl.setStyle("-fx-font-size:13px;-fx-font-weight:bold;-fx-text-fill:" + t1 + ";");
        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        Label totalAmtLbl = new Label(fmtDT(totalPaye));
        totalAmtLbl.setStyle("-fx-font-size:16px;-fx-font-weight:800;-fx-text-fill:#4f46e5;");
        totalRow.getChildren().addAll(totalLbl, sp, totalAmtLbl);
        resumeRows.getChildren().add(totalRow);

        // Update stats bar
        buildStatsBar(total, reussies, attente, totalPaye);

        if (transactionsBadge != null)
            transactionsBadge.setText(total + " transaction" + (total > 1 ? "s" : ""));
    }

    private void buildStatsBar(long total, long reussies, long attente, double totalPaye) {
        if (statsBar == null) return;
        statsBar.getChildren().clear();
        statsBar.setSpacing(10);

        statsBar.getChildren().add(buildStatCard("Total", String.valueOf(total), "#4f46e5",
                "rgba(99,102,241,0.08)", "rgba(99,102,241,0.18)"));
        statsBar.getChildren().add(buildStatCard("Validées", String.valueOf(reussies), "#16a34a",
                "rgba(22,163,74,0.08)", "rgba(22,163,74,0.18)"));
        statsBar.getChildren().add(buildStatCard("En attente", String.valueOf(attente), "#d97706",
                "rgba(245,158,11,0.08)", "rgba(245,158,11,0.18)"));
        statsBar.getChildren().add(buildStatCard("Total payé",
                fmtDT2(totalPaye), "#4f46e5",
                "rgba(99,102,241,0.08)", "rgba(99,102,241,0.18)"));
    }

    private HBox buildStatCard(String label, String value, String color, String bg, String border) {
        HBox card = new HBox(12);
        card.setAlignment(Pos.CENTER_LEFT);
        String lightBg  = isDarkMode ? bg  : bg.replace("0.08","0.14").replace("0.04","0.09");
        String lightBdr = isDarkMode ? border : border.replace("0.18","0.32").replace("0.20","0.32");
        card.setStyle("-fx-background-color:" + lightBg + ";-fx-border-color:" + lightBdr + ";" +
                "-fx-border-width:1.5;-fx-border-radius:14;-fx-background-radius:14;-fx-padding:12 16;");
        HBox.setHgrow(card, Priority.ALWAYS);
        VBox info = new VBox(3);
        Label num = new Label(value);
        num.setStyle("-fx-font-size:20px;-fx-font-weight:800;-fx-text-fill:" + color + ";");
        Label lbl = new Label(label);
        String lc = isDarkMode ? "rgba(255,255,255,0.55)" : "#64748b";
        lbl.setStyle("-fx-font-size:11px;-fx-font-weight:600;-fx-text-fill:" + lc + ";");
        info.getChildren().addAll(num, lbl);
        card.getChildren().add(info);
        return card;
    }

    private void addResumeRow(String label, String value, String valueColor, String labelColor) {
        HBox row = new HBox(); row.setAlignment(Pos.CENTER_LEFT);
        row.setStyle("-fx-padding:6 0;-fx-border-color:" +
                (isDarkMode ? "rgba(255,255,255,0.04)" : "#f8f7ff") +
                ";-fx-border-width:0 0 1 0;");
        Label lbl = new Label(label);
        lbl.setStyle("-fx-font-size:12px;-fx-text-fill:" + labelColor + ";");
        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        Label val = new Label(value);
        val.setStyle("-fx-font-size:13px;-fx-font-weight:bold;-fx-text-fill:" + valueColor + ";");
        row.getChildren().addAll(lbl, sp, val);
        resumeRows.getChildren().add(row);
    }

    private void updateStats(List<Paiement> list) { /* kept for compat */ }

    // ── Dark mode ─────────────────────────────────────────────────────────

    private void applyDarkMode() {
        if (isDarkMode) {
            if (!rootPane.getStyleClass().contains("dark")) rootPane.getStyleClass().add("dark");
        } else {
            rootPane.getStyleClass().remove("dark");
        }
    }

    // ── Cart pill update ──────────────────────────────────────────────────

    private void updateCartPill() {
        try {
            com.hanouti.hanoutiem4.dao.PanierDAO panierDAO = new com.hanouti.hanoutiem4.dao.PanierDAO();
            java.util.List<com.hanouti.hanoutiem4.model.Panier> panierItems =
                    panierDAO.getCartItems(currentUserId);
            int count = panierItems.stream().mapToInt(com.hanouti.hanoutiem4.model.Panier::getQuantite).sum();
            double total = panierItems.stream()
                    .mapToDouble(i -> i.getPrixUnitaire() * i.getQuantite()).sum();
            if (cartAmountLabel != null) cartAmountLabel.setText(fmtDT2(total));
            if (cartCountBadge  != null) cartCountBadge.setText(String.valueOf(count));
        } catch (Exception ignored) {}
    }

    // ── Navigation handlers ───────────────────────────────────────────────

    @FXML public void handleFilter()       { applyCurrentFilter(); }
    @FXML public void handleReset()        { currentFilter = "Tous"; setupFilterTabs(); applyCurrentFilter(); }
    @FXML public void handleRetour()       { navigateTo("Panier.fxml",  "7anouti-E — Panier",      1280, 780); }
    @FXML public void handleOpenWishlist() { navigateTo("wishlist.fxml","7anouti-E — Mes Favoris",  1250, 700); }
    @FXML public void handleNavPanier()    { navigateTo("Panier.fxml",  "7anouti-E — Panier",      1280, 780); }
    @FXML public void handleNavFavoris()   { navigateTo("wishlist.fxml","7anouti-E — Mes Favoris",  1250, 700); }

    private void navigateTo(String fxmlFile, String title, int w, int h) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/hanouti/hanoutiem4/" + fxmlFile));
            Scene scene = new Scene(loader.load(), w, h);
            Stage stage = (Stage) rootPane.getScene().getWindow();
            stage.setTitle(title); stage.setScene(scene);
        } catch (IOException e) { showAlert("Erreur navigation", e.getMessage()); }
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title); alert.setContentText(message); alert.showAndWait();
    }

    private void showStyledAlert(String title, String message, String accentColor) {
        boolean dark = isDarkMode;
        String bgCard = dark ? "#110F26" : "#FFFFFF";
        String t1 = dark ? "#F1F0FF" : "#1e1b4b";
        String t3 = dark ? "rgba(241,240,255,0.55)" : "#64748b";

        StackPane overlay = new StackPane();
        overlay.setStyle("-fx-background-color:rgba(0,0,0,0.45);");
        javafx.scene.layout.AnchorPane.setTopAnchor(overlay,0.0);
        javafx.scene.layout.AnchorPane.setBottomAnchor(overlay,0.0);
        javafx.scene.layout.AnchorPane.setLeftAnchor(overlay,0.0);
        javafx.scene.layout.AnchorPane.setRightAnchor(overlay,0.0);

        VBox card = new VBox(16);
        card.setMaxWidth(400); card.setMaxHeight(Region.USE_PREF_SIZE);
        card.setAlignment(Pos.CENTER);
        card.setStyle("-fx-background-color:" + bgCard + ";-fx-background-radius:20;" +
                "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.45),40,0,0,10);-fx-padding:32 36;");
        card.setScaleX(0.85); card.setScaleY(0.85); card.setOpacity(0);
        StackPane.setAlignment(card, Pos.CENTER);

        Label titleLbl = new Label(title);
        titleLbl.setStyle("-fx-font-size:16px;-fx-font-weight:bold;-fx-text-fill:" + accentColor + ";-fx-wrap-text:true;");
        titleLbl.setWrapText(true);
        Label msgLbl = new Label(message);
        msgLbl.setStyle("-fx-font-size:13px;-fx-text-fill:" + t3 + ";-fx-wrap-text:true;");
        msgLbl.setWrapText(true);

        Button okBtn = new Button("OK");
        okBtn.setStyle(BTN_RESET +
                "-fx-background-color:" + accentColor + ";-fx-background-radius:10;" +
                "-fx-text-fill:white;-fx-font-size:13px;-fx-font-weight:bold;-fx-padding:10 32;-fx-cursor:hand;");
        okBtn.setMinWidth(120);

        card.getChildren().addAll(titleLbl, msgLbl, okBtn);
        overlay.getChildren().add(card);
        rootPane.getChildren().add(overlay);

        Runnable dismiss = () -> {
            FadeTransition fo = new FadeTransition(Duration.millis(160), overlay); fo.setToValue(0);
            fo.setOnFinished(ev -> rootPane.getChildren().remove(overlay)); fo.play();
        };
        okBtn.setOnAction(e -> dismiss.run());
        overlay.setOnMouseClicked(e -> { if (e.getTarget() == overlay) dismiss.run(); });

        card.setOpacity(0); card.setScaleX(0.85); card.setScaleY(0.85);
        FadeTransition fi = new FadeTransition(Duration.millis(200), card); fi.setToValue(1);
        ScaleTransition si = new ScaleTransition(Duration.millis(220), card);
        si.setToX(1); si.setToY(1); si.setInterpolator(Interpolator.EASE_OUT);
        new ParallelTransition(fi, si).play();
    }

    private void showCustomConfirm(String title, String message,
                                   String confirmLabel, String confirmColor, String confirmBg, Runnable onConfirm) {
        boolean dark = isDarkMode;
        String bgCard = dark ? "#110F26" : "#FFFFFF";
        String t1 = dark ? "#F1F0FF" : "#1e1b4b";
        String t3 = dark ? "rgba(241,240,255,0.55)" : "#64748b";
        String borderClr = dark ? "rgba(99,102,241,0.25)" : "rgba(99,102,241,0.18)";

        StackPane overlay = new StackPane();
        overlay.setStyle("-fx-background-color:rgba(0,0,0,0.50);");
        javafx.scene.layout.AnchorPane.setTopAnchor(overlay,0.0);
        javafx.scene.layout.AnchorPane.setBottomAnchor(overlay,0.0);
        javafx.scene.layout.AnchorPane.setLeftAnchor(overlay,0.0);
        javafx.scene.layout.AnchorPane.setRightAnchor(overlay,0.0);

        VBox card = new VBox(18);
        card.setMaxWidth(400); card.setMaxHeight(Region.USE_PREF_SIZE);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setStyle("-fx-background-color:" + bgCard + ";-fx-background-radius:20;" +
                "-fx-border-color:" + borderClr + ";-fx-border-width:1;-fx-border-radius:20;" +
                "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.50),50,0,0,12);-fx-padding:28 30;");
        card.setScaleX(0.85); card.setScaleY(0.85); card.setOpacity(0);
        StackPane.setAlignment(card, Pos.CENTER);

        Label titleLbl = new Label(title);
        titleLbl.setStyle("-fx-font-size:15px;-fx-font-weight:bold;-fx-text-fill:" + t1 + ";-fx-wrap-text:true;");
        titleLbl.setWrapText(true);
        Label msgLbl = new Label(message);
        msgLbl.setStyle("-fx-font-size:12px;-fx-text-fill:" + t3 + ";-fx-wrap-text:true;-fx-line-spacing:4;");
        msgLbl.setWrapText(true);

        HBox btnRow = new HBox(10); btnRow.setAlignment(Pos.CENTER_RIGHT);
        Button cancelBtn2 = new Button("Annuler");
        cancelBtn2.setStyle(BTN_RESET +
                "-fx-background-color:transparent;-fx-background-radius:10;" +
                "-fx-text-fill:" + t3 + ";-fx-font-size:12px;-fx-font-weight:bold;" +
                "-fx-border-color:" + borderClr + ";-fx-border-width:1;-fx-border-radius:10;" +
                "-fx-padding:9 20;-fx-cursor:hand;");
        Button confirmBtn = new Button(confirmLabel);
        confirmBtn.setStyle(BTN_RESET +
                "-fx-background-color:" + confirmColor + ";-fx-background-radius:10;" +
                "-fx-text-fill:white;-fx-font-size:12px;-fx-font-weight:bold;" +
                "-fx-padding:9 24;-fx-cursor:hand;");
        btnRow.getChildren().addAll(cancelBtn2, confirmBtn);

        card.getChildren().addAll(titleLbl, msgLbl, btnRow);
        overlay.getChildren().add(card);
        rootPane.getChildren().add(overlay);

        Runnable dismiss = () -> {
            FadeTransition fo = new FadeTransition(Duration.millis(150), overlay); fo.setToValue(0);
            fo.setOnFinished(ev -> rootPane.getChildren().remove(overlay)); fo.play();
        };
        cancelBtn2.setOnAction(e -> dismiss.run());
        confirmBtn.setOnAction(e -> { dismiss.run(); onConfirm.run(); });
        overlay.setOnMouseClicked(e -> { if (e.getTarget() == overlay) dismiss.run(); });

        FadeTransition fi = new FadeTransition(Duration.millis(200), card); fi.setToValue(1);
        ScaleTransition si = new ScaleTransition(Duration.millis(220), card);
        si.setToX(1); si.setToY(1); si.setInterpolator(Interpolator.EASE_OUT);
        new ParallelTransition(fi, si).play();
    }

    // ── Status color helper ───────────────────────────────────────────────
    private String statusColor(String statut) {
        if (statut == null) return "#DC2626";
        return switch (statut.toLowerCase()) {
            case "validé"     -> "#16A34A";
            case "en attente" -> "#D97706";
            case "annulé"     -> "#6B7280";
            default           -> "#DC2626";
        };
    }

    // ══════════════════════════════════════════════════════════════════════
    //  CANCEL DIALOG — unchanged from original
    // ══════════════════════════════════════════════════════════════════════
    private void showMarkAsPaidDialog(Paiement p) {
        showCustomConfirm(
                "✅  Confirmer le paiement en espèces",
                "Confirmer la réception du paiement en espèces pour :\n"
                        + "Transaction #TXN-" + txnId(p) + "\n"
                        + fmtDT(p.getMontant()) + "\n\n"
                        + "Le statut passera à \"Validé\".",
                "Confirmer le paiement", "#16a34a", "rgba(22,163,74,0.10)",
                () -> {
                    try {
                        dao.updateStatut(p.getReferenceTransaction(), "validé");
                        loadData();
                        showStyledAlert("✅ Paiement validé !",
                                "TXN-" + txnId(p) + " marquée comme payée.", "#16a34a");
                    } catch (SQLException ex) {
                        showAlert("Erreur", "Impossible de mettre à jour : " + ex.getMessage());
                    }
                });
    }

    private void showCancelDialog(Paiement p) {
        showCustomConfirm(
                "⏹  Annuler cette transaction",
                "Transaction TXN-" + txnId(p) +
                        "\n" + fmtDT(p.getMontant()) + " — " + p.getMethode() +
                        "\n\nLe statut passera à \"Annulé\". Action irréversible.",
                "Confirmer l'annulation", "#d97706", "rgba(217,119,6,0.10)",
                () -> {
                    try { dao.annulerPaiement(p.getReferenceTransaction()); loadData(); }
                    catch (SQLException ex) { showAlert("Erreur", "Impossible d'annuler : " + ex.getMessage()); }
                });
    }

    // ══════════════════════════════════════════════════════════════════════
    //  EDIT DIALOG — unchanged from original
    // ══════════════════════════════════════════════════════════════════════
    private void showEditDialog(Paiement p) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Changer le mode de paiement");
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initOwner(rootPane.getScene().getWindow());

        ComboBox<String> methodeBox = new ComboBox<>(
                javafx.collections.FXCollections.observableArrayList("Carte", "CIB", "D17"));
        methodeBox.setValue("Carte");
        methodeBox.setMaxWidth(Double.MAX_VALUE);

        Label titleLbl = new Label("💳  Nouveau mode de paiement");
        Label info = new Label("La commande restera EN ATTENTE jusqu'à confirmation de la livraison.");
        info.setWrapText(true);

        VBox content = new VBox(14, titleLbl, methodeBox, info);
        content.setPadding(new Insets(20)); content.setMinWidth(320);

        DialogPane pane = dialog.getDialogPane();
        pane.setContent(content);
        pane.getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        String bgMain = isDarkMode ? "#110F26" : "#ffffff";
        String border = isDarkMode ? "#4f46e5" : "#a5b4fc";
        String textPrimary = isDarkMode ? "#FFFFFF" : "#1e1b4b";
        String textSecond  = isDarkMode ? "#C4C4E0" : "#4B5563";

        pane.setStyle("-fx-background-color:" + bgMain + ";-fx-border-color:" + border +
                ";-fx-border-width:1.5;-fx-font-family:'Segoe UI';");
        content.setStyle("-fx-background-color:transparent;");
        titleLbl.setStyle("-fx-font-size:14px;-fx-font-weight:bold;-fx-text-fill:" + textPrimary + ";");
        info.setStyle("-fx-font-size:12px;-fx-text-fill:" + textSecond + ";");
        methodeBox.setStyle("-fx-background-color:" + (isDarkMode ? "#1e1b4b" : "#f0f0ff") +
                ";-fx-border-color:" + border + ";-fx-border-radius:10;-fx-background-radius:10;" +
                "-fx-text-fill:" + textPrimary + ";-fx-font-size:13px;");
        pane.lookupButton(ButtonType.OK).setStyle(
                "-fx-background-color:linear-gradient(to right,#4f46e5,#6366f1);-fx-text-fill:white;" +
                        "-fx-font-weight:bold;-fx-background-radius:10;-fx-padding:9 28;-fx-cursor:hand;-fx-font-size:13px;");
        pane.lookupButton(ButtonType.CANCEL).setStyle(
                "-fx-background-color:transparent;-fx-text-fill:" + textSecond +
                        ";-fx-border-color:" + border +
                        ";-fx-border-radius:10;-fx-background-radius:10;-fx-padding:9 22;-fx-cursor:hand;-fx-font-size:13px;");

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            String nouvelleMethode = methodeBox.getValue();
            if ("Carte".equals(nouvelleMethode) || "CIB".equals(nouvelleMethode) || "D17".equals(nouvelleMethode)) {
                try {
                    FXMLLoader loader = new FXMLLoader(
                            getClass().getResource("/com/hanouti/hanoutiem4/Paiement.fxml"));
                    Scene scene = new Scene(loader.load(), 1000, 850);
                    PaiementController ctrl = loader.getController();
                    ctrl.setMontantTotal(p.getMontant());
                    ctrl.setUserEmail(UserSession.getInstance().getUserEmail());
                    Stage stage = (Stage) rootPane.getScene().getWindow();
                    stage.setTitle("7anouti-E — Paiement"); stage.setScene(scene);
                } catch (IOException ex) {
                    showAlert("Erreur navigation", "Impossible d'ouvrir le paiement : " + ex.getMessage());
                }
            } else {
                try { dao.modifierMethode(p.getReferenceTransaction(), nouvelleMethode); loadData(); }
                catch (SQLException ex) { showAlert("Erreur", "Impossible de modifier : " + ex.getMessage()); }
            }
        }
    }

    // ── HTML helpers ──────────────────────────────────────────────────────

    private String getPdfCssBase() {
        return "@import url('https://fonts.googleapis.com/css2?family=Inter:wght@400;600;700;800&display=swap');"
                + "*{box-sizing:border-box;margin:0;padding:0;}"
                + "body{font-family:'Inter',system-ui,sans-serif;background:#EFF6FF;padding:24px;}"
                + "@media print{body{background:white;padding:0;} .page{box-shadow:none;} .footer .print-hint{display:none;}}"
                + ".page{max-width:860px;margin:0 auto;background:white;border-radius:12px;"
                + "box-shadow:0 4px 24px rgba(79,70,229,0.10);overflow:hidden;}"
                + ".header{background:#4f46e5;padding:22px 24px;color:white;}"
                + ".logo-row{display:flex;align-items:center;gap:10px;margin-bottom:16px;}"
                + ".logo-box{width:36px;height:36px;border-radius:10px;background:rgba(255,255,255,0.18);"
                + "display:flex;align-items:center;justify-content:center;font-size:18px;}"
                + ".brand{font-size:15px;font-weight:700;color:white;}"
                + ".brand-tag{font-size:10px;color:rgba(255,255,255,0.65);}"
                + ".doc-title{font-size:22px;font-weight:800;color:white;margin-bottom:4px;}"
                + ".doc-sub{font-size:12px;color:rgba(255,255,255,0.70);}"
                + ".meta-row{display:flex;gap:0;margin-top:18px;padding-top:16px;"
                + "border-top:1px solid rgba(255,255,255,0.20);}"
                + ".meta-item{flex:1;text-align:center;}"
                + ".meta-val{font-size:16px;font-weight:700;color:white;}"
                + ".meta-lbl{font-size:10px;color:rgba(255,255,255,0.60);}"
                + ".card{margin:16px;border-radius:10px;border:1px solid #e0e7ff;overflow:hidden;}"
                + ".section{padding:14px 18px;border-bottom:1px solid #f0f0ff;}"
                + ".section-title{font-size:10px;font-weight:700;color:#94a3b8;text-transform:uppercase;"
                + "letter-spacing:.07em;margin-bottom:10px;}"
                + ".badge{display:inline-block;padding:3px 10px;border-radius:20px;font-size:10px;font-weight:700;}"
                + ".badge-v{background:#dcfce7;color:#16a34a;}"
                + ".badge-a{background:#fef9c3;color:#d97706;}"
                + ".badge-x{background:#f3f4f6;color:#6b7280;}"
                + ".mono{font-family:monospace;font-size:12px;font-weight:700;color:#4f46e5;}"
                + ".footer{padding:14px 24px;display:flex;justify-content:space-between;"
                + "font-size:11px;color:#94a3b8;border-top:1px solid #f0f0ff;background:#fafafe;}";
    }

    private String buildInvoiceHtml(Paiement p, List<com.hanouti.hanoutiem4.model.Panier> items,
                                    double total, String dateStr, String qrUrl) {
        String statusBg = "validé".equalsIgnoreCase(p.getStatut()) ? "#dcfce7" : "#fef9c3";
        String statusTxt= "validé".equalsIgnoreCase(p.getStatut()) ? "#16a34a" : "#d97706";
        String statLabel = capitalize(p.getStatut() != null ? p.getStatut() : "—");
        String meth      = p.getMethode() != null ? p.getMethode() : "—";

        StringBuilder prodRows = new StringBuilder();
        for (com.hanouti.hanoutiem4.model.Panier item : items) {
            String emoji = emojiForProduct(item.getNomProduit());
            prodRows.append("<tr>")
                    .append("<td style=\"display:flex;align-items:center;gap:8px;padding:10px 18px;\">"
                            + "<span style=\"font-size:18px;\">" + emoji + "</span>"
                            + "<span style=\"font-size:13px;font-weight:600;color:#1e1b4b;\">"
                            + escHtml(item.getNomProduit()) + "</span></td>")
                    .append("<td style=\"text-align:center;padding:10px 12px;font-size:13px;color:#64748b;\">×")
                    .append(item.getQuantite()).append("</td>")
                    .append("<td style=\"text-align:right;padding:10px 18px;font-size:13px;font-weight:700;color:#4f46e5;\">")
                    .append(fmtDT2(item.getSousTotal())).append("</td>")
                    .append("</tr>\n");
        }

        double eur = total * 0.298, usd = total * 0.346;
        String qrImg = (qrUrl != null && !qrUrl.isEmpty())
                ? "<img src=\"" + qrUrl + "\" width=\"80\" height=\"80\" alt=\"QR Code\" "
                  + "style=\"border-radius:8px;border:3px solid white;\">"
                : "<div style=\"width:80px;height:80px;background:rgba(255,255,255,0.18);border-radius:8px;"
                  + "display:flex;align-items:center;justify-content:center;font-size:30px;\">&#9638;</div>";

        return "<!DOCTYPE html><html lang=\"fr\"><head><meta charset=\"UTF-8\">"
                + "<title>Facture TXN-" + txnId(p) + " · 7anouti-E</title>"
                + "<style>" + getPdfCssBase()
                + "table{width:100%;border-collapse:collapse;}"
                + "th{font-size:11px;font-weight:600;color:#64748b;text-align:left;padding:10px 18px;"
                + "background:#fafafe;border-bottom:1px solid #e0e7ff;}"
                + "tr{border-bottom:1px solid #f8f7ff;}"
                + "tr:last-child{border-bottom:none;}"
                + ".total-block{padding:14px 18px;}"
                + ".t-row{display:flex;justify-content:space-between;font-size:13px;color:#64748b;margin-bottom:6px;}"
                + ".t-total{display:flex;justify-content:space-between;font-size:16px;font-weight:800;"
                + "color:#4f46e5;padding-top:10px;border-top:2px solid #eef2ff;margin-top:6px;}"
                + ".currency{display:flex;gap:10px;margin-top:12px;}"
                + ".cur-pill{background:#eef2ff;border-radius:8px;padding:6px 14px;text-align:center;min-width:90px;}"
                + ".cur-flag{font-size:16px;display:block;}"
                + ".cur-amt{font-size:13px;font-weight:700;color:#4f46e5;display:block;}"
                + ".cur-code{font-size:10px;color:#94a3b8;display:block;}"
                + ".qr-section{display:flex;align-items:center;gap:16px;padding:16px 18px;"
                + "background:#4f46e5;margin:16px;border-radius:10px;}"
                + ".client-row{display:flex;align-items:center;gap:10px;}"
                + ".avatar{width:38px;height:38px;border-radius:50%;background:#eef2ff;"
                + "display:flex;align-items:center;justify-content:center;"
                + "font-size:13px;font-weight:700;color:#4f46e5;flex-shrink:0;}"
                + "</style></head><body><div class=\"page\">"
                + "<div class=\"header\">"
                + "<div class=\"logo-row\"><div class=\"logo-box\">&#x1F6CD;</div>"
                + "<div><div class=\"brand\">7anouti-E</div><div class=\"brand-tag\">Facture officielle</div></div>"
                + "<div style=\"margin-left:auto;text-align:right\">"
                + "<div style=\"font-size:10px;opacity:.65;\">Référence</div>"
                + "<div style=\"font-size:14px;font-weight:700;\">TXN-" + txnId(p) + "</div></div></div>"
                + "<div class=\"doc-title\">Facture</div>"
                + "<div class=\"doc-sub\">" + dateStr + " · " + meth + "</div>"
                + "<div style=\"margin-top:14px;display:inline-block;background:" + statusBg + ";"
                + "color:" + statusTxt + ";padding:5px 16px;border-radius:20px;font-size:12px;font-weight:700;\">"
                + statLabel + "</div></div>"
                // Client section
                + "<div class=\"card\"><div class=\"section\">"
                + "<div class=\"section-title\">Client</div>"
                + "<div class=\"client-row\"><div class=\"avatar\">"
                + UserSession.getInstance().getUserName().substring(0,1).toUpperCase()
                + (UserSession.getInstance().getUserName().length()>1
                ? String.valueOf(UserSession.getInstance().getUserName().charAt(
                UserSession.getInstance().getUserName().indexOf(' ')>0
                ? UserSession.getInstance().getUserName().indexOf(' ')+1 : 1)).toUpperCase()
                : "") + "</div>"
                + "<div><div style=\"font-size:13px;font-weight:700;color:#1e1b4b;\">"
                + escHtml(UserSession.getInstance().getUserName()) + "</div>"
                + "<div style=\"font-size:11px;color:#94a3b8;\">"
                + escHtml(UserSession.getInstance().getUserEmail()) + "</div></div></div></div>"
                // Products table
                + "<div class=\"section\" style=\"padding:0;\"><div class=\"section-title\" style=\"padding:12px 18px 0;\">"
                + "Articles achetés</div>"
                + "<table><thead><tr><th>Produit</th>"
                + "<th style=\"text-align:center;padding:10px 12px;\">Qté</th>"
                + "<th style=\"text-align:right;padding:10px 18px;\">Total</th></tr></thead>"
                + "<tbody>" + prodRows + "</tbody></table></div>"
                // Totals
                + "<div class=\"total-block\">"
                + "<div class=\"t-row\"><span>Livraison</span><span>7,00 DT</span></div>"
                + "<div class=\"t-total\"><span>Total payé</span><span>" + fmtDT(total) + "</span></div>"
                + "<div class=\"currency\">"
                + "<div class=\"cur-pill\"><span class=\"cur-flag\">&#x1F1F9;&#x1F1F3;</span>"
                + "<span class=\"cur-amt\">" + fmtDT(total).replace(" DT","") + "</span>"
                + "<span class=\"cur-code\">TND</span></div>"
                + "<div class=\"cur-pill\"><span class=\"cur-flag\">&#x1F1EA;&#x1F1FA;</span>"
                + "<span class=\"cur-amt\">" + String.format("%.2f",eur) + "</span>"
                + "<span class=\"cur-code\">EUR</span></div>"
                + "<div class=\"cur-pill\"><span class=\"cur-flag\">&#x1F1FA;&#x1F1F8;</span>"
                + "<span class=\"cur-amt\">" + String.format("%.2f",usd) + "</span>"
                + "<span class=\"cur-code\">USD</span></div></div></div></div>"
                // QR section
                + "<div class=\"qr-section\">" + qrImg
                + "<div><div style=\"font-size:14px;font-weight:700;color:white;margin-bottom:4px;\">"
                + "QR Code · Vérification</div>"
                + "<div style=\"font-size:11px;color:rgba(255,255,255,.70);\">"
                + "Scannez pour vérifier cette facture en ligne</div>"
                + "<div style=\"font-size:11px;color:rgba(255,255,255,.55);margin-top:4px;\">"
                + "Réf : TXN-" + txnId(p) + "</div></div></div>"
                + "<div class=\"footer\">"
                + "<span>&#x1F512; Facture officielle · 7anouti-E</span>"
                + "<span>Ctrl+P pour imprimer en PDF</span></div>"
                + "</div></body></html>";
    }

    private void exportInvoiceHtml(Paiement p) {
        try {
            List<com.hanouti.hanoutiem4.model.Panier> items = getItemsForPaiement(p);
            double total = p.getMontant();
            SimpleDateFormat sdf = new SimpleDateFormat("dd MMMM yyyy 'à' HH:mm", java.util.Locale.FRANCE);
            String dateStr = p.getDatePaiement() != null ? sdf.format(p.getDatePaiement()) : "Date inconnue";

            // ── Start local QR server so phone can scan and see the invoice ──
            String localIp  = getLocalIp();
            String serverUrl = "http://" + localIp + ":" + QR_SERVER_PORT + "/facture/" + txnId(p);
            startQrServer(p, items, total, dateStr, serverUrl);

            // ── QR code encodes the local server URL ──
            String qrEncoded = java.net.URLEncoder.encode(serverUrl, StandardCharsets.UTF_8);
            String qrUrl = "https://quickchart.io/qr?text=" + qrEncoded
                    + "&size=140&margin=1&format=png&ecLevel=M";

            // ── Build and open invoice HTML in browser ──
            String html = buildInvoiceHtml(p, items, total, dateStr, qrUrl);
            String fname = "facture_TXN-" + txnId(p) + "_"
                    + new SimpleDateFormat("yyyyMMdd").format(new java.util.Date()) + ".html";
            java.io.File tmp = new java.io.File(System.getProperty("java.io.tmpdir"), fname);
            try (java.io.PrintWriter pw = new java.io.PrintWriter(tmp, StandardCharsets.UTF_8)) {
                pw.print(html);
            }
            try { java.awt.Desktop.getDesktop().browse(tmp.toURI()); }
            catch (Exception ignored) {}

            showStyledAlert("✓ Facture ouverte !",
                    "Facture TXN-" + txnId(p) + " ouverte dans le navigateur.\n"
                            + "Scannez le QR code avec votre téléphone (même WiFi)\n"
                            + "pour voir la facture sur mobile — valide 5 minutes.\n"
                            + "Ctrl+P pour enregistrer en PDF.",
                    "#16a34a");
        } catch (Exception ex) {
            showAlert("Erreur", "Impossible de générer la facture : " + ex.getMessage());
        }
    }

    // ── QR Code local HTTP server ────────────────────────────────────────────
    //  Starts a tiny HTTP server on localhost:7472 that serves the invoice HTML.
    //  The QR code encodes http://<local-ip>:7472/facture/<txnId>
    //  Works when phone and PC are on the same WiFi network.
    //  Server auto-stops after 5 minutes.

    private void startQrServer(Paiement p, List<com.hanouti.hanoutiem4.model.Panier> items,
                               double total, String dateStr, String baseUrl) {
        // Stop any previously running server
        stopQrServer();
        try {
            // QR code on phone version encodes the transaction reference
            String qrTxnEncoded = java.net.URLEncoder.encode("TXN-" + txnId(p), StandardCharsets.UTF_8);
            String qrTxnUrl = "https://quickchart.io/qr?text=" + qrTxnEncoded
                    + "&size=140&margin=1&format=png&ecLevel=M";
            String qrInvoiceHtml = buildInvoiceHtml(p, items, total, dateStr, qrTxnUrl);

            activeQrServer = HttpServer.create(new InetSocketAddress(QR_SERVER_PORT), 0);
            final String txn = txnId(p);

            // Route: /facture/<txnId> → full beautiful invoice HTML
            activeQrServer.createContext("/facture/" + txn, (HttpExchange ex) -> {
                byte[] bytes = qrInvoiceHtml.getBytes(StandardCharsets.UTF_8);
                ex.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
                ex.sendResponseHeaders(200, bytes.length);
                try (var os = ex.getResponseBody()) { os.write(bytes); }
            });
            // Route: / → redirect to invoice
            activeQrServer.createContext("/", (HttpExchange ex) -> {
                String redirect = "<html><head><meta http-equiv=\"refresh\" content=\"0;url=/facture/"
                        + txn + "\"></head><body></body></html>";
                byte[] bytes = redirect.getBytes(StandardCharsets.UTF_8);
                ex.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
                ex.sendResponseHeaders(200, bytes.length);
                try (var os = ex.getResponseBody()) { os.write(bytes); }
            });

            activeQrServer.setExecutor(Executors.newSingleThreadExecutor());
            activeQrServer.start();

            // Auto-stop after 5 minutes
            new Thread(() -> {
                try { Thread.sleep(5 * 60 * 1000); } catch (InterruptedException ignored) {}
                stopQrServer();
            }).start();
        } catch (Exception e) {
            System.err.println("[QR Server] Failed to start: " + e.getMessage());
        }
    }

    private void stopQrServer() {
        if (activeQrServer != null) {
            try { activeQrServer.stop(0); } catch (Exception ignored) {}
            activeQrServer = null;
        }
    }

    private String getLocalIp() {
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (Exception e) {
            return "192.168.1.1";
        }
    }

    private String escHtml(String s) {
        if (s == null) return "";
        return s.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;");
    }

    // ══════════════════════════════════════════════════════════════════════
    //  FACTURE POPUP — fully preserved, indigo palette applied
    // ══════════════════════════════════════════════════════════════════════
    private void showFacturePopup(Paiement p) {
        boolean dark  = isDarkMode;
        String bgCard = dark ? "#110F26" : "#FFFFFF";
        String bgHead = "linear-gradient(to right, #4f46e5, #6366f1)";
        String bgBody = dark ? "#1A1830" : "#F8FAFF";
        String t1     = dark ? "#F1F0FF" : "#1E3A5F";
        String t3     = dark ? "rgba(241,240,255,0.50)" : "#6B7280";
        String divClr = dark ? "rgba(255,255,255,0.07)" : "rgba(99,102,241,0.10)";
        String rowBg  = dark ? "rgba(255,255,255,0.03)" : "#FFFFFF";
        String rowHov = dark ? "rgba(255,255,255,0.07)" : "#EFF6FF";

        StackPane overlay = new StackPane();
        overlay.setStyle("-fx-background-color: rgba(0,0,0,0.55);");
        javafx.scene.layout.AnchorPane.setTopAnchor(overlay,0.0);
        javafx.scene.layout.AnchorPane.setBottomAnchor(overlay,0.0);
        javafx.scene.layout.AnchorPane.setLeftAnchor(overlay,0.0);
        javafx.scene.layout.AnchorPane.setRightAnchor(overlay,0.0);
        overlay.setOpacity(0);

        VBox card = new VBox(0);
        card.setMaxWidth(620); card.setMinWidth(500);
        card.setStyle("-fx-background-color:" + bgCard + ";-fx-background-radius:22;" +
                "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.50),60,0,0,16);");
        card.setScaleX(0.84); card.setScaleY(0.84); card.setOpacity(0);

        // ── Header ──
        VBox header = new VBox(6);
        header.setStyle("-fx-background-color:" + bgHead + ";-fx-background-radius:22 22 0 0;-fx-padding:22 26 18 26;");
        HBox topRow = new HBox(); topRow.setAlignment(Pos.CENTER_RIGHT);
        Button closeBtn = new Button("✕");
        closeBtn.setStyle("-fx-background-color:rgba(255,255,255,0.18);-fx-text-fill:white;" +
                "-fx-font-size:11px;-fx-font-weight:bold;-fx-background-radius:8;-fx-padding:4 9;-fx-cursor:hand;");
        topRow.getChildren().add(closeBtn);

        HBox titleRow = new HBox(12); titleRow.setAlignment(Pos.CENTER_LEFT);
        Label docIcon = new Label("📄"); docIcon.setStyle("-fx-font-size:28px;");
        VBox titleInfo = new VBox(3);
        Label titleLbl = new Label("Facture — Transaction #TXN-" + txnId(p));
        titleLbl.setStyle("-fx-text-fill:white;-fx-font-size:16px;-fx-font-weight:bold;");
        titleLbl.setWrapText(true);

        SimpleDateFormat sdf2 = new SimpleDateFormat("dd/MM/yyyy HH:mm");
        String dateStr2 = p.getDatePaiement() != null ? sdf2.format(p.getDatePaiement()) : "Date inconnue";
        Label dateLbl2 = new Label("🗓  " + dateStr2 + "   •   " + UserSession.getInstance().getUserName());
        dateLbl2.setStyle("-fx-text-fill:rgba(255,255,255,0.72);-fx-font-size:11px;");
        titleInfo.getChildren().addAll(titleLbl, dateLbl2);
        titleRow.getChildren().addAll(docIcon, titleInfo);
        header.getChildren().addAll(topRow, titleRow);

        // ── Body ──
        ScrollPane scroll = new ScrollPane();
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setStyle("-fx-background:" + bgBody + ";-fx-background-color:" + bgBody + ";-fx-border-color:transparent;");
        scroll.setMaxHeight(540);

        VBox body = new VBox(0);
        body.setStyle("-fx-background-color:" + bgBody + ";-fx-padding:0 0 10 0;");

        // Client section
        VBox buyerSection = new VBox(6);
        buyerSection.setStyle("-fx-padding:16 26 14 26;-fx-background-color:" + rowBg + ";");
        Label buyerTitle = new Label("CLIENT");
        buyerTitle.setStyle("-fx-font-size:9px;-fx-font-weight:bold;-fx-text-fill:" + t3 + ";");
        HBox buyerRow = new HBox(20); buyerRow.setAlignment(Pos.CENTER_LEFT);
        Label buyerName  = new Label("👤  " + UserSession.getInstance().getUserName());
        buyerName.setStyle("-fx-font-size:13px;-fx-font-weight:bold;-fx-text-fill:" + t1 + ";");
        Label buyerEmail = new Label("✉  " + UserSession.getInstance().getUserEmail());
        buyerEmail.setStyle("-fx-font-size:12px;-fx-text-fill:" + t3 + ";");
        buyerRow.getChildren().addAll(buyerName, buyerEmail);
        buyerSection.getChildren().addAll(buyerTitle, buyerRow);

        Separator sep1 = new Separator(); sep1.setStyle("-fx-background-color:" + divClr + ";");

        // Table header
        HBox tableHead = new HBox();
        tableHead.setStyle("-fx-padding:10 26 8 26;-fx-background-color:" +
                (dark ? "rgba(99,102,241,0.15)" : "rgba(99,102,241,0.06)") + ";");
        Label hProd = makeTableHeader("PRODUIT", t3); HBox.setHgrow(hProd, Priority.ALWAYS);
        Label hQty  = makeTableHeader("QTÉ",    t3); hQty.setPrefWidth(50);  hQty.setAlignment(Pos.CENTER_RIGHT);
        Label hPrix = makeTableHeader("P.UNIT", t3); hPrix.setPrefWidth(90); hPrix.setAlignment(Pos.CENTER_RIGHT);
        Label hTot  = makeTableHeader("TOTAL",  t3); hTot.setPrefWidth(90);  hTot.setAlignment(Pos.CENTER_RIGHT);
        tableHead.getChildren().addAll(hProd, hQty, hPrix, hTot);

        // Product rows
        VBox productRows = new VBox(0);
        double totalTND = 0.0;
        try {
            List<com.hanouti.hanoutiem4.model.Panier> items = getItemsForPaiement(p);
            if (items.isEmpty()) {
                productRows.getChildren().add(buildProductRow(
                        "Commande #" + p.getCommandeId(), 1, p.getMontant(), p.getMontant(),
                        rowBg, rowHov, t1, t3, divClr, true));
                totalTND = p.getMontant();
            } else {
                for (int i = 0; i < items.size(); i++) {
                    com.hanouti.hanoutiem4.model.Panier item = items.get(i);
                    boolean last = (i == items.size() - 1);
                    productRows.getChildren().add(buildProductRow(
                            item.getNomProduit(), item.getQuantite(),
                            item.getPrixUnitaire(), item.getSousTotal(),
                            rowBg, rowHov, t1, t3, divClr, last));
                    totalTND += item.getSousTotal();
                }
            }
        } catch (Exception ex) {
            totalTND = p.getMontant();
            productRows.getChildren().add(buildProductRow(
                    "Impossible de charger les articles", 0, 0, p.getMontant(),
                    rowBg, rowHov, "#EF4444", t3, divClr, true));
        }

        // Currency section
        double rateEUR = 0.298, rateUSD = 0.346;
        VBox currencySection = new VBox(8);
        currencySection.setStyle("-fx-padding:14 26 14 26;-fx-background-color:" + rowBg + ";");
        Label currTitle = new Label("MONTANT TOTAL");
        currTitle.setStyle("-fx-font-size:9px;-fx-font-weight:bold;-fx-text-fill:" + t3 + ";");
        HBox currRow = new HBox(10); currRow.setAlignment(Pos.CENTER_LEFT);
        VBox tndBox = makeCurrencyBox("TND", String.format("%.3f", totalTND), "#4f46e5", dark);
        Label eq = new Label("="); eq.setStyle("-fx-font-size:16px;-fx-text-fill:" + t3 + ";-fx-padding:0 4;");
        VBox eurBox = makeCurrencyBox("EUR", String.format("%.2f", totalTND * rateEUR), "#059669", dark);
        Label sl = new Label("/"); sl.setStyle("-fx-font-size:16px;-fx-text-fill:" + t3 + ";-fx-padding:0 4;");
        VBox usdBox = makeCurrencyBox("USD", String.format("%.2f", totalTND * rateUSD), "#4f46e5", dark);
        Label rateNote = new Label("Taux moyens (mai 2026) : 1 TND ≈ " + rateEUR + " EUR  /  " + rateUSD + " USD");
        rateNote.setStyle("-fx-font-size:9px;-fx-text-fill:" + t3 + ";-fx-font-style:italic;");
        currRow.getChildren().addAll(tndBox, eq, eurBox, sl, usdBox);
        currencySection.getChildren().addAll(currTitle, currRow, rateNote);

        Separator sep4 = new Separator(); sep4.setStyle("-fx-background-color:" + divClr + ";");

        // Method + status row
        HBox metaRow = new HBox(20); metaRow.setAlignment(Pos.CENTER_LEFT);
        metaRow.setStyle("-fx-padding:12 26 12 26;-fx-background-color:" + rowBg + ";");
        Label methLbl3 = new Label("💳  " + p.getMethode());
        methLbl3.setStyle("-fx-font-size:12px;-fx-font-weight:bold;-fx-text-fill:#4f46e5;");
        String sc = statusColor(p.getStatut());
        Label statLbl = new Label(p.getStatut().toUpperCase());
        statLbl.setStyle("-fx-font-size:10px;-fx-font-weight:bold;-fx-text-fill:" + sc +
                ";-fx-background-color:" + sc + "22;-fx-background-radius:20;-fx-padding:3 10;");
        metaRow.getChildren().addAll(methLbl3, statLbl);

        Separator sep5 = new Separator(); sep5.setStyle("-fx-background-color:" + divClr + ";");

        // QR Code
        String qrContent = "7anouti-E | Facture | TXN-" + txnId(p) +
                " | Client: " + UserSession.getInstance().getUserName() +
                " | Date: " + dateStr2 +
                " | Montant: " + String.format("%.3f TND", totalTND) +
                " | Methode: " + p.getMethode() + " | Statut: " + p.getStatut();

        HBox qrSection = new HBox(20); qrSection.setAlignment(Pos.CENTER_LEFT);
        qrSection.setStyle("-fx-padding:18 26 18 26;-fx-background-color:" + rowBg + ";");

        Label noQrFallback = new Label("⏳ Chargement QR...");
        noQrFallback.setStyle("-fx-text-fill:" + t3 + ";-fx-font-size:11px;");

        VBox qrFrame = new VBox(); qrFrame.setAlignment(Pos.CENTER);
        qrFrame.setStyle("-fx-background-color:#FFFFFF;-fx-background-radius:12;-fx-padding:10;" +
                "-fx-effect:dropshadow(gaussian,rgba(79,70,229,0.25),12,0,0,4);");
        qrFrame.setMinWidth(130); qrFrame.setMinHeight(130);

        VBox qrInfo = new VBox(8); qrInfo.setAlignment(Pos.CENTER_LEFT); HBox.setHgrow(qrInfo, Priority.ALWAYS);
        Label qrTitle2 = new Label("QR CODE · FACTURE");
        qrTitle2.setStyle("-fx-font-size:9px;-fx-font-weight:bold;-fx-text-fill:" + t3 + ";");
        Label qrRefLbl = new Label("Ref : TXN-" + txnId(p));
        qrRefLbl.setStyle("-fx-font-size:11px;-fx-font-weight:bold;-fx-text-fill:" + t1 + ";");
        qrRefLbl.setWrapText(true);
        Label qrDesc = new Label("Scannez ce code pour obtenir les\ndétails complètes de cette facture.");
        qrDesc.setStyle("-fx-font-size:11px;-fx-text-fill:" + t3 + ";"); qrDesc.setWrapText(true);
        Label qrNote = new Label("🏪 7anouti-E — Votre marché en ligne");
        qrNote.setStyle("-fx-font-size:10px;-fx-text-fill:#4f46e5;-fx-font-weight:bold;");
        qrInfo.getChildren().addAll(qrTitle2, qrRefLbl, qrDesc, qrNote);
        qrSection.getChildren().addAll(qrFrame, qrInfo);
        loadQRCodeAsync(qrContent, 130, qrFrame, qrSection, noQrFallback);

        body.getChildren().addAll(buyerSection, sep1, tableHead, productRows,
                new Separator(), currencySection, sep4, metaRow, sep5, qrSection);
        scroll.setContent(body);

        // ── Footer ──
        HBox footer = new HBox(10); footer.setAlignment(Pos.CENTER_RIGHT);
        footer.setStyle("-fx-padding:14 24 20 24;-fx-background-color:" + bgCard + ";");

        boolean canModify = "en attente".equalsIgnoreCase(p.getStatut()) && "Espèces".equals(p.getMethode());
        if (canModify) {
            Button modifierBtn = new Button("Modifier le paiement");
            modifierBtn.setStyle("-fx-background-color:rgba(99,102,241,0.08);-fx-text-fill:#4f46e5;" +
                    "-fx-font-size:12px;-fx-font-weight:bold;-fx-background-radius:10;" +
                    "-fx-border-color:rgba(99,102,241,0.30);-fx-border-width:1;-fx-border-radius:10;" +
                    "-fx-padding:9 20;-fx-cursor:hand;");
            modifierBtn.setOnAction(e -> {
                rootPane.getChildren().remove(overlay);
                showEditDialog(p);
            });
            footer.getChildren().add(modifierBtn);
            Region footSpacer = new Region(); HBox.setHgrow(footSpacer, Priority.ALWAYS);
            footer.getChildren().add(footSpacer);
        }

        Button exportInvBtn = new Button("⬇ Exporter en PDF");
        exportInvBtn.setStyle(BTN_RESET +
                "-fx-background-color:transparent;-fx-background-radius:10;" +
                "-fx-border-color:rgba(255,255,255,0.55);-fx-border-width:1;-fx-border-radius:10;" +
                "-fx-text-fill:white;-fx-font-size:12px;-fx-font-weight:600;-fx-padding:9 20;-fx-cursor:hand;");
        exportInvBtn.setStyle(BTN_RESET +
                "-fx-background-color:rgba(99,102,241,0.12);-fx-background-radius:10;" +
                "-fx-border-color:rgba(99,102,241,0.35);-fx-border-width:1;-fx-border-radius:10;" +
                "-fx-text-fill:#4f46e5;-fx-font-size:12px;-fx-font-weight:600;-fx-padding:9 20;-fx-cursor:hand;");
        exportInvBtn.setOnAction(ev -> {
            rootPane.getChildren().remove(overlay);
            exportInvoiceHtml(p);
        });
        footer.getChildren().add(exportInvBtn);

        Button closeBtn2 = new Button("Fermer");
        closeBtn2.setStyle("-fx-background-color:linear-gradient(to right,#4f46e5,#6366f1);" +
                "-fx-text-fill:white;-fx-font-size:13px;-fx-font-weight:bold;" +
                "-fx-background-radius:12;-fx-padding:10 32;-fx-cursor:hand;");
        footer.getChildren().add(closeBtn2);

        card.getChildren().addAll(header, scroll, footer);
        overlay.getChildren().add(card);
        rootPane.getChildren().add(overlay);

        // Animations
        FadeTransition fIn = new FadeTransition(Duration.millis(200), overlay); fIn.setToValue(1);
        ScaleTransition sIn = new ScaleTransition(Duration.millis(260), card);
        sIn.setToX(1); sIn.setToY(1); sIn.setInterpolator(Interpolator.EASE_OUT);
        FadeTransition pIn = new FadeTransition(Duration.millis(260), card); pIn.setToValue(1);
        new ParallelTransition(fIn, sIn, pIn).play();

        Runnable dismiss = () -> {
            FadeTransition fo = new FadeTransition(Duration.millis(170), overlay); fo.setToValue(0);
            ScaleTransition so = new ScaleTransition(Duration.millis(150), card); so.setToX(0.88); so.setToY(0.88);
            ParallelTransition pt = new ParallelTransition(fo, so);
            pt.setOnFinished(ev -> rootPane.getChildren().remove(overlay));
            pt.play();
        };
        closeBtn.setOnAction(e -> dismiss.run());
        closeBtn2.setOnAction(e -> dismiss.run());
        overlay.setOnMouseClicked(e -> { if (e.getTarget() == overlay) dismiss.run(); });
    }

    // ── Preserved helper methods ──────────────────────────────────────────

    private List<com.hanouti.hanoutiem4.model.Panier> getItemsForPaiement(Paiement p)
            throws java.sql.SQLException {
        List<com.hanouti.hanoutiem4.model.Panier> list = new java.util.ArrayList<>();
        try {
            String sql = "SELECT * FROM lignes_commande WHERE reference_transaction = ?";
            try (java.sql.PreparedStatement ps =
                         com.hanouti.hanoutiem4.util.DBConnection.getInstance()
                                 .getConnection().prepareStatement(sql)) {
                ps.setString(1, p.getReferenceTransaction());
                try (java.sql.ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        com.hanouti.hanoutiem4.model.Panier item = new com.hanouti.hanoutiem4.model.Panier(
                                0, rs.getInt("produit_id"),
                                rs.getInt("quantite"), rs.getDouble("prix_unitaire"));
                        item.setNomProduit(rs.getString("nom_produit"));
                        list.add(item);
                    }
                }
            }
        } catch (Exception ignored) {}
        return list;
    }

    private Label makeTableHeader(String text, String color) {
        Label l = new Label(text);
        l.setStyle("-fx-font-size:9px;-fx-font-weight:bold;-fx-text-fill:" + color + ";");
        return l;
    }

    private HBox buildProductRow(String nom, int qty, double prixUnit, double total,
                                 String rowBg, String rowHov, String t1, String t3, String divClr, boolean last) {
        HBox row = new HBox(); row.setAlignment(Pos.CENTER_LEFT);
        String s = "-fx-padding:11 26;-fx-background-color:" + rowBg + ";" +
                (last ? "" : "-fx-border-color:" + divClr + ";-fx-border-width:0 0 1 0;");
        row.setStyle(s);
        row.setOnMouseEntered(e -> row.setStyle("-fx-padding:11 26;-fx-background-color:" + rowHov + ";" +
                (last ? "" : "-fx-border-color:" + divClr + ";-fx-border-width:0 0 1 0;")));
        row.setOnMouseExited(e  -> row.setStyle(s));
        Label nomLbl = new Label(nom);
        nomLbl.setStyle("-fx-font-size:12px;-fx-font-weight:bold;-fx-text-fill:" + t1 + ";");
        nomLbl.setWrapText(true); HBox.setHgrow(nomLbl, Priority.ALWAYS);
        Label qtyLbl = new Label(qty > 0 ? String.valueOf(qty) : "—");
        qtyLbl.setStyle("-fx-font-size:12px;-fx-text-fill:" + t3 + ";");
        qtyLbl.setPrefWidth(50); qtyLbl.setAlignment(Pos.CENTER_RIGHT);
        Label prixLbl = new Label(qty > 0 ? String.format("%.2f", prixUnit) : "—");
        prixLbl.setStyle("-fx-font-size:12px;-fx-text-fill:" + t3 + ";");
        prixLbl.setPrefWidth(90); prixLbl.setAlignment(Pos.CENTER_RIGHT);
        Label totalLbl = new Label(String.format("%.2f TND", total));
        totalLbl.setStyle("-fx-font-size:12px;-fx-font-weight:bold;-fx-text-fill:#4f46e5;");
        totalLbl.setPrefWidth(90); totalLbl.setAlignment(Pos.CENTER_RIGHT);
        row.getChildren().addAll(nomLbl, qtyLbl, prixLbl, totalLbl);
        return row;
    }

    private VBox makeCurrencyBox(String currency, String amount, String color, boolean dark) {
        VBox box = new VBox(2); box.setAlignment(Pos.CENTER_LEFT);
        box.setStyle("-fx-background-color:" + color + "18;-fx-background-radius:10;-fx-padding:8 14;-fx-min-width:100;");
        String flag = "TND".equals(currency) ? "🇹🇳" : "EUR".equals(currency) ? "🇪🇺" : "🇺🇸";
        Label flagLbl = new Label(flag); flagLbl.setStyle("-fx-font-size:12px;");
        Label amtLbl = new Label(amount);
        amtLbl.setStyle("-fx-font-size:16px;-fx-font-weight:bold;-fx-text-fill:" + color + ";");
        Label currLbl = new Label(currency);
        currLbl.setStyle("-fx-font-size:9px;-fx-font-weight:bold;-fx-text-fill:" + color + ";-fx-opacity:0.75;");
        box.getChildren().addAll(flagLbl, amtLbl, currLbl);
        return box;
    }

    private void loadQRCodeAsync(String content, int size, VBox qrFrame,
                                 HBox qrSection, Label noQrFallback) {
        String qrUrl;
        try {
            String encoded = java.net.URLEncoder.encode(content, StandardCharsets.UTF_8);
            qrUrl = "https://quickchart.io/qr?text=" + encoded + "&size=" + size + "&margin=1&format=png";
        } catch (Exception e) { return; }
        final String url = qrUrl;
        new Thread(() -> {
            try {
                javafx.scene.image.Image img = new javafx.scene.image.Image(url, size, size, true, true);
                Platform.runLater(() -> {
                    if (!img.isError()) {
                        ImageView iv = new ImageView(img);
                        iv.setFitWidth(size); iv.setFitHeight(size);
                        qrFrame.getChildren().clear();
                        qrFrame.getChildren().add(iv);
                        if (!qrSection.getChildren().contains(qrFrame))
                            qrSection.getChildren().add(0, qrFrame);
                        qrSection.getChildren().remove(noQrFallback);
                    }
                });
            } catch (Exception e) {
                System.err.println("[QR] Erreur chargement: " + e.getMessage());
            }
        }).start();
    }

    // ── SVG helpers ───────────────────────────────────────────────────────

    /** Filled (not stroked) SVG icon — for button graphics */
    private SVGPath buildBtnIcon(String d, String fillColor, double size) {
        SVGPath p = new SVGPath();
        p.setContent(d);
        p.setFill(Color.web(fillColor));
        p.setStroke(Color.TRANSPARENT);
        double s = size / 24.0; p.setScaleX(s); p.setScaleY(s);
        return p;
    }

    /** Stroked (outline) SVG icon — for decorative icons */
    private SVGPath svgIcon(String d, String strokeColor, double size) {
        SVGPath p = new SVGPath();
        p.setContent(d);
        p.setFill(Color.TRANSPARENT);
        p.setStroke(Color.web(strokeColor));
        p.setStrokeWidth(1.9);
        p.setStrokeLineCap(StrokeLineCap.ROUND);
        p.setStrokeLineJoin(StrokeLineJoin.ROUND);
        double s = size / 24.0; p.setScaleX(s); p.setScaleY(s);
        return p;
    }

    // ── Stagger animation ─────────────────────────────────────────────────

    private void staggerCards(javafx.collections.ObservableList<javafx.scene.Node> nodes) {
        for (int i = 0; i < nodes.size(); i++) {
            javafx.scene.Node card = nodes.get(i);
            card.setOpacity(0); card.setTranslateY(20);
            FadeTransition f = new FadeTransition(Duration.millis(360), card);
            f.setFromValue(0); f.setToValue(1);
            TranslateTransition t = new TranslateTransition(Duration.millis(360), card);
            t.setFromY(20); t.setToY(0); t.setInterpolator(Interpolator.EASE_OUT);
            ParallelTransition pt = new ParallelTransition(f, t);
            pt.setDelay(Duration.millis(80 + i * 55));
            pt.play();
        }
    }
    // ══════════════════════════════════════════════════════════════════
    // ANALYSE IA — Groq llama-3.3-70b
    // ══════════════════════════════════════════════════════════════════
    @FXML private void handleAnalyseIA() {
        // Show loading overlay
        showStyledAlert("✨ Analyse en cours...",
                "L'IA analyse tes " + (allData != null ? allData.size() : 0) + " transactions. Patiente quelques secondes...", "#4f46e5");

        new Thread(() -> {
            try {
                // ── Collect stats from paiements ─────────────────────
                java.sql.Connection conn = com.hanouti.hanoutiem4.util.DBConnection.getInstance().getConnection();

                // Total & count this month
                String sqlMonth =
                        "SELECT COUNT(*) as nb, COALESCE(SUM(montant),0) as total " +
                                "FROM paiements WHERE user_id = ? " +
                                "AND MONTH(date_paiement)=MONTH(NOW()) AND YEAR(date_paiement)=YEAR(NOW())";
                int nbMois = 0; double totalMois = 0;
                try (java.sql.PreparedStatement ps = conn.prepareStatement(sqlMonth)) {
                    ps.setInt(1, currentUserId);
                    java.sql.ResultSet rs = ps.executeQuery();
                    if (rs.next()) { nbMois = rs.getInt("nb"); totalMois = rs.getDouble("total"); }
                }

                // Last month
                String sqlLastMonth =
                        "SELECT COALESCE(SUM(montant),0) as total FROM paiements WHERE user_id = ? " +
                                "AND MONTH(date_paiement)=MONTH(DATE_SUB(NOW(), INTERVAL 1 MONTH)) " +
                                "AND YEAR(date_paiement)=YEAR(DATE_SUB(NOW(), INTERVAL 1 MONTH))";
                double totalDernier = 0;
                try (java.sql.PreparedStatement ps = conn.prepareStatement(sqlLastMonth)) {
                    ps.setInt(1, currentUserId);
                    java.sql.ResultSet rs = ps.executeQuery();
                    if (rs.next()) totalDernier = rs.getDouble("total");
                }

                // Preferred method
                String sqlMethod =
                        "SELECT methode, COUNT(*) as nb FROM paiements WHERE user_id = ? " +
                                "GROUP BY methode ORDER BY nb DESC LIMIT 1";
                String methodePref = "inconnue";
                try (java.sql.PreparedStatement ps = conn.prepareStatement(sqlMethod)) {
                    ps.setInt(1, currentUserId);
                    java.sql.ResultSet rs = ps.executeQuery();
                    if (rs.next()) methodePref = rs.getString("methode");
                }

                // Total all time
                String sqlTotal =
                        "SELECT COUNT(*) as nb, COALESCE(SUM(montant),0) as total " +
                                "FROM paiements WHERE user_id = ? AND statut = 'valide'";
                int nbTotal = 0; double totalAll = 0;
                try (java.sql.PreparedStatement ps = conn.prepareStatement(sqlTotal)) {
                    ps.setInt(1, currentUserId);
                    java.sql.ResultSet rs = ps.executeQuery();
                    if (rs.next()) { nbTotal = rs.getInt("nb"); totalAll = rs.getDouble("total"); }
                }

                double variation = totalDernier > 0
                        ? ((totalMois - totalDernier) / totalDernier) * 100 : 0;

                // ── Build prompt ──────────────────────────────────────
                String systemPrompt =
                        "Tu es un assistant shopping enthousiaste pour 7anouti-E, une application e-commerce tunisienne. " +
                                "Ton rôle est de féliciter le client pour ses achats, valoriser ses choix, " +
                                "et l'encourager à continuer à profiter des offres de la boutique. " +
                                "Tu ne donnes JAMAIS de conseils pour réduire les dépenses. " +
                                "Réponds en français, de façon chaleureuse, positive et courte. " +
                                "2 paragraphes maximum. Pas de markdown. Sois précis avec les chiffres.";

                String userMsg = String.format(
                        "Voici les achats du client ce mois :\n" +
                                "- Ce mois-ci : %d commandes pour %.2f DT\n" +
                                "- Mois dernier : %.2f DT (variation : %+.1f%%)\n" +
                                "- Méthode préférée : %s\n" +
                                "- Total historique : %d commandes validées, %.2f DT dépensés\n\n" +
                                "Félicite-le chaleureusement pour ses achats, commente positivement " +
                                "l'évolution par rapport au mois dernier, et encourage-le à continuer " +
                                "à découvrir les nouveaux produits de la boutique.",
                        nbMois, totalMois, totalDernier, variation,
                        methodePref, nbTotal, totalAll
                );

                String result = com.hanouti.hanoutiem4.util.ClaudeService.ask(systemPrompt, userMsg);

                final String finalResult = (result != null && !result.isBlank())
                        ? result
                        : "Impossible d'obtenir une réponse de l'IA. Vérifie ta connexion internet.";

                final double finalTotalMois = totalMois;
                final int finalNbMois = nbMois;
                final double finalVariation = variation;
                final String finalMethode = methodePref;

                javafx.application.Platform.runLater(() ->
                        showAnalyseResult(finalResult, finalTotalMois, finalNbMois, finalVariation, finalMethode)
                );

            } catch (Exception e) {
                javafx.application.Platform.runLater(() ->
                        showStyledAlert("Erreur", "Impossible d'analyser : " + e.getMessage(), "#dc2626")
                );
            }
        }).start();
    }

    private void showAnalyseResult(String aiText, double totalMois, int nbMois,
                                   double variation, String methode) {
        boolean dark   = isDarkMode;
        String bgCard  = dark ? "#1a1a2e" : "#ffffff";
        String t1      = dark ? "#f8fafc"  : "#0f172a";
        String t2      = dark ? "#94a3b8"  : "#64748b";

        StackPane overlay = new StackPane();
        overlay.setStyle("-fx-background-color:rgba(0,0,0,0.55);");
        javafx.scene.layout.AnchorPane.setTopAnchor(overlay,0.0);
        javafx.scene.layout.AnchorPane.setBottomAnchor(overlay,0.0);
        javafx.scene.layout.AnchorPane.setLeftAnchor(overlay,0.0);
        javafx.scene.layout.AnchorPane.setRightAnchor(overlay,0.0);

        VBox card = new VBox(0);
        card.setMaxWidth(520); card.setMaxHeight(javafx.scene.layout.Region.USE_PREF_SIZE);
        card.setStyle("-fx-background-color:" + bgCard + ";-fx-background-radius:20;" +
                "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.40),40,0,0,12);");
        StackPane.setAlignment(card, Pos.CENTER);
        card.setScaleX(0.85); card.setScaleY(0.85); card.setOpacity(0);

        // Header gradient
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setStyle("-fx-background-color:linear-gradient(to right,#3730a3,#4f46e5,#6366f1);" +
                "-fx-background-radius:20 20 0 0;-fx-padding:20 24;");
        Label hIcon = new Label("✨");
        hIcon.setStyle("-fx-font-size:22px;");
        VBox hText = new VBox(2);
        Label hTitle = new Label("Ton bilan shopping 🛍️");
        hTitle.setStyle("-fx-font-size:16px;-fx-font-weight:bold;-fx-text-fill:white;");
        Label hSub = new Label("Généré par IA — Groq llama-3.3-70b");
        hSub.setStyle("-fx-font-size:11px;-fx-text-fill:rgba(255,255,255,0.65);");
        hText.getChildren().addAll(hTitle, hSub);
        header.getChildren().addAll(hIcon, hText);

        // Mini stats row
        HBox statsRow = new HBox(8);
        statsRow.setStyle("-fx-background-color:" + (dark?"#0f0f1a":"#f8f7ff") + ";-fx-padding:14 24;");
        statsRow.getChildren().addAll(
                miniStat(String.format("%.0f DT", totalMois), "Ce mois", "#4f46e5"),
                miniStat(String.valueOf(nbMois), "Commandes", "#059669"),
                miniStat((variation >= 0 ? "+" : "") + String.format("%.0f%%", variation),
                        "vs mois dernier", variation >= 0 ? "#dc2626" : "#059669"),
                miniStat(methode, "Méthode fav.", "#d97706")
        );

        // AI text
        javafx.scene.control.ScrollPane scroll = new javafx.scene.control.ScrollPane();
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background:transparent;-fx-background-color:transparent;-fx-border-width:0;");
        scroll.setMaxHeight(200);
        Label aiLbl = new Label(aiText);
        aiLbl.setWrapText(true);
        aiLbl.setStyle("-fx-font-size:13px;-fx-text-fill:" + t2 + ";-fx-line-spacing:5;-fx-padding:18 24 12 24;");
        scroll.setContent(aiLbl);

        // Footer
        HBox footer = new HBox();
        footer.setAlignment(Pos.CENTER_RIGHT);
        footer.setStyle("-fx-padding:12 24 20 24;");
        Button closeBtn = new Button("Fermer");
        String btnReset = "-fx-focus-color:transparent;-fx-faint-focus-color:transparent;-fx-border-width:0;";
        closeBtn.setStyle(btnReset + "-fx-background-color:#4f46e5;-fx-background-radius:10;" +
                "-fx-text-fill:white;-fx-font-size:13px;-fx-font-weight:bold;-fx-padding:10 32;-fx-cursor:hand;");
        closeBtn.setOnAction(e -> {
            javafx.animation.FadeTransition fo = new javafx.animation.FadeTransition(
                    javafx.util.Duration.millis(150), overlay);
            fo.setToValue(0);
            fo.setOnFinished(ev -> rootPane.getChildren().remove(overlay));
            fo.play();
        });
        footer.getChildren().add(closeBtn);

        card.getChildren().addAll(header, statsRow, scroll, footer);
        overlay.getChildren().add(card);
        rootPane.getChildren().add(overlay);
        overlay.setOnMouseClicked(e -> { if (e.getTarget() == overlay) closeBtn.fire(); });

        javafx.animation.FadeTransition fi = new javafx.animation.FadeTransition(
                javafx.util.Duration.millis(220), card); fi.setToValue(1);
        javafx.animation.ScaleTransition si = new javafx.animation.ScaleTransition(
                javafx.util.Duration.millis(240), card);
        si.setToX(1); si.setToY(1);
        si.setInterpolator(javafx.animation.Interpolator.EASE_OUT);
        new javafx.animation.ParallelTransition(fi, si).play();
    }

    private VBox miniStat(String value, String label, String color) {
        VBox box = new VBox(2);
        box.setAlignment(Pos.CENTER);
        box.setStyle("-fx-background-color:white;-fx-background-radius:10;-fx-padding:10 14;" +
                "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.06),8,0,0,2);");
        HBox.setHgrow(box, javafx.scene.layout.Priority.ALWAYS);
        Label val = new Label(value);
        val.setStyle("-fx-font-size:15px;-fx-font-weight:bold;-fx-text-fill:" + color + ";");
        Label lbl = new Label(label);
        lbl.setStyle("-fx-font-size:10px;-fx-text-fill:#94a3b8;");
        box.getChildren().addAll(val, lbl);
        return box;
    }

}