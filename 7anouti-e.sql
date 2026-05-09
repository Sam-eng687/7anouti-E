-- phpMyAdmin SQL Dump
-- version 5.2.3
-- https://www.phpmyadmin.net/
--
-- Host: 127.0.0.1:3306
-- Generation Time: May 08, 2026 at 10:06 PM
-- Server version: 8.4.7
-- PHP Version: 8.3.28

SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
START TRANSACTION;
SET time_zone = "+00:00";


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;

--
-- Database: `7anouti-e`
--

-- --------------------------------------------------------

--
-- Stand-in structure for view `acheteur`
-- (See below for the actual view)
--
DROP VIEW IF EXISTS `acheteur`;
CREATE TABLE IF NOT EXISTS `acheteur` (
`id_acheteur` int
,`nom` varchar(100)
,`prenom` varchar(100)
,`e_mail` varchar(150)
,`num_tel` varchar(20)
);

-- --------------------------------------------------------

--
-- Table structure for table `commandes`
--

DROP TABLE IF EXISTS `commandes`;
CREATE TABLE IF NOT EXISTS `commandes` (
  `id_commande` int NOT NULL AUTO_INCREMENT,
  `numero_commande` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `id_acheteur` int NOT NULL,
  `id_vendeur` int NOT NULL,
  `id_societe_livraison` int DEFAULT NULL,
  `adresse_livraison` text COLLATE utf8mb4_unicode_ci NOT NULL,
  `date_creation` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `date_livraison_preferee` date DEFAULT NULL,
  `mode_paiement` enum('CARTE','ESPECES') COLLATE utf8mb4_unicode_ci NOT NULL,
  `total` decimal(10,2) NOT NULL,
  `score_priorite` int DEFAULT '0',
  `statut` enum('CREEE','CONFIRMEE','EN_PREPARATION','EXPEDIEE','LIVREE','ANNULEE','REFUSEE') COLLATE utf8mb4_unicode_ci DEFAULT 'CREEE',
  `motif_refus` text COLLATE utf8mb4_unicode_ci,
  `facture_pdf` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `facture_qr` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id_commande`),
  UNIQUE KEY `numero_commande` (`numero_commande`),
  KEY `id_acheteur` (`id_acheteur`),
  KEY `id_vendeur` (`id_vendeur`),
  KEY `id_societe_livraison` (`id_societe_livraison`)
) ENGINE=MyISAM AUTO_INCREMENT=9 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Dumping data for table `commandes`
--

INSERT INTO `commandes` (`id_commande`, `numero_commande`, `id_acheteur`, `id_vendeur`, `id_societe_livraison`, `adresse_livraison`, `date_creation`, `date_livraison_preferee`, `mode_paiement`, `total`, `score_priorite`, `statut`, `motif_refus`, `facture_pdf`, `facture_qr`, `created_at`, `updated_at`) VALUES
(7, 'CMD-2026-0006', 1, 4, NULL, 'Borj Touil, Délégation Raoued, Gouvernorat Ariana, 2056, Tunisie', '2026-05-08 14:54:02', '2026-05-19', 'ESPECES', 120.00, 4, 'CREEE', NULL, NULL, NULL, '2026-05-08 14:54:02', '2026-05-08 20:32:00'),
(2, 'CMD002', 2, 4, 2, 'Ariana, Ennasr', '2026-05-08 12:06:36', '2026-05-16', 'ESPECES', 4200.00, 10, 'EXPEDIEE', NULL, NULL, NULL, '2026-05-08 12:06:36', '2026-05-08 20:33:17'),
(3, 'CMD003', 3, 5, NULL, 'Manouba, Douar Hicher', '2026-05-08 12:06:36', '2026-05-17', 'CARTE', 89.90, 6, 'CREEE', NULL, NULL, NULL, '2026-05-08 12:06:36', '2026-05-08 20:31:44'),
(6, 'CMD-2026-0005', 2, 4, NULL, 'Rue de l\'Esthétique, Cite El Izdihar, Délégation El Ouardia, Tunis, Gouvernorat Tunis, 1009, Tunisie', '2026-05-08 14:53:47', '2026-05-18', 'CARTE', 267.00, 2, 'CREEE', NULL, NULL, NULL, '2026-05-08 14:53:47', '2026-05-08 20:31:22'),
(5, 'CMD005', 2, 4, NULL, 'Sfax, Centre Ville', '2026-05-08 12:06:36', '2026-05-19', 'ESPECES', 32.50, 4, 'CREEE', NULL, NULL, NULL, '2026-05-08 12:06:36', '2026-05-08 20:31:54'),
(8, 'CMD-20260508-00006', 1, 4, NULL, 'Borj Touil, Délégation Raoued, Gouvernorat Ariana, 2056, Tunisie', '2026-05-08 16:09:19', '2026-05-19', 'ESPECES', 24.00, 0, 'CREEE', NULL, NULL, NULL, '2026-05-08 16:09:19', '2026-05-08 19:08:46');

-- --------------------------------------------------------

--
-- Table structure for table `fournisseur`
--

DROP TABLE IF EXISTS `fournisseur`;
CREATE TABLE IF NOT EXISTS `fournisseur` (
  `id_fournisseur` int NOT NULL AUTO_INCREMENT,
  `id_vendeur` int NOT NULL,
  `nom_societe` varchar(200) COLLATE utf8mb4_unicode_ci NOT NULL,
  `contact_nom` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `email` varchar(150) COLLATE utf8mb4_unicode_ci NOT NULL,
  `telephone` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL,
  `adresse` text COLLATE utf8mb4_unicode_ci NOT NULL,
  `conditions_livraison` text COLLATE utf8mb4_unicode_ci,
  `actif` tinyint(1) DEFAULT '1',
  PRIMARY KEY (`id_fournisseur`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------

--
-- Table structure for table `ligne_commandes`
--

DROP TABLE IF EXISTS `ligne_commandes`;
CREATE TABLE IF NOT EXISTS `ligne_commandes` (
  `id_ligne` int NOT NULL AUTO_INCREMENT,
  `id_commande` int NOT NULL,
  `id_produit` int NOT NULL,
  `quantite` int NOT NULL,
  `prix_unitaire` decimal(10,2) NOT NULL,
  `sous_total` decimal(10,2) GENERATED ALWAYS AS ((`quantite` * `prix_unitaire`)) STORED,
  PRIMARY KEY (`id_ligne`),
  KEY `id_commande` (`id_commande`),
  KEY `id_produit` (`id_produit`)
) ;

--
-- Dumping data for table `ligne_commandes`
--

INSERT INTO `ligne_commandes` (`id_ligne`, `id_commande`, `id_produit`, `quantite`, `prix_unitaire`) VALUES
(1, 2, 4, 1, 3500.00),
(2, 2, 5, 2, 350.00),
(3, 3, 2, 3, 19.90),
(4, 3, 6, 1, 30.20),
(5, 5, 1, 1, 12.50),
(6, 5, 9, 2, 10.00),
(7, 6, 3, 3, 49.00),
(8, 6, 7, 2, 60.00),
(9, 7, 8, 2, 40.00),
(10, 7, 1, 2, 20.00),
(11, 8, 1, 2, 12.00);

-- --------------------------------------------------------

--
-- Table structure for table `livraisons`
--

DROP TABLE IF EXISTS `livraisons`;
CREATE TABLE IF NOT EXISTS `livraisons` (
  `id_livraison` int NOT NULL AUTO_INCREMENT,
  `id_commande` int NOT NULL,
  `numero_commande` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `id_societe` int NOT NULL,
  `mode_assignation` enum('AUTOMATIQUE','MANUELLE') COLLATE utf8mb4_unicode_ci NOT NULL,
  `statut_livraison` enum('ASSIGNEE','EN_COURS','LIVREE','ANNULEE') COLLATE utf8mb4_unicode_ci DEFAULT 'ASSIGNEE',
  `date_assignation` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `date_debut_livraison` timestamp NULL DEFAULT NULL,
  `date_livraison` timestamp NULL DEFAULT NULL,
  PRIMARY KEY (`id_livraison`),
  KEY `id_commande` (`id_commande`),
  KEY `id_societe` (`id_societe`)
) ENGINE=MyISAM AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Dumping data for table `livraisons`
--

INSERT INTO `livraisons` (`id_livraison`, `id_commande`, `numero_commande`, `id_societe`, `mode_assignation`, `statut_livraison`, `date_assignation`, `date_debut_livraison`, `date_livraison`) VALUES
(1, 2, 'CMD002', 2, 'AUTOMATIQUE', 'ASSIGNEE', '2026-05-08 20:33:06', NULL, NULL);

-- --------------------------------------------------------

--
-- Table structure for table `notifications`
--

DROP TABLE IF EXISTS `notifications`;
CREATE TABLE IF NOT EXISTS `notifications` (
  `notification_id` int NOT NULL AUTO_INCREMENT,
  `user_id` int NOT NULL,
  `type` enum('COMMANDE','STOCK','LIVRAISON','SYSTEME') COLLATE utf8mb4_unicode_ci NOT NULL,
  `event` enum('PRODUIT_INDISPONIBLE','STOCK_FAIBLE','COMMANDE_REFUSEE','COMMANDE_CONFIRMEE','COMMANDE_EXPEDIEE','COMMANDE_LIVREE','LIVRAISON_ASSIGNEE','NOTE_LIVRAISON') COLLATE utf8mb4_unicode_ci NOT NULL,
  `titre` varchar(150) COLLATE utf8mb4_unicode_ci NOT NULL,
  `message` text COLLATE utf8mb4_unicode_ci NOT NULL,
  `reference_id` int DEFAULT NULL,
  `is_read` tinyint(1) DEFAULT '0',
  `date_creation` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`notification_id`),
  KEY `user_id` (`user_id`)
) ENGINE=MyISAM AUTO_INCREMENT=26 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Dumping data for table `notifications`
--

INSERT INTO `notifications` (`notification_id`, `user_id`, `type`, `event`, `titre`, `message`, `reference_id`, `is_read`, `date_creation`) VALUES
(1, 1, 'COMMANDE', 'COMMANDE_CONFIRMEE', 'Commande confirmée', 'Votre commande CMD-20250510-00001 a été confirmée.', 1, 1, '2026-05-07 16:44:22'),
(2, 3, 'LIVRAISON', 'LIVRAISON_ASSIGNEE', 'Nouvelle commande assignée', 'Une commande vous a été assignée (ID : 1).', 1, 1, '2026-05-07 16:44:41'),
(3, 1, 'COMMANDE', 'COMMANDE_EXPEDIEE', 'Commande expédiée !', 'Votre commande CMD-20250510-00001 est en route.', 1, 1, '2026-05-07 16:44:46'),
(4, 3, 'LIVRAISON', 'LIVRAISON_ASSIGNEE', 'Nouvelle commande assignée', 'Une commande vous a été assignée (ID : 1).', 1, 1, '2026-05-07 17:21:13'),
(5, 3, 'LIVRAISON', 'LIVRAISON_ASSIGNEE', 'Nouvelle commande assignée', 'Une commande vous a été assignée (ID : 1).', 1, 1, '2026-05-07 17:21:21'),
(6, 3, 'LIVRAISON', 'LIVRAISON_ASSIGNEE', 'Nouvelle commande assignée', 'Une commande vous a été assignée (ID : 4).', 4, 1, '2026-05-07 17:21:35'),
(7, 1, 'COMMANDE', 'COMMANDE_REFUSEE', 'Commande refusée', 'Commande CMD-20250510-00002 refusée. Motif : Repture du stock', 2, 1, '2026-05-07 17:41:11'),
(8, 1, 'COMMANDE', 'COMMANDE_EXPEDIEE', '🚀 Commande expédiée !', 'Votre commande CMD-20250509-00003 est en route.', 3, 1, '2026-05-08 11:30:50'),
(9, 1, 'COMMANDE', 'COMMANDE_REFUSEE', '❌ Commande refusée — adresse invalide', 'Adresse de livraison introuvable ou invalide : « Ariana ». Veuillez corriger votre adresse et renvoyer la commande.', 7, 1, '2026-05-08 15:02:17'),
(10, 6, 'LIVRAISON', 'LIVRAISON_ASSIGNEE', 'Nouvelle commande assignée', 'Une commande vous a été assignée (ID : 2).', 2, 0, '2026-05-08 17:34:12'),
(11, 2, 'COMMANDE', 'COMMANDE_CONFIRMEE', '✅ Commande confirmée', 'Votre commande CMD002 a été confirmée.', 2, 1, '2026-05-08 17:34:12'),
(12, 8, 'LIVRAISON', 'LIVRAISON_ASSIGNEE', 'Nouvelle commande assignée', 'Une commande vous a été assignée (ID : 2).', 2, 0, '2026-05-08 17:39:46'),
(13, 2, 'COMMANDE', 'COMMANDE_CONFIRMEE', '✅ Commande confirmée', 'Votre commande CMD002 a été confirmée.', 2, 1, '2026-05-08 17:39:46'),
(14, 7, 'LIVRAISON', 'LIVRAISON_ASSIGNEE', 'Nouvelle commande assignée', 'Une commande vous a été assignée (ID : 2).', 2, 0, '2026-05-08 17:43:30'),
(15, 2, 'COMMANDE', 'COMMANDE_CONFIRMEE', '✅ Commande confirmée', 'Votre commande CMD002 a été confirmée.', 2, 1, '2026-05-08 17:43:30'),
(16, 2, 'COMMANDE', 'COMMANDE_REFUSEE', '❌ Commande refusée', 'Commande CMD002 refusée. Motif : indisponible', 2, 1, '2026-05-08 18:33:08'),
(17, 2, 'COMMANDE', 'COMMANDE_EXPEDIEE', '🚀 Commande expédiée !', 'Votre commande CMD-2026-0005 est en route.', 6, 1, '2026-05-08 18:33:18'),
(18, 2, 'COMMANDE', 'COMMANDE_REFUSEE', '❌ Commande refusée', 'Commande CMD002 refusée. Motif : indisponible', 2, 0, '2026-05-08 19:13:50'),
(19, 2, 'COMMANDE', 'COMMANDE_REFUSEE', '❌ Commande refusée', 'Commande CMD002 refusée. Motif : indisponible', 2, 0, '2026-05-08 19:23:56'),
(20, 2, 'COMMANDE', 'COMMANDE_REFUSEE', '❌ Commande refusée', 'Commande CMD002 refusée. Motif : indispo', 2, 0, '2026-05-08 19:25:30'),
(21, 2, 'COMMANDE', 'COMMANDE_REFUSEE', '❌ Commande refusée', 'Commande CMD002 refusée. Motif : indispo', 2, 0, '2026-05-08 19:27:59'),
(22, 2, 'COMMANDE', 'COMMANDE_EXPEDIEE', '🚀 Commande expédiée !', 'Votre commande CMD-2026-0005 est en route.', 6, 0, '2026-05-08 20:06:56'),
(23, 6, 'LIVRAISON', 'LIVRAISON_ASSIGNEE', 'Nouvelle commande assignée', 'Une commande vous a été assignée (ID : 2).', 2, 0, '2026-05-08 20:33:06'),
(24, 2, 'COMMANDE', 'COMMANDE_CONFIRMEE', '✅ Commande confirmée', 'Votre commande CMD002 a été confirmée.', 2, 0, '2026-05-08 20:33:06'),
(25, 2, 'COMMANDE', 'COMMANDE_EXPEDIEE', '🚀 Commande expédiée !', 'Votre commande CMD002 est en route.', 2, 0, '2026-05-08 20:33:29');

-- --------------------------------------------------------

--
-- Table structure for table `paiements`
--

DROP TABLE IF EXISTS `paiements`;
CREATE TABLE IF NOT EXISTS `paiements` (
  `paiement_id` int NOT NULL AUTO_INCREMENT,
  `commande_id` int NOT NULL,
  `montant` decimal(10,2) NOT NULL,
  `methode` enum('CARTE','CIB','ESPECES','D17') COLLATE utf8mb4_unicode_ci NOT NULL,
  `statut` enum('EN_ATTENTE','VALIDÉ','ÉCHEC') COLLATE utf8mb4_unicode_ci NOT NULL,
  `date_paiement` datetime DEFAULT CURRENT_TIMESTAMP,
  `reference_transaction` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`paiement_id`),
  UNIQUE KEY `reference_transaction` (`reference_transaction`),
  KEY `commande_id` (`commande_id`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------

--
-- Table structure for table `panier`
--

DROP TABLE IF EXISTS `panier`;
CREATE TABLE IF NOT EXISTS `panier` (
  `panier_id` int NOT NULL AUTO_INCREMENT,
  `user_id` int NOT NULL,
  `produit_id` int NOT NULL,
  `quantite` int NOT NULL DEFAULT '1',
  `prix_unitaire` decimal(10,2) NOT NULL,
  `date_ajout` datetime DEFAULT CURRENT_TIMESTAMP,
  `statut` enum('ACTIF','VALIDÉ') COLLATE utf8mb4_unicode_ci DEFAULT 'ACTIF',
  PRIMARY KEY (`panier_id`),
  KEY `user_id` (`user_id`),
  KEY `produit_id` (`produit_id`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------

--
-- Table structure for table `produit`
--

DROP TABLE IF EXISTS `produit`;
CREATE TABLE IF NOT EXISTS `produit` (
  `id_produit` int NOT NULL AUTO_INCREMENT,
  `id_vendeur` int NOT NULL,
  `id_fournisseur` int DEFAULT NULL,
  `nom` varchar(200) COLLATE utf8mb4_unicode_ci NOT NULL,
  `description` text COLLATE utf8mb4_unicode_ci,
  `categorie` enum('ALIMENTAIRE','ELECTRONIQUE','MEDICAMENT','HYGIENE','DECOR','MAKEUP') COLLATE utf8mb4_unicode_ci NOT NULL,
  `prix` decimal(10,2) NOT NULL,
  `quantite_stock` int NOT NULL DEFAULT '0',
  `seuil_alerte` int DEFAULT '5',
  `image` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `statut` enum('ACTIF','SUSPENDU','SUPPRIME') COLLATE utf8mb4_unicode_ci DEFAULT 'ACTIF',
  `date_ajout` datetime DEFAULT CURRENT_TIMESTAMP,
  `quantite_vendu` int NOT NULL DEFAULT '0',
  `moyenne` float DEFAULT '0',
  PRIMARY KEY (`id_produit`)
) ENGINE=InnoDB AUTO_INCREMENT=11 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Dumping data for table `produit`
--

INSERT INTO `produit` (`id_produit`, `id_vendeur`, `id_fournisseur`, `nom`, `description`, `categorie`, `prix`, `quantite_stock`, `seuil_alerte`, `image`, `statut`, `date_ajout`, `quantite_vendu`, `moyenne`) VALUES
(1, 1, 1, 'Lait Délice 1L', 'Lait demi-écrémé', 'ALIMENTAIRE', 2.50, 100, 10, 'lait.jpg', 'ACTIF', '2026-05-08 13:04:59', 45, 4.5),
(2, 1, 2, 'iPhone 15', 'Smartphone Apple 128GB', 'ELECTRONIQUE', 4200.00, 15, 3, 'iphone15.jpg', 'ACTIF', '2026-05-08 13:04:59', 8, 4.9),
(3, 2, 3, 'Doliprane 1000mg', 'Boîte de 8 comprimés', 'MEDICAMENT', 8.50, 200, 20, 'doliprane.jpg', 'ACTIF', '2026-05-08 13:04:59', 120, 4.7),
(4, 2, 1, 'Shampoing Dove', 'Shampoing hydratant 400ml', 'HYGIENE', 15.90, 50, 5, 'dove.jpg', 'ACTIF', '2026-05-08 13:04:59', 25, 4.3),
(5, 3, 4, 'Vase Moderne', 'Vase décoratif en verre', 'DECOR', 75.00, 12, 2, 'vase.jpg', 'ACTIF', '2026-05-08 13:04:59', 6, 4.1),
(6, 3, 5, 'Rouge à lèvres Maybelline', 'Rouge matte longue tenue', 'MAKEUP', 32.50, 40, 5, 'lipstick.jpg', 'ACTIF', '2026-05-08 13:04:59', 19, 4.6),
(7, 1, 2, 'Casque JBL', 'Casque Bluetooth sans fil', 'ELECTRONIQUE', 180.00, 30, 5, 'jbl.jpg', 'ACTIF', '2026-05-08 13:04:59', 12, 4.8),
(8, 2, 3, 'Gel Désinfectant', 'Gel antibactérien 250ml', 'HYGIENE', 9.90, 80, 10, 'gel.jpg', 'ACTIF', '2026-05-08 13:04:59', 34, 4.2),
(9, 1, 1, 'Pâtes Barilla', 'Spaghetti 500g', 'ALIMENTAIRE', 3.20, 150, 15, 'barilla.jpg', 'ACTIF', '2026-05-08 13:04:59', 60, 4.4),
(10, 3, 4, 'Cadre Photo', 'Cadre en bois 20x30', 'DECOR', 22.00, 25, 4, 'cadre.jpg', 'ACTIF', '2026-05-08 13:04:59', 10, 4);

-- --------------------------------------------------------

--
-- Table structure for table `societes_livraison`
--

DROP TABLE IF EXISTS `societes_livraison`;
CREATE TABLE IF NOT EXISTS `societes_livraison` (
  `id_societe` int NOT NULL AUTO_INCREMENT,
  `id_user` int NOT NULL,
  `nom_societe` varchar(150) COLLATE utf8mb4_unicode_ci NOT NULL,
  `zone_couverture` text COLLATE utf8mb4_unicode_ci NOT NULL,
  `adresse_societe` text COLLATE utf8mb4_unicode_ci,
  `note` decimal(2,1) DEFAULT '0.0',
  `statut` enum('ACTIVE','INACTIVE') COLLATE utf8mb4_unicode_ci DEFAULT 'ACTIVE',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id_societe`),
  UNIQUE KEY `id_user` (`id_user`)
) ENGINE=MyISAM AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Dumping data for table `societes_livraison`
--

