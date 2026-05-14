package projet.hanouti.produit_fournisseur.controllers;

import javafx.animation.TranslateTransition;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;
import projet.hanouti.produit_fournisseur.entities.Produit;
import projet.hanouti.produit_fournisseur.services.ProduitService;
import projet.hanouti.produit_fournisseur.services.WishlistService;
import projet.hanouti.produit_fournisseur.utils.SessionManager;

import java.io.File;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class CatalogueController {

    @FXML private TextField        searchField;
    @FXML private ComboBox<String> filterCategorie;
    @FXML private ComboBox<String> sortPrix;
    @FXML private TextField        fieldPrixMin;
    @FXML private TextField        fieldPrixMax;
    @FXML private Label            lblCount;
    @FXML private VBox             catalogueContainer;
    @FXML private BorderPane       catalogueRoot;

    private final ProduitService ps = new ProduitService();
    private final WishlistService wishlistService = new WishlistService();
    private final Set<Integer> wishlistProductIds = new HashSet<>();
    private List<Produit> allProduits;
    private List<Produit> filteredProduits;

    private VBox detailPanel;

    private static final int PRODUCTS_PER_PAGE = 10;
    private static final int COLUMNS           = 4;
    private int currentPage = 0;

    @FXML
    public void initialize() {
        filterCategorie.getItems().addAll(
                "Toutes","ALIMENTAIRE","ELECTRONIQUE",
                "MEDICAMENT","HYGIENE","DECOR","MAKEUP");
        filterCategorie.setValue("Toutes");

        sortPrix.getItems().addAll(
                "Par default",
                "Prix croissant (moins cher d'abord)",
                "Prix decroissant (plus cher d'abord)");
        sortPrix.setValue("Par default");

        allProduits      = ps.getAllActiveProducts();
        filteredProduits = allProduits;
        preloadWishlist();
        buildDetailPanel();
        renderPage();
    }

    private void preloadWishlist() {
        wishlistProductIds.clear();
        int userId = SessionManager.getCurrentAcheteurId();
        if (userId > 0) {
            wishlistProductIds.addAll(wishlistService.getByUser(userId));
        }
    }

    //  Panier 
    private void ajouterAuPanier(Produit p) {
        var acheteur = SessionManager.getCurrentAcheteur();
        if (acheteur == null) {
            Alert err = new Alert(Alert.AlertType.ERROR);
            err.setTitle("Non connecte"); err.setHeaderText(null);
            err.setContentText("Aucun acheteur connecte."); err.showAndWait();
            return;
        }
        try {
            ps.ajouterAuPanier(acheteur.getIdAcheteur(),
                    p.getIdProduit(),
                    p.getPrix());
        } catch (Exception ex) {
            Alert err = new Alert(Alert.AlertType.ERROR);
            err.setTitle("Erreur panier"); err.setHeaderText(null);
            err.setContentText("Erreur : " + ex.getMessage()); err.showAndWait();
        }
    }

    //  Detail panel 
    private void buildDetailPanel() {
        detailPanel = new VBox();
        detailPanel.setPrefWidth(0);
        detailPanel.setMaxWidth(360);
        detailPanel.setVisible(false);
        detailPanel.setManaged(false);
        detailPanel.getStyleClass().add("form-panel");
        detailPanel.setStyle(
                "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.12),12,0,-2,0);");
        catalogueRoot.setRight(detailPanel);
    }

    private void openDetail(Produit p) {
        detailPanel.getChildren().clear();

        VBox innerContent = new VBox(16);
        innerContent.setPadding(new Insets(20, 18, 28, 18));

        // Close button
        HBox topRow = new HBox();
        topRow.setAlignment(Pos.CENTER_RIGHT);
        Button closeBtn = new Button("X");
        closeBtn.setStyle(
                "-fx-background-color:transparent;-fx-text-fill:#888;" +
                        "-fx-font-size:16px;-fx-cursor:hand;-fx-border-color:transparent;");
        closeBtn.setOnAction(e -> closeDetail());
        topRow.getChildren().add(closeBtn);

        javafx.scene.Node imgNode = buildLargeImageNode(p);

        Label prodName = new Label(p.getNom());
        prodName.setStyle(
                "-fx-font-size:18px;-fx-font-weight:bold;-fx-text-fill:#050A38;");
        prodName.setWrapText(true);

        //  Promo declared first so everything below can use it 
        ProduitService.PromoInfo promo =
                ps.getPromoForProduit(p.getIdProduit(), p.getPrix());

        //  Category + discount badge on same line (same as card) 
        HBox detailCatRow = new HBox(6);
        detailCatRow.setAlignment(Pos.CENTER_LEFT);
        Label catBadge = new Label(getCatEmoji(p.getCategorie()) + "  " + p.getCategorie());
        catBadge.getStyleClass().add("badge-" + getCatClass(p.getCategorie()));
        detailCatRow.getChildren().add(catBadge);
        if (promo != null) {
            Label discountBadge = new Label("-" + (int) promo.discount + "%");
            discountBadge.setStyle(
                    "-fx-background-color:#FF6B00;-fx-text-fill:white;" +
                            "-fx-font-size:10px;-fx-font-weight:bold;" +
                            "-fx-padding:2 6;-fx-background-radius:20;");
            detailCatRow.getChildren().add(discountBadge);
        }

        //  titre_acheteur beneath (same as card) 
        Label detailTitre = null;
        if (promo != null) {
            detailTitre = new Label(promo.titreAcheteur);
            detailTitre.setStyle(
                    "-fx-font-size:11px;-fx-font-weight:bold;-fx-text-fill:#FF6B00;");
            detailTitre.setWrapText(true);
        }

        //  Price 
        javafx.scene.Node detailPriceNode;
        if (promo != null) {
            VBox priceBox = new VBox(2);
            Label oldP = new Label(String.format("%.2f TND", p.getPrix()));
            oldP.setStyle("-fx-font-size:14px;-fx-text-fill:#aaa;-fx-strikethrough:true;");
            Label newP = new Label(String.format("%.2f TND", promo.prixApres));
            newP.setStyle("-fx-font-size:22px;-fx-font-weight:bold;-fx-text-fill:#16A34A;");
            priceBox.getChildren().addAll(oldP, newP);
            detailPriceNode = priceBox;
        } else {
            Label prodPrice = new Label(String.format("%.2f TND", p.getPrix()));
            prodPrice.setStyle(
                    "-fx-font-size:22px;-fx-font-weight:bold;-fx-text-fill:#192BCC;");
            detailPriceNode = prodPrice;
        }

        Separator sep1 = new Separator();

        Label descTitle = new Label("Description");
        descTitle.setStyle("-fx-font-size:12px;-fx-font-weight:bold;-fx-text-fill:#555;");
        String descText = (p.getDescription() != null && !p.getDescription().trim().isEmpty())
                ? p.getDescription() : "Aucune description disponible.";
        Label descLabel = new Label(descText);
        descLabel.setStyle("-fx-font-size:12px;-fx-text-fill:#444;-fx-line-spacing:3;");
        descLabel.setWrapText(true);

        Separator sep2 = new Separator();

        String stockStyle =
                p.getQuantiteStock() <= p.getSeuilAlerte()          ? "stock-badge-red"
                        : p.getQuantiteStock() <= p.getSeuilAlerte() * 2    ? "stock-badge-orange"
                        : "stock-badge-green";
        Label stockLbl = new Label("Stock: " + p.getQuantiteStock() + " unite(s)");
        stockLbl.getStyleClass().add(stockStyle);

        Label vendeurLbl = new Label("Vendu par: " + ps.getVendeurName(p.getIdVendeur()));
        vendeurLbl.setStyle("-fx-font-size:11px;-fx-text-fill:#888;");

        Separator sep3 = new Separator();

        Label ratingTitle = new Label("Evaluation du produit");
        ratingTitle.setStyle(
                "-fx-font-size:12px;-fx-font-weight:bold;-fx-text-fill:#555;");

        Label scoreLabel = new Label();
        if (p.getMoyenne() > 0) {
            scoreLabel.setText(String.format("%.1f / 5", p.getMoyenne()));
            scoreLabel.setStyle("-fx-font-size:13px;-fx-font-weight:bold;" +
                    "-fx-text-fill:#444;-fx-padding:0 0 0 8;");
        } else {
            scoreLabel.setText("Aucun vote");
            scoreLabel.setStyle("-fx-font-size:13px;-fx-font-weight:bold;" +
                    "-fx-text-fill:#888;-fx-padding:0 0 0 8;");
        }

        HBox starsBox = new HBox(4);
        starsBox.setAlignment(Pos.CENTER_LEFT);

        final int[] currentRating = {
                p.getMoyenne() > 0 ? (int) Math.round(p.getMoyenne()) : 0
        };
        Button[] stars = new Button[5];

        for (int i = 0; i < 5; i++) {
            final int starIndex = i + 1;
            Button star = new Button(starIndex <= currentRating[0] ? "\u2605" : "\u2606");
            star.setStyle(
                    "-fx-background-color:transparent;" +
                            "-fx-font-size:24px;-fx-cursor:hand;" +
                            "-fx-border-color:transparent;-fx-padding:0 2;" +
                            "-fx-text-fill:" + (starIndex <= currentRating[0]
                            ? "#FFB300" : "#ccc") + ";");
            stars[i] = star;
        }

        for (int i = 0; i < 5; i++) {
            final int starIndex = i + 1;
            Button star = stars[i];

            star.setOnMouseEntered(e -> {
                for (int j = 0; j < 5; j++) {
                    stars[j].setText(j < starIndex ? "\u2605" : "\u2606");
                    stars[j].setStyle(
                            "-fx-background-color:transparent;" +
                                    "-fx-font-size:24px;-fx-cursor:hand;" +
                                    "-fx-border-color:transparent;-fx-padding:0 2;" +
                                    "-fx-text-fill:" + (j < starIndex ? "#FFB300" : "#ccc") + ";");
                }
            });

            star.setOnMouseExited(e -> {
                for (int j = 0; j < 5; j++) {
                    stars[j].setText(j < currentRating[0] ? "\u2605" : "\u2606");
                    stars[j].setStyle(
                            "-fx-background-color:transparent;" +
                                    "-fx-font-size:24px;-fx-cursor:hand;" +
                                    "-fx-border-color:transparent;-fx-padding:0 2;" +
                                    "-fx-text-fill:" + (j < currentRating[0]
                                    ? "#FFB300" : "#ccc") + ";");
                }
            });

            star.setOnAction(e -> {
                currentRating[0] = starIndex;
                for (int j = 0; j < 5; j++) {
                    stars[j].setText(j < starIndex ? "\u2605" : "\u2606");
                    stars[j].setStyle(
                            "-fx-background-color:transparent;" +
                                    "-fx-font-size:24px;-fx-cursor:hand;" +
                                    "-fx-border-color:transparent;-fx-padding:0 2;" +
                                    "-fx-text-fill:" + (j < starIndex ? "#FFB300" : "#ccc") + ";");
                }
                try {
                    ps.saveRating(p.getIdProduit(), starIndex);
                    Produit updated = ps.getById(p.getIdProduit());
                    if (updated != null) p.setMoyenne(updated.getMoyenne());
                    scoreLabel.setText(String.format("%.1f / 5", p.getMoyenne()));
                    scoreLabel.setStyle("-fx-font-size:13px;-fx-font-weight:bold;" +
                            "-fx-text-fill:#444;-fx-padding:0 0 0 8;");
                    renderPage();
                } catch (Exception ex) {
                    Alert err = new Alert(Alert.AlertType.ERROR);
                    err.setTitle("Erreur evaluation");
                    err.setHeaderText(null);
                    err.setContentText("Impossible d'enregistrer votre evaluation : " + ex.getMessage());
                    err.showAndWait();
                }
            });

            starsBox.getChildren().add(star);
        }
        starsBox.getChildren().add(scoreLabel);

        Separator sep4 = new Separator();

        boolean inWishlist = wishlistProductIds.contains(p.getIdProduit());
        Button heartBtn = new Button();
        heartBtn.setMaxWidth(Double.MAX_VALUE);
        applyDetailHeartState(heartBtn, inWishlist);
        heartBtn.setOnAction(e -> {
            boolean nowInWishlist = toggleWishlist(p);
            applyDetailHeartState(heartBtn, nowInWishlist);
            e.consume();
        });

        Button cartBtn = new Button("Ajouter au panier");
        cartBtn.setMaxWidth(Double.MAX_VALUE);
        cartBtn.setStyle(
                "-fx-background-color:#192BCC;-fx-text-fill:white;" +
                        "-fx-font-size:12px;-fx-font-weight:bold;-fx-background-radius:8;" +
                        "-fx-padding:10 14;-fx-cursor:hand;-fx-border-color:transparent;");
        cartBtn.setOnAction(e -> { ajouterAuPanier(p); e.consume(); });

        // Build content
        innerContent.getChildren().addAll(topRow, imgNode, prodName, detailCatRow);
        if (detailTitre != null) innerContent.getChildren().add(detailTitre);
        innerContent.getChildren().addAll(detailPriceNode,
                sep1, descTitle, descLabel,
                sep2, stockLbl, vendeurLbl,
                sep3, ratingTitle, starsBox,
                sep4, heartBtn, cartBtn);

        if (promo != null && promo.dateExpiration != null) {
            java.time.LocalDateTime expTime = promo.dateExpiration;

            Label expLabel = new Label();
            expLabel.setStyle(
                    "-fx-font-size:10px;-fx-font-weight:bold;" +
                            "-fx-background-radius:20;-fx-padding:2 8;");

            javafx.animation.Timeline timeline = new javafx.animation.Timeline(
                    new javafx.animation.KeyFrame(javafx.util.Duration.seconds(1), e -> {
                        java.time.LocalDateTime now = java.time.LocalDateTime.now();
                        if (now.isAfter(expTime)) {
                            expLabel.setText("Promo expiree");
                            expLabel.setStyle(
                                    "-fx-font-size:10px;-fx-font-weight:bold;" +
                                            "-fx-text-fill:#CC2222;" +
                                            "-fx-background-color:rgba(204,34,34,0.10);" +
                                            "-fx-background-radius:20;-fx-padding:2 8;");
                        } else {
                            long total   = java.time.Duration.between(now, expTime).getSeconds();
                            long days    = total / 86400;
                            long hours   = (total % 86400) / 3600;
                            long minutes = (total % 3600) / 60;
                            long seconds = total % 60;

                            String color = days == 0 ? "#CC2222" : days <= 7 ? "#FF6B00" : "#888";
                            String bg    = days == 0 ? "rgba(204,34,34,0.10)"
                                    : days <= 7 ? "rgba(255,107,0,0.10)"
                                    : "rgba(136,136,136,0.10)";

                            expLabel.setText(String.format(
                                    "%dj %02dh %02dm %02ds", days, hours, minutes, seconds));
                            expLabel.setStyle(
                                    "-fx-font-size:10px;-fx-font-weight:bold;" +
                                            "-fx-text-fill:" + color + ";" +
                                            "-fx-background-color:" + bg + ";" +
                                            "-fx-background-radius:20;-fx-padding:2 8;");
                        }
                    })
            );
            timeline.setCycleCount(javafx.animation.Animation.INDEFINITE);
            timeline.play();

            // Stop timeline when card is removed from scene
            expLabel.sceneProperty().addListener((obs, oldScene, newScene) -> {
                if (newScene == null) timeline.stop();
            });

            innerContent.getChildren().add(expLabel);
        }
        ScrollPane scroll = new ScrollPane(innerContent);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroll.setStyle("-fx-background-color:transparent;-fx-border-color:transparent;");
        scroll.getStyleClass().add("scroll-clean");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        detailPanel.getChildren().add(scroll);
        slideDetailIn();
    }

    private void closeDetail() { slideDetailOut(); }

    private void slideDetailIn() {
        detailPanel.setVisible(true);
        detailPanel.setManaged(true);
        detailPanel.setPrefWidth(360);
        TranslateTransition tt =
                new TranslateTransition(Duration.millis(260), detailPanel);
        tt.setFromX(360); tt.setToX(0); tt.play();
    }

    private void slideDetailOut() {
        TranslateTransition tt =
                new TranslateTransition(Duration.millis(200), detailPanel);
        tt.setFromX(0); tt.setToX(360);
        tt.setOnFinished(e -> {
            detailPanel.setVisible(false);
            detailPanel.setManaged(false);
            detailPanel.setPrefWidth(0);
        });
        tt.play();
    }

    //  Filters 
    @FXML
    public void applyFilters() {
        String search  = searchField.getText() == null ? ""
                : searchField.getText().trim().toLowerCase();
        String cat     = filterCategorie.getValue();
        double prixMin = parseDouble(fieldPrixMin.getText(), 0);
        double prixMax = parseDouble(fieldPrixMax.getText(), Double.MAX_VALUE);

        filteredProduits = allProduits.stream()
                .filter(p -> "ACTIF".equals(p.getStatut()))
                .filter(p -> search.isEmpty()
                        || p.getNom().toLowerCase().contains(search))
                .filter(p -> cat == null || "Toutes".equals(cat)
                        || cat.equals(p.getCategorie()))
                .filter(p -> p.getPrix() >= prixMin && p.getPrix() <= prixMax)
                .collect(Collectors.toList());

        String sort = sortPrix.getValue();
        if ("Prix croissant (moins cher d'abord)".equals(sort)) {
            filteredProduits.sort(Comparator.comparingDouble(Produit::getPrix));
        } else if ("Prix decroissant (plus cher d'abord)".equals(sort)) {
            filteredProduits.sort(Comparator.comparingDouble(Produit::getPrix).reversed());
        }

        currentPage = 0;
        renderPage();
        closeDetail();
    }

    @FXML
    public void resetFilters() {
        searchField.clear();
        filterCategorie.setValue("Toutes");
        sortPrix.setValue("Par default");
        fieldPrixMin.clear();
        fieldPrixMax.clear();
        filteredProduits = allProduits;
        currentPage = 0;
        renderPage();
        closeDetail();
    }

    private double parseDouble(String s, double def) {
        try { return Double.parseDouble(s.trim()); }
        catch (Exception e) { return def; }
    }

    //  Pagination 
    private void renderPage() {
        catalogueContainer.getChildren().clear();

        int totalProducts = filteredProduits.size();
        int totalPages    = (int) Math.ceil(
                (double) totalProducts / PRODUCTS_PER_PAGE);
        if (totalPages == 0) totalPages = 1;

        lblCount.setText(totalProducts + " produit(s)");

        if (totalProducts == 0) {
            Label empty = new Label("Aucun produit disponible.");
            empty.setStyle("-fx-text-fill:#aaa;-fx-font-size:15px;");
            catalogueContainer.getChildren().add(empty);
            buildPaginationBar(0, 1);
            return;
        }

        int from = currentPage * PRODUCTS_PER_PAGE;
        int to   = Math.min(from + PRODUCTS_PER_PAGE, totalProducts);
        List<Produit> pageProducts = filteredProduits.subList(from, to);

        String sort = sortPrix.getValue();
        boolean isSorted = "Prix croissant (moins cher d'abord)".equals(sort)
                || "Prix decroissant (plus cher d'abord)".equals(sort);

        VBox content = new VBox(20);

        if (isSorted) {
            content.getChildren().add(buildGrid(pageProducts));
        } else {
            Map<String, List<Produit>> byCategory = pageProducts.stream()
                    .collect(Collectors.groupingBy(Produit::getCategorie));
            String[] order = {
                    "ALIMENTAIRE","ELECTRONIQUE","MEDICAMENT",
                    "HYGIENE","DECOR","MAKEUP"};
            for (String c : order) {
                if (!byCategory.containsKey(c)) continue;
                List<Produit> list = byCategory.get(c);
                HBox header = new HBox(10);
                header.setAlignment(Pos.CENTER_LEFT);
                Label icon  = new Label(getCatEmoji(c));
                icon.setStyle("-fx-font-size:18px;");
                Label name  = new Label(c);
                name.setStyle(
                        "-fx-font-size:17px;-fx-font-weight:bold;-fx-text-fill:#192BCC;");
                Label count = new Label("(" + list.size() + ")");
                count.setStyle("-fx-font-size:12px;-fx-text-fill:#aaa;");
                header.getChildren().addAll(icon, name, count);
                content.getChildren().addAll(header, new Separator(), buildGrid(list));
            }
        }

        catalogueContainer.getChildren().add(content);
        buildPaginationBar(currentPage, totalPages);
    }

    private GridPane buildGrid(List<Produit> list) {
        GridPane grid = new GridPane();
        grid.setHgap(16); grid.setVgap(16);
        grid.setPadding(new Insets(8, 0, 8, 0));
        for (int i = 0; i < COLUMNS; i++) {
            ColumnConstraints cc = new ColumnConstraints();
            cc.setPercentWidth(100.0 / COLUMNS);
            cc.setHgrow(Priority.ALWAYS);
            grid.getColumnConstraints().add(cc);
        }
        int col = 0, row = 0;
        for (Produit p : list) {
            VBox card = buildCard(p);
            card.setMaxWidth(Double.MAX_VALUE);
            GridPane.setHgrow(card, Priority.ALWAYS);
            grid.add(card, col, row);
            col++;
            if (col == COLUMNS) { col = 0; row++; }
        }
        return grid;
    }

    //  Pagination bar 
    private void buildPaginationBar(int currentPg, int totalPages) {
        HBox bar = new HBox(8);
        bar.setAlignment(Pos.CENTER);
        bar.setPadding(new Insets(20, 0, 10, 0));

        Button prevBtn = new Button("<");
        prevBtn.setStyle(
                "-fx-background-color:" + (currentPg > 0 ? "#192BCC" : "#ddd") + ";" +
                        "-fx-text-fill:" + (currentPg > 0 ? "white" : "#aaa") + ";" +
                        "-fx-font-size:16px;-fx-background-radius:8;" +
                        "-fx-min-width:36px;-fx-min-height:36px;" +
                        "-fx-cursor:" + (currentPg > 0 ? "hand" : "default") + ";" +
                        "-fx-border-color:transparent;");
        prevBtn.setDisable(currentPg == 0);
        prevBtn.setOnAction(e -> { currentPage--; renderPage(); closeDetail(); });
        bar.getChildren().add(prevBtn);

        for (int i = 0; i < totalPages; i++) {
            final int pageIndex = i;
            boolean isActive = (i == currentPg);
            Button pageBtn = new Button(String.valueOf(i + 1));
            pageBtn.setStyle(
                    "-fx-background-color:" + (isActive ? "#192BCC" : "#F0F2FF") + ";" +
                            "-fx-text-fill:" + (isActive ? "white" : "#192BCC") + ";" +
                            "-fx-font-size:13px;" +
                            "-fx-font-weight:" + (isActive ? "bold" : "normal") + ";" +
                            "-fx-background-radius:8;" +
                            "-fx-min-width:36px;-fx-min-height:36px;" +
                            "-fx-cursor:" + (isActive ? "default" : "hand") + ";" +
                            "-fx-border-color:" + (isActive ? "transparent" : "#192BCC") + ";" +
                            "-fx-border-radius:8;-fx-border-width:1;");
            if (!isActive)
                pageBtn.setOnAction(e -> {
                    currentPage = pageIndex; renderPage(); closeDetail();
                });
            bar.getChildren().add(pageBtn);
        }

        Button nextBtn = new Button(">");
        nextBtn.setStyle(
                "-fx-background-color:" +
                        (currentPg < totalPages - 1 ? "#192BCC" : "#ddd") + ";" +
                        "-fx-text-fill:" +
                        (currentPg < totalPages - 1 ? "white" : "#aaa") + ";" +
                        "-fx-font-size:16px;-fx-background-radius:8;" +
                        "-fx-min-width:36px;-fx-min-height:36px;" +
                        "-fx-cursor:" + (currentPg < totalPages - 1 ? "hand" : "default") + ";" +
                        "-fx-border-color:transparent;");
        nextBtn.setDisable(currentPg >= totalPages - 1);
        nextBtn.setOnAction(e -> { currentPage++; renderPage(); closeDetail(); });

        Label pageInfo = new Label("Page " + (currentPg + 1) + " / " + totalPages);
        pageInfo.setStyle("-fx-font-size:12px;-fx-text-fill:#888;-fx-padding:0 8;");

        bar.getChildren().addAll(nextBtn, pageInfo);
        catalogueContainer.getChildren().add(bar);
    }

    //  Build product card 
    private VBox buildCard(Produit p) {
        VBox card = new VBox(8);
        card.getStyleClass().add("product-card");
        card.setPadding(new Insets(14));
        card.setStyle("-fx-cursor:hand;");

        javafx.scene.Node imgNode = buildImageNode(p);

        Label name = new Label(p.getNom());
        name.getStyleClass().add("card-title");
        name.setWrapText(true);

        Label vendeur = new Label("Par: " + ps.getVendeurName(p.getIdVendeur()));
        vendeur.setStyle("-fx-font-size:11px;-fx-text-fill:#888;");

        ProduitService.PromoInfo promo =
                ps.getPromoForProduit(p.getIdProduit(), p.getPrix());

        // Category + discount on same line
        HBox catRow = new HBox(6);
        catRow.setAlignment(Pos.CENTER_LEFT);
        Label badge = new Label(p.getCategorie());
        badge.getStyleClass().add("badge-" + getCatClass(p.getCategorie()));
        catRow.getChildren().add(badge);
        if (promo != null) {
            Label discountBadge = new Label("-" + (int) promo.discount + "%");
            discountBadge.setStyle(
                    "-fx-background-color:#FF6B00;-fx-text-fill:white;" +
                            "-fx-font-size:10px;-fx-font-weight:bold;" +
                            "-fx-padding:2 6;-fx-background-radius:20;");
            catRow.getChildren().add(discountBadge);
        }

        // titre_acheteur beneath
        Label titreLabel = null;
        if (promo != null) {
            titreLabel = new Label(promo.titreAcheteur);
            titreLabel.setStyle(
                    "-fx-font-size:11px;-fx-font-weight:bold;-fx-text-fill:#FF6B00;");
            titreLabel.setWrapText(true);
        }

        // Price
        javafx.scene.Node priceNode;
        if (promo != null) {
            VBox priceBox = new VBox(2);
            Label oldP = new Label(String.format("%.2f TND", p.getPrix()));
            oldP.setStyle("-fx-font-size:12px;-fx-text-fill:#aaa;-fx-strikethrough:true;");
            Label newP = new Label(String.format("%.2f TND", promo.prixApres));
            newP.setStyle("-fx-font-size:16px;-fx-font-weight:bold;-fx-text-fill:#16A34A;");
            priceBox.getChildren().addAll(oldP, newP);
            priceNode = priceBox;
        } else {
            Label price = new Label(String.format("%.2f TND", p.getPrix()));
            price.getStyleClass().add("card-price");
            priceNode = price;
        }

        String stockStyle =
                p.getQuantiteStock() <= p.getSeuilAlerte()       ? "stock-badge-red"
                        : p.getQuantiteStock() <= p.getSeuilAlerte() * 2 ? "stock-badge-orange"
                        : "stock-badge-green";
        Label stock = new Label("Stock: " + p.getQuantiteStock());
        stock.getStyleClass().add(stockStyle);

        boolean inWishlist = wishlistProductIds.contains(p.getIdProduit());
        Button heartBtn = new Button();
        applyCardHeartState(heartBtn, inWishlist);
        heartBtn.setOnAction(e -> {
            boolean nowInWishlist = toggleWishlist(p);
            applyCardHeartState(heartBtn, nowInWishlist);
            e.consume();
        });
        Button cartBtn = new Button("Ajouter au panier");
        cartBtn.setStyle(
                "-fx-background-color:#192BCC;-fx-text-fill:white;" +
                        "-fx-font-size:13px;-fx-background-radius:10;" +
                        "-fx-padding:12 14;-fx-cursor:hand;-fx-border-color:transparent;");
        cartBtn.setMinWidth(150);
        cartBtn.setPrefWidth(160);
        cartBtn.setMaxWidth(Double.MAX_VALUE);
        cartBtn.setOnAction(e -> { ajouterAuPanier(p); e.consume(); });

        HBox ratingBox = new HBox(3);
        ratingBox.setAlignment(Pos.CENTER_RIGHT);
        ratingBox.setMinWidth(48);
        if (p.getMoyenne() > 0) {
            Label star = new Label("\u2605");
            star.setStyle("-fx-font-size:14px;-fx-text-fill:#FFB300;-fx-font-weight:bold;");
            Label avg = new Label(String.format("%.1f", p.getMoyenne()));
            avg.setStyle("-fx-font-size:13px;-fx-text-fill:#BFC7FF;-fx-font-weight:bold;");
            ratingBox.getChildren().addAll(star, avg);
        }

        HBox actionButtons = new HBox(8, heartBtn, cartBtn);
        actionButtons.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(cartBtn, Priority.ALWAYS);

        Region bottomSpacer = new Region();
        HBox.setHgrow(bottomSpacer, Priority.ALWAYS);
        HBox bottomRow = new HBox(8, actionButtons, bottomSpacer, ratingBox);
        bottomRow.setAlignment(Pos.CENTER_LEFT);
        bottomRow.setMaxWidth(Double.MAX_VALUE);

        Region cardSpacer = new Region();
        VBox.setVgrow(cardSpacer, Priority.ALWAYS);

        card.getChildren().addAll(imgNode, name, vendeur, catRow);
        if (titreLabel != null) card.getChildren().add(titreLabel);
        card.getChildren().addAll(priceNode, stock, cardSpacer, bottomRow);

        if (promo != null && promo.dateExpiration != null) {
            java.time.LocalDateTime expTime = promo.dateExpiration;

            Label expLabel = new Label();
            expLabel.setStyle(
                    "-fx-font-size:10px;-fx-font-weight:bold;" +
                            "-fx-background-radius:20;-fx-padding:2 8;");

            javafx.animation.Timeline timeline = new javafx.animation.Timeline(
                    new javafx.animation.KeyFrame(javafx.util.Duration.seconds(1), e -> {
                        java.time.LocalDateTime now = java.time.LocalDateTime.now();
                        if (now.isAfter(expTime)) {
                            expLabel.setText("Promo expiree");
                            expLabel.setStyle(
                                    "-fx-font-size:10px;-fx-font-weight:bold;" +
                                            "-fx-text-fill:#CC2222;" +
                                            "-fx-background-color:rgba(204,34,34,0.10);" +
                                            "-fx-background-radius:20;-fx-padding:2 8;");
                        } else {
                            long total   = java.time.Duration.between(now, expTime).getSeconds();
                            long days    = total / 86400;
                            long hours   = (total % 86400) / 3600;
                            long minutes = (total % 3600) / 60;
                            long seconds = total % 60;

                            String color = days == 0 ? "#CC2222" : days <= 7 ? "#FF6B00" : "#888";
                            String bg    = days == 0 ? "rgba(204,34,34,0.10)"
                                    : days <= 7 ? "rgba(255,107,0,0.10)"
                                    : "rgba(136,136,136,0.10)";

                            expLabel.setText(String.format(
                                    "%dj %02dh %02dm %02ds", days, hours, minutes, seconds));
                            expLabel.setStyle(
                                    "-fx-font-size:10px;-fx-font-weight:bold;" +
                                            "-fx-text-fill:" + color + ";" +
                                            "-fx-background-color:" + bg + ";" +
                                            "-fx-background-radius:20;-fx-padding:2 8;");
                        }
                    })
            );
            timeline.setCycleCount(javafx.animation.Animation.INDEFINITE);
            timeline.play();

            // Stop timeline when card is removed from scene
            expLabel.sceneProperty().addListener((obs, oldScene, newScene) -> {
                if (newScene == null) timeline.stop();
            });

            card.getChildren().add(expLabel);
        }
        card.setOnMouseClicked(e -> openDetail(p));
        return card;


    }

    private boolean toggleWishlist(Produit p) {
        int userId = SessionManager.getCurrentAcheteurId();
        if (userId <= 0) {
            Alert err = new Alert(Alert.AlertType.ERROR);
            err.setTitle("Non connecte");
            err.setHeaderText(null);
            err.setContentText("Aucun acheteur connecte.");
            err.showAndWait();
            return wishlistProductIds.contains(p.getIdProduit());
        }

        int produitId = p.getIdProduit();
        boolean alreadyIn = wishlistProductIds.contains(produitId);
        boolean ok;
        if (alreadyIn) {
            ok = wishlistService.remove(userId, produitId);
            if (ok) wishlistProductIds.remove(produitId);
        } else {
            ok = wishlistService.add(userId, produitId);
            if (ok || wishlistService.isInWishlist(userId, produitId)) {
                wishlistProductIds.add(produitId);
                ok = true;
            }
        }

        if (!ok) {
            Alert err = new Alert(Alert.AlertType.ERROR);
            err.setTitle("Erreur favoris");
            err.setHeaderText(null);
            err.setContentText("Impossible de mettre a jour vos favoris.");
            err.showAndWait();
        }
        return wishlistProductIds.contains(produitId);
    }

    private void applyCardHeartState(Button heartBtn, boolean inWishlist) {
        heartBtn.setText(inWishlist ? "\u2665" : "\u2661");
        heartBtn.setStyle(
                "-fx-background-color:" + (inWishlist ? "#CC2222" : "#F5F5F5") + ";" +
                        "-fx-text-fill:" + (inWishlist ? "white" : "#CC2222") + ";" +
                        "-fx-font-size:17px;-fx-font-weight:bold;-fx-background-radius:24;" +
                        "-fx-min-width:48px;-fx-min-height:48px;" +
                        "-fx-max-width:48px;-fx-max-height:48px;" +
                        "-fx-cursor:hand;-fx-border-color:transparent;");
    }

    private void applyDetailHeartState(Button heartBtn, boolean inWishlist) {
        heartBtn.setText(inWishlist ? "\u2665  Ajoute aux favoris" : "\u2661  Ajouter aux favoris");
        heartBtn.setStyle(
                "-fx-background-color:" + (inWishlist ? "#CC2222" : "#FFEEEE") + ";" +
                        "-fx-text-fill:" + (inWishlist ? "white" : "#CC2222") + ";" +
                        "-fx-font-size:12px;-fx-background-radius:8;" +
                        "-fx-padding:10 14;-fx-cursor:hand;-fx-border-color:transparent;");
    }

    //  Image helpers 
    private javafx.scene.Node buildImageNode(Produit p) {
        String ip = p.getImage();
        StackPane container = new StackPane();
        container.setAlignment(Pos.CENTER);
        container.setMaxWidth(Double.MAX_VALUE);
        container.setPrefHeight(180);
        container.setMinHeight(180);
        container.setMaxHeight(180);
        container.setStyle("-fx-background-color:#F5F5F5;-fx-background-radius:12;");

        Rectangle clip = new Rectangle();
        clip.setArcWidth(24);
        clip.setArcHeight(24);
        clip.widthProperty().bind(container.widthProperty());
        clip.heightProperty().bind(container.heightProperty());
        container.setClip(clip);

        if (ip != null && !ip.trim().isEmpty()) {
            try {
                Image img = ip.startsWith("http") || ip.startsWith("file:")
                        ? new Image(ip, 640, 420, true, true)
                        : new Image(new File(ip).toURI().toString(), 640, 420, true, true);
                ImageView iv = new ImageView(img);
                iv.setPreserveRatio(false);
                iv.setSmooth(true);
                iv.setCache(true);
                container.widthProperty().addListener((obs, oldW, newW) ->
                        updateCoverViewport(iv, img, newW.doubleValue(), container.getHeight()));
                container.heightProperty().addListener((obs, oldH, newH) ->
                        updateCoverViewport(iv, img, container.getWidth(), newH.doubleValue()));
                updateCoverViewport(iv, img, 260, 180);
                container.getChildren().add(iv);
                return container;
            } catch (Exception ignored) {}
        }
        Label emoji = new Label(getCatEmoji(p.getCategorie()));
        emoji.setStyle(
                "-fx-font-size:48px;" +
                        "-fx-pref-width:160px;-fx-pref-height:160px;" +
                        "-fx-alignment:center;");
        container.getChildren().add(emoji);
        return container;
    }

    private void updateCoverViewport(ImageView iv, Image img, double frameW, double frameH) {
        if (frameW <= 0 || frameH <= 0 || img.getWidth() <= 0 || img.getHeight() <= 0) return;

        double imgW = img.getWidth();
        double imgH = img.getHeight();
        double frameRatio = frameW / frameH;
        double imgRatio = imgW / imgH;

        if (frameRatio > imgRatio) {
            double cropH = imgW / frameRatio;
            double y = (imgH - cropH) / 2.0;
            iv.setViewport(new Rectangle2D(0, y, imgW, cropH));
        } else {
            double cropW = imgH * frameRatio;
            double x = (imgW - cropW) / 2.0;
            iv.setViewport(new Rectangle2D(x, 0, cropW, imgH));
        }

        iv.setFitWidth(frameW);
        iv.setFitHeight(frameH);
    }

    private javafx.scene.Node buildLargeImageNode(Produit p) {
        String ip = p.getImage();
        VBox box = new VBox();
        box.setAlignment(Pos.CENTER);
        box.getStyleClass().add("preview-box");
        box.setStyle("-fx-padding:10;");
        if (ip != null && !ip.trim().isEmpty()) {
            try {
                Image img = ip.startsWith("http") || ip.startsWith("file:")
                        ? new Image(ip, 320, 180, true, true)
                        : new Image(new File(ip).toURI().toString(), 320, 180, true, true);
                ImageView iv = new ImageView(img);
                iv.setFitWidth(320); iv.setFitHeight(180);
                iv.setPreserveRatio(true);
                box.getChildren().add(iv);
                return box;
            } catch (Exception ignored) {}
        }
        box.getChildren().add(buildEmojiPlaceholder(p.getCategorie(), 320, 150, 48));
        return box;
    }

    private Label buildEmojiPlaceholder(String cat, double w, double h, double fs) {
        Label lbl = new Label(getCatEmoji(cat));
        lbl.setStyle(
                "-fx-font-size:" + fs + "px;-fx-alignment:center;" +
                        "-fx-pref-width:" + w + "px;-fx-pref-height:" + h + "px;" +
                        "-fx-background-color:" + getCatColor(cat) + ";" +
                        "-fx-background-radius:8;");
        return lbl;
    }

    private String getCatEmoji(String cat) {
        return switch (cat) {
            case "ALIMENTAIRE"  -> "\u2615";
            case "ELECTRONIQUE" -> "\u26A1";
            case "MEDICAMENT"   -> "+";
            case "HYGIENE"      -> "\u2726";
            case "DECOR"        -> "\u25C6";
            case "MAKEUP"       -> "\u2665";
            default             -> "\u25CF";
        };
    }

    private String getCatColor(String cat) {
        return switch (cat) {
            case "ALIMENTAIRE"  -> "#EEFFEE";
            case "ELECTRONIQUE" -> "#FFF8EE";
            case "MEDICAMENT"   -> "#EEF0FF";
            case "HYGIENE"      -> "#E8F8FF";
            case "DECOR"        -> "#F0FFE8";
            case "MAKEUP"       -> "#FFF0F8";
            default             -> "#F5F5F5";
        };
    }

    private String getCatClass(String cat) {
        return switch (cat) {
            case "ALIMENTAIRE"  -> "green";
            case "ELECTRONIQUE" -> "orange";
            case "MEDICAMENT"   -> "blue";
            case "HYGIENE"      -> "blue";
            case "DECOR"        -> "green";
            case "MAKEUP"       -> "purple";
            default             -> "gray";
        };
    }
}
