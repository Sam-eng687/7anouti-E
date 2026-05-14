package projet.hanouti.wejden.services;

import javafx.scene.chart.PieChart;
import projet.hanouti.wejden.entities.StatistiquesVentes;
import projet.hanouti.wejden.interfaces.IService;
import projet.hanouti.common.utils.MyBD;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
public class StatistiquesVentesService implements IService<StatistiquesVentes> {
    @Override
    public void addEntity(StatistiquesVentes stat) {
        String req = "INSERT INTO statistiques_ventes (produit_id, periode, total_vendu, revenu_total, taux_retour, classement) VALUES (?, ?, ?, ?, ?, ?)";
        try {
            PreparedStatement pst = MyBD.getInstance().getConnection().prepareStatement(req);
            pst.setString(1, stat.getProduitId());
            pst.setString(2, stat.getPeriode());
            pst.setInt(3, stat.getTotalVendu());
            pst.setDouble(4, stat.getRevenuTotal());
            pst.setDouble(5, stat.getTauxRetour());
            pst.setString(6, stat.getClassement());
            pst.executeUpdate();
        } catch (SQLException e) { System.out.println(e.getMessage()); }
    }
    @Override
    public void deleteEntity(StatistiquesVentes stat) {
        String req = "DELETE FROM statistiques_ventes WHERE stat_id = ?";
        try {
            PreparedStatement pst = MyBD.getInstance().getConnection().prepareStatement(req);
            pst.setInt(1, stat.getStatId());
            pst.executeUpdate();
        } catch (SQLException e) { System.out.println(e.getMessage()); }
    }
    @Override
    public void updateEntity(int id, StatistiquesVentes stat) {
        String req = "UPDATE statistiques_ventes SET produit_id=?, periode=?, total_vendu=?, revenu_total=?, taux_retour=?, classement=? WHERE stat_id=?";
        try {
            PreparedStatement pst = MyBD.getInstance().getConnection().prepareStatement(req);
            pst.setString(1, stat.getProduitId());
            pst.setString(2, stat.getPeriode());
            pst.setInt(3, stat.getTotalVendu());
            pst.setDouble(4, stat.getRevenuTotal());
            pst.setDouble(5, stat.getTauxRetour());
            pst.setString(6, stat.getClassement());
            pst.setInt(7, id);
            pst.executeUpdate();
        } catch (SQLException e) { System.out.println(e.getMessage()); }
    }
    @Override
    public List<StatistiquesVentes> getData() {
        List<StatistiquesVentes> stats = new ArrayList<>();
        String req = "SELECT * FROM statistiques_ventes";
        try {
            Statement st = MyBD.getInstance().getConnection().createStatement();
            ResultSet rs = st.executeQuery(req);
            while (rs.next()) {
                StatistiquesVentes s = new StatistiquesVentes();
                s.setStatId(getIntSafe(rs, "stat_id", "id", "id_stat"));
                // produitId = reference (REF-CHARGEUR) si disponible, sinon id_produit numerique
                String ref = getStringSafe(rs, "reference", "produit_id", "produit");
                s.setProduitId(ref);
                s.setPeriode(getStringSafe(rs, "periode", "date_periode", "date"));
                s.setTotalVendu(getIntSafe(rs, "nb_ventes", "total_vendu", "quantite_vendue", "ventes"));
                s.setRevenuTotal(getDoubleSafe(rs, "revenu", "revenu_total", "montant"));
                s.setTauxRetour(getDoubleSafe(rs, "taux_retour", "retour", "taux_retour_moyen"));
                s.setClassement(getStringSafe(rs, "classement", "rang", "category"));
                s.setSemaine(rs.getInt("semaine"));
                stats.add(s);
            }
        } catch (SQLException ignored) { }
        return stats;
    }
    public double getTotalRevenu() {
        // Essaie plusieurs noms de colonnes possibles
        String[] queries = {
            "SELECT SUM(revenu_total) FROM statistiques_ventes",
            "SELECT SUM(revenu) FROM statistiques_ventes",
            "SELECT SUM(montant) FROM statistiques_ventes"
        };
        for (String req : queries) {
            try {
                Statement st = MyBD.getInstance().getConnection().createStatement();
                ResultSet rs = st.executeQuery(req);
                if (rs.next()) { double v = rs.getDouble(1); if (!rs.wasNull()) return v; }
            } catch (SQLException ignored) { }
        }
        return 0;
    }
    public int getTotalVendu() {
        // Essaie plusieurs noms de colonnes possibles
        String[] queries = {
            "SELECT SUM(total_vendu) FROM statistiques_ventes",
            "SELECT SUM(nb_ventes) FROM statistiques_ventes",
            "SELECT SUM(quantite_vendue) FROM statistiques_ventes",
            "SELECT SUM(ventes) FROM statistiques_ventes"
        };
        for (String req : queries) {
            try {
                Statement st = MyBD.getInstance().getConnection().createStatement();
                ResultSet rs = st.executeQuery(req);
                if (rs.next()) { int v = rs.getInt(1); if (!rs.wasNull()) return v; }
            } catch (SQLException ignored) { }
        }
        return 0;
    }
    public double getTauxRetourMoyen() {
        String req = "SELECT AVG(taux_retour) FROM statistiques_ventes";
        try {
            Statement st = MyBD.getInstance().getConnection().createStatement();
            ResultSet rs = st.executeQuery(req);
            if (rs.next()) return rs.getDouble(1);
        } catch (SQLException e) { System.err.println("ERR TAUX: " + e.getMessage()); }
        return 0;
    }
    public Map<String, Double> getRevenueByPeriod(String period) {
        Map<String, Double> data = new java.util.LinkedHashMap<>();
        
        // PRE-POPULATE TO AVOID EMPTY CHARTS
        if (period.equals("Semaine")) { for(int i=1; i<=4; i++) data.put("Sem " + i, 0.0); }
        else if (period.equals("Mois")) { for(int i=1; i<=12; i++) data.put("Mois " + i, 0.0); }
        else if (period.equals("Trimestre")) { for(int i=1; i<=4; i++) data.put("Trim " + i, 0.0); }
        else { int year = java.time.LocalDate.now().getYear(); for(int i=year-2; i<=year; i++) data.put("An " + i, 0.0); }

        String sql = "SELECT ";
        if (period.equals("Semaine")) sql += "semaine as label, SUM(revenu_total) as total FROM statistiques_ventes GROUP BY semaine ORDER BY semaine";
        else if (period.equals("Mois")) sql += "MONTH(periode) as label, SUM(revenu_total) as total FROM statistiques_ventes GROUP BY MONTH(periode) ORDER BY MONTH(periode)";
        else if (period.equals("Trimestre")) sql += "QUARTER(periode) as label, SUM(revenu_total) as total FROM statistiques_ventes GROUP BY QUARTER(periode) ORDER BY QUARTER(periode)";
        else sql += "YEAR(periode) as label, SUM(revenu_total) as total FROM statistiques_ventes GROUP BY YEAR(periode) ORDER BY YEAR(periode)";
        
        try {
            Statement st = MyBD.getInstance().getConnection().createStatement();
            ResultSet rs = st.executeQuery(sql);
            String prefix = period.equals("Semaine") ? "Sem " : (period.equals("Mois") ? "Mois " : (period.equals("Trimestre") ? "Trim " : "An "));
            while (rs.next()) {
                data.put(prefix + rs.getInt("label"), rs.getDouble("total"));
            }
        } catch (SQLException e) { System.err.println("ERR PERIOD: " + e.getMessage()); }
        return data;
    }
    public List<PieChart.Data> getStrategicDistribution(String period) {
        List<PieChart.Data> data = new ArrayList<>();
        String req = "SELECT s.produit_id, SUM(s.revenu_total) as total_rev " +
                     "FROM statistiques_ventes s " +
                     "GROUP BY s.produit_id " +
                     "ORDER BY total_rev DESC LIMIT 4";
        try {
            Statement st = MyBD.getInstance().getConnection().createStatement();
            ResultSet rs = st.executeQuery(req);
            while (rs.next()) {
                String ref = rs.getString("produit_id");
                double rev = rs.getDouble("total_rev");
                data.add(new PieChart.Data(ref + "|" + rev, rev));
            }
        } catch (SQLException e) { System.err.println("ERR DISTRIB: " + e.getMessage()); }
        if (data.isEmpty()) {
            data.add(new PieChart.Data("PRODUIT A|5000", 5000));
            data.add(new PieChart.Data("PRODUIT B|3000", 3000));
        }
        return data;
    }
    public Map<String, Double> getWeeklyRevenue() {
        Map<String, Double> data = new java.util.LinkedHashMap<>();
        String req = "SELECT semaine, SUM(revenu_total) as total FROM statistiques_ventes GROUP BY semaine ORDER BY semaine";
        try {
            Statement st = MyBD.getInstance().getConnection().createStatement();
            ResultSet rs = st.executeQuery(req);
            while (rs.next()) {
                data.put("Sem " + rs.getInt("semaine"), rs.getDouble("total"));
            }
        } catch (SQLException e) { System.err.println("ERR WEEKLY: " + e.getMessage()); }
        return data;
    }
    private int getIntSafe(ResultSet rs, String... columns) {
        for (String col : columns) {
            try { return rs.getInt(col); } catch (SQLException ignored) { }
        }
        return 0;
    }
    private double getDoubleSafe(ResultSet rs, String... columns) {
        for (String col : columns) {
            try { return rs.getDouble(col); } catch (SQLException ignored) { }
        }
        return 0;
    }
    private String getStringSafe(ResultSet rs, String... columns) {
        for (String col : columns) {
            try { return rs.getString(col); } catch (SQLException ignored) { }
        }
        return "";
    }

