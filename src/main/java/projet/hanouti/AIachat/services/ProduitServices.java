package projet.hanouti.AIachat.services;

import projet.hanouti.AIachat.entities.Produit;
import projet.hanouti.common.utils.MyBD;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class ProduitServices {

    private final InteractionUtilisateurServices interactionService = new InteractionUtilisateurServices();

    private Connection getConnection() {
        return MyBD.getInstance().getConnection();
    }

    /**
     * Maps a ResultSet row to a Produit.
     *
     * Real DB columns  →  Java field
     * ─────────────────────────────────
     * nom              →  libelle
     * note_moy         →  noteMoyenne
     * categorie        →  categorie   (stored UPPERCASE in DB, returned as-is)
     */
    private Produit mapRow(ResultSet rs) throws SQLException {
        double noteRaw = rs.getDouble("moyenne");
        Double note    = rs.wasNull() ? null : noteRaw;

        Produit p = new Produit(
                rs.getInt("id_produit"),
                rs.getString("nom"),          // DB column is "nom", not "libelle"
                rs.getString("description"),
                rs.getDouble("prix"),
                rs.getInt("quantite_stock"),
                rs.getString("categorie"),    // stored as MEDICAMENT / ALIMENTAIRE / ELECTRONIQUE / HYGIENE / DECOR / MAKEUP / AUTRE
                note
        );

        // Load product image path from DB (column: "image").
        // If your column has a different name, change "image" below.
        try {
            String img = rs.getString("image");
            p.setImageUrl(img); // null if the cell is empty → card shows category gradient
        } catch (SQLException ignored) {
            // Column not present in this result set - safe to skip
        }

        return p;
    }

    public Produit getById(int idProduit) {
        String query = "SELECT * FROM produit WHERE id_produit = ?";
        try (PreparedStatement pst = getConnection().prepareStatement(query)) {
            pst.setInt(1, idProduit);
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }
        } catch (SQLException | RuntimeException e) {
            System.out.println("[ERROR] ProduitServices.getById: " + e.getMessage());
        }
        return null;
    }

    public List<Produit> getAllInStock() {
        List<Produit> list = new ArrayList<>();
        // Only show ACTIF products that have stock
        String query = "SELECT * FROM produit WHERE quantite_stock > 0 AND statut = 'ACTIF'";
        try (PreparedStatement pst = getConnection().prepareStatement(query);
             ResultSet rs = pst.executeQuery()) {
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException | RuntimeException e) {
            System.out.println("[ERROR] ProduitServices.getAllInStock: " + e.getMessage());
        }
        return list;
    }

    public List<Produit> getByCategories(List<String> categories) {
        if (categories == null || categories.isEmpty()) return getTopRated();
        List<Produit> list = new ArrayList<>();
        String query = "SELECT * FROM produit WHERE quantite_stock > 0 AND statut = 'ACTIF' ORDER BY moyenne DESC";
        try (PreparedStatement pst = getConnection().prepareStatement(query);
             ResultSet rs = pst.executeQuery()) {
            while (rs.next() && list.size() < 6) {
                Produit p = mapRow(rs);
                // categories from DB are uppercase; compare case-insensitively
                if (categories.stream().anyMatch(c -> c.equalsIgnoreCase(p.getCategorie())))
                    list.add(p);
            }
        } catch (SQLException | RuntimeException e) {
            System.out.println("[ERROR] ProduitServices.getByCategories: " + e.getMessage());
        }
        return list;
    }

    public List<Produit> getTopRated() {
        List<Produit> list = new ArrayList<>();
        // note_moy is the real column name
        String query = "SELECT * FROM produit WHERE quantite_stock > 0 AND statut = 'ACTIF' ORDER BY moyenne DESC LIMIT 18";
        try (PreparedStatement pst = getConnection().prepareStatement(query);
             ResultSet rs = pst.executeQuery()) {
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException | RuntimeException e) {
            System.out.println("[ERROR] ProduitServices.getTopRated: " + e.getMessage());
        }
        return list;
    }

    public List<String> getDistinctCategories() {
        List<String> categories = new ArrayList<>();
        String query = "SELECT DISTINCT categorie FROM produit WHERE quantite_stock > 0 AND statut = 'ACTIF'";
        try (PreparedStatement pst = getConnection().prepareStatement(query);
             ResultSet rs = pst.executeQuery()) {
            while (rs.next()) categories.add(rs.getString("categorie"));
        } catch (SQLException | RuntimeException e) {
            System.out.println("[ERROR] ProduitServices.getDistinctCategories: " + e.getMessage());
        }
        return categories;
    }

    public List<Produit> getIdleRecommendations(int idAcheteur, List<String> categoriesByFreq) {
        List<Produit> products = getAllInStock();
        if (products.isEmpty()) return products;

        // Batch retrieve interactions to avoid N+1 query problem
        Map<Integer, Map<String, Integer>> interactionsMap = interactionService.getInteractionsMap(idAcheteur);

        List<ScoredIdle> scored = new ArrayList<>();
        for (Produit p : products) {
            if (p == null) continue;
            double score  = p.getNoteMoyenne() != null ? p.getNoteMoyenne() : 3.0;
            score += categoryPositionBonus(p.getCategorie(), categoriesByFreq);

            Map<String, Integer> counts = interactionsMap.get(p.getIdProduit());
            if (counts != null) {
                score += counts.getOrDefault(InteractionUtilisateurServices.TYPE_VIEW, 0)        * 2.0;
                score += counts.getOrDefault(InteractionUtilisateurServices.TYPE_ADD_TO_CART, 0) * 5.0;
            }

            scored.add(new ScoredIdle(p, score));
        }

        scored.sort(Comparator.comparingDouble((ScoredIdle s) -> s.score).reversed());
        List<Produit> top = new ArrayList<>();
        for (int i = 0; i < scored.size() && i < 18; i++) top.add(scored.get(i).produit);
        return top;
    }

    private double categoryPositionBonus(String categorie, List<String> categoriesByFreq) {
        if (categoriesByFreq == null || categoriesByFreq.isEmpty() || categorie == null) return 1;
        // case-insensitive index search (DB stores UPPERCASE, history may vary)
        for (int i = 0; i < categoriesByFreq.size(); i++) {
            if (categorie.equalsIgnoreCase(categoriesByFreq.get(i))) {
                if (i == 0) return 10;
                if (i == 1) return 7;
                if (i == 2) return 4;
                return 1;
            }
        }
        return 1;
    }

    private static class ScoredIdle {
        final Produit produit;
        final double  score;
        ScoredIdle(Produit p, double s) { produit = p; score = s; }
    }
}

