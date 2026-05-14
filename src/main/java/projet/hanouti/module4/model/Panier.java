package projet.hanouti.module4.model;
import java.util.Date;
public class Panier {
    private int panierId;
    private int userId;
    private int produitId;
    private int quantite;
    private double prixUnitaire;
    private Date dateAjout;
    private String statut;
    private String nomProduit; // for display in the UI

    // Constructor
    public Panier(int userId, int produitId, int quantite, double prixUnitaire) {
        this.userId = userId;
        this.produitId = produitId;
        this.quantite = quantite;
        this.prixUnitaire = prixUnitaire;
        this.statut = "actif";
    }

    // Getters and Setters
    public int getPanierId() { return panierId; }
    public void setPanierId(int panierId) { this.panierId = panierId; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public int getProduitId() { return produitId; }
    public void setProduitId(int produitId) { this.produitId = produitId; }

    public int getQuantite() { return quantite; }
    public void setQuantite(int quantite) { this.quantite = quantite; }

    public double getPrixUnitaire() { return prixUnitaire; }
    public void setPrixUnitaire(double prixUnitaire) { this.prixUnitaire = prixUnitaire; }

    public Date getDateAjout() { return dateAjout; }
    public void setDateAjout(Date dateAjout) { this.dateAjout = dateAjout; }

    public String getStatut() { return statut; }
    public void setStatut(String statut) { this.statut = statut; }

    public String getNomProduit() { return nomProduit; }
    public void setNomProduit(String nomProduit) { this.nomProduit = nomProduit; }

    // Calculate total price for this item
    public double getSousTotal() {
        return quantite * prixUnitaire;
    }
}