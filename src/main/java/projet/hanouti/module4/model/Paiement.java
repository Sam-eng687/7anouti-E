package projet.hanouti.module4.model;
import java.util.Date;
public class Paiement {
    private int paiementId;
    private int commandeId;
    private double montant;
    private String methode;
    private String statut;
    private Date datePaiement;
    private String referenceTransaction;

    // Constructor
    public Paiement(int commandeId, double montant, String methode, String statut) {
        this.commandeId = commandeId;
        this.montant = montant;
        this.methode = methode;
        this.statut = statut;
    }

    // Getters and Setters
    public int getPaiementId() { return paiementId; }
    public void setPaiementId(int paiementId) { this.paiementId = paiementId; }

    public int getCommandeId() { return commandeId; }
    public void setCommandeId(int commandeId) { this.commandeId = commandeId; }

    public double getMontant() { return montant; }
    public void setMontant(double montant) { this.montant = montant; }

    public String getMethode() { return methode; }
    public void setMethode(String methode) { this.methode = methode; }

    public String getStatut() { return statut; }
    public void setStatut(String statut) { this.statut = statut; }

    public Date getDatePaiement() { return datePaiement; }
    public void setDatePaiement(Date datePaiement) { this.datePaiement = datePaiement; }

    public String getReferenceTransaction() { return referenceTransaction; }
    public void setReferenceTransaction(String referenceTransaction) {
        this.referenceTransaction = referenceTransaction;
    }
}