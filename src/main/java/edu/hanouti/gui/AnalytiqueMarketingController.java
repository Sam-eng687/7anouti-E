package edu.hanouti.gui;

import edu.hanouti.entities.ConseilsMarketing;
import edu.hanouti.entities.StatistiquesVentes;
import edu.hanouti.services.ConseilsMarketingService;
import edu.hanouti.services.StatistiquesVentesService;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Text;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class AnalytiqueMarketingController implements Initializable {

    @FXML private VBox mainContainer;
    @FXML private HBox kpiContainer;
    @FXML private Text viewTitle;
    @FXML private Text viewSubtitle;
    @FXML private ToggleButton themeToggle;
    
    @FXML private VBox conseilsView;
    @FXML private VBox campagnesView;
    @FXML private VBox statsView;
    
    @FXML private Button btnConseils;
    @FXML private Button btnCampagnes;
    @FXML private Button btnStats;
    
    @FXML private Text countTotal;
    @FXML private Text countAppliques;
    @FXML private Text countDestockage;
    @FXML private Text countUrgents;
    
    @FXML private BarChart<String, Number> budgetChart;
    @FXML private AreaChart<String, Number> salesChart;
    @FXML private PieChart statusPieChart;
    
    @FXML private FlowPane conseilsContainer;
    @FXML private FlowPane campagnesContainer;

    private final StatistiquesVentesService statsService = new StatistiquesVentesService();
    private final ConseilsMarketingService conseilsService = new ConseilsMarketingService();

    private List<StatistiquesVentes> allStats;
    private List<ConseilsMarketing> allConseils;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        loadData();
        showStatsView(); // Par défaut ce matin
    }

    public void loadData() {
        allStats = statsService.getData();
        allConseils = conseilsService.getData();
        updateCharts();
        renderConseils(allConseils);
        updateMiniCounters();
    }

    private void updateMiniCounters() {
        if (countTotal != null) countTotal.setText(String.valueOf(allConseils.size()));
        if (countAppliques != null) countAppliques.setText(String.valueOf(allConseils.stream().filter(ConseilsMarketing::isApplique).count()));
        if (countDestockage != null) countDestockage.setText(String.valueOf(allConseils.stream().filter(c -> c.getTypeConseil().equalsIgnoreCase("Destockage")).count()));
        if (countUrgents != null) countUrgents.setText(String.valueOf(allConseils.stream().filter(c -> c.getImpactEstime().equalsIgnoreCase("Haute") || c.getImpactEstime().equalsIgnoreCase("Urgent")).count()));
    }

    private void updateCharts() {
        // BarChart
        budgetChart.getData().clear();
        XYChart.Series<String, Number> seriesB = new XYChart.Series<>();
        seriesB.setName("Budget Alloué");
        allStats.stream().limit(5).forEach(s -> seriesB.getData().add(new XYChart.Data<>(s.getProduitId(), s.getRevenuTotal() * 0.8)));
        budgetChart.getData().add(seriesB);

        // AreaChart
        salesChart.getData().clear();
        XYChart.Series<String, Number> seriesS = new XYChart.Series<>();
        seriesS.setName("Revenus");
        allStats.stream().limit(6).forEach(s -> seriesS.getData().add(new XYChart.Data<>(s.getPeriode(), s.getRevenuTotal())));
        salesChart.getData().add(seriesS);

        // PieChart
        ObservableList<PieChart.Data> pieData = FXCollections.observableArrayList(
            new PieChart.Data("Actives", 5),
            new PieChart.Data("Terminées", 7),
            new PieChart.Data("Brouillons", 2)
        );
        statusPieChart.setData(pieData);
    }

    @FXML public void showConseilsView() {
        switchView(conseilsView, btnConseils, "💡 Conseils Marketing", "Optimisez vos ventes grâce aux recommandations intelligentes de l'IA.");
        updateKPIs("conseils");
    }

    @FXML public void showCampagnesView() {
        switchView(campagnesView, btnCampagnes, "📢 Campagnes Marketing", "Gérez vos actions marketing avec suivi de budget et performance.");
        updateKPIs("campagnes");
    }

    @FXML public void showStatsView() {
        switchView(statsView, btnStats, "📊 Analyses & Statistiques", "Performances, ROI, et évolution de vos budgets marketing.");
        updateKPIs("stats");
    }

    private void switchView(VBox view, Button btn, String title, String subtitle) {
        conseilsView.setVisible(false); conseilsView.setManaged(false);
        campagnesView.setVisible(false); campagnesView.setManaged(false);
        statsView.setVisible(false); statsView.setManaged(false);
        view.setVisible(true); view.setManaged(true);
        
        btnConseils.getStyleClass().remove("tab-btn-active");
        btnCampagnes.getStyleClass().remove("tab-btn-active");
        btnStats.getStyleClass().remove("tab-btn-active");
        btn.getStyleClass().add("tab-btn-active");
        
        viewTitle.setText(title);
        viewSubtitle.setText(subtitle);
    }

    private void updateKPIs(String type) {
        kpiContainer.getChildren().clear();
        if (type.equals("stats")) {
            addKPICard("BUDGET ALLOUÉ", "45,000 TND", "border-blue");
            addKPICard("REVENUS GÉNÉRÉS", "124,500 TND", "border-green");
            addKPICard("ROI GLOBAL", "4.8x", "border-pink");
        } else if (type.equals("campagnes")) {
            addKPICard("💰 BUDGET TOTAL", "45,000 TND", "border-blue");
            addKPICard("✅ ACTIVES", "5", "border-green");
            addKPICard("💸 DÉPENSÉ", "28,400 TND", "border-orange");
        }
    }

    private void addKPICard(String title, String value, String borderClass) {
        VBox card = new VBox(5);
        card.getStyleClass().addAll("kpi-card-styled", borderClass);
        HBox.setHgrow(card, Priority.ALWAYS);
        Label lblT = new Label(title); lblT.getStyleClass().add("kpi-label");
        Label lblV = new Label(value); lblV.getStyleClass().add("kpi-value-styled");
        card.getChildren().addAll(lblT, lblV);
        kpiContainer.getChildren().add(card);
    }

    private void renderConseils(List<ConseilsMarketing> conseils) {
        conseilsContainer.getChildren().clear();
        for (ConseilsMarketing c : conseils) {
            VBox card = new VBox(15);
            card.getStyleClass().add("chart-card");
            card.setPrefWidth(350);
            Label title = new Label("📦 " + c.getProduitId()); title.getStyleClass().add("chart-title");
            Label desc = new Label(c.getDescription()); desc.setWrapText(true); desc.setStyle("-fx-text-fill: #64748B;");
            Button btn = new Button("Appliquer"); btn.getStyleClass().add("action-btn"); btn.setMaxWidth(Double.MAX_VALUE);
            card.getChildren().addAll(title, desc, new Separator(), btn);
            conseilsContainer.getChildren().add(card);
        }
    }

    @FXML void handleThemeToggle() {
        if (themeToggle.isSelected()) {
            mainContainer.getStyleClass().add("dark-theme");
            themeToggle.setText("☀️ Clair");
        } else {
            mainContainer.getStyleClass().remove("dark-theme");
            themeToggle.setText("🌙 Sombre");
        }
    }

    @FXML void handleAddStat() { loadData(); }
    @FXML void handleAddConseil() { loadData(); }
    @FXML void handleExit() { System.exit(0); }
}
