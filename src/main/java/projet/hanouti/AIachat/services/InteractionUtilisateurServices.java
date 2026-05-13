package projet.hanouti.AIachat.services;

import projet.hanouti.AIachat.entities.InteractionUtilisateur;
import projet.hanouti.common.utils.MyBD;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InteractionUtilisateurServices {
    public static final String TYPE_VIEW             = "VIEW";
    public static final String TYPE_CLICK_PRODUCT    = "CLICK_PRODUCT";
    public static final String TYPE_ADD_TO_CART      = "ADD_TO_CART";
    public static final String TYPE_BOUGHT           = "BOUGHT";
    public static final String TYPE_ADD_TO_WISHLIST  = "ADD_TO_WISHLIST";

    private Connection getConnection() {
        return MyBD.getInstance().getConnection();
    }

    // ── Public log methods (unchanged signatures) ─────────────────────────────

    public void logView(int idAcheteur, int idProduit) {
        logInteraction(idAcheteur, idProduit, TYPE_VIEW);
    }

    public void logProductClick(int idAcheteur, int idProduit) {
        logInteraction(idAcheteur, idProduit, TYPE_CLICK_PRODUCT);
    }

    public void logAddToCart(int idAcheteur, int idProduit) {
        logInteraction(idAcheteur, idProduit, TYPE_ADD_TO_CART);
    }

    public void logBought(int idAcheteur, int idProduit) {
        logInteraction(idAcheteur, idProduit, TYPE_BOUGHT);
    }

    public void logAddToWishlist(int idAcheteur, int idProduit) {
        logInteraction(idAcheteur, idProduit, TYPE_ADD_TO_WISHLIST);
    }

    public void logInteraction(int idAcheteur, int idProduit, String type) {
        upsertInteraction(idAcheteur, idProduit, type);
    }

    // ── UPSERT: if row exists → nb+1 + refresh last_interaction
    //           if not        → insert new row with nb=1          ────────────

    private void upsertInteraction(int idAcheteur, int idProduit, String type) {
        if (idAcheteur <= 0 || idProduit <= 0 || type == null || type.trim().isEmpty()) {
            System.out.println("[WARN] InteractionUtilisateurServices.upsertInteraction: invalid params ignored");
            return;
        }

        // ON DUPLICATE KEY relies on the UNIQUE KEY uq_interaction(id_acheteur, id_produit, type_interaction)
        String query =
                "INSERT INTO Interaction_Utilisateur (id_acheteur, id_produit, type_interaction, nb_interaction, last_interaction) " +
                        "VALUES (?, ?, ?, 1, ?) " +
                        "ON DUPLICATE KEY UPDATE " +
                        "nb_interaction = nb_interaction + 1, " +
                        "last_interaction = ?";

        try (PreparedStatement pst = getConnection().prepareStatement(query)) {
            Timestamp now = Timestamp.valueOf(LocalDateTime.now());
            pst.setInt(1, idAcheteur);
            pst.setInt(2, idProduit);
            pst.setString(3, type);
            pst.setTimestamp(4, now);
            pst.setTimestamp(5, now);
            pst.executeUpdate();
        } catch (SQLException | RuntimeException e) {
            System.out.println("[ERROR] InteractionUtilisateurServices.upsertInteraction: " + e.getMessage());
        }
    }

    // ── READ ──────────────────────────────────────────────────────────────────

    public List<InteractionUtilisateur> getByAcheteur(int idAcheteur) {
        List<InteractionUtilisateur> list = new ArrayList<>();
        String query = "SELECT * FROM Interaction_Utilisateur WHERE id_acheteur = ? ORDER BY id_interaction DESC";

        try (PreparedStatement pst = getConnection().prepareStatement(query)) {
            pst.setInt(1, idAcheteur);
            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    Timestamp ts = rs.getTimestamp("last_interaction");
                    list.add(new InteractionUtilisateur(
                            rs.getInt("id_interaction"),
                            rs.getInt("id_acheteur"),
                            rs.getInt("id_produit"),
                            rs.getString("type_interaction"),
                            rs.getInt("nb_interaction"),
                            ts != null ? ts.toLocalDateTime() : null
                    ));
                }
            }
        } catch (SQLException | RuntimeException e) {
            System.out.println("[ERROR] InteractionUtilisateurServices.getByAcheteur: " + e.getMessage());
        }
        return list;
    }

    // ── COUNT: reads nb_interaction directly instead of COUNT(*) ─────────────

    public int countViews(int idAcheteur, int idProduit) {
        return countByType(idAcheteur, idProduit, TYPE_VIEW);
    }

    public int countProductClicks(int idAcheteur, int idProduit) {
        return countByType(idAcheteur, idProduit, TYPE_CLICK_PRODUCT);
    }

    public int countAddToCart(int idAcheteur, int idProduit) {
        return countByType(idAcheteur, idProduit, TYPE_ADD_TO_CART);
    }

    public int countBought(int idAcheteur, int idProduit) {
        return countByType(idAcheteur, idProduit, TYPE_BOUGHT);
    }

    private int countByType(int idAcheteur, int idProduit, String type) {
        String query =
                "SELECT nb_interaction FROM Interaction_Utilisateur " +
                        "WHERE id_acheteur = ? AND id_produit = ? AND type_interaction = ?";

        try (PreparedStatement pst = getConnection().prepareStatement(query)) {
            pst.setInt(1, idAcheteur);
            pst.setInt(2, idProduit);
            pst.setString(3, type);
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) return rs.getInt("nb_interaction");
            }
        } catch (SQLException | RuntimeException e) {
            System.out.println("[ERROR] InteractionUtilisateurServices.countByType: " + e.getMessage());
        }
        return 0;
    }

    /**
     * Batch retrieval of all interactions for a user.
     * Returns Map<ProductId, Map<InteractionType, Count>>
     */
    public Map<Integer, Map<String, Integer>> getInteractionsMap(int idAcheteur) {
        Map<Integer, Map<String, Integer>> masterMap = new HashMap<>();
        if (idAcheteur <= 0) return masterMap;

        String query = "SELECT id_produit, type_interaction, nb_interaction FROM Interaction_Utilisateur WHERE id_acheteur = ?";
        try (PreparedStatement pst = getConnection().prepareStatement(query)) {
            pst.setInt(1, idAcheteur);
            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    int pid = rs.getInt("id_produit");
                    String type = rs.getString("type_interaction");
                    int nb = rs.getInt("nb_interaction");
                    masterMap.computeIfAbsent(pid, k -> new HashMap<>()).put(type, nb);
                }
            }
        } catch (SQLException | RuntimeException e) {
            System.out.println("[ERROR] InteractionUtilisateurServices.getInteractionsMap: " + e.getMessage());
        }
        return masterMap;
    }

    // ── DELETE: auto-purge rows older than 7 days for a given user ────────────

    public void deleteOlderThanOneWeek(int idAcheteur) {
        // Uses getByAcheteur() to find candidates first (so Read is part of the flow)
        List<InteractionUtilisateur> all = getByAcheteur(idAcheteur);
        if (all.isEmpty()) return;

        LocalDateTime cutoff = LocalDateTime.now().minusDays(7);
        int deleted = 0;

        String query = "DELETE FROM Interaction_Utilisateur WHERE id_interaction = ?";
        try (PreparedStatement pst = getConnection().prepareStatement(query)) {
            for (InteractionUtilisateur i : all) {
                if (i.getLastInteraction() != null && i.getLastInteraction().isBefore(cutoff)) {
                    pst.setInt(1, i.getIdInteraction());
                    pst.addBatch();
                    deleted++;
                }
            }
            if (deleted > 0) {
                pst.executeBatch();
                System.out.println("[INFO] InteractionUtilisateurServices.deleteOlderThanOneWeek: deleted "
                        + deleted + " stale interaction(s) for acheteur " + idAcheteur);
            }
        } catch (SQLException | RuntimeException e) {
            System.out.println("[ERROR] InteractionUtilisateurServices.deleteOlderThanOneWeek: " + e.getMessage());
        }
    }
}

