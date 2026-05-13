package edu.hanouti.entities;

import java.sql.Date;

public class ConseilsMarketing {
    private int conseilId;
    private String produitId;
    private String typeConseil;
    private String description;
    private String impactEstime;
    private Date dateGeneration;
    private boolean applique;
    private String produitNom;
    private int score;
    private String urgence;

    public ConseilsMarketing() {}

    public String getProduitNom() { return produitNom; }
    public void setProduitNom(String produitNom) { this.produitNom = produitNom; }
    public int getScore() { return score; }
    public void setScore(int score) { this.score = score; }
    public String getUrgence() { return urgence; }
    public void setUrgence(String urgence) { this.urgence = urgence; }

    public ConseilsMarketing(String produitId, String typeConseil, String description,
                              String impactEstime, Date dateGeneration, boolean applique) {
        this.produitId = produitId;
        this.typeConseil = typeConseil;
        this.description = description;
        this.impactEstime = impactEstime;
        this.dateGeneration = dateGeneration;
        this.applique = applique;
    }

    public int getConseilId() { return conseilId; }
    public void setConseilId(int conseilId) { this.conseilId = conseilId; }
    public String getProduitId() { return produitId; }
    public void setProduitId(String produitId) { this.produitId = produitId; }
    public String getTypeConseil() { return typeConseil; }
    public void setTypeConseil(String typeConseil) { this.typeConseil = typeConseil; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getImpactEstime() { return impactEstime; }
    public void setImpactEstime(String impactEstime) { this.impactEstime = impactEstime; }
    public Date getDateGeneration() { return dateGeneration; }
    public void setDateGeneration(Date dateGeneration) { this.dateGeneration = dateGeneration; }
    public boolean isApplique() { return applique; }
    public void setApplique(boolean applique) { this.applique = applique; }
}
