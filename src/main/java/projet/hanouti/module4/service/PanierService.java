package com.hanouti.hanoutiem4.service;

import com.hanouti.hanoutiem4.dao.PanierDAO;
import com.hanouti.hanoutiem4.interfaces.IService;
import com.hanouti.hanoutiem4.model.Panier;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Service pour la gestion du panier.
 * Implémente IService<Panier> selon la structure du prof.
 */
public class PanierService implements IService<Panier> {

    private final PanierDAO dao;

    public PanierService() {
        PanierDAO temp = null;
        try {
            temp = new PanierDAO();
        } catch (Exception e) {
            System.err.println("[PanierService] Erreur connexion DB: " + e.getMessage());
        }
        this.dao = temp;
    }

    @Override
    public void addEntity(Panier panier) {
        try {
            dao.addToCart(panier);
            System.out.println("[PanierService] Article ajouté : " + panier.getNomProduit());
        } catch (SQLException e) {
            System.err.println("[PanierService] Erreur addEntity: " + e.getMessage());
        }
    }

    @Override
    public void deleteEntity(Panier panier) {
        try {
            dao.removeFromCart(panier.getPanierId());
            System.out.println("[PanierService] Article retiré.");
        } catch (SQLException e) {
            System.err.println("[PanierService] Erreur deleteEntity: " + e.getMessage());
        }
    }

    @Override
    public void updateEntity(int id, Panier panier) {
        try {
            dao.updateQuantite(id, panier.getQuantite());
            System.out.println("[PanierService] Quantité mise à jour : " + panier.getQuantite());
        } catch (SQLException e) {
            System.err.println("[PanierService] Erreur updateEntity: " + e.getMessage());
        }
    }

    @Override
    public List<Panier> getData() {
        return new ArrayList<>();
    }

    public List<Panier> getCartItems(int userId) {
        try {
            return dao.getCartItems(userId);
        } catch (SQLException e) {
            System.err.println("[PanierService] Erreur getCartItems: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    public void clearCart(int userId) {
        try {
            dao.clearCart(userId);
        } catch (SQLException e) {
            System.err.println("[PanierService] Erreur clearCart: " + e.getMessage());
        }
    }
}