package tn.hanouti.livreur.dao;

import tn.hanouti.livreur.model.Score;
import tn.hanouti.util.DBConnection;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ScoreDAO {

    private Connection cnx = DBConnection.getInstance().getCnx();

    // ─────────────────────────────────────────────
    // AJOUTER UNE ÉVALUATION
    // ─────────────────────────────────────────────
    public void add(Score s) throws SQLException {
        String sql = "INSERT INTO Score (id_livreur, note, commentaire, livre_dans_delai, date_evaluation) VALUES (?, ?, ?, ?, ?)";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, s.getIdLivreur());
        ps.setInt(2, s.getNote());
        ps.setString(3, s.getCommentaire());
        ps.setBoolean(4, s.isLivreDansDelai());
        ps.setObject(5, s.getDateEvaluation());
        ps.executeUpdate();

        // Recalcule et met à jour le score global du livreur
        mettreAJourScoreGlobal(s.getIdLivreur());
    }

    // ─────────────────────────────────────────────
    // RÉCUPÉRER L'HISTORIQUE D'UN LIVREUR
    // ─────────────────────────────────────────────
    public List<Score> getByLivreur(int idLivreur) throws SQLException {
        List<Score> liste = new ArrayList<>();
        String sql = "SELECT * FROM Score WHERE id_livreur = ? ORDER BY date_evaluation DESC";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, idLivreur);
        ResultSet rs = ps.executeQuery();
        while (rs.next()) {
            liste.add(mapRow(rs));
        }
        return liste;
    }

    // ─────────────────────────────────────────────
    // CALCULER LE SCORE MOYEN D'UN LIVREUR (sur 100)
    // ─────────────────────────────────────────────
    public int calculerScoreMoyen(int idLivreur) throws SQLException {
        String sql = "SELECT AVG(note) as moyenne FROM Score WHERE id_livreur = ?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, idLivreur);
        ResultSet rs = ps.executeQuery();
        if (rs.next()) {
            double moyenne = rs.getDouble("moyenne"); // note sur 5
            return (int) Math.round((moyenne / 5.0) * 100); // convertit en score sur 100
        }
        return 0;
    }

    // ─────────────────────────────────────────────
    // METTRE À JOUR LE SCORE GLOBAL DANS LA TABLE LIVREUR
    // appelée automatiquement après chaque add()
    // ─────────────────────────────────────────────
    private void mettreAJourScoreGlobal(int idLivreur) throws SQLException {
        int scoreCalcule = calculerScoreMoyen(idLivreur);
        String sql = "UPDATE Livreur SET score = ? WHERE id_livreur = ?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, scoreCalcule);
        ps.setInt(2, idLivreur);
        ps.executeUpdate();
    }

    // ─────────────────────────────────────────────
    // MAPPER UNE LIGNE SQL → objet Score
    // ─────────────────────────────────────────────
    private Score mapRow(ResultSet rs) throws SQLException {
        Timestamp ts = rs.getTimestamp("date_evaluation");
        LocalDateTime dateEval = (ts != null) ? ts.toLocalDateTime() : null;

        return new Score(
                rs.getInt("id_score"),
                rs.getInt("id_livreur"),
                rs.getInt("note"),
                rs.getString("commentaire"),
                rs.getBoolean("livre_dans_delai"),
                dateEval
        );
    }
}
