package edu.hanouti.gui;

import edu.hanouti.entities.ConseilsMarketing;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

import java.sql.Date;

public class ConseilDialogController {
    @FXML private Label dialogTitle;
    @FXML private TextField txtProduit;
    @FXML private ComboBox<String> cmbType;
    @FXML private TextArea txtDesc;
    @FXML private CheckBox chkApplique;

    private ConseilsMarketing conseil;

    public void initialize() {
        cmbType.getItems().addAll("Réassort", "Promotion", "Packaging", "Fidélisation");
    }

    public void setConseil(ConseilsMarketing conseil) {
        this.conseil = conseil;
        if (conseil != null) {
            dialogTitle.setText("Modifier le Conseil");
            txtProduit.setText(conseil.getProduitId());
            cmbType.setValue(conseil.getTypeConseil());
            txtDesc.setText(conseil.getDescription());
            chkApplique.setSelected(conseil.isApplique());
        }
    }

    public ConseilsMarketing getConseil() {
        if (conseil == null) {
            conseil = new ConseilsMarketing();
            conseil.setDateGeneration(new Date(System.currentTimeMillis()));
            conseil.setImpactEstime("Moyen"); // Default
        }
        conseil.setProduitId(txtProduit.getText());
        conseil.setTypeConseil(cmbType.getValue());
        conseil.setDescription(txtDesc.getText());
        conseil.setApplique(chkApplique.isSelected());
        return conseil;
    }
}
