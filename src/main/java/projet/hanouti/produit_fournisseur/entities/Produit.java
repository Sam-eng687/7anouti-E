package projet.hanouti.produit_fournisseur.entities;

import java.time.LocalDateTime;

public class Produit {
    private int           idProduit;
    private int           idVendeur;
    private Integer       idFournisseur;
    private String        nom;
    private String        description;
    private String        categorie;
    private double        prix;
    private int           quantiteStock;
    private int           seuilAlerte;
    private String        image;
    private String        statut;
    private LocalDateTime dateAjout;
    private float         moyenne;

    public Produit() {}

    public Produit(int idVendeur, Integer idFournisseur, String nom,
                   String description, String categorie, double prix,
                   int quantiteStock, int seuilAlerte, String image, String statut) {
        this.idVendeur     = idVendeur;
        this.idFournisseur = idFournisseur;
        this.nom           = nom;
        this.description   = description;
        this.categorie     = categorie;
        this.prix          = prix;
        this.quantiteStock = quantiteStock;
        this.seuilAlerte   = seuilAlerte;
        this.image         = image;
        this.statut        = statut;
    }

    public int           getIdProduit()               { return idProduit; }
    public void          setIdProduit(int id)          { this.idProduit = id; }
    public int           getIdVendeur()               { return idVendeur; }
    public void          setIdVendeur(int id)          { this.idVendeur = id; }
    public Integer       getIdFournisseur()           { return idFournisseur; }
    public void          setIdFournisseur(Integer id)  { this.idFournisseur = id; }
    public String        getNom()                     { return nom; }
    public void          setNom(String n)              { this.nom = n; }
    public String        getDescription()             { return description; }
    public void          setDescription(String d)      { this.description = d; }
    public String        getCategorie()               { return categorie; }
    public void          setCategorie(String c)        { this.categorie = c; }
    public double        getPrix()                    { return prix; }
    public void          setPrix(double p)             { this.prix = p; }
    public int           getQuantiteStock()           { return quantiteStock; }
    public void          setQuantiteStock(int q)       { this.quantiteStock = q; }
    public int           getSeuilAlerte()             { return seuilAlerte; }
    public void          setSeuilAlerte(int s)         { this.seuilAlerte = s; }
    public String        getImage()                   { return image; }
    public void          setImage(String i)            { this.image = i; }
    public String        getStatut()                  { return statut; }
    public void          setStatut(String s)           { this.statut = s; }
    public LocalDateTime getDateAjout()               { return dateAjout; }
    public void          setDateAjout(LocalDateTime d) { this.dateAjout = d; }
    public float         getMoyenne()                 { return moyenne; }
    public void          setMoyenne(float m)           { this.moyenne = m; }

    @Override
    public String toString() { return nom + "  " + prix + " TND (" + categorie + ")"; }
}