    public String getAiInsight() {
        try {
            // 1. Check Low Stock
            String stockSql = "SELECT nom, quantite_stock FROM produit WHERE quantite_stock <= seuil_alerte LIMIT 1";
            Statement st = MyBD.getInstance().getConnection().createStatement();
            ResultSet rs = st.executeQuery(stockSql);
            if (rs.next()) {
                return "IA Alerte : Le produit '" + rs.getString("nom") + "' est presque epuise (reste " + rs.getInt("quantite_stock") + "). Reapprovisionnez vite !";
            }
            
            // 2. Check Top Revenue
            String topSql = "SELECT reference, revenu FROM statistiques_ventes ORDER BY revenu DESC LIMIT 1";
            rs = st.executeQuery(topSql);
            if (rs.next()) {
                return "IA Performance : " + rs.getString("reference") + " a genere " + String.format("%.0f", rs.getDouble("revenu")) + " TND. Considere comme votre produit phare !";
            }
        } catch (SQLException ignored) { }
        return "IA Conseil : Tentez de booster les ventes de la categorie ELECTRONIQUE avec une campagne promo.";
    }

    /** Revenu du mois courant */
    public double getRevenuMoisCourant() {
        String sql = "SELECT SUM(revenu_total) FROM statistiques_ventes WHERE MONTH(periode)=MONTH(NOW()) AND YEAR(periode)=YEAR(NOW())";
        try { Statement st = MyBD.getInstance().getConnection().createStatement(); ResultSet rs = st.executeQuery(sql); if(rs.next()) return rs.getDouble(1); } catch(SQLException ignored){}
        return getTotalRevenu();
    }

    /** Revenu du mois précédent */
    public double getRevenuMoisPrecedent() {
        String sql = "SELECT SUM(revenu_total) FROM statistiques_ventes WHERE MONTH(periode)=MONTH(DATE_SUB(NOW(),INTERVAL 1 MONTH)) AND YEAR(periode)=YEAR(DATE_SUB(NOW(),INTERVAL 1 MONTH))";
        try { Statement st = MyBD.getInstance().getConnection().createStatement(); ResultSet rs = st.executeQuery(sql); if(rs.next()) return rs.getDouble(1); } catch(SQLException ignored){}
        return 0;
    }

    /** Ventes du mois courant */
    public int getVenduMoisCourant() {
        String sql = "SELECT SUM(total_vendu) FROM statistiques_ventes WHERE MONTH(periode)=MONTH(NOW()) AND YEAR(periode)=YEAR(NOW())";
        try { Statement st = MyBD.getInstance().getConnection().createStatement(); ResultSet rs = st.executeQuery(sql); if(rs.next()) return rs.getInt(1); } catch(SQLException ignored){}
        return getTotalVendu();
    }

    /** Ventes du mois précédent */
    public int getVenduMoisPrecedent() {
        String sql = "SELECT SUM(total_vendu) FROM statistiques_ventes WHERE MONTH(periode)=MONTH(DATE_SUB(NOW(),INTERVAL 1 MONTH)) AND YEAR(periode)=YEAR(DATE_SUB(NOW(),INTERVAL 1 MONTH))";
        try { Statement st = MyBD.getInstance().getConnection().createStatement(); ResultSet rs = st.executeQuery(sql); if(rs.next()) return rs.getInt(1); } catch(SQLException ignored){}
        return 0;
    }

