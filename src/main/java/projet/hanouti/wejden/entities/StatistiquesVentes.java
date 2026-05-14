package projet.hanouti.wejden.entities;

public class StatistiquesVentes {
    private int statId;
    private String produitId;
    private String periode;
    private int totalVendu;
    private double revenuTotal;
    private double tauxRetour;
    private String classement;
    private int semaine;

    private String produitNom;

    public StatistiquesVentes() {
    }

    public String getProduitNom() { return produitNom; }
    public void setProduitNom(String produitNom) { this.produitNom = produitNom; }

    public StatistiquesVentes(String produitId, String periode, int totalVendu, double revenuTotal, double tauxRetour, String classement, int semaine) {
        this.produitId = produitId;
        this.periode = periode;
        this.totalVendu = totalVendu;
        this.revenuTotal = revenuTotal;
        this.tauxRetour = tauxRetour;
        this.classement = classement;
        this.semaine = semaine;
    }

    public int getSemaine() { return semaine; }
    public void setSemaine(int semaine) { this.semaine = semaine; }

    public int getStatId() { return statId; }
    public void setStatId(int statId) { this.statId = statId; }

    public String getProduitId() { return produitId; }
    public void setProduitId(String produitId) { this.produitId = produitId; }

    public String getPeriode() { return periode; }
    public void setPeriode(String periode) { this.periode = periode; }

    public int getTotalVendu() { return totalVendu; }
    public void setTotalVendu(int totalVendu) { this.totalVendu = totalVendu; }

    public double getRevenuTotal() { return revenuTotal; }
    public void setRevenuTotal(double revenuTotal) { this.revenuTotal = revenuTotal; }

    public double getTauxRetour() { return tauxRetour; }
    public void setTauxRetour(double tauxRetour) { this.tauxRetour = tauxRetour; }

    public String getClassement() { return classement; }
    public void setClassement(String classement) { this.classement = classement; }

    public double getRevUnite() {
        if (totalVendu <= 0) return 0;
        return revenuTotal / totalVendu;
    }
}
