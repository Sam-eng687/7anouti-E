package tn.hanouti.dao;

import tn.hanouti.model.SuiviLivraison;
import tn.hanouti.util.DBConnection;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO for the 'livraisons' table (team schema).
 * Extra columns id_livreur, adresse_client, localisation_actuelle
 * are added by TestDataSeeder on first run.
 */
public class SuiviLivraisonDAO {

    private Connection cnx = DBConnection.getInstance().getCnx();

    // ─────────────────────────────────────────────
    // LIRE EN ATTENTE (statut = ASSIGNEE)
    // ─────────────────────────────────────────────
    public List<SuiviLivraison> getEnAttente() throws SQLException {
        List<SuiviLivraison> liste = new ArrayList<>();
        String sql = "SELECT * FROM livraisons WHERE statut_livraison = 'ASSIGNEE'";
        ResultSet rs = cnx.createStatement().executeQuery(sql);
        while (rs.next()) liste.add(mapRow(rs));
        return liste;
    }

    // ─────────────────────────────────────────────
    // LIRE PAR LIVREUR (statut EN_COURS ou LIVREE)
    // ─────────────────────────────────────────────
    public List<SuiviLivraison> getByLivreur(int idLivreur) throws SQLException {
        List<SuiviLivraison> liste = new ArrayList<>();
        String sql = "SELECT * FROM livraisons WHERE id_livreur = ? ORDER BY id_livraison DESC";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, idLivreur);
        ResultSet rs = ps.executeQuery();
        while (rs.next()) liste.add(mapRow(rs));
        return liste;
    }

    // ─────────────────────────────────────────────
    // AFFECTER UN LIVREUR
    // statut passe de ASSIGNEE → EN_COURS
    // ─────────────────────────────────────────────
    public void affecterLivreur(int idLivraison, int idLivreur,
                                String heureEstimee) throws SQLException {
        // Parse heureEstimee "14h30" → LocalDateTime today at 14:30
        LocalDateTime dateLivraison = parseHeure(heureEstimee);

        String sql = "UPDATE livraisons SET id_livreur=?, statut_livraison='EN_COURS', " +
                "date_debut_livraison=NOW(), date_livraison=? WHERE id_livraison=?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, idLivreur);
        ps.setObject(2, dateLivraison);
        ps.setInt(3, idLivraison);
        ps.executeUpdate();
    }

    // ─────────────────────────────────────────────
    // MODIFIER LE STATUT
    // ─────────────────────────────────────────────
    public void modifierStatut(int idLivraison, String statut) throws SQLException {
        // Map old statut values to new schema
        String newStatut = switch (statut) {
            case "EN_ATTENTE" -> "ASSIGNEE";
            case "AFFECTEE"   -> "EN_COURS";
            case "LIVREE"     -> "LIVREE";
            case "ANNULEE"    -> "ANNULEE";
            default           -> statut;
        };

        String sql = "UPDATE livraisons SET statut_livraison=?" +
                (newStatut.equals("LIVREE") ? ", date_livraison=NOW()" : "") +
                " WHERE id_livraison=?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setString(1, newStatut);
        ps.setInt(2, idLivraison);
        ps.executeUpdate();
    }

    // ─────────────────────────────────────────────
    // MAPPER
    // ─────────────────────────────────────────────
    private SuiviLivraison mapRow(ResultSet rs) throws SQLException {
        Timestamp tsAssign = rs.getTimestamp("date_assignation");
        Timestamp tsDebut  = rs.getTimestamp("date_debut_livraison");
        Timestamp tsLiv    = rs.getTimestamp("date_livraison");

        // Extra columns — may not exist if ALTER TABLE hasn't run yet
        int idLivreur = 0;
        String adresse = null;
        String localisation = null;
        try { idLivreur    = rs.getInt("id_livreur"); } catch (SQLException ignored) {}
        try { adresse      = rs.getString("adresse_client"); } catch (SQLException ignored) {}
        try { localisation = rs.getString("localisation_actuelle"); } catch (SQLException ignored) {}

        return new SuiviLivraison(
                rs.getInt("id_livraison"),
                rs.getInt("id_commande"),
                rs.getString("numero_commande"),
                rs.getInt("id_societe"),
                rs.getString("mode_assignation"),
                rs.getString("statut_livraison"),
                tsAssign != null ? tsAssign.toLocalDateTime() : null,
                tsDebut  != null ? tsDebut.toLocalDateTime()  : null,
                tsLiv    != null ? tsLiv.toLocalDateTime()    : null,
                idLivreur,
                adresse,
                localisation
        );
    }

    // ─────────────────────────────────────────────
    // HELPER — parse "14h30" → LocalDateTime today
    // ─────────────────────────────────────────────
    private LocalDateTime parseHeure(String heure) {
        try {
            if (heure != null && heure.contains("h")) {
                String[] parts = heure.split("h");
                int h = Integer.parseInt(parts[0]);
                int m = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;
                return LocalDateTime.now().withHour(h).withMinute(m).withSecond(0);
            }
        } catch (Exception ignored) {}
        return LocalDateTime.now().plusMinutes(30);
    }
}
