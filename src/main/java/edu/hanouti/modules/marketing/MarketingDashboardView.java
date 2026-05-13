package edu.hanouti.modules.marketing;

import edu.hanouti.gui.HanoutiDashboard;
import edu.hanouti.entities.CampagneMarketing;
import edu.hanouti.entities.StatistiquesVentes;
import edu.hanouti.services.CampagneMarketingService;
import edu.hanouti.services.StatistiquesVentesService;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Text;

import java.util.List;

/**
 * Vue principale du module Marketing (Vendeur)
 * Réutilise le thème de HanoutiDashboard et la connexion MyConnection
 */
public class MarketingDashboardView {

    private final CampagneMarketingService campagnesService = new CampagneMarketingService();
    private final StatistiquesVentesService statsService = new StatistiquesVentesService();

    /**
     * Construit la vue principale du module Marketing
     * @param darkMode État du thème (dark/light) depuis HanoutiDashboard
     * @return ScrollPane contenant le contenu du module
     */
    public ScrollPane buildView(boolean darkMode) {
        // Couleurs adaptatives selon le thème
        String bgDeep = darkMode ? "#0a0d1a" : "#f0f4f8";
        String bgCard = darkMode ? "#111425" : "#ffffff";
        String text1 = darkMode ? "#F1F5F9" : "#0f172a";
        String text2 = darkMode ? "#94A3B8" : "#334155";
        String text3 = darkMode ? "#475569" : "#64748b";
        String accentBlue = "#38bdf8";
        String accentGreen = "#10B981";
        String accentOrange = "#F97316";

        VBox root = new VBox(24);
        root.setPadding(new Insets(28, 32, 32, 32));
        root.setStyle("-fx-background-color: " + bgDeep + ";");

        // ═══════════════════════════════════════════════════════════
        // HEADER — Titre du module
        // ═══════════════════════════════════════════════════════════
        VBox header = new VBox(8);
        header.setAlignment(Pos.CENTER_LEFT);

        Label titleLabel = new Label("📊 Espace Marketing Vendeur");
        titleLabel.setStyle(
            "-fx-font-size: 28px; " +
            "-fx-font-weight: 900; " +
            "-fx-text-fill: " + text1 + ";"
        );

        Label subtitleLabel = new Label("Gérez vos campagnes, analysez vos performances et optimisez vos ventes");
        subtitleLabel.setStyle(
            "-fx-font-size: 14px; " +
            "-fx-text-fill: " + text2 + ";"
        );

        header.getChildren().addAll(titleLabel, subtitleLabel);

        // ═══════════════════════════════════════════════════════════
        // STATISTIQUES RAPIDES — Cartes KPI
        // ═══════════════════════════════════════════════════════════
        HBox kpiRow = new HBox(16);
        kpiRow.setAlignment(Pos.CENTER_LEFT);

        try {
            List<CampagneMarketing> campagnes = campagnesService.getData();
            List<StatistiquesVentes> stats = statsService.getData();

            int totalCampagnes = campagnes.size();
            long campagnesActives = campagnes.stream()
                .filter(c -> "ACTIVE".equalsIgnoreCase(c.getStatut()))
                .count();
            double budgetTotal = campagnesService.getBudgetTotal();
            double depenseTotal = campagnesService.getDepenseTotal();

            // Carte 1 : Campagnes actives
            VBox card1 = createKpiCard(
                "🎯", 
                String.valueOf(campagnesActives), 
                "Campagnes Actives",
                accentBlue,
                bgCard,
                text1,
                text2,
                darkMode
            );

            // Carte 2 : Total campagnes
            VBox card2 = createKpiCard(
                "📢", 
                String.valueOf(totalCampagnes), 
                "Total Campagnes",
                accentGreen,
                bgCard,
                text1,
                text2,
                darkMode
            );

            // Carte 3 : Budget utilisé
            double budgetPct = budgetTotal > 0 ? (depenseTotal / budgetTotal * 100) : 0;
            VBox card3 = createKpiCard(
                "💰", 
                String.format("%.0f%%", budgetPct), 
                "Budget Utilisé",
                accentOrange,
                bgCard,
                text1,
                text2,
                darkMode
            );

            // Carte 4 : Produits suivis
            VBox card4 = createKpiCard(
                "📦", 
                String.valueOf(stats.size()), 
                "Produits Suivis",
                "#8B5CF6",
                bgCard,
                text1,
                text2,
                darkMode
            );

            HBox.setHgrow(card1, Priority.ALWAYS);
            HBox.setHgrow(card2, Priority.ALWAYS);
            HBox.setHgrow(card3, Priority.ALWAYS);
            HBox.setHgrow(card4, Priority.ALWAYS);

            kpiRow.getChildren().addAll(card1, card2, card3, card4);

        } catch (Exception e) {
            Label errorLabel = new Label("⚠️ Erreur lors du chargement des statistiques");
            errorLabel.setStyle("-fx-text-fill: " + accentOrange + "; -fx-font-size: 14px;");
            kpiRow.getChildren().add(errorLabel);
        }

        // ═══════════════════════════════════════════════════════════
        // SECTION ACTIONS RAPIDES
        // ═══════════════════════════════════════════════════════════
        VBox actionsSection = new VBox(16);
        actionsSection.setStyle(
            "-fx-background-color: " + bgCard + "; " +
            "-fx-background-radius: 16; " +
            "-fx-padding: 24; " +
            "-fx-border-color: " + (darkMode ? "rgba(255,255,255,0.07)" : "rgba(0,0,0,0.08)") + "; " +
            "-fx-border-width: 1; " +
            "-fx-border-radius: 16;"
        );

        Label actionsTitle = new Label("⚡ Actions Rapides");
        actionsTitle.setStyle(
            "-fx-font-size: 18px; " +
            "-fx-font-weight: 700; " +
            "-fx-text-fill: " + text1 + ";"
        );

        HBox buttonsRow = new HBox(12);
        buttonsRow.setAlignment(Pos.CENTER_LEFT);

        Button btnNewCampaign = createActionButton("➕ Nouvelle Campagne", accentBlue, darkMode);
        Button btnViewStats = createActionButton("📊 Voir Statistiques", accentGreen, darkMode);
        Button btnManageProducts = createActionButton("📦 Gérer Produits", "#8B5CF6", darkMode);

        buttonsRow.getChildren().addAll(btnNewCampaign, btnViewStats, btnManageProducts);
        actionsSection.getChildren().addAll(actionsTitle, buttonsRow);

        // ═══════════════════════════════════════════════════════════
        // MESSAGE D'INFORMATION
        // ═══════════════════════════════════════════════════════════
        VBox infoBox = new VBox(8);
        infoBox.setStyle(
            "-fx-background-color: " + (darkMode ? "rgba(56,189,248,0.1)" : "rgba(56,189,248,0.08)") + "; " +
            "-fx-background-radius: 12; " +
            "-fx-padding: 16; " +
            "-fx-border-color: " + accentBlue + "44; " +
            "-fx-border-width: 1; " +
            "-fx-border-radius: 12;"
        );

        Label infoTitle = new Label("ℹ️ Module Marketing Vendeur");
        infoTitle.setStyle(
            "-fx-font-size: 14px; " +
            "-fx-font-weight: 700; " +
            "-fx-text-fill: " + text1 + ";"
        );

        Label infoText = new Label(
            "Ce module vous permet de gérer vos campagnes marketing, suivre vos performances de vente, " +
            "et optimiser votre stratégie commerciale. Utilisez les actions rapides ci-dessus pour commencer."
        );
        infoText.setWrapText(true);
        infoText.setStyle(
            "-fx-font-size: 13px; " +
            "-fx-text-fill: " + text2 + ";"
        );

        infoBox.getChildren().addAll(infoTitle, infoText);

        // ═══════════════════════════════════════════════════════════
        // ASSEMBLAGE FINAL
        // ═══════════════════════════════════════════════════════════
        root.getChildren().addAll(header, kpiRow, actionsSection, infoBox);

        ScrollPane scrollPane = new ScrollPane(root);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle(
            "-fx-background-color: transparent; " +
            "-fx-background: " + bgDeep + "; " +
            "-fx-border-color: transparent;"
        );

        return scrollPane;
    }

