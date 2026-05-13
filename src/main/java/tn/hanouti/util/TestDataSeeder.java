package tn.hanouti.util;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Inserts realistic test data into hanouti_db on first run.
 * Uses the team's 'livraisons' table schema.
 * Resets livraisons to original state on every startup for testing.
 */
public class TestDataSeeder {

    private static final Connection cnx = DBConnection.getInstance().getCnx();

    public static void seed() {
        try {
            createTablesIfNeeded();
            addExtraColumnsIfNeeded();

            if (alreadySeeded()) {
                System.out.println("[Seeder] Données de test déjà présentes — skip.");
                resetLivraisons();
                return;
            }

            clearOldData();
            insertLivreurs();
            insertLivraisons();
            insertScores();

            System.out.println("[Seeder] ✅ Données de test insérées avec succès !");
        } catch (SQLException e) {
            System.err.println("[Seeder] Erreur : " + e.getMessage());
        }
    }

    // ─────────────────────────────────────────────
    // CREATE TABLES
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
            "  genre_vehicule       VARCHAR(50)  NOT NULL DEFAULT 'Voiture'" +
            ")"
        );

        // Team's livraisons table (without FK constraints for standalone testing)
        st.executeUpdate(
            "CREATE TABLE IF NOT EXISTS livraisons (" +
            "  id_livraison         INT AUTO_INCREMENT PRIMARY KEY," +
            "  id_commande          INT            NOT NULL," +
            "  numero_commande      VARCHAR(50)    NOT NULL," +
            "  id_societe           INT            NOT NULL DEFAULT 1," +
            "  mode_assignation     ENUM('AUTOMATIQUE','MANUELLE') NOT NULL DEFAULT 'MANUELLE'," +
            "  statut_livraison     ENUM('ASSIGNEE','EN_COURS','LIVREE','ANNULEE') NOT NULL DEFAULT 'ASSIGNEE'," +
            "  date_assignation     DATETIME       NULL," +
            "  date_debut_livraison DATETIME       NULL," +
            "  date_livraison       DATETIME       NULL" +
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

        st.close();
    }

    // ─────────────────────────────────────────────
    // ADD EXTRA COLUMNS needed by this module
    // (id_livreur, adresse_client, localisation_actuelle)
    // ─────────────────────────────────────────────
    private static void addExtraColumnsIfNeeded() throws SQLException {
        Statement st = cnx.createStatement();
        String[] alters = {
            "ALTER TABLE livraisons ADD COLUMN id_livreur INT NOT NULL DEFAULT 0",
            "ALTER TABLE livraisons ADD COLUMN adresse_client VARCHAR(255)",
            "ALTER TABLE livraisons ADD COLUMN localisation_actuelle VARCHAR(100)"
        };
        for (String sql : alters) {
            try { st.executeUpdate(sql); }
            catch (SQLException ignored) {} // column already exists
        }
        st.close();
    }

    // ─────────────────────────────────────────────
    // RESET LIVRAISONS every startup
    // ─────────────────────────────────────────────
    public static void resetLivraisons() throws SQLException {
        Statement st = cnx.createStatement();
        st.executeUpdate("DELETE FROM livraisons");
        st.executeUpdate("ALTER TABLE livraisons AUTO_INCREMENT = 1");

        // ASSIGNEE (EN_ATTENTE)
        st.executeUpdate("INSERT INTO livraisons (id_commande, numero_commande, id_societe, mode_assignation, statut_livraison, id_livreur, adresse_client, localisation_actuelle, date_assignation) VALUES (201, 'CMD-201', 1, 'MANUELLE', 'ASSIGNEE', 0, 'Rue de Marseille, Tunis 1000', NULL, NOW())");
        st.executeUpdate("INSERT INTO livraisons (id_commande, numero_commande, id_societe, mode_assignation, statut_livraison, id_livreur, adresse_client, localisation_actuelle, date_assignation) VALUES (202, 'CMD-202', 1, 'MANUELLE', 'ASSIGNEE', 0, 'Avenue Habib Bourguiba, Tunis 1001', NULL, NOW())");
        st.executeUpdate("INSERT INTO livraisons (id_commande, numero_commande, id_societe, mode_assignation, statut_livraison, id_livreur, adresse_client, localisation_actuelle, date_assignation) VALUES (203, 'CMD-203', 1, 'AUTOMATIQUE', 'ASSIGNEE', 0, 'Rue Ibn Khaldoun, Sfax 3000', NULL, NOW())");

        // EN_COURS (AFFECTEE) — with GPS
        st.executeUpdate("INSERT INTO livraisons (id_commande, numero_commande, id_societe, mode_assignation, statut_livraison, id_livreur, adresse_client, localisation_actuelle, date_assignation, date_debut_livraison, date_livraison) VALUES (204, 'CMD-204', 1, 'MANUELLE', 'EN_COURS', 4, 'Cité El Ghazella, Ariana 2083', '36.8700,10.1950', NOW(), NOW(), DATE_ADD(NOW(), INTERVAL 30 MINUTE))");
        st.executeUpdate("INSERT INTO livraisons (id_commande, numero_commande, id_societe, mode_assignation, statut_livraison, id_livreur, adresse_client, localisation_actuelle, date_assignation, date_debut_livraison, date_livraison) VALUES (205, 'CMD-205', 1, 'MANUELLE', 'EN_COURS', 5, 'Rue du Lac Malaren, Les Berges du Lac 1053', '36.8320,10.2300', NOW(), NOW(), DATE_ADD(NOW(), INTERVAL 45 MINUTE))");

        // LIVREE — historical
        st.executeUpdate("INSERT INTO livraisons (id_commande, numero_commande, id_societe, mode_assignation, statut_livraison, id_livreur, adresse_client, date_assignation, date_debut_livraison, date_livraison) VALUES (101, 'CMD-101', 1, 'MANUELLE', 'LIVREE', 4, 'Avenue de la Liberté, Tunis 1002', NOW(), NOW(), NOW())");
        st.executeUpdate("INSERT INTO livraisons (id_commande, numero_commande, id_societe, mode_assignation, statut_livraison, id_livreur, adresse_client, date_assignation, date_debut_livraison, date_livraison) VALUES (102, 'CMD-102', 1, 'MANUELLE', 'LIVREE', 5, 'Rue Alain Savary, El Menzah 1004', NOW(), NOW(), NOW())");
        st.executeUpdate("INSERT INTO livraisons (id_commande, numero_commande, id_societe, mode_assignation, statut_livraison, id_livreur, adresse_client, date_assignation, date_debut_livraison, date_livraison) VALUES (103, 'CMD-103', 1, 'AUTOMATIQUE', 'LIVREE', 1, 'Rue de Palestine, Tunis 1002', NOW(), NOW(), NOW())");
        st.executeUpdate("INSERT INTO livraisons (id_commande, numero_commande, id_societe, mode_assignation, statut_livraison, id_livreur, adresse_client, date_assignation, date_debut_livraison, date_livraison) VALUES (104, 'CMD-104', 1, 'AUTOMATIQUE', 'LIVREE', 2, 'Avenue Mohamed V, Tunis 1001', NOW(), NOW(), NOW())");
        st.executeUpdate("INSERT INTO livraisons (id_commande, numero_commande, id_societe, mode_assignation, statut_livraison, id_livreur, adresse_client, date_assignation, date_debut_livraison, date_livraison) VALUES (105, 'CMD-105', 1, 'MANUELLE', 'LIVREE', 3, 'Zone Industrielle, Ben Arous 2013', NOW(), NOW(), NOW())");

        // Reset livreur availability
        st.executeUpdate("UPDATE Livreur SET disponibilite = TRUE  WHERE telephone IN ('22334455','55667788','33445566')");
        st.executeUpdate("UPDATE Livreur SET disponibilite = FALSE WHERE telephone IN ('22564847','44556677','66778899')");

        st.close();
        System.out.println("[Seeder] ✅ Livraisons réinitialisées — prêt pour les tests !");
    }

    // ─────────────────────────────────────────────
    // CHECK IF ALREADY SEEDED
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
    // CLEAR OLD DATA
    // ─────────────────────────────────────────────
    private static void clearOldData() throws SQLException {
        Statement st = cnx.createStatement();
        st.executeUpdate("SET FOREIGN_KEY_CHECKS = 0");
        st.executeUpdate("TRUNCATE TABLE Score");
        st.executeUpdate("TRUNCATE TABLE livraisons");
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
        st.executeUpdate("INSERT INTO Livreur (nom_livreur, telephone, id_societe_livraison, disponibilite, date_naissance, photo, score, genre_vehicule) VALUES ('Ali Ben Salem',      '22334455', 1, TRUE,  '1998-03-15', NULL, 87, 'Voiture')");
        st.executeUpdate("INSERT INTO Livreur (nom_livreur, telephone, id_societe_livraison, disponibilite, date_naissance, photo, score, genre_vehicule) VALUES ('Sana Trabelsi',      '55667788', 1, TRUE,  '2000-07-22', NULL, 92, 'Voiture')");
        st.executeUpdate("INSERT INTO Livreur (nom_livreur, telephone, id_societe_livraison, disponibilite, date_naissance, photo, score, genre_vehicule) VALUES ('Mohamed Gharbi',     '33445566', 1, TRUE,  '1995-11-08', NULL, 74, 'Petit camion')");
        st.executeUpdate("INSERT INTO Livreur (nom_livreur, telephone, id_societe_livraison, disponibilite, date_naissance, photo, score, genre_vehicule) VALUES ('Saif Eddine Dhaoui', '22564847', 1, FALSE, '2002-01-10', NULL, 65, 'Voiture')");
        st.executeUpdate("INSERT INTO Livreur (nom_livreur, telephone, id_societe_livraison, disponibilite, date_naissance, photo, score, genre_vehicule) VALUES ('Youssef Mansouri',   '44556677', 1, FALSE, '1997-05-30', NULL, 81, 'Voiture')");
        st.executeUpdate("INSERT INTO Livreur (nom_livreur, telephone, id_societe_livraison, disponibilite, date_naissance, photo, score, genre_vehicule) VALUES ('Rim Belhaj',         '66778899', 1, FALSE, '1999-09-14', NULL, 55, 'Petit camion')");
        st.close();
        System.out.println("[Seeder] 6 livreurs insérés.");
    }

    // ─────────────────────────────────────────────
    // INSERT LIVRAISONS
    // ─────────────────────────────────────────────
    private static void insertLivraisons() throws SQLException {
        resetLivraisons();
    }

    // ─────────────────────────────────────────────
    // INSERT SCORES
    // ─────────────────────────────────────────────
    private static void insertScores() throws SQLException {
        Statement st = cnx.createStatement();
        st.executeUpdate("INSERT INTO Score (id_livreur, note, commentaire, livre_dans_delai, date_evaluation) VALUES (1, 5, 'Très rapide et courtois !',   TRUE,  '2026-05-01 10:30:00')");
        st.executeUpdate("INSERT INTO Score (id_livreur, note, commentaire, livre_dans_delai, date_evaluation) VALUES (1, 4, 'Livraison correcte.',          TRUE,  '2026-05-05 14:00:00')");
        st.executeUpdate("INSERT INTO Score (id_livreur, note, commentaire, livre_dans_delai, date_evaluation) VALUES (2, 5, 'Parfait, à l''heure pile.',    TRUE,  '2026-05-02 11:00:00')");
        st.executeUpdate("INSERT INTO Score (id_livreur, note, commentaire, livre_dans_delai, date_evaluation) VALUES (2, 5, 'Excellent service.',           TRUE,  '2026-05-08 09:15:00')");
        st.executeUpdate("INSERT INTO Score (id_livreur, note, commentaire, livre_dans_delai, date_evaluation) VALUES (3, 3, 'Un peu en retard mais sympa.', FALSE, '2026-05-03 16:45:00')");
        st.executeUpdate("INSERT INTO Score (id_livreur, note, commentaire, livre_dans_delai, date_evaluation) VALUES (4, 4, 'Bien, colis en bon état.',     TRUE,  '2026-05-04 12:30:00')");
        st.executeUpdate("INSERT INTO Score (id_livreur, note, commentaire, livre_dans_delai, date_evaluation) VALUES (5, 4, 'Rapide et professionnel.',     TRUE,  '2026-05-06 15:00:00')");
        st.executeUpdate("INSERT INTO Score (id_livreur, note, commentaire, livre_dans_delai, date_evaluation) VALUES (5, 3, 'Retard de 10 minutes.',        FALSE, '2026-05-09 17:30:00')");
        st.executeUpdate("INSERT INTO Score (id_livreur, note, commentaire, livre_dans_delai, date_evaluation) VALUES (6, 2, 'Colis abîmé à la livraison.',  FALSE, '2026-05-07 13:00:00')");
        st.close();
        System.out.println("[Seeder] 9 évaluations insérées.");
    }
}
