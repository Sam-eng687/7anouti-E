package projet.hanouti.GestionCommandes.entities;

import projet.hanouti.GestionCommandes.enums.StatutSociete;

public class SocieteLivraison {

    private int idSociete;
    private int idUser;

    private String nomSociete;

    private String zoneCouverture;

    private String adresseSociete;

    private double note;

    private StatutSociete statut;


    public SocieteLivraison() {
    }

    public SocieteLivraison(int idSociete, int idUser, String nomSociete, String zoneCouverture, String adresseSociete, double note, StatutSociete statut) {
        this.idSociete = idSociete;
        this.idUser = idUser;
        this.nomSociete = nomSociete;
        this.zoneCouverture = zoneCouverture;
        this.adresseSociete = adresseSociete;
        this.note = note;
        this.statut = statut;
    }

    public int getIdSociete() {
        return idSociete;
    }

    public void setIdSociete(int idSociete) {
        this.idSociete = idSociete;
    }

    public int getIdUser() {
        return idUser;
    }

    public void setIdUser(int idUser) {
        this.idUser = idUser;
    }

    public String getNomSociete() {
        return nomSociete;
    }

    public void setNomSociete(String nomSociete) {
        this.nomSociete = nomSociete;
    }

    public String getZoneCouverture() {
        return zoneCouverture;
    }

    public void setZoneCouverture(String zoneCouverture) {
        this.zoneCouverture = zoneCouverture;
    }

    public String getAdresseSociete() {
        return adresseSociete;
    }

    public void setAdresseSociete(String adresseSociete) {
        this.adresseSociete = adresseSociete;
    }

    public double getNote() {
        return note;
    }

    public void setNote(double note) {
        this.note = note;
    }

    public StatutSociete getStatut() {
        return statut;
    }

    public void setStatut(StatutSociete statut) {
        this.statut = statut;
    }

    @Override
    public String toString() {
        return "SocieteLivraison{" +
                "idSociete=" + idSociete +
                ", idUser=" + idUser +
                ", nomSociete='" + nomSociete + '\'' +
                ", zoneCouverture='" + zoneCouverture + '\'' +
                ", adresseSociete='" + adresseSociete + '\'' +
                ", note=" + note +
                ", statut=" + statut +
                '}';
    }
}
