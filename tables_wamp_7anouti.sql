-- Création de la base de données (si nécessaire)
CREATE DATABASE IF NOT EXISTS `7anouti` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `7anouti`;

-- =========================================================================
-- TABLE 1 : produit (Basé sur la capture d'écran)
-- =========================================================================
CREATE TABLE IF NOT EXISTS `produit` (
  `id_produit` int(11) NOT NULL AUTO_INCREMENT,
  `id_vendeur` int(11) NOT NULL,
  `id_fournisseur` int(11) DEFAULT NULL,
  `nom` varchar(200) COLLATE utf8mb4_unicode_ci NOT NULL,
  `description` text COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `categorie` enum('MEDICAMENT','ALIMENTAIRE','ELECTRONIQUE') COLLATE utf8mb4_unicode_ci NOT NULL,
  `prix` decimal(10,2) NOT NULL,
  `quantite_stock` int(11) NOT NULL DEFAULT 0,
  `seuil_alerte` int(11) DEFAULT 5,
  `image` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `statut` enum('ACTIF','SUSPENDU','SUPPRIME') COLLATE utf8mb4_unicode_ci DEFAULT 'ACTIF',
  `date_ajout` datetime DEFAULT CURRENT_TIMESTAMP,
  `quantite_vendu` int(11) NOT NULL DEFAULT 0,
  PRIMARY KEY (`id_produit`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- =========================================================================
-- TABLE 2 : interaction_utilisateur (Basé sur le texte copié)
-- =========================================================================
CREATE TABLE IF NOT EXISTS `interaction_utilisateur` (
  `id_interaction` int(11) NOT NULL AUTO_INCREMENT,
  `id_acheteur` int(11) NOT NULL,
  `id_produit` int(11) NOT NULL,
  `type_interaction` varchar(50) NOT NULL, -- Ex: 'VIEW'
  `date_interaction` datetime NOT NULL,
  PRIMARY KEY (`id_interaction`),
  KEY `fk_interaction_produit` (`id_produit`),
  CONSTRAINT `fk_interaction_produit` FOREIGN KEY (`id_produit`) REFERENCES `produit` (`id_produit`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- =========================================================================
-- TABLE 3 : historique_ia (Basé sur le texte copié)
-- =========================================================================
CREATE TABLE IF NOT EXISTS `historique_ia` (
  `id_recherche` int(11) NOT NULL AUTO_INCREMENT,
  `id_acheteur` int(11) NOT NULL,
  `mots_cles` text NOT NULL,
  `produit_suggere_id` int(11) DEFAULT NULL,
  `date_recherche` datetime NOT NULL,
  PRIMARY KEY (`id_recherche`),
  KEY `fk_historique_produit` (`produit_suggere_id`),
  CONSTRAINT `fk_historique_produit` FOREIGN KEY (`produit_suggere_id`) REFERENCES `produit` (`id_produit`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- =========================================================================
-- INSERTION DE DONNÉES DE TEST (Simulées pour le Dashboard Marketing)
-- =========================================================================

-- Insertion de produits
INSERT INTO `produit` (`id_produit`, `id_vendeur`, `nom`, `categorie`, `prix`, `quantite_stock`, `seuil_alerte`, `quantite_vendu`) VALUES
(7, 1, 'Câble USB-C', 'ELECTRONIQUE', 15.00, 50, 10, 120),
(11, 1, 'Écouteurs Sans Fil', 'ELECTRONIQUE', 80.00, 5, 10, 45), -- Stock faible (Alerte IA !)
(18, 1, 'Batterie Externe', 'ELECTRONIQUE', 45.00, 30, 5, 20),
(21, 1, 'Chargeur Rapide 65W', 'ELECTRONIQUE', 35.00, 100, 20, 200), -- Très populaire (Succès IA !)
(23, 1, 'Clé USB 64Go', 'ELECTRONIQUE', 20.00, 0, 5, 80), -- En rupture de stock
(24, 1, 'Souris Sans Fil', 'ELECTRONIQUE', 25.00, 40, 10, 35),
(25, 1, 'Composant Arduino', 'ELECTRONIQUE', 12.00, 15, 5, 10),
(26, 1, 'Clavier Mécanique', 'ELECTRONIQUE', 90.00, 25, 5, 15),
(27, 1, 'Tapis de souris', 'ELECTRONIQUE', 10.00, 60, 10, 50),
(28, 1, 'Lampe LED Bureau', 'ELECTRONIQUE', 30.00, 20, 5, 12),
(37, 1, 'Hub USB 3.0', 'ELECTRONIQUE', 18.00, 45, 10, 40),
(39, 1, 'Support Téléphone', 'ELECTRONIQUE', 12.00, 80, 15, 150),
(40, 1, 'Décoration LED', 'ELECTRONIQUE', 22.00, 15, 5, 8);

-- Insertion d'interactions utilisateur (Vues)
INSERT INTO `interaction_utilisateur` (`id_interaction`, `id_acheteur`, `id_produit`, `type_interaction`, `date_interaction`) VALUES
(34, 1, 21, 'VIEW', '2026-05-01 12:44:44'),
(35, 1, 18, 'VIEW', '2026-05-01 12:44:44'),
(36, 1, 11, 'VIEW', '2026-05-01 12:44:44'),
(37, 1, 39, 'VIEW', '2026-05-01 12:44:44'),
(38, 1, 7, 'VIEW', '2026-05-01 12:44:44'),
(39, 1, 37, 'VIEW', '2026-05-01 12:44:44'),
(40, 1, 21, 'VIEW', '2026-05-01 12:46:07'),
(41, 1, 18, 'VIEW', '2026-05-01 12:46:07'),
(42, 1, 11, 'VIEW', '2026-05-01 12:46:07'),
(43, 1, 39, 'VIEW', '2026-05-01 12:46:07'),
(44, 1, 7, 'VIEW', '2026-05-01 12:46:07'),
(45, 1, 37, 'VIEW', '2026-05-01 12:46:07'),
(46, 1, 24, 'VIEW', '2026-05-01 12:59:38'),
(47, 1, 26, 'VIEW', '2026-05-01 12:59:38'),
(48, 1, 40, 'VIEW', '2026-05-01 13:01:48'),
(49, 1, 28, 'VIEW', '2026-05-01 13:11:56'),
(50, 1, 25, 'VIEW', '2026-05-01 13:17:02'),
(51, 1, 28, 'VIEW', '2026-05-01 13:17:02'),
(52, 1, 24, 'VIEW', '2026-05-01 13:17:02'),
(53, 1, 27, 'VIEW', '2026-05-01 13:17:02'),
(54, 1, 26, 'VIEW', '2026-05-01 13:17:02'),
(55, 1, 23, 'VIEW', '2026-05-01 13:17:02'),
(56, 1, 27, 'VIEW', '2026-05-01 14:08:33');

-- Insertion de l'historique des recherches IA
INSERT INTO `historique_ia` (`id_recherche`, `id_acheteur`, `mots_cles`, `produit_suggere_id`, `date_recherche`) VALUES
(30, 1, 'chargeur smartphone cable telephone accessoire tel', 21, '2026-05-01 12:44:44'),
(31, 1, 'chargeur telephone cable telephone accessoire tele', 21, '2026-05-01 12:46:07'),
(32, 1, 'chargeur telephone cable telephone accessoire tele', NULL, '2026-05-01 12:57:12'),
(33, 1, 'chargeur telephone cable telephone accessoire tele', 24, '2026-05-01 12:59:38'),
(34, 1, 'accessoires maison decoration', 40, '2026-05-01 13:01:48'),
(35, 1, 'faire gateau donner ingredients', NULL, '2026-05-01 13:02:58'),
(36, 1, 'faire gateau donner ingredients gateau', NULL, '2026-05-01 13:11:15'),
(37, 1, 'lampe composant electronique', 28, '2026-05-01 13:11:56'),
(38, 1, 'composant electronique', NULL, '2026-05-01 13:12:45'),
(39, 1, 'composants electroniques', NULL, '2026-05-01 13:16:15'),
(40, 1, 'composant electronique', 25, '2026-05-01 13:17:02'),
(41, 1, 'imprimante', NULL, '2026-05-01 14:08:06'),
(42, 1, 'souris', 27, '2026-05-01 14:08:33');
