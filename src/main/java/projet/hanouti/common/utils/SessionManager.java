package projet.hanouti.common.utils;

import projet.hanouti.user_auth.entities.User;

/**
 * SessionManager — Singleton
 * Garde l'utilisateur connecte en memoire pendant toute la duree de l'application.
 * Accessible depuis n'importe quelle classe via SessionManager.getInstance()
 */
public class SessionManager {

    private static SessionManager instance;
    private User connectedUser;

    // Constructeur prive — singleton
    private SessionManager() {}

    public static SessionManager getInstance() {
        if (instance == null) {
            instance = new SessionManager();
        }
        return instance;
    }

    // =================== SETTERS / GETTERS ===================

    /** Enregistrer l'utilisateur connecte en session */
    public void setConnectedUser(User user) {
        this.connectedUser = user;
        System.out.println("[Session] Utilisateur connecte: "
                + (user != null ? user.getNom() + " " + user.getPrenom()
                + " | Role: " + user.getRole() : "null"));
    }

    /** Recuperer l'utilisateur connecte */
    public User getConnectedUser() {
        return connectedUser;
    }

    /** Verifier si une session est active */
    public boolean isLoggedIn() {
        return connectedUser != null;
    }

    /** Recuperer le nom complet de l'utilisateur connecte */
    public String getFullName() {
        if (connectedUser == null) return "";
        return connectedUser.getNom() + " " + connectedUser.getPrenom();
    }

    /** Deconnecter l'utilisateur (vider la session) */
    public void logout() {
        System.out.println("[Session] Deconnexion de: "
                + (connectedUser != null ? connectedUser.getE_mail() : "?"));
        connectedUser = null;
    }
    private boolean darkMode = true;

    public boolean isDarkMode() {
        return darkMode;
    }

    public void setDarkMode(boolean darkMode) {
        this.darkMode = darkMode;
    }
}