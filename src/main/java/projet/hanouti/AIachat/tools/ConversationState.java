package projet.hanouti.AIachat.tools;

/**
 * Represents the current state of the AI assistant conversation.
 *
 * Flow:
 * IDLE → WAITING_NEED → WAITING_BUDGET → SHOWING_RESULTS
 *   ↑_________________________________________|  (reset / new search)
 *
 * IMAGE_SEARCH is an independent branch:
 * IDLE ──────────────────┐
 * WAITING_NEED (empty) ──┼──→ IMAGE_SEARCH → SHOWING_IMAGE_RESULTS
 * SHOWING_RESULTS ───────┘              ↓
 *                                      IDLE (reset) or WAITING_NEED (new search)
 *
 * SHOWING_RESULTS handles in-place refinement through RefinementDetector.
 * WAITING_BUDGET is reused for explicit budget negotiation; the controller
 * flag isBudgetNegotiation distinguishes that path from the first budget ask.
 */
public enum ConversationState {

    /** App just opened or user reset. Shows recommendation cards. */
    IDLE,

    /** Waiting for user to describe their need (2+ keywords required). */
    WAITING_NEED,

    /** Keywords extracted. Now waiting for a valid budget number. */
    WAITING_BUDGET,

    /** Scoring done. Results displayed as cards and can be refined. */
    SHOWING_RESULTS,

    /** User submitted an image. Gemini is identifying the product.
     *  No budget involved - results shown directly after identification. */
    IMAGE_SEARCH
}

