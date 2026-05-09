package projet.hanouti.GestionCommandes.entities;

import projet.hanouti.GestionCommandes.enums.ModePaiement;
import projet.hanouti.GestionCommandes.enums.StatutCommande;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class Commande {

    private int idCommande;
    private String numeroCommande;

    private int idAcheteur;
    private int idVendeur;
    private Integer idSocieteLivraison;

    private String adresseLivraison;

    private java.time.LocalDateTime dateCreation;
    private java.time.LocalDate dateLivraisonPreferee;

    private ModePaiement modePaiement;

    private double total;

    private int scorePriorite;

    private StatutCommande statut;

    private String motifRefus;

    private String facturePdf;
    private String factureQr;

    private java.time.LocalDateTime createdAt;
    private java.time.LocalDateTime updatedAt;

    public Commande() {
    }

    public Commande(int idCommande, String numeroCommande, int idAcheteur, int idVendeur, Integer idSocieteLivraison, String adresseLivraison, LocalDateTime dateCreation, LocalDate dateLivraisonPreferee, ModePaiement modePaiement, double total, int scorePriorite, StatutCommande statut, String motifRefus, String facturePdf, String factureQr, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.idCommande = idCommande;
        this.numeroCommande = numeroCommande;
        this.idAcheteur = idAcheteur;
        this.idVendeur = idVendeur;
        this.idSocieteLivraison = idSocieteLivraison;
        this.adresseLivraison = adresseLivraison;
        this.dateCreation = dateCreation;
        this.dateLivraisonPreferee = dateLivraisonPreferee;
        this.modePaiement = modePaiement;
        this.total = total;
        this.scorePriorite = scorePriorite;
        this.statut = statut;
        this.motifRefus = motifRefus;
        this.facturePdf = facturePdf;
        this.factureQr = factureQr;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public int getIdCommande() {
        return idCommande;
    }

    public void setIdCommande(int idCommande) {
        this.idCommande = idCommande;
    }

    public String getNumeroCommande() {
        return numeroCommande;
    }

    public void setNumeroCommande(String numeroCommande) {
        this.numeroCommande = numeroCommande;
    }

    public int getIdAcheteur() {
        return idAcheteur;
    }

    public void setIdAcheteur(int idAcheteur) {
        this.idAcheteur = idAcheteur;
    }

    public int getIdVendeur() {
        return idVendeur;
    }

    public void setIdVendeur(int idVendeur) {
        this.idVendeur = idVendeur;
    }

    public Integer getIdSocieteLivraison() {
        return idSocieteLivraison;
    }

    public void setIdSocieteLivraison(Integer idSocieteLivraison) {
        this.idSocieteLivraison = idSocieteLivraison;
    }

    public String getAdresseLivraison() {
        return adresseLivraison;
    }

    public void setAdresseLivraison(String adresseLivraison) {
        this.adresseLivraison = adresseLivraison;
    }

    public LocalDateTime getDateCreation() {
        return dateCreation;
    }

    public void setDateCreation(LocalDateTime dateCreation) {
        this.dateCreation = dateCreation;
    }

    public LocalDate getDateLivraisonPreferee() {
        return dateLivraisonPreferee;
    }

    public void setDateLivraisonPreferee(LocalDate dateLivraisonPreferee) {
        this.dateLivraisonPreferee = dateLivraisonPreferee;
    }

    public ModePaiement getModePaiement() {
        return modePaiement;
    }

    public void setModePaiement(ModePaiement modePaiement) {
        this.modePaiement = modePaiement;
    }

    public double getTotal() {
        return total;
    }

    public void setTotal(double total) {
        this.total = total;
    }

    public int getScorePriorite() {
        return scorePriorite;
    }

    public void setScorePriorite(int scorePriorite) {
        this.scorePriorite = scorePriorite;
    }

    public StatutCommande getStatut() {
        return statut;
    }

    public void setStatut(StatutCommande statut) {
        this.statut = statut;
    }

    public String getMotifRefus() {
        return motifRefus;
    }

    public void setMotifRefus(String motifRefus) {
        this.motifRefus = motifRefus;
    }

    public String getFacturePdf() {
        return facturePdf;
    }

    public void setFacturePdf(String facturePdf) {
        this.facturePdf = facturePdf;
    }

    public String getFactureQr() {
        return factureQr;
    }

    public void setFactureQr(String factureQr) {
        this.factureQr = factureQr;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public String toString() {
        return "Commande{" +
                "idCommande=" + idCommande +
                ", numeroCommande='" + numeroCommande + '\'' +
                ", idAcheteur=" + idAcheteur +
                ", idVendeur=" + idVendeur +
                ", idSocieteLivraison=" + idSocieteLivraison +
                ", adresseLivraison='" + adresseLivraison + '\'' +
                ", dateCreation=" + dateCreation +
                ", dateLivraisonPreferee=" + dateLivraisonPreferee +
                ", modePaiement=" + modePaiement +
                ", total=" + total +
                ", scorePriorite=" + scorePriorite +
                ", statut=" + statut +
                ", motifRefus='" + motifRefus + '\'' +
                ", facturePdf='" + facturePdf + '\'' +
                ", factureQr='" + factureQr + '\'' +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}
