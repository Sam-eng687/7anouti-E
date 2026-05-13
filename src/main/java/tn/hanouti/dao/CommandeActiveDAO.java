package tn.hanouti.dao;

import tn.hanouti.model.CommandeActive;
import tn.hanouti.util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO for the customer-facing active orders view.
 * Reads from the 'livraisons' table (team schema)
 * joined with Livreur for driver info.
 */
public class CommandeActiveDAO {

    private Connection cnx = DBConnection.getInstance().getCnx();

    // ─────────────────────────────────────────────
    // GET ALL ACTIVE ORDERS (EN_COURS)
    // ─────────────────────────────────────────────
    public List<CommandeActive> getCommandesActives() throws SQLException {
        List<CommandeActive> liste = new ArrayList<>();
        String sql = "SELECT l.id_livraison, l.id_commande, l.numero_commande, " +
                "l.adresse_client, l.date_livraison, l.statut_livraison, " +
                "l.localisation_actuelle, l.id_livreur, " +
                "COALESCE(lv.nom_livreur, 'Non assigné') AS nom_livreur, " +
                "COALESCE(lv.telephone, '—') AS telephone, " +
                "COALESCE(lv.genre_vehicule, 'Voiture') AS genre_vehicule " +
                "FROM livraisons l " +
                "LEFT JOIN Livreur lv ON l.id_livreur = lv.id_livreur " +
                "WHERE l.statut_livraison = 'EN_COURS' " +
                "ORDER BY l.id_livraison DESC";
        ResultSet rs = cnx.createStatement().executeQuery(sql);
        while (rs.next()) liste.add(mapRow(rs));
        return liste;
    }

    // ─────────────────────────────────────────────
    // GET LATEST DRIVER LOCATION — polled every 4s
    // ─────────────────────────────────────────────
    public String getLocalisationActuelle(int idLivraison) throws SQLException {
        PreparedStatement ps = cnx.prepareStatement(
                "SELECT localisation_actuelle FROM livraisons WHERE id_livraison = ?");
        ps.setInt(1, idLivraison);
        ResultSet rs = ps.executeQuery();
        if (rs.next()) return rs.getString("localisation_actuelle");
        return null;
    }

    // ─────────────────────────────────────────────
    // GET LATEST ETA — polled alongside location
    // ─────────────────────────────────────────────
    public String getHeureEstimee(int idLivraison) throws SQLException {
        PreparedStatement ps = cnx.prepareStatement(
                "SELECT date_livraison FROM livraisons WHERE id_livraison = ?");
        ps.setInt(1, idLivraison);
        ResultSet rs = ps.executeQuery();
        if (rs.next()) {
            Timestamp ts = rs.getTimestamp("date_livraison");
            if (ts != null) {
                var ldt = ts.toLocalDateTime();
                return String.format("%02dh%02d", ldt.getHour(), ldt.getMinute());
            }
        }
        return null;
    }

    // ─────────────────────────────────────────────
    // MAPPER
    // ─────────────────────────────────────────────
    private CommandeActive mapRow(ResultSet rs) throws SQLException {
        // ETA from date_livraison
        String eta = null;
        Timestamp ts = rs.getTimestamp("date_livraison");
        if (ts != null) {
            var ldt = ts.toLocalDateTime();
            eta = String.format("%02dh%02d", ldt.getHour(), ldt.getMinute());
        }

        // Map EN_COURS → AFFECTEE for display compatibility
        String statut = rs.getString("statut_livraison");
        if ("EN_COURS".equals(statut)) statut = "AFFECTEE";

        CommandeActive c = new CommandeActive(
                rs.getInt("id_livraison"),
                rs.getInt("id_commande"),
                rs.getString("adresse_client"),
                eta,
                statut,
                rs.getInt("id_livreur"),
                rs.getString("nom_livreur"),
                rs.getString("telephone"),
                rs.getString("genre_vehicule"),
                rs.getString("localisation_actuelle")
        );
        return c;
    }
}
