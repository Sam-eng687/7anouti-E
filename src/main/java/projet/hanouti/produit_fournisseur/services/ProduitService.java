package projet.hanouti.produit_fournisseur.services;

import projet.hanouti.produit_fournisseur.entities.Produit;
import projet.hanouti.produit_fournisseur.interfaces.IService;
import projet.hanouti.produit_fournisseur.utils.MyConnection;
import projet.hanouti.produit_fournisseur.utils.SessionManager;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ProduitService implements IService<Produit> {

    private final Connection cnx = MyConnection.getInstance().getCnx();

    @Override
    public void addEntity(Produit p) {
        String req = "INSERT INTO Produit (id_vendeur, id_fournisseur, nom, description, categorie, prix, quantite_stock, seuil_alerte, image, statut) VALUES (?,?,?,?,?,?,?,?,?,?)";
        try {
            PreparedStatement pst = cnx.prepareStatement(req);
            pst.setInt(1, p.getIdVendeur());
            if (p.getIdFournisseur() != null) pst.setInt(2, p.getIdFournisseur());
            else pst.setNull(2, Types.INTEGER);
            pst.setString(3,  p.getNom());
            pst.setString(4,  p.getDescription());
            pst.setString(5,  p.getCategorie());
            pst.setDouble(6,  p.getPrix());
            pst.setInt   (7,  p.getQuantiteStock());
            pst.setInt   (8,  p.getSeuilAlerte());
            pst.setString(9,  p.getImage());
            pst.setString(10, p.getStatut() != null ? p.getStatut() : "ACTIF");
            pst.executeUpdate();
            System.out.println("Produit ajoute!");
        } catch (SQLException e) { System.out.println(e.getMessage()); }
    }

    @Override
    public void deleteEntity(Produit p) {
        String req = "DELETE FROM Produit WHERE id_produit = ? AND id_vendeur = ?";
        try {
            PreparedStatement pst = cnx.prepareStatement(req);
            pst.setInt(1, p.getIdProduit());
            pst.setInt(2, p.getIdVendeur());
            pst.executeUpdate();
        } catch (SQLException e) { System.out.println(e.getMessage()); }
    }

    @Override
    public void updateEntity(int id, Produit p) {
        String req = "UPDATE Produit SET id_fournisseur=?, nom=?, description=?, categorie=?, prix=?, quantite_stock=?, seuil_alerte=?, image=?, statut=? WHERE id_produit=? AND id_vendeur=?";
        try {
            PreparedStatement pst = cnx.prepareStatement(req);
            if (p.getIdFournisseur() != null) pst.setInt(1, p.getIdFournisseur());
            else pst.setNull(1, Types.INTEGER);
            pst.setString(2,  p.getNom());
            pst.setString(3,  p.getDescription());
            pst.setString(4,  p.getCategorie());
            pst.setDouble(5,  p.getPrix());
            pst.setInt   (6,  p.getQuantiteStock());
            pst.setInt   (7,  p.getSeuilAlerte());
            pst.setString(8,  p.getImage());
            pst.setString(9,  p.getStatut());
            pst.setInt   (10, id);
            pst.setInt   (11, SessionManager.getCurrentVendeurId());
            pst.executeUpdate();
        } catch (SQLException e) { System.out.println(e.getMessage()); }
    }

    @Override
    public List<Produit> getData() {
        return getByVendeur(SessionManager.getCurrentVendeurId());
    }

    public List<Produit> getByVendeur(int vendeurId) {
        List<Produit> list = new ArrayList<>();
        String req = "SELECT * FROM Produit WHERE id_vendeur = ? AND statut != 'SUPPRIME' ORDER BY date_ajout DESC";
        try {
            PreparedStatement pst = cnx.prepareStatement(req);
            pst.setInt(1, vendeurId);
            ResultSet rs = pst.executeQuery();
            while (rs.next()) { list.add(map(rs)); }
        } catch (SQLException e) { System.out.println(e.getMessage()); }
        return list;
    }

    public List<Produit> searchByNom(String keyword) {
        List<Produit> list = new ArrayList<>();
        String req = "SELECT * FROM Produit WHERE id_vendeur=? AND nom LIKE ? AND statut != 'SUPPRIME'";
        try {
            PreparedStatement pst = cnx.prepareStatement(req);
            pst.setInt   (1, SessionManager.getCurrentVendeurId());
            pst.setString(2, "%" + keyword + "%");
            ResultSet rs = pst.executeQuery();
            while (rs.next()) { list.add(map(rs)); }
        } catch (SQLException e) { System.out.println(e.getMessage()); }
        return list;
    }

    // Check duplicate with price included
    public Produit findExisting(String nom, String categorie,
                                Integer idFournisseur, int idVendeur, double prix) {
        String req = "SELECT * FROM Produit WHERE nom=? AND categorie=? " +
                "AND id_vendeur=? AND prix=? AND statut != 'SUPPRIME'";
        if (idFournisseur != null) req += " AND id_fournisseur=?";
        try {
            PreparedStatement pst = cnx.prepareStatement(req);
            pst.setString(1, nom);
            pst.setString(2, categorie);
            pst.setInt   (3, idVendeur);
            pst.setDouble(4, prix);
            if (idFournisseur != null) pst.setInt(5, idFournisseur);
            ResultSet rs = pst.executeQuery();
            if (rs.next()) return map(rs);
        } catch (SQLException e) { System.out.println(e.getMessage()); }
        return null;
    }

    // Check duplicate excluding current product being edited, with price
    public Produit findExistingExcluding(String nom, String categorie,
                                         Integer idFournisseur, int idVendeur,
                                         int excludeId, double prix) {
        String req = "SELECT * FROM Produit WHERE nom=? AND categorie=? " +
                "AND id_vendeur=? AND id_produit != ? " +
                "AND prix=? AND statut != 'SUPPRIME'";
        if (idFournisseur != null) req += " AND id_fournisseur=?";
        try {
            PreparedStatement pst = cnx.prepareStatement(req);
            pst.setString(1, nom);
            pst.setString(2, categorie);
            pst.setInt   (3, idVendeur);
            pst.setInt   (4, excludeId);
            pst.setDouble(5, prix);
            if (idFournisseur != null) pst.setInt(6, idFournisseur);
            ResultSet rs = pst.executeQuery();
            if (rs.next()) return map(rs);
        } catch (SQLException e) { System.out.println(e.getMessage()); }
        return null;
    }

    public void increaseStock(int idProduit, int quantiteToAdd) {
        String req = "UPDATE Produit SET quantite_stock = quantite_stock + ? " +
                "WHERE id_produit = ?";
        try {
            PreparedStatement pst = cnx.prepareStatement(req);
            pst.setInt(1, quantiteToAdd);
            pst.setInt(2, idProduit);
            pst.executeUpdate();
            System.out.println("Stock augmente de " + quantiteToAdd);
        } catch (SQLException e) { System.out.println(e.getMessage()); }
    }

    public List<Produit> getAllActiveProducts() {
        List<Produit> list = new ArrayList<>();
        String req = "SELECT * FROM Produit WHERE statut = 'ACTIF' ORDER BY date_ajout DESC";
        try {
            PreparedStatement pst = cnx.prepareStatement(req);
            ResultSet rs = pst.executeQuery();
            while (rs.next()) { list.add(map(rs)); }
        } catch (SQLException e) { System.out.println(e.getMessage()); }
        return list;
    }

    public void ajouterAuPanier(int userId, int produitId, double prixUnitaire) {
        String checkSql = "SELECT panier_id, quantite FROM panier " +
                "WHERE user_id = ? AND produit_id = ? AND statut = 'actif'";
        try {
            PreparedStatement check = cnx.prepareStatement(checkSql);
            check.setInt(1, userId);
            check.setInt(2, produitId);
            ResultSet rs = check.executeQuery();

            if (rs.next()) {
                int panierId = rs.getInt("panier_id");
                int newQty   = rs.getInt("quantite") + 1;
                PreparedStatement upd = cnx.prepareStatement(
                        "UPDATE panier SET quantite = ? WHERE panier_id = ?");
                upd.setInt(1, newQty);
                upd.setInt(2, panierId);
                upd.executeUpdate();
            } else {
                PreparedStatement pst = cnx.prepareStatement(
                        "INSERT INTO panier (user_id, produit_id, quantite, prix_unitaire, statut) " +
                                "VALUES (?, ?, 1, ?, 'actif')");
                pst.setInt   (1, userId);
                pst.setInt   (2, produitId);
                pst.setDouble(3, prixUnitaire);
                pst.executeUpdate();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    private Produit map(ResultSet rs) throws SQLException {
        Produit p = new Produit();
        p.setIdProduit    (rs.getInt   ("id_produit"));
        p.setIdVendeur    (rs.getInt   ("id_vendeur"));
        int idF = rs.getInt("id_fournisseur");
        p.setIdFournisseur(rs.wasNull() ? null : idF);
        p.setNom          (rs.getString("nom"));
        p.setDescription  (rs.getString("description"));
        p.setCategorie    (rs.getString("categorie"));
        p.setPrix         (rs.getDouble("prix"));
        p.setQuantiteStock(rs.getInt   ("quantite_stock"));
        p.setSeuilAlerte  (rs.getInt   ("seuil_alerte"));
        p.setImage        (rs.getString("image"));
        p.setStatut       (rs.getString("statut"));
        Timestamp ts = rs.getTimestamp("date_ajout");
        if (ts != null) p.setDateAjout(ts.toLocalDateTime());
        p.setMoyenne(getOptionalFloat(rs, "moyenne", getOptionalFloat(rs, "moyenne", 0)));
        return p;
    }

    private float getOptionalFloat(ResultSet rs, String column, float fallback) {
        try {
            float value = rs.getFloat(column);
            return rs.wasNull() ? fallback : value;
        } catch (SQLException ignored) {
            return fallback;
        }
    }
    public static class PromoInfo {
        public final String        titreAcheteur;
        public final float         discount;
        public final double        prixApres;
        public final java.time.LocalDateTime dateExpiration;

        public PromoInfo(String titreAcheteur, float discount,
                         double prixAvant, java.time.LocalDateTime dateExpiration) {
            this.titreAcheteur  = titreAcheteur;
            this.discount       = discount;
            this.prixApres      = prixAvant * (1 - discount / 100.0);
            this.dateExpiration = dateExpiration;
        }
    }
    public PromoInfo getPromoForProduit(int idProduit, double prixActuel) {
        String sql =
                "SELECT titre_acheteur, discount, date_expiration " +
                        "FROM conseils_ia " +
                        "WHERE id_produit = ? " +
                        "  AND type = 'Promotion' " +
                        "  AND etat = 'ACCEPTE' " +
                        "  AND (date_expiration IS NULL OR date_expiration > NOW()) " +
                        "ORDER BY score DESC LIMIT 1";
        try {
            PreparedStatement pst = cnx.prepareStatement(sql);
            pst.setInt(1, idProduit);
            ResultSet rs = pst.executeQuery();
            if (rs.next()) {
                String titre    = rs.getString("titre_acheteur");
                float  discount = rs.getFloat ("discount");
                java.sql.Timestamp ts = rs.getTimestamp("date_expiration");
                java.time.LocalDateTime exp = ts != null ? ts.toLocalDateTime() : null;
                return new PromoInfo(titre, discount, prixActuel, exp);
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return null;
    }
    public void saveRating(int idProduit, float newNote) {
        Connection cn = MyConnection.getInstance().getCnx();
        int idAcheteur = SessionManager.getCurrentAcheteurId();
        if (idAcheteur <= 0) {
            throw new RuntimeException("Aucun acheteur connecte.");
        }
        try {
            ensureRatingTable(cn);
            upsertBuyerRating(cn, idAcheteur, idProduit, newNote);
            boolean updated = updateRatingColumnIfExists(cn, idProduit, "moyenne");
            updated = updateRatingColumnIfExists(cn, idProduit, "moyenne") || updated;
            if (!updated) {
                throw new SQLException("Colonne moyenne introuvable.");
            }
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    private void ensureRatingTable(Connection cn) throws SQLException {
        try (Statement st = cn.createStatement()) {
            st.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS produit_rating (
                        id_rating INT PRIMARY KEY AUTO_INCREMENT,
                        id_acheteur INT NOT NULL,
                        id_produit INT NOT NULL,
                        note FLOAT NOT NULL,
                        date_note DATETIME DEFAULT CURRENT_TIMESTAMP,
                        UNIQUE KEY uq_produit_rating (id_acheteur, id_produit)
                    )
                    """);
        }
    }

    private void upsertBuyerRating(Connection cn, int idAcheteur, int idProduit, float note) throws SQLException {
        String sql = """
                INSERT INTO produit_rating (id_acheteur, id_produit, note, date_note)
                VALUES (?, ?, ?, NOW())
                ON DUPLICATE KEY UPDATE note = VALUES(note), date_note = NOW()
                """;
        try (PreparedStatement pst = cn.prepareStatement(sql)) {
            pst.setInt(1, idAcheteur);
            pst.setInt(2, idProduit);
            pst.setFloat(3, note);
            pst.executeUpdate();
        }
    }

    private boolean updateRatingColumnIfExists(Connection cn, int idProduit, String column) throws SQLException {
        float moyenne = 0;
        try (PreparedStatement avg = cn.prepareStatement(
                "SELECT AVG(note) AS moyenne FROM produit_rating WHERE id_produit = ?")) {
            avg.setInt(1, idProduit);
            try (ResultSet rs = avg.executeQuery()) {
                if (rs.next()) moyenne = rs.getFloat("moyenne");
            }
        }

        try (PreparedStatement update = cn.prepareStatement(
                "UPDATE Produit SET " + column + " = ? WHERE id_produit = ?")) {
            update.setFloat(1, moyenne);
            update.setInt(2, idProduit);
            update.executeUpdate();
            return true;
        } catch (SQLException e) {
            if (isUnknownColumn(e)) return false;
            throw e;
        }
    }

    private boolean isUnknownColumn(SQLException e) {
        return e.getErrorCode() == 1054
                || (e.getMessage() != null && e.getMessage().toLowerCase().contains("unknown column"));
    }

    public Produit getById(int idProduit) {
        Connection cn = MyConnection.getInstance().getCnx();
        try {
            PreparedStatement ps = cn.prepareStatement(
                    "SELECT * FROM Produit WHERE id_produit = ?");
            ps.setInt(1, idProduit);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return map(rs);
            return null;
        } catch (SQLException e) {
            return null;
        }
    }

    public String getVendeurName(int idVendeur) {
        var current = SessionManager.getCurrentVendeur();
        if (current != null && current.getIdVendeur() == idVendeur) {
            String currentName = (safe(current.getPrenom()) + " " + safe(current.getNom())).trim();
            if (!currentName.isEmpty()) return currentName;
        }

        String fromUsers = getVendeurNameFromTable("users", idVendeur);
        if (!fromUsers.isEmpty()) return fromUsers;

        String fromUser = getVendeurNameFromTable("user", idVendeur);
        if (!fromUser.isEmpty()) return fromUser;

        return "Vendeur";
    }

    private String getVendeurNameFromTable(String tableName, int idVendeur) {
        try (PreparedStatement ps = cnx.prepareStatement(
                "SELECT prenom, nom, e_mail FROM " + tableName + " WHERE id = ?")) {
            ps.setInt(1, idVendeur);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String prenom = rs.getString("prenom");
                    String nom = rs.getString("nom");
                    String fullName = (safe(prenom) + " " + safe(nom)).trim();
                    if (!fullName.isEmpty()) return fullName;

                    String email = rs.getString("e_mail");
                    if (email != null && !email.isBlank()) return email;
                }
            }
        } catch (SQLException ignored) {
        }
        return "";
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