    /**
     * Crée une carte KPI stylisée
     */
    private VBox createKpiCard(String icon, String value, String label, 
                               String accentColor, String bgCard, 
                               String text1, String text2, boolean darkMode) {
        VBox card = new VBox(12);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(20));
        card.setStyle(
            "-fx-background-color: " + bgCard + "; " +
            "-fx-background-radius: 14; " +
            "-fx-border-color: " + (darkMode ? "rgba(255,255,255,0.07)" : "rgba(0,0,0,0.08)") + "; " +
            "-fx-border-width: 1; " +
            "-fx-border-radius: 14; " +
            "-fx-effect: dropshadow(gaussian, " + (darkMode ? "rgba(0,0,0,0.3)" : "rgba(0,0,0,0.08)") + ", 8, 0, 0, 2);"
        );

        // Icône
        Label iconLabel = new Label(icon);
        iconLabel.setStyle(
            "-fx-font-size: 32px; " +
            "-fx-text-fill: " + accentColor + ";"
        );

        // Valeur
        Label valueLabel = new Label(value);
        valueLabel.setStyle(
            "-fx-font-size: 28px; " +
            "-fx-font-weight: 900; " +
            "-fx-text-fill: " + text1 + ";"
        );

        // Label
        Label textLabel = new Label(label);
        textLabel.setStyle(
            "-fx-font-size: 13px; " +
            "-fx-text-fill: " + text2 + ";"
        );

        card.getChildren().addAll(iconLabel, valueLabel, textLabel);
        return card;
    }

    /**
     * Crée un bouton d'action stylisé
     */
    private Button createActionButton(String text, String accentColor, boolean darkMode) {
        Button btn = new Button(text);
        btn.setStyle(
            "-fx-background-color: " + accentColor + "; " +
            "-fx-text-fill: white; " +
            "-fx-font-size: 13px; " +
            "-fx-font-weight: 700; " +
            "-fx-background-radius: 10; " +
            "-fx-padding: 12 24; " +
            "-fx-cursor: hand; " +
            "-fx-effect: dropshadow(gaussian, " + accentColor + "44, 8, 0, 0, 2);"
        );

        // Effet hover
        btn.setOnMouseEntered(e -> btn.setStyle(
            "-fx-background-color: derive(" + accentColor + ", 10%); " +
            "-fx-text-fill: white; " +
            "-fx-font-size: 13px; " +
            "-fx-font-weight: 700; " +
            "-fx-background-radius: 10; " +
            "-fx-padding: 12 24; " +
            "-fx-cursor: hand; " +
            "-fx-effect: dropshadow(gaussian, " + accentColor + "66, 12, 0, 0, 3);"
        ));

        btn.setOnMouseExited(e -> btn.setStyle(
            "-fx-background-color: " + accentColor + "; " +
            "-fx-text-fill: white; " +
            "-fx-font-size: 13px; " +
            "-fx-font-weight: 700; " +
            "-fx-background-radius: 10; " +
            "-fx-padding: 12 24; " +
            "-fx-cursor: hand; " +
            "-fx-effect: dropshadow(gaussian, " + accentColor + "44, 8, 0, 0, 2);"
        ));

        return btn;
    }
}
