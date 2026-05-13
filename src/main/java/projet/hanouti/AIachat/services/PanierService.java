package projet.hanouti.AIachat.services;

import projet.hanouti.common.utils.MyBD;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Handles all operations on the {@code panier} table.
 *
 * Table schema:
 * <pre>
 * CREATE TABLE IF NOT EXISTS panier (
 *   panier_id             INT            NOT NULL AUTO_INCREMENT,
 *   user_id               INT            NOT NULL,
 *   produit_id            INT            NOT NULL,
 *   quantite              INT            NOT NULL DEFAULT 1,
 *   prix_unitaire         DECIMAL(10,2)  NOT NULL,
 *   date_ajout            DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
 *   statut                VARCHAR(50)    NOT NULL DEFAULT 'actif',
 *   reference_transaction VARCHAR(100)   DEFAULT NULL,
 *   PRIMARY KEY (panier_id)
 * ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
 * </pre>
 *
 * When the same user adds the same product again, the quantity is incremented
 * rather than inserting a duplicate row (upsert behaviour).
 */
public class PanierService {

    private Connection getConnection() {
        return MyBD.getInstance().getConnection();
    }

    /**
     * Adds a product to the user's cart with quantity = 1 and statut = 'actif'.
     * If an active row already exists for (user_id, produit_id), increments quantite by 1.
     *
     * @param userId      the authenticated buyer's id
     * @param produitId   the product being added
     * @param prixUnitaire the unit price at the time of adding (snapshot)
     */
    public void addToCart(int userId, int produitId, double prixUnitaire) {
        if (userId <= 0 || produitId <= 0) {
            System.out.println("[WARN] PanierService.addToCart: invalid params ignored");
            return;
        }

        // Check if an active row already exists for this user + product
        String selectSql =
                "SELECT panier_id, quantite FROM panier " +
                "WHERE user_id = ? AND produit_id = ? AND statut = 'actif' LIMIT 1";

        try (PreparedStatement sel = getConnection().prepareStatement(selectSql)) {
            sel.setInt(1, userId);
            sel.setInt(2, produitId);
            try (ResultSet rs = sel.executeQuery()) {
                if (rs.next()) {
                    // Row exists — increment quantity
                    int panierId  = rs.getInt("panier_id");
                    int newQty    = rs.getInt("quantite") + 1;
                    String updateSql = "UPDATE panier SET quantite = ? WHERE panier_id = ?";
                    try (PreparedStatement upd = getConnection().prepareStatement(updateSql)) {
                        upd.setInt(1, newQty);
                        upd.setInt(2, panierId);
                        upd.executeUpdate();
                        System.out.println("[INFO] PanierService: updated qty to " + newQty
                                + " for panier_id=" + panierId);
                    }
                    return;
                }
            }
        } catch (SQLException | RuntimeException e) {
            System.out.println("[ERROR] PanierService.addToCart (select): " + e.getMessage());
            return;
        }

        // No existing row — insert new
        String insertSql =
                "INSERT INTO panier (user_id, produit_id, quantite, prix_unitaire, statut) " +
                "VALUES (?, ?, 1, ?, 'actif')";

        try (PreparedStatement ins = getConnection().prepareStatement(insertSql)) {
            ins.setInt(1, userId);
            ins.setInt(2, produitId);
            ins.setBigDecimal(3, java.math.BigDecimal.valueOf(prixUnitaire)
                    .setScale(2, java.math.RoundingMode.HALF_UP));
            ins.executeUpdate();
            System.out.println("[INFO] PanierService: inserted produit_id=" + produitId
                    + " for user_id=" + userId + " at " + prixUnitaire + " DT");
        } catch (SQLException | RuntimeException e) {
            System.out.println("[ERROR] PanierService.addToCart (insert): " + e.getMessage());
        }
    }
}
