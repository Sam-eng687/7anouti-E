package projet.hanouti.AImarketing.entities;

import java.sql.Date;

public class ConseilsMarketing {
    private int conseilId;
    private String produitId;
    private String produitNom;
    private String typeConseil;
    private String description;
    private String impactEstime;
    private Date dateGeneration;
    private boolean applique;
    private boolean ignore;
    private int score;

    public int getConseilId() {
        return conseilId;
    }

    public void setConseilId(int conseilId) {
        this.conseilId = conseilId;
    }

    public String getProduitId() {
        return produitId;
    }

    public void setProduitId(String produitId) {
        this.produitId = produitId;
    }

    public String getProduitNom() {
        return produitNom;
    }

    public void setProduitNom(String produitNom) {
        this.produitNom = produitNom;
    }

    public String getTypeConseil() {
        return typeConseil;
    }

    public void setTypeConseil(String typeConseil) {
        this.typeConseil = typeConseil;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getImpactEstime() {
        return impactEstime;
    }

    public void setImpactEstime(String impactEstime) {
        this.impactEstime = impactEstime;
    }

    public Date getDateGeneration() {
        return dateGeneration;
    }

    public void setDateGeneration(Date dateGeneration) {
        this.dateGeneration = dateGeneration;
    }

    public boolean isApplique() {
        return applique;
    }

    public void setApplique(boolean applique) {
        this.applique = applique;
    }

    public boolean isIgnore() {
        return ignore;
    }

    public void setIgnore(boolean ignore) {
        this.ignore = ignore;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }
}
