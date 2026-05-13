
package edu.hanouti.services;

import edu.hanouti.utils.MyConnection;
import java.sql.*;
import java.util.*;

public class ConseilsIAService {

    public List<Map<String, Object>> getConseils() {
        List<Map<String, Object>> list = new ArrayList<>();
        try {
            Connection c = MyConnection.getInstance().getCnx();
            String sql = "SELECT ci.*, p.nom AS produit_nom FROM conseils_ia ci " +
                    "LEFT JOIN produit p ON p.id_produit = CAST(ci.id_produit AS UNSIGNED) " +
                    "ORDER BY ci.id_conseil DESC";
            Statement st = c.createStatement();
            ResultSet rs = st.executeQuery(sql);

            while (rs.next()) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("id", rs.getInt("id_conseil"));
                String idProdStr = rs.getString("id_produit");
                int idProdInt = 0;
                if (idProdStr != null && !idProdStr.isBlank()) {
                    try { idProdInt = Integer.parseInt(idProdStr.trim()); } catch (NumberFormatException ignored) {}
                }
                row.put("id_produit", idProdInt);
                String nomProduit = rs.getString("produit_nom");
                row.put("produit_nom", (nomProduit != null && !nomProduit.isBlank()) ? nomProduit : null);
                row.put("type", rs.getString("type"));
                row.put("description", rs.getString("description"));
                row.put("urgence", rs.getString("urgence"));
                row.put("score", rs.getInt("score"));
                row.put("etat", rs.getString("etat"));
                list.add(row);
            }
        } catch (SQLException e) {
            System.err.println("Erreur lecture conseils_ia: " + e.getMessage());
        }
        return list;
    }

    public List<Map<String, Object>> getLatestAdvice(int limit) {
        List<Map<String, Object>> list = new ArrayList<>();
        try {
            Connection c = MyConnection.getInstance().getCnx();
            String sql = "SELECT ci.*, p.nom AS produit_nom FROM conseils_ia ci " +
                    "LEFT JOIN produit p ON p.id_produit = CAST(ci.id_produit AS UNSIGNED) " +
                    "ORDER BY (ci.etat='NOUVEAU') DESC, ci.id_conseil DESC LIMIT ?";
            PreparedStatement pst = c.prepareStatement(sql);
            pst.setInt(1, limit);
            ResultSet rs = pst.executeQuery();

            while (rs.next()) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("id", rs.getInt("id_conseil"));
                String idProdStr = rs.getString("id_produit");
                int idProdInt = 0;
                if (idProdStr != null && !idProdStr.isBlank()) {
                    try { idProdInt = Integer.parseInt(idProdStr.trim()); } catch (NumberFormatException ignored) {}
                }
                row.put("id_produit", idProdInt);
                String nomProduit = rs.getString("produit_nom");
                row.put("produit_nom", (nomProduit != null && !nomProduit.isBlank()) ? nomProduit : null);
                row.put("type", rs.getString("type"));
                row.put("description", rs.getString("description"));
                row.put("urgence", rs.getString("urgence"));
                row.put("score", rs.getInt("score"));
                row.put("etat", rs.getString("etat"));
                list.add(row);
            }
        } catch (SQLException e) {
            System.err.println("Erreur lecture latest advice: " + e.getMessage());
        }
        return list;
    }

    /** Une ligne conseils_ia par id (pour page partagée / QR). */
    public Map<String, Object> getConseilById(int idConseil) {
        try {
            PreparedStatement pst = MyConnection.getInstance().getCnx().prepareStatement(
                    "SELECT ci.*, p.nom AS produit_nom FROM conseils_ia ci " +
                            "LEFT JOIN produit p ON p.id_produit = CAST(ci.id_produit AS UNSIGNED) " +
                            "WHERE ci.id_conseil=?");
            pst.setInt(1, idConseil);
            ResultSet rs = pst.executeQuery();
            if (rs.next()) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("id", rs.getInt("id_conseil"));
                String idProdStr = rs.getString("id_produit");
                int idProdInt = 0;
                if (idProdStr != null && !idProdStr.isBlank()) {
                    try { idProdInt = Integer.parseInt(idProdStr.trim()); } catch (NumberFormatException ignored) {}
                }
                row.put("id_produit", idProdInt);
                String nomProduit = rs.getString("produit_nom");
                row.put("produit_nom", (nomProduit != null && !nomProduit.isBlank()) ? nomProduit : null);
                row.put("type", rs.getString("type"));
                row.put("description", rs.getString("description"));
                row.put("urgence", rs.getString("urgence"));
                row.put("score", rs.getInt("score"));
                row.put("etat", rs.getString("etat"));
                return row;
            }
        } catch (SQLException e) {
            System.err.println("Erreur getConseilById: " + e.getMessage());
        }
        return null;
    }

    public void accepter(int idConseil) {
        try {
            String sql = "UPDATE conseils_ia SET etat='ACCEPTE', date_accepte=NOW() WHERE id_conseil=?";
            PreparedStatement pst = MyConnection.getInstance().getCnx().prepareStatement(sql);
            pst.setInt(1, idConseil);
            pst.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Erreur accepter: " + e.getMessage());
        }
    }

    public void ignorer(int idConseil) {
        try {
            String sql = "UPDATE conseils_ia SET etat='IGNORE', date_ignore=NOW() WHERE id_conseil=?";
            PreparedStatement pst = MyConnection.getInstance().getCnx().prepareStatement(sql);
            pst.setInt(1, idConseil);
            pst.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Erreur ignorer: " + e.getMessage());
        }
    }

    public int compter(String etat) {
        try {
            String sql = (etat == null || etat.equalsIgnoreCase("Tous"))
                    ? "SELECT COUNT(*) FROM conseils_ia"
                    : "SELECT COUNT(*) FROM conseils_ia WHERE etat=?";
            PreparedStatement pst = MyConnection.getInstance().getCnx().prepareStatement(sql);
            if (etat != null && !etat.equalsIgnoreCase("Tous"))
                pst.setString(1, etat);
            ResultSet rs = pst.executeQuery();
            if (rs.next())
                return rs.getInt(1);
        } catch (SQLException e) {
            System.err.println("Erreur compter: " + e.getMessage());
        }
        return 0;
    }

    public void accepterAvecConfig(int idConseil, String dateDebut, String dateFin, int remise) {
        try {
            Connection c = MyConnection.getInstance().getCnx();
            c.setAutoCommit(false);

            String updateSql = "UPDATE conseils_ia SET etat='ACCEPTE', date_accepte=NOW() WHERE id_conseil=?";
            String selectSql = "SELECT id_produit, type FROM conseils_ia WHERE id_conseil=?";

            try (PreparedStatement upd = c.prepareStatement(updateSql);
                 PreparedStatement sel = c.prepareStatement(selectSql)) {

                upd.setInt(1, idConseil);
                upd.executeUpdate();

                sel.setInt(1, idConseil);
                ResultSet rs = sel.executeQuery();
                if (rs.next()) {
                    int idProd = rs.getInt("id_produit");
                    String type = rs.getString("type");
                    java.sql.Date dStart = java.sql.Date
                            .valueOf(java.time.LocalDate.parse(dateDebut.trim()));
                    java.sql.Date dEnd = java.sql.Date
                            .valueOf(java.time.LocalDate.parse(dateFin.trim()));

                    String ins = "INSERT INTO campagne_marketing (vendor_id, nom, type_action, objectif, statut, date_debut, date_fin, budget_alloue, budget_depense, score_ia) "
                            + "VALUES (1, ?, ?, ?, ?, ?, ?, 0, 0, ?)";
                    String nomCamp = "Promo IA -" + remise + "% (" + (type != null ? type : "Promotion") + ")";
                    try (PreparedStatement insP = c.prepareStatement(ins)) {
                        insP.setString(1, nomCamp);
                        insP.setString(2, "PROMOTION");
                        insP.setString(3, "Remise " + remise + "% sur produit #" + idProd);
                        insP.setString(4, "ACTIVE");
                        insP.setDate(5, dStart);
                        insP.setDate(6, dEnd);
                        insP.setDouble(7, remise);
                        insP.executeUpdate();
                    } catch (SQLException insertEx) {
                        String insLite = "INSERT INTO campagne_marketing (nom, type_action, canal, budget_alloue, budget_depense, date_debut, date_fin, statut, score_ia, vendor_id) "
                                + "VALUES (?, 'PROMOTION', 'IA', 0, 0, ?, ?, 'ACTIVE', ?, 1)";
                        try (PreparedStatement insP2 = c.prepareStatement(insLite)) {
                            insP2.setString(1, nomCamp + " #" + idProd);
                            insP2.setDate(2, dStart);
                            insP2.setDate(3, dEnd);
                            insP2.setDouble(4, remise);
                            insP2.executeUpdate();
                        }
                    }
                }
                c.commit();
            } catch (Exception ex) {
                c.rollback();
                throw new RuntimeException(ex);
            } finally {
                c.setAutoCommit(true);
            }
        } catch (Exception e) {
            System.err.println("Erreur accepterAvecConfig: " + e.getMessage());
        }
    }

    /** Nom affiché pour une carte conseil (évite Produit #0 si jointure absente). */
    public String getNomProduit(int idProduit) {
        if (idProduit <= 0)
            return null;
        try {
            String sql = "SELECT nom FROM produit WHERE id_produit = ?";
            PreparedStatement pst = MyConnection.getInstance().getCnx().prepareStatement(sql);
            pst.setInt(1, idProduit);
            ResultSet rs = pst.executeQuery();
            if (rs.next()) {
                String n = rs.getString("nom");
                return n != null && !n.isBlank() ? n : null;
            }
        } catch (SQLException e) {
            System.err.println("Erreur getNomProduit: " + e.getMessage());
        }
        return null;
    }

    public List<Map<String, String>> getProduits() {
        List<Map<String, String>> list = new ArrayList<>();
        try {
            Connection c = MyConnection.getInstance().getCnx();
            String sql = "SELECT id_produit, nom FROM produit";
            Statement st = c.createStatement();
            ResultSet rs = st.executeQuery(sql);
            while (rs.next()) {
                Map<String, String> m = new HashMap<>();
                m.put("id", String.valueOf(rs.getInt("id_produit")));
                m.put("nom", rs.getString("nom"));
                list.add(m);
            }
        } catch (SQLException e) {
            System.err.println("Erreur lecture produits: " + e.getMessage());
        }
        return list;
    }

    public void destocker(int idProduit) {
        try {
            Connection c = MyConnection.getInstance().getCnx();
            String sql = "UPDATE produit SET quantite_stock = 0 WHERE id_produit = ?";
            PreparedStatement pst = c.prepareStatement(sql);
            pst.setInt(1, idProduit);
            pst.executeUpdate();
            System.out.println("Produit destocke (stock=0), id=" + idProduit);
        } catch (SQLException e) {
            System.err.println("Erreur destockage: " + e.getMessage());
        }
    }
}