package projet.hanouti.module4.model;

import java.util.Date;

/**
 * Modèle Wishlist — représente un article dans la liste de souhaits d'un utilisateur.
 * Chemin : src/main/java/com/hanouti/hanoutiem4/model/Wishlist.java
 */
public class Wishlist {

    private int wishlistId;
    private int userId;
    private int produitId;
    private Date dateAjout;

    // Champs d'affichage (JOIN avec produits)
    private String nomProduit;
    private double prixProduit;

    // ── Constructeur ────────────────────────────────────────
    public Wishlist(int userId, int produitId) {
        this.userId    = userId;
        this.produitId = produitId;
    }

    // ── Getters / Setters ───────────────────────────────────
    public int getWishlistId()                       { return wishlistId; }
    public void setWishlistId(int wishlistId)        { this.wishlistId = wishlistId; }

    public int getUserId()                           { return userId; }
    public void setUserId(int userId)                { this.userId = userId; }

    public int getProduitId()                        { return produitId; }
    public void setProduitId(int produitId)          { this.produitId = produitId; }

    public Date getDateAjout()                       { return dateAjout; }
    public void setDateAjout(Date dateAjout)         { this.dateAjout = dateAjout; }

    public String getNomProduit()                    { return nomProduit; }
    public void setNomProduit(String nomProduit)     { this.nomProduit = nomProduit; }

    public double getPrixProduit()                   { return prixProduit; }
    public void setPrixProduit(double prixProduit)   { this.prixProduit = prixProduit; }
}