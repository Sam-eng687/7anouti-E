package projet.hanouti.AIachat.services;

import projet.hanouti.AIachat.entities.ConseilPromo;
import projet.hanouti.AIachat.entities.Produit;
import projet.hanouti.common.utils.MyBD;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Handles all promotion and bundle data for the buyer side (M2).
 *
 * Two responsibilities:
 *  1. loadActivePromoMap()  - fast Map<productId, discount%> loaded once at
 *                             controller startup, used for price overlay on
 *                             every card and for effective price in ScoringService.
 *
 *  2. getActiveConseils()   - full ConseilPromo list for rendering the
 *                             Discounts & Offers section in IDLE.
 *
 * Only queries etat='ACCEPTE', type IN ('Promotion','Bundle'),
 * within the active date window (date_accepte <= NOW() < date_expiration).
 * Never reads or writes produit.prix.
 */
public class PromotionService {

    private final ProduitServices produitServices = new ProduitServices();

    private Connection getConnection() {
        return MyBD.getInstance().getConnection();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 1 - Promo map (price overlay + scoring)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Returns Map<productId, discount%> covering every product that currently
     * has an active accepted Promotion or Bundle conseil.
     *
     * For Promotion: id_produit comes directly from conseils_ia.
     * For Bundle:    id_produit comes from conseil_produits junction table.
     *
     * If a product appears in multiple active offers (edge case), the highest
     * discount wins - the buyer always gets the best deal shown.
     *
     * Called once at controller startup and stored as a field.
     * Result is passed to ScoringService and all card builders.
     */
    public Map<Integer, Double> loadActivePromoMap() {
        Map<Integer, Double> map = new HashMap<>();

        // ── Promotion: single product per conseil ─────────────────────────────
        String promoQuery =
                "SELECT id_produit, discount FROM conseils_ia " +
                "WHERE etat = 'ACCEPTE' " +
                "AND type = 'Promotion' " +
                "AND id_produit IS NOT NULL " +
                "AND discount IS NOT NULL " +
                "AND date_accepte <= NOW() " +
                "AND date_expiration > NOW()";

        try (PreparedStatement pst = getConnection().prepareStatement(promoQuery);
             ResultSet rs = pst.executeQuery()) {
            while (rs.next()) {
                int    pid  = rs.getInt("id_produit");
                double disc = rs.getDouble("discount");
                map.merge(pid, disc, Math::max);
            }
            System.out.println("[INFO] PromotionService: " + map.size() + " Promotion product(s) loaded");
        } catch (SQLException | RuntimeException e) {
            System.out.println("[ERROR] PromotionService.loadActivePromoMap (Promotion): " + e.getMessage());
        }

        // ── Bundle: multiple products per conseil via junction table ──────────
        String bundleQuery =
                "SELECT cp.id_produit, c.discount " +
                "FROM conseils_ia c " +
                "JOIN conseil_produits cp ON c.id_conseil = cp.id_conseil " +
                "WHERE c.etat = 'ACCEPTE' " +
                "AND c.type = 'Bundle' " +
                "AND c.discount IS NOT NULL " +
                "AND c.date_accepte <= NOW() " +
                "AND c.date_expiration > NOW()";

        int bundleCount = 0;
        try (PreparedStatement pst = getConnection().prepareStatement(bundleQuery);
             ResultSet rs = pst.executeQuery()) {
            while (rs.next()) {
                int    pid  = rs.getInt("id_produit");
                double disc = rs.getDouble("discount");
                map.merge(pid, disc, Math::max);
                bundleCount++;
            }
            System.out.println("[INFO] PromotionService: " + bundleCount + " Bundle product link(s) loaded");
        } catch (SQLException | RuntimeException e) {
            System.out.println("[ERROR] PromotionService.loadActivePromoMap (Bundle): " + e.getMessage());
        }

        System.out.println("[INFO] PromotionService: promo map total = " + map.size() + " product(s)");
        return map;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 2 - Full conseil list (Discounts & Offers section)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Returns the full list of active accepted conseils for rendering
     * the Discounts & Offers section in IDLE.
     *
     * Each ConseilPromo carries its Produit list:
     *   Promotion → 1 product fetched via id_produit
     *   Bundle    → 2+ products fetched from conseil_produits
     *
     * Ordered by score DESC so highest-confidence offers appear first.
     * Skipped if produit(s) can't be resolved (deleted product etc.).
     */
    public List<ConseilPromo> getActiveConseils() {
        List<ConseilPromo> list = new ArrayList<>();

        String query =
                "SELECT id_conseil, type, titre_acheteur, discount, date_expiration, id_produit " +
                "FROM conseils_ia " +
                "WHERE etat = 'ACCEPTE' " +
                "AND type IN ('Promotion', 'Bundle') " +
                "AND discount IS NOT NULL " +
                "AND date_accepte <= NOW() " +
                "AND date_expiration > NOW() " +
                "ORDER BY score DESC";

        try (PreparedStatement pst = getConnection().prepareStatement(query);
             ResultSet rs = pst.executeQuery()) {

            while (rs.next()) {
                int           idConseil  = rs.getInt("id_conseil");
                String        type       = rs.getString("type");
                String        titre      = rs.getString("titre_acheteur");
                double        disc       = rs.getDouble("discount");
                Timestamp     ts         = rs.getTimestamp("date_expiration");
                LocalDateTime expiration = (ts != null) ? ts.toLocalDateTime() : null;

                // ── Resolve product(s) ────────────────────────────────────────
                List<Produit> produits = new ArrayList<>();

                if ("Promotion".equals(type)) {
                    int idProduit = rs.getInt("id_produit");
                    if (!rs.wasNull()) {
                        Produit p = produitServices.getById(idProduit);
                        if (p != null) produits.add(p);
                    }
                } else {
                    // Bundle - load all linked products from junction table
                    produits = getBundleProduits(idConseil);
                }

                // Skip this conseil if no valid products resolved
                if (produits.isEmpty()) {
                    System.out.println("[WARN] PromotionService: conseil " + idConseil + " skipped - no valid products");
                    continue;
                }

                // ── Fallback title if M7 hasn't filled it yet ─────────────────
                if (titre == null || titre.isBlank()) {
                    titre = "Promotion".equals(type)
                            ? "Offre spéciale -" + (int) disc + "%"
                            : "Bundle -" + (int) disc + "%";
                }

                list.add(new ConseilPromo(idConseil, type, titre, disc, expiration, produits));
            }

        } catch (SQLException | RuntimeException e) {
            System.out.println("[ERROR] PromotionService.getActiveConseils: " + e.getMessage());
        }

        System.out.println("[INFO] PromotionService: " + list.size() + " active conseil(s) for display");
        return list;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Private helpers
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Fetches all products linked to a Bundle conseil via conseil_produits.
     * Returns an empty list (never null) if anything goes wrong.
     */
    private List<Produit> getBundleProduits(int idConseil) {
        List<Produit> produits = new ArrayList<>();
        String query = "SELECT id_produit FROM conseil_produits WHERE id_conseil = ?";

        try (PreparedStatement pst = getConnection().prepareStatement(query)) {
            pst.setInt(1, idConseil);
            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    Produit p = produitServices.getById(rs.getInt("id_produit"));
                    if (p != null) produits.add(p);
                }
            }
        } catch (SQLException | RuntimeException e) {
            System.out.println("[ERROR] PromotionService.getBundleProduits (conseil=" + idConseil + "): " + e.getMessage());
        }

        return produits;
    }
}


