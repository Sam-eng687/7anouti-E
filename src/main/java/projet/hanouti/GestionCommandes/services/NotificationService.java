package projet.hanouti.GestionCommandes.services;

import projet.hanouti.GestionCommandes.entities.Notification;
import projet.hanouti.GestionCommandes.enums.EventNotification;
import projet.hanouti.GestionCommandes.enums.TypeNotification;
import projet.hanouti.GestionCommandes.interfaces.IntService;
import projet.hanouti.common.utils.MyBD;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Service transversal de gestion des notifications.
 *
 * Ce service est utilisé par tous les modules de l'application :
 * commandes, stock, livraison, système.
 * Les notifications sont stockées en base et affichées dans l'interface
 * via l'icône de cloche (🔔) dans le header.
 */
public class NotificationService implements IntService<Notification> {

    private final Connection conn;

    public NotificationService() {
        this.conn = MyBD.getInstance().getConnection();
    }

    // =========================================================
    // ISERVICE — CRUD DE BASE
    // =========================================================

    /**
     * Persiste une notification en base de données.
     */
    @Override
    public Notification add(Notification notif) {
        String sql = """
            INSERT INTO notifications
              (user_id, type, event, titre, message, reference_id, is_read, date_creation)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            """;
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, notif.getUserId());
            ps.setString(2, notif.getType().name());
            ps.setString(3, notif.getEvent().name());
            ps.setString(4, notif.getTitre());
            ps.setString(5, notif.getMessage());

            if (notif.getReferenceId() != null)
                ps.setInt(6, notif.getReferenceId());
            else
                ps.setNull(6, Types.INTEGER);

            ps.setBoolean(7, notif.isRead());
            ps.setObject(8, notif.getDateCreation() != null
                    ? notif.getDateCreation()
                    : LocalDateTime.now());

