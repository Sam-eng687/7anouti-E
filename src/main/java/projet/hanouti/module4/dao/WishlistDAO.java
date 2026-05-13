package com.hanouti.hanoutiem4.dao;

import com.hanouti.hanoutiem4.model.Wishlist;
import com.hanouti.hanoutiem4.util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO Wishlist — toutes les opérations SQL sur la table wishlist.
 *
 * FIX BUG-05 : tous les PreparedStatement sont maintenant dans des blocs
 *              try-with-resources pour éviter les fuites mémoire/connexion.
 *
 * Note JOIN : la table produits s'appelle bien "produits" (avec 's') dans
 * ce projet — à vérifier si un collègue l'a nommée différemment.
 */
public class WishlistDAO {

    private final Connection conn;

    public WishlistDAO() throws SQLException {
        conn = DBConnection.getInstance().getConnection();
    }

    // ──────────────────────────────────────────────────────
    // ADD — Ajouter un produit à la wishlist
    // INSERT IGNORE pour éviter les doublons (UNIQUE KEY)
    // ──────────────────────────────────────────────────────
    public void addToWishlist(int userId, int produitId) throws SQLException {
        String sql = "INSERT IGNORE INTO wishlist (user_id, produit_id) VALUES (?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {   // FIX BUG-05
            ps.setInt(1, userId);
            ps.setInt(2, produitId);
            ps.executeUpdate();
        }
    }

    // ──────────────────────────────────────────────────────
    // REMOVE — Supprimer un produit de la wishlist
    // ──────────────────────────────────────────────────────
    public void removeFromWishlist(int userId, int produitId) throws SQLException {
        String sql = "DELETE FROM wishlist WHERE user_id = ? AND produit_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {   // FIX BUG-05
            ps.setInt(1, userId);
            ps.setInt(2, produitId);
            ps.executeUpdate();
        }
    }

    // ──────────────────────────────────────────────────────
    // TOGGLE — Clic sur le cœur : ajoute si absent, supprime si présent
    // Retourne TRUE si le produit est maintenant dans la wishlist
    // ──────────────────────────────────────────────────────
    public boolean toggleWishlist(int userId, int produitId) throws SQLException {
        if (isInWishlist(userId, produitId)) {
            removeFromWishlist(userId, produitId);
            return false;
        } else {
            addToWishlist(userId, produitId);
            return true;
        }
    }

    // ──────────────────────────────────────────────────────
    // CHECK — Est-ce que ce produit est déjà dans la wishlist ?
    // ──────────────────────────────────────────────────────
    public boolean isInWishlist(int userId, int produitId) throws SQLException {
        String sql = "SELECT 1 FROM wishlist WHERE user_id = ? AND produit_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {   // FIX BUG-05
            ps.setInt(1, userId);
            ps.setInt(2, produitId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    // ──────────────────────────────────────────────────────
    // GET ALL — Récupérer tous les articles wishlist d'un user
    // Fait un JOIN avec produits pour afficher nom + prix
    // ──────────────────────────────────────────────────────
    public List<Wishlist> getWishlistByUser(int userId) throws SQLException {
        List<Wishlist> list = new ArrayList<>();
        String sql = "SELECT w.wishlist_id, w.user_id, w.produit_id, w.date_ajout, "
                + "p.nom AS nom_produit, p.prix AS prix_produit "
                + "FROM wishlist w "
                + "JOIN produits p ON w.produit_id = p.produit_id "
                + "WHERE w.user_id = ? "
                + "ORDER BY w.date_ajout DESC";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {   // FIX BUG-05
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Wishlist w = new Wishlist(rs.getInt("user_id"), rs.getInt("produit_id"));
                    w.setWishlistId(rs.getInt("wishlist_id"));
                    w.setDateAjout(rs.getDate("date_ajout"));
                    w.setNomProduit(rs.getString("nom_produit"));
                    w.setPrixProduit(rs.getDouble("prix_produit"));
                    list.add(w);
                }
            }
        }
        return list;
    }

    // ──────────────────────────────────────────────────────
    // MOVE TO CART — Déplacer un article de la wishlist vers le panier
    // ──────────────────────────────────────────────────────
    public void moveToCart(int userId, int produitId, double prix) throws SQLException {
        String insertPanier = "INSERT INTO panier (user_id, produit_id, quantite, prix_unitaire, statut) "
                + "VALUES (?, ?, 1, ?, 'actif') "
                + "ON DUPLICATE KEY UPDATE quantite = quantite + 1";
        try (PreparedStatement ps = conn.prepareStatement(insertPanier)) {  // FIX BUG-05
            ps.setInt(1, userId);
            ps.setInt(2, produitId);
            ps.setDouble(3, prix);
            ps.executeUpdate();
        }
        removeFromWishlist(userId, produitId);
    }

    // CLEAR WISHLIST — Vider toute la wishlist d'un utilisateur
    public void clearWishlist(int userId) throws java.sql.SQLException {
        String sql = "DELETE FROM wishlist WHERE user_id = ?";
        try (java.sql.PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.executeUpdate();
        }
    }
}