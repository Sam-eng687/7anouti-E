package projet.hanouti.user_auth.controllers.back;

import javafx.animation.*;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.scene.text.Text;
import javafx.util.Duration;
import projet.hanouti.common.utils.SessionManager;
import projet.hanouti.user_auth.entities.User;
import projet.hanouti.user_auth.enums.Role;
import projet.hanouti.user_auth.enums.Status;
import projet.hanouti.user_auth.services.UserCRUD;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public class AdminUsersController {

    @FXML private Label statTotal, statActive, statBanned, statRoles;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> filterRole, filterStatus;
    @FXML private Button refreshBtn;
    @FXML private TableView<User> usersTable;
    @FXML private TableColumn<User, String> colId, colNom, colPrenom, colEmail, colRole, colStatus, colDate, colActions;

    @FXML private StackPane overlayPane;
    @FXML private VBox detailCard;
    @FXML private Button closeDetailBtn, detailSaveBtn, detailCancelBtn;
    @FXML private Text detailTitle;
    @FXML private ImageView detailAvatar;
    @FXML private Label detailAvatarLetter, detailMessage;
    @FXML private TextField detailNom, detailPrenom, detailEmail, detailTel, detailDate;
    @FXML private ComboBox<String> detailRole, detailStatus;

    private final UserCRUD userCRUD = new UserCRUD();
    private final ObservableList<User> allUsers = FXCollections.observableArrayList();
    private FilteredList<User> filteredUsers;
    private User currentDetailUser;

    @FXML
    public void initialize() {
        setupFilters();
        setupTable();
        setupDetailOverlay();
        refreshBtn.setOnAction(e -> {
            loadUsers();
            bounceNode(refreshBtn);
        });
        loadUsers();
        playEntrance();
    }

    private void setupFilters() {
        filterRole.setItems(FXCollections.observableArrayList("Tous", "admin", "acheteur", "vendeur", "livreur", "fournisseur"));
        filterRole.setValue("Tous");
        filterStatus.setItems(FXCollections.observableArrayList("Tous", "Unbanned", "Banned"));
        filterStatus.setValue("Tous");
    }

    private void setupTable() {
        colId.setCellValueFactory(c -> new SimpleStringProperty(String.valueOf(c.getValue().getId())));
        colNom.setCellValueFactory(c -> new SimpleStringProperty(value(c.getValue().getNom())));
        colPrenom.setCellValueFactory(c -> new SimpleStringProperty(value(c.getValue().getPrenom())));
        colEmail.setCellValueFactory(c -> new SimpleStringProperty(value(c.getValue().getE_mail())));
        colRole.setCellValueFactory(c -> {
            Role role = c.getValue().getRole();
            return new SimpleStringProperty(role != null ? role.name() : "");
        });
        colStatus.setCellValueFactory(c -> {
            Status status = c.getValue().getStatus();
            return new SimpleStringProperty(status != null ? status.name() : "");
        });
        colDate.setCellValueFactory(c -> new SimpleStringProperty(value(c.getValue().getDate_naiss())));

        colStatus.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                    return;
                }
                Label badge = new Label(item);
                badge.getStyleClass().add("status-badge");
                badge.getStyleClass().add("Banned".equals(item) ? "status-banned" : "status-active");
                setGraphic(badge);
                setText(null);
            }
        });

        colRole.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                    return;
                }
                Label badge = new Label(item);
                badge.getStyleClass().add("role-badge");
                switch (item) {
                    case "admin" -> badge.getStyleClass().add("role-admin");
                    case "vendeur" -> badge.getStyleClass().add("role-vendeur");
                    case "acheteur" -> badge.getStyleClass().add("role-acheteur");
                    case "livreur" -> badge.getStyleClass().add("role-livreur");
                    case "fournisseur" -> badge.getStyleClass().add("role-fournisseur");
                }
                setGraphic(badge);
                setText(null);
            }
        });

        colActions.setCellFactory(col -> new TableCell<>() {
            private final Button viewBtn = makeIconBtn("\u25CF", "action-view", "Voir les details");
            private final Button editBtn = makeIconBtn("\u270E", "action-edit", "Modifier");
            private final Button banBtn = makeIconBtn("\u2715", "action-ban", "Bannir");
            private final HBox box = new HBox(8, viewBtn, editBtn, banBtn);

            {
                box.setAlignment(javafx.geometry.Pos.CENTER);
                viewBtn.setOnAction(e -> openDetail(getTableView().getItems().get(getIndex()), false));
                editBtn.setOnAction(e -> openDetail(getTableView().getItems().get(getIndex()), true));
                banBtn.setOnAction(e -> toggleBan(getTableView().getItems().get(getIndex())));
            }

            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                    return;
                }
                User user = getTableView().getItems().get(getIndex());
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

        filteredUsers = new FilteredList<>(allUsers, p -> true);
        usersTable.setItems(filteredUsers);
        searchField.textProperty().addListener((obs, old, val) -> applyFilters());
        filterRole.valueProperty().addListener((obs, old, val) -> applyFilters());
        filterStatus.valueProperty().addListener((obs, old, val) -> applyFilters());
    }

    private void setupDetailOverlay() {
        detailRole.setItems(FXCollections.observableArrayList("admin", "acheteur", "vendeur", "livreur", "fournisseur"));
        detailStatus.setItems(FXCollections.observableArrayList("Unbanned", "Banned"));
        closeDetailBtn.setOnAction(e -> closeDetail());
        detailCancelBtn.setOnAction(e -> closeDetail());
        detailSaveBtn.setOnAction(e -> saveDetail());
        overlayPane.setOnMouseClicked(e -> {
            if (e.getTarget() == overlayPane) {
                closeDetail();
            }
        });
    }

    private Button makeIconBtn(String icon, String styleClass, String tooltipText) {
        Button button = new Button(icon);
        button.getStyleClass().addAll("action-btn", styleClass);
        button.setTooltip(new Tooltip(tooltipText));
        return button;
    }

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
        int total = allUsers.size();
        long active = allUsers.stream().filter(u -> u.getStatus() == Status.Unbanned).count();
        long banned = allUsers.stream().filter(u -> u.getStatus() == Status.Banned).count();
        animateStatLabel(statTotal, total);
        animateStatLabel(statActive, (int) active);
        animateStatLabel(statBanned, (int) banned);
        statRoles.setText(String.valueOf(Role.values().length));
    }

    private void animateStatLabel(Label label, int target) {
        int current;
        try {
            current = Integer.parseInt(label.getText());
        } catch (NumberFormatException e) {
            current = 0;
        }
        if (current == target) {
            label.setText(String.valueOf(target));
            return;
        }

        final int start = current;
        Timeline timeline = new Timeline();
        int steps = 20;
        for (int i = 0; i <= steps; i++) {
            final int value = start + (int) ((target - start) * ((double) i / steps));
            timeline.getKeyFrames().add(new KeyFrame(Duration.millis(i * 25), e -> label.setText(String.valueOf(value))));
        }
        timeline.play();
    }

    private void applyFilters() {
        String search = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase();
        String roleFilter = filterRole.getValue();
        String statusFilter = filterStatus.getValue();

        filteredUsers.setPredicate(user -> {
            if (!search.isEmpty()) {
                boolean matches = (user.getNom() != null && user.getNom().toLowerCase().contains(search))
                        || (user.getPrenom() != null && user.getPrenom().toLowerCase().contains(search))
                        || (user.getE_mail() != null && user.getE_mail().toLowerCase().contains(search))
                        || (user.getRole() != null && user.getRole().name().toLowerCase().contains(search));
                if (!matches) {
                    return false;
                }
            }
            if (roleFilter != null && !"Tous".equals(roleFilter)
                    && (user.getRole() == null || !user.getRole().name().equals(roleFilter))) {
                return false;
            }
            return statusFilter == null || "Tous".equals(statusFilter)
                    || (user.getStatus() != null && user.getStatus().name().equals(statusFilter));
        });
    }

    private void openDetail(User user, boolean editable) {
        currentDetailUser = user;
        detailTitle.setText(editable ? "Modifier Utilisateur" : "Details Utilisateur");
        detailNom.setText(value(user.getNom()));
        detailPrenom.setText(value(user.getPrenom()));
        detailEmail.setText(value(user.getE_mail()));
        detailTel.setText(value(user.getNum_tel()));
        detailDate.setText(value(user.getDate_naiss()));
        detailRole.setValue(user.getRole() != null ? user.getRole().name() : "acheteur");
        detailStatus.setValue(user.getStatus() != null ? user.getStatus().name() : "Unbanned");
        detailAvatarLetter.setText(value(user.getNom()).isEmpty() ? "?" : user.getNom().substring(0, 1).toUpperCase());

        if (user.getImage() != null && !user.getImage().isBlank()) {
            try {
                detailAvatar.setClip(new Circle(38, 38, 38));
                detailAvatar.setImage(new Image("file:" + user.getImage(), 76, 76, false, true));
                detailAvatarLetter.setVisible(false);
            } catch (Exception e) {
                detailAvatar.setImage(null);
                detailAvatarLetter.setVisible(true);
            }
        } else {
            detailAvatar.setImage(null);
            detailAvatarLetter.setVisible(true);
        }

        detailNom.setDisable(true);
        detailPrenom.setDisable(true);
        detailEmail.setDisable(true);
        detailTel.setDisable(true);
        detailDate.setDisable(true);
        detailRole.setDisable(!editable);
        detailStatus.setDisable(!editable);
        detailSaveBtn.setVisible(editable);
        detailSaveBtn.setManaged(editable);
        detailMessage.setVisible(false);
        detailMessage.setManaged(false);

        overlayPane.setVisible(true);
        overlayPane.setManaged(true);
        overlayPane.setOpacity(0);
        detailCard.setScaleX(0.9);
        detailCard.setScaleY(0.9);

        FadeTransition fade = new FadeTransition(Duration.millis(200), overlayPane);
        fade.setToValue(1);
        ScaleTransition scale = new ScaleTransition(Duration.millis(250), detailCard);
        scale.setToX(1);
        scale.setToY(1);
        scale.setInterpolator(Interpolator.EASE_OUT);
        new ParallelTransition(fade, scale).play();
    }

    private void closeDetail() {
        FadeTransition fade = new FadeTransition(Duration.millis(150), overlayPane);
        fade.setToValue(0);
        ScaleTransition scale = new ScaleTransition(Duration.millis(150), detailCard);
        scale.setToX(0.9);
        scale.setToY(0.9);
        ParallelTransition transition = new ParallelTransition(fade, scale);
        transition.setOnFinished(e -> {
            overlayPane.setVisible(false);
            overlayPane.setManaged(false);
            detailCard.setScaleX(1);
            detailCard.setScaleY(1);
            currentDetailUser = null;
        });
        transition.play();
    }

    private void saveDetail() {
        if (currentDetailUser == null) {
            return;
        }

        try {
            currentDetailUser.setRole(Role.valueOf(detailRole.getValue()));
            currentDetailUser.setStatus(Status.valueOf(detailStatus.getValue()));
            userCRUD.updateUserAdminFields(currentDetailUser);
            showDetailMessage("Role et statut mis a jour avec succes !", false);
            loadUsers();

            PauseTransition closeDelay = new PauseTransition(Duration.millis(800));
            closeDelay.setOnFinished(e -> closeDetail());
            closeDelay.play();
        } catch (SQLException ex) {
            showDetailMessage("Erreur: " + ex.getMessage(), true);
        }
    }

    private void showDetailMessage(String msg, boolean isError) {
        detailMessage.setText(msg);
        detailMessage.setVisible(true);
        detailMessage.setManaged(true);
        detailMessage.getStyleClass().removeAll("detail-msg-error", "detail-msg-success");
        detailMessage.getStyleClass().add(isError ? "detail-msg-error" : "detail-msg-success");
    }

    private void toggleBan(User user) {
        Status newStatus = user.getStatus() == Status.Banned ? Status.Unbanned : Status.Banned;
        String action = newStatus == Status.Banned ? "bannir" : "debannir";
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmation");
        confirm.setHeaderText("Voulez-vous " + action + " " + value(user.getNom()) + " " + value(user.getPrenom()) + " ?");
        confirm.setContentText("Email: " + value(user.getE_mail()));
        styleDashboardDialog(confirm);

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                userCRUD.updateUserStatus(user.getId(), newStatus);
                loadUsers();
            } catch (SQLException ex) {
                showAlert("Erreur", ex.getMessage());
            }
        }
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setContentText(content);
        styleDashboardDialog(alert);
        alert.showAndWait();
    }

    private void styleDashboardDialog(Alert alert) {
        alert.setGraphic(null);
        DialogPane pane = alert.getDialogPane();
        if (!pane.getStyleClass().contains("dashboard-dialog")) {
            pane.getStyleClass().add("dashboard-dialog");
        }
        java.net.URL css = getClass().getResource("/styles/user_auth/back/dashboard.css");
        if (css != null && !pane.getStylesheets().contains(css.toExternalForm())) {
            pane.getStylesheets().add(css.toExternalForm());
        }
        if (SessionManager.getInstance().isDarkMode()) {
            pane.setStyle("-card: #14122E; -card-b: rgba(165,180,252,0.18); -inp: rgba(255,255,255,0.06); -inp-b: rgba(165,180,252,0.16); -t1: #F1F0FF; -t2: #A5B4FC; -b600: #6366F1; -b700: #4F46E5;");
        } else {
            pane.setStyle("-card: #FFFFFF; -card-b: rgba(99,102,241,0.16); -inp: #EEF2FF; -inp-b: rgba(99,102,241,0.16); -t1: #1E1B4B; -t2: #4F46E5; -b600: #4F46E5; -b700: #4338CA;");
        }
    }

    private void playEntrance() {
        usersTable.setOpacity(0);
        FadeTransition fade = new FadeTransition(Duration.millis(600), usersTable);
        fade.setFromValue(0);
        fade.setToValue(1);
        fade.setDelay(Duration.millis(180));
        fade.play();
    }

    private void bounceNode(javafx.scene.Node node) {
        if (node == null) {
            return;
        }
        ScaleTransition scale = new ScaleTransition(Duration.millis(130), node);
        scale.setFromX(0.88);
        scale.setFromY(0.88);
        scale.setToX(1.0);
        scale.setToY(1.0);
        scale.setInterpolator(Interpolator.EASE_OUT);
        scale.play();
    }

    private String value(String val) {
        return val == null ? "" : val;
    }
}
