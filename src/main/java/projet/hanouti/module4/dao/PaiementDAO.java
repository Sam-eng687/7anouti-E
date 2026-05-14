package projet.hanouti.module4.dao;

import projet.hanouti.module4.model.Paiement;
import projet.hanouti.module4.util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO Paiements.
 *
 * FIX BUG-06 :
 *   1. Tous les PreparedStatement sont dans des try-with-resources (plus de fuite).
 *   2. addPaiement() et ajouterPaiement() fusionnés — addPaiement() est conservé
 *      comme méthode principale, ajouterPaiement() délègue vers lui (rétrocompatibilité).
 *   3. getHistorique() filtre maintenant par user_id (comme getHistoriqueByUser).
 *      L'ancienne version sans filtre est gardée sous getHistoriqueAll() pour l'admin.
 */
public class PaiementDAO {

    private final Connection conn;

    public PaiementDAO() throws SQLException {
        conn = DBConnection.getInstance().getConnection();
    }

    // ──────────────────────────────────────────────────────
    // ADD — insérer un paiement (méthode principale)
    // ──────────────────────────────────────────────────────
    public void addPaiement(Paiement p) throws SQLException {
        String sql = "INSERT INTO paiements (commande_id, montant, methode, statut, reference_transaction) "
                + "VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, p.getCommandeId());
            ps.setDouble(2, p.getMontant());
            ps.setString(3, p.getMethode());
            ps.setString(4, p.getStatut());
            ps.setString(5, p.getReferenceTransaction() != null ? p.getReferenceTransaction() : "REF-" + System.currentTimeMillis());
            ps.executeUpdate();
        }
    }

    /**
     * FIX BUG-06 : anciennement dupliquait addPaiement() avec du code différent.
     * Désormais, délègue simplement vers addPaiement() pour rétrocompatibilité.
     */
    public void ajouterPaiement(Paiement paiement) throws SQLException {
        addPaiement(paiement);
    }

    // ──────────────────────────────────────────────────────
    // ADD avec user_id (méthode utilisée depuis PaiementController)
    // ──────────────────────────────────────────────────────
    public void addPaiementForUser(Paiement p, int userId) throws SQLException {
        String sql = "INSERT INTO paiements "
                + "(commande_id, montant, methode, statut, reference_transaction, user_id) "
                + "VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, p.getCommandeId());
            ps.setDouble(2, p.getMontant());
            ps.setString(3, p.getMethode());
            ps.setString(4, p.getStatut());
            ps.setString(5, p.getReferenceTransaction() != null ? p.getReferenceTransaction() : "REF-" + System.currentTimeMillis());
            ps.setInt(6, userId);
            ps.executeUpdate();
        }
    }

    // ──────────────────────────────────────────────────────
    // GET historique d'un utilisateur (méthode principale)
    // ──────────────────────────────────────────────────────
    public List<Paiement> getHistoriqueByUser(int userId) throws SQLException {
        List<Paiement> list = new ArrayList<>();
        String sql = "SELECT * FROM paiements WHERE user_id = ? ORDER BY date_paiement DESC";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        }
        return list;
    }

    /**
     * FIX BUG-06 : l'ancienne getHistorique() retournait TOUS les paiements,
     * ignorant le user_id. Elle est renommée getHistoriqueAll() et réservée
     * à un éventuel usage admin. getHistorique(int userId) filtre correctement.
     */
    public List<Paiement> getHistorique(int userId) throws SQLException {
        return getHistoriqueByUser(userId);
    }

    /** Tous les paiements — usage admin uniquement. */
    public List<Paiement> getHistoriqueAll() throws SQLException {
        List<Paiement> list = new ArrayList<>();
        String sql = "SELECT * FROM paiements ORDER BY date_paiement DESC";
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        }
        return list;
    }

    // ──────────────────────────────────────────────────────
    // UPDATE statut
    // ──────────────────────────────────────────────────────
    public void updateStatut(String reference, String newStatut) throws SQLException {
        String sql = "UPDATE paiements SET statut = ? WHERE reference_transaction = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newStatut);
            ps.setString(2, reference);
            ps.executeUpdate();
        }
    }

    // ──────────────────────────────────────────────────────
    // UPDATE méthode seulement
    // ──────────────────────────────────────────────────────
    public void modifierMethode(String reference, String nouvelleMethode) throws SQLException {
        String sql = "UPDATE paiements SET methode = ? WHERE reference_transaction = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, nouvelleMethode);
            ps.setString(2, reference);
            ps.executeUpdate();
        }
    }

    // ──────────────────────────────────────────────────────
    // UPDATE méthode + montant
    // ──────────────────────────────────────────────────────
    public void modifierPaiement(String reference, String nouvelleMethode, double nouveauMontant) throws SQLException {
        String sql = "UPDATE paiements SET methode = ?, montant = ? WHERE reference_transaction = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, nouvelleMethode);
            ps.setDouble(2, nouveauMontant);
            ps.setString(3, reference);
            ps.executeUpdate();
        }
    }

    // ──────────────────────────────────────────────────────
    // ANNULER (soft delete — statut = 'annulé')
    // ──────────────────────────────────────────────────────
    public void annulerPaiement(String reference) throws SQLException {
        String sql = "UPDATE paiements SET statut = 'annulé' WHERE reference_transaction = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, reference);
            ps.executeUpdate();
        }
    }

    // ──────────────────────────────────────────────────────
    // Helper — mapper un ResultSet vers un Paiement
    // ──────────────────────────────────────────────────────
    private Paiement mapRow(ResultSet rs) throws SQLException {
        Paiement p = new Paiement(
                rs.getInt("commande_id"),
                rs.getDouble("montant"),
                rs.getString("methode"),
                rs.getString("statut")
        );
        p.setPaiementId(rs.getInt("paiement_id"));
        p.setReferenceTransaction(rs.getString("reference_transaction"));
        p.setDatePaiement(rs.getTimestamp("date_paiement"));        return p;
    }
}