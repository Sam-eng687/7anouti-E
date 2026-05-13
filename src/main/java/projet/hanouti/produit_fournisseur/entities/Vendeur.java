package projet.hanouti.produit_fournisseur.entities;

public class Vendeur {
    private int    idVendeur;
    private String nom;
    private String prenom;
    private String email;

    public Vendeur() {}

    public Vendeur(String nom, String prenom, String email) {
        this.nom    = nom;
        this.prenom = prenom;
        this.email  = email;
    }

    public int    getIdVendeur()         { return idVendeur; }
    public void   setIdVendeur(int id)   { this.idVendeur = id; }
    public String getNom()               { return nom; }
    public void   setNom(String nom)     { this.nom = nom; }
    public String getPrenom()            { return prenom; }
    public void   setPrenom(String p)    { this.prenom = p; }
    public String getEmail()             { return email; }
    public void   setEmail(String email) { this.email = email; }

    @Override
    public String toString() { return prenom + " " + nom; }
}