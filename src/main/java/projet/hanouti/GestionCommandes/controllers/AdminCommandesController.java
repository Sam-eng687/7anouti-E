package projet.hanouti.GestionCommandes.controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import projet.hanouti.GestionCommandes.entities.Commande;
import projet.hanouti.GestionCommandes.entities.LigneCommande;
import projet.hanouti.GestionCommandes.enums.StatutCommande;
import projet.hanouti.common.utils.MyBD;
import projet.hanouti.GestionCommandes.services.CommandeService;
import projet.hanouti.GestionCommandes.services.LigneCommandeService;

import java.io.FileWriter;
import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

/**
 * Controller — Vue admin "Gestion des commandes"
 * Vue globale : toutes les commandes de la plateforme.
 */
public class AdminCommandesController implements Initializable {

    @FXML private Label kpiTotal, kpiAttente, kpiLivrees, kpiCA, lblIA, lblCount;
    @FXML private TextField  searchField;
    @FXML private ComboBox<String> filterStatut, filterVendeur, filterPaiement;
    @FXML private DatePicker filterDate;
    @FXML private ListView<Commande> listCommandes;

    // Detail
    @FXML private VBox   emptyState, motifBox;
    @FXML private ScrollPane detailScroll;
    @FXML private Label  detNumero, detDate, detStatut, detVip;
    @FXML private Label  detAcheteur, detVendeur, detTotal, detScore, detSociete, detMotif;
    @FXML private ListView<LigneCommande> detListLignes;

