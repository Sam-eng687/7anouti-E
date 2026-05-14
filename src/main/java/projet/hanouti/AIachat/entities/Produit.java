package projet.hanouti.AIachat.entities;

/**
 * Produit stub for M2.
 *
 * Real DB table: produit
 * ┌─────────────────┬──────────────────────────────────────────┐
 * │ DB column       │ Java field / getter                      │
 * ├─────────────────┼──────────────────────────────────────────┤
 * │ id_produit      │ idProduit   / getIdProduit()             │
 * │ nom             │ libelle     / getLibelle()  ← mapped     │
 * │ description     │ description / getDescription()           │
 * │ prix            │ prix        / getPrix()                  │
 * │ quantite_stock  │ quantiteStock / getQuantiteStock()       │
 * │ categorie       │ categorie   / getCategorie()  UPPERCASE  │
 * │ moyenne        │ noteMoyenne / getNoteMoyenne() ← mapped  │
 * └─────────────────┴──────────────────────────────────────────┘
 *
 * The field name "libelle" is kept intentionally so that ScoringService,
 * GeminiService, and AssistantIAController need zero changes.
 * ProduitServices.mapRow() passes rs.getString("nom") into the libelle param.
 */
public class Produit {

    private int     idProduit;
    private String  libelle;        // DB: nom
    private String  description;
    private double  prix;
    private int     quantiteStock;
    private String  categorie;      // DB stores UPPERCASE: MEDICAMENT, ALIMENTAIRE, ELECTRONIQUE, HYGIENE, DECOR, MAKEUP, AUTRE
    private Double  noteMoyenne;    // DB: moyenne - nullable
    private String  imageUrl;       // DB: image_url - nullable, product photo path or URL

    public Produit() {}

    public Produit(int idProduit, String libelle, String description,
                   double prix, int quantiteStock, String categorie, Double noteMoyenne) {
        this.idProduit     = idProduit;
        this.libelle       = libelle;
        this.description   = description;
        this.prix          = prix;
        this.quantiteStock = quantiteStock;
        this.categorie     = categorie;
        this.noteMoyenne   = noteMoyenne;
    }

    public int     getIdProduit()     { return idProduit; }
    public String  getLibelle()       { return libelle; }
    public String  getDescription()   { return description; }
    public double  getPrix()          { return prix; }
    public int     getQuantiteStock() { return quantiteStock; }
    public String  getCategorie()     { return categorie; }
    public Double  getNoteMoyenne()   { return noteMoyenne; }
    public String  getImageUrl()      { return imageUrl; }

    public void setIdProduit(int v)        { this.idProduit     = v; }
    public void setLibelle(String v)       { this.libelle       = v; }
    public void setDescription(String v)   { this.description   = v; }
    public void setPrix(double v)          { this.prix          = v; }
    public void setQuantiteStock(int v)    { this.quantiteStock = v; }
    public void setCategorie(String v)     { this.categorie     = v; }
    public void setNoteMoyenne(Double v)   { this.noteMoyenne   = v; }
    public void setImageUrl(String v)      { this.imageUrl      = v; }

    @Override
    public String toString() {
        return "Produit{id=" + idProduit + ", libelle='" + libelle
                + "', prix=" + prix + ", categorie='" + categorie
                + "', note=" + noteMoyenne + "}";
    }
}

