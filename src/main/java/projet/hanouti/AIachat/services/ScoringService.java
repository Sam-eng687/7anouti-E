package projet.hanouti.AIachat.services;

import projet.hanouti.AIachat.entities.Produit;
import projet.hanouti.AIachat.tools.KeywordExtractor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class ScoringService {

    private static final int    THRESHOLD      = 20;
    private static final double DEFAULT_RATING = 3.0;
    private static final int    SCORE_IN_TITLE = 10;
    private static final int    SCORE_IN_DESC  = 5;
    private static final int    SCORE_IN_BUDGET = 10;
    private static final double SCORE_VIEW           = 0.5;
    private static final double SCORE_CLICK_PRODUCT  = 1.5;
    private static final double SCORE_ADD_TO_CART    = 3.0;
    private static final double SCORE_BOUGHT         = 5.0;

    private final InteractionUtilisateurServices interactionService = new InteractionUtilisateurServices();

    public static class ScoredProduct {
        public final Produit produit;
        public final double  score;
        public final boolean overBudget;

        public ScoredProduct(Produit produit, double score, boolean overBudget) {
            this.produit    = produit;
            this.score      = score;
            this.overBudget = overBudget;
        }
    }

    // ── Convenience overloads (no promo map) ─────────────────────────────────

    public List<ScoredProduct> score(List<String> keywords, String requestedCategorie,
                                     double budget, List<Produit> products) {
        return score(keywords, requestedCategorie, budget, products, 0, Collections.emptyMap());
    }

    public List<ScoredProduct> score(List<String> keywords, double budget, List<Produit> products) {
        return score(keywords, null, budget, products, 0, Collections.emptyMap());
    }

    public List<ScoredProduct> score(List<String> keywords, String requestedCategorie,
                                     double budget, List<Produit> products, int idAcheteur) {
        return score(keywords, requestedCategorie, budget, products, idAcheteur, Collections.emptyMap());
    }

    // ── Main scoring method ───────────────────────────────────────────────────

    /**
     * @param activePromoMap  productId → discount% loaded by PromotionService.
     *                        If a product is in this map, its effective price
     *                        (after discount) is used for ALL budget logic.
     *                        produit.prix in DB is never modified.
     */
    public List<ScoredProduct> score(List<String> keywords, String requestedCategorie,
                                     double budget, List<Produit> products,
                                     int idAcheteur, Map<Integer, Double> activePromoMap) {
        if (products == null || products.isEmpty()) return Collections.emptyList();

        List<String> safeKeywords   = keywords != null ? keywords : Collections.emptyList();
        String       normRequestedCat = (requestedCategorie != null) ? normalize(requestedCategorie) : null;
        Map<Integer, Double> promoMap = activePromoMap != null ? activePromoMap : Collections.emptyMap();

        // Batch retrieve interactions to avoid N+1 query problem
        Map<Integer, Map<String, Integer>> interactionsMap = interactionService.getInteractionsMap(idAcheteur);

        List<ScoredProduct> results = new ArrayList<>();

        for (Produit p : products) {
            if (p == null) continue;

            // ── Effective price (promo-adjusted) ─────────────────────────────
            Double promoDiscount = promoMap.get(p.getIdProduit());
            double effectivePrice = promoDiscount != null
                    ? p.getPrix() * (1.0 - promoDiscount / 100.0)
                    : p.getPrix();

            // ── Budget hard cutoff (uses effective price) ─────────────────────
            boolean overBudget = effectivePrice > budget;
            if (overBudget && budget > 0) {
                double overRatio = (effectivePrice - budget) / budget;
                if (overRatio >= 1.0) continue;
            }

            // ── Category hard filter ──────────────────────────────────────────
            String productCat = normalize(p.getCategorie());
            if (normRequestedCat != null && !normRequestedCat.equals(productCat)) {
                continue;
            }

            // ── Keyword scoring ───────────────────────────────────────────────
            double score        = 0;
            double keywordScore = 0;

            String libelle     = normalize(p.getLibelle());
            String description = normalize(p.getDescription());

            for (String rawKeyword : safeKeywords) {
                String keyword = stem(rawKeyword);
                if (keyword.length() < 2) continue;

                boolean matched = false;
                for (String token : libelle.split("\\s+")) {
                    if (stem(token).equals(keyword) && token.length() >= 2) {
                        score        += SCORE_IN_TITLE;
                        keywordScore += SCORE_IN_TITLE;
                        matched = true;
                        break;
                    }
                }
                if (!matched) {
                    for (String token : description.split("\\s+")) {
                        if (stem(token).equals(keyword) && token.length() >= 2) {
                            score        += SCORE_IN_DESC;
                            keywordScore += SCORE_IN_DESC;
                            break;
                        }
                    }
                }
            }

            // Hard gate: no keyword match → discard
            if (keywordScore == 0) continue;

            // ── Budget scoring (uses effective price) ─────────────────────────
            if (!overBudget) {
                score += SCORE_IN_BUDGET;
            } else {
                double overRatio = (effectivePrice - budget) / budget;
                score += SCORE_IN_BUDGET * (1.0 - overRatio);
            }

            // ── Rating ────────────────────────────────────────────────────────
            double rating = p.getNoteMoyenne() != null ? p.getNoteMoyenne() : DEFAULT_RATING;
            score += rating;

            // ── Interaction signals (using batch-retrieved Map) ────────────────
            if (idAcheteur > 0) {
                Map<String, Integer> counts = interactionsMap.get(p.getIdProduit());
                if (counts != null) {
                    score += counts.getOrDefault(InteractionUtilisateurServices.TYPE_VIEW, 0)          * SCORE_VIEW;
                    score += counts.getOrDefault(InteractionUtilisateurServices.TYPE_CLICK_PRODUCT, 0) * SCORE_CLICK_PRODUCT;
                    score += counts.getOrDefault(InteractionUtilisateurServices.TYPE_ADD_TO_CART, 0)   * SCORE_ADD_TO_CART;
                    score += counts.getOrDefault(InteractionUtilisateurServices.TYPE_BOUGHT, 0)        * SCORE_BOUGHT;
                }
            }

            // ── Threshold ─────────────────────────────────────────────────────
            if (score >= THRESHOLD) {
                results.add(new ScoredProduct(p, score, overBudget));
            }
        }

        results.sort((a, b) -> Double.compare(b.score, a.score));

        // Fallback: if category filter produced fewer than 2 results, retry without it
        if (normRequestedCat != null && results.size() < 2) {
            return score(keywords, null, budget, products, idAcheteur, promoMap);
        }

        return results;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // scoreForImage - no budget, no promo map needed
    // ─────────────────────────────────────────────────────────────────────────

    public List<ScoredProduct> scoreForImage(List<String> keywords, String categorie,
                                             List<Produit> products, int idAcheteur) {
        System.out.println("[SCORE IMAGE] keywords=" + keywords + " | categorie=" + categorie);

        if (categorie == null || categorie.isBlank()) {
            System.out.println("[SCORE IMAGE] Rejected: categorie is null");
            return Collections.emptyList();
        }
        if (products == null || products.isEmpty()) {
            System.out.println("[SCORE IMAGE] Rejected: product list is empty");
            return Collections.emptyList();
        }

        List<String> safeKeywords  = keywords != null ? keywords : Collections.emptyList();
        String       normCategorie = normalize(categorie);
        List<ScoredProduct> results = new ArrayList<>();

        // Batch retrieve interactions
        Map<Integer, Map<String, Integer>> interactionsMap = interactionService.getInteractionsMap(idAcheteur);

        for (Produit p : products) {
            if (p == null) continue;

            if (!normCategorie.equals(normalize(p.getCategorie()))) continue;

            double score        = 0;
            double keywordScore = 0;

            String libelle     = normalize(p.getLibelle());
            String description = normalize(p.getDescription());

            for (String rawKeyword : safeKeywords) {
                String keyword = stem(rawKeyword);
                if (keyword.length() < 2) continue;

                boolean matched = false;
                for (String token : libelle.split("\\s+")) {
                    if (stem(token).equals(keyword) && token.length() >= 2) {
                        score        += SCORE_IN_TITLE;
                        keywordScore += SCORE_IN_TITLE;
                        matched = true;
                        break;
                    }
                }
                if (!matched) {
                    for (String token : description.split("\\s+")) {
                        if (stem(token).equals(keyword) && token.length() >= 2) {
                            score        += SCORE_IN_DESC;
                            keywordScore += SCORE_IN_DESC;
                            break;
                        }
                    }
                }
            }

            if (keywordScore < 15) continue;

            double rating = p.getNoteMoyenne() != null ? p.getNoteMoyenne() : DEFAULT_RATING;
            score += rating;

            if (idAcheteur > 0) {
                Map<String, Integer> counts = interactionsMap.get(p.getIdProduit());
                if (counts != null) {
                    score += counts.getOrDefault(InteractionUtilisateurServices.TYPE_VIEW, 0)          * SCORE_VIEW;
                    score += counts.getOrDefault(InteractionUtilisateurServices.TYPE_CLICK_PRODUCT, 0) * SCORE_CLICK_PRODUCT;
                    score += counts.getOrDefault(InteractionUtilisateurServices.TYPE_ADD_TO_CART, 0)   * SCORE_ADD_TO_CART;
                    score += counts.getOrDefault(InteractionUtilisateurServices.TYPE_BOUGHT, 0)        * SCORE_BOUGHT;
                }
            }

            if (score >= 12) {
                results.add(new ScoredProduct(p, score, false));
            }
        }

        results.sort((a, b) -> Double.compare(b.score, a.score));
        System.out.println("[SCORE IMAGE] Found " + results.size() + " result(s) before top-6 cut");

        return results.size() > 6 ? new ArrayList<>(results.subList(0, 6)) : results;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────

    private String stem(String w) {
        String n = normalize(w);
        return (n.length() > 4 && n.endsWith("s")) ? n.substring(0, n.length() - 1) : n;
    }

    private String normalize(String text) {
        return KeywordExtractor.removeAccents(text == null ? "" : text).toLowerCase();
    }
}

