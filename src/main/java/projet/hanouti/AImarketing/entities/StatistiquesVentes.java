package projet.hanouti.AImarketing.entities;

public class StatistiquesVentes {
    private int statId;
    private String produitId;
    private String produitNom;
    private String periode;
    private int totalVendu;
    private double revenuTotal;
    private double tauxRetour;
    private String classement;
    /** Optionnel (schema Premium / Wejden) */
    private int semaine;

    public int getSemaine() {
        return semaine;
    }

    public void setSemaine(int semaine) {
        this.semaine = semaine;
    }

    /** Revenu moyen par unite vendue (aligne module Premium). */
    public double getRevUnite() {
        if (totalVendu <= 0) {
            return 0;
        }
        return revenuTotal / totalVendu;
    }

    public int getStatId() {
        return statId;
    }

    public void setStatId(int statId) {
        this.statId = statId;
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

    public String getPeriode() {
        return periode;
    }

    public void setPeriode(String periode) {
        this.periode = periode;
    }

    public int getTotalVendu() {
        return totalVendu;
    }

    public void setTotalVendu(int totalVendu) {
        this.totalVendu = totalVendu;
    }

    public double getRevenuTotal() {
        return revenuTotal;
    }

    public void setRevenuTotal(double revenuTotal) {
        this.revenuTotal = revenuTotal;
    }

    public double getTauxRetour() {
        return tauxRetour;
    }

    public void setTauxRetour(double tauxRetour) {
        this.tauxRetour = tauxRetour;
    }

    public String getClassement() {
        return classement;
    }

    public void setClassement(String classement) {
        this.classement = classement;
    }
}
