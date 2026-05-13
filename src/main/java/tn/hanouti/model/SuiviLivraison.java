package tn.hanouti.model;

import java.time.LocalDateTime;

/**
 * Maps to the shared 'livraisons' table (team schema).
 * Extra columns (id_livreur, adresse_client, localisation_actuelle)
 * are added by this module via ALTER TABLE on first run.
 */
public class SuiviLivraison {

    private int idLivraison;
    private int idCommande;
    private String numeroCommande;
    private int idSociete;
    private String modeAssignation;   // AUTOMATIQUE / MANUELLE
    private String statut;            // ASSIGNEE / EN_COURS / LIVREE / ANNULEE
    private LocalDateTime dateAssignation;
    private LocalDateTime dateDebutLivraison;
    private LocalDateTime dateLivraison;

    // Extra columns added by this module
    private int idLivreur;
    private String adresseClient;
    private String localisationActuelle;

    public SuiviLivraison() {}

    public SuiviLivraison(int idLivraison, int idCommande, String numeroCommande,
                          int idSociete, String modeAssignation, String statut,
                          LocalDateTime dateAssignation, LocalDateTime dateDebutLivraison,
                          LocalDateTime dateLivraison,
                          int idLivreur, String adresseClient, String localisationActuelle) {
        this.idLivraison = idLivraison;
        this.idCommande = idCommande;
        this.numeroCommande = numeroCommande;
        this.idSociete = idSociete;
        this.modeAssignation = modeAssignation;
        this.statut = statut;
        this.dateAssignation = dateAssignation;
        this.dateDebutLivraison = dateDebutLivraison;
        this.dateLivraison = dateLivraison;
        this.idLivreur = idLivreur;
        this.adresseClient = adresseClient;
        this.localisationActuelle = localisationActuelle;
    }

    // ── Compatibility helpers ──────────────────────────────────

    /** Returns id_livraison — used where old code used idSuivi */
    public int getIdSuivi() { return idLivraison; }

    /** Returns date_livraison as string for display */
    public String getHeureEstimee() {
        if (dateLivraison == null) return null;
        return String.format("%02dh%02d",
                dateLivraison.getHour(), dateLivraison.getMinute());
    }

    /** Maps old statut values to new ones for display */
    public String getStatutDisplay() {
        if (statut == null) return "—";
        return switch (statut) {
            case "ASSIGNEE"  -> "EN_ATTENTE";
            case "EN_COURS"  -> "AFFECTEE";
            case "LIVREE"    -> "LIVREE";
            case "ANNULEE"   -> "ANNULEE";
            default          -> statut;
        };
    }

    // ── Getters & Setters ──────────────────────────────────────

    public int getIdLivraison() { return idLivraison; }
    public void setIdLivraison(int idLivraison) { this.idLivraison = idLivraison; }

    public int getIdCommande() { return idCommande; }
    public void setIdCommande(int idCommande) { this.idCommande = idCommande; }

    public String getNumeroCommande() { return numeroCommande; }
    public void setNumeroCommande(String numeroCommande) { this.numeroCommande = numeroCommande; }

    public int getIdSociete() { return idSociete; }
    public void setIdSociete(int idSociete) { this.idSociete = idSociete; }

    public String getModeAssignation() { return modeAssignation; }
    public void setModeAssignation(String modeAssignation) { this.modeAssignation = modeAssignation; }

    public String getStatut() { return statut; }
    public void setStatut(String statut) { this.statut = statut; }

    public LocalDateTime getDateAssignation() { return dateAssignation; }
    public void setDateAssignation(LocalDateTime dateAssignation) { this.dateAssignation = dateAssignation; }

    public LocalDateTime getDateDebutLivraison() { return dateDebutLivraison; }
    public void setDateDebutLivraison(LocalDateTime dateDebutLivraison) { this.dateDebutLivraison = dateDebutLivraison; }

    public LocalDateTime getDateLivraison() { return dateLivraison; }
    public void setDateLivraison(LocalDateTime dateLivraison) { this.dateLivraison = dateLivraison; }

    public int getIdLivreur() { return idLivreur; }
    public void setIdLivreur(int idLivreur) { this.idLivreur = idLivreur; }

    public String getAdresseClient() { return adresseClient; }
    public void setAdresseClient(String adresseClient) { this.adresseClient = adresseClient; }

    public String getLocalisationActuelle() { return localisationActuelle; }
    public void setLocalisationActuelle(String localisationActuelle) {
        this.localisationActuelle = localisationActuelle;
    }
}
