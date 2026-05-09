package projet.hanouti.GestionCommandes.services;

import projet.hanouti.GestionCommandes.entities.SocieteLivraison;
import projet.hanouti.GestionCommandes.enums.StatutSociete;
import projet.hanouti.GestionCommandes.interfaces.IntService;
import projet.hanouti.common.utils.MyBD;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Service de gestion des sociétés de livraison.
 * Gère les sociétés disponibles, leur zone de couverture,
 * leur note, et la logique de sélection automatique.
 */
public class SocieteLivraisonService implements IntService<SocieteLivraison> {

    private final Connection conn;

    public SocieteLivraisonService() {
        this.conn = MyBD.getInstance().getConnection();
    }

    // =========================================================
    // ISERVICE — CRUD DE BASE
    // =========================================================

    /**
     * Ajoute une société de livraison en base.
     */
    @Override
    public SocieteLivraison add(SocieteLivraison societe) {
        String sql = """
            INSERT INTO societes_livraison
              (id_user, nom_societe, zone_couverture, adresse_societe, note, statut)
            VALUES (?, ?, ?, ?, ?, ?)
            """;
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, societe.getIdUser());
            ps.setString(2, societe.getNomSociete());
            ps.setString(3, societe.getZoneCouverture());
            ps.setString(4, societe.getAdresseSociete());
            ps.setDouble(5, societe.getNote());
            ps.setString(6, societe.getStatut().name());

            ps.executeUpdate();

            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) societe.setIdSociete(keys.getInt(1));

        } catch (SQLException e) {
            System.err.println("[SocieteLivraisonService.add] Erreur : " + e.getMessage());
        }
        return societe;
    }

    /**
     * Supprime une société de livraison par son ID.
     */
    @Override
    public void delete(int id) {
        String sql = "DELETE FROM societes_livraison WHERE id_societe = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[SocieteLivraisonService.delete] Erreur : " + e.getMessage());
        }
    }

    /**
     * Met à jour les informations d'une société de livraison.
     */
    @Override
    public SocieteLivraison update(SocieteLivraison societe) {
        String sql = """
            UPDATE societes_livraison SET
              nom_societe    = ?,
              zone_couverture = ?,
              adresse_societe = ?,
              note           = ?,
              statut         = ?
            WHERE id_societe = ?
            """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, societe.getNomSociete());
            ps.setString(2, societe.getZoneCouverture());
            ps.setString(3, societe.getAdresseSociete());
            ps.setDouble(4, societe.getNote());
            ps.setString(5, societe.getStatut().name());
            ps.setInt(6, societe.getIdSociete());
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[SocieteLivraisonService.update] Erreur : " + e.getMessage());
        }
        return societe;
    }

    /**
     * Récupère une société par son ID.
     */
    @Override
    public SocieteLivraison getById(int id) {
        String sql = "SELECT * FROM societes_livraison WHERE id_societe = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapRow(rs);
        } catch (SQLException e) {
            System.err.println("[SocieteLivraisonService.getById] Erreur : " + e.getMessage());
        }
        return null;
    }

    /**
     * Récupère toutes les sociétés de livraison.
     */
    @Override
    public List<SocieteLivraison> getAll() {
        List<SocieteLivraison> list = new ArrayList<>();
        String sql = "SELECT * FROM societes_livraison ORDER BY note DESC";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) {
            System.err.println("[SocieteLivraisonService.getAll] Erreur : " + e.getMessage());
        }
        return list;
    }

    // =========================================================
    // METHODES METIER SPECIFIQUES
    // =========================================================

    /**
     * Récupère uniquement les sociétés de livraison actives.
     * Utilisé pour la liste de choix manuel du vendeur.
     *
     * @return liste des sociétés avec statut ACTIVE
     */
    public List<SocieteLivraison> getSocietesActives() {
        List<SocieteLivraison> list = new ArrayList<>();
        String sql = "SELECT * FROM societes_livraison WHERE statut = 'ACTIVE' ORDER BY note DESC";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) {
            System.err.println("[SocieteLivraisonService.getSocietesActives] Erreur : " + e.getMessage());
        }
        return list;
    }

    /**
     * Récupère les sociétés actives couvrant une zone géographique donnée.
     * La recherche est insensible à la casse et utilise LIKE pour la souplesse.
     *
     * @param zone fragment de zone à rechercher (ex: "Tunis", "Sfax")
     * @return liste des sociétés correspondantes, triées par note décroissante
     */
    public List<SocieteLivraison> getByZone(String zone) {
        List<SocieteLivraison> list = new ArrayList<>();
        String sql = """
            SELECT * FROM societes_livraison
            WHERE statut = 'ACTIVE'
              AND LOWER(zone_couverture) LIKE LOWER(?)
            ORDER BY note DESC
            """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, "%" + zone + "%");
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) {
            System.err.println("[SocieteLivraisonService.getByZone] Erreur : " + e.getMessage());
        }
        return list;
    }

    /**
     * Met à jour la note d'une société suite à une évaluation acheteur.
     * Calcule la moyenne pondérée : (note_actuelle * nb_livraisons + nouvelle_note) / (nb_livraisons + 1).
     * La note est plafonnée entre 0.0 et 5.0.
     *
     * @param idSociete    ID de la société à noter
     * @param nouvelleNote note donnée par l'acheteur (0.0 à 5.0)
     */
    public void updateNote(int idSociete, double nouvelleNote) {
        if (nouvelleNote < 0 || nouvelleNote > 5) {
            throw new IllegalArgumentException("La note doit être comprise entre 0 et 5.");
        }

        // Récupère la note actuelle et le nombre de livraisons effectuées
        String sqlCount = """
            SELECT sl.note, COUNT(l.id_livraison) AS nb_livraisons
            FROM societes_livraison sl
            LEFT JOIN livraisons l ON l.id_societe = sl.id_societe
                AND l.statut_livraison = 'LIVREE'
            WHERE sl.id_societe = ?
            GROUP BY sl.id_societe
            """;

        try (PreparedStatement ps = conn.prepareStatement(sqlCount)) {
            ps.setInt(1, idSociete);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                double noteActuelle = rs.getDouble("note");
                int nbLivraisons = rs.getInt("nb_livraisons");

                // Moyenne pondérée
                double nouvelleMoyenne;
                if (nbLivraisons == 0) {
                    nouvelleMoyenne = nouvelleNote;
                } else {
                    nouvelleMoyenne = (noteActuelle * nbLivraisons + nouvelleNote)
                            / (nbLivraisons + 1);
                }

                // Arrondi à 2 décimales, plafonné à 5.0
                nouvelleMoyenne = Math.min(5.0, Math.round(nouvelleMoyenne * 100.0) / 100.0);

                String sqlUpdate = "UPDATE societes_livraison SET note = ? WHERE id_societe = ?";
                try (PreparedStatement psUpdate = conn.prepareStatement(sqlUpdate)) {
                    psUpdate.setDouble(1, nouvelleMoyenne);
                    psUpdate.setInt(2, idSociete);
                    psUpdate.executeUpdate();
                }
            }
        } catch (SQLException e) {
            System.err.println("[SocieteLivraisonService.updateNote] Erreur : " + e.getMessage());
        }
    }

    /**
     * Sélectionne automatiquement la meilleure société de livraison pour une zone.
     *
     * Critères de sélection (dans l'ordre) :
     *  1. Doit couvrir la zone de l'adresse de livraison
     *  2. Doit être ACTIVE (disponible)
     *  3. Triée par note décroissante (étoiles)
     *
     * @param adresseLivraison adresse complète de livraison
     * @return la société la mieux notée disponible, ou null si aucune
     */
    public SocieteLivraison selectMeilleureSociete(String adresseLivraison) {
        if (adresseLivraison == null || adresseLivraison.isBlank()) return null;

        // Extrait le fragment géographique principal (dernier mot ou ville)
        // Ex: "12 Rue de la République, Tunis" → cherche "Tunis"
        String[] mots = adresseLivraison.trim().split("[,\\s]+");
        String zoneRecherche = mots[mots.length - 1];

        List<SocieteLivraison> candidates = getByZone(zoneRecherche);

        // Retourne la première (meilleure note) ou cherche plus large si vide
        if (!candidates.isEmpty()) return candidates.get(0);

        // Fallback : toutes les sociétés actives, meilleure note
        List<SocieteLivraison> actives = getSocietesActives();
        return actives.isEmpty() ? null : actives.get(0);
    }

    /**
     * Active une société de livraison.
     *
     * @param idSociete ID de la société
     */
    public void activer(int idSociete) {
        changerStatut(idSociete, StatutSociete.ACTIVE);
    }

    /**
     * Désactive une société de livraison.
     *
     * @param idSociete ID de la société
     */
    public void desactiver(int idSociete) {
        changerStatut(idSociete, StatutSociete.INACTIVE);
    }

    /**
     * Récupère une société de livraison par l'ID de son utilisateur.
     *
     * @param idUser ID utilisateur du responsable
     * @return la société correspondante, ou null
     */
    public SocieteLivraison getByUser(int idUser) {
        String sql = "SELECT * FROM societes_livraison WHERE id_user = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idUser);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapRow(rs);
        } catch (SQLException e) {
            System.err.println("[SocieteLivraisonService.getByUser] Erreur : " + e.getMessage());
        }
        return null;
    }

    // =========================================================
    // METHODES PRIVEES UTILITAIRES
    // =========================================================

    /**
     * Change le statut d'une société (ACTIVE / INACTIVE).
     */
    private void changerStatut(int idSociete, StatutSociete statut) {
        String sql = "UPDATE societes_livraison SET statut = ? WHERE id_societe = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, statut.name());
            ps.setInt(2, idSociete);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[SocieteLivraisonService.changerStatut] Erreur : " + e.getMessage());
        }
    }

    /**
     * Mappe une ligne ResultSet vers un objet SocieteLivraison.
     */
    private SocieteLivraison mapRow(ResultSet rs) throws SQLException {
        SocieteLivraison s = new SocieteLivraison();
        s.setIdSociete(rs.getInt("id_societe"));
        s.setIdUser(rs.getInt("id_user"));
        s.setNomSociete(rs.getString("nom_societe"));
        s.setZoneCouverture(rs.getString("zone_couverture"));
        s.setAdresseSociete(rs.getString("adresse_societe"));
        s.setNote(rs.getDouble("note"));
        s.setStatut(StatutSociete.valueOf(rs.getString("statut")));
        return s;
    }
}

