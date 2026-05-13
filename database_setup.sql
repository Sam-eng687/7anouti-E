/* 
   Configuration de la base de données 7anouti-E
   Tables : Statistiques, Alertes IA, Conseils, Campagnes
*/

CREATE DATABASE IF NOT EXISTS hanouti_db;
USE hanouti_db;

-- 1. Table des Statistiques de Ventes
CREATE TABLE IF NOT EXISTS statistiques_ventes (
    id INT AUTO_INCREMENT PRIMARY KEY,
    reference VARCHAR(50),
    produit_nom VARCHAR(100),
    periode VARCHAR(20),
    semaine INT,
    quantite_vendue INT,
    revenu_total DOUBLE,
    taux_retour DOUBLE,
    classement VARCHAR(50),
    id_vendeur INT DEFAULT 1
);

-- 2. Table des Alertes IA
CREATE TABLE IF NOT EXISTS alerte_ia (
    id INT AUTO_INCREMENT PRIMARY KEY,
    id_vendeur INT DEFAULT 1,
    message TEXT,
    niveau VARCHAR(20), -- CRITIQUE, WARNING, INFO
    score_sante INT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 3. Table des Conseils IA
CREATE TABLE IF NOT EXISTS conseils_ia (
    id INT AUTO_INCREMENT PRIMARY KEY,
    type VARCHAR(50), -- PROMO, STOCK, BUNDLE, DESTOCK
    description TEXT,
    date_genere DATE,
    etat VARCHAR(20) DEFAULT 'NOUVEAU', -- NOUVEAU, APPLIQUE, IGNORE
    impact_estime VARCHAR(20), -- ELEVE, MOYEN
    produit_concerne VARCHAR(100)
);

-- 4. Table des Campagnes Marketing
CREATE TABLE IF NOT EXISTS campagne_marketing (
    id INT AUTO_INCREMENT PRIMARY KEY,
    nom VARCHAR(100),
    type_action VARCHAR(50), 
    canal VARCHAR(50), 
    budget_alloue DOUBLE,
    budget_depense DOUBLE DEFAULT 0,
    date_debut DATE,
    date_fin DATE,
    statut VARCHAR(20) DEFAULT 'BROUILLON', 
    score_ia DOUBLE DEFAULT 0,
    vendor_id INT DEFAULT 1
);

-- ---------------------------------------------------------
-- INSERTIONS DE DONNÉES
-- ---------------------------------------------------------

-- Statistiques
INSERT INTO statistiques_ventes (reference, produit_nom, periode, semaine, quantite_vendue, revenu_total, taux_retour, classement, id_vendeur) VALUES
('REF-IPHONE-15', 'iPhone 15 Pro', 'Mai 2024', 18, 45, 157500, 1.2, 'Top 10', 1),
('REF-MAC-M3', 'MacBook Air M3', 'Mai 2024', 18, 22, 88000, 0.5, 'Top 10', 1),
('REF-AIRPODS-3', 'AirPods Gen 3', 'Mai 2024', 18, 120, 72000, 4.5, 'Top 50', 1),
('REF-WATCH-9', 'Apple Watch 9', 'Mai 2024', 17, 35, 45500, 2.1, 'Top 50', 1),
('REF-CABLE-USB', 'Cable USB-C 2m', 'Mai 2024', 17, 300, 15000, 8.2, 'Faible', 1);

-- Alertes IA
INSERT INTO alerte_ia (message, niveau, score_sante, id_vendeur) VALUES
('Taux de retour anormal sur les cables USB-C (+30% vs mois dernier)', 'CRITIQUE', 65, 1),
('Rupture de stock imminente sur iPhone 15 Pro (2 jours restants)', 'WARNING', 82, 1),
('Nouveau segment client identifié : acheteurs de nuit (22h-02h)', 'INFO', 90, 1),
('Campagne "Promo Eté" génère un ROI de 4.5x, budget saturé', 'WARNING', 88, 1),
('Performances globales en hausse de 12% cette semaine', 'INFO', 95, 1);

-- Conseils IA
INSERT INTO conseils_ia (type, description, date_genere, impact_estime, produit_concerne) VALUES
('PROMO', 'Appliquer -15% sur AirPods Gen 3 pour liquider le stock avant la v4.', '2024-05-10', 'ELEVE', 'AirPods Gen 3'),
('BUNDLE', 'Pack "Bureau Pro" : MacBook Air + Cable USB-C avec -10% de remise.', '2024-05-11', 'MOYEN', 'MacBook Air M3'),
('STOCK', 'Réapprovisionner d''urgence l''iPhone 15 Pro (Seulement 5 unités en stock).', '2024-05-11', 'ELEVE', 'iPhone 15 Pro'),
('DESTOCK', 'Vente flash 24h sur les Apple Watch 9 pour booster la visibilité.', '2024-05-09', 'MOYEN', 'Apple Watch 9'),
('PROMO', 'Email ciblé aux anciens acheteurs d''iPhone pour proposer les accessoires.', '2024-05-08', 'ELEVE', 'Accessoires iPhone');

-- Campagnes
INSERT INTO campagne_marketing (nom, type_action, canal, budget_alloue, budget_depense, date_debut, date_fin, statut, score_ia) VALUES
('Lancement M3', 'VISIBILITE', 'SOCIAL', 5000, 3200, '2024-05-01', '2024-05-31', 'ACTIVE', 8.5),
('Soldes Flash', 'VENTES', 'EMAIL', 1200, 1150, '2024-05-10', '2024-05-12', 'ACTIVE', 9.2),
('Fidélité Platinium', 'FIDELISATION', 'SMS', 2500, 0, '2024-06-01', '2024-06-15', 'BROUILLON', 7.0),
('Printemps 2024', 'VENTES', 'BANNIERE', 8000, 8000, '2024-03-01', '2024-04-30', 'TERMINEE', 6.8),
('Récupération Panier', 'VENTES', 'EMAIL', 500, 210, '2024-05-01', '2024-12-31', 'ACTIVE', 9.8);
