package projet.hanouti.AImarketing.controllers;

import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import projet.hanouti.AImarketing.entities.ConseilsMarketing;
import projet.hanouti.AImarketing.interfaces.ThemeAware;
import projet.hanouti.AImarketing.services.MarketingDataService;
import projet.hanouti.AImarketing.tools.MarketingFormatters;
import projet.hanouti.common.utils.SessionManager;

import java.util.List;

public class ConseilDecisionController implements ThemeAware {
    @FXML private VBox root;
    @FXML private HBox kpiRow;
    @FXML private FlowPane adviceGrid;
    @FXML private Button activeBtn;
    @FXML private Button appliedBtn;
    @FXML private Button ignoredBtn;
    @FXML private Button allTypeBtn;
    @FXML private Button promotionTypeBtn;
    @FXML private Button destockTypeBtn;
    @FXML private Button bundleTypeBtn;
    @FXML private Button generateBtn;

    private final MarketingDataService dataService = new MarketingDataService();
    private List<ConseilsMarketing> conseils;
    private String statusFilter = "ACTIVE";
    private String typeFilter = "ALL";

    @FXML
    private void initialize() {
        applyTheme(SessionManager.getInstance().isDarkMode());
        conseils = dataService.getConseils();
        activeBtn.setOnAction(e -> setStatusFilter("ACTIVE"));
        appliedBtn.setOnAction(e -> setStatusFilter("APPLIED"));
        ignoredBtn.setOnAction(e -> setStatusFilter("IGNORED"));
        allTypeBtn.setOnAction(e -> setTypeFilter("ALL"));
        promotionTypeBtn.setOnAction(e -> setTypeFilter("PROMOTION"));
        destockTypeBtn.setOnAction(e -> setTypeFilter("DESTOCKAGE"));
        bundleTypeBtn.setOnAction(e -> setTypeFilter("BUNDLE"));
        generateBtn.setOnAction(e -> generateNewConseils());
        render();
    }

    @Override
    public void applyTheme(boolean dark) {
        if (root == null) return;
        root.getStyleClass().remove("light");
        if (!dark) root.getStyleClass().add("light");
    }

    private void setStatusFilter(String filter) {
        statusFilter = filter;
        render();
    }

    private void setTypeFilter(String filter) {
        typeFilter = filter;
        render();
    }

    private void generateNewConseils() {
        dataService.generateConseils();
        conseils = dataService.getConseils();
        render();
        showInfo("Conseils IA", "Nouveaux conseils générés avec succès !");
    }

    private void showInfo(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private void render() {
        long active = conseils.stream().filter(c -> !c.isApplique() && !c.isIgnore()).count();
        long applied = conseils.stream().filter(ConseilsMarketing::isApplique).count();
        long ignored = conseils.stream().filter(ConseilsMarketing::isIgnore).count();
        double confidence = conseils.stream().mapToInt(ConseilsMarketing::getScore).filter(v -> v > 0).average().orElse(0);

        kpiRow.getChildren().setAll(
                kpi("blue", "TOTAL CONSEILS", String.valueOf(conseils.size()), "generes ce mois"),
                kpi("green", "APPLIQUES", String.valueOf(applied), "taux d'action"),
                kpi("orange", "NOUVEAUX", String.valueOf(active), "en attente d'action"),
                kpi("violet", "CONFIANCE", MarketingFormatters.percent(confidence), "taux d'application")
        );

        setSelected(activeBtn, "ACTIVE".equals(statusFilter));
        setSelected(appliedBtn, "APPLIED".equals(statusFilter));
        setSelected(ignoredBtn, "IGNORED".equals(statusFilter));
        setSelected(allTypeBtn, "ALL".equals(typeFilter));
        setSelected(promotionTypeBtn, "PROMOTION".equals(typeFilter));
        setSelected(destockTypeBtn, "DESTOCKAGE".equals(typeFilter));
        setSelected(bundleTypeBtn, "BUNDLE".equals(typeFilter));

        adviceGrid.getChildren().clear();
        conseils.stream()
                .filter(this::matchesStatus)
                .filter(this::matchesType)
                .forEach(c -> adviceGrid.getChildren().add(adviceCard(c)));
    }

    private boolean matchesStatus(ConseilsMarketing c) {
        if ("APPLIED".equals(statusFilter)) return c.isApplique();
        if ("IGNORED".equals(statusFilter)) return c.isIgnore();
        return !c.isApplique() && !c.isIgnore();
    }

    private boolean matchesType(ConseilsMarketing c) {
        if ("ALL".equals(typeFilter)) return true;
        String type = MarketingFormatters.value(c.getTypeConseil(), "").toUpperCase();
        return type.contains(typeFilter) || ("PROMOTION".equals(typeFilter) && type.contains("PROMO"));
    }

    private VBox adviceCard(ConseilsMarketing c) {
        VBox card = new VBox(12);
        card.getStyleClass().addAll("am-advice-card", dataService.typeClass(c.getTypeConseil()));
        card.setPadding(new Insets(20));
        card.setPrefWidth(330);

        HBox head = new HBox(10);
        head.setAlignment(Pos.CENTER_LEFT);
        Label title = new Label(MarketingFormatters.value(c.getProduitNom(), "Produit #" + c.getProduitId()));
        title.getStyleClass().add("am-card-title");
        Label type = new Label(MarketingFormatters.value(c.getTypeConseil(), "Promotion"));
        type.getStyleClass().add("am-chip");
        HBox.setHgrow(title, Priority.ALWAYS);
        head.getChildren().addAll(title, type);

        Label desc = new Label(MarketingFormatters.value(c.getDescription(), "Aucun conseil IA disponible."));
        desc.setWrapText(true);
        desc.getStyleClass().add("am-card-text");

        HBox meta = new HBox(8);
        Label impact = new Label(MarketingFormatters.value(c.getImpactEstime(), "Moyen"));
        impact.getStyleClass().add("am-mini-chip");
        Label score = new Label((c.getScore() <= 0 ? 75 : c.getScore()) + "%");
        score.getStyleClass().add("am-mini-chip-blue");
        meta.getChildren().addAll(impact, score);

        HBox actions = new HBox(8);
        Button accept = new Button("Accepter");
        accept.getStyleClass().add("am-primary-btn");
        accept.setOnAction(e -> updateState(c, "APPLIQUE"));
        Button ignore = new Button("Ignorer");
        ignore.getStyleClass().add("am-soft-danger-btn");
        ignore.setOnAction(e -> updateState(c, "IGNORE"));
        Button more = new Button("Plus");
        more.getStyleClass().add("am-soft-info-btn");
        actions.getChildren().addAll(accept, ignore, more);

        card.getChildren().addAll(head, desc, meta, actions);
        return card;
    }

    private void updateState(ConseilsMarketing c, String state) {
        dataService.setConseilState(c.getConseilId(), state);
        c.setApplique("APPLIQUE".equals(state));
        c.setIgnore("IGNORE".equals(state));
        render();
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

    private void setSelected(Button button, boolean selected) {
        button.getStyleClass().remove("selected");
        if (selected) button.getStyleClass().add("selected");
    }
}