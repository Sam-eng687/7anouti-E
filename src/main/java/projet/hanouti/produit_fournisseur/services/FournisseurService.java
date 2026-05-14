package projet.hanouti.produit_fournisseur.services;

import projet.hanouti.produit_fournisseur.entities.Fournisseur;
import projet.hanouti.produit_fournisseur.interfaces.IService;
import projet.hanouti.produit_fournisseur.utils.MyConnection;
import projet.hanouti.produit_fournisseur.utils.SessionManager;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class FournisseurService implements IService<Fournisseur> {

    private final Connection cnx = MyConnection.getInstance().getCnx();

    @Override
    public void addEntity(Fournisseur f) {
        String req = "INSERT INTO Fournisseur (id_vendeur, nom_societe, contact_nom, email, telephone, adresse, conditions_livraison, actif) VALUES (?,?,?,?,?,?,?,?)";
        try {
            PreparedStatement pst = cnx.prepareStatement(req);
            pst.setInt    (1, f.getIdVendeur());
            pst.setString (2, f.getNomSociete());
            pst.setString (3, f.getContactNom());
            pst.setString (4, f.getEmail());
            pst.setString (5, f.getTelephone());
            pst.setString (6, f.getAdresse());
            pst.setString (7, f.getConditionsLivraison());
            pst.setBoolean(8, f.isActif());
            pst.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    public boolean existsAlready(String nomSociete, String email, int idVendeur) {
        String req = "SELECT COUNT(*) FROM Fournisseur WHERE nom_societe=? AND email=? AND id_vendeur=?";
        try {
            PreparedStatement pst = cnx.prepareStatement(req);
            pst.setString(1, nomSociete);
            pst.setString(2, email);
            pst.setInt   (3, idVendeur);
            ResultSet rs = pst.executeQuery();
            if (rs.next()) return rs.getInt(1) > 0;
        } catch (SQLException e) { System.out.println(e.getMessage()); }
        return false;
    }

    @Override
    public void deleteEntity(Fournisseur f) {
        String req = "DELETE FROM Fournisseur WHERE id_fournisseur = ? AND id_vendeur = ?";
        try {
            PreparedStatement pst = cnx.prepareStatement(req);
            pst.setInt(1, f.getIdFournisseur());
            pst.setInt(2, f.getIdVendeur());
            pst.executeUpdate();
        } catch (SQLException e) { System.out.println(e.getMessage()); }
    }

    @Override
    public void updateEntity(int id, Fournisseur f) {
        String req = "UPDATE Fournisseur SET nom_societe=?, contact_nom=?, email=?, telephone=?, adresse=?, conditions_livraison=?, actif=? WHERE id_fournisseur=? AND id_vendeur=?";
        try {
            PreparedStatement pst = cnx.prepareStatement(req);
            pst.setString (1, f.getNomSociete());
            pst.setString (2, f.getContactNom());
            pst.setString (3, f.getEmail());
            pst.setString (4, f.getTelephone());
            pst.setString (5, f.getAdresse());
            pst.setString (6, f.getConditionsLivraison());
            pst.setBoolean(7, f.isActif());
            pst.setInt    (8, id);
            pst.setInt    (9, SessionManager.getCurrentVendeurId());
            pst.executeUpdate();
        } catch (SQLException e) { System.out.println(e.getMessage()); }
    }

    @Override
    public List<Fournisseur> getData() {
        List<Fournisseur> list = new ArrayList<>();
        String req = "SELECT * FROM Fournisseur WHERE id_vendeur = ?";
        try {
            PreparedStatement pst = cnx.prepareStatement(req);
            pst.setInt(1, SessionManager.getCurrentVendeurId());
            ResultSet rs = pst.executeQuery();
            while (rs.next()) { list.add(map(rs)); }
        } catch (SQLException e) { System.out.println(e.getMessage()); }
        return list;
    }

    private Fournisseur map(ResultSet rs) throws SQLException {
        Fournisseur f = new Fournisseur();
        f.setIdFournisseur      (rs.getInt    ("id_fournisseur"));
        f.setIdVendeur          (rs.getInt    ("id_vendeur"));
        f.setNomSociete         (rs.getString ("nom_societe"));
        f.setContactNom         (rs.getString ("contact_nom"));
        f.setEmail              (rs.getString ("email"));
        f.setTelephone          (rs.getString ("telephone"));
        f.setAdresse            (rs.getString ("adresse"));
        f.setConditionsLivraison(rs.getString ("conditions_livraison"));
        f.setActif              (rs.getBoolean("actif"));
        return f;
    }
}