package projet.hanouti.GestionCommandes.services;

import projet.hanouti.GestionCommandes.entities.Commande;
import projet.hanouti.GestionCommandes.entities.LigneCommande;
import projet.hanouti.GestionCommandes.enums.ModePaiement;
import projet.hanouti.GestionCommandes.enums.StatutCommande;
import projet.hanouti.GestionCommandes.interfaces.IntService;
import projet.hanouti.common.utils.MyBD;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Service complet pour la gestion des commandes.
 * Couvre : création, consultation, modification, cycle de vie,
 * priorisation intelligente, gestion VIP, rupture de stock.
 */
public class CommandeService implements IntService<Commande> {

    private final Connection conn;

    public CommandeService() {
        this.conn = MyBD.getInstance().getConnection();
    }

    // =========================================================
    // ISERVICE — CRUD DE BASE
    // =========================================================

    /**
     * Ajoute une commande en base de données.
     * Génère automatiquement le numero_commande au format CMD-YYYYMMDD-XXXXX.
     */
    @Override
    public Commande add(Commande commande) {
        String sql = """
            INSERT INTO commandes
              (numero_commande, id_acheteur, id_vendeur, id_societe_livraison,
               adresse_livraison, date_livraison_preferee, mode_paiement,
               total, score_priorite, statut, motif_refus, facture_pdf, facture_qr)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, commande.getNumeroCommande());
            ps.setInt(2, commande.getIdAcheteur());
            ps.setInt(3, commande.getIdVendeur());
            if (commande.getIdSocieteLivraison() != null)
                ps.setInt(4, commande.getIdSocieteLivraison());
            else
                ps.setNull(4, Types.INTEGER);
            ps.setString(5, commande.getAdresseLivraison());
            ps.setObject(6, commande.getDateLivraisonPreferee());
            ps.setString(7, commande.getModePaiement().name());
            ps.setDouble(8, commande.getTotal());
            ps.setInt(9, commande.getScorePriorite());
            ps.setString(10, commande.getStatut().name());
            ps.setString(11, commande.getMotifRefus());
            ps.setString(12, commande.getFacturePdf());
            ps.setString(13, commande.getFactureQr());

            ps.executeUpdate();

            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) {
                commande.setIdCommande(keys.getInt(1));
            }
        } catch (SQLException e) {
            System.err.println("[CommandeService.add] Erreur : " + e.getMessage());
        }
        return commande;
    }

    /**
     * Supprime une commande par son ID.
     */
    @Override
    public void delete(int id) {
        String sql = "DELETE FROM commandes WHERE id_commande = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[CommandeService.delete] Erreur : " + e.getMessage());
        }
    }

    /**
     * Met à jour tous les champs modifiables d'une commande.
     */
    @Override
    public Commande update(Commande commande) {
        String sql = """
            UPDATE commandes SET
              adresse_livraison = ?,
              date_livraison_preferee = ?,
              mode_paiement = ?,
              total = ?,
              score_priorite = ?,
              statut = ?,
              motif_refus = ?,
              id_societe_livraison = ?,
              facture_pdf = ?,
              facture_qr = ?
            WHERE id_commande = ?
            """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, commande.getAdresseLivraison());
            ps.setObject(2, commande.getDateLivraisonPreferee());
            ps.setString(3, commande.getModePaiement().name());
            ps.setDouble(4, commande.getTotal());
            ps.setInt(5, commande.getScorePriorite());
            ps.setString(6, commande.getStatut().name());
            ps.setString(7, commande.getMotifRefus());
            if (commande.getIdSocieteLivraison() != null)
                ps.setInt(8, commande.getIdSocieteLivraison());
            else
                ps.setNull(8, Types.INTEGER);
            ps.setString(9, commande.getFacturePdf());
            ps.setString(10, commande.getFactureQr());
            ps.setInt(11, commande.getIdCommande());

            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[CommandeService.update] Erreur : " + e.getMessage());
        }
        return commande;
    }

    /**
     * Récupère une commande par son ID.
     */
    @Override
    public Commande getById(int id) {
        String sql = "SELECT * FROM commandes WHERE id_commande = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapRow(rs);
        } catch (SQLException e) {
            System.err.println("[CommandeService.getById] Erreur : " + e.getMessage());
        }
        return null;
    }

    /**
     * Récupère toutes les commandes (vue admin).
     */
    @Override
    public List<Commande> getAll() {
        List<Commande> list = new ArrayList<>();
        String sql = "SELECT * FROM commandes ORDER BY date_creation DESC";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) {
            System.err.println("[CommandeService.getAll] Erreur : " + e.getMessage());
        }
        return list;
    }

    // =========================================================
    // CREATION METIER
    // =========================================================

    /**
     * Crée une commande complète depuis le panier.
     * - Paiement CARTE  → statut CREEE immédiatement
     * - Paiement ESPECES → statut CREEE, attend confirmation
     * Calcule automatiquement le score de priorité.
     *
     * @param idAcheteur ID de l'acheteur
     * @param idVendeur  ID du vendeur
     * @param lignes     liste des lignes de commande
     * @param mode       mode de paiement (CARTE ou ESPECES)
     * @param adresse    adresse de livraison validée
     * @return la commande créée avec son ID
     */
    public Commande createFromPanier(int idAcheteur, int idVendeur,
                                     List<LigneCommande> lignes,
                                     ModePaiement mode,
                                     String adresse) {
        // Calcul du total
        double total = lignes.stream()
                .mapToDouble(l -> l.getQuantite() * l.getPrixUnitaire())
                .sum();

        // Génération du numéro de commande
        String numeroCommande = genererNumeroCommande();

        Commande commande = new Commande();
        commande.setNumeroCommande(numeroCommande);
        commande.setIdAcheteur(idAcheteur);
        commande.setIdVendeur(idVendeur);
        commande.setAdresseLivraison(adresse);
        commande.setModePaiement(mode);
        commande.setTotal(total);
        commande.setStatut(StatutCommande.CREEE);
        commande.setDateCreation(LocalDateTime.now());

        // Score de priorité calculé avant insertion
        commande.setScorePriorite(calculerScore(commande));

        // Persistance de la commande
        commande = add(commande);

        // Persistance des lignes
        LigneCommandeService ligneService = new LigneCommandeService();
        for (LigneCommande ligne : lignes) {
            ligne.setIdCommande(commande.getIdCommande());
            ligne.setSousTotal(ligne.getQuantite() * ligne.getPrixUnitaire());
            ligneService.add(ligne);
        }

        return commande;
    }

    // =========================================================
    // CONSULTATION PAR ACTEUR
    // =========================================================

    /**
     * Retourne toutes les commandes d'un acheteur, triées par date décroissante.
     */
    public List<Commande> getByAcheteur(int idAcheteur) {
        List<Commande> list = new ArrayList<>();
        String sql = "SELECT * FROM commandes WHERE id_acheteur = ? ORDER BY date_creation DESC";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idAcheteur);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) {
            System.err.println("[CommandeService.getByAcheteur] Erreur : " + e.getMessage());
        }
        return list;
    }

    /**
     * Retourne toutes les commandes d'un vendeur, triées par date décroissante.
     */
    public List<Commande> getByVendeur(int idVendeur) {
        List<Commande> list = new ArrayList<>();
        String sql = "SELECT * FROM commandes WHERE id_vendeur = ? ORDER BY date_creation DESC";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idVendeur);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) {
            System.err.println("[CommandeService.getByVendeur] Erreur : " + e.getMessage());
        }
        return list;
    }

    /**
     * Vue globale admin : toutes les commandes avec détails.
     */
    public List<Commande> getAllForAdmin() {
        return getAll();
    }

    // =========================================================
    // MODIFICATION (ACHETEUR — AVANT EXPEDITION)
    // =========================================================

    /**
     * Met à jour l'adresse de livraison d'une commande.
     * Autorisé uniquement si statut != EXPEDIEE / LIVREE / ANNULEE / REFUSEE.
     */
    public void updateAdresse(int idCommande, String nouvelleAdresse) {
        if (!peutEtreModifiee(idCommande)) {
            throw new IllegalStateException("Modification impossible : commande déjà expédiée ou terminée.");
        }
        String sql = "UPDATE commandes SET adresse_livraison = ? WHERE id_commande = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, nouvelleAdresse);
            ps.setInt(2, idCommande);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[CommandeService.updateAdresse] Erreur : " + e.getMessage());
        }
    }

    /**
     * Remplace toutes les lignes d'une commande par de nouvelles lignes.
     * Recalcule automatiquement le total.
     */
    public void updateLignes(int idCommande, List<LigneCommande> nouvellesLignes) {
        if (!peutEtreModifiee(idCommande)) {
            throw new IllegalStateException("Modification impossible : commande déjà expédiée ou terminée.");
        }
        LigneCommandeService ligneService = new LigneCommandeService();

        // Suppression des anciennes lignes
        List<LigneCommande> anciennes = ligneService.getByCommande(idCommande);
        for (LigneCommande l : anciennes) {
            ligneService.delete(l.getIdLigne());
        }

        // Ajout des nouvelles lignes
        double nouveauTotal = 0;
        for (LigneCommande ligne : nouvellesLignes) {
            ligne.setIdCommande(idCommande);
            ligne.setSousTotal(ligne.getQuantite() * ligne.getPrixUnitaire());
            ligneService.add(ligne);
            nouveauTotal += ligne.getSousTotal();
        }

        // Recalcul du total
        String sql = "UPDATE commandes SET total = ? WHERE id_commande = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDouble(1, nouveauTotal);
            ps.setInt(2, idCommande);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[CommandeService.updateLignes] Erreur : " + e.getMessage());
        }
    }

    /**
     * Met à jour la date de livraison préférée.
     * Si null → livraison standard automatique.
     */
    public void updateDateLivraisonPreferee(int idCommande, LocalDate date) {
        if (!peutEtreModifiee(idCommande)) {
            throw new IllegalStateException("Modification impossible : commande déjà expédiée ou terminée.");
        }
        String sql = "UPDATE commandes SET date_livraison_preferee = ? WHERE id_commande = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, date);
            ps.setInt(2, idCommande);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[CommandeService.updateDateLivraisonPreferee] Erreur : " + e.getMessage());
        }
    }

    // =========================================================
    // CYCLE DE VIE — TRANSITIONS DE STATUT
    // =========================================================

    /**
     * Acheteur annule sa commande.
     * Autorisé uniquement si statut = CREEE, CONFIRMEE ou EN_PREPARATION.
     */
    public void annuler(int idCommande) {
        Commande c = getById(idCommande);
        if (c == null) throw new IllegalArgumentException("Commande introuvable : " + idCommande);

        StatutCommande statut = c.getStatut();
        if (statut != StatutCommande.CREEE
                && statut != StatutCommande.CONFIRMEE
                && statut != StatutCommande.EN_PREPARATION) {
            throw new IllegalStateException(
                    "Annulation impossible. Statut actuel : " + statut.name()
                            + ". L'annulation est impossible après expédition."
            );
        }
        changerStatut(idCommande, StatutCommande.ANNULEE);
    }

    /**
     * Vendeur confirme une commande.
     * Transition : CREEE → CONFIRMEE.
     */
    public void confirmer(int idCommande) {
        verifierTransition(idCommande, StatutCommande.CREEE, StatutCommande.CONFIRMEE);
        changerStatut(idCommande, StatutCommande.CONFIRMEE);
    }

    /**
     * Vendeur refuse une commande avec un motif obligatoire.
     * Un email automatique est envoyé à l'acheteur via EmailService.
     */
    public void refuser(int idCommande, String motif) {
        if (motif == null || motif.isBlank()) {
            throw new IllegalArgumentException("Le motif de refus est obligatoire.");
        }
        Commande c = getById(idCommande);
        if (c == null) throw new IllegalArgumentException("Commande introuvable : " + idCommande);

        String sql = "UPDATE commandes SET statut = 'REFUSEE', motif_refus = ? WHERE id_commande = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, motif);
            ps.setInt(2, idCommande);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[CommandeService.refuser] Erreur : " + e.getMessage());
        }
        // ── Email notification to acheteur ──────────────────────
        String email = getEmailUtilisateur(c.getIdAcheteur());
        if (email != null) {
            EmailService emailService = new EmailService();
            emailService.envoyerEmailRefus(email, c, motif);
        }
    }

    /**
     * Vendeur passe la commande en préparation.
     * Transition : CONFIRMEE → EN_PREPARATION.
     */
    public void passerEnPreparation(int idCommande) {
        verifierTransition(idCommande, StatutCommande.CONFIRMEE, StatutCommande.EN_PREPARATION);
        changerStatut(idCommande, StatutCommande.EN_PREPARATION);
    }

    /**
     * Vendeur expédie la commande.
     * Transition : EN_PREPARATION → EXPEDIEE.
     */
    public void expedier(int idCommande) {
        verifierTransition(idCommande, StatutCommande.EN_PREPARATION, StatutCommande.EXPEDIEE);
        changerStatut(idCommande, StatutCommande.EXPEDIEE);

        // ── Email notification to acheteur ──────────────────────
        Commande c = getById(idCommande);
        if (c != null) {
            String email = getEmailUtilisateur(c.getIdAcheteur());
            if (email != null) {
                new EmailService().envoyerEmailExpedition(email, c);
            }
        }
    }

    /**
     * Marque la commande comme livrée.
     * Transition : EXPEDIEE → LIVREE.
     */
    public void livrer(int idCommande) {
        verifierTransition(idCommande, StatutCommande.EXPEDIEE, StatutCommande.LIVREE);
        changerStatut(idCommande, StatutCommande.LIVREE);
    }

    // =========================================================
    // PRIORISATION INTELLIGENTE (IA VENDEUR)
    // =========================================================

    /**
     * Calcule le score de priorité d'une commande selon les règles métier :
     *   +20 si acheteur VIP
     *   +25 Médicament / +20 Alimentaire / +15 Electronique /
     *   +10 Hygiène / +8 Makeup / +5 Décor
     *   +20 si commande déjà payée par carte
     *
     * La catégorie du produit est récupérée depuis les lignes de commande.
     * Si plusieurs catégories → on prend le score max parmi les lignes.
     */
    public int calculerScore(Commande commande) {
        int score = 0;

        // Score VIP
        if (isAcheteurVIP(commande.getIdAcheteur())) {
            score += 20;
        }

        // Score paiement
        if (commande.getModePaiement() == ModePaiement.CARTE) {
            score += 20;
        }

        // Score catégorie — récupéré depuis les produit liés aux lignes
        if (commande.getIdCommande() > 0) {
            String sql = """
                SELECT MAX(
                    CASE p.categorie
                        WHEN 'MEDICAMENT'   THEN 25
                        WHEN 'ALIMENTAIRE'  THEN 20
                        WHEN 'ELECTRONIQUE' THEN 15
                        WHEN 'HYGIENE'      THEN 10
                        WHEN 'MAKEUP'       THEN 8
                        WHEN 'DECOR'        THEN 5
                        ELSE 0
                    END
                ) AS score_categorie
                FROM ligne_commandes lc
                JOIN produit p ON lc.id_produit = p.id_produit
                WHERE lc.id_commande = ?
                """;
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, commande.getIdCommande());
                ResultSet rs = ps.executeQuery();
                if (rs.next()) score += rs.getInt("score_categorie");
            } catch (SQLException e) {
                System.err.println("[CommandeService.calculerScore] Erreur catégorie : " + e.getMessage());
            }
        }

        return score;
    }

    /**
     * Retourne les commandes d'un vendeur triées par score de priorité décroissant.
     */
    public List<Commande> getCommandesPrioritisees(int idVendeur) {
        List<Commande> list = new ArrayList<>();
        String sql = """
            SELECT * FROM commandes
            WHERE id_vendeur = ?
            ORDER BY score_priorite DESC, date_creation ASC
            """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idVendeur);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) {
            System.err.println("[CommandeService.getCommandesPrioritisees] Erreur : " + e.getMessage());
        }
        return list;
    }

    // =========================================================
    // PROGRAMME VIP
    // =========================================================

    /**
     * Vérifie si un acheteur est VIP (plus de 3 commandes passées).
     * Seules les commandes non annulées / non refusées sont comptées.
     */
    public boolean isAcheteurVIP(int idAcheteur) {
        String sql = """
            SELECT COUNT(*) AS nb FROM commandes
            WHERE id_acheteur = ?
              AND statut NOT IN ('ANNULEE', 'REFUSEE')
            """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idAcheteur);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt("nb") > 3;
        } catch (SQLException e) {
            System.err.println("[CommandeService.isAcheteurVIP] Erreur : " + e.getMessage());
        }
        return false;
    }

    // =========================================================
    // RUPTURE DE STOCK
    // =========================================================

    /**
     * Signale qu'un produit est indisponible dans une commande.
     * Supprime la ligne correspondante et recalcule le total.
     * Envoie une notification à l'acheteur.
     * Si la commande n'a plus aucune ligne → annulation automatique.
     */
    public void signalerProduitIndisponible(int idCommande, int idProduit) {
        LigneCommandeService ligneService = new LigneCommandeService();
        List<LigneCommande> lignes = ligneService.getByCommande(idCommande);

        // Suppression de la ligne concernée
        lignes.stream()
                .filter(l -> l.getIdProduit() == idProduit)
                .findFirst()
                .ifPresent(l -> ligneService.delete(l.getIdLigne()));

        // Recalcul du total
        List<LigneCommande> restantes = ligneService.getByCommande(idCommande);
        if (restantes.isEmpty()) {
            // Plus aucun produit → annulation automatique
            changerStatut(idCommande, StatutCommande.ANNULEE);
        } else {
            double nouveauTotal = restantes.stream()
                    .mapToDouble(LigneCommande::getSousTotal)
                    .sum();
            String sql = "UPDATE commandes SET total = ? WHERE id_commande = ?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setDouble(1, nouveauTotal);
                ps.setInt(2, idCommande);
                ps.executeUpdate();
            } catch (SQLException e) {
                System.err.println("[CommandeService.signalerProduitIndisponible] Erreur : " + e.getMessage());
            }
        }

        // Notification acheteur via NotificationService
        Commande c = getById(idCommande);
        if (c != null) {
            NotificationService notifService = new NotificationService();
            notifService.envoyerNotification(
                    c.getIdAcheteur(),
                    projet.hanouti.GestionCommandes.enums.TypeNotification.COMMANDE,
                    projet.hanouti.GestionCommandes.enums.EventNotification.PRODUIT_INDISPONIBLE,
                    "Produit indisponible",
                    "Un produit de votre commande " + c.getNumeroCommande() + " n'est plus disponible.",
                    idCommande
            );
        }
    }

    // =========================================================
    // METHODES PRIVEES UTILITAIRES
    // =========================================================

    /**
     * Vérifie si la commande peut encore être modifiée par l'acheteur.
     * Modification autorisée uniquement avant expédition.
     */
    private boolean peutEtreModifiee(int idCommande) {
        Commande c = getById(idCommande);
        if (c == null) return false;
        StatutCommande s = c.getStatut();
        if (s == StatutCommande.CREEE
                || s == StatutCommande.CONFIRMEE
                || s == StatutCommande.EN_PREPARATION) {
            return true;
        }
        if (s == StatutCommande.REFUSEE) {
            String motif = c.getMotifRefus();
            return motif != null && motif.toLowerCase().contains("adresse");
        }
        return false;
    }

    /**
     * Vérifie qu'une transition de statut est valide avant de l'exécuter.
     */
    private void verifierTransition(int idCommande,
                                    StatutCommande statutAttendu,
                                    StatutCommande statutCible) {
        Commande c = getById(idCommande);
        if (c == null) throw new IllegalArgumentException("Commande introuvable : " + idCommande);
        if (c.getStatut() != statutAttendu) {
            throw new IllegalStateException(
                    "Transition invalide. Statut actuel : " + c.getStatut().name()
                            + " | Attendu : " + statutAttendu.name()
                            + " | Cible : " + statutCible.name()
            );
        }
    }

    /**
     * Exécute le changement de statut d'une commande en base.
     */
    private void changerStatut(int idCommande, StatutCommande nouveauStatut) {
        String sql = "UPDATE commandes SET statut = ? WHERE id_commande = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, nouveauStatut.name());
            ps.setInt(2, idCommande);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[CommandeService.changerStatut] Erreur : " + e.getMessage());
        }
    }

    /**
     * Génère un numéro de commande unique au format : CMD-YYYYMMDD-XXXXX
     * Ex : CMD-20250512-00042
     */
    private String genererNumeroCommande() {
        String date = LocalDate.now().toString().replace("-", "");
        String sql = "SELECT COUNT(*) AS total FROM commandes WHERE DATE(date_creation) = CURDATE()";
        int seq = 1;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ResultSet rs = ps.executeQuery();
            if (rs.next()) seq = rs.getInt("total") + 1;
        } catch (SQLException e) {
            System.err.println("[CommandeService.genererNumeroCommande] Erreur : " + e.getMessage());
        }
        return String.format("CMD-%s-%05d", date, seq);
    }

    /**
     * Mappe une ligne ResultSet vers un objet Commande.
     */
    private Commande mapRow(ResultSet rs) throws SQLException {
        Commande c = new Commande();
        c.setIdCommande(rs.getInt("id_commande"));
        c.setNumeroCommande(rs.getString("numero_commande"));
        c.setIdAcheteur(rs.getInt("id_acheteur"));
        c.setIdVendeur(rs.getInt("id_vendeur"));

        int idSociete = rs.getInt("id_societe_livraison");
        c.setIdSocieteLivraison(rs.wasNull() ? null : idSociete);

        c.setAdresseLivraison(rs.getString("adresse_livraison"));

        Timestamp ts = rs.getTimestamp("date_creation");
        if (ts != null) c.setDateCreation(ts.toLocalDateTime());

        java.sql.Date datePref = rs.getDate("date_livraison_preferee");
        if (datePref != null) c.setDateLivraisonPreferee(datePref.toLocalDate());

        c.setModePaiement(ModePaiement.valueOf(rs.getString("mode_paiement")));
        c.setTotal(rs.getDouble("total"));
        c.setScorePriorite(rs.getInt("score_priorite"));
        c.setStatut(StatutCommande.valueOf(rs.getString("statut")));
        c.setMotifRefus(rs.getString("motif_refus"));
        c.setFacturePdf(rs.getString("facture_pdf"));
        c.setFactureQr(rs.getString("facture_qr"));
        return c;
    }

    /**
     * Fetches the email of a user (acheteur or vendeur) by their user ID.
     * Works whether you used views or physical tables.
     */
    private String getEmailUtilisateur(int idUser) {
        String sql = "SELECT e_mail FROM users WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idUser);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getString("e_mail");
        } catch (SQLException e) {
            System.err.println("[CommandeService.getEmailUtilisateur] " + e.getMessage());
        }
        return null;
    }
}