-- =============================================================================
-- 7anouti_e — schéma complet (MySQL 8.x recommandé, compatible 5.7+)
-- Exécution : mysql -u root -p < sql/7anouti_e_full_schema.sql
-- Ou : ouvrir ce fichier dans MySQL Workbench et exécuter tout le script.
--
-- Regroupe : auth (users), marketing / Wejden, AI achat, panier,
--            tables optionnelles module Wejden (rapport / feedback).
-- Aligné avec MyBD (jdbc:mysql://.../7anouti_e par défaut).
-- =============================================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

CREATE DATABASE IF NOT EXISTS `7anouti_e`
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;

USE `7anouti_e`;

-- ----------------------------------------------------------------------------- users
-- UserCRUD, Query.java, VendeurService (role vendeur / fournisseur)
-- ----------------------------------------------------------------------------- 
CREATE TABLE IF NOT EXISTS `users` (
    `id`                  INT AUTO_INCREMENT PRIMARY KEY,
    `nom`                 VARCHAR(120)  NOT NULL DEFAULT '',
    `prenom`              VARCHAR(120)  NOT NULL DEFAULT '',
    `date_naiss`          VARCHAR(32)   NULL COMMENT 'Stocké en texte côté app',
    `e_mail`              VARCHAR(255)  NOT NULL,
    `num_tel`             VARCHAR(32)   NULL,
    `mot_de_pass`         VARCHAR(128)  NOT NULL COMMENT 'Hash SHA-256 hex (64 chars)',
    `image`               VARCHAR(512)  NULL,
    `role`                VARCHAR(32)   NOT NULL DEFAULT 'acheteur'
        COMMENT 'admin, acheteur, vendeur, livreur, fournisseur',
    `status`              VARCHAR(32)   NOT NULL DEFAULT 'Unbanned'
        COMMENT 'Banned, Unbanned',
    `face_id_enabled`     TINYINT(1)    NOT NULL DEFAULT 0,
    `face_image_path`     VARCHAR(512)  NULL,
    `adresse`             TEXT          NULL,
    `entreprise`          VARCHAR(255)  NULL,
    `type_produit`        VARCHAR(255)  NULL,
    `vehicule`            VARCHAR(120)  NULL,
    `permis`              VARCHAR(120)  NULL,
    `zone_livraison`      VARCHAR(255)  NULL,
    `email_verified`      TINYINT(1)    NOT NULL DEFAULT 0,
    UNIQUE KEY `uq_users_email` (`e_mail`),
    KEY `idx_users_role` (`role`),
    KEY `idx_users_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------------------------------------------------------- produit
-- ----------------------------------------------------------------------------- 
CREATE TABLE IF NOT EXISTS `produit` (
    `id_produit`      INT AUTO_INCREMENT PRIMARY KEY,
    `nom`             VARCHAR(255) NOT NULL,
    `description`     TEXT NULL,
    `prix`            DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    `quantite_stock`  INT NOT NULL DEFAULT 0,
    `seuil_alerte`    INT NOT NULL DEFAULT 5,
    `categorie`       VARCHAR(64) NOT NULL DEFAULT 'AUTRE',
    `moyenne`        DECIMAL(4,2) NULL,
    `image`           VARCHAR(512) NULL,
    `statut`          VARCHAR(32) NOT NULL DEFAULT 'ACTIF',
    KEY `idx_produit_stock` (`quantite_stock`),
    KEY `idx_produit_statut` (`statut`),
    KEY `idx_produit_categorie` (`categorie`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------------------------------------------------------- conseils_ia
-- ----------------------------------------------------------------------------- 
CREATE TABLE IF NOT EXISTS `conseils_ia` (
    `id_conseil`      INT NOT NULL AUTO_INCREMENT,
    `type`            ENUM('Promotion','Destockage','Bundle') NOT NULL,
    `urgence`         VARCHAR(20) NOT NULL,
    `description`     TEXT NOT NULL,
    `titre_acheteur`  VARCHAR(120) DEFAULT NULL,
    `score`           INT DEFAULT '0',
    `discount`        FLOAT DEFAULT NULL,
    `etat`            ENUM('ACCEPTE','IGNORE','EN_ATTENTE') DEFAULT 'EN_ATTENTE',
    `id_produit`      INT DEFAULT NULL,
    `date_genere`     DATETIME DEFAULT CURRENT_TIMESTAMP,
    `date_ignore`     DATETIME DEFAULT NULL,
    `date_accepte`    DATETIME DEFAULT NULL,
    `date_expiration` DATETIME DEFAULT NULL,
    PRIMARY KEY (`id_conseil`),
    KEY `id_produit` (`id_produit`)
) ENGINE=InnoDB AUTO_INCREMENT=29 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------------------------------------------------------- conseil_produits (bundles / promos)
-- ----------------------------------------------------------------------------- 
CREATE TABLE IF NOT EXISTS `conseil_produits` (
    `id_conseil`  INT NOT NULL,
    `id_produit`  INT NOT NULL,
    PRIMARY KEY (`id_conseil`, `id_produit`),
    KEY `idx_cp_conseil` (`id_conseil`),
    KEY `idx_cp_produit` (`id_produit`),
    CONSTRAINT `fk_cp_conseil` FOREIGN KEY (`id_conseil`) REFERENCES `conseils_ia` (`id_conseil`) ON DELETE CASCADE,
    CONSTRAINT `fk_cp_produit` FOREIGN KEY (`id_produit`) REFERENCES `produit` (`id_produit`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------------------------------------------------------- conseils_marketing
-- ConseilsMarketingService.java
-- ----------------------------------------------------------------------------- 
CREATE TABLE IF NOT EXISTS `conseils_marketing` (
    `conseil_id`       INT AUTO_INCREMENT PRIMARY KEY,
    `produit_id`       VARCHAR(64) NOT NULL COMMENT 'Souvent id numérique en texte pour jointure CAST',
    `type_conseil`     VARCHAR(128) NOT NULL,
    `description`      TEXT NOT NULL,
    `impact_estime`    VARCHAR(255) NULL,
    `date_generation`  DATE NULL,
    `applique`         TINYINT(1) NOT NULL DEFAULT 0,
    KEY `idx_cm_produit` (`produit_id`(16)),
    KEY `idx_cm_applique` (`applique`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------------------------------------------------------- campagne_marketing
-- ----------------------------------------------------------------------------- 
CREATE TABLE IF NOT EXISTS `campagne_marketing` (
    `id`               INT AUTO_INCREMENT PRIMARY KEY,
    `vendor_id`        INT NOT NULL DEFAULT 1,
    `id_vendeur`       INT NULL,
    `nom`              VARCHAR(255) NOT NULL,
    `type_action`      VARCHAR(100) NULL,
    `objectif`         TEXT NULL,
    `canal`            VARCHAR(100) NULL,
    `statut`           VARCHAR(50) NOT NULL DEFAULT 'BROUILLON',
    `date_debut`       DATE NULL,
    `date_fin`         DATE NULL,
    `budget_alloue`    DECIMAL(14,2) NOT NULL DEFAULT 0.00,
    `budget_depense`   DECIMAL(14,2) NOT NULL DEFAULT 0.00,
    `score_ia`         DECIMAL(5,2) NULL DEFAULT 0.00,
    `ia_score`         DECIMAL(5,2) NULL,
    `ia_conseil`       TEXT NULL,
    KEY `idx_campagne_vendor` (`vendor_id`),
    KEY `idx_campagne_statut` (`statut`),
    KEY `idx_campagne_dates` (`date_debut`, `date_fin`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------------------------------------------------------- statistiques_ventes
-- ----------------------------------------------------------------------------- 
CREATE TABLE IF NOT EXISTS `statistiques_ventes` (
    `stat_id`           INT AUTO_INCREMENT PRIMARY KEY,
    `produit_id`        VARCHAR(64) NOT NULL,
    `reference`         VARCHAR(128) NULL,
    `periode`           DATE NOT NULL,
    `semaine`           INT NULL DEFAULT 0,
    `total_vendu`       INT NOT NULL DEFAULT 0,
    `quantite_vendue`   INT NULL,
    `nb_ventes`         INT NULL,
    `ventes`            INT NULL,
    `revenu_total`      DECIMAL(14,2) NOT NULL DEFAULT 0.00,
    `revenu`            DECIMAL(14,2) NULL,
    `montant`           DECIMAL(14,2) NULL,
    `taux_retour`       DECIMAL(8,4) NOT NULL DEFAULT 0.0000,
    `classement`        VARCHAR(64) NULL,
    `vendor_id`         INT NULL DEFAULT 1,
    `id_vendeur`        INT NULL,
    KEY `idx_stats_produit` (`produit_id`(32)),
    KEY `idx_stats_periode` (`periode`),
    KEY `idx_stats_semaine` (`semaine`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------------------------------------------------------- alerte_ia
-- ----------------------------------------------------------------------------- 
CREATE TABLE IF NOT EXISTS `alerte_ia` (
    `id_alerte`     INT AUTO_INCREMENT PRIMARY KEY,
    `id_vendeur`    INT NOT NULL DEFAULT 1,
    `message`       TEXT NOT NULL,
    `niveau`        VARCHAR(32) NOT NULL DEFAULT 'INFO',
    `score_sante`   INT NOT NULL DEFAULT 100,
    `created_at`    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY `idx_alerte_vendeur` (`id_vendeur`),
    KEY `idx_alerte_created` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------------------------------------------------------- Historique_IA
-- HistoriqueIAServices.java
-- ----------------------------------------------------------------------------- 
CREATE TABLE IF NOT EXISTS `Historique_IA` (
    `id_recherche`       INT AUTO_INCREMENT PRIMARY KEY,
    `id_acheteur`        INT NOT NULL,
    `mots_cles`          TEXT NOT NULL,
    `produit_suggere_id` INT NULL,
    `date_recherche`     DATETIME DEFAULT CURRENT_TIMESTAMP,
    KEY `idx_hist_acheteur` (`id_acheteur`),
    CONSTRAINT `fk_historique_ia_user`
        FOREIGN KEY (`id_acheteur`) REFERENCES `users` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_historique_ia_produit`
        FOREIGN KEY (`produit_suggere_id`) REFERENCES `produit` (`id_produit`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------------------------------------------------------- Interaction_Utilisateur
-- InteractionUtilisateurServices.java — même logique que le dashboard (vues = VIEW)
-- ----------------------------------------------------------------------------- 
CREATE TABLE IF NOT EXISTS `Interaction_Utilisateur` (
    `id_interaction`     INT AUTO_INCREMENT PRIMARY KEY,
    `id_acheteur`        INT NOT NULL,
    `id_produit`         INT NOT NULL,
    `type_interaction`   ENUM('VIEW','CLICK_PRODUCT','ADD_TO_CART','BOUGHT','ADD_TO_WISHLIST') NOT NULL,
    `nb_interaction`     INT DEFAULT 1,
    `last_interaction`   DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY `uq_interaction` (`id_acheteur`, `id_produit`, `type_interaction`),
    KEY `idx_iu_produit` (`id_produit`),
    CONSTRAINT `fk_interaction_user`
        FOREIGN KEY (`id_acheteur`) REFERENCES `users` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_interaction_produit`
        FOREIGN KEY (`id_produit`) REFERENCES `produit` (`id_produit`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------------------------------------------------------- wishlist
-- ----------------------------------------------------------------------------- 
CREATE TABLE IF NOT EXISTS `wishlist` (
    `id`           INT AUTO_INCREMENT PRIMARY KEY,
    `user_id`      INT NOT NULL,
    `produit_id`   INT NOT NULL,
    `date_ajout`   DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY `uq_user_produit` (`user_id`, `produit_id`),
    CONSTRAINT `fk_wishlist_user`
        FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_wishlist_produit`
        FOREIGN KEY (`produit_id`) REFERENCES `produit` (`id_produit`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------------------------------------------------------- panier
-- PanierService.java
-- ----------------------------------------------------------------------------- 
CREATE TABLE IF NOT EXISTS `panier` (
    `panier_id`              INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `user_id`                INT NOT NULL,
    `produit_id`             INT NOT NULL,
    `quantite`               INT NOT NULL DEFAULT 1,
    `prix_unitaire`          DECIMAL(10,2) NOT NULL,
    `date_ajout`             DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `statut`                 VARCHAR(50) NOT NULL DEFAULT 'actif',
    `reference_transaction`  VARCHAR(100) DEFAULT NULL,
    KEY `idx_panier_user` (`user_id`),
    KEY `idx_panier_user_prod_actif` (`user_id`, `produit_id`, `statut`),
    CONSTRAINT `fk_panier_user`
        FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_panier_produit`
        FOREIGN KEY (`produit_id`) REFERENCES `produit` (`id_produit`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------------------------------------------------------- rapport_ventes / feedback_marche (ressources-wejden/schema_module7.sql)
-- ----------------------------------------------------------------------------- 
CREATE TABLE IF NOT EXISTS `rapport_ventes` (
    `id_rapport`   INT AUTO_INCREMENT PRIMARY KEY,
    `id_boutique`  INT NOT NULL,
    `total_ca`     DOUBLE NOT NULL,
    `mois_annee`   VARCHAR(7) NOT NULL COMMENT 'MM/AAAA',
    KEY `idx_rapport_boutique` (`id_boutique`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `feedback_marche` (
    `id_feedback`        INT AUTO_INCREMENT PRIMARY KEY,
    `id_produit`         INT NOT NULL,
    `probleme_detecte`   TEXT NOT NULL,
    `strategie_suggeree` TEXT NOT NULL,
    `statut`             VARCHAR(20) DEFAULT 'Ignore',
    KEY `idx_feedback_produit` (`id_produit`),
    CONSTRAINT `fk_feedback_produit`
        FOREIGN KEY (`id_produit`) REFERENCES `produit` (`id_produit`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

SET FOREIGN_KEY_CHECKS = 1;

-- =============================================================================
-- Données de démo (optionnel — commenter si non désiré)
-- =============================================================================
INSERT INTO `produit` (`nom`, `description`, `prix`, `quantite_stock`, `seuil_alerte`, `categorie`, `moyenne`, `statut`)
SELECT v.nom, v.description, v.prix, v.qty, v.seuil, v.cat, v.note, v.statut
FROM (
    SELECT 'iPhone 15 Pro' AS nom, 'Smartphone' AS description, 4599.00 AS prix,
           12 AS qty, 5 AS seuil, 'ELECTRONIQUE' AS cat, 4.5 AS note, 'ACTIF' AS statut
) AS v
WHERE NOT EXISTS (SELECT 1 FROM `produit` p WHERE p.nom = v.nom LIMIT 1);

INSERT INTO `statistiques_ventes` (`produit_id`, `reference`, `periode`, `semaine`, `total_vendu`, `quantite_vendue`,
    `revenu_total`, `taux_retour`, `classement`, `vendor_id`)
SELECT v.pid, v.ref, v.per, v.sem, v.tv, v.qv, v.rev, v.tr, v.cl, v.vid
FROM (
    SELECT 'REF-IPHONE-15' AS pid, 'REF-IPHONE-15' AS ref, CURDATE() AS per, WEEK(CURDATE(), 3) AS sem,
           120 AS tv, 120 AS qv, 380000.00 AS rev, 2.5 AS tr, 'A' AS cl, 1 AS vid
) AS v
WHERE NOT EXISTS (SELECT 1 FROM `statistiques_ventes` s WHERE s.reference = v.ref LIMIT 1);

INSERT INTO `conseils_ia` (`id_produit`, `type`, `urgence`, `description`, `score`, `etat`)
SELECT v.idp, v.typ, v.urg, v.descr, v.scr, v.et
FROM (
    SELECT (SELECT COALESCE(MIN(id_produit), 1) FROM `produit`) AS idp,
           'Promotion' AS typ, 'NORMAL' AS urg,
           'Campagne flash week-end sur best-seller.' AS descr, 85 AS scr, 'EN_ATTENTE' AS et
) AS v
WHERE NOT EXISTS (
    SELECT 1 FROM `conseils_ia` c WHERE c.description = v.descr LIMIT 1
);

INSERT INTO `campagne_marketing` (`vendor_id`, `nom`, `type_action`, `objectif`, `canal`, `statut`, `date_debut`, `date_fin`,
    `budget_alloue`, `budget_depense`, `score_ia`)
SELECT v.vid, v.nom, v.ta, v.obj, v.can, v.st, v.d1, v.d2, v.ba, v.bd, v.sc
FROM (
    SELECT 1 AS vid, 'Fidelite Platinium' AS nom, 'EMAIL' AS ta, 'Fideliser la base clients' AS obj,
           'EMAIL' AS can, 'ACTIVE' AS st, CURDATE() AS d1, DATE_ADD(CURDATE(), INTERVAL 30 DAY) AS d2,
           2500.00 AS ba, 0.00 AS bd, 7.0 AS sc
) AS v
WHERE NOT EXISTS (SELECT 1 FROM `campagne_marketing` c WHERE c.nom = v.nom LIMIT 1);

INSERT INTO `alerte_ia` (`id_vendeur`, `message`, `niveau`, `score_sante`)
SELECT v.vend, v.msg, v.niv, v.score
FROM (
    SELECT 1 AS vend, 'Pipeline IA : synchronisation OK.' AS msg, 'INFO' AS niv, 92 AS score
) AS v
WHERE NOT EXISTS (SELECT 1 FROM `alerte_ia` a WHERE a.message = v.msg LIMIT 1);

-- ----------------------------------------------------------------------------- Comptes de démo (connexion app — même hash SHA-256 que UserCRUD pour "123456")
-- test@7anouti.tn / 123456  |  admin@7anouti.tn / 123456
-- ----------------------------------------------------------------------------- 
INSERT INTO `users` (`nom`, `prenom`, `date_naiss`, `e_mail`, `num_tel`, `mot_de_pass`, `image`, `role`, `status`, `face_id_enabled`, `face_image_path`, `email_verified`)
SELECT 'Demo', '7anouti', '2000-01-01', 'test@7anouti.tn', '00000000',
       '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92',
       NULL, 'acheteur', 'Unbanned', 0, NULL, 1
FROM (SELECT 1) AS _
WHERE NOT EXISTS (SELECT 1 FROM `users` u WHERE u.e_mail = 'test@7anouti.tn' LIMIT 1);

INSERT INTO `users` (`nom`, `prenom`, `date_naiss`, `e_mail`, `num_tel`, `mot_de_pass`, `image`, `role`, `status`, `face_id_enabled`, `face_image_path`, `email_verified`)
SELECT 'Admin', '7anouti', '2000-01-01', 'admin@7anouti.tn', '00000000',
       '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92',
       NULL, 'admin', 'Unbanned', 0, NULL, 1
FROM (SELECT 1) AS _
WHERE NOT EXISTS (SELECT 1 FROM `users` u WHERE u.e_mail = 'admin@7anouti.tn' LIMIT 1);
