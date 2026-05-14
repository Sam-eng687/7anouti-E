package projet.hanouti.AIachat.services;

import projet.hanouti.common.utils.MyBD;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class WishlistService {

    private Connection getConnection() {
        return MyBD.getInstance().getConnection();
    }

    // ──
    // CREATE ────────────────────────────────────────────────────────────────

    /**
     * Adds a product to the user's wishlist.
     * INSERT IGNORE silently skips if already exists (UNIQUE KEY uq_user_produit).
     * Returns true if a new row was inserted, false if it already existed.
     */
    public boolean add(int userId, int produitId) {
        if (userId <= 0 || produitId <= 0) {
            System.out.println("[WARN] WishlistService.add: invalid params ignored");
            return false;
        }
        String query = "INSERT IGNORE INTO wishlist (user_id, produit_id) VALUES (?, ?)";
        try (PreparedStatement pst = getConnection().prepareStatement(query)) {
            pst.setInt(1, userId);
            pst.setInt(2, produitId);
            int rows = pst.executeUpdate();
            return rows > 0;
        } catch (SQLException | RuntimeException e) {
            System.out.println("[ERROR] WishlistService.add: " + e.getMessage());
            return false;
        }
    }

    // ── READ ──────────────────────────────────────────────────────────────────

    /**
     * Returns all produit ids in the wishlist for this user.
     * Used on startup to preload the wishlist set in memory.
     */
    public List<Integer> getByUser(int userId) {
        List<Integer> ids = new ArrayList<>();
        if (userId <= 0) return ids;
        String query = "SELECT produit_id FROM wishlist WHERE user_id = ? ORDER BY date_ajout DESC";
        try (PreparedStatement pst = getConnection().prepareStatement(query)) {
            pst.setInt(1, userId);
            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) ids.add(rs.getInt("produit_id"));
            }
        } catch (SQLException | RuntimeException e) {
            System.out.println("[ERROR] WishlistService.getByUser: " + e.getMessage());
        }
        return ids;
    }

    /**
     * Checks if a specific product is already in the user's wishlist.
     * Used to determine heart button color.
     */
    public boolean isInWishlist(int userId, int produitId) {
        if (userId <= 0 || produitId <= 0) return false;
        String query = "SELECT 1 FROM wishlist WHERE user_id = ? AND produit_id = ? LIMIT 1";
        try (PreparedStatement pst = getConnection().prepareStatement(query)) {
            pst.setInt(1, userId);
            pst.setInt(2, produitId);
            try (ResultSet rs = pst.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException | RuntimeException e) {
            System.out.println("[ERROR] WishlistService.isInWishlist: " + e.getMessage());
            return false;
        }
    }

    // ── DELETE ────────────────────────────────────────────────────────────────

    /**
     * Removes a product from the user's wishlist.
     * Returns true if a row was deleted.
     */
    public boolean remove(int userId, int produitId) {
        if (userId <= 0 || produitId <= 0) {
            System.out.println("[WARN] WishlistService.remove: invalid params ignored");
            return false;
        }
        String query = "DELETE FROM wishlist WHERE user_id = ? AND produit_id = ?";
        try (PreparedStatement pst = getConnection().prepareStatement(query)) {
            pst.setInt(1, userId);
            pst.setInt(2, produitId);
            int rows = pst.executeUpdate();
            return rows > 0;
        } catch (SQLException | RuntimeException e) {
            System.out.println("[ERROR] WishlistService.remove: " + e.getMessage());
            return false;
        }
    }
}

