package projet.hanouti.AIachat.entities;

import java.time.LocalDateTime;

public class HistoriqueIA {

    private int     idRecherche;
    private int     idAcheteur;
    private String  motsCles;
    private Integer produitSuggreId;  // Integer (not int) because it can be NULL in DB
    private LocalDateTime dateRecherche;

    // ── Constructors ──────────────────────────────────────────────────────────

    public HistoriqueIA() {}

    // Used when inserting - no id yet, produit can be null
    public HistoriqueIA(int idAcheteur, String motsCles, Integer produitSuggreId) {
        this.idAcheteur       = idAcheteur;
        this.motsCles         = motsCles;
        this.produitSuggreId  = produitSuggreId;
    }

    // Used when reading from DB - id is known
    public HistoriqueIA(int idRecherche, int idAcheteur, String motsCles, Integer produitSuggreId) {
        this.idRecherche      = idRecherche;
        this.idAcheteur       = idAcheteur;
        this.motsCles         = motsCles;
        this.produitSuggreId  = produitSuggreId;
    }

    public HistoriqueIA(int idRecherche, int idAcheteur, String motsCles, Integer produitSuggreId, LocalDateTime dateRecherche) {
        this.idRecherche = idRecherche;
        this.idAcheteur = idAcheteur;
        this.motsCles = motsCles;
        this.produitSuggreId = produitSuggreId;
        this.dateRecherche = dateRecherche;
    }

    // ── Getters & Setters ─────────────────────────────────────────────────────

    public int getIdRecherche() { return idRecherche; }
    public void setIdRecherche(int idRecherche) { this.idRecherche = idRecherche; }

    public int getIdAcheteur() { return idAcheteur; }
    public void setIdAcheteur(int idAcheteur) { this.idAcheteur = idAcheteur; }

    public String getMotsCles() { return motsCles; }
    public void setMotsCles(String motsCles) { this.motsCles = motsCles; }

    public Integer getProduitSuggreId() { return produitSuggreId; }
    public void setProduitSuggreId(Integer produitSuggreId) { this.produitSuggreId = produitSuggreId; }

    public LocalDateTime getDateRecherche() { return dateRecherche; }
    public void setDateRecherche(LocalDateTime dateRecherche) { this.dateRecherche = dateRecherche; }

    // ── toString ──────────────────────────────────────────────────────────────

    @Override
    public String toString() {
        return "HistoriqueIA{" +
                "idRecherche=" + idRecherche +
                ", idAcheteur=" + idAcheteur +
                ", motsCles='" + motsCles + '\'' +
                ", produitSuggreId=" + (produitSuggreId != null ? produitSuggreId : "aucun résultat") +
                ", dateRecherche=" + dateRecherche +
                '}';
    }
}


