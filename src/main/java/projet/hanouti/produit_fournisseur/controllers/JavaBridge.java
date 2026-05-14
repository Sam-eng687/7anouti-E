package projet.hanouti.produit_fournisseur.controllers;

import javafx.application.Platform;
import javafx.scene.control.Button;
import javafx.scene.control.Label;

public class JavaBridge {

    private final Label addrLabel;
    private final Button confirmBtn;
    private final AddressCallback callback;

    public interface AddressCallback {
        void onAddressReady(String address);
    }

    public JavaBridge(Label addrLabel, Button confirmBtn, AddressCallback callback) {
        this.addrLabel = addrLabel;
        this.confirmBtn = confirmBtn;
        this.callback = callback;
    }

    public void onAddressFound(String address) {
        Platform.runLater(() -> {
            addrLabel.setStyle("-fx-text-fill:#222;-fx-font-size:12px;");
            addrLabel.setText(address);
            callback.onAddressReady(address);
        });
    }
}
