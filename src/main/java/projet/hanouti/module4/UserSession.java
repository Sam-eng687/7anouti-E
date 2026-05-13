package com.hanouti.hanoutiem4;

/**
 * Singleton that holds shared app state across all screens.
 * — dark/light mode (persists across navigation)
 * — connected user info
 *
 * FIX BUG-04 : logout() remet maintenant userId = 0 (non connecté)
 *              au lieu de userId = 1 (qui pointait sur Samar).
 */
public class UserSession {

    private static UserSession instance;

    private boolean darkMode    = false;
    private String  userName    = "Samar Zeidi";
    private String  userEmail   = "zeidisamar3@gmail.com";
    private int     userId      = 1;

    private UserSession() {}

    public static UserSession getInstance() {
        if (instance == null) instance = new UserSession();
        return instance;
    }

    // ── dark mode ─────────────────────────────────────────────
    public boolean isDarkMode()              { return darkMode; }
    public void    setDarkMode(boolean dark) { this.darkMode = dark; }

    // ── user info ─────────────────────────────────────────────
    public String getUserName()              { return userName; }
    public void   setUserName(String name)   { this.userName = name; }

    public String getUserEmail()             { return userEmail; }
    public void   setUserEmail(String email) { this.userEmail = email; }

    public int  getUserId()                  { return userId; }
    public void setUserId(int id)            { this.userId = id; }

    /** FIX BUG-04 : reset complet — userId = 0 signifie "non connecté" */
    public void logout() {
        darkMode  = false;
        userName  = "";
        userEmail = "";
        userId    = 0;   // ← était 1, ce qui chargeait les données de Samar après logout
    }
}