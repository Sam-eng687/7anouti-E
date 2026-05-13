package com.hanouti.hanoutiem4.model;

/**
 * Modèle léger pour lire la table produit (M2).
 * On lit seulement — pas de setters pour les champs critiques.
 */
public class Produit {

    private final int    id;
    private final String nom;
    private final String description;
    private final String categorie;
    private final double prix;
    private final int    stock;

    public Produit(int id, String nom, String description,
                   String categorie, double prix, int stock) {
        this.id          = id;
        this.nom         = nom;
        this.description = description;
        this.categorie   = categorie;
        this.prix        = prix;
        this.stock       = stock;
    }

    public int    getId()          { return id; }
    public int    getProduitId()    { return id; } // alias for integration
    public String getNom()         { return nom; }
    public String getDescription() { return description; }
    public String getCategorie()   { return categorie; }
    public double getPrix()        { return prix; }
    public int    getStock()       { return stock; }

    /** Emoji par catégorie pour l'affichage IA */
    public String getEmoji() {
        if (categorie == null) return "🛒";
        return switch (categorie.toUpperCase()) {
            case "ALIMENTAIRE"  -> "🍽️";
            case "ELECTRONIQUE" -> "⚡";
            case "MEDICAMENT"   -> "💊";
            case "HYGIENE"      -> "🧴";
            case "DECOR"        -> "🪴";
            case "MAKEUP"       -> "💄";
            default             -> "🛒";
        };
    }

    @Override
    public String toString() {
        return nom + " (" + String.format("%.2f", prix) + " TND)";
    }
}