    private final CommandeService      cmdService   = new CommandeService();
    private final LigneCommandeService    ligneService   = new LigneCommandeService();
    private final projet.hanouti.GestionCommandes.services.SocieteLivraisonService societeService = new projet.hanouti.GestionCommandes.services.SocieteLivraisonService();
    private ObservableList<Commande> allCommandes   = FXCollections.observableArrayList();
    private Commande selectedCommande = null;
    private static final DateTimeFormatter DT_FMT   = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        filterStatut.setItems(FXCollections.observableArrayList(
                "Tous","CREEE","CONFIRMEE","EN_PREPARATION","EXPEDIEE","LIVREE","ANNULEE","REFUSEE"));
        filterPaiement.setItems(FXCollections.observableArrayList("Tous","CARTE","ESPECES"));
        listCommandes.setCellFactory(lv -> new CommandeCell());
        detListLignes.setCellFactory(lv -> new LigneCell());
        loadCommandes();
    }

    private void loadCommandes() {
        List<Commande> list = cmdService.getAllForAdmin();
        allCommandes = FXCollections.observableArrayList(list);
        listCommandes.setItems(allCommandes);

        kpiTotal.setText(String.valueOf(list.size()));
        kpiAttente.setText(String.valueOf(list.stream().filter(c ->
                c.getStatut()==StatutCommande.CREEE||c.getStatut()==StatutCommande.CONFIRMEE
                        ||c.getStatut()==StatutCommande.EN_PREPARATION).count()));
        kpiLivrees.setText(String.valueOf(list.stream().filter(c->c.getStatut()==StatutCommande.LIVREE).count()));
        double ca = list.stream().filter(c->c.getStatut()==StatutCommande.LIVREE)
                .mapToDouble(Commande::getTotal).sum();
        kpiCA.setText(String.format("%.0f TND", ca));
        lblCount.setText(list.size()+" commande(s)");

        // IA strip
        long prio = list.stream().filter(c->c.getScorePriorite()>=40).count();
        lblIA.setText("🤖 IA : "+list.size()+" commandes analysées — "+prio+" haute priorité. "
                +"CA livré : "+String.format("%.2f", ca)+" TND.");

        // Populate vendeur filter
        list.stream().map(c->String.valueOf(c.getIdVendeur())).distinct()
                .forEach(v -> { if(!filterVendeur.getItems().contains(v)) filterVendeur.getItems().add(v); });
        if (!filterVendeur.getItems().contains("Tous")) filterVendeur.getItems().add(0,"Tous");
    }

    @FXML
    private void onCommandeClicked() {
        Commande c = listCommandes.getSelectionModel().getSelectedItem();
        if (c==null) return;
        selectedCommande=c; showDetail(c);
    }

    private void showDetail(Commande c) {
        emptyState.setVisible(false); emptyState.setManaged(false);
        detailScroll.setVisible(true); detailScroll.setManaged(true);

        detNumero.setText(c.getNumeroCommande());
        detDate.setText(c.getDateCreation()!=null ? c.getDateCreation().format(DT_FMT) : "—");
        applyBadge(detStatut, c.getStatut());
        boolean vip=cmdService.isAcheteurVIP(c.getIdAcheteur());
        detVip.setVisible(vip); detVip.setManaged(vip);

        detAcheteur.setText("Acheteur #"+c.getIdAcheteur());
        detVendeur.setText("Vendeur #"+c.getIdVendeur());
        detTotal.setText(String.format("%.2f TND", c.getTotal()));
        detScore.setText("⚡ "+c.getScorePriorite()+" pts");
        if (c.getIdSocieteLivraison() != null) {
            var soc = societeService.getById(c.getIdSocieteLivraison());
            detSociete.setText(soc != null ? "🚚 " + soc.getNomSociete() : "Non assignée");
        } else { detSociete.setText("Non assignée"); }

        List<LigneCommande> lignes=ligneService.getByCommande(c.getIdCommande());
        enrichWithProductNames(lignes);
        detListLignes.setItems(FXCollections.observableArrayList(lignes));

        boolean ref=c.getStatut()==StatutCommande.REFUSEE;
        motifBox.setVisible(ref); motifBox.setManaged(ref);
        if(ref&&c.getMotifRefus()!=null) detMotif.setText(c.getMotifRefus());
    }

    private void enrichWithProductNames(java.util.List<LigneCommande> lignes) {
        try {
            java.sql.Connection cnx = MyBD.getInstance().getConnection();
            for (LigneCommande l : lignes) {
                try (java.sql.PreparedStatement ps = cnx.prepareStatement(
                        "SELECT nom FROM produit WHERE id_produit = ?")) {
                    ps.setInt(1, l.getIdProduit());
                    java.sql.ResultSet rs = ps.executeQuery();
                    if (rs.next()) l.setNomProduit(rs.getString("nom"));
                }
            }
        } catch (Exception e) {
            System.err.println("[AdminCommandesController.enrichWithProductNames] " + e.getMessage());
        }
    }

    @FXML
    private void onVoirDetails() {
        if (selectedCommande==null) return;
        try {
            FXMLLoader loader=new FXMLLoader(getClass().getResource("/FXML/GestionCommandes/PopupDetailCommande.fxml"));
            Parent popup=loader.load();
            PopupDetailCommandeController ctrl=loader.getController();
            ctrl.init(selectedCommande,"VENDEUR",this::loadCommandes); // admin uses vendeur actions
            Stage st=new Stage(StageStyle.TRANSPARENT);
            Scene sc=new Scene(popup); sc.setFill(null);
            if (listCommandes.getScene() != null) {
                sc.getStylesheets().addAll(listCommandes.getScene().getStylesheets());
                if (listCommandes.getScene().getRoot().getStyleClass().contains("light-mode")
                        && !popup.getStyleClass().contains("light-mode")) {
                    popup.getStyleClass().add("light-mode");
                }
            }
            st.setScene(sc); st.initModality(Modality.APPLICATION_MODAL); st.showAndWait();
        } catch(Exception e){System.err.println("[AdminCommandesController] "+e.getMessage());}
    }

    @FXML private void onSearch()  { applyFilters(); }
    @FXML private void onFilter()  { applyFilters(); }

    @FXML
    private void onReset() {
        searchField.clear(); filterStatut.setValue(null); filterVendeur.setValue(null);
        filterPaiement.setValue(null); filterDate.setValue(null);
        listCommandes.setItems(allCommandes); lblCount.setText(allCommandes.size()+" commande(s)");
    }

    private void applyFilters() {
        String q=searchField.getText().toLowerCase().trim();
        String st=filterStatut.getValue(), vd=filterVendeur.getValue(), pm=filterPaiement.getValue();
        var date=filterDate.getValue();
        var f=allCommandes.stream().filter(c ->
                (q.isEmpty()||c.getNumeroCommande().toLowerCase().contains(q)||c.getAdresseLivraison().toLowerCase().contains(q))
                        &&(st==null||st.equals("Tous")||c.getStatut().name().equals(st))
                        &&(vd==null||vd.equals("Tous")||String.valueOf(c.getIdVendeur()).equals(vd))
                        &&(pm==null||pm.equals("Tous")||c.getModePaiement().name().equals(pm))
                        &&(date==null||(c.getDateCreation()!=null&&c.getDateCreation().toLocalDate().equals(date)))
        ).collect(Collectors.toList());
        listCommandes.setItems(FXCollections.observableArrayList(f));
        lblCount.setText(f.size()+" commande(s)");
    }

    @FXML
    private void onExportCSV() {
        List<Commande> toExport = new java.util.ArrayList<>(listCommandes.getItems());
        try (FileWriter fw = new FileWriter("commandes_export.csv")) {
            fw.write("ID,Numero,Acheteur,Vendeur,Total,Statut,Mode,Date\n");
            for (Commande c : toExport) {
                fw.write(String.join(",",
                        String.valueOf(c.getIdCommande()), c.getNumeroCommande(),
                        String.valueOf(c.getIdAcheteur()), String.valueOf(c.getIdVendeur()),
                        String.format("%.2f", c.getTotal()), c.getStatut().name(),
                        c.getModePaiement().name(),
                        c.getDateCreation()!=null?c.getDateCreation().format(DT_FMT):""
                ) + "\n");
            }
            new Alert(Alert.AlertType.INFORMATION,"Export réussi : commandes_export.csv").showAndWait();
        } catch(Exception e){
            new Alert(Alert.AlertType.ERROR,"Erreur export : "+e.getMessage()).showAndWait();
        }
    }

    private void applyBadge(Label l, StatutCommande s){
        l.getStyleClass().clear(); l.getStyleClass().addAll("statut-badge","badge-"+s.name());
        l.setText(switch(s){case CREEE->"🆕 Créée";case CONFIRMEE->"✅ Confirmée";
            case EN_PREPARATION->"🔧 Préparation";case EXPEDIEE->"🚀 Expédiée";
            case LIVREE->"📬 Livrée";case ANNULEE->"🚫 Annulée";case REFUSEE->"❌ Refusée";});
    }

    private class CommandeCell extends ListCell<Commande> {
        private final VBox  card=new VBox(7);
        private final HBox  r1=new HBox(8),r2=new HBox(8);
        private final Label lNum=new Label(),lStat=new Label(),lDate=new Label(),
                lVd=new Label(),lTotal=new Label();
        CommandeCell(){
            Region s1=new Region(),s2=new Region();
            HBox.setHgrow(s1,Priority.ALWAYS); HBox.setHgrow(s2,Priority.ALWAYS);
            r1.getChildren().addAll(lNum,s1,lStat);
            r2.getChildren().addAll(lVd,s2,lTotal);
            card.getChildren().addAll(r1,lDate,r2);
            VBox.setMargin(card,new Insets(3,0,3,0));
            lNum.getStyleClass().add("cmd-numero"); lStat.getStyleClass().add("statut-badge");
            lDate.getStyleClass().add("cmd-date"); lVd.getStyleClass().add("cmd-adresse");
            lTotal.getStyleClass().add("cmd-total");
        }
        @Override protected void updateItem(Commande c,boolean empty){
            super.updateItem(c,empty); if(empty||c==null){setGraphic(null);return;}
            lNum.setText(c.getNumeroCommande());
            lDate.setText(c.getDateCreation()!=null?"📅 "+c.getDateCreation().format(DT_FMT):"");
            lVd.setText("🏪 Vendeur #"+c.getIdVendeur()+" · 👤 Acheteur #"+c.getIdAcheteur());
            lTotal.setText(String.format("%.2f TND",c.getTotal()));
            lStat.getStyleClass().removeIf(s->s.startsWith("badge-"));
            lStat.getStyleClass().add("badge-"+c.getStatut().name());
            lStat.setText(switch(c.getStatut()){case CREEE->"🆕";case CONFIRMEE->"✅";
                case EN_PREPARATION->"🔧";case EXPEDIEE->"🚀";
                case LIVREE->"📬";case ANNULEE->"🚫";case REFUSEE->"❌";});
            card.getStyleClass().clear(); card.getStyleClass().addAll("cmd-card","statut-"+c.getStatut().name());
            if(isSelected()) card.getStyleClass().add("cmd-card-selected");
            setGraphic(card);
        }
    }

    private static class LigneCell extends ListCell<LigneCommande> {
        private final HBox row=new HBox(10);
        private final Label lP=new Label(),lQ=new Label(),lPx=new Label();
        LigneCell(){
            Region sp=new Region(); HBox.setHgrow(sp,Priority.ALWAYS);
            row.getChildren().addAll(lP,lQ,sp,lPx); row.getStyleClass().add("ligne-item");
            lP.getStyleClass().add("ligne-produit"); lQ.getStyleClass().add("ligne-qte");
            lPx.getStyleClass().add("ligne-prix");
        }
        @Override protected void updateItem(LigneCommande l,boolean empty){
            super.updateItem(l,empty); if(empty||l==null){setGraphic(null);return;}
            String n=(l.getNomProduit()!=null&&!l.getNomProduit().isBlank())?l.getNomProduit():"Produit inconnu";
            lP.setText(n); lQ.setText("×"+l.getQuantite());
            lPx.setText(String.format("%.2f TND",l.getSousTotal())); setGraphic(row);
        }
    }
}
