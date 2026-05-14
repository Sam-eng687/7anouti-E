package tn.hanouti.livreur.util;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import tn.hanouti.livreur.controller.DashboardLivreurController;
import tn.hanouti.livreur.controller.TrackingCarteController;
import tn.hanouti.livreur.dao.CommandeActiveDAO;
import tn.hanouti.livreur.dao.LivreurDAO;
import tn.hanouti.livreur.model.CommandeActive;
import tn.hanouti.livreur.model.Livreur;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

/**
 * Small host-application adapter for embedding the delivery module without
 * changing the existing authentication flow. Pass the livreur id obtained from
 * the shared SessionManager, then place the returned Parent in the host
 * dashboard/container.
 */
public final class LivraisonModuleRouter {

    private LivraisonModuleRouter() {
    }

    public static Parent loadDashboardForLivreurSession(int idFromSession)
            throws SQLException, IOException {
        Livreur l = new LivreurDAO().getById(idFromSession);
        if (l == null) {
            throw new SQLException("Livreur introuvable pour id=" + idFromSession);
        }

        String fxml = l.isResponsable()
                ? "/fxml/livreur/DashboardResponsable.fxml"
                : "/fxml/livreur/DashboardLivreur.fxml";

        FXMLLoader loader = new FXMLLoader(LivraisonModuleRouter.class.getResource(fxml));
        Parent root = loader.load();

        if (!l.isResponsable()) {
            DashboardLivreurController ctrl = loader.getController();
            ctrl.setLivreur(l.getIdLivreur(), l.getNomLivreur());
        }

        return root;
    }

    public static Parent loadTrackingForFirstActiveCommand()
            throws SQLException, IOException {
        FXMLLoader loader = new FXMLLoader(
                LivraisonModuleRouter.class.getResource("/fxml/livreur/TrackingCarte.fxml"));
        Parent root = loader.load();

        List<CommandeActive> actives = new CommandeActiveDAO().getCommandesActives();
        if (!actives.isEmpty()) {
            TrackingCarteController ctrl = loader.getController();
            ctrl.chargerCommande(actives.get(0));
        }

        return root;
    }
}
