package projet.hanouti.wejden.services;

import projet.hanouti.common.utils.MyBD;

import java.sql.*;
import java.util.*;

public class VendeurService {

    /** Retourne tous les vendeurs depuis users WHERE role='vendeur' */
    public List<Map<String, String>> getAll() {
        List<Map<String, String>> list = new ArrayList<>();
        String sql = "SELECT id, nom, prenom, e_mail, num_tel FROM users " +
                "WHERE role='vendeur' AND status='Unbanned' ORDER BY nom";
        try {
            Statement st = MyBD.getInstance().getConnection().createStatement();
            ResultSet rs = st.executeQuery(sql);
            while (rs.next()) {
                Map<String, String> m = new LinkedHashMap<>();
                m.put("id", String.valueOf(rs.getInt("id")));
                m.put("nom", rs.getString("nom") + " " + rs.getString("prenom"));
                m.put("email", rs.getString("e_mail"));
                m.put("tel", rs.getString("num_tel") != null ? rs.getString("num_tel") : "");
                list.add(m);
            }
            System.out.println("Vendeurs trouves : " + list.size());
        } catch (SQLException e) {
            System.err.println("Erreur VendeurService.getAll: " + e.getMessage());
        }
        return list;
    }

    /** Recherche vendeurs par nom (autocomplete) */
    public List<Map<String, String>> searchByNom(String query) {
        List<Map<String, String>> list = new ArrayList<>();
        String sql = "SELECT id, nom, prenom, e_mail, num_tel FROM users " +
                "WHERE role='vendeur' AND status='Unbanned' " +
                "AND (nom LIKE ? OR prenom LIKE ? OR CONCAT(nom,' ',prenom) LIKE ?) " +
                "ORDER BY nom LIMIT 10";
        try {
            PreparedStatement ps = MyBD.getInstance().getConnection().prepareStatement(sql);
            String q = "%" + query + "%";
            ps.setString(1, q);
            ps.setString(2, q);
            ps.setString(3, q);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Map<String, String> m = new LinkedHashMap<>();
                m.put("id", String.valueOf(rs.getInt("id")));
                m.put("nom", rs.getString("nom") + " " + rs.getString("prenom"));
                m.put("email", rs.getString("e_mail"));
                m.put("tel", rs.getString("num_tel") != null ? rs.getString("num_tel") : "");
                list.add(m);
            }
        } catch (SQLException e) {
            System.err.println("Erreur VendeurService.searchByNom: " + e.getMessage());
        }
        return list;
    }
}
