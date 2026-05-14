package tn.hanouti.livreur.util;


import tn.hanouti.util.DBConnection;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Inserts realistic test data into hanouti_db on first run.
 * Safe to call every startup — skips insertion if data already exists.
 */
public class TestDataSeeder {

    private static final Connection cnx = DBConnection.getInstance().getCnx();

    public static void seed() {
        try {
            createTablesIfNeeded();

            if (alreadySeeded()) {
                // Livreurs already exist — only reset deliveries and scores
                // so the app can be retested from a clean state each run
                resetLivraisonsEtScores();
                System.out.println("[Seeder] ✅ Livraisons et scores réinitialisés pour les tests.");
                return;
            }

            // First run — insert everything
            clearOldData();
            insertLivreurs();
            insertSuiviLivraisons();
            insertScores();

            System.out.println("[Seeder] ✅ Données de test insérées avec succès !");
        } catch (SQLException e) {
            System.err.println("[Seeder] Erreur : " + e.getMessage());
        }
    }

    // ─────────────────────────────────────────────
    // CREATE TABLES IF NOT EXISTS
    // ─────────────────────────────────────────────
    private static void createTablesIfNeeded() throws SQLException {
        Statement st = cnx.createStatement();

        st.executeUpdate(
            "CREATE TABLE IF NOT EXISTS Livreur (" +
            "  id_livreur           INT AUTO_INCREMENT PRIMARY KEY," +
            "  nom_livreur          VARCHAR(100) NOT NULL," +
            "  telephone            VARCHAR(20)  NOT NULL," +
            "  id_societe_livraison INT          NOT NULL DEFAULT 1," +
            "  disponibilite        BOOLEAN      NOT NULL DEFAULT TRUE," +
            "  date_naissance       DATE," +
            "  photo                VARCHAR(255)," +
            "  score                INT          NOT NULL DEFAULT 0," +
            "  genre_vehicule       VARCHAR(50)  NOT NULL DEFAULT 'Voiture'," +
            "  is_responsable       BOOLEAN      NOT NULL DEFAULT FALSE" +
            ")"
        );

        // Add is_responsable column if table already existed without it
        try {
            st.executeUpdate(
                "ALTER TABLE Livreur ADD COLUMN is_responsable BOOLEAN NOT NULL DEFAULT FALSE");
        } catch (SQLException ignored) {
            // Column already exists — ignore
        }

        st.executeUpdate(
            "CREATE TABLE IF NOT EXISTS Suivi_Livraison (" +
            "  id_suivi              INT AUTO_INCREMENT PRIMARY KEY," +
            "  id_commande           INT          NOT NULL," +
            "  id_livreur            INT          NOT NULL DEFAULT 0," +
            "  adresse_client        VARCHAR(255)," +
            "  localisation_actuelle VARCHAR(100)," +
            "  heure_estimee         VARCHAR(20)," +
            "  statut                VARCHAR(30)  NOT NULL DEFAULT 'EN_ATTENTE'" +
            ")"
        );

        st.executeUpdate(
            "CREATE TABLE IF NOT EXISTS Score (" +
            "  id_score          INT AUTO_INCREMENT PRIMARY KEY," +
            "  id_livreur        INT          NOT NULL," +
            "  note              INT          NOT NULL," +
            "  commentaire       VARCHAR(255)," +
            "  livre_dans_delai  BOOLEAN      NOT NULL DEFAULT TRUE," +
            "  date_evaluation   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP" +
            ")"
        );

        // Fix heure_estimee column type if it was created as DATETIME
        try {
            st.executeUpdate(
                "ALTER TABLE Suivi_Livraison MODIFY COLUMN heure_estimee VARCHAR(20)");
        } catch (SQLException ignored) {
            // Already correct type — ignore
        }

        st.close();
    }

    // ─────────────────────────────────────────────
    // CHECK IF ALREADY SEEDED WITH OUR TEST DATA
    // ─────────────────────────────────────────────
    private static boolean alreadySeeded() throws SQLException {
        Statement st = cnx.createStatement();
        ResultSet rs = st.executeQuery(
            "SELECT COUNT(*) FROM Livreur WHERE telephone IN " +
            "('22334455','55667788','33445566','22564847','44556677','66778899')");
        rs.next();
        int count = rs.getInt(1);
        rs.close();
        st.close();
        return count >= 6;
    }

    // ─────────────────────────────────────────────
    // RESET DELIVERIES AND SCORES (called on every startup after first run)
    // Keeps livreurs intact, resets livraisons + scores to test state
    // ─────────────────────────────────────────────
    private static void resetLivraisonsEtScores() throws SQLException {
        Statement st = cnx.createStatement();
        st.executeUpdate("SET FOREIGN_KEY_CHECKS = 0");
        st.executeUpdate("TRUNCATE TABLE Score");
        st.executeUpdate("TRUNCATE TABLE Suivi_Livraison");
        // Reset livreur availability to match the test data state
        st.executeUpdate("UPDATE Livreur SET disponibilite = TRUE,  is_responsable = FALSE");
        st.executeUpdate("UPDATE Livreur SET disponibilite = FALSE WHERE telephone IN ('22564847','44556677','66778899')");
        st.executeUpdate("UPDATE Livreur SET is_responsable = TRUE  WHERE telephone = '22334455'");
        st.executeUpdate("SET FOREIGN_KEY_CHECKS = 1");
        st.close();
        insertSuiviLivraisons();
        insertScores();
    }

    // ─────────────────────────────────────────────
    // CLEAR ALL EXISTING DATA BEFORE FRESH INSERT
    // ─────────────────────────────────────────────
    private static void clearOldData() throws SQLException {
        Statement st = cnx.createStatement();
        st.executeUpdate("SET FOREIGN_KEY_CHECKS = 0");
        st.executeUpdate("TRUNCATE TABLE Score");
        st.executeUpdate("TRUNCATE TABLE Suivi_Livraison");
        st.executeUpdate("TRUNCATE TABLE Livreur");
        st.executeUpdate("SET FOREIGN_KEY_CHECKS = 1");
        st.close();
        System.out.println("[Seeder] Anciennes données supprimées.");
    }

    // ─────────────────────────────────────────────
    // INSERT LIVREURS
    // ─────────────────────────────────────────────
    private static void insertLivreurs() throws SQLException {
        Statement st = cnx.createStatement();

        // 1 responsable livraison (is_responsable = TRUE)
        st.executeUpdate("INSERT INTO Livreur (nom_livreur, telephone, id_societe_livraison, disponibilite, date_naissance, photo, score, genre_vehicule, is_responsable) VALUES " +
            "('Ali Ben Salem',      '22334455', 1, TRUE,  '1998-03-15', NULL, 87, 'Voiture',      TRUE)");

        // 2 livreurs disponibles (is_responsable = FALSE)
        st.executeUpdate("INSERT INTO Livreur (nom_livreur, telephone, id_societe_livraison, disponibilite, date_naissance, photo, score, genre_vehicule, is_responsable) VALUES " +
            "('Sana Trabelsi',      '55667788', 1, TRUE,  '2000-07-22', NULL, 92, 'Voiture',      FALSE)");
        st.executeUpdate("INSERT INTO Livreur (nom_livreur, telephone, id_societe_livraison, disponibilite, date_naissance, photo, score, genre_vehicule, is_responsable) VALUES " +
            "('Mohamed Gharbi',     '33445566', 1, TRUE,  '1995-11-08', NULL, 74, 'Petit camion', FALSE)");

        // 2 en livraison (indisponibles — ont des commandes AFFECTEE)
        st.executeUpdate("INSERT INTO Livreur (nom_livreur, telephone, id_societe_livraison, disponibilite, date_naissance, photo, score, genre_vehicule, is_responsable) VALUES " +
            "('Saif Eddine Dhaoui', '22564847', 1, FALSE, '2002-01-10', NULL, 65, 'Voiture',      FALSE)");
        st.executeUpdate("INSERT INTO Livreur (nom_livreur, telephone, id_societe_livraison, disponibilite, date_naissance, photo, score, genre_vehicule, is_responsable) VALUES " +
            "('Youssef Mansouri',   '44556677', 1, FALSE, '1997-05-30', NULL, 81, 'Voiture',      FALSE)");

        // 1 indisponible sans livraison active
        st.executeUpdate("INSERT INTO Livreur (nom_livreur, telephone, id_societe_livraison, disponibilite, date_naissance, photo, score, genre_vehicule, is_responsable) VALUES " +
            "('Rim Belhaj',         '66778899', 1, FALSE, '1999-09-14', NULL, 55, 'Petit camion', FALSE)");

        st.close();
        System.out.println("[Seeder] 6 livreurs insérés (1 responsable, 5 livreurs).");
    }

    // ─────────────────────────────────────────────
    // INSERT SUIVI_LIVRAISON
    // ─────────────────────────────────────────────
    private static void insertSuiviLivraisons() throws SQLException {
        Statement st = cnx.createStatement();

        // EN_ATTENTE — not yet assigned (id_livreur = 0)
        st.executeUpdate("INSERT INTO Suivi_Livraison (id_commande, id_livreur, adresse_client, localisation_actuelle, heure_estimee, statut) VALUES " +
            "(201, 0, 'Rue de Marseille, Tunis 1000', NULL, NULL, 'EN_ATTENTE')");
        st.executeUpdate("INSERT INTO Suivi_Livraison (id_commande, id_livreur, adresse_client, localisation_actuelle, heure_estimee, statut) VALUES " +
            "(202, 0, 'Avenue Habib Bourguiba, Tunis 1001', NULL, NULL, 'EN_ATTENTE')");
        st.executeUpdate("INSERT INTO Suivi_Livraison (id_commande, id_livreur, adresse_client, localisation_actuelle, heure_estimee, statut) VALUES " +
            "(203, 0, 'Rue Ibn Khaldoun, Sfax 3000', NULL, NULL, 'EN_ATTENTE')");

        // AFFECTEE — livreur 4 (Saif Eddine) en route vers Ariana
        // localisation_actuelle = "lat,lon" used by the live tracking map
        st.executeUpdate("INSERT INTO Suivi_Livraison (id_commande, id_livreur, adresse_client, localisation_actuelle, heure_estimee, statut) VALUES " +
            "(204, 4, 'Cité El Ghazella, Ariana 2083', '36.8700,10.1950', '14h30', 'AFFECTEE')");

        // AFFECTEE — livreur 5 (Youssef) en route vers Les Berges du Lac
        st.executeUpdate("INSERT INTO Suivi_Livraison (id_commande, id_livreur, adresse_client, localisation_actuelle, heure_estimee, statut) VALUES " +
            "(205, 5, 'Rue du Lac Malaren, Les Berges du Lac 1053', '36.8320,10.2300', '15h00', 'AFFECTEE')");

        // LIVREE — historical deliveries
        st.executeUpdate("INSERT INTO Suivi_Livraison (id_commande, id_livreur, adresse_client, localisation_actuelle, heure_estimee, statut) VALUES " +
            "(101, 4, 'Avenue de la Liberté, Tunis 1002', NULL, '11h15', 'LIVREE')");
        st.executeUpdate("INSERT INTO Suivi_Livraison (id_commande, id_livreur, adresse_client, localisation_actuelle, heure_estimee, statut) VALUES " +
            "(102, 5, 'Rue Alain Savary, El Menzah 1004', NULL, '10h45', 'LIVREE')");
        st.executeUpdate("INSERT INTO Suivi_Livraison (id_commande, id_livreur, adresse_client, localisation_actuelle, heure_estimee, statut) VALUES " +
            "(103, 1, 'Rue de Palestine, Tunis 1002', NULL, '09h30', 'LIVREE')");
        st.executeUpdate("INSERT INTO Suivi_Livraison (id_commande, id_livreur, adresse_client, localisation_actuelle, heure_estimee, statut) VALUES " +
            "(104, 2, 'Avenue Mohamed V, Tunis 1001', NULL, '13h00', 'LIVREE')");
        st.executeUpdate("INSERT INTO Suivi_Livraison (id_commande, id_livreur, adresse_client, localisation_actuelle, heure_estimee, statut) VALUES " +
            "(105, 3, 'Zone Industrielle, Ben Arous 2013', NULL, '16h20', 'LIVREE')");

        st.close();
        System.out.println("[Seeder] 10 livraisons insérées (3 EN_ATTENTE, 2 AFFECTEE, 5 LIVREE).");
    }

    // ─────────────────────────────────────────────
    // INSERT SCORES
    // ─────────────────────────────────────────────
    private static void insertScores() throws SQLException {
        Statement st = cnx.createStatement();

        st.executeUpdate("INSERT INTO Score (id_livreur, note, commentaire, livre_dans_delai, date_evaluation) VALUES (1, 5, 'Très rapide et courtois !',       TRUE,  '2026-05-01 10:30:00')");
        st.executeUpdate("INSERT INTO Score (id_livreur, note, commentaire, livre_dans_delai, date_evaluation) VALUES (1, 4, 'Livraison correcte.',              TRUE,  '2026-05-05 14:00:00')");
        st.executeUpdate("INSERT INTO Score (id_livreur, note, commentaire, livre_dans_delai, date_evaluation) VALUES (2, 5, 'Parfait, à l''heure pile.',        TRUE,  '2026-05-02 11:00:00')");
        st.executeUpdate("INSERT INTO Score (id_livreur, note, commentaire, livre_dans_delai, date_evaluation) VALUES (2, 5, 'Excellent service.',               TRUE,  '2026-05-08 09:15:00')");
        st.executeUpdate("INSERT INTO Score (id_livreur, note, commentaire, livre_dans_delai, date_evaluation) VALUES (3, 3, 'Un peu en retard mais sympa.',     FALSE, '2026-05-03 16:45:00')");
        st.executeUpdate("INSERT INTO Score (id_livreur, note, commentaire, livre_dans_delai, date_evaluation) VALUES (4, 4, 'Bien, colis en bon état.',         TRUE,  '2026-05-04 12:30:00')");
        st.executeUpdate("INSERT INTO Score (id_livreur, note, commentaire, livre_dans_delai, date_evaluation) VALUES (5, 4, 'Rapide et professionnel.',         TRUE,  '2026-05-06 15:00:00')");
        st.executeUpdate("INSERT INTO Score (id_livreur, note, commentaire, livre_dans_delai, date_evaluation) VALUES (5, 3, 'Retard de 10 minutes.',            FALSE, '2026-05-09 17:30:00')");
        st.executeUpdate("INSERT INTO Score (id_livreur, note, commentaire, livre_dans_delai, date_evaluation) VALUES (6, 2, 'Colis abîmé à la livraison.',      FALSE, '2026-05-07 13:00:00')");

        st.close();
        System.out.println("[Seeder] 9 évaluations insérées.");
    }
}

