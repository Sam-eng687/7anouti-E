package projet.hanouti.AIachat.tools;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Keyword-based detector for refinement messages sent after results are shown.
 */
public class RefinementDetector {

    private static final String[] EXCLUDE_PATTERNS = {
            "pas ca", "pas ce", "pas celui", "autre chose", "autre produit",
            "pas ce produit", "pas celui la", "non pas ca", "change de",
            "je ne veux pas ca", "pas celui ci", "autre option", "change",
            "different", "pas interessant", "pas ce que je veux"
    };

    private static final String[] PRICE_DOWN_PATTERNS = {
            "moins cher", "trop cher", "abordable", "pas cher", "economique",
            "prix bas", "bon marche", "moins couteux", "budget reduit",
            "plus abordable", "baisse le prix", "prix reduit", "moins expensive",
            "pas assez abordable", "trop expensive"
    };

    private static final String[] PRICE_UP_PATTERNS = {
            "peu importe le prix", "peu importe", "je peux payer plus",
            "budget illimite", "pas de limite", "augmente le budget",
            "plus de budget", "payer plus", "je peux mettre plus",
            "depenser plus", "pas de probleme de prix", "budget plus grand",
            "augmenter budget"
    };

    private static final String[] SORT_RATING_PATTERNS = {
            "mieux note", "meilleure note", "bien note", "par note",
            "selon note", "trier par note", "note plus haute", "note elevee",
            "mieux evalue", "meilleur avis", "top note", "trier par avis",
            "les mieux notes"
    };

    private static final String[] CATEGORY_TRIGGER_PATTERNS = {
            "seulement", "que des", "uniquement", "juste des", "que la",
            "que le", "filtre", "categorie", "de type", "type", "juste"
    };

    public static RefinementIntent detect(String input, List<String> availableCategories) {
        if (input == null || input.trim().isEmpty()) {
            return singleIntent(RefinementType.NEW_SEARCH);
        }

        String normalized = normalize(input);
        Set<RefinementType> types = new LinkedHashSet<>();
        Map<RefinementType, String> parameters = new HashMap<>();

        if (matchesAny(normalized, EXCLUDE_PATTERNS)) {
            types.add(RefinementType.EXCLUDE_TOP);
        }

        String category = detectCategory(normalized, availableCategories);
        if (category != null) {
            types.add(RefinementType.CATEGORY_FILTER);
            parameters.put(RefinementType.CATEGORY_FILTER, category);
        }

        if (matchesAny(normalized, PRICE_UP_PATTERNS)) {
            types.add(RefinementType.PRICE_UP);
        }

        if (!types.contains(RefinementType.PRICE_UP) && matchesAny(normalized, PRICE_DOWN_PATTERNS)) {
            types.add(RefinementType.PRICE_DOWN);
        }

        if (matchesAny(normalized, SORT_RATING_PATTERNS)) {
            types.add(RefinementType.SORT_RATING);
        }

        if (types.isEmpty()) {
            types.add(RefinementType.NEW_SEARCH);
        }

        return new RefinementIntent(types, parameters);
    }

    private static boolean matchesAny(String text, String[] patterns) {
        for (String pattern : patterns) {
            if (text.contains(pattern)) return true;
        }
        return false;
    }

    private static String detectCategory(String normalizedInput, List<String> availableCategories) {
        if (availableCategories == null || availableCategories.isEmpty()) return null;
        if (!matchesAny(normalizedInput, CATEGORY_TRIGGER_PATTERNS)) return null;

        for (String category : availableCategories) {
            if (normalizedInput.contains(normalize(category))) {
                return category;
            }
        }
        return null;
    }

    private static String normalize(String text) {
        return KeywordExtractor.removeAccents(text == null ? "" : text)
                .toLowerCase()
                .replaceAll("[^a-z0-9\\s]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private static RefinementIntent singleIntent(RefinementType type) {
        Set<RefinementType> types = new LinkedHashSet<>();
        types.add(type);
        return new RefinementIntent(types, new HashMap<>());
    }
}


