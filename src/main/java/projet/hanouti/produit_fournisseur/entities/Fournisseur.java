package projet.hanouti.produit_fournisseur.entities;

public class Fournisseur {
    private int     idFournisseur;
    private int     idVendeur;
    private String  nomSociete;
    private String  contactNom;
    private String  email;
    private String  telephone;
    private String  adresse;
    private String  conditionsLivraison;
    private boolean actif;

    public Fournisseur() {}

    public Fournisseur(int idVendeur, String nomSociete, String contactNom,
                       String email, String telephone, String adresse,
                       String conditionsLivraison, boolean actif) {
        this.idVendeur           = idVendeur;
        this.nomSociete          = nomSociete;
        this.contactNom          = contactNom;
        this.email               = email;
        this.telephone           = telephone;
        this.adresse             = adresse;
        this.conditionsLivraison = conditionsLivraison;
        this.actif               = actif;
    }

    public int     getIdFournisseur()              { return idFournisseur; }
    public void    setIdFournisseur(int id)         { this.idFournisseur = id; }
    public int     getIdVendeur()                  { return idVendeur; }
    public void    setIdVendeur(int id)             { this.idVendeur = id; }
    public String  getNomSociete()                 { return nomSociete; }
    public void    setNomSociete(String n)          { this.nomSociete = n; }
    public String  getContactNom()                 { return contactNom; }
    public void    setContactNom(String c)          { this.contactNom = c; }
    public String  getEmail()                      { return email; }
    public void    setEmail(String e)               { this.email = e; }
    public String  getTelephone()                  { return telephone; }
    public void    setTelephone(String t)           { this.telephone = t; }
    public String  getAdresse()                    { return adresse; }
    public void    setAdresse(String a)             { this.adresse = a; }
    public String  getConditionsLivraison()        { return conditionsLivraison; }
    public void    setConditionsLivraison(String c) { this.conditionsLivraison = c; }
    public boolean isActif()                       { return actif; }
    public void    setActif(boolean actif)          { this.actif = actif; }

    @Override
    public String toString() { return nomSociete; }
}