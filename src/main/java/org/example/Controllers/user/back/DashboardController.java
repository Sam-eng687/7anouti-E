package org.example.Controllers.user.back;

import javafx.animation.*;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.shape.Circle;
import javafx.scene.text.Text;
import javafx.util.Duration;

import org.example.Entites.user.Role;
import org.example.Entites.user.Status;
import org.example.Entites.user.User;
import org.example.Services.user.UserCRUD;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public class DashboardController {

    @FXML private AnchorPane rootPane;
    @FXML private VBox sidebar;
    @FXML private ImageView sidebarLogo;
    @FXML private VBox navContainer;
    @FXML private Button navUsers, navProducts, navOrders, navStats, navSettings, navSupport;
    @FXML private Button themeToggleBtn;
    @FXML private Label themeIcon;
    @FXML private Label themeLabel;
    @FXML private Button logoutBtn;

    @FXML private VBox adminUsersContent;


    @FXML private Button notifBtn;
    @FXML private Button cartBtn;
    @FXML private MenuButton profileMenu;
    @FXML private MenuItem editProfileItem;
    @FXML private MenuItem paymentHistoryItem;
    @FXML private MenuItem logoutProfileItem;

    private boolean isDarkMode = true;
    @FXML private HBox headerBar;
    @FXML private Text headerTitle, headerSubtitle;
    @FXML private Label adminNameLabel, adminRoleLabel;
    @FXML private StackPane adminAvatarWrap;
    @FXML private ImageView adminAvatar;
    @FXML private Label statTotal, statActive, statBanned, statRoles;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> filterRole, filterStatus;
    @FXML private Button refreshBtn;
    @FXML private TableView<User> usersTable;
    @FXML private TableColumn<User, String> colId, colNom, colPrenom, colEmail, colRole, colStatus, colDate, colActions;
    @FXML private StackPane overlayPane;
    @FXML private VBox detailCard;
    @FXML private Button closeDetailBtn;
    @FXML private Text detailTitle;
    @FXML private ImageView detailAvatar;
    @FXML private Label detailAvatarLetter;
    @FXML private TextField detailNom, detailPrenom, detailEmail, detailTel, detailDate;
    @FXML private ComboBox<String> detailRole, detailStatus;
    @FXML private Button detailSaveBtn, detailCancelBtn;
    @FXML private Label detailMessage;
    // Error labels pour la validation en temps reel
    @FXML private Label detailErrNom;
    @FXML private Label detailErrPrenom;
    @FXML private Label detailErrEmail;
    @FXML private Label detailErrTel;

    @FXML private Button hamburgerBtn;


    private final UserCRUD userCRUD = new UserCRUD();
    private ObservableList<User> allUsers = FXCollections.observableArrayList();
    private FilteredList<User> filteredUsers;
    private User currentDetailUser = null;
    private User connectedAdmin = null;

    private boolean editingOwnProfile = false;

    private boolean sidebarOpen = false;

    @FXML
    public void initialize() {

        if (!rootPane.getStyleClass().contains("dash-root"))
            rootPane.getStyleClass().add("dash-root");

        // ===== VERIFICATION DE LA SESSION =====
        org.example.Utils.SessionManager session = org.example.Utils.SessionManager.getInstance();
        if (!session.isLoggedIn()) {
            System.out.println("[Session] Aucune session active — redirection vers Login");
            redirectToLoginImmediately();
            return;
        }

        // Charger l'utilisateur depuis la session
        User sessionUser = session.getConnectedUser();
        if (sessionUser == null || sessionUser.getRole() == null) {
            redirectToLoginImmediately();
            return;
        }

        connectedAdmin = sessionUser;
        System.out.println("[Session] Session active pour: " + session.getFullName());

        setDarkMode(true);

        profileMenu.setText("👤 " + sessionUser.getPrenom());

        logoutProfileItem.setOnAction(e -> navigateToLogin());

        editProfileItem.setOnAction(e -> openProfileEdit(sessionUser));
        paymentHistoryItem.setOnAction(e ->
                showAlert("Historique", "Historique paiement prêt pour intégration.")
        );

        notifBtn.setOnAction(e ->
                showAlert("Notifications", "Centre notifications prêt pour intégration.")
        );

        if (sessionUser.getRole() == Role.acheteur) {
            cartBtn.setVisible(true);
            cartBtn.setManaged(true);
            paymentHistoryItem.setVisible(true);
        } else {
            cartBtn.setVisible(false);
            cartBtn.setManaged(false);
            paymentHistoryItem.setVisible(false);
        }

        // Theme toggle — Button normal (pas ToggleButton)
        setDarkMode(true);
        themeToggleBtn.setOnAction(e -> {
            isDarkMode = !isDarkMode;
            setDarkMode(isDarkMode);
            bounceNode(themeToggleBtn);
        });
        // Style hover logout
        logoutBtn.setOnMouseEntered(e ->
                logoutBtn.setStyle("-fx-background-color: rgba(239,68,68,0.18);"));
        logoutBtn.setOnMouseExited(e ->
                logoutBtn.setStyle("-fx-background-color: transparent;"));

        // Logo
        try {
            java.io.InputStream s = getClass().getResourceAsStream("/user/image/logo.png");
            if (s != null) sidebarLogo.setImage(new Image(s));
        } catch (Exception ignored) {}


        sidebar.setVisible(false);
        sidebar.setManaged(false);

        hamburgerBtn.setOnAction(e -> toggleSidebar());

        // Admin avatar clip
        Circle adminClip = new Circle(19, 19, 19);
        adminAvatar.setClip(adminClip);

        // Nav setup (labels inside graphic, set from FXML)
        setupRoleNavigation(sessionUser.getRole());
        configureContentByRole(sessionUser);

        logoutBtn.setVisible(false);
        logoutBtn.setManaged(false);
        // Filters
        filterRole.setItems(FXCollections.observableArrayList("Tous", "admin", "acheteur", "vendeur", "livreur"));        filterStatus.setItems(FXCollections.observableArrayList("Tous", "Unbanned", "Banned"));
        filterStatus.setValue("Tous");

        // Table columns
        colId.setCellValueFactory(c -> new SimpleStringProperty(String.valueOf(c.getValue().getId())));
        colNom.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getNom()));
        colPrenom.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getPrenom()));
        colEmail.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getE_mail()));
        colRole.setCellValueFactory(c -> {
            Role r = c.getValue().getRole();
            return new SimpleStringProperty(r != null ? r.name() : "");
        });
        colStatus.setCellValueFactory(c -> {
            Status s = c.getValue().getStatus();
            return new SimpleStringProperty(s != null ? s.name() : "");
        });
        colDate.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getDate_naiss() != null ? c.getValue().getDate_naiss() : ""));

        // Status badge
        colStatus.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setGraphic(null); setText(null); return; }
                Label badge = new Label(item);
                badge.getStyleClass().add("status-badge");
                badge.getStyleClass().add("Banned".equals(item) ? "status-banned" : "status-active");
                setGraphic(badge); setText(null);
            }
        });

        // Role badge with color
        colRole.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setGraphic(null); setText(null); return; }
                Label badge = new Label(item);
                badge.getStyleClass().add("role-badge");
                switch (item) {
                    case "admin"    -> badge.getStyleClass().add("role-admin");
                    case "vendeur"  -> badge.getStyleClass().add("role-vendeur");
                    case "acheteur" -> badge.getStyleClass().add("role-acheteur");
                    case "livreur"  -> badge.getStyleClass().add("role-livreur");
                }
                setGraphic(badge); setText(null);
            }
        });

        // ========== ACTION COLUMN — ICONES CIRCULAIRES ==========
        colActions.setCellFactory(col -> new TableCell<>() {

            // Icones : oeil / crayon / interdit-ou-check / corbeille
            private final Button viewBtn   = makeIconBtn("\u25CF", "action-view",   "Voir les details");
            private final Button editBtn   = makeIconBtn("\u270E",  "action-edit",   "Modifier");
            private final Button banBtn    = makeIconBtn("\u2715",  "action-ban",    "Bannir");
            private final HBox box = new HBox(8, viewBtn, editBtn, banBtn);
            {
                box.setAlignment(Pos.CENTER);

                viewBtn.setOnAction(e -> {
                    User u = getTableView().getItems().get(getIndex());
                    openDetail(u, false);
                });
                editBtn.setOnAction(e -> {
                    User u = getTableView().getItems().get(getIndex());
                    openDetail(u, true);
                });
                banBtn.setOnAction(e -> {
                    User u = getTableView().getItems().get(getIndex());
                    toggleBan(u);
                });
            }

            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) { setGraphic(null); return; }
                User user = getTableView().getItems().get(getIndex());
                // Ban/unban switch
                banBtn.getStyleClass().removeAll("action-ban", "action-unban");
                if (user.getStatus() == Status.Banned) {
                    banBtn.setText("\u2713");
                    banBtn.getStyleClass().add("action-unban");
                    banBtn.setTooltip(new Tooltip("Debannir"));
                } else {
                    banBtn.setText("\u2715");
                    banBtn.getStyleClass().add("action-ban");
                    banBtn.setTooltip(new Tooltip("Bannir"));
                }
                setGraphic(box);
            }
        });

        // Filtered list
        filteredUsers = new FilteredList<>(allUsers, p -> true);
        usersTable.setItems(filteredUsers);
        searchField.textProperty().addListener((obs, old, val) -> applyFilters());
        filterRole.valueProperty().addListener((obs, old, val) -> applyFilters());
        filterStatus.valueProperty().addListener((obs, old, val) -> applyFilters());

        // Detail overlay
        detailRole.setItems(FXCollections.observableArrayList("admin", "acheteur", "vendeur", "livreur"));        detailStatus.setItems(FXCollections.observableArrayList("Unbanned", "Banned"));
        closeDetailBtn.setOnAction(e -> closeDetail());
        detailCancelBtn.setOnAction(e -> closeDetail());
        detailSaveBtn.setOnAction(e -> saveDetail());
        overlayPane.setOnMouseClicked(e -> { if (e.getTarget() == overlayPane) closeDetail(); });

        // Refresh
        refreshBtn.setOnAction(e -> { loadUsers(); bounceNode(refreshBtn); });

        if (sessionUser.getRole() == Role.admin) {
            loadUsers();
        }

        playEntrance();
    }

    /** Creer un bouton icone circulaire avec style CSS */
    private Button makeIconBtn(String icon, String styleClass, String tooltipText) {
        Button btn = new Button(icon);
        btn.getStyleClass().addAll("action-btn", styleClass);
        btn.setTooltip(new Tooltip(tooltipText));
        return btn;
    }

    // =================== SET ADMIN ===================

    public void setConnectedAdmin(User admin) {
        // Si admin est null, lire depuis la session
        if (admin == null) {
            admin = org.example.Utils.SessionManager.getInstance().getConnectedUser();
        }
        this.connectedAdmin = admin;
        if (admin != null) {
            adminNameLabel.setText(admin.getNom() + " " + admin.getPrenom());
            adminRoleLabel.setText(admin.getRole() != null ? admin.getRole().name() : "vendeur");
            if (admin.getImage() != null && !admin.getImage().isBlank()) {
                try {
                    adminAvatar.setImage(new Image("file:" + admin.getImage(), 38, 38, false, true));
                } catch (Exception ignored) {}
            }
        }
    }

    // =================== DATA ===================

    private void loadUsers() {
        try {
            List<User> users = userCRUD.ShowUsers();
            allUsers.setAll(users);
            updateStats();
        } catch (SQLException ex) {
            showAlert("Erreur", "Impossible de charger les utilisateurs:\n" + ex.getMessage());
        }
    }

    private void updateStats() {
        int total  = allUsers.size();
        long active = allUsers.stream().filter(u -> u.getStatus() == Status.Unbanned).count();
        long banned = allUsers.stream().filter(u -> u.getStatus() == Status.Banned).count();
        animateStatLabel(statTotal,  total);
        animateStatLabel(statActive, (int) active);
        animateStatLabel(statBanned, (int) banned);
        statRoles.setText(String.valueOf(Role.values().length));
    }

    private void animateStatLabel(Label label, int target) {
        int current;
        try { current = Integer.parseInt(label.getText()); } catch (NumberFormatException e) { current = 0; }
        if (current == target) { label.setText(String.valueOf(target)); return; }
        final int start = current;
        Timeline tl = new Timeline();
        int steps = 20;
        for (int i = 0; i <= steps; i++) {
            final int val = start + (int) ((target - start) * ((double) i / steps));
            tl.getKeyFrames().add(new KeyFrame(Duration.millis(i * 25), e -> label.setText(String.valueOf(val))));
        }
        tl.play();
    }

    // =================== FILTERS ===================

    private void applyFilters() {
        String search = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase();
        String roleFilter   = filterRole.getValue();
        String statusFilter = filterStatus.getValue();

        filteredUsers.setPredicate(user -> {
            if (!search.isEmpty()) {
                boolean m = (user.getNom()    != null && user.getNom().toLowerCase().contains(search))
                        || (user.getPrenom()  != null && user.getPrenom().toLowerCase().contains(search))
                        || (user.getE_mail()  != null && user.getE_mail().toLowerCase().contains(search))
                        || (user.getRole()    != null && user.getRole().name().toLowerCase().contains(search));
                if (!m) return false;
            }
            if (roleFilter != null && !"Tous".equals(roleFilter))
                if (user.getRole() == null || !user.getRole().name().equals(roleFilter)) return false;
            if (statusFilter != null && !"Tous".equals(statusFilter))
                if (user.getStatus() == null || !user.getStatus().name().equals(statusFilter)) return false;
            return true;
        });
    }

    // =================== DETAIL OVERLAY ===================

    private void openDetail(User user, boolean editable) {
        currentDetailUser = user;
        detailTitle.setText(editable ? "Modifier Utilisateur" : "Details Utilisateur");
        detailNom.setText(user.getNom());
        detailPrenom.setText(user.getPrenom());
        detailEmail.setText(user.getE_mail());
        detailTel.setText(user.getNum_tel());
        detailDate.setText(user.getDate_naiss());
        detailRole.setValue(user.getRole()   != null ? user.getRole().name()   : "acheteur");
        detailStatus.setValue(user.getStatus() != null ? user.getStatus().name() : "Unbanned");
        detailAvatarLetter.setText(user.getNom() != null && !user.getNom().isEmpty()
                ? user.getNom().substring(0, 1).toUpperCase() : "?");

        if (user.getImage() != null && !user.getImage().isBlank()) {
            try {
                detailAvatar.setClip(new Circle(38, 38, 38));
                detailAvatar.setImage(new Image("file:" + user.getImage(), 76, 76, false, true));
                detailAvatarLetter.setVisible(false);
            } catch (Exception e) { detailAvatar.setImage(null); detailAvatarLetter.setVisible(true); }
        } else { detailAvatar.setImage(null); detailAvatarLetter.setVisible(true); }

        detailNom.setEditable(false);
        detailPrenom.setEditable(false);
        detailEmail.setEditable(false);
        detailTel.setEditable(false);
        detailDate.setEditable(false);
        detailRole.setVisible(true);
        detailRole.setManaged(true);
        detailStatus.setVisible(true);
        detailStatus.setManaged(true);

        detailRole.setDisable(!editable);
        detailStatus.setDisable(!editable);        detailNom.setDisable(true);
        detailPrenom.setDisable(true);
        detailEmail.setDisable(true);
        detailTel.setDisable(true);
        detailDate.setDisable(true);
        detailSaveBtn.setVisible(editable); detailSaveBtn.setManaged(editable);
        detailMessage.setVisible(false); detailMessage.setManaged(false);

        // Validation en temps reel si mode edition
        if (editable) {
            org.example.Utils.FormValidator.clearError(detailErrNom);
            org.example.Utils.FormValidator.clearError(detailErrPrenom);
            org.example.Utils.FormValidator.clearError(detailErrEmail);
            org.example.Utils.FormValidator.clearError(detailErrTel);
            org.example.Utils.FormValidator.setupEditValidation(
                    detailNom,    detailErrNom,
                    detailPrenom, detailErrPrenom,
                    detailEmail,  detailErrEmail,
                    detailTel,    detailErrTel
            );
        }

        overlayPane.setVisible(true); overlayPane.setManaged(true);
        overlayPane.setOpacity(0); detailCard.setScaleX(0.9); detailCard.setScaleY(0.9);

        FadeTransition fi = new FadeTransition(Duration.millis(200), overlayPane); fi.setToValue(1);
        ScaleTransition si = new ScaleTransition(Duration.millis(250), detailCard);
        si.setToX(1); si.setToY(1); si.setInterpolator(Interpolator.EASE_OUT);
        new ParallelTransition(fi, si).play();
    }

    private void closeDetail() {
        FadeTransition fo = new FadeTransition(Duration.millis(150), overlayPane);
        fo.setToValue(0);

        ScaleTransition so = new ScaleTransition(Duration.millis(150), detailCard);
        so.setToX(0.9);
        so.setToY(0.9);

        ParallelTransition pt = new ParallelTransition(fo, so);

        pt.setOnFinished(e -> {
            overlayPane.setVisible(false);
            overlayPane.setManaged(false);

            detailCard.setScaleX(1);
            detailCard.setScaleY(1);

            currentDetailUser = null;
            editingOwnProfile = false;
        });

        pt.play();
    }

    private void saveDetail() {
        if (currentDetailUser == null) return;

        try {
            if (editingOwnProfile) {

                currentDetailUser.setNom(detailNom.getText().trim());
                currentDetailUser.setPrenom(detailPrenom.getText().trim());
                currentDetailUser.setE_mail(detailEmail.getText().trim());
                currentDetailUser.setNum_tel(detailTel.getText().trim());
                currentDetailUser.setDate_naiss(detailDate.getText().trim());

                userCRUD.updateUserProfile(currentDetailUser);

                org.example.Utils.SessionManager.getInstance().setConnectedUser(currentDetailUser);

                updateHeaderUser(currentDetailUser);

                showDetailMessage("Profil mis à jour avec succès !", false);

            } else {

                currentDetailUser.setRole(Role.valueOf(detailRole.getValue()));
                currentDetailUser.setStatus(Status.valueOf(detailStatus.getValue()));

                userCRUD.updateUserAdminFields(currentDetailUser);

                showDetailMessage("Role et statut mis à jour avec succès !", false);

                loadUsers();
            }

            PauseTransition redirect = new PauseTransition(Duration.millis(800));
            redirect.setOnFinished(e -> {
                closeDetail();
                editingOwnProfile = false;
            });
            redirect.play();

        } catch (SQLException ex) {
            showDetailMessage("Erreur: " + ex.getMessage(), true);
        }
    }

    private void showDetailMessage(String msg, boolean isError) {
        detailMessage.setText(msg); detailMessage.setVisible(true); detailMessage.setManaged(true);
        detailMessage.getStyleClass().removeAll("detail-msg-error", "detail-msg-success");
        detailMessage.getStyleClass().add(isError ? "detail-msg-error" : "detail-msg-success");
    }

    // =================== BAN / DELETE ===================

    private void toggleBan(User user) {
        Status newStatus = (user.getStatus() == Status.Banned) ? Status.Unbanned : Status.Banned;
        String action = (newStatus == Status.Banned) ? "bannir" : "debannir";
        Alert c = new Alert(Alert.AlertType.CONFIRMATION);
        c.setTitle("Confirmation");
        c.setHeaderText("Voulez-vous " + action + " " + user.getNom() + " " + user.getPrenom() + " ?");
        c.setContentText("Email: " + user.getE_mail());
        Optional<ButtonType> r = c.showAndWait();
        if (r.isPresent() && r.get() == ButtonType.OK) {
            try { userCRUD.updateUserStatus(user.getId(), newStatus); loadUsers(); }
            catch (SQLException ex) { showAlert("Erreur", ex.getMessage()); }
        }
    }

    // =================== THEME ===================

    private void setDarkMode(boolean dark) {
        if (dark) {
            if (!rootPane.getStyleClass().contains("dark"))
                rootPane.getStyleClass().add("dark");
        } else {
            rootPane.getStyleClass().remove("dark");
        }
        // Mettre a jour l'icone et le label dans le bouton theme
        if (themeIcon != null)  themeIcon.setText(dark ? "\u2600" : "\u263D");
        if (themeLabel != null) themeLabel.setText(dark ? "Mode Jour" : "Mode Nuit");
    }

    // =================== NAVIGATION ===================

    private void setupNavButton(Button btn, String title, String subtitle) {
        btn.setOnAction(e -> {
            for (Button b : new Button[]{navUsers, navProducts, navOrders, navStats, navSettings, navSupport})
                b.getStyleClass().remove("nav-btn-active");
            if (!btn.getStyleClass().contains("nav-btn-active")) btn.getStyleClass().add("nav-btn-active");
            headerTitle.setText(title);
            headerSubtitle.setText(subtitle);
            bounceNode(btn);
        });
    }

    private void navigateToLogin() {
        Alert c = new Alert(Alert.AlertType.CONFIRMATION);
        c.setTitle("Deconnexion"); c.setHeaderText("Voulez-vous vous deconnecter ?");
        Optional<ButtonType> r = c.showAndWait();
        if (r.isPresent() && r.get() == ButtonType.OK) {
            // ===== VIDER LA SESSION =====
            org.example.Utils.SessionManager.getInstance().logout();
            redirectToLoginImmediately();
        }
    }

    /** Rediriger vers Login sans confirmation (session invalide ou deconnexion forcee) */
    private void redirectToLoginImmediately() {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                    getClass().getResource("/user/login/login_view.fxml"));
            javafx.scene.Parent root = loader.load();
            javafx.scene.Scene scene = rootPane.getScene();
            scene.getStylesheets().clear();
            java.net.URL css = getClass().getResource("/user/login/login.css");
            if (css != null) scene.getStylesheets().add(css.toExternalForm());
            scene.setRoot(root);
        } catch (Exception ex) {
            showAlert("Erreur", "Impossible de charger la page de connexion.");
            ex.printStackTrace();
        }
    }

    private void showAlert(String title, String content) {
        Alert a = new Alert(Alert.AlertType.ERROR); a.setTitle(title); a.setContentText(content); a.showAndWait();
    }

    // =================== ANIMATIONS ===================

    private void playEntrance() {
        sidebar.setTranslateX(-260); sidebar.setOpacity(0);
        TranslateTransition ts = new TranslateTransition(Duration.millis(500), sidebar); ts.setToX(0);
        FadeTransition fs = new FadeTransition(Duration.millis(500), sidebar); fs.setToValue(1);
        fs.setInterpolator(Interpolator.EASE_OUT);
        new ParallelTransition(ts, fs).play();

        headerBar.setOpacity(0);
        FadeTransition fh = new FadeTransition(Duration.millis(600), headerBar);
        fh.setFromValue(0); fh.setToValue(1); fh.setDelay(Duration.millis(200)); fh.play();

        usersTable.setOpacity(0);
        FadeTransition ft = new FadeTransition(Duration.millis(600), usersTable);
        ft.setFromValue(0); ft.setToValue(1); ft.setDelay(Duration.millis(400)); ft.play();
    }

    private void bounceNode(Node n) {
        if (n == null) return;
        ScaleTransition sc = new ScaleTransition(Duration.millis(130), n);
        sc.setFromX(0.88); sc.setFromY(0.88); sc.setToX(1.0); sc.setToY(1.0);
        sc.setInterpolator(Interpolator.EASE_OUT); sc.play();
    }
    private void setupRoleNavigation(Role role) {
        resetNavVisibility();

        if (role == Role.admin) {
            setNav(navUsers, "Dashboard global", "Vue globale plateforme");
            setNav(navProducts, "Gestion acheteurs", "Gestion des comptes acheteurs");
            setNav(navOrders, "Gestion vendeurs", "Gestion des comptes vendeurs");
            setNav(navStats, "Gestion sociétés de livraison", "Gestion livraison");
            setNav(navSettings, "Gestion commandes", "Supervision commandes");
            setNav(navSupport, "Historique IA et interactions", "Audit IA");
        }

        else if (role == Role.acheteur) {
            setNav(navUsers, "Dashboard", "Accueil acheteur");
            setNav(navProducts, "Catalogue des produits", "Explorer les produits");
            setNav(navOrders, "AI Achats", "Assistant intelligent");
            setNav(navStats, "Wishlist", "Produits favoris");
            setNav(navSettings, "Mes commandes", "Suivi commandes");
            navSupport.setVisible(false);
            navSupport.setManaged(false);
        }

        else if (role == Role.vendeur) {
            setNav(navUsers, "Dashboard", "Accueil vendeur");
            setNav(navProducts, "My Shop", "Gestion boutique");
            setNav(navOrders, "Les commandes", "Commandes reçues");
            setNav(navStats, "AI Marketing", "Recommandations marketing");
            setNav(navSettings, "Mes campagnes", "Campagnes commerciales");
            setNav(navSupport, "Mes fournisseurs", "Gestion fournisseurs");
        }

        else if (role == Role.livreur) {
            setNav(navUsers, "Dashboard", "Accueil livreur");
            setNav(navProducts, "Mes livreurs", "Gestion livreurs");
            setNav(navOrders, "Livraisons", "Suivi livraisons");

            navStats.setVisible(false);
            navStats.setManaged(false);
            navSettings.setVisible(false);
            navSettings.setManaged(false);
            navSupport.setVisible(false);
            navSupport.setManaged(false);
        }

        setupNavButton(navUsers, getNavTitle(navUsers), "Module prêt pour intégration");
        setupNavButton(navProducts, getNavTitle(navProducts), "Module prêt pour intégration");
        setupNavButton(navOrders, getNavTitle(navOrders), "Module prêt pour intégration");
        setupNavButton(navStats, getNavTitle(navStats), "Module prêt pour intégration");
        setupNavButton(navSettings, getNavTitle(navSettings), "Module prêt pour intégration");
        setupNavButton(navSupport, getNavTitle(navSupport), "Module prêt pour intégration");
    }
    private void resetNavVisibility() {
        for (Button b : new Button[]{navUsers, navProducts, navOrders, navStats, navSettings, navSupport}) {
            b.setVisible(true);
            b.setManaged(true);
        }
    }

    private String getNavTitle(Button btn) {
        if (btn.getGraphic() instanceof HBox hbox) {
            for (Node node : hbox.getChildren()) {
                if (node instanceof Label label && label.getStyleClass().contains("nav-label")) {
                    return label.getText();
                }
            }
        }
        return btn.getText();
    }

    private void setNav(Button btn, String title, String tooltip) {
        btn.setTooltip(new Tooltip(tooltip));

        if (btn.getGraphic() instanceof HBox hbox) {
            for (Node node : hbox.getChildren()) {
                if (node instanceof Label label && label.getStyleClass().contains("nav-label")) {
                    label.setText(title);
                }
            }
        } else {
            btn.setText(title);
        }
    }
    private void configureContentByRole(User user) {
        if (user.getRole() == Role.admin) {
            adminUsersContent.setVisible(true);
            adminUsersContent.setManaged(true);
            loadUsers();
            return;
        }

        adminUsersContent.getChildren().clear();

        VBox card = new VBox(12);
        card.getStyleClass().add("stat-card");
        card.setMaxWidth(720);

        Label title = new Label("Bienvenue " + user.getPrenom());
        title.setStyle("-fx-text-fill: #1E1B4B; -fx-font-size: 28px; -fx-font-weight: 900;");

        Label role = new Label("Espace " + user.getRole().name());
        role.setStyle("-fx-background-color: rgba(99,102,241,0.12); -fx-text-fill: #4F46E5; -fx-background-radius: 999; -fx-padding: 6 14; -fx-font-weight: 900;");

        Label desc = new Label("Choisissez une fonctionnalité depuis le menu hamburger. Cette zone est prête pour intégrer les modules de l'équipe.");
        desc.setWrapText(true);
        desc.setStyle("-fx-text-fill: #6B7280; -fx-font-size: 14px;");

        card.getChildren().addAll(role, title, desc);
        adminUsersContent.getChildren().add(card);
    }
    private void toggleSidebar() {
        sidebarOpen = !sidebarOpen;

        if (sidebarOpen) {
            sidebar.setVisible(true);
            sidebar.setManaged(true);

            TranslateTransition slide = new TranslateTransition(Duration.millis(260), sidebar);
            slide.setFromX(-260);
            slide.setToX(0);
            slide.setInterpolator(Interpolator.EASE_OUT);
            slide.play();

        } else {
            TranslateTransition slide = new TranslateTransition(Duration.millis(220), sidebar);
            slide.setFromX(0);
            slide.setToX(-260);
            slide.setInterpolator(Interpolator.EASE_IN);

            slide.setOnFinished(e -> {
                sidebar.setVisible(false);
                sidebar.setManaged(false);
            });

            slide.play();
        }
    }
    private void openProfileEdit(User user) {
        editingOwnProfile = true;

        openDetail(user, true);

        detailTitle.setText("Modifier mon profil");

        detailNom.setDisable(false);
        detailPrenom.setDisable(false);
        detailEmail.setDisable(false);
        detailTel.setDisable(false);
        detailDate.setDisable(false);

        detailNom.setEditable(true);
        detailPrenom.setEditable(true);
        detailEmail.setEditable(true);
        detailTel.setEditable(true);
        detailDate.setEditable(true);

        detailRole.setDisable(true);
        detailStatus.setDisable(true);
    }
    private void updateHeaderUser(User user) {
        if (user == null) return;

        adminNameLabel.setText(user.getNom() + " " + user.getPrenom());
        adminRoleLabel.setText(user.getRole() != null ? user.getRole().name() : "");

        if (user.getImage() != null && !user.getImage().isBlank()) {
            try {
                adminAvatar.setImage(new Image("file:" + user.getImage(), 38, 38, false, true));
            } catch (Exception ignored) {}
        }
    }
}