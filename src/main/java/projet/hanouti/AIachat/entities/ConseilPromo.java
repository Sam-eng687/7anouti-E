package projet.hanouti.AIachat.entities;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Represents an active accepted conseil (Promotion or Bundle)
 * as shown to the buyer in the Discounts & Offers section.
 *
 * Populated by PromotionService.getActiveConseils().
 * Never modifies produit.prix - effective price is computed on the fly.
 */
public class ConseilPromo {

    private int             idConseil;
    private String          type;           // "Promotion" | "Bundle"
    private String          titreAcheteur;  // buyer-facing title filled by M7's AI
    private double          discount;       // percentage - e.g. 15.0 = -15%
    private LocalDateTime   dateExpiration;
    private List<Produit>   produits;       // 1 product for Promotion, 2+ for Bundle

    public ConseilPromo() {}

    public ConseilPromo(int idConseil, String type, String titreAcheteur,
                        double discount, LocalDateTime dateExpiration, List<Produit> produits) {
        this.idConseil      = idConseil;
        this.type           = type;
        this.titreAcheteur  = titreAcheteur;
        this.discount       = discount;
        this.dateExpiration = dateExpiration;
        this.produits       = produits;
    }

    // ── Getters & Setters ─────────────────────────────────────────────────────

    public int getIdConseil()                        { return idConseil; }
    public void setIdConseil(int v)                  { this.idConseil = v; }

    public String getType()                          { return type; }
    public void setType(String v)                    { this.type = v; }

    public String getTitreAcheteur()                 { return titreAcheteur; }
    public void setTitreAcheteur(String v)           { this.titreAcheteur = v; }

    public double getDiscount()                      { return discount; }
    public void setDiscount(double v)                { this.discount = v; }

    public LocalDateTime getDateExpiration()         { return dateExpiration; }
    public void setDateExpiration(LocalDateTime v)   { this.dateExpiration = v; }

    public List<Produit> getProduits()               { return produits; }
    public void setProduits(List<Produit> v)         { this.produits = v; }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Computes the effective price after applying the discount.
     * Promotion  → discounted single product price
     * Bundle     → discounted sum of all product prices
     * Never touches produit.prix in the DB.
     */
    public double getEffectivePrice() {
        if (produits == null || produits.isEmpty()) return 0;
        double total = produits.stream().mapToDouble(Produit::getPrix).sum();
        return total * (1.0 - discount / 100.0);
    }

    /**
     * Original total price before discount.
     * Bundle → sum of individual prices.
     * Promotion → single product price.
     */
    public double getOriginalPrice() {
        if (produits == null || produits.isEmpty()) return 0;
        return produits.stream().mapToDouble(Produit::getPrix).sum();
    }

    @Override
    public String toString() {
        return "ConseilPromo{id=" + idConseil
                + ", type='" + type + '\''
                + ", discount=" + discount + "%"
                + ", produits=" + produits + "}";
    }
}


