CREATE TABLE IF NOT EXISTS rapport_ventes (
    id_rapport INT AUTO_INCREMENT PRIMARY KEY,
    id_boutique INT NOT NULL,
    total_ca DOUBLE NOT NULL,
    mois_annee VARCHAR(7) NOT NULL -- Format MM/AAAA
);

CREATE TABLE IF NOT EXISTS feedback_marche (
    id_feedback INT AUTO_INCREMENT PRIMARY KEY,
    id_produit INT NOT NULL,
    probleme_detecte TEXT NOT NULL,
    strategie_suggeree TEXT NOT NULL,
    statut VARCHAR(20) DEFAULT 'Ignore' -- 'Applique' ou 'Ignore'
);
