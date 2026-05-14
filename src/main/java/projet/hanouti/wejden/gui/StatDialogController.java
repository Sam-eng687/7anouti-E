package projet.hanouti.wejden.gui;

import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import projet.hanouti.wejden.entities.StatistiquesVentes;

public class StatDialogController {
    @FXML private Label dialogTitle;
    @FXML private TextField txtProduit;
    @FXML private TextField txtPeriode;
    @FXML private TextField txtVentes;
    @FXML private TextField txtRevenu;
    @FXML private ComboBox<String> cmbClassement;

    private StatistiquesVentes stat;

    public void initialize() {
        cmbClassement.getItems().addAll("Top 10", "Top 50", "Stable", "Baisse");
    }

    public void setStat(StatistiquesVentes stat) {
        this.stat = stat;
        if (stat != null) {
            dialogTitle.setText("Modifier la Statistique");
            txtProduit.setText(stat.getProduitId());
            txtPeriode.setText(stat.getPeriode());
            txtVentes.setText(String.valueOf(stat.getTotalVendu()));
            txtRevenu.setText(String.valueOf(stat.getRevenuTotal()));
            cmbClassement.setValue(stat.getClassement());
        }
    }

    public StatistiquesVentes getStat() {
        if (stat == null) stat = new StatistiquesVentes();
        stat.setProduitId(txtProduit.getText());
        stat.setPeriode(txtPeriode.getText());
        stat.setTotalVendu(Integer.parseInt(txtVentes.getText()));
        stat.setRevenuTotal(Double.parseDouble(txtRevenu.getText()));
        stat.setClassement(cmbClassement.getValue());
        return stat;
    }
}
