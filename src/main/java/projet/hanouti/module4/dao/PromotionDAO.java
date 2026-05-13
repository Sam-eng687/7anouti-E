package com.hanouti.hanoutiem4.dao;

import com.hanouti.hanoutiem4.model.Promotion;
import com.hanouti.hanoutiem4.util.DBConnection;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO Promotions — accès à la table `promotions`.
 *
 * SQL de création de la table (à soumettre à l'équipe) :
 *
 *   CREATE TABLE promotions (
 *     promo_id    INT AUTO_INCREMENT PRIMARY KEY,
 *     produit_id  INT NOT NULL,
 *     pourcentage DECIMAL(5,2) NOT NULL,
 *     date_debut  DATETIME NOT NULL,
 *     date_fin    DATETIME,
 *     actif       TINYINT(1) DEFAULT 1,
 *     FOREIGN KEY (produit_id) REFERENCES produit(id_produit)
 *   );
 */
public class PromotionDAO {

    private final Connection conn;

    public PromotionDAO() throws SQLException {
        conn = DBConnection.getInstance().getConnection();
    }

    /**
     * Retourne la promo active pour un produit donné, ou null s'il n'y en a pas.
     * "Active" = actif = 1 ET date_debut <= NOW() ET (date_fin IS NULL OR date_fin > NOW())
     */
    public Promotion getActivePromoForProduct(int produitId) throws SQLException {
        String sql = "SELECT * FROM promotions "
                + "WHERE produit_id = ? AND actif = 1 "
                + "AND date_debut <= NOW() "
                + "AND (date_fin IS NULL OR date_fin > NOW()) "
                + "ORDER BY pourcentage DESC "   // si plusieurs promos, prend la plus avantageuse
                + "LIMIT 1";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, produitId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }
        }
        return null;
    }

    /**
     * Retourne toutes les promos actives (utile pour affichage admin ou badge global).
     */
    public List<Promotion> getAllActivePromos() throws SQLException {
        List<Promotion> list = new ArrayList<>();
        String sql = "SELECT * FROM promotions WHERE actif = 1 "
                + "AND date_debut <= NOW() "
                + "AND (date_fin IS NULL OR date_fin > NOW())";
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) list.add(mapRow(rs));
        }
        return list;
    }

    /**
     * Insère une nouvelle promo.
     */
    public void addPromotion(Promotion promo) throws SQLException {
        String sql = "INSERT INTO promotions (produit_id, pourcentage, date_debut, date_fin, actif) "
                + "VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, promo.getProduitId());
            ps.setDouble(2, promo.getPourcentage());
            ps.setTimestamp(3, Timestamp.valueOf(promo.getDateDebut()));
            ps.setTimestamp(4, promo.getDateFin() != null ? Timestamp.valueOf(promo.getDateFin()) : null);
            ps.setBoolean(5, promo.isActif());
            ps.executeUpdate();
        }
    }

    /**
     * Désactive une promo (soft delete).
     */
    public void desactiverPromotion(int promoId) throws SQLException {
        String sql = "UPDATE promotions SET actif = 0 WHERE promo_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, promoId);
            ps.executeUpdate();
        }
    }

    // ── Helper ────────────────────────────────────────────
    private Promotion mapRow(ResultSet rs) throws SQLException {
        Promotion p = new Promotion();
        p.setPromoId(rs.getInt("promo_id"));
        p.setProduitId(rs.getInt("produit_id"));
        p.setPourcentage(rs.getDouble("pourcentage"));

        Timestamp debut = rs.getTimestamp("date_debut");
        if (debut != null) p.setDateDebut(debut.toLocalDateTime());

        Timestamp fin = rs.getTimestamp("date_fin");
        p.setDateFin(fin != null ? fin.toLocalDateTime() : null);

        p.setActif(rs.getBoolean("actif"));
        return p;
    }
}