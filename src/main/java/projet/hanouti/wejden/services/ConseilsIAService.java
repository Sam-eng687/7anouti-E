package projet.hanouti.wejden.services;

import projet.hanouti.common.utils.MyBD;

import java.sql.*;
import java.util.*;

public class ConseilsIAService {

    // Lit les conseils depuis ta vraie table conseils_ia SANS JOIN pour eviter les
    // erreurs
    public List<Map<String, Object>> getConseils() {
        List<Map<String, Object>> list = new ArrayList<>();
        try {
            Connection c = MyBD.getInstance().getConnection();
            // JOIN avec produit pour recuperer le vrai nom du produit directement
            String sql = "SELECT ci.*, p.nom AS produit_nom FROM conseils_ia ci " +
                         "LEFT JOIN produit p ON p.id_produit = CAST(ci.id_produit AS UNSIGNED) " +
                         "ORDER BY ci.id_conseil DESC";
            Statement st = c.createStatement();
            ResultSet rs = st.executeQuery(sql);

            while (rs.next()) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("id", rs.getInt("id_conseil"));
                // id_produit est VARCHAR dans la table
                String idProdStr = rs.getString("id_produit");
                int idProdInt = 0;
                if (idProdStr != null && !idProdStr.isBlank()) {
                    try { idProdInt = Integer.parseInt(idProdStr.trim()); } catch (NumberFormatException ignored) {}
                }
                row.put("id_produit", idProdInt);
                // Nom du produit depuis le JOIN
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
            Connection c = MyBD.getInstance().getConnection();
            // JOIN avec produit pour recuperer le nom
            String sql = "SELECT ci.*, p.nom AS produit_nom FROM conseils_ia ci " +
                         "LEFT JOIN produit p ON p.id_produit = CAST(ci.id_produit AS UNSIGNED) " +
                         "ORDER BY (ci.etat='EN_ATTENTE') DESC, ci.id_conseil DESC LIMIT ?";
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
            PreparedStatement pst = MyBD.getInstance().getConnection().prepareStatement(
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
            Connection c = MyBD.getInstance().getConnection();
            c.setAutoCommit(false); // Transaction pour garantir la cohérence

            // 1. Récupérer les informations du conseil avant mise à jour
            String selectSql = "SELECT type, id_produit FROM conseils_ia WHERE id_conseil = ?";
            String type = "";
            String idProdStr = "";

            try (PreparedStatement selPst = c.prepareStatement(selectSql)) {
                selPst.setInt(1, idConseil);
                ResultSet rs = selPst.executeQuery();
                if (rs.next()) {
                    type = rs.getString("type");
                    idProdStr = rs.getString("id_produit");
                }
            }

            // 2. Mettre à jour le conseil (Etat, Dates)
            // On met à jour date_genere, date_accepte et date_expiration (+7 jours)
            String updateConseilSql = "UPDATE conseils_ia SET " +
                    "etat = 'ACCEPTE', " +
                    "date_genere = NOW(), " +
                    "date_accepte = NOW(), " +
                    "date_expiration = DATE_ADD(NOW(), INTERVAL 7 DAY) " +
                    "WHERE id_conseil = ?";
            
            try (PreparedStatement updPst = c.prepareStatement(updateConseilSql)) {
                updPst.setInt(1, idConseil);
                updPst.executeUpdate();
            }

            // 3. Si c'est un DESTOCKAGE, on vide le stock du produit lié
            if (type != null && type.equalsIgnoreCase("Destockage")) {
                String updateStockSql = "UPDATE produit SET quantite_stock = 0 WHERE id_produit = ?";
                try (PreparedStatement stockPst = c.prepareStatement(updateStockSql)) {
                    stockPst.setString(1, idProdStr);
                    stockPst.executeUpdate();
                }
            }

            c.commit();
            c.setAutoCommit(true);
            System.out.println("LOG IA: Conseil #" + idConseil + " (" + type + ") accepté avec succès.");

        } catch (SQLException e) {
            System.err.println("Erreur lors de l'acceptation: " + e.getMessage());
        }
    }

    public void ignorer(int idConseil) {
        try {
            String sql = "UPDATE conseils_ia SET etat='IGNORE', date_ignore=NOW() WHERE id_conseil=?";
            PreparedStatement pst = MyBD.getInstance().getConnection().prepareStatement(sql);
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
            PreparedStatement pst = MyBD.getInstance().getConnection().prepareStatement(sql);
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
            Connection c = MyBD.getInstance().getConnection();
            c.setAutoCommit(false);

            // 1. Mettre à jour le conseil avec les dates et la remise choisie
            String updateSql = "UPDATE conseils_ia SET " +
                    "etat='ACCEPTE', " +
                    "discount=?, " +
                    "date_genere=NOW(), " +
                    "date_accepte=NOW(), " +
                    "date_expiration=? " + // La date de fin choisie dans le dialogue
                    "WHERE id_conseil=?";
            
            String selectSql = "SELECT id_produit, type FROM conseils_ia WHERE id_conseil=?";

            try (PreparedStatement upd = c.prepareStatement(updateSql);
                    PreparedStatement sel = c.prepareStatement(selectSql)) {

                upd.setDouble(1, remise);
                upd.setString(2, dateFin); // dateFin est au format "YYYY-MM-DD"
                upd.setInt(3, idConseil);
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

                    // Même schéma que CampagneMarketingService.addEntity ; repli sans colonne objectif si besoin
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

    /** Nom affiché pour une carte conseil (évite « Produit #0 » si jointure absente). */
    public String getNomProduit(int idProduit) {
        if (idProduit <= 0)
            return null;
        try {
            String sql = "SELECT nom FROM produit WHERE id_produit = ?";
            PreparedStatement pst = MyBD.getInstance().getConnection().prepareStatement(sql);
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
            Connection c = MyBD.getInstance().getConnection();
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
            Connection c = MyBD.getInstance().getConnection();
            String sql = "UPDATE produit SET quantite_stock = 0 WHERE id_produit = ?";
            PreparedStatement pst = c.prepareStatement(sql);
            pst.setInt(1, idProduit);
            pst.executeUpdate();
            System.out.println("Produit destocke (stock=0), id=" + idProduit);
        } catch (SQLException e) {
            System.err.println("Erreur destockage: " + e.getMessage());
        }
    }

    /**
     * Démarre le générateur automatique de conseils IA basé sur les interactions utilisateurs.
     * Le générateur analyse les données toutes les 30 minutes par défaut.
     */
    public void startAutoGeneration() {
        AutoConseilGeneratorService.getInstance().start();
    }

    /**
     * Arrête le générateur automatique de conseils IA
     */
    public void stopAutoGeneration() {
        AutoConseilGeneratorService.getInstance().stop();
    }

    /**
     * Génère immédiatement des conseils (sans attendre le prochain cycle)
     */
    public void generateConseilsNow() {
        AutoConseilGeneratorService.getInstance().generateNow();
    }

    /**
     * Vérifie si le générateur automatique est en cours d'exécution
     */
    public boolean isAutoGenerationRunning() {
        return AutoConseilGeneratorService.getInstance().isRunning();
    }

    /**
     * Configure l'intervalle de génération automatique (en minutes)
     */
    public void setAutoGenerationInterval(int minutes) {
        AutoConseilGeneratorService.getInstance().setIntervalMinutes(minutes);
    }

    /**
     * Récupère l'intervalle de génération automatique (en minutes)
     */
    public int getAutoGenerationInterval() {
        return AutoConseilGeneratorService.getInstance().getIntervalMinutes();
    }
}
