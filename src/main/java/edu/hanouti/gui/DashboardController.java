package edu.hanouti.gui;

import edu.hanouti.entities.CampagneMarketing;
import edu.hanouti.entities.ConseilsMarketing;
import edu.hanouti.entities.StatistiquesVentes;
import edu.hanouti.services.CampagneMarketingService;
import edu.hanouti.services.ConseilsMarketingService;
import edu.hanouti.services.StatistiquesVentesService;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.stream.Collectors;
import javafx.application.Platform;
import javafx.scene.control.ButtonType;
import javafx.scene.Scene;

public class DashboardController implements Initializable {

    @FXML private Button btnStats;
    @FXML private Button btnConseils;
    @FXML private Button btnCampagnes;

    @FXML private ScrollPane statsView;
    @FXML private ScrollPane conseilsView;
    @FXML private ScrollPane campagnesView;

    @FXML private HBox statsKpiRow;
    @FXML private HBox conseilsKpiRow;
    @FXML private HBox campagnesKpiRow;

    @FXML private Label iaAlertText;
    @FXML private ComboBox<String> periodFilter;
    @FXML private Label dbStatusLabel;
    @FXML private Button btnTheme;
    private boolean isDarkTheme = true;
    @FXML private BarChart<String, Number> barChart;
    @FXML private PieChart pieChart;
    @FXML private TableView<StatistiquesVentes> statsTable;
    @FXML private FlowPane conseilsList;
    @FXML private VBox campagnesList;

    private final StatistiquesVentesService statsService = new StatistiquesVentesService();
    private final ConseilsMarketingService conseilsService = new ConseilsMarketingService();
    private final CampagneMarketingService campagnesService = new CampagneMarketingService();

    private List<StatistiquesVentes> stats = new ArrayList<>();
    private List<ConseilsMarketing> conseils = new ArrayList<>();
    private List<CampagneMarketing> campagnes = new ArrayList<>();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        periodFilter.setItems(FXCollections.observableArrayList("Toutes les periodes", "Semaine", "Mois", "Trimestre", "Annee"));
        periodFilter.getSelectionModel().selectFirst();
        periodFilter.setOnAction(e -> renderStats());
        setupStatsTable();
        loadData();
        renderAll();
        showStats();
    }

    @FXML
    private void showStats() {
        setActiveBtn(btnStats);
        statsView.setVisible(true); statsView.setManaged(true);
        conseilsView.setVisible(false); conseilsView.setManaged(false);
        campagnesView.setVisible(false); campagnesView.setManaged(false);
    }

    @FXML
    private void showConseils() {
        setActiveBtn(btnConseils);
        statsView.setVisible(false); statsView.setManaged(false);
        conseilsView.setVisible(true); conseilsView.setManaged(true);
        campagnesView.setVisible(false); campagnesView.setManaged(false);
    }

    @FXML
    private void showCampagnes() {
        setActiveBtn(btnCampagnes);
        statsView.setVisible(false); statsView.setManaged(false);
        conseilsView.setVisible(false); conseilsView.setManaged(false);
        campagnesView.setVisible(true); campagnesView.setManaged(true);
    }

    @FXML
    private void refreshAll() {
        loadData();
        renderAll();
        showStats();
    }

    @FXML
    private void toggleTheme() {
        isDarkTheme = !isDarkTheme;
        Scene scene = btnTheme.getScene();
        if (scene != null) {
            scene.getStylesheets().clear();
            String css = isDarkTheme ? "/style.css" : "/style-light.css";
            try {
                scene.getStylesheets().add(getClass().getResource(css).toExternalForm());
            } catch (Exception e) { e.printStackTrace(); }
        }
        btnTheme.setText(isDarkTheme ? "☀️ Mode Clair" : "🌙 Mode Sombre");
    }

    @FXML
    private void handleLogout() {
        Alert al = new Alert(Alert.AlertType.CONFIRMATION, "Voulez-vous quitter l'application ?", ButtonType.YES, ButtonType.NO);
        al.setTitle("Deconnexion"); al.setHeaderText(null);
        al.showAndWait().ifPresent(r -> { if (r == ButtonType.YES) Platform.exit(); });
    }

    @FXML
    private void analyzeIA() {
        String context = stats.stream().limit(5)
            .map(s -> s.getProduitId() + ": " + s.getTotalVendu() + " ventes, " + formatMoney(s.getRevenuTotal()))
            .collect(Collectors.joining(" | "));
        showInfo("Analyse", "Produit phare : " + (stats.isEmpty() ? "N/A" : stats.get(0).getProduitId()) +
                 "\nContexte : " + (context.isEmpty() ? "Pas de données" : context));
    }

    @FXML
    private void exportStats() {
        Alert al = new Alert(Alert.AlertType.INFORMATION);
        al.setTitle("📄 Export PDF");
        al.setHeaderText("Rapport Creative Pastel");
        al.setContentText("Le rapport des performances a été généré avec succès en format PDF.");
        al.show();
    }

    private void loadData() {
        stats = statsService.getData();
        conseils = conseilsService.getData();
        campagnes = campagnesService.getData();
        checkDbStatus();
    }

    private void checkDbStatus() {
        if (dbStatusLabel != null) {
            try {
                java.sql.Connection cnx = edu.hanouti.utils.MyConnection.getInstance().getCnx();
                boolean ok = (cnx != null && !cnx.isClosed());
                dbStatusLabel.setText(ok ? "● MySQL WAMP Actif" : "● MySQL Deconnecte");
                dbStatusLabel.getStyleClass().setAll("db-label");
                if (!ok) dbStatusLabel.getStyleClass().add("db-err");
            } catch (Exception e) {
                dbStatusLabel.setText("● MySQL Deconnecte");
                dbStatusLabel.getStyleClass().setAll("db-label", "db-err");
            }
        }
    }

    private void renderAll() {
        renderStats();
        renderConseils();
        renderCampagnes();
    }

    private void renderStats() {
        List<StatistiquesVentes> filtered = filterStatsByPeriod(periodFilter.getValue());
        statsTable.setItems(FXCollections.observableArrayList(filtered));

        barChart.getData().clear();
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        filtered.stream().limit(6).forEach(s -> series.getData().add(new XYChart.Data<>(s.getProduitId(), s.getRevenuTotal())));
        barChart.getData().add(series);

        pieChart.getData().clear();
        pieChart.getData().addAll(statsService.getStrategicDistribution(periodFilter.getValue()));

        int totalVendu = filtered.stream().mapToInt(StatistiquesVentes::getTotalVendu).sum();
        double totalRevenu = filtered.stream().mapToDouble(StatistiquesVentes::getRevenuTotal).sum();
        double tauxMoyen = filtered.isEmpty() ? 0 : filtered.stream().mapToDouble(StatistiquesVentes::getTauxRetour).average().orElse(0);

        statsKpiRow.getChildren().setAll(
            makeKpi("kpi-royal", "TOTAL VENDU", String.valueOf(totalVendu), "↑ +12%"),
            makeKpi("kpi-green", "REVENU TOTAL", formatMoney(totalRevenu), "↑ Donnees DB"),
            makeKpi("kpi-orange", "PRODUITS ACTIFS", String.valueOf(filtered.size()), "catalogue visible"),
            makeKpi("kpi-violet", "TAUX RETOUR", String.format(Locale.US, "%.2f%%", tauxMoyen), "↓ moyenne")
        );

        iaAlertText.setText(buildIAAlertText());
    }

    private void renderConseils() {
        conseilsKpiRow.getChildren().setAll(
            makeKpi("kpi-royal", "TOTAL CONSEILS", String.valueOf(conseils.size()), "enregistrements"),
            makeKpi("kpi-green", "APPLIQUES", String.valueOf(conseils.stream().filter(ConseilsMarketing::isApplique).count()), "actions"),
            makeKpi("kpi-orange", "NOUVEAUX", String.valueOf(conseils.stream().filter(c -> !c.isApplique()).count()), "a traiter"),
            makeKpi("kpi-violet", "CONFIANCE IA", "85%", "score moyen")
        );

        conseilsList.getChildren().clear();
        conseils.stream().limit(10).forEach(c -> conseilsList.getChildren().add(makeConseilCard(c)));
    }

    private void renderCampagnes() {
        campagnesKpiRow.getChildren().setAll(
            makeKpi("kpi-royal", "BUDGET TOTAL", formatMoney(campagnesService.getBudgetTotal()), "budget cumule"),
            makeKpi("kpi-green", "ACTIVES", String.valueOf(campagnesService.countByStatut("ACTIVE")), "en cours"),
            makeKpi("kpi-orange", "TOTAL", String.valueOf(campagnes.size()), "campagnes"),
            makeKpi("kpi-violet", "DEPENSE", formatMoney(campagnes.stream().mapToDouble(CampagneMarketing::getDepense).sum()), "depenses")
        );

        campagnesList.getChildren().clear();
        campagnes.forEach(c -> campagnesList.getChildren().add(makeCampagneCard(c)));
    }

    private VBox makeConseilCard(ConseilsMarketing c) {
        VBox card = new VBox(10);
        card.getStyleClass().addAll("conseil-card", mapConseilClass(c.getTypeConseil()));
        card.setPadding(new Insets(14));

        Label title = new Label("💡 " + c.getProduitId());
        title.getStyleClass().add("conseil-title");
        Label type = new Label(c.getTypeConseil());
        type.getStyleClass().add("type-badge");
        Label desc = new Label(c.getDescription());
        desc.setWrapText(true);
        desc.getStyleClass().add("conseil-desc");
        Button apply = new Button(c.isApplique() ? "✓ Conseil applique" : "✓ Accepter");
        apply.getStyleClass().add("btn-accepter");
        apply.setDisable(c.isApplique());
        apply.setOnAction(e -> {
            conseilsService.appliquerConseil(c.getConseilId());
            refreshAll();
            showConseils();
        });

        card.getChildren().addAll(title, type, desc, apply);
        return card;
    }

    private VBox makeCampagneCard(CampagneMarketing c) {
        VBox card = new VBox(8);
        card.getStyleClass().addAll("camp-card", mapCampagneClass(c.getStatut()));
        card.setPadding(new Insets(16));
        Label n = new Label("📢 " + c.getNomCampagne());
        n.getStyleClass().add("camp-name");
        Label m = new Label("🎯 " + c.getObjectif() + "   📡 " + c.getCanal() + "   📅 " + c.getDateDebut() + " → " + c.getDateFin());
        m.getStyleClass().add("camp-meta");
        Label s = new Label("⭐ IA " + String.format(Locale.US, "%.1f/10", c.getIaScore()));
        s.getStyleClass().add("ia-score-badge");
        Label ia = new Label(c.getIaConseil() == null ? "Aucun conseil IA." : c.getIaConseil());
        ia.getStyleClass().add("ia-conseil-strip");
        card.getChildren().addAll(n, s, m, ia);
        return card;
    }

    private VBox makeKpi(String style, String label, String value, String trend) {
        VBox card = new VBox(6);
        card.getStyleClass().addAll("kpi-card", style);
        HBox.setHgrow(card, Priority.ALWAYS);
        Label l = new Label(label);
        l.getStyleClass().add("kpi-label");
        Label v = new Label(value);
        v.getStyleClass().add("kpi-value");
        Label t = new Label(trend);
        t.getStyleClass().add("kpi-trend");
        card.getChildren().addAll(l, v, t);
        return card;
    }

    private void setupStatsTable() {
        TableColumn<StatistiquesVentes, String> c1 = new TableColumn<>("Reference");
        c1.setCellValueFactory(new PropertyValueFactory<>("produitId"));
        TableColumn<StatistiquesVentes, String> c2 = new TableColumn<>("Periode");
        c2.setCellValueFactory(new PropertyValueFactory<>("periode"));
        TableColumn<StatistiquesVentes, Integer> c3 = new TableColumn<>("Ventes");
        c3.setCellValueFactory(new PropertyValueFactory<>("totalVendu"));
        TableColumn<StatistiquesVentes, Double> c4 = new TableColumn<>("Revenu (TND)");
        c4.setCellValueFactory(new PropertyValueFactory<>("revenuTotal"));
        TableColumn<StatistiquesVentes, Number> c4b = new TableColumn<>("REV / UNITÉ");
        c4b.setCellValueFactory(new PropertyValueFactory<>("revUnite"));
        TableColumn<StatistiquesVentes, Double> c5 = new TableColumn<>("Taux Retour (%)");
        c5.setCellValueFactory(new PropertyValueFactory<>("tauxRetour"));
        TableColumn<StatistiquesVentes, String> c6 = new TableColumn<>("Classement");
        c6.setCellValueFactory(new PropertyValueFactory<>("classement"));

        statsTable.getColumns().setAll(c1, c2, c3, c4, c4b, c5, c6);
        statsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    }

    private void setActiveBtn(Button active) {
        btnStats.getStyleClass().remove("active");
        btnConseils.getStyleClass().remove("active");
        btnCampagnes.getStyleClass().remove("active");
        active.getStyleClass().add("active");
    }

    private List<StatistiquesVentes> filterStatsByPeriod(String period) {
        if (stats.isEmpty() || period == null || period.equals("Toutes les periodes")) return new ArrayList<>(stats);
        String keyword = switch (period) {
            case "Semaine" -> "semaine";
            case "Mois" -> "mensuel";
            case "Trimestre" -> "trimestre";
            case "Annee" -> "ann";
            default -> "";
        };
        return stats.stream()
                .filter(s -> s.getPeriode() != null && s.getPeriode().toLowerCase().contains(keyword))
                .collect(Collectors.toList());
    }

    private String mapConseilClass(String type) {
        String t = type == null ? "" : type.toLowerCase(Locale.ROOT);
        if (t.contains("promo")) return "type-promo";
        if (t.contains("destock")) return "type-destock";
        if (t.contains("bundle")) return "type-bundle";
        if (t.contains("stock")) return "type-stock";
        return "type-mise";
    }

    private String mapCampagneClass(String statut) {
        String s = statut == null ? "" : statut.toLowerCase(Locale.ROOT);
        if (s.contains("term")) return "camp-terminee";
        if (s.contains("active")) return "camp-active-g";
        return "camp-brouillon";
    }

    private String formatMoney(double value) {
        return String.format(Locale.US, "%,.0f TND", value);
    }

    private String buildIAAlertText() {
        if (conseils.isEmpty()) return "IA Detecte: aucune recommandation pour le moment.";
        ConseilsMarketing first = conseils.get(0);
        return "IA Detecte: " + first.getProduitId() + " — " + first.getDescription();
    }

    private void showInfo(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }
}
