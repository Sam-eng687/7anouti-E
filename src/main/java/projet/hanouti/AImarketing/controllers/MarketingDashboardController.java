package projet.hanouti.AImarketing.controllers;

import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import projet.hanouti.AImarketing.entities.StatistiquesVentes;
import projet.hanouti.AImarketing.interfaces.ThemeAware;
import projet.hanouti.AImarketing.services.MarketingDataService;
import projet.hanouti.AImarketing.tools.MarketingFormatters;
import projet.hanouti.common.utils.SessionManager;

import java.util.Comparator;
import java.util.List;

public class MarketingDashboardController implements ThemeAware {
    @FXML private VBox root;
    @FXML private HBox kpiRow;
    @FXML private Label insightLabel;
    @FXML private Label topProductLabel;
    @FXML private Label totalRevenueLabel;
    @FXML private Label returnRateLabel;

    private final MarketingDataService dataService = new MarketingDataService();
    private List<StatistiquesVentes> stats;

    @FXML
    private void initialize() {
        applyTheme(SessionManager.getInstance().isDarkMode());
        stats = dataService.getStatistiques();
        render();
    }

    @Override
    public void applyTheme(boolean dark) {
        if (root == null) return;
        root.getStyleClass().remove("light");
        if (!dark) root.getStyleClass().add("light");
    }

    private void render() {
        int totalVendu = stats.stream().mapToInt(StatistiquesVentes::getTotalVendu).sum();
        double revenu = stats.stream().mapToDouble(StatistiquesVentes::getRevenuTotal).sum();
        double retour = stats.stream().mapToDouble(StatistiquesVentes::getTauxRetour).average().orElse(0);
        long actifs = stats.stream().filter(s -> s.getTotalVendu() > 0).count();
        StatistiquesVentes top = stats.stream()
                .max(Comparator.comparingDouble(StatistiquesVentes::getRevenuTotal))
                .orElse(null);

        kpiRow.getChildren().setAll(
                kpi("blue", "TOTAL VENDU", String.valueOf(totalVendu), totalVendu + " unites ce mois"),
                kpi("green", "REVENU TOTAL", MarketingFormatters.money(revenu), MarketingFormatters.shortMoney(revenu)),
                kpi("violet", "PRODUITS ACTIFS", String.valueOf(actifs), actifs + " actifs"),
                kpi("pink", "TAUX DE RETOUR", MarketingFormatters.percent(retour), "Taux retour produit")
        );

        String topLabel = top == null ? "N/A" : dataService.labelFor(top);
        topProductLabel.setText("Produit phare : " + topLabel);
        totalRevenueLabel.setText("Revenu total : " + MarketingFormatters.money(revenu));
        returnRateLabel.setText("Taux retour moyen : " + MarketingFormatters.percent(retour));
        insightLabel.setText("Produit phare : " + topLabel + "  .  Revenu total : " + MarketingFormatters.money(revenu)
                + "  .  Taux retour moyen : " + MarketingFormatters.percent(retour));
    }

    private VBox kpi(String color, String label, String value, String detail) {
        VBox card = new VBox(12);
        card.getStyleClass().addAll("am-kpi", "am-kpi-" + color);
        HBox.setHgrow(card, Priority.ALWAYS);
        card.setPadding(new Insets(24));

        Label labelNode = new Label(label);
        labelNode.getStyleClass().add("am-kpi-label");
        Label valueNode = new Label(value);
        valueNode.getStyleClass().add("am-kpi-value");
        Label detailNode = new Label(detail);
        detailNode.getStyleClass().add("am-muted");

        card.getChildren().addAll(labelNode, valueNode, detailNode);
        return card;
    }
}
