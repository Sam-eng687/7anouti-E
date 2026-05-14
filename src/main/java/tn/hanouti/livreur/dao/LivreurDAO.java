package tn.hanouti.livreur.dao;

import tn.hanouti.livreur.model.Livreur;
import tn.hanouti.util.DBConnection;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class LivreurDAO {

    private Connection cnx = DBConnection.getInstance().getCnx();

    // ─────────────────────────────────────────────
    // AJOUTER
    // ─────────────────────────────────────────────
    public void add(Livreur l) throws SQLException {
        if (existeDoublon(l.getNomLivreur(), l.getTelephone())) {
            throw new SQLException("Ce livreur existe déjà (même nom et téléphone) !");
        }
        String sql = "INSERT INTO Livreur (nom_livreur, telephone, id_societe_livraison, " +
                "date_naissance, photo, score, genre_vehicule, is_responsable) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setString(1, l.getNomLivreur());
        ps.setString(2, l.getTelephone());
        ps.setInt(3, l.getIdSocieteLivraison());
        ps.setObject(4, l.getDateNaissance());
        ps.setString(5, l.getPhoto());
        ps.setInt(6, l.getScore());
        ps.setString(7, l.getGenreVehicule());
        ps.setBoolean(8, l.isResponsable());
        ps.executeUpdate();
    }

    // ─────────────────────────────────────────────
    // LIRE TOUS
    // ─────────────────────────────────────────────
    public List<Livreur> getAll() throws SQLException {
        List<Livreur> liste = new ArrayList<>();
        ResultSet rs = cnx.createStatement().executeQuery("SELECT * FROM Livreur");
        while (rs.next()) liste.add(mapRow(rs));
        return liste;
    }

    // ─────────────────────────────────────────────
    // LIRE LES DISPONIBLES
    // ─────────────────────────────────────────────
    public List<Livreur> getDisponibles() throws SQLException {
        List<Livreur> liste = new ArrayList<>();
        ResultSet rs = cnx.createStatement()
                .executeQuery("SELECT * FROM Livreur WHERE disponibilite = TRUE");
        while (rs.next()) liste.add(mapRow(rs));
        return liste;
    }

    // ─────────────────────────────────────────────
    // LIRE UN SEUL
    // ─────────────────────────────────────────────
    public Livreur getById(int id) throws SQLException {
        PreparedStatement ps = cnx.prepareStatement(
                "SELECT * FROM Livreur WHERE id_livreur = ?");
        ps.setInt(1, id);
        ResultSet rs = ps.executeQuery();
        if (rs.next()) return mapRow(rs);
        return null;
    }

    // ─────────────────────────────────────────────
    // MODIFIER
    // ─────────────────────────────────────────────
    public void update(Livreur l) throws SQLException {
        String sql = "UPDATE Livreur SET nom_livreur=?, telephone=?, disponibilite=?, " +
                "date_naissance=?, photo=?, score=?, genre_vehicule=?, is_responsable=? WHERE id_livreur=?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setString(1, l.getNomLivreur());
        ps.setString(2, l.getTelephone());
        ps.setBoolean(3, l.isDisponibilite());
        ps.setObject(4, l.getDateNaissance());
        ps.setString(5, l.getPhoto());
        ps.setInt(6, l.getScore());
        ps.setString(7, l.getGenreVehicule());
        ps.setBoolean(8, l.isResponsable());
        ps.setInt(9, l.getIdLivreur());
        ps.executeUpdate();
    }

    // ─────────────────────────────────────────────
    // SUPPRIMER
    // ─────────────────────────────────────────────
    public void delete(int id) throws SQLException {
        PreparedStatement ps = cnx.prepareStatement(
                "DELETE FROM Livreur WHERE id_livreur = ?");
        ps.setInt(1, id);
        ps.executeUpdate();
    }

    // ─────────────────────────────────────────────
    // ASSIGNER → marque indisponible
    // ─────────────────────────────────────────────
    public void assignerLivraison(int idLivreur) throws SQLException {
        Livreur l = getById(idLivreur);
        if (l == null) throw new SQLException("Livreur introuvable !");
        if (!l.isDisponibilite()) throw new SQLException("Ce livreur est déjà indisponible !");
        PreparedStatement ps = cnx.prepareStatement(
                "UPDATE Livreur SET disponibilite = FALSE WHERE id_livreur = ?");
        ps.setInt(1, idLivreur);
        ps.executeUpdate();
    }

    // ─────────────────────────────────────────────
    // LIBÉRER → marque disponible
    // ─────────────────────────────────────────────
    public void libererLivreur(int idLivreur) throws SQLException {
        PreparedStatement ps = cnx.prepareStatement(
                "UPDATE Livreur SET disponibilite = TRUE WHERE id_livreur = ?");
        ps.setInt(1, idLivreur);
        ps.executeUpdate();
    }

    // ─────────────────────────────────────────────
    // DÉTECTION DES DOUBLONS
    // ─────────────────────────────────────────────
    private boolean existeDoublon(String nom, String telephone) throws SQLException {
        PreparedStatement ps = cnx.prepareStatement(
                "SELECT COUNT(*) FROM Livreur WHERE nom_livreur = ? AND telephone = ?");
        ps.setString(1, nom);
        ps.setString(2, telephone);
        ResultSet rs = ps.executeQuery();
        rs.next();
        return rs.getInt(1) > 0;
    }

    // ─────────────────────────────────────────────
    // MAPPER
    // ─────────────────────────────────────────────
    private Livreur mapRow(ResultSet rs) throws SQLException {
        Date sqlDate = rs.getDate("date_naissance");
        LocalDate dateNaissance = (sqlDate != null) ? sqlDate.toLocalDate() : null;
        return new Livreur(
                rs.getInt("id_livreur"),
                rs.getString("nom_livreur"),
                rs.getString("telephone"),
                rs.getInt("id_societe_livraison"),
                rs.getBoolean("disponibilite"),
                dateNaissance,
                rs.getString("photo"),
                rs.getInt("score"),
                rs.getString("genre_vehicule"),
                rs.getBoolean("is_responsable")
        );
    }
}

