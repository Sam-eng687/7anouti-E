package projet.hanouti.AIachat.entities;

import java.time.LocalDateTime;

public class InteractionUtilisateur {

    private int idInteraction;
    private int idAcheteur;
    private int idProduit;
    private String typeInteraction; // VIEW | CLICK_PRODUCT | ADD_TO_CART | BOUGHT
    private int nbInteraction;
    private LocalDateTime lastInteraction;

    public InteractionUtilisateur() {}

    public InteractionUtilisateur(int idAcheteur, int idProduit, String typeInteraction) {
        this.idAcheteur = idAcheteur;
        this.idProduit = idProduit;
        this.typeInteraction = typeInteraction;
        this.nbInteraction = 1;
        this.lastInteraction = LocalDateTime.now();
    }

    public InteractionUtilisateur(int idInteraction, int idAcheteur, int idProduit,
                                  String typeInteraction, int nbInteraction, LocalDateTime lastInteraction) {
        this.idInteraction = idInteraction;
        this.idAcheteur = idAcheteur;
        this.idProduit = idProduit;
        this.typeInteraction = typeInteraction;
        this.nbInteraction = nbInteraction;
        this.lastInteraction = lastInteraction;
    }

    public int getIdInteraction()            { return idInteraction; }
    public void setIdInteraction(int v)      { this.idInteraction = v; }

    public int getIdAcheteur()               { return idAcheteur; }
    public void setIdAcheteur(int v)         { this.idAcheteur = v; }

    public int getIdProduit()                { return idProduit; }
    public void setIdProduit(int v)          { this.idProduit = v; }

    public String getTypeInteraction()       { return typeInteraction; }
    public void setTypeInteraction(String v) { this.typeInteraction = v; }

    public int getNbInteraction()            { return nbInteraction; }
    public void setNbInteraction(int v)      { this.nbInteraction = v; }

    public LocalDateTime getLastInteraction()          { return lastInteraction; }
    public void setLastInteraction(LocalDateTime v)    { this.lastInteraction = v; }

    @Override
    public String toString() {
        return "InteractionUtilisateur{" +
                "idInteraction=" + idInteraction +
                ", idAcheteur=" + idAcheteur +
                ", idProduit=" + idProduit +
                ", typeInteraction='" + typeInteraction + '\'' +
                ", nbInteraction=" + nbInteraction +
                ", lastInteraction=" + lastInteraction +
                '}';
    }
}

