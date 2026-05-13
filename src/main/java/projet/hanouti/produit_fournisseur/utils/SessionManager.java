package projet.hanouti.produit_fournisseur.utils;

import projet.hanouti.produit_fournisseur.entities.Acheteur;
import projet.hanouti.produit_fournisseur.entities.Vendeur;
import projet.hanouti.user_auth.entities.User;

public final class SessionManager {
    private SessionManager() {
    }

    public static int getCurrentVendeurId() {
        return projet.hanouti.common.utils.SessionManager.getCurrentUserId();
    }

    public static int getCurrentAcheteurId() {
        return projet.hanouti.common.utils.SessionManager.getCurrentUserId();
    }

    public static Vendeur getCurrentVendeur() {
        User user = projet.hanouti.common.utils.SessionManager.getCurrentUser();
        if (user == null) return null;

        Vendeur vendeur = new Vendeur();
        vendeur.setIdVendeur(user.getId());
        vendeur.setNom(value(user.getNom()));
        vendeur.setPrenom(value(user.getPrenom()));
        vendeur.setEmail(value(user.getE_mail()));
        return vendeur;
    }

    public static Acheteur getCurrentAcheteur() {
        User user = projet.hanouti.common.utils.SessionManager.getCurrentUser();
        if (user == null) return null;

        Acheteur acheteur = new Acheteur();
        acheteur.setIdAcheteur(user.getId());
        acheteur.setNom(value(user.getNom()));
        acheteur.setPrenom(value(user.getPrenom()));
        acheteur.setEmail(value(user.getE_mail()));
        return acheteur;
    }

    private static String value(String value) {
        return value == null ? "" : value;
    }
}
