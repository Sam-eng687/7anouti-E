package edu.hanouti.services;

import edu.hanouti.entities.ConseilsMarketing;
import edu.hanouti.interfaces.IService;
import edu.hanouti.utils.MyConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ConseilsMarketingService implements IService<ConseilsMarketing> {

    @Override
    public void addEntity(ConseilsMarketing conseil) {
        String req = "INSERT INTO conseils_marketing (produit_id, type_conseil, description, impact_estime, date_generation, applique) VALUES (?, ?, ?, ?, ?, ?)";
        try {
            PreparedStatement pst = MyConnection.getInstance().getCnx().prepareStatement(req);
            pst.setString(1, conseil.getProduitId());
            pst.setString(2, conseil.getTypeConseil());
            pst.setString(3, conseil.getDescription());
            pst.setString(4, conseil.getImpactEstime());
            pst.setDate(5, conseil.getDateGeneration());
            pst.setBoolean(6, conseil.isApplique());
            pst.executeUpdate();
        } catch (SQLException e) { System.out.println(e.getMessage()); }
    }

    @Override
    public void deleteEntity(ConseilsMarketing conseil) {
        String req = "DELETE FROM conseils_marketing WHERE conseil_id = ?";
        try {
            PreparedStatement pst = MyConnection.getInstance().getCnx().prepareStatement(req);
            pst.setInt(1, conseil.getConseilId());
            pst.executeUpdate();
        } catch (SQLException e) { System.out.println(e.getMessage()); }
    }

    @Override
    public void updateEntity(int id, ConseilsMarketing conseil) {
        String req = "UPDATE conseils_marketing SET produit_id=?, type_conseil=?, description=?, impact_estime=?, date_generation=?, applique=? WHERE conseil_id=?";
        try {
            PreparedStatement pst = MyConnection.getInstance().getCnx().prepareStatement(req);
            pst.setString(1, conseil.getProduitId());
            pst.setString(2, conseil.getTypeConseil());
            pst.setString(3, conseil.getDescription());
            pst.setString(4, conseil.getImpactEstime());
            pst.setDate(5, conseil.getDateGeneration());
            pst.setBoolean(6, conseil.isApplique());
            pst.setInt(7, id);
            pst.executeUpdate();
        } catch (SQLException e) { System.out.println(e.getMessage()); }
    }

    @Override
    public List<ConseilsMarketing> getData() {
        List<ConseilsMarketing> conseils = new ArrayList<>();
        // Jointure avec la table produit pour avoir le nom reel
        // CAST necessaire car conseils_ia.id_produit est VARCHAR et produit.id_produit est INT
        String req = "SELECT c.id_conseil, c.id_produit, c.type, c.description, c.score, c.etat, c.urgence, c.date_genere, p.nom " +
                     "FROM conseils_ia c LEFT JOIN produit p ON CAST(c.id_produit AS UNSIGNED) = p.id_produit " +
                     "UNION ALL " +
                     "SELECT conseil_id as id_conseil, CAST(c.produit_id AS CHAR) as id_produit, type_conseil as type, description, 0 as score, IF(applique, 'ACCEPTE', 'NOUVEAU') as etat, 'NORMAL' as urgence, date_generation as date_genere, p.nom " +
                     "FROM conseils_marketing c LEFT JOIN produit p ON c.produit_id = p.id_produit";
        try {
            Statement st = MyConnection.getInstance().getCnx().createStatement();
            ResultSet rs = st.executeQuery(req);
            while (rs.next()) {
                ConseilsMarketing c = new ConseilsMarketing();
                c.setConseilId(rs.getInt("id_conseil"));
                c.setProduitId(rs.getString("id_produit"));
                c.setProduitNom(rs.getString("nom"));
                c.setTypeConseil(rs.getString("type"));
                c.setDescription(rs.getString("description"));
                c.setScore(rs.getInt("score"));
                c.setUrgence(rs.getString("urgence"));
                c.setDateGeneration(rs.getDate("date_genere"));
                c.setApplique("ACCEPTE".equals(rs.getString("etat")));
                conseils.add(c);
            }
        } catch (SQLException ignored) { 
            return getOldData();
        }
        return conseils;
    }

    private List<ConseilsMarketing> getOldData() {
        List<ConseilsMarketing> conseils = new ArrayList<>();
        String req = "SELECT * FROM conseils_marketing";
        try {
            Statement st = MyConnection.getInstance().getCnx().createStatement();
            ResultSet rs = st.executeQuery(req);
            while (rs.next()) {
                ConseilsMarketing c = new ConseilsMarketing();
                c.setConseilId(getIntSafe(rs, "conseil_id", "id_conseil", "id"));
                c.setProduitId(getStringSafe(rs, "produit_id", "produit", "reference"));
                c.setTypeConseil(getStringSafe(rs, "type_conseil", "type", "categorie"));
                c.setDescription(getStringSafe(rs, "description", "contenu", "message"));
                c.setImpactEstime(getStringSafe(rs, "impact_estime", "impact", "priorite"));
                c.setDateGeneration(getDateSafe(rs, "date_generation", "date_creation", "date"));
                c.setApplique(getBooleanSafe(rs, "applique", "applied", "etat"));
                conseils.add(c);
            }
        } catch (SQLException ignored) { }
        return conseils;
    }

    public long countAppliques() {
        String req = "SELECT COUNT(*) FROM conseils_marketing WHERE applique = true";
        try {
            Statement st = MyConnection.getInstance().getCnx().createStatement();
            ResultSet rs = st.executeQuery(req);
            if (rs.next()) return rs.getLong(1);
        } catch (SQLException e) { System.out.println(e.getMessage()); }
        return 0;
    }

    public void appliquerConseil(int id) {
        String req = "UPDATE conseils_marketing SET applique = true WHERE conseil_id = ?";
        try {
            PreparedStatement pst = MyConnection.getInstance().getCnx().prepareStatement(req);
            pst.setInt(1, id);
            pst.executeUpdate();
        } catch (SQLException ignored) { }
    }

    public void updateStatut(int id, String etat) {
        String req = "UPDATE conseils_ia SET etat = ? WHERE id_conseil = ?";
        try {
            PreparedStatement pst = MyConnection.getInstance().getCnx().prepareStatement(req);
            pst.setString(1, etat);
            pst.setInt(2, id);
            pst.executeUpdate();
            
            // Also update old table if exists
            String oldReq = "UPDATE conseils_marketing SET applique = ? WHERE conseil_id = ?";
            PreparedStatement pstOld = MyConnection.getInstance().getCnx().prepareStatement(oldReq);
            pstOld.setBoolean(1, "ACCEPTE".equals(etat));
            pstOld.setInt(2, id);
            pstOld.executeUpdate();
        } catch (SQLException ignored) { }
    }

    private int getIntSafe(ResultSet rs, String... columns) {
        for (String col : columns) {
            try { return rs.getInt(col); } catch (SQLException ignored) { }
        }
        return 0;
    }

    private String getStringSafe(ResultSet rs, String... columns) {
        for (String col : columns) {
            try { return rs.getString(col); } catch (SQLException ignored) { }
        }
        return "";
    }

    private java.sql.Date getDateSafe(ResultSet rs, String... columns) {
        for (String col : columns) {
            try { return rs.getDate(col); } catch (SQLException ignored) { }
        }
        return new java.sql.Date(System.currentTimeMillis());
    }

    private boolean getBooleanSafe(ResultSet rs, String... columns) {
        for (String col : columns) {
            try { return rs.getBoolean(col); } catch (SQLException ignored) { }
        }
        return false;
    }
}
