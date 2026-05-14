package tn.hanouti.livreur.model;

/**
 * Represents an active order currently out for delivery — customer-facing view.
 * Combines data from Suivi_Livraison and Livreur tables.
 */
public class CommandeActive {

    private int idSuivi;
    private int idCommande;
    private String adresseClient;
    private String heureEstimee;
    private String statut;

    // Livreur info (joined)
    private int idLivreur;
    private String nomLivreur;
    private String telephoneLivreur;
    private String genreVehicule;

    // GPS coordinates stored as "lat,lon" strings in localisation_actuelle
    private String localisationActuelle;

    public CommandeActive() {}

    public CommandeActive(int idSuivi, int idCommande, String adresseClient,
                          String heureEstimee, String statut,
                          int idLivreur, String nomLivreur,
                          String telephoneLivreur, String genreVehicule,
                          String localisationActuelle) {
        this.idSuivi = idSuivi;
        this.idCommande = idCommande;
        this.adresseClient = adresseClient;
        this.heureEstimee = heureEstimee;
        this.statut = statut;
        this.idLivreur = idLivreur;
        this.nomLivreur = nomLivreur;
        this.telephoneLivreur = telephoneLivreur;
        this.genreVehicule = genreVehicule;
        this.localisationActuelle = localisationActuelle;
    }

    // ── Getters & Setters ──────────────────────────────────────

    public int getIdSuivi() { return idSuivi; }
    public void setIdSuivi(int idSuivi) { this.idSuivi = idSuivi; }

    public int getIdCommande() { return idCommande; }
    public void setIdCommande(int idCommande) { this.idCommande = idCommande; }

    public String getAdresseClient() { return adresseClient; }
    public void setAdresseClient(String adresseClient) { this.adresseClient = adresseClient; }

    public String getHeureEstimee() { return heureEstimee; }
    public void setHeureEstimee(String heureEstimee) { this.heureEstimee = heureEstimee; }

    public String getStatut() { return statut; }
    public void setStatut(String statut) { this.statut = statut; }

    public int getIdLivreur() { return idLivreur; }
    public void setIdLivreur(int idLivreur) { this.idLivreur = idLivreur; }

    public String getNomLivreur() { return nomLivreur; }
    public void setNomLivreur(String nomLivreur) { this.nomLivreur = nomLivreur; }

    public String getTelephoneLivreur() { return telephoneLivreur; }
    public void setTelephoneLivreur(String telephoneLivreur) { this.telephoneLivreur = telephoneLivreur; }

    public String getGenreVehicule() { return genreVehicule; }
    public void setGenreVehicule(String genreVehicule) { this.genreVehicule = genreVehicule; }

    public String getLocalisationActuelle() { return localisationActuelle; }
    public void setLocalisationActuelle(String localisationActuelle) {
        this.localisationActuelle = localisationActuelle;
    }

    /**
     * Returns a human-readable status label in French.
     */
    public String getStatutLabel() {
        if (statut == null) return "—";
        return switch (statut) {
            case "AFFECTEE"  -> "En livraison";
            case "EN_ATTENTE" -> "En attente";
            case "LIVREE"    -> "Livrée";
            default          -> statut;
        };
    }

    /**
     * Returns the vehicle emoji for display.
     */
    public String getVehiculeEmoji() {
        if (genreVehicule == null) return "🚗";
        return genreVehicule.toLowerCase().contains("camion") ? "🚚" : "🛵";
    }
}

