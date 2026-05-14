package projet.hanouti.wejden.services;



import projet.hanouti.wejden.entities.CampagneMarketing;
import projet.hanouti.wejden.interfaces.IService;
import projet.hanouti.common.utils.MyBD;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CampagneMarketingService implements IService<CampagneMarketing> {

    public void createTableIfNotExists() {
        String req = "CREATE TABLE IF NOT EXISTS campagne_marketing (" +
                     "campagne_id INT AUTO_INCREMENT PRIMARY KEY," +
                     "nom_campagne VARCHAR(255)," +
                     "objectif VARCHAR(100)," +
                     "canal VARCHAR(100)," +
                     "budget DOUBLE," +
                     "depense DOUBLE," +
                     "statut VARCHAR(50)," +
                     "date_debut DATE," +
                     "date_fin DATE," +
                     "ia_score DOUBLE," +
                     "ia_conseil TEXT)";
        try {
            Statement st = MyBD.getInstance().getConnection().createStatement();
            st.execute(req);
        } catch (SQLException e) { System.out.println(e.getMessage()); }
    }

    public void addEntity(CampagneMarketing c) {
        // Vérifier si une campagne avec le même nom existe déjà
        String checkSql = "SELECT COUNT(*) FROM campagne_marketing WHERE nom = ? AND vendor_id = 1";
        try {
            PreparedStatement check = MyBD.getInstance().getConnection().prepareStatement(checkSql);
            check.setString(1, c.getNomCampagne());
            ResultSet rs = check.executeQuery();
            if (rs.next() && rs.getInt(1) > 0) {
                System.out.println("Campagne deja existante : " + c.getNomCampagne() + " - ignoree");
                return;
            }
        } catch (SQLException ignored) {}

        String req = "INSERT INTO campagne_marketing (vendor_id, nom, type_action, objectif, statut, date_debut, date_fin, budget_alloue, budget_depense, score_ia) VALUES (1, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try {
            PreparedStatement pst = MyBD.getInstance().getConnection().prepareStatement(req);
            pst.setString(1, c.getNomCampagne());
            pst.setString(2, c.getCanal());
            pst.setString(3, c.getObjectif());
            pst.setString(4, c.getStatut());
            pst.setDate(5, c.getDateDebut());
            pst.setDate(6, c.getDateFin());
            pst.setDouble(7, c.getBudget());
            pst.setDouble(8, c.getDepense());
            pst.setDouble(9, c.getIaScore());
            pst.executeUpdate();
            System.out.println("✅ Campagne ajoutée : " + c.getNomCampagne());
        } catch (SQLException e) { System.out.println("Erreur ADD: " + e.getMessage()); }
    }

    @Override
    public void deleteEntity(CampagneMarketing c) {
        String req = "DELETE FROM campagne_marketing WHERE id = ?";
        try {
            PreparedStatement pst = MyBD.getInstance().getConnection().prepareStatement(req);
            pst.setInt(1, c.getCampagneId());
            pst.executeUpdate();
            System.out.println("✅ Campagne supprimée id=" + c.getCampagneId());
        } catch (SQLException e) { System.out.println("Erreur DEL: " + e.getMessage()); }
    }

    @Override
    public void updateEntity(int id, CampagneMarketing c) {
        String req = "UPDATE campagne_marketing SET nom=?, type_action=?, objectif=?, statut=?, date_debut=?, date_fin=?, budget_alloue=?, budget_depense=?, score_ia=? WHERE id=?";
        try {
            PreparedStatement pst = MyBD.getInstance().getConnection().prepareStatement(req);
            pst.setString(1, c.getNomCampagne());
            pst.setString(2, c.getCanal());
            pst.setString(3, c.getObjectif());
            pst.setString(4, c.getStatut());
            pst.setDate(5, c.getDateDebut());
            pst.setDate(6, c.getDateFin());
            pst.setDouble(7, c.getBudget());
            pst.setDouble(8, c.getDepense());
            pst.setDouble(9, c.getIaScore());
            pst.setInt(10, id);
            pst.executeUpdate();
            System.out.println("✅ Campagne mise à jour id=" + id);
        } catch (SQLException e) { System.out.println("Erreur UPD: " + e.getMessage()); }
    }

    public List<CampagneMarketing> getData() {
        // Auto-corriger les campagnes expirées
        try {
            String autoFix = "UPDATE campagne_marketing SET statut='TERMINEE' " +
                             "WHERE statut='ACTIVE' AND date_fin < CURDATE()";
            MyBD.getInstance().getConnection().createStatement().executeUpdate(autoFix);
        } catch (SQLException ignored) {}

        List<CampagneMarketing> list = new ArrayList<>();
        String req = "SELECT * FROM campagne_marketing";
        try {
            Connection cnx = MyBD.getInstance().getConnection();
            Statement st = cnx.createStatement();
            ResultSet rs = st.executeQuery(req);
            ResultSetMetaData meta = rs.getMetaData();
            int count = 0;
            while (rs.next()) {
                count++;
                CampagneMarketing c = new CampagneMarketing();
                // Recherche dynamique de la colonne ID
                int idCol = 1;
                for(int i=1; i<=meta.getColumnCount(); i++) {
                    String colName = meta.getColumnName(i).toLowerCase();
                    if(colName.equals("id") || colName.equals("campagne_id")) { idCol = i; break; }
                }
                c.setCampagneId(rs.getInt(idCol));
                System.out.println("📋 Campagne chargée: id=" + rs.getInt(idCol) + " nom=" + rs.getString("nom"));
                
                // Mapping resilient pour les autres colonnes
                c.setNomCampagne(getStringSafe(rs, "nom", "nom_campagne", "titre"));
                c.setObjectif(getStringSafe(rs, "objectif", "type_action"));
                c.setCanal(getStringSafe(rs, "type_action", "canal", "type"));
                c.setBudget(getDoubleSafe(rs, "budget_alloue", "budget", "montant"));
                c.setDepense(getDoubleSafe(rs, "budget_depense", "depense"));
                c.setStatut(getStringSafe(rs, "statut", "etat"));
                c.setDateDebut(rs.getDate("date_debut"));
                c.setDateFin(rs.getDate("date_fin"));
                c.setIaScore(getDoubleSafe(rs, "score_ia", "ia_score"));
                list.add(c);
            }
            System.out.println("🔍 [DEBUG SQL] " + count + " campagnes recuperees avec succes.");
        } catch (SQLException e) { 
            System.err.println("❌ [ERREUR SQL GET]: " + e.getMessage()); 
        }
        return list;
    }

    private String getStringSafe(ResultSet rs, String... cols) {
        for(String s : cols) { try { return rs.getString(s); } catch(Exception ignored) {} }
        return "N/A";
    }
    private double getDoubleSafe(ResultSet rs, String... cols) {
        for(String s : cols) { try { return rs.getDouble(s); } catch(Exception ignored) {} }
        return 0.0;
    }

    public double getBudgetTotal() {
        try {
            Statement st = MyBD.getInstance().getConnection().createStatement();
            ResultSet rs = st.executeQuery("SELECT SUM(budget_alloue) FROM campagne_marketing");
            if (rs.next()) return rs.getDouble(1);
        } catch (SQLException e) { System.err.println("ERR BUDGET SUM: " + e.getMessage()); }
        return 0;
    }

    public double getDepenseTotal() {
        try {
            Statement st = MyBD.getInstance().getConnection().createStatement();
            ResultSet rs = st.executeQuery("SELECT SUM(budget_depense) FROM campagne_marketing");
            if (rs.next()) return rs.getDouble(1);
        } catch (SQLException e) { System.err.println("ERR DEPENSE SUM: " + e.getMessage()); }
        return 0;
    }

    public long countByStatut(String statut) {
        String req = "SELECT COUNT(*) FROM campagne_marketing WHERE statut = ?";
        try {
            PreparedStatement pst = MyBD.getInstance().getConnection().prepareStatement(req);
            pst.setString(1, statut);
            ResultSet rs = pst.executeQuery();
            if (rs.next()) return rs.getLong(1);
        } catch (SQLException ignored) { }
        return 0;
    }

    /**
     * Calcule le revenu attribue a une campagne
     * Attribution par periode : ventes dont la date est entre date_debut et date_fin
     */
    public double getRevenuAttribue(java.sql.Date dateDebut, java.sql.Date dateFin) {
        if (dateDebut == null || dateFin == null) return 0;
        // Essayer plusieurs noms de colonnes possibles
        String[] queries = {
            "SELECT SUM(revenu_total) FROM statistiques_ventes WHERE periode BETWEEN ? AND ?",
            "SELECT SUM(revenu) FROM statistiques_ventes WHERE periode BETWEEN ? AND ?",
            "SELECT SUM(montant) FROM statistiques_ventes WHERE periode BETWEEN ? AND ?"
        };
        for (String sql : queries) {
            try {
                PreparedStatement ps = MyBD.getInstance().getConnection().prepareStatement(sql);
                ps.setDate(1, dateDebut);
                ps.setDate(2, dateFin);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    double val = rs.getDouble(1);
                    if (!rs.wasNull()) return val;
                }
            } catch (SQLException ignored) {}
        }
        return 0;
    }

    /**
     * Calcule le ROI d'une campagne
     * ROI = (Revenu attribue - Budget depense) / Budget depense * 100
     */
    public double getRoi(CampagneMarketing c) {
        if (c.getDepense() <= 0) return 0;
        double revenu = getRevenuAttribue(c.getDateDebut(), c.getDateFin());
        return ((revenu - c.getDepense()) / c.getDepense()) * 100;
    }

    private int getIntSafe(ResultSet rs, String... columns) {
        for (String col : columns) {
            try { return rs.getInt(col); } catch (SQLException ignored) { }
        }
        return 0;
    }

    private Date getDateSafe(ResultSet rs, String... columns) {
        for (String col : columns) {
            try { return rs.getDate(col); } catch (SQLException ignored) { }
        }
        return null;
    }
}
