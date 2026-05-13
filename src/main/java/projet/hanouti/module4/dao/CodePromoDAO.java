package com.hanouti.hanoutiem4.dao;

import com.hanouti.hanoutiem4.util.DBConnection;

import java.sql.*;

/**
 * DAO pour la table codes_promo.
 *
 * SQL de création :
 *   CREATE TABLE codes_promo (
 *     code_id          INT AUTO_INCREMENT PRIMARY KEY,
 *     code             VARCHAR(30) NOT NULL UNIQUE,
 *     type_reduction   ENUM('POURCENTAGE','MONTANT_FIXE') NOT NULL DEFAULT 'POURCENTAGE',
 *     valeur           DECIMAL(10,2) NOT NULL,
 *     montant_min      DECIMAL(10,2) DEFAULT 0,
 *     date_debut       DATETIME NOT NULL,
 *     date_fin         DATETIME DEFAULT NULL,
 *     utilisations_max INT DEFAULT NULL,
 *     utilisations     INT DEFAULT 0,
 *     actif            TINYINT(1) DEFAULT 1
 *   );
 */
public class CodePromoDAO {

    private final Connection conn;

    public CodePromoDAO() throws SQLException {
        conn = DBConnection.getInstance().getConnection();
    }

    /**
     * Résultat de la validation d'un code promo.
     */
    public static class ResultatCode {
        public final boolean valide;
        public final String  type;        // "POURCENTAGE" ou "MONTANT_FIXE"
        public final double  valeur;      // montant de la réduction
        public final double  montantMin;
        public final String  message;     // message d'erreur si invalide
        public final int     codeId;

        public ResultatCode(int codeId, String type, double valeur, double montantMin) {
            this.valide      = true;
            this.codeId      = codeId;
            this.type        = type;
            this.valeur      = valeur;
            this.montantMin  = montantMin;
            this.message     = null;
        }

        public ResultatCode(String message) {
            this.valide     = false;
            this.codeId     = -1;
            this.type       = null;
            this.valeur     = 0;
            this.montantMin = 0;
            this.message    = message;
        }

        /**
         * Calcule le montant de la réduction à appliquer sur le panier.
         */
        public double calculerReduction(double montantPanier) {
            if (!valide) return 0;
            if ("POURCENTAGE".equals(type)) {
                return montantPanier * valeur / 100.0;
            } else {
                return Math.min(valeur, montantPanier); // ne pas dépasser le total
            }
        }
    }

    /**
     * Vérifie un code promo et retourne le résultat.
     * Vérifie : existence, actif, dates valides, utilisations restantes.
     * Ne vérifie PAS le montant minimum ici — c'est le controller qui le fait
     * pour pouvoir afficher un message précis à l'utilisateur.
     */
    public ResultatCode validerCode(String code) throws SQLException {
        if (code == null || code.isBlank()) {
            return new ResultatCode("Veuillez saisir un code promo.");
        }

        String sql = "SELECT * FROM codes_promo WHERE code = ? AND actif = 1";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, code.trim().toUpperCase());
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return new ResultatCode("Code promo invalide ou expiré.");
                }

                // Vérifier les dates
                Timestamp debut = rs.getTimestamp("date_debut");
                Timestamp fin   = rs.getTimestamp("date_fin");
                Timestamp now   = new Timestamp(System.currentTimeMillis());

                if (debut != null && now.before(debut)) {
                    return new ResultatCode("Ce code n'est pas encore actif.");
                }
                if (fin != null && now.after(fin)) {
                    return new ResultatCode("Ce code promo a expiré.");
                }

                // Vérifier les utilisations
                // ✅ Fix — capture wasNull() immediately after reading utilisations_max
                int utilisationsMax        = rs.getInt("utilisations_max");
                boolean maxIsNull          = rs.wasNull();   // ← must be right here
                int utilisations           = rs.getInt("utilisations");
                if (!maxIsNull && utilisationsMax > 0 && utilisations >= utilisationsMax) {
                    return new ResultatCode("Ce code a atteint son nombre maximum d'utilisations.");
                }

                return new ResultatCode(
                        rs.getInt("code_id"),
                        rs.getString("type_reduction"),
                        rs.getDouble("valeur"),
                        rs.getDouble("montant_min")
                );
            }
        }
    }

    /**
     * Incrémente le compteur d'utilisations après un paiement réussi.
     */
    public void incrementerUtilisation(int codeId) throws SQLException {
        String sql = "UPDATE codes_promo SET utilisations = utilisations + 1 WHERE code_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, codeId);
            ps.executeUpdate();
        }
    }
}