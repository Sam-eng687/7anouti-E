package projet.hanouti.module4.service;

import projet.hanouti.module4.dao.PaiementDAO;
import projet.hanouti.module4.interfaces.IService;
import projet.hanouti.module4.model.Paiement;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Service pour la gestion des paiements.
 * Implémente IService<Paiement> selon la structure du prof.
 */
public class PaiementService implements IService<Paiement> {

    private final PaiementDAO dao;

    public PaiementService() {
        PaiementDAO temp = null;
        try {
            temp = new PaiementDAO();
        } catch (SQLException e) {
            System.err.println("[PaiementService] Erreur connexion DB: " + e.getMessage());
        }
        this.dao = temp;
    }

    @Override
    public void addEntity(Paiement paiement) {
        try {
            dao.addPaiement(paiement);
            System.out.println("[PaiementService] Paiement ajouté.");
        } catch (SQLException e) {
            System.err.println("[PaiementService] Erreur addEntity: " + e.getMessage());
        }
    }

    @Override
    public void deleteEntity(Paiement paiement) {
        try {
            dao.annulerPaiement(paiement.getReferenceTransaction());
            System.out.println("[PaiementService] Paiement annulé.");
        } catch (SQLException e) {
            System.err.println("[PaiementService] Erreur deleteEntity: " + e.getMessage());
        }
    }

    @Override
    public void updateEntity(int id, Paiement paiement) {
        try {
            dao.updateStatut(paiement.getReferenceTransaction(), paiement.getStatut());
            System.out.println("[PaiementService] Statut mis à jour : " + paiement.getStatut());
        } catch (SQLException e) {
            System.err.println("[PaiementService] Erreur updateEntity: " + e.getMessage());
        }
    }

    @Override
    public List<Paiement> getData() {
        try {
            return dao.getHistoriqueAll();
        } catch (SQLException e) {
            System.err.println("[PaiementService] Erreur getData: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    public List<Paiement> getPaiementsByUser(int userId) {
        try {
            return dao.getHistoriqueByUser(userId);
        } catch (SQLException e) {
            System.err.println("[PaiementService] Erreur getPaiementsByUser: " + e.getMessage());
            return new ArrayList<>();
        }
    }
}