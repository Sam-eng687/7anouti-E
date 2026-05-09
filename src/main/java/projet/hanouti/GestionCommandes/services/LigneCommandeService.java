package projet.hanouti.GestionCommandes.services;

import projet.hanouti.GestionCommandes.entities.LigneCommande;
import projet.hanouti.GestionCommandes.interfaces.IntService;
import projet.hanouti.common.utils.MyBD;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
public class LigneCommandeService implements IntService<LigneCommande> {

    private final Connection conn;

    public LigneCommandeService() {
        this.conn = MyBD.getInstance().getConnection();
    }

    // =========================================================
    // ISERVICE — CRUD DE BASE
    // =========================================================

    /**
     * Ajoute une ligne de commande en base.
     * Calcule automatiquement le sous_total.
     */
    @Override
    public LigneCommande add(LigneCommande ligne) {
        String sql = """
        INSERT INTO ligne_commandes
          (id_commande, id_produit, quantite, prix_unitaire)
        VALUES (?, ?, ?, ?)
        """;
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            // sous_total is a generated column — MySQL computes it automatically
            ligne.setSousTotal(calculerSousTotal(ligne.getQuantite(), ligne.getPrixUnitaire()));

            ps.setInt(1, ligne.getIdCommande());
            ps.setInt(2, ligne.getIdProduit());
            ps.setInt(3, ligne.getQuantite());
            ps.setDouble(4, ligne.getPrixUnitaire());

            ps.executeUpdate();

            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) ligne.setIdLigne(keys.getInt(1));

        } catch (SQLException e) {
            System.err.println("[LigneCommandeService.add] Erreur : " + e.getMessage());
        }
        return ligne;
    }

    /**
     * Supprime une ligne de commande par son ID.
     */
    @Override
    public void delete(int id) {
        String sql = "DELETE FROM ligne_commandes WHERE id_ligne = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[LigneCommandeService.delete] Erreur : " + e.getMessage());
        }
    }

    /**
     * Met à jour une ligne de commande.
     * Recalcule le sous_total automatiquement.
     */
    @Override
    public LigneCommande update(LigneCommande ligne) {
        String sql = """
        UPDATE ligne_commandes SET
          id_produit    = ?,
          quantite      = ?,
          prix_unitaire = ?
        WHERE id_ligne = ?
        """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            // sous_total is a generated column — MySQL recomputes it automatically
            ligne.setSousTotal(calculerSousTotal(ligne.getQuantite(), ligne.getPrixUnitaire()));

            ps.setInt(1, ligne.getIdProduit());
            ps.setInt(2, ligne.getQuantite());
            ps.setDouble(3, ligne.getPrixUnitaire());
            ps.setInt(4, ligne.getIdLigne());  // shifted from 5 → 4

            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[LigneCommandeService.update] Erreur : " + e.getMessage());
        }
        return ligne;
    }

    /**
     * Récupère une ligne par son ID.
     */
    @Override
    public LigneCommande getById(int id) {
        String sql = "SELECT * FROM ligne_commandes WHERE id_ligne = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapRow(rs);
        } catch (SQLException e) {
            System.err.println("[LigneCommandeService.getById] Erreur : " + e.getMessage());
        }
        return null;
    }

    /**
     * Récupère toutes les lignes de toutes les commandes.
     */
    @Override
    public List<LigneCommande> getAll() {
        List<LigneCommande> list = new ArrayList<>();
        String sql = "SELECT * FROM ligne_commandes";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) {
            System.err.println("[LigneCommandeService.getAll] Erreur : " + e.getMessage());
        }
        return list;
    }

    // =========================================================
    // METHODES METIER SPECIFIQUES
    // =========================================================

    /**
     * Récupère toutes les lignes appartenant à une commande donnée.
     *
     * @param idCommande ID de la commande
     * @return liste des lignes de cette commande
     */
    public List<LigneCommande> getByCommande(int idCommande) {
        List<LigneCommande> list = new ArrayList<>();
        String sql = "SELECT * FROM ligne_commandes WHERE id_commande = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idCommande);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) {
            System.err.println("[LigneCommandeService.getByCommande] Erreur : " + e.getMessage());
        }
        return list;
    }

    /**
     * Met à jour la quantité d'une ligne spécifique.
     * Recalcule et persiste le nouveau sous_total.
     *
     * @param idLigne          ID de la ligne à modifier
     * @param nouvelleQuantite nouvelle quantité (doit être > 0)
     */
    public void updateQuantite(int idLigne, int nouvelleQuantite) {
        if (nouvelleQuantite <= 0) {
            throw new IllegalArgumentException("La quantité doit être supérieure à 0.");
        }
        LigneCommande ligne = getById(idLigne);
        if (ligne == null) {
            throw new IllegalArgumentException("Ligne introuvable : " + idLigne);
        }
        ligne.setQuantite(nouvelleQuantite);
        ligne.setSousTotal(calculerSousTotal(nouvelleQuantite, ligne.getPrixUnitaire()));
        update(ligne);
    }

    /**
     * Ajoute un produit à une commande existante.
     * Si le produit est déjà présent dans la commande, incrémente sa quantité.
     *
     * @param idCommande   ID de la commande
     * @param idProduit    ID du produit
     * @param quantite     quantité à ajouter
     * @param prixUnitaire prix unitaire au moment de l'ajout
     */
    public void addLigne(int idCommande, int idProduit, int quantite, double prixUnitaire) {
        // Vérifie si le produit est déjà dans la commande
        List<LigneCommande> existantes = getByCommande(idCommande);
        for (LigneCommande l : existantes) {
            if (l.getIdProduit() == idProduit) {
                // Incrémente la quantité existante
                updateQuantite(l.getIdLigne(), l.getQuantite() + quantite);
                return;
            }
        }
        // Sinon, crée une nouvelle ligne
        LigneCommande nouvelle = new LigneCommande();
        nouvelle.setIdCommande(idCommande);
        nouvelle.setIdProduit(idProduit);
        nouvelle.setQuantite(quantite);
        nouvelle.setPrixUnitaire(prixUnitaire);
        nouvelle.setSousTotal(calculerSousTotal(quantite, prixUnitaire));
        add(nouvelle);
    }

    /**
     * Supprime un produit d'une commande.
     *
     * @param idLigne ID de la ligne à supprimer
     */
    public void removeLigne(int idLigne) {
        delete(idLigne);
    }

    /**
     * Calcule le sous-total d'une ligne (quantité × prix unitaire).
     * Arrondi à 2 décimales.
     *
     * @param quantite     quantité du produit
     * @param prixUnitaire prix unitaire du produit
     * @return sous-total arrondi
     */
    public double calculerSousTotal(int quantite, double prixUnitaire) {
        double sousTotal = quantite * prixUnitaire;
        return Math.round(sousTotal * 100.0) / 100.0;
    }

    /**
     * Calcule le total d'une commande à partir de ses lignes.
     *
     * @param idCommande ID de la commande
     * @return total calculé
     */
    public double calculerTotalCommande(int idCommande) {
        return getByCommande(idCommande).stream()
                .mapToDouble(LigneCommande::getSousTotal)
                .sum();
    }

    /**
     * Supprime toutes les lignes d'une commande (ex: avant de les remplacer).
     *
     * @param idCommande ID de la commande
     */
    public void deleteByCommande(int idCommande) {
        String sql = "DELETE FROM ligne_commandes WHERE id_commande = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idCommande);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[LigneCommandeService.deleteByCommande] Erreur : " + e.getMessage());
        }
    }

    // =========================================================
    // METHODE PRIVEE — MAPPING
    // =========================================================

    /**
     * Mappe une ligne ResultSet vers un objet LigneCommande.
     */
    private LigneCommande mapRow(ResultSet rs) throws SQLException {
        LigneCommande l = new LigneCommande();
        l.setIdLigne(rs.getInt("id_ligne"));
        l.setIdCommande(rs.getInt("id_commande"));
        l.setIdProduit(rs.getInt("id_produit"));
        l.setQuantite(rs.getInt("quantite"));
        l.setPrixUnitaire(rs.getDouble("prix_unitaire"));
        l.setSousTotal(rs.getDouble("sous_total"));
        return l;
    }
}
