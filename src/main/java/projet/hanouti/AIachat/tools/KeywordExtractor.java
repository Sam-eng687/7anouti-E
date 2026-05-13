package projet.hanouti.AIachat.tools;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

public class KeywordExtractor {

    private static final Set<String> STOP_WORDS = new HashSet<>(Arrays.asList(
            "je", "tu", "il", "elle", "nous", "vous", "ils", "elles",
            "moi", "toi", "lui", "eux", "me", "te", "se", "le", "la", "les", "l",
            "un", "une", "des", "du", "de", "d", "ce", "cet", "cette", "ces",
            "et", "ou", "a", "au", "aux", "en", "y", "sur", "sous", "chez",
            "dans", "par", "pour", "avec", "sans", "vers", "entre", "contre",
            "mon", "ma", "mes", "ton", "ta", "tes", "son", "sa", "ses",
            "notre", "votre", "leur", "leurs",
            "veux", "vouloir", "veut", "voulez", "ai", "avoir", "cherche", "chercher",
            "recherche", "rechercher", "besoin", "voudrais", "aimerais", "peux", "puis",
            "aller", "acheter", "trouver", "montre", "donne", "propose", "conseille",
            "recommande", "voir", "afficher", "vais", "est", "sont", "suis", "fait",
            "faut", "need", "want", "buy", "show", "find",
            "que", "qui", "quoi", "comment", "quand", "ou", "quel", "quelle",
            "quels", "quelles", "combien",
            "produit", "produits", "article", "articles", "chose", "truc",
            "pas", "plus", "moins", "tres", "bien", "bon", "bonne", "meilleur",
            "meilleure", "meilleurs", "meilleures", "cher", "chere", "disponible",
            "disponibles", "aussi", "comme", "alors", "donc", "mais", "car", "si",
            "svp", "stp", "merci", "environ",

            // Action verbs that appear in shopping requests but carry no product meaning
            "faire", "donner", "preparer", "cuisiner", "cuire", "utiliser", "mettre",
            "prendre", "choisir", "selectionner", "commander", "livrer",
            // Generic nouns that should never be keywords
            "ingredients", "ingredient", "liste", "recette", "produits",
            "article", "articles", "chose", "truc", "machin",
            // More French stop words
            "quand", "depuis", "jusque", "apres", "avant", "pendant",
            "toujours", "jamais", "parfois", "souvent", "maintenant"
    ));

    private static final Set<String> RESET_WORDS = new HashSet<>(Arrays.asList(
            "recommencer", "reset", "debut", "annuler", "quitter",
            "nouvelle", "restart", "retour"
    ));

    private static final Set<String> RESET_PHRASES = new HashSet<>(Arrays.asList(
            "nouvelle recherche"
    ));

    public static String removeAccents(String text) {
        if (text == null) return "";
        String normalized = Normalizer.normalize(text, Normalizer.Form.NFD);
        Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
        return pattern.matcher(normalized).replaceAll("");
    }

    public static List<String> extractKeywords(String input) {
        if (input == null || input.trim().isEmpty()) return new ArrayList<>();

        String cleaned = removeAccents(input.toLowerCase().trim());
        cleaned = cleaned.replaceAll("[^a-z0-9\\s]", " ");

        String[] words = cleaned.split("\\s+");
        List<String> keywords = new ArrayList<>();
        for (String word : words) {
            if (word.length() >= 2 && !STOP_WORDS.contains(word)) {
                keywords.add(word);
            }
        }
        return keywords;
    }

    public static boolean isResetKeyword(String input) {
        if (input == null || input.trim().isEmpty()) return false;
        String cleaned = removeAccents(input.toLowerCase().trim()).replaceAll("[^a-z0-9\\s]", " ");
        cleaned = cleaned.replaceAll("\\s+", " ").trim();
        if (RESET_PHRASES.contains(cleaned)) return true;
        for (String word : cleaned.split("\\s+")) {
            if (RESET_WORDS.contains(word)) return true;
        }
        return false;
    }

    public static Double parseBudget(String input) {
        if (input == null || input.trim().isEmpty()) return null;
        String cleaned = input.replaceAll("[^0-9.]", " ").trim();
        String[] parts = cleaned.split("\\s+");
        for (String part : parts) {
            if (!part.isEmpty()) {
                try {
                    double value = Double.parseDouble(part);
                    if (value > 0) return value;
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return null;
    }
}


