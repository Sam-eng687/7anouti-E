package projet.hanouti.GestionCommandes.services;

import projet.hanouti.GestionCommandes.entities.Livraison;
import projet.hanouti.GestionCommandes.entities.SocieteLivraison;
import projet.hanouti.GestionCommandes.enums.ModeAssignation;
import projet.hanouti.GestionCommandes.enums.StatutLivraison;
import projet.hanouti.GestionCommandes.interfaces.IntService;
import projet.hanouti.common.utils.MyBD;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class LivraisonService implements IntService<Livraison> {

    private final Connection conn;

    public LivraisonService() {
        this.conn = MyBD.getInstance().getConnection();
    }

    // =========================================================
    // ISERVICE — CRUD DE BASE
    // =========================================================

    /**
     * Crée un enregistrement de livraison en base.
     */
    @Override
    public Livraison add(Livraison livraison) {
        String sql = """
            INSERT INTO livraisons
              (id_commande, numero_commande, id_societe, mode_assignation, statut_livraison,
               date_assignation, date_debut_livraison, date_livraison)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            """;
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, livraison.getIdCommande());
            ps.setString(2, livraison.getNumeroCommande());
            ps.setInt(3, livraison.getIdSociete());
            ps.setString(4, livraison.getModeAssignation().name());
            ps.setString(5, livraison.getStatutLivraison().name());
            ps.setObject(6, livraison.getDateAssignation());
            ps.setObject(7, livraison.getDateDebutLivraison());
            ps.setObject(8, livraison.getDateLivraison());

            ps.executeUpdate();

            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) livraison.setIdLivraison(keys.getInt(1));

        } catch (SQLException e) {
            System.err.println("[LivraisonService.add] Erreur : " + e.getMessage());
        }
        return livraison;
    }

    /**
     * Supprime une livraison par son ID.
     */
    @Override
    public void delete(int id) {
        String sql = "DELETE FROM livraisons WHERE id_livraison = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[LivraisonService.delete] Erreur : " + e.getMessage());
        }
    }

    /**
     * Met à jour une livraison (statut, dates).
     */
    @Override
    public Livraison update(Livraison livraison) {
        String sql = """
            UPDATE livraisons SET
              id_societe           = ?,
              mode_assignation     = ?,
              statut_livraison     = ?,
              date_debut_livraison = ?,
              date_livraison       = ?
            WHERE id_livraison = ?
            """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, livraison.getIdSociete());
            ps.setString(2, livraison.getModeAssignation().name());
            ps.setString(3, livraison.getStatutLivraison().name());
            ps.setObject(4, livraison.getDateDebutLivraison());
            ps.setObject(5, livraison.getDateLivraison());
            ps.setInt(6, livraison.getIdLivraison());
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[LivraisonService.update] Erreur : " + e.getMessage());
        }
        return livraison;
    }

    /**
     * Récupère une livraison par son ID.
     */
    @Override
    public Livraison getById(int id) {
        String sql = "SELECT * FROM livraisons WHERE id_livraison = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapRow(rs);
        } catch (SQLException e) {
            System.err.println("[LivraisonService.getById] Erreur : " + e.getMessage());
        }
        return null;
    }

    /**
     * Récupère toutes les livraisons.
     */
    @Override
    public List<Livraison> getAll() {
        List<Livraison> list = new ArrayList<>();
        String sql = "SELECT * FROM livraisons ORDER BY date_assignation DESC";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) {
            System.err.println("[LivraisonService.getAll] Erreur : " + e.getMessage());
        }
        return list;
    }

    // =========================================================
    // ATTRIBUTION — AUTOMATIQUE
    // =========================================================

    /**
     * Attribution automatique : sélectionne la meilleure société de livraison
     * selon la zone de couverture, la note/score (étoiles) et la disponibilité.
     *
     * Logique :
     *  1. Récupère l'adresse de livraison de la commande
     *  2. Filtre les sociétés actives couvrant la zone
     *  3. Trie par note décroissante
     *  4. Assigne la première société disponible
     *
     * @param idCommande ID de la commande à livrer
     * @return la livraison créée
     */
    public Livraison attribuerAutomatiquement(int idCommande) {
        String[] commandeInfo = getCommandeInfo(idCommande);
        String adresse = commandeInfo[0];

        SocieteLivraisonService societeService = new SocieteLivraisonService();
        SocieteLivraison meilleure = societeService.selectMeilleureSociete(adresse);

        if (meilleure == null) {
            throw new IllegalStateException(
                    "Aucune société de livraison disponible pour la zone : " + adresse
            );
        }

        return creerLivraison(idCommande, meilleure.getIdSociete(), ModeAssignation.AUTOMATIQUE);
    }

    // =========================================================
    // ATTRIBUTION — MANUELLE
    // =========================================================

    /**
     * Attribution manuelle : le vendeur choisit explicitement une société.
     *
     * @param idCommande ID de la commande
     * @param idSociete  ID de la société choisie par le vendeur
     * @return la livraison créée
     */
    public Livraison attribuerManuellement(int idCommande, int idSociete) {
        return creerLivraison(idCommande, idSociete, ModeAssignation.MANUELLE);
    }

    // =========================================================
    // CONSULTATION
    // =========================================================

    /**
     * Récupère la livraison associée à une commande donnée.
     *
     * @param idCommande ID de la commande
     * @return livraison correspondante, ou null
     */
    public Livraison getByCommande(int idCommande) {
        String sql = "SELECT * FROM livraisons WHERE id_commande = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idCommande);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapRow(rs);
        } catch (SQLException e) {
            System.err.println("[LivraisonService.getByCommande] Erreur : " + e.getMessage());
        }
        return null;
    }

    /**
     * Récupère toutes les livraisons d'une société de livraison.
     *
     * @param idSociete ID de la société
     * @return liste des livraisons assignées à cette société
     */
    public List<Livraison> getBySociete(int idSociete) {
        List<Livraison> list = new ArrayList<>();
        String sql = "SELECT * FROM livraisons WHERE id_societe = ? ORDER BY date_assignation DESC";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idSociete);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) {
            System.err.println("[LivraisonService.getBySociete] Erreur : " + e.getMessage());
        }
        return list;
    }

    // =========================================================
    // MISE A JOUR DU STATUT
    // =========================================================

    /**
     * Met à jour le statut d'une livraison.
     * Gère automatiquement les dates selon la transition :
     *  - EN_COURS  → enregistre date_debut
     *  - LIVREE    → enregistre date_livraison
     *
     * @param idLivraison ID de la livraison
     * @param statut      nouveau statut
     */
    public void updateStatut(int idLivraison, StatutLivraison statut) {
        Livraison livraison = getById(idLivraison);
        if (livraison == null) {
            throw new IllegalArgumentException("Livraison introuvable : " + idLivraison);
        }

        livraison.setStatutLivraison(statut);

        switch (statut) {
            case EN_COURS -> livraison.setDateDebutLivraison(LocalDateTime.now());
            case LIVREE   -> livraison.setDateLivraison(LocalDateTime.now());
            default       -> { /* pas de date à mettre à jour */ }
        }

        update(livraison);

        if (statut == StatutLivraison.LIVREE) {
            notifierLivraisonTerminee(livraison.getIdCommande());
        }
    }

    // =========================================================
    // METHODES PRIVEES UTILITAIRES
    // =========================================================

    /**
     * Crée et persiste un enregistrement de livraison.
     * Récupère numero_commande depuis la table commandes avant insertion.
     * Met également à jour id_societe_livraison dans la table commandes.
     */
    private Livraison creerLivraison(int idCommande, int idSociete, ModeAssignation mode) {
        String[] commandeInfo = getCommandeInfo(idCommande);

        Livraison livraison = new Livraison();
        livraison.setIdCommande(idCommande);
        livraison.setNumeroCommande(commandeInfo[1]);
        livraison.setIdSociete(idSociete);
        livraison.setModeAssignation(mode);
        livraison.setStatutLivraison(StatutLivraison.ASSIGNEE);
        livraison.setDateAssignation(LocalDateTime.now());

        livraison = add(livraison);

        // Mise à jour de la référence société dans la commande
        String sql = "UPDATE commandes SET id_societe_livraison = ? WHERE id_commande = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idSociete);
            ps.setInt(2, idCommande);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[LivraisonService.creerLivraison] MAJ commande erreur : " + e.getMessage());
        }

        // Notification à la société de livraison
        NotificationService notifService = new NotificationService();
        SocieteLivraisonService societeService = new SocieteLivraisonService();
        SocieteLivraison societe = societeService.getById(idSociete);

        if (societe != null) {
            notifService.envoyerNotification(
                    societe.getIdUser(),
                    projet.hanouti.GestionCommandes.enums.TypeNotification.LIVRAISON,
                    projet.hanouti.GestionCommandes.enums.EventNotification.LIVRAISON_ASSIGNEE,
                    "Nouvelle commande assignée",
                    "Une commande vous a été assignée (ID : " + idCommande + ").",
                    idCommande
            );
        }

        return livraison;
    }

    /**
     * Récupère adresse_livraison et numero_commande depuis la table commandes.
     *
     * @param idCommande ID de la commande
     * @return String[0] = adresse_livraison, String[1] = numero_commande
     */
    private String[] getCommandeInfo(int idCommande) {
        String sql = "SELECT adresse_livraison, numero_commande FROM commandes WHERE id_commande = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idCommande);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return new String[]{
                        rs.getString("adresse_livraison"),
                        rs.getString("numero_commande")
                };
            }
        } catch (SQLException e) {
            System.err.println("[LivraisonService.getCommandeInfo] Erreur : " + e.getMessage());
        }
        return new String[]{"", ""};
    }

    /**
     * Raccourci pour récupérer uniquement l'adresse de livraison.
     */
    private String getAdresseLivraison(int idCommande) {
        return getCommandeInfo(idCommande)[0];
    }

    /**
     * Envoie une notification à l'acheteur quand sa commande est livrée,
     * et une invitation à noter le service de livraison.
     */
    private void notifierLivraisonTerminee(int idCommande) {
        String sql = "SELECT id_acheteur FROM commandes WHERE id_commande = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idCommande);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                int idAcheteur = rs.getInt("id_acheteur");
                NotificationService notifService = new NotificationService();

                notifService.envoyerNotification(
                        idAcheteur,
                        projet.hanouti.GestionCommandes.enums.TypeNotification.LIVRAISON,
                        projet.hanouti.GestionCommandes.enums.EventNotification.COMMANDE_LIVREE,
                        "Commande livrée !",
                        "Votre commande a bien été livrée. Merci pour votre achat.",
                        idCommande
                );

                notifService.envoyerNotification(
                        idAcheteur,
                        projet.hanouti.GestionCommandes.enums.TypeNotification.LIVRAISON,
                        projet.hanouti.GestionCommandes.enums.EventNotification.NOTE_LIVRAISON,
                        "Évaluez votre livraison",
                        "Comment s'est passée votre livraison ? Donnez une note au service.",
                        idCommande
                );
            }
        } catch (SQLException e) {
            System.err.println("[LivraisonService.notifierLivraisonTerminee] Erreur : " + e.getMessage());
        }
    }

    /**
     * Mappe une ligne ResultSet vers un objet Livraison.
     */
    private Livraison mapRow(ResultSet rs) throws SQLException {
        Livraison l = new Livraison();
        l.setIdLivraison(rs.getInt("id_livraison"));
        l.setIdCommande(rs.getInt("id_commande"));
        l.setNumeroCommande(rs.getString("numero_commande"));
        l.setIdSociete(rs.getInt("id_societe"));
        l.setModeAssignation(ModeAssignation.valueOf(rs.getString("mode_assignation")));
        l.setStatutLivraison(StatutLivraison.valueOf(rs.getString("statut_livraison")));

        Timestamp dateAssig = rs.getTimestamp("date_assignation");
        if (dateAssig != null) l.setDateAssignation(dateAssig.toLocalDateTime());

        Timestamp dateDebut = rs.getTimestamp("date_debut_livraison");
        if (dateDebut != null) l.setDateDebutLivraison(dateDebut.toLocalDateTime());

        Timestamp dateLiv = rs.getTimestamp("date_livraison");
        if (dateLiv != null) l.setDateLivraison(dateLiv.toLocalDateTime());

        return l;
    }
}