INSERT INTO `societes_livraison` (`id_societe`, `id_user`, `nom_societe`, `zone_couverture`, `adresse_societe`, `note`, `statut`, `created_at`) VALUES
(2, 6, 'Jemai Delivery', 'Tunis - Ariana', 'Ariana', 0.0, 'ACTIVE', '2026-05-08 12:22:10'),
(3, 7, 'Haddad Delivery', 'Sousse - Monastir', 'Sousse', 0.0, 'ACTIVE', '2026-05-08 12:22:10'),
(4, 8, 'Benzarti Delivery', 'Grand Tunis', 'Tunis', 0.0, 'ACTIVE', '2026-05-08 12:22:10');

-- --------------------------------------------------------

--
-- Table structure for table `users`
--

DROP TABLE IF EXISTS `users`;
CREATE TABLE IF NOT EXISTS `users` (
  `id` int NOT NULL,
  `nom` varchar(100) COLLATE utf8mb4_general_ci NOT NULL,
  `prenom` varchar(100) COLLATE utf8mb4_general_ci NOT NULL,
  `date_naiss` varchar(50) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `e_mail` varchar(150) COLLATE utf8mb4_general_ci NOT NULL,
  `num_tel` varchar(20) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `mot_de_pass` varchar(255) COLLATE utf8mb4_general_ci NOT NULL,
  `image` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `role` enum('admin','acheteur','vendeur','livreur') COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'acheteur',
  `status` enum('Banned','Unbanned') COLLATE utf8mb4_general_ci NOT NULL,
  `face_id_enabled` tinyint(1) DEFAULT '0',
  `face_image_path` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `adresse` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `entreprise` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `type_produit` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `vehicule` varchar(100) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `permis` varchar(100) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `zone_livraison` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `users`
--

INSERT INTO `users` (`id`, `nom`, `prenom`, `date_naiss`, `e_mail`, `num_tel`, `mot_de_pass`, `image`, `role`, `status`, `face_id_enabled`, `face_image_path`, `adresse`, `entreprise`, `type_produit`, `vehicule`, `permis`, `zone_livraison`) VALUES
(1, 'Jandoubi', 'Samar', '2001-06-15', 'samar.admin@gmail.com', '20000001', 'admin123', NULL, 'admin', 'Unbanned', 0, NULL, 'Tunis Centre', NULL, NULL, NULL, NULL, NULL),
(2, 'Ben Ali', 'Ahmed', '1998-03-10', 'samarjandoubi45@gmail.com', '20000002', 'buyer123', NULL, 'acheteur', 'Unbanned', 0, NULL, 'Ariana', NULL, NULL, NULL, NULL, NULL),
(3, 'Trabelsi', 'Mariem', '2000-11-20', 'mariem.buyer@gmail.com', '20000003', 'buyer123', NULL, 'acheteur', 'Unbanned', 0, NULL, 'Manouba', NULL, NULL, NULL, NULL, NULL),
(4, 'Khaldi', 'Omar', '1995-05-01', 'omar.seller@gmail.com', '20000004', 'seller123', NULL, 'vendeur', 'Unbanned', 0, NULL, 'Sfax', 'Tech Store Tunisia', 'ELECTRONIQUE', NULL, NULL, NULL),
(5, 'Masmoudi', 'Ines', '1997-09-12', 'ines.seller@gmail.com', '20000005', 'seller123', NULL, 'vendeur', 'Unbanned', 0, NULL, 'Tunis', 'Beauty World', 'MAKEUP', NULL, NULL, NULL),
(6, 'Jemai', 'Ali', '1996-07-22', 'ali.delivery@gmail.com', '20000006', 'livreur123', NULL, 'livreur', 'Unbanned', 0, NULL, 'Ariana', NULL, NULL, 'Motorcycle', 'B', 'Tunis - Ariana'),
(7, 'Haddad', 'Mohamed', '1994-12-05', 'mohamed.delivery@gmail.com', '20000007', 'livreur123', NULL, 'livreur', 'Unbanned', 0, NULL, 'Sousse', NULL, NULL, 'Car', 'B', 'Sousse - Monastir'),
(8, 'Benzarti', 'Yassine', '1999-02-18', 'yassine.delivery@gmail.com', '20000008', 'livreur123', NULL, 'livreur', 'Unbanned', 0, NULL, 'Tunis', NULL, NULL, 'Scooter', 'A', 'Grand Tunis');

--
-- Triggers `users`
--
DROP TRIGGER IF EXISTS `after_livreur_insert`;
DELIMITER $$
CREATE TRIGGER `after_livreur_insert` AFTER INSERT ON `users` FOR EACH ROW BEGIN
    IF NEW.role = 'livreur' THEN
        INSERT INTO societes_livraison
        (id_user, nom_societe, zone_couverture, adresse_societe, note, statut, created_at)
        VALUES
        (
            NEW.id,
            CONCAT(NEW.nom, ' Delivery'),
            NEW.zone_livraison,
            NEW.adresse,
            0,
            'ACTIVE',
            NOW()
        );
    END IF;
END
$$
DELIMITER ;

-- --------------------------------------------------------

--
-- Stand-in structure for view `vendeur`
-- (See below for the actual view)
--
DROP VIEW IF EXISTS `vendeur`;
CREATE TABLE IF NOT EXISTS `vendeur` (
`id_vendeur` int
,`nom` varchar(100)
,`prenom` varchar(100)
,`e_mail` varchar(150)
,`num_tel` varchar(20)
);

-- --------------------------------------------------------

--
-- Structure for view `acheteur`
--
DROP TABLE IF EXISTS `acheteur`;

DROP VIEW IF EXISTS `acheteur`;
CREATE ALGORITHM=UNDEFINED DEFINER=`root`@`localhost` SQL SECURITY DEFINER VIEW `acheteur`  AS SELECT `users`.`id` AS `id_acheteur`, `users`.`nom` AS `nom`, `users`.`prenom` AS `prenom`, `users`.`e_mail` AS `e_mail`, `users`.`num_tel` AS `num_tel` FROM `users` WHERE (`users`.`role` = 'ACHETEUR') ;

-- --------------------------------------------------------

--
-- Structure for view `vendeur`
--
DROP TABLE IF EXISTS `vendeur`;

DROP VIEW IF EXISTS `vendeur`;
CREATE ALGORITHM=UNDEFINED DEFINER=`root`@`localhost` SQL SECURITY DEFINER VIEW `vendeur`  AS SELECT `users`.`id` AS `id_vendeur`, `users`.`nom` AS `nom`, `users`.`prenom` AS `prenom`, `users`.`e_mail` AS `e_mail`, `users`.`num_tel` AS `num_tel` FROM `users` WHERE (`users`.`role` = 'VENDEUR') ;
COMMIT;

/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
