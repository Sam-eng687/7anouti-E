package projet.hanouti.module4.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Singleton connexion MySQL — version sécurisée.
 *
 * FIX BUG-08 (v2) — 4 problèmes corrigés :
 *   1. getInstance() synchronized → thread-safe (QR thread + JavaFX thread)
 *   2. getConnection() synchronized + isValid(2) ping MySQL
 *   3. Reconnexion si isValid()/isClosed() lève exception (connexion zombie)
 *   4. URL avec autoReconnect, UTF-8, serverTimezone pour MySQL 5.7/8
 */
public class DBConnection {

    private static DBConnection instance;
    private Connection connection;

    private static final String URL =
            "jdbc:mysql://localhost:3306/7anouti_e" +
                    "?autoReconnect=true&useSSL=false&serverTimezone=UTC" +
                    "&allowPublicKeyRetrieval=true&characterEncoding=UTF-8";
    private static final String USER     = "root";
    private static final String PASSWORD = "";

    private DBConnection() throws SQLException {
        connection = DriverManager.getConnection(URL, USER, PASSWORD);
    }

    /** synchronized → une seule instance même en multi-thread */
    public static synchronized DBConnection getInstance() throws SQLException {
        if (instance == null) instance = new DBConnection();
        return instance;
    }

    /**
     * Retourne une connexion garantie valide.
     * isValid(2) envoie SELECT 1 à MySQL (timeout 2s) — détecte les connexions
     * "zombie" (fermées côté serveur après 8h d'inactivité).
     */
    public synchronized Connection getConnection() throws SQLException {
        try {
            if (connection == null || connection.isClosed() || !connection.isValid(2)) {
                connection = DriverManager.getConnection(URL, USER, PASSWORD);
            }
        } catch (SQLException e) {
            // isValid()/isClosed() a échoué → connexion vraiment morte, on recrée
            connection = DriverManager.getConnection(URL, USER, PASSWORD);
        }
        return connection;
    }
}