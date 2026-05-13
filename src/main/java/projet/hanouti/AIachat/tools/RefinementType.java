package projet.hanouti.AIachat.tools;

/**
 * Refinement intents accepted while the assistant is showing results.
 */
public enum RefinementType {

    /** User wants cheaper options. */
    PRICE_DOWN,

    /** User is willing to spend more. */
    PRICE_UP,

    /** User wants results from a specific category. */
    CATEGORY_FILTER,

    /** User wants results sorted by rating descending. */
    SORT_RATING,

    /** User rejects the first visible result. */
    EXCLUDE_TOP,

    /** No refinement matched, so the message starts a new search. */
    NEW_SEARCH
}


