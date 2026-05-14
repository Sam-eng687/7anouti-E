package tn.hanouti.livreur.dao;

import tn.hanouti.livreur.model.SuiviLivraison;
import tn.hanouti.util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class SuiviLivraisonDAO {

    private Connection cnx = DBConnection.getInstance().getCnx();

    // ─────────────────────────────────────────────
    // LIRE EN ATTENTE — livraisons non encore affectées
    // ─────────────────────────────────────────────
    public List<SuiviLivraison> getEnAttente() throws SQLException {
        List<SuiviLivraison> liste = new ArrayList<>();
        String sql = "SELECT * FROM Suivi_Livraison WHERE statut = 'EN_ATTENTE'";
        ResultSet rs = cnx.createStatement().executeQuery(sql);
        while (rs.next()) liste.add(mapRow(rs));
        return liste;
    }

    // ─────────────────────────────────────────────
    // LIRE AFFECTÉES
    // ─────────────────────────────────────────────
    public List<SuiviLivraison> getAffectees() throws SQLException {
        List<SuiviLivraison> liste = new ArrayList<>();
        String sql = "SELECT * FROM Suivi_Livraison WHERE statut = 'AFFECTEE' ORDER BY id_suivi DESC";
        ResultSet rs = cnx.createStatement().executeQuery(sql);
        while (rs.next()) liste.add(mapRow(rs));
        return liste;
    }

    // ─────────────────────────────────────────────
    // LIRE LIVRÉES
    // ─────────────────────────────────────────────
    public List<SuiviLivraison> getLivrees() throws SQLException {
        List<SuiviLivraison> liste = new ArrayList<>();
        String sql = "SELECT * FROM Suivi_Livraison WHERE statut = 'LIVREE' ORDER BY id_suivi DESC";
        ResultSet rs = cnx.createStatement().executeQuery(sql);
        while (rs.next()) liste.add(mapRow(rs));
        return liste;
    }

    // ─────────────────────────────────────────────
    // LIRE PAR LIVREUR — interface livreur
    // ─────────────────────────────────────────────
    public List<SuiviLivraison> getByLivreur(int idLivreur) throws SQLException {
        List<SuiviLivraison> liste = new ArrayList<>();
        String sql = "SELECT * FROM Suivi_Livraison WHERE id_livreur = ? ORDER BY id_suivi DESC";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, idLivreur);
        ResultSet rs = ps.executeQuery();
        while (rs.next()) liste.add(mapRow(rs));
        return liste;
    }

    // ─────────────────────────────────────────────
    // AFFECTER UN LIVREUR
    // ─────────────────────────────────────────────
    public void affecterLivreur(int idSuivi, int idLivreur, String heureEstimee) throws SQLException {
        String sql = "UPDATE Suivi_Livraison SET id_livreur=?, statut='AFFECTEE', heure_estimee=? WHERE id_suivi=?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, idLivreur);
        ps.setString(2, heureEstimee);
        ps.setInt(3, idSuivi);
        ps.executeUpdate();
    }

    // ─────────────────────────────────────────────
    // MODIFIER LE STATUT
    // ─────────────────────────────────────────────
    public void modifierStatut(int idSuivi, String statut) throws SQLException {
        String sql = "UPDATE Suivi_Livraison SET statut=? WHERE id_suivi=?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setString(1, statut);
        ps.setInt(2, idSuivi);
        ps.executeUpdate();
    }

    // ─────────────────────────────────────────────
    // MAPPER
    // ─────────────────────────────────────────────
    private SuiviLivraison mapRow(ResultSet rs) throws SQLException {
        return new SuiviLivraison(
                rs.getInt("id_suivi"),
                rs.getInt("id_commande"),
                rs.getInt("id_livreur"),
                rs.getString("adresse_client"),
                rs.getString("localisation_actuelle"),
                rs.getString("heure_estimee"),
                rs.getString("statut")
        );
    }
}