            ps.executeUpdate();

            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) notif.setNotificationId(keys.getInt(1));

        } catch (SQLException e) {
            System.err.println("[NotificationService.add] Erreur : " + e.getMessage());
        }
        return notif;
    }

    /**
     * Supprime une notification par son ID.
     */
    @Override
    public void delete(int id) {
        String sql = "DELETE FROM notifications WHERE notification_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[NotificationService.delete] Erreur : " + e.getMessage());
        }
    }

    /**
     * Met à jour une notification (principalement is_read).
     */
    @Override
    public Notification update(Notification notif) {
        String sql = """
            UPDATE notifications SET
              titre       = ?,
              message     = ?,
              is_read     = ?
            WHERE notification_id = ?
            """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, notif.getTitre());
            ps.setString(2, notif.getMessage());
            ps.setBoolean(3, notif.isRead());
            ps.setInt(4, notif.getNotificationId());
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[NotificationService.update] Erreur : " + e.getMessage());
        }
        return notif;
    }

    /**
     * Récupère une notification par son ID.
     */
    @Override
    public Notification getById(int id) {
        String sql = "SELECT * FROM notifications WHERE notification_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapRow(rs);
        } catch (SQLException e) {
            System.err.println("[NotificationService.getById] Erreur : " + e.getMessage());
        }
        return null;
    }

    /**
     * Récupère toutes les notifications (toutes les tables, admin).
     */
    @Override
    public List<Notification> getAll() {
        List<Notification> list = new ArrayList<>();
        String sql = "SELECT * FROM notifications ORDER BY date_creation DESC";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) {
            System.err.println("[NotificationService.getAll] Erreur : " + e.getMessage());
        }
        return list;
    }

    // =========================================================
    // METHODE PRINCIPALE : ENVOI DE NOTIFICATION
    // =========================================================

    /**
     * Crée et envoie une notification à un utilisateur.
     * Point d'entrée principal utilisé par tous les autres services.
     *
     * @param userId      ID de l'utilisateur destinataire
     * @param type        type de notification (COMMANDE, LIVRAISON, STOCK, SYSTEME)
     * @param event       événement déclencheur
     * @param titre       titre court de la notification
     * @param message     message détaillé
     * @param referenceId ID de référence (ex: id_commande), nullable
     */
    public void envoyerNotification(int userId,
                                    TypeNotification type,
                                    EventNotification event,
                                    String titre,
                                    String message,
                                    Integer referenceId) {
        Notification notif = new Notification();
        notif.setUserId(userId);
        notif.setType(type);
        notif.setEvent(event);
        notif.setTitre(titre);
        notif.setMessage(message);
        notif.setReferenceId(referenceId);
        notif.setRead(false);
        notif.setDateCreation(LocalDateTime.now());
        add(notif);
    }

    // =========================================================
    // CONSULTATION PAR UTILISATEUR
    // =========================================================

    /**
     * Récupère toutes les notifications d'un utilisateur,
     * triées par date décroissante (plus récentes en premier).
     *
     * @param userId ID de l'utilisateur
     * @return liste de toutes ses notifications
     */
    public List<Notification> getByUser(int userId) {
        List<Notification> list = new ArrayList<>();
        String sql = """
            SELECT * FROM notifications
            WHERE user_id = ?
            ORDER BY date_creation DESC
            """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) {
            System.err.println("[NotificationService.getByUser] Erreur : " + e.getMessage());
        }
        return list;
    }

    /**
     * Récupère uniquement les notifications non lues d'un utilisateur.
     * Utilisé pour alimenter le badge de la cloche 🔔.
     *
     * @param userId ID de l'utilisateur
     * @return liste des notifications non lues
     */
    public List<Notification> getNonLues(int userId) {
        List<Notification> list = new ArrayList<>();
        String sql = """
            SELECT * FROM notifications
            WHERE user_id = ? AND is_read = FALSE
            ORDER BY date_creation DESC
            """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) {
            System.err.println("[NotificationService.getNonLues] Erreur : " + e.getMessage());
        }
        return list;
    }

    /**
     * Récupère les notifications d'un utilisateur filtrées par type.
     *
     * @param userId ID de l'utilisateur
     * @param type   type de notification (COMMANDE, LIVRAISON, etc.)
     * @return liste filtrée
     */
    public List<Notification> getByUserAndType(int userId, TypeNotification type) {
        List<Notification> list = new ArrayList<>();
        String sql = """
            SELECT * FROM notifications
            WHERE user_id = ? AND type = ?
            ORDER BY date_creation DESC
            """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, type.name());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) {
            System.err.println("[NotificationService.getByUserAndType] Erreur : " + e.getMessage());
        }
        return list;
    }

    // =========================================================
    // GESTION DES LECTURES
    // =========================================================

    /**
     * Marque une notification spécifique comme lue.
     *
     * @param notificationId ID de la notification
     */
    public void marquerCommeLue(int notificationId) {
        String sql = "UPDATE notifications SET is_read = TRUE WHERE notification_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, notificationId);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[NotificationService.marquerCommeLue] Erreur : " + e.getMessage());
        }
    }

    /**
     * Marque toutes les notifications d'un utilisateur comme lues.
     * Appelé quand l'utilisateur ouvre le panneau de notifications.
     *
     * @param userId ID de l'utilisateur
     */
    public void marquerToutesCommeLues(int userId) {
        String sql = "UPDATE notifications SET is_read = TRUE WHERE user_id = ? AND is_read = FALSE";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[NotificationService.marquerToutesCommeLues] Erreur : " + e.getMessage());
        }
    }

    // =========================================================
    // COMPTAGE — BADGE CLOCHE
    // =========================================================

    /**
     * Retourne le nombre de notifications non lues d'un utilisateur.
     * Utilisé pour afficher le badge numérique sur l'icône 🔔.
     *
     * @param userId ID de l'utilisateur
     * @return nombre de notifications non lues
     */
    public int countNonLues(int userId) {
        String sql = "SELECT COUNT(*) AS nb FROM notifications WHERE user_id = ? AND is_read = FALSE";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt("nb");
        } catch (SQLException e) {
            System.err.println("[NotificationService.countNonLues] Erreur : " + e.getMessage());
        }
        return 0;
    }

    // =========================================================
    // SUPPRESSION EN LOT
    // =========================================================

    /**
     * Supprime toutes les notifications d'un utilisateur.
     *
     * @param userId ID de l'utilisateur
     */
    public void deleteAllByUser(int userId) {
        String sql = "DELETE FROM notifications WHERE user_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[NotificationService.deleteAllByUser] Erreur : " + e.getMessage());
        }
    }

    /**
     * Supprime toutes les notifications déjà lues d'un utilisateur.
     *
     * @param userId ID de l'utilisateur
     */
    public void deleteAllLuesByUser(int userId) {
        String sql = "DELETE FROM notifications WHERE user_id = ? AND is_read = TRUE";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[NotificationService.deleteAllLuesByUser] Erreur : " + e.getMessage());
        }
    }

    // =========================================================
    // METHODE PRIVEE — MAPPING
    // =========================================================

    /**
     * Mappe une ligne ResultSet vers un objet Notification.
     */
    private Notification mapRow(ResultSet rs) throws SQLException {
        Notification n = new Notification();
        n.setNotificationId(rs.getInt("notification_id"));
        n.setUserId(rs.getInt("user_id"));
        n.setType(TypeNotification.valueOf(rs.getString("type")));
        n.setEvent(EventNotification.valueOf(rs.getString("event")));
        n.setTitre(rs.getString("titre"));
        n.setMessage(rs.getString("message"));

        int refId = rs.getInt("reference_id");
        n.setReferenceId(rs.wasNull() ? null : refId);

        n.setRead(rs.getBoolean("is_read"));

        Timestamp ts = rs.getTimestamp("date_creation");
        if (ts != null) n.setDateCreation(ts.toLocalDateTime());

        return n;
    }
}