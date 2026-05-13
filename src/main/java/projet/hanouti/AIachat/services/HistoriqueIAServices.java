package projet.hanouti.AIachat.services;

import projet.hanouti.AIachat.entities.HistoriqueIA;
import projet.hanouti.common.utils.MyBD;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class HistoriqueIAServices {

    private Connection getConnection() {
        return MyBD.getInstance().getConnection();
    }

    // ── CREATE ────────────────────────────────────────────────────────────────

    public void add(HistoriqueIA historique) {
        if (historique == null || historique.getMotsCles() == null
                || historique.getMotsCles().trim().length() < 3) {
            System.out.println("[WARN] HistoriqueIAServices.add: query too short, skipped");
            return;
        }
        String query = "INSERT INTO Historique_IA (id_acheteur, mots_cles, produit_suggere_id, date_recherche) VALUES (?, ?, ?, ?)";
        try (PreparedStatement pst = getConnection().prepareStatement(query)) {
            pst.setInt(1, historique.getIdAcheteur());
            pst.setString(2, historique.getMotsCles());
            if (historique.getProduitSuggreId() != null) pst.setInt(3, historique.getProduitSuggreId());
            else                                          pst.setNull(3, Types.INTEGER);
            pst.setTimestamp(4, Timestamp.valueOf(LocalDateTime.now()));
            pst.executeUpdate();
        } catch (SQLException | RuntimeException e) {
            System.out.println("[ERROR] HistoriqueIAServices.add: " + e.getMessage());
        }
    }

    // ── READ ──────────────────────────────────────────────────────────────────

    public List<HistoriqueIA> getAll() {
        List<HistoriqueIA> list = new ArrayList<>();
        String query = "SELECT * FROM Historique_IA ORDER BY id_recherche DESC";
        try (PreparedStatement pst = getConnection().prepareStatement(query);
             ResultSet rs = pst.executeQuery()) {
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException | RuntimeException e) {
            System.out.println("[ERROR] HistoriqueIAServices.getAll: " + e.getMessage());
        }
        return list;
    }

    public List<HistoriqueIA> getByAcheteur(int idAcheteur) {
        List<HistoriqueIA> list = new ArrayList<>();
        String query = "SELECT * FROM Historique_IA WHERE id_acheteur = ? ORDER BY id_recherche DESC";
        try (PreparedStatement pst = getConnection().prepareStatement(query)) {
            pst.setInt(1, idAcheteur);
            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        } catch (SQLException | RuntimeException e) {
            System.out.println("[ERROR] HistoriqueIAServices.getByAcheteur: " + e.getMessage());
        }
        return list;
    }

    public List<String> getCategoriesFromHistoryByFrequency(int idAcheteur) {
        List<String> categories = new ArrayList<>();
        String query =
                "SELECT p.categorie, COUNT(*) AS freq " +
                        "FROM Historique_IA h " +
                        "JOIN produit p ON h.produit_suggere_id = p.id_produit " +
                        "WHERE h.id_acheteur = ? " +
                        "AND h.date_recherche >= NOW() - INTERVAL 30 DAY " +
                        "AND h.produit_suggere_id IS NOT NULL " +
                        "GROUP BY p.categorie " +
                        "ORDER BY freq DESC";
        try (PreparedStatement pst = getConnection().prepareStatement(query)) {
            pst.setInt(1, idAcheteur);
            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) categories.add(rs.getString("categorie"));
            }
        } catch (SQLException | RuntimeException e) {
            System.out.println("[ERROR] HistoriqueIAServices.getCategoriesFromHistoryByFrequency: " + e.getMessage());
        }
        return categories;
    }

    public List<String> getCategoriesFromHistory(int idAcheteur) {
        List<String> categories = new ArrayList<>();
        String query =
                "SELECT DISTINCT p.categorie " +
                        "FROM Historique_IA h " +
                        "JOIN produit p ON h.produit_suggere_id = p.id_produit " +
                        "WHERE h.id_acheteur = ? AND h.produit_suggere_id IS NOT NULL";
        try (PreparedStatement pst = getConnection().prepareStatement(query)) {
            pst.setInt(1, idAcheteur);
            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) categories.add(rs.getString("categorie"));
            }
        } catch (SQLException | RuntimeException e) {
            System.out.println("[ERROR] HistoriqueIAServices.getCategoriesFromHistory: " + e.getMessage());
        }
        return categories;
    }

    // ── UPDATE ────────────────────────────────────────────────────────────────

    /**
     * Called silently after any refinement.
     * Updates the most recent row for this user with the new top product id,
     * regardless of whether it was null or not.
     */
    public void updateProduitSuggere(int idAcheteur, int idProduit) {
        List<HistoriqueIA> history = getByAcheteur(idAcheteur);
        if (history.isEmpty()) return;

        // getByAcheteur orders DESC so first entry = most recent row
        int targetId = history.get(0).getIdRecherche();

        String query = "UPDATE Historique_IA SET produit_suggere_id = ? WHERE id_recherche = ?";
        try (PreparedStatement pst = getConnection().prepareStatement(query)) {
            pst.setInt(1, idProduit);
            pst.setInt(2, targetId);
            pst.executeUpdate();
        } catch (SQLException | RuntimeException e) {
            System.out.println("[ERROR] HistoriqueIAServices.updateProduitSuggere: " + e.getMessage());
        }
    }

    // ── DELETE ────────────────────────────────────────────────────────────────

    /**
     * Existing bulk delete - kept as is.
     */
    public void deleteByAcheteur(int idAcheteur) {
        String query = "DELETE FROM Historique_IA WHERE id_acheteur = ?";
        try (PreparedStatement pst = getConnection().prepareStatement(query)) {
            pst.setInt(1, idAcheteur);
            pst.executeUpdate();
        } catch (SQLException | RuntimeException e) {
            System.out.println("[ERROR] HistoriqueIAServices.deleteByAcheteur: " + e.getMessage());
        }
    }

    /**
     * Called before inserting a new successful search row.
     * If the most recent row for this user has produit_suggere_id = NULL
     * (previous search found nothing), delete it - the new search replaces it.
     * Uses getByAcheteur() so Read feeds Delete naturally.
     */
    public void deleteLatestNullRow(int idAcheteur) {
        List<HistoriqueIA> history = getByAcheteur(idAcheteur);
        if (history.isEmpty()) return;

        HistoriqueIA latest = history.get(0); // DESC order → most recent first
        if (latest.getProduitSuggreId() != null) return; // not null → nothing to delete

        String query = "DELETE FROM Historique_IA WHERE id_recherche = ?";
        try (PreparedStatement pst = getConnection().prepareStatement(query)) {
            pst.setInt(1, latest.getIdRecherche());
            pst.executeUpdate();
        } catch (SQLException | RuntimeException e) {
            System.out.println("[ERROR] HistoriqueIAServices.deleteLatestNullRow: " + e.getMessage());
        }
    }

    /**
     * Auto-purge: silently deletes rows older than 7 days on startup.
     * Uses getByAcheteur() so Read is part of the Delete flow.
     */
    public void deleteOlderThanOneWeek(int idAcheteur) {
        List<HistoriqueIA> history = getByAcheteur(idAcheteur);
        if (history.isEmpty()) return;

        LocalDateTime cutoff = LocalDateTime.now().minusDays(7);
        int deleted = 0;

        String query = "DELETE FROM Historique_IA WHERE id_recherche = ?";
        try (PreparedStatement pst = getConnection().prepareStatement(query)) {
            for (HistoriqueIA h : history) {
                if (h.getDateRecherche() != null && h.getDateRecherche().isBefore(cutoff)) {
                    pst.setInt(1, h.getIdRecherche());
                    pst.addBatch();
                    deleted++;
                }
            }
            if (deleted > 0) {
                pst.executeBatch();
            }
        } catch (SQLException | RuntimeException e) {
            System.out.println("[ERROR] HistoriqueIAServices.deleteOlderThanOneWeek: " + e.getMessage());
        }
    }

    // ── PRIVATE ───────────────────────────────────────────────────────────────

    private HistoriqueIA mapRow(ResultSet rs) throws SQLException {
        int     produitId      = rs.getInt("produit_suggere_id");
        Integer produitSuggeId = rs.wasNull() ? null : produitId;
        Timestamp ts           = rs.getTimestamp("date_recherche");
        return new HistoriqueIA(
                rs.getInt("id_recherche"),
                rs.getInt("id_acheteur"),
                rs.getString("mots_cles"),
                produitSuggeId,
                ts != null ? ts.toLocalDateTime() : null
        );
    }
}

