package projet.hanouti.AImarketing.entities;

import java.sql.Date;

public class CampagneMarketing {
    private int campagneId;
    private String nomCampagne;
    private String objectif;
    private String canal;
    private double budget;
    private double depense;
    private String statut;
    private Date dateDebut;
    private Date dateFin;
    private double iaScore;
    private String iaConseil;

    public int getCampagneId() {
        return campagneId;
    }

    public void setCampagneId(int campagneId) {
        this.campagneId = campagneId;
    }

    public String getNomCampagne() {
        return nomCampagne;
    }

    public void setNomCampagne(String nomCampagne) {
        this.nomCampagne = nomCampagne;
    }

    public String getObjectif() {
        return objectif;
    }

    public void setObjectif(String objectif) {
        this.objectif = objectif;
    }

    public String getCanal() {
        return canal;
    }

    public void setCanal(String canal) {
        this.canal = canal;
    }

    public double getBudget() {
        return budget;
    }

    public void setBudget(double budget) {
        this.budget = budget;
    }

    public double getDepense() {
        return depense;
    }

    public void setDepense(double depense) {
        this.depense = depense;
    }

    public String getStatut() {
        return statut;
    }

    public void setStatut(String statut) {
        this.statut = statut;
    }

    public Date getDateDebut() {
        return dateDebut;
    }

    public void setDateDebut(Date dateDebut) {
        this.dateDebut = dateDebut;
    }

    public Date getDateFin() {
        return dateFin;
    }

    public void setDateFin(Date dateFin) {
        this.dateFin = dateFin;
    }

    public double getIaScore() {
        return iaScore;
    }

    public void setIaScore(double iaScore) {
        this.iaScore = iaScore;
    }

    public String getIaConseil() {
        return iaConseil;
    }

    public void setIaConseil(String iaConseil) {
        this.iaConseil = iaConseil;
    }
}
