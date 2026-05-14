package tn.hanouti.livreur.dao;

import tn.hanouti.livreur.model.CommandeActive;
import tn.hanouti.util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO for the customer-facing active orders tracking view.
 * Joins Suivi_Livraison with Livreur to provide all data needed
 * for the order list and the live map screen.
 */
public class CommandeActiveDAO {

    private Connection cnx = DBConnection.getInstance().getCnx();

    // ─────────────────────────────────────────────
    // GET ALL ACTIVE ORDERS (AFFECTEE status)
    // Used to populate the customer's active orders list.
    // ─────────────────────────────────────────────
    public List<CommandeActive> getCommandesActives() throws SQLException {
        List<CommandeActive> liste = new ArrayList<>();
        String sql = "SELECT sl.id_suivi, sl.id_commande, sl.adresse_client, " +
                "sl.heure_estimee, sl.statut, sl.localisation_actuelle, " +
                "sl.id_livreur, " +
                "COALESCE(l.nom_livreur, 'Non assigné') AS nom_livreur, " +
                "COALESCE(l.telephone, '—') AS telephone, " +
                "COALESCE(l.genre_vehicule, 'Voiture') AS genre_vehicule " +
                "FROM Suivi_Livraison sl " +
                "LEFT JOIN Livreur l ON sl.id_livreur = l.id_livreur " +
                "WHERE sl.statut = 'AFFECTEE' " +
                "ORDER BY sl.id_suivi DESC";
        ResultSet rs = cnx.createStatement().executeQuery(sql);
        while (rs.next()) liste.add(mapRow(rs));
        return liste;
    }

    // ─────────────────────────────────────────────
    // GET ONE — for refreshing the tracking screen
    // ─────────────────────────────────────────────
    public CommandeActive getById(int idSuivi) throws SQLException {
        String sql = "SELECT sl.id_suivi, sl.id_commande, sl.adresse_client, " +
                "sl.heure_estimee, sl.statut, sl.localisation_actuelle, " +
                "sl.id_livreur, " +
                "COALESCE(l.nom_livreur, 'Non assigné') AS nom_livreur, " +
                "COALESCE(l.telephone, '—') AS telephone, " +
                "COALESCE(l.genre_vehicule, 'Voiture') AS genre_vehicule " +
                "FROM Suivi_Livraison sl " +
                "LEFT JOIN Livreur l ON sl.id_livreur = l.id_livreur " +
                "WHERE sl.id_suivi = ?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, idSuivi);
        ResultSet rs = ps.executeQuery();
        if (rs.next()) return mapRow(rs);
        return null;
    }

    // ─────────────────────────────────────────────
    // GET LATEST DRIVER LOCATION — polled every few seconds
    // Returns "lat,lon" string or null if not set.
    // ─────────────────────────────────────────────
    public String getLocalisationActuelle(int idSuivi) throws SQLException {
        String sql = "SELECT localisation_actuelle FROM Suivi_Livraison WHERE id_suivi = ?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, idSuivi);
        ResultSet rs = ps.executeQuery();
        if (rs.next()) return rs.getString("localisation_actuelle");
        return null;
    }

    // ─────────────────────────────────────────────
    // GET LATEST ETA — polled alongside location
    // ─────────────────────────────────────────────
    public String getHeureEstimee(int idSuivi) throws SQLException {
        String sql = "SELECT heure_estimee FROM Suivi_Livraison WHERE id_suivi = ?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, idSuivi);
        ResultSet rs = ps.executeQuery();
        if (rs.next()) return rs.getString("heure_estimee");
        return null;
    }

    // ─────────────────────────────────────────────
    // MAPPER
    // ─────────────────────────────────────────────
    private CommandeActive mapRow(ResultSet rs) throws SQLException {
        return new CommandeActive(
                rs.getInt("id_suivi"),
                rs.getInt("id_commande"),
                rs.getString("adresse_client"),
                rs.getString("heure_estimee"),
                rs.getString("statut"),
                rs.getInt("id_livreur"),
                rs.getString("nom_livreur"),
                rs.getString("telephone"),
                rs.getString("genre_vehicule"),
                rs.getString("localisation_actuelle")
        );
    }
}

