package projet.hanouti.AIachat.entities;

import java.util.List;

/**
 * Result returned by GeminiService.analyzeImage().
 *
 * If identified=false, productName/brand/keywords/categorie are all null
 * and failureReason explains why (blurry photo, unrecognized product, etc.)
 *
 * If identified=true, categorie is always non-null - Gemini is required to
 * return a confident category for image search to proceed.
 */
public class ImageRecognitionResult {

    private boolean      identified;
    private String       productName;   // shown to user in chat bubble
    private String       brand;         // informational only, may be null
    private List<String> keywords;      // drives DB search scoring
    private String       categorie;     // mandatory when identified=true
    private String       failureReason; // shown to user when identified=false

    public ImageRecognitionResult() {}

    /** Constructor for a successful identification */
    public ImageRecognitionResult(String productName, String brand,
                                  List<String> keywords, String categorie) {
        this.identified   = true;
        this.productName  = productName;
        this.brand        = brand;
        this.keywords     = keywords;
        this.categorie    = categorie;
        this.failureReason = null;
    }

    /** Constructor for a failed identification */
    public ImageRecognitionResult(String failureReason) {
        this.identified    = false;
        this.failureReason = failureReason;
        this.productName   = null;
        this.brand         = null;
        this.keywords      = null;
        this.categorie     = null;
    }

    // ── Getters & Setters ─────────────────────────────────────────────────────

    public boolean isIdentified()           { return identified; }
    public void    setIdentified(boolean v) { this.identified = v; }

    public String getProductName()             { return productName; }
    public void   setProductName(String v)     { this.productName = v; }

    public String getBrand()             { return brand; }
    public void   setBrand(String v)     { this.brand = v; }

    public List<String> getKeywords()              { return keywords; }
    public void         setKeywords(List<String> v){ this.keywords = v; }

    public String getCategorie()             { return categorie; }
    public void   setCategorie(String v)     { this.categorie = v; }

    public String getFailureReason()             { return failureReason; }
    public void   setFailureReason(String v)     { this.failureReason = v; }

    // ── toString ──────────────────────────────────────────────────────────────

    @Override
    public String toString() {
        if (!identified) return "ImageRecognitionResult{identified=false, reason='" + failureReason + "'}";
        return "ImageRecognitionResult{" +
                "productName='" + productName + '\'' +
                ", brand='" + brand + '\'' +
                ", categorie='" + categorie + '\'' +
                ", keywords=" + keywords +
                '}';
    }
}

