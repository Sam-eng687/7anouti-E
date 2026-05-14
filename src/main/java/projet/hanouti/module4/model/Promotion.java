package projet.hanouti.module4.model;

import java.time.LocalDateTime;

/**
 * Model représentant une promotion sur un produit.
 *
 * Correspond à la table SQL :
 *   CREATE TABLE promotions (
 *     promo_id    INT AUTO_INCREMENT PRIMARY KEY,
 *     produit_id  INT NOT NULL,
 *     pourcentage DECIMAL(5,2) NOT NULL,
 *     date_debut  DATETIME NOT NULL,
 *     date_fin    DATETIME,          -- NULL = promo permanente
 *     actif       TINYINT(1) DEFAULT 1,
 *     FOREIGN KEY (produit_id) REFERENCES produit(id_produit)
 *   );
 */
public class Promotion {

    private int           promoId;
    private int           produitId;
    private double        pourcentage;   // ex: 20.0 = 20%
    private LocalDateTime dateDebut;
    private LocalDateTime dateFin;       // null si permanente
    private boolean       actif;

    public Promotion() {}

    public Promotion(int produitId, double pourcentage, LocalDateTime dateDebut, LocalDateTime dateFin) {
        this.produitId   = produitId;
        this.pourcentage = pourcentage;
        this.dateDebut   = dateDebut;
        this.dateFin     = dateFin;
        this.actif       = true;
    }

    // ── Getters / Setters ──────────────────────────────────
    public int           getPromoId()               { return promoId; }
    public void          setPromoId(int promoId)    { this.promoId = promoId; }

    public int           getProduitId()             { return produitId; }
    public void          setProduitId(int id)       { this.produitId = id; }

    public double        getPourcentage()           { return pourcentage; }
    public void          setPourcentage(double p)   { this.pourcentage = p; }

    public LocalDateTime getDateDebut()             { return dateDebut; }
    public void          setDateDebut(LocalDateTime d) { this.dateDebut = d; }

    public LocalDateTime getDateFin()               { return dateFin; }
    public void          setDateFin(LocalDateTime d)   { this.dateFin = d; }

    public boolean       isActif()                  { return actif; }
    public void          setActif(boolean actif)    { this.actif = actif; }

    /**
     * Retourne true si la promo est en cours (actif + dans la période).
     * Utile pour vérifier côté Java sans refaire un appel SQL.
     */
    public boolean isEnCours() {
        if (!actif) return false;
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(dateDebut)) return false;
        if (dateFin != null && now.isAfter(dateFin)) return false;
        return true;
    }

    @Override
    public String toString() {
        return "Promotion{produitId=" + produitId
                + ", pourcentage=" + pourcentage
                + ", dateFin=" + dateFin
                + ", actif=" + actif + "}";
    }
}