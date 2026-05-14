package projet.hanouti.module4.dao;
import projet.hanouti.module4.model.Panier;
import projet.hanouti.module4.util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PanierDAO {

    // Always fetch a fresh connection reference — never cache it as a field.
    // The singleton DBConnection auto-reconnects if the connection dropped.
    private Connection getConn() throws SQLException {
        return DBConnection.getInstance().getConnection();
    }

    // ADD item to cart — upsert: if the product already exists for this user,
    // increment its quantity instead of inserting a duplicate row.
    public void addToCart(Panier p) throws SQLException {
        // 1. Check whether this product is already in the active cart
        String checkSql = "SELECT panier_id, quantite FROM panier "
                + "WHERE user_id = ? AND produit_id = ? AND statut = 'actif'";
        try (PreparedStatement check = getConn().prepareStatement(checkSql)) {
            check.setInt(1, p.getUserId());
            check.setInt(2, p.getProduitId());
            try (ResultSet rs = check.executeQuery()) {
                if (rs.next()) {
                    // Row already exists — just increment its quantity
                    int existingId  = rs.getInt("panier_id");
                    int newQuantite = rs.getInt("quantite") + p.getQuantite();
                    String updateSql = "UPDATE panier SET quantite = ? WHERE panier_id = ?";
                    try (PreparedStatement upd = getConn().prepareStatement(updateSql)) {
                        upd.setInt(1, newQuantite);
                        upd.setInt(2, existingId);
                        upd.executeUpdate();
                    }
                    return;
                }
            }
        }
        // 2. Not found — insert a fresh row
        String sql = "INSERT INTO panier (user_id, produit_id, quantite, prix_unitaire, statut) "
                + "VALUES (?, ?, ?, ?, 'actif')";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setInt(1, p.getUserId());
            ps.setInt(2, p.getProduitId());
            ps.setInt(3, p.getQuantite());
            ps.setDouble(4, p.getPrixUnitaire());
            ps.executeUpdate();
        }
    }

    // GET all cart items for a user
    public List<Panier> getCartItems(int userId) throws SQLException {
        List<Panier> list = new ArrayList<>();
        String sql = "SELECT pa.*, pr.nom as nom_produit "
                + "FROM panier pa "
                + "JOIN produits pr ON pa.produit_id = pr.produit_id "
                + "WHERE pa.user_id = ? AND pa.statut = 'actif'";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Panier p = new Panier(
                            rs.getInt("user_id"),
                            rs.getInt("produit_id"),
                            rs.getInt("quantite"),
                            rs.getDouble("prix_unitaire")
                    );
                    p.setPanierId(rs.getInt("panier_id"));
                    p.setNomProduit(rs.getString("nom_produit"));
                    list.add(p);
                }
            }
        }
        return list;
    }

    // GET available stock for a product — tries multiple common column names
    public int getStockDisponible(int produitId) throws SQLException {
        // Try each common column name until one works
        String[] candidates = {"quantite_stock", "stock", "stock_disponible", "quantite", "qte_stock"};
        for (String col : candidates) {
            try {
                String sql = "SELECT " + col + " FROM produits WHERE produit_id = ?";
                try (PreparedStatement ps = getConn().prepareStatement(sql)) {
                    ps.setInt(1, produitId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) return rs.getInt(col);
                    }
                }
            } catch (SQLException ignored) {
                // Column name didn't match — try next candidate
            }
        }
        return 0; // Unknown stock — caller will allow increment
    }

    // UPDATE quantity
    public void updateQuantite(int panierId, int newQuantite) throws SQLException {
        String sql = "UPDATE panier SET quantite = ? WHERE panier_id = ?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setInt(1, newQuantite);
            ps.setInt(2, panierId);
            ps.executeUpdate();
        }
    }

    // DELETE item from cart
    public void removeFromCart(int panierId) throws SQLException {
        String sql = "DELETE FROM panier WHERE panier_id = ?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setInt(1, panierId);
            ps.executeUpdate();
        }
    }

    // CLEAR entire cart after payment
    public void clearCart(int userId) throws SQLException {
        String sql = "UPDATE panier SET statut = 'commandé' WHERE user_id = ?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.executeUpdate();
        }
    }

    // CONSOLIDATE — merge duplicate (user_id, produit_id) rows into one.
    // Called at the start of every cart load so existing DB duplicates are
    // automatically fixed, regardless of how they got there.
    public void consolidateDuplicates(int userId) throws SQLException {
        String findDups =
                "SELECT produit_id, SUM(quantite) AS total_qty, MIN(panier_id) AS keep_id " +
                        "FROM panier WHERE user_id = ? AND statut = 'actif' " +
                        "GROUP BY produit_id HAVING COUNT(*) > 1";
        try (PreparedStatement ps = getConn().prepareStatement(findDups)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int produitId = rs.getInt("produit_id");
                    int totalQty  = rs.getInt("total_qty");
                    int keepId    = rs.getInt("keep_id");
                    // Set the surviving row's quantity to the combined total
                    try (PreparedStatement upd = getConn().prepareStatement(
                            "UPDATE panier SET quantite = ? WHERE panier_id = ?")) {
                        upd.setInt(1, totalQty);
                        upd.setInt(2, keepId);
                        upd.executeUpdate();
                    }
                    // Delete every other row for the same user + product
                    try (PreparedStatement del = getConn().prepareStatement(
                            "DELETE FROM panier WHERE user_id = ? AND produit_id = ? " +
                                    "AND statut = 'actif' AND panier_id != ?")) {
                        del.setInt(1, userId);
                        del.setInt(2, produitId);
                        del.setInt(3, keepId);
                        del.executeUpdate();
                    }
                }
            }
        }
    }

    // SEARCH similar products by keyword in name (excludes the given produitId)
    public List<Panier> searchSimilarProducts(String keyword, int excludeProduitId) throws SQLException {
        List<Panier> list = new ArrayList<>();
        String[] words = keyword.trim().split("\\s+");
        String searchWord = words.length > 0 ? words[0] : keyword;
        String sql = "SELECT pr.produit_id, pr.nom AS nom_produit, pr.prix AS prix_unitaire " +
                "FROM produits pr " +
                "WHERE pr.nom LIKE ? AND pr.produit_id != ? " +
                "ORDER BY pr.nom ASC LIMIT 10";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, "%" + searchWord + "%");
            ps.setInt(2, excludeProduitId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Panier p = new Panier(0, rs.getInt("produit_id"), 1, rs.getDouble("prix_unitaire"));
                    p.setNomProduit(rs.getString("nom_produit"));
                    list.add(p);
                }
            }
        }
        return list;
    }
}