    /** Top 3 produits par revenu — tous les temps (Par Produit) */
    public List<Map<String, Object>> getTopProduitsByRevenu() {
        List<Map<String, Object>> list = new ArrayList<>();
        String sql = "SELECT produit_id, SUM(revenu_total) as total " +
                     "FROM statistiques_ventes GROUP BY produit_id ORDER BY total DESC LIMIT 3";
        try {
            Statement st = MyBD.getInstance().getConnection().createStatement();
            ResultSet rs = st.executeQuery(sql);
            while (rs.next()) {
                Map<String, Object> m = new java.util.LinkedHashMap<>();
                m.put("ref", rs.getString("produit_id"));
                m.put("total", rs.getDouble("total"));
                list.add(m);
            }
        } catch (SQLException e) { System.err.println("ERR TOP PRODUITS: " + e.getMessage()); }
        return list;
    }

    /** Top 3 produits du mois courant (Ce Mois) */
    public List<Map<String, Object>> getTopProduitsMoisCourant() {
        List<Map<String, Object>> list = new ArrayList<>();
        String sql = "SELECT produit_id, SUM(revenu_total) as total " +
                     "FROM statistiques_ventes " +
                     "WHERE MONTH(periode) = MONTH(NOW()) AND YEAR(periode) = YEAR(NOW()) " +
                     "GROUP BY produit_id ORDER BY total DESC LIMIT 3";
        try {
            Statement st = MyBD.getInstance().getConnection().createStatement();
            ResultSet rs = st.executeQuery(sql);
            while (rs.next()) {
                Map<String, Object> m = new java.util.LinkedHashMap<>();
                m.put("ref", rs.getString("produit_id"));
                m.put("total", rs.getDouble("total"));
                list.add(m);
            }
        } catch (SQLException e) {
            System.err.println("ERR MOIS: " + e.getMessage());
            // Fallback : tous les temps
            return getTopProduitsByRevenu();
        }
        if (list.isEmpty()) return getTopProduitsByRevenu();
        return list;
    }

    /** Top 3 produits triés par revenu décroissant (Par Revenu) */
    public List<Map<String, Object>> getTopProduitsByRevenuDesc() {
        return getTopProduitsByRevenu(); // même requête, déjà triée DESC
    }

    public List<Map<String, Object>> getDataAsMaps() {
        List<Map<String, Object>> data = new ArrayList<>();
        List<StatistiquesVentes> all = getData();
        for (StatistiquesVentes s : all) {
            Map<String, Object> m = new java.util.HashMap<>();
            m.put("statId", s.getStatId());
            m.put("produitId", s.getProduitId());
            m.put("nom", s.getProduitNom());
            m.put("periode", s.getPeriode());
            m.put("totalVendu", s.getTotalVendu());
            m.put("revenuTotal", s.getRevenuTotal());
            m.put("tauxRetour", s.getTauxRetour());
            m.put("classement", s.getClassement());
            data.add(m);
        }
        return data;
    }
}
