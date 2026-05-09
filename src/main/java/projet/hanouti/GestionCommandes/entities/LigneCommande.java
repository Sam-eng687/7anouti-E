package projet.hanouti.GestionCommandes.entities;

/**
 * LigneCommande — represents one product line inside a commande.
 * nomProduit is fetched from DB and set by the controllers
 * so we NEVER display raw IDs in the UI.
 */
public class LigneCommande {

    private int    idLigne;
    private int    idCommande;
    private int    idProduit;
    private String nomProduit;   // populated by enrichWithProductNames() — never show idProduit in UI
    private int    quantite;
    private double prixUnitaire;
    private double sousTotal;

    // ── Constructors ──────────────────────────────────────────
    public LigneCommande() {}

    public LigneCommande(int idCommande, int idProduit, String nomProduit,
                         int quantite, double prixUnitaire) {
        this.idCommande   = idCommande;
        this.idProduit    = idProduit;
        this.nomProduit   = nomProduit;
        this.quantite     = quantite;
        this.prixUnitaire = prixUnitaire;
        this.sousTotal    = quantite * prixUnitaire;
    }

    // ── Getters / Setters ─────────────────────────────────────
    public int    getIdLigne()       { return idLigne; }
    public void   setIdLigne(int v)  { this.idLigne = v; }

    public int    getIdCommande()       { return idCommande; }
    public void   setIdCommande(int v)  { this.idCommande = v; }

    public int    getIdProduit()        { return idProduit; }
    public void   setIdProduit(int v)   { this.idProduit = v; }

    /** Product name — use this in UI, not idProduit */
    public String getNomProduit()       { return nomProduit; }
    public void   setNomProduit(String v) { this.nomProduit = v; }

    public int    getQuantite()         { return quantite; }
    public void   setQuantite(int v)    { this.quantite = v; }

    public double getPrixUnitaire()        { return prixUnitaire; }
    public void   setPrixUnitaire(double v){ this.prixUnitaire = v; }

    public double getSousTotal()           { return sousTotal; }
    public void   setSousTotal(double v)   { this.sousTotal = v; }

    @Override
    public String toString() {
        return (nomProduit != null ? nomProduit : "Produit inconnu")
                + " × " + quantite
                + " = " + String.format("%.2f TND", sousTotal);
    }
}