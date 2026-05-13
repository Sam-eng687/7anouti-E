CREATE DATABASE IF NOT EXISTS hanouti;
USE hanouti;

-- Table des produits (pour les statistiques)
CREATE TABLE IF NOT EXISTS produits (
    id_produit INT AUTO_INCREMENT PRIMARY KEY,
    reference VARCHAR(50) NOT NULL,
    nom_produit VARCHAR(100) NOT NULL,
    prix DECIMAL(10,2) NOT NULL,
    stock INT NOT NULL,
    categorie VARCHAR(50)
);

-- Table des statistiques de ventes
CREATE TABLE IF NOT EXISTS statistiques_ventes (
    stat_id INT AUTO_INCREMENT PRIMARY KEY,
    produit_id VARCHAR(50),
    periode VARCHAR(50),
    total_vendu INT,
    revenu_total DECIMAL(10,2),
    taux_retour DECIMAL(5,2),
    classement VARCHAR(20)
);

-- Table des campagnes marketing
CREATE TABLE IF NOT EXISTS campagnes_marketing (
    campagne_id INT AUTO_INCREMENT PRIMARY KEY,
    nom_campagne VARCHAR(100),
    type_action VARCHAR(50),
    date_debut DATE,
    date_fin DATE,
    budget DECIMAL(10,2),
    depense DECIMAL(10,2),
    statut VARCHAR(20)
);

-- Table des conseils générés par l'API IA
CREATE TABLE IF NOT EXISTS conseils_ia (
    conseil_id INT AUTO_INCREMENT PRIMARY KEY,
    produit_ref VARCHAR(50),
    type_conseil VARCHAR(50),
    description TEXT,
    impact_estime VARCHAR(20),
    date_generation DATE,
    est_applique BOOLEAN DEFAULT FALSE
);

-- --- DONNÉES DE TEST BOURRÉES DE SENS ---

INSERT INTO produits (reference, nom_produit, prix, stock, categorie) VALUES
('REF-001', 'Smartphone X', 1200.00, 5, 'Électronique'), -- Stock faible, l'IA devrait le voir
('REF-002', 'Casque Audio', 150.00, 120, 'Électronique'), -- Stock élevé
('REF-003', 'Clavier Mécanique', 200.00, 0, 'Informatique'); -- Rupture de stock, l'IA va crier !

INSERT INTO statistiques_ventes (produit_id, periode, total_vendu, revenu_total, taux_retour, classement) VALUES
('REF-001', 'Mois', 45, 54000.00, 1.2, 'Top 10'),
('REF-002', 'Mois', 12, 1800.00, 5.0, 'Moyen'),
('REF-003', 'Mois', 80, 16000.00, 0.5, 'Top 10');
