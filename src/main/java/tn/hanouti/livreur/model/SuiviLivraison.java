package tn.hanouti.livreur.model;

public class SuiviLivraison {
    private int idSuivi;
    private int idCommande;
    private int idLivreur;
    private String adresseClient;  // adresse reçue depuis Module 5
    private String localisationActuelle;
    private String heureEstimee;
    private String statut;         // EN_ATTENTE / AFFECTEE / LIVREE

    public SuiviLivraison() {}

    // Constructeur minimal — créé par Module 5
    public SuiviLivraison(int idCommande, String adresseClient) {
        this.idCommande = idCommande;
        this.adresseClient = adresseClient;
        this.statut = "EN_ATTENTE";
    }

    // Constructeur complet — lecture depuis DB
    public SuiviLivraison(int idSuivi, int idCommande, int idLivreur,
                          String adresseClient, String localisationActuelle,
                          String heureEstimee, String statut) {
        this.idSuivi = idSuivi;
        this.idCommande = idCommande;
        this.idLivreur = idLivreur;
        this.adresseClient = adresseClient;
        this.localisationActuelle = localisationActuelle;
        this.heureEstimee = heureEstimee;
        this.statut = statut;
    }

    // Getters & Setters
    public int getIdSuivi() { return idSuivi; }
    public void setIdSuivi(int idSuivi) { this.idSuivi = idSuivi; }

    public int getIdCommande() { return idCommande; }
    public void setIdCommande(int idCommande) { this.idCommande = idCommande; }

    public int getIdLivreur() { return idLivreur; }
    public void setIdLivreur(int idLivreur) { this.idLivreur = idLivreur; }

    public String getAdresseClient() { return adresseClient; }
    public void setAdresseClient(String adresseClient) { this.adresseClient = adresseClient; }

    public String getLocalisationActuelle() { return localisationActuelle; }
    public void setLocalisationActuelle(String loc) { this.localisationActuelle = loc; }

    public String getHeureEstimee() { return heureEstimee; }
    public void setHeureEstimee(String heure) { this.heureEstimee = heure; }

    public String getStatut() { return statut; }
    public void setStatut(String statut) { this.statut = statut; }
}
