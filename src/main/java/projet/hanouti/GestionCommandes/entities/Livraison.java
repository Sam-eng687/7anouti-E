package projet.hanouti.GestionCommandes.entities;

import projet.hanouti.GestionCommandes.enums.ModeAssignation;
import projet.hanouti.GestionCommandes.enums.StatutLivraison;

import java.time.LocalDateTime;

public class Livraison {

    private int idLivraison;

    private int idCommande;
    private int idSociete;

    private ModeAssignation modeAssignation;

    private StatutLivraison statutLivraison;

    private java.time.LocalDateTime dateAssignation;

    private java.time.LocalDateTime dateDebutLivraison;

    private java.time.LocalDateTime dateLivraison;

    private String numeroCommande;

    public String getNumeroCommande() { return numeroCommande; }
    public void setNumeroCommande(String numeroCommande) { this.numeroCommande = numeroCommande; }
    public Livraison() {
    }

    public Livraison(int idLivraison, int idCommande, int idSociete, ModeAssignation modeAssignation, StatutLivraison statutLivraison, LocalDateTime dateAssignation, LocalDateTime dateDebutLivraison, LocalDateTime dateLivraison) {
        this.idLivraison = idLivraison;
        this.idCommande = idCommande;
        this.idSociete = idSociete;
        this.modeAssignation = modeAssignation;
        this.statutLivraison = statutLivraison;
        this.dateAssignation = dateAssignation;
        this.dateDebutLivraison = dateDebutLivraison;
        this.dateLivraison = dateLivraison;
    }

    public int getIdLivraison() {
        return idLivraison;
    }

    public void setIdLivraison(int idLivraison) {
        this.idLivraison = idLivraison;
    }

    public int getIdCommande() {
        return idCommande;
    }

    public void setIdCommande(int idCommande) {
        this.idCommande = idCommande;
    }

    public int getIdSociete() {
        return idSociete;
    }

    public void setIdSociete(int idSociete) {
        this.idSociete = idSociete;
    }

    public ModeAssignation getModeAssignation() {
        return modeAssignation;
    }

    public void setModeAssignation(ModeAssignation modeAssignation) {
        this.modeAssignation = modeAssignation;
    }

    public StatutLivraison getStatutLivraison() {
        return statutLivraison;
    }

    public void setStatutLivraison(StatutLivraison statutLivraison) {
        this.statutLivraison = statutLivraison;
    }

    public LocalDateTime getDateAssignation() {
        return dateAssignation;
    }

    public void setDateAssignation(LocalDateTime dateAssignation) {
        this.dateAssignation = dateAssignation;
    }

    public LocalDateTime getDateDebutLivraison() {
        return dateDebutLivraison;
    }

    public void setDateDebutLivraison(LocalDateTime dateDebutLivraison) {
        this.dateDebutLivraison = dateDebutLivraison;
    }

    public LocalDateTime getDateLivraison() {
        return dateLivraison;
    }

    public void setDateLivraison(LocalDateTime dateLivraison) {
        this.dateLivraison = dateLivraison;
    }

    @Override
    public String toString() {
        return "Livraison{" +
                "idLivraison=" + idLivraison +
                ", idCommande=" + idCommande +
                ", idSociete=" + idSociete +
                ", modeAssignation=" + modeAssignation +
                ", statutLivraison=" + statutLivraison +
                ", dateAssignation=" + dateAssignation +
                ", dateDebutLivraison=" + dateDebutLivraison +
                ", dateLivraison=" + dateLivraison +
                '}';
    }
}
