package tn.hanouti.livreur.model;

import java.time.LocalDateTime;

public class Score {
    private int idScore;
    private int idLivreur;          // livreur concerné
    private int note;               // note attribuée (1 à 5)
    private String commentaire;     // avis du client
    private boolean livreDansDelai; // livraison dans les délais ?
    private LocalDateTime dateEvaluation;

    public Score() {}

    // Constructeur pour ajouter une évaluation
    public Score(int idLivreur, int note, String commentaire, boolean livreDansDelai) {
        this.idLivreur = idLivreur;
        this.note = note;
        this.commentaire = commentaire;
        this.livreDansDelai = livreDansDelai;
        this.dateEvaluation = LocalDateTime.now();
    }

    // Constructeur complet (lecture depuis DB)
    public Score(int idScore, int idLivreur, int note, String commentaire,
                 boolean livreDansDelai, LocalDateTime dateEvaluation) {
        this.idScore = idScore;
        this.idLivreur = idLivreur;
        this.note = note;
        this.commentaire = commentaire;
        this.livreDansDelai = livreDansDelai;
        this.dateEvaluation = dateEvaluation;
    }

    // Getters & Setters
    public int getIdScore() { return idScore; }
    public void setIdScore(int idScore) { this.idScore = idScore; }

    public int getIdLivreur() { return idLivreur; }
    public void setIdLivreur(int idLivreur) { this.idLivreur = idLivreur; }

    public int getNote() { return note; }
    public void setNote(int note) { this.note = note; }

    public String getCommentaire() { return commentaire; }
    public void setCommentaire(String commentaire) { this.commentaire = commentaire; }

    public boolean isLivreDansDelai() { return livreDansDelai; }
    public void setLivreDansDelai(boolean livreDansDelai) { this.livreDansDelai = livreDansDelai; }

    public LocalDateTime getDateEvaluation() { return dateEvaluation; }
    public void setDateEvaluation(LocalDateTime dateEvaluation) { this.dateEvaluation = dateEvaluation; }
}
