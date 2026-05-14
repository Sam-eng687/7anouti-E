package tn.hanouti.livreur.model;

import java.time.LocalDate;
import java.time.Period;

public class Livreur {
    private int idLivreur;
    private String nomLivreur;
    private String telephone;
    private int idSocieteLivraison;
    private boolean disponibilite;
    private LocalDate dateNaissance;
    private String photo;
    private int score;
    private String genreVehicule; // "Voiture" ou "Petit camion"
    private boolean isResponsable; // true = responsable livraison, false = livreur normal

    public Livreur() {}

    // Constructeur simple (ajout)
    public Livreur(String nomLivreur, String telephone, int idSocieteLivraison) {
        this.nomLivreur = nomLivreur;
        this.telephone = telephone;
        this.idSocieteLivraison = idSocieteLivraison;
        this.disponibilite = true;
        this.score = 0;
        this.genreVehicule = "Voiture";
        this.isResponsable = false;
    }

    // Constructeur complet (lecture depuis DB)
    public Livreur(int idLivreur, String nomLivreur, String telephone,
                   int idSocieteLivraison, boolean disponibilite,
                   LocalDate dateNaissance, String photo, int score,
                   String genreVehicule, boolean isResponsable) {
        this.idLivreur = idLivreur;
        this.nomLivreur = nomLivreur;
        this.telephone = telephone;
        this.idSocieteLivraison = idSocieteLivraison;
        this.disponibilite = disponibilite;
        this.dateNaissance = dateNaissance;
        this.photo = photo;
        this.score = score;
        this.genreVehicule = genreVehicule;
        this.isResponsable = isResponsable;
    }

    // Calcule l'âge automatiquement
    public int getAge() {
        if (dateNaissance == null) return 0;
        return Period.between(dateNaissance, LocalDate.now()).getYears();
    }

    // Getters & Setters
    public int getIdLivreur() { return idLivreur; }
    public void setIdLivreur(int idLivreur) { this.idLivreur = idLivreur; }

    public String getNomLivreur() { return nomLivreur; }
    public void setNomLivreur(String nomLivreur) { this.nomLivreur = nomLivreur; }

    public String getTelephone() { return telephone; }
    public void setTelephone(String telephone) { this.telephone = telephone; }

    public int getIdSocieteLivraison() { return idSocieteLivraison; }
    public void setIdSocieteLivraison(int id) { this.idSocieteLivraison = id; }

    public boolean isDisponibilite() { return disponibilite; }
    public void setDisponibilite(boolean disponibilite) { this.disponibilite = disponibilite; }

    public LocalDate getDateNaissance() { return dateNaissance; }
    public void setDateNaissance(LocalDate dateNaissance) { this.dateNaissance = dateNaissance; }

    public String getPhoto() { return photo; }
    public void setPhoto(String photo) { this.photo = photo; }

    public int getScore() { return score; }
    public void setScore(int score) { this.score = score; }

    public String getGenreVehicule() { return genreVehicule; }
    public void setGenreVehicule(String genreVehicule) { this.genreVehicule = genreVehicule; }

    public boolean isResponsable() { return isResponsable; }
    public void setResponsable(boolean responsable) { this.isResponsable = responsable; }
}
