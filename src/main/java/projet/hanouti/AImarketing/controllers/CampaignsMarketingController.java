package projet.hanouti.AImarketing.controllers;

import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import projet.hanouti.AImarketing.entities.CampagneMarketing;
import projet.hanouti.AImarketing.entities.ConseilsMarketing;
import projet.hanouti.AImarketing.interfaces.ThemeAware;
import projet.hanouti.AImarketing.services.MarketingDataService;
import projet.hanouti.AImarketing.tools.MarketingFormatters;
import projet.hanouti.common.utils.SessionManager;

import java.util.List;

public class CampaignsMarketingController implements ThemeAware {
    @FXML private VBox root;
    @FXML private HBox kpiRow;
    @FXML private HBox adviceRow;
    @FXML private VBox campaignList;
    @FXML private Button addCampaignBtn;
    @FXML private Button emailVendorBtn;

    private final MarketingDataService dataService = new MarketingDataService();
    private List<CampagneMarketing> campagnes;
    private List<ConseilsMarketing> conseils;

    @FXML
    private void initialize() {
        applyTheme(SessionManager.getInstance().isDarkMode());
        addCampaignBtn.setOnAction(e -> showInfo("Ajout campagne", "Le formulaire d'ajout est pret pour etre branche a la table campagne_marketing."));
        emailVendorBtn.setOnAction(e -> showInfo("Email vendeur", "Action email vendeur prete pour integration SMTP."));
        campagnes = dataService.getCampagnes();
        conseils = dataService.getConseils();
        render();
    }

    @Override
    public void applyTheme(boolean dark) {
        if (root == null) return;
        root.getStyleClass().remove("light");
        if (!dark) root.getStyleClass().add("light");
    }

    private void render() {
        double budget = campagnes.stream().mapToDouble(CampagneMarketing::getBudget).sum();
        double depense = campagnes.stream().mapToDouble(CampagneMarketing::getDepense).sum();
        long active = campagnes.stream().filter(c -> "ACTIVE".equalsIgnoreCase(c.getStatut())).count();

        kpiRow.getChildren().setAll(
                kpi("blue", "BUDGET TOTAL", MarketingFormatters.money(budget), MarketingFormatters.shortMoney(budget)),
                kpi("green", "ACTIVES", String.valueOf(active), "en cours d'execution"),
                kpi("orange", "TOTAL", String.valueOf(campagnes.size()), "actions marketing enregistrees"),
                kpi("violet", "DEPENSE", MarketingFormatters.money(depense), MarketingFormatters.shortMoney(depense))
        );

        adviceRow.getChildren().clear();
        conseils.stream().limit(3).forEach(c -> adviceRow.getChildren().add(adviceStrip(c)));

        campaignList.getChildren().clear();
        campagnes.forEach(c -> campaignList.getChildren().add(campaignCard(c)));
    }

    private VBox adviceStrip(ConseilsMarketing conseil) {
        VBox card = new VBox(8);
        card.getStyleClass().addAll("am-advice-strip", dataService.typeClass(conseil.getTypeConseil()));
        HBox.setHgrow(card, Priority.ALWAYS);
        card.setPadding(new Insets(14));
        Label type = new Label(MarketingFormatters.value(conseil.getTypeConseil(), "Promotion"));
        type.getStyleClass().add("am-strip-title");
        Label desc = new Label(MarketingFormatters.value(conseil.getDescription(), "Conseil IA automatique."));
        desc.setWrapText(true);
        desc.getStyleClass().add("am-card-text");
        card.getChildren().addAll(type, desc);
        return card;
    }

    private VBox campaignCard(CampagneMarketing c) {
        VBox card = new VBox(12);
        card.getStyleClass().add("am-campaign-card");
        card.setPadding(new Insets(18, 22, 16, 22));

        HBox head = new HBox(12);
        head.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        Label title = new Label(MarketingFormatters.value(c.getNomCampagne(), "Campagne"));
        title.getStyleClass().add("am-card-title");
        Label ia = new Label("IA");
        ia.getStyleClass().add("am-chip");
        Label status = new Label(MarketingFormatters.value(c.getStatut(), "BROUILLON"));
        status.getStyleClass().add("am-chip");
        HBox.setHgrow(title, Priority.ALWAYS);
        head.getChildren().addAll(title, ia, status);

        Label meta = new Label(MarketingFormatters.value(c.getObjectif(), "FIDELISATION")
                + "  |  " + MarketingFormatters.value(c.getCanal(), "EMAIL")
                + "  |  " + MarketingFormatters.dateRange(c.getDateDebut(), c.getDateFin()));
        meta.getStyleClass().add("am-muted");

        Label info = new Label(MarketingFormatters.value(c.getIaConseil(), "Brouillon - campagne non encore lancee"));
        info.getStyleClass().add("am-campaign-note");
        info.setWrapText(true);

        HBox metrics = new HBox(12);
        VBox budget = metric("Budget consomme", MarketingFormatters.money(c.getDepense()) + " / " + MarketingFormatters.money(c.getBudget()));
        VBox revenu = metric("Revenu genere", "N/A");
        metrics.getChildren().addAll(budget, revenu);

        HBox bars = new HBox(8);
        bars.getStyleClass().add("am-progress-row");
        Label budgetPct = new Label("Budget " + percentOf(c.getDepense(), c.getBudget()));
        Label revenuePct = new Label("Revenu 0%");
        budgetPct.getStyleClass().add("am-muted");
        revenuePct.getStyleClass().add("am-muted");
        bars.getChildren().addAll(budgetPct, revenuePct);

        HBox actions = new HBox(8);
        actions.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);
        Button edit = new Button("Modifier");
        edit.getStyleClass().add("am-ghost-btn");
        Button delete = new Button("Supprimer");
        delete.getStyleClass().add("am-ghost-btn");
        actions.getChildren().addAll(edit, delete);

        card.getChildren().addAll(head, meta, info, metrics, bars, actions);
        return card;
    }

    private VBox metric(String title, String value) {
        VBox box = new VBox(4);
        box.getStyleClass().add("am-metric-box");
        HBox.setHgrow(box, Priority.ALWAYS);
        Label t = new Label(title);
        t.getStyleClass().add("am-muted");
        Label v = new Label(value);
        v.getStyleClass().add("am-metric-value");
        box.getChildren().addAll(t, v);
        return box;
    }

    private String percentOf(double value, double total) {
        if (total <= 0) return "0%";
        return Math.round((value / total) * 100.0) + "%";
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

    private void showInfo(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
