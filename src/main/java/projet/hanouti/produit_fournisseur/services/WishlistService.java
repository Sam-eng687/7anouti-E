package projet.hanouti.produit_fournisseur.services;

import projet.hanouti.produit_fournisseur.utils.MyConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class WishlistService {

    private Connection getConnection() {
        return MyConnection.getInstance().getCnx();
    }

    public boolean add(int userId, int produitId) {
        if (userId <= 0 || produitId <= 0) {
            System.out.println("[WARN] WishlistService.add: invalid params ignored");
            return false;
        }

        String query = "INSERT IGNORE INTO wishlist (user_id, produit_id) VALUES (?, ?)";
        try (PreparedStatement pst = getConnection().prepareStatement(query)) {
            pst.setInt(1, userId);
            pst.setInt(2, produitId);
            return pst.executeUpdate() > 0;
        } catch (SQLException | RuntimeException e) {
            System.out.println("[ERROR] WishlistService.add: " + e.getMessage());
            return false;
        }
    }

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

    public boolean remove(int userId, int produitId) {
        if (userId <= 0 || produitId <= 0) {
            System.out.println("[WARN] WishlistService.remove: invalid params ignored");
            return false;
        }

        String query = "DELETE FROM wishlist WHERE user_id = ? AND produit_id = ?";
        try (PreparedStatement pst = getConnection().prepareStatement(query)) {
            pst.setInt(1, userId);
            pst.setInt(2, produitId);
            return pst.executeUpdate() > 0;
        } catch (SQLException | RuntimeException e) {
            System.out.println("[ERROR] WishlistService.remove: " + e.getMessage());
            return false;
        }
    }
}
