-- Reference SQL for AIachat integration.
-- This file is not run automatically by the Java application.
-- It targets the integrated database used by MyBD: 7anouti_e.

CREATE TABLE IF NOT EXISTS Historique_IA (
  id_recherche INT PRIMARY KEY AUTO_INCREMENT,
  id_acheteur INT NOT NULL,
  mots_cles TEXT NOT NULL,
  produit_suggere_id INT NULL,
  date_recherche DATETIME DEFAULT NOW(),
  CONSTRAINT fk_historique_ia_user
    FOREIGN KEY (id_acheteur) REFERENCES users(id),
  CONSTRAINT fk_historique_ia_produit
    FOREIGN KEY (produit_suggere_id) REFERENCES produit(id_produit)
);

CREATE TABLE IF NOT EXISTS Interaction_Utilisateur (
  id_interaction INT PRIMARY KEY AUTO_INCREMENT,
  id_acheteur INT NOT NULL,
  id_produit INT NOT NULL,
  type_interaction ENUM('VIEW','CLICK_PRODUCT','ADD_TO_CART','BOUGHT','ADD_TO_WISHLIST') NOT NULL,
  nb_interaction INT DEFAULT 1,
  last_interaction DATETIME DEFAULT NOW(),
  UNIQUE KEY uq_interaction (id_acheteur, id_produit, type_interaction),
  CONSTRAINT fk_interaction_user
    FOREIGN KEY (id_acheteur) REFERENCES users(id),
  CONSTRAINT fk_interaction_produit
    FOREIGN KEY (id_produit) REFERENCES produit(id_produit)
);

CREATE TABLE IF NOT EXISTS wishlist (
  id INT PRIMARY KEY AUTO_INCREMENT,
  user_id INT NOT NULL,
  produit_id INT NOT NULL,
  date_ajout DATETIME DEFAULT NOW(),
  UNIQUE KEY uq_user_produit (user_id, produit_id),
  CONSTRAINT fk_wishlist_user
    FOREIGN KEY (user_id) REFERENCES users(id),
  CONSTRAINT fk_wishlist_produit
    FOREIGN KEY (produit_id) REFERENCES produit(id_produit)
);
