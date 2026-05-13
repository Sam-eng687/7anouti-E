package com.hanouti.hanoutiem4.dao;

import com.hanouti.hanoutiem4.model.Produit;
import com.hanouti.hanoutiem4.util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ProduitDAO {

    // No cached connection — always fetch fresh to benefit from DBConnection auto-reconnect
    private Connection getConn() throws SQLException {
        return DBConnection.getInstance().getConnection();
    }

    public ProduitDAO() throws SQLException {
        // Validate connection at construction time
        getConn();
    }

    public List<Produit> searchByKeywords(List<String> keywords) throws SQLException {
        if (keywords == null || keywords.isEmpty()) return new ArrayList<>();

        StringBuilder sql = new StringBuilder(
                "SELECT produit_id, nom, description, categorie, prix, stock " +
                        "FROM produits WHERE stock > 0 AND ("
        );
        for (int i = 0; i < keywords.size(); i++) {
            if (i > 0) sql.append(" OR ");
            sql.append("LOWER(nom) LIKE ? OR LOWER(categorie) LIKE ?");
        }
        sql.append(") LIMIT 4");

        List<Produit> result = new ArrayList<>();
        Set<String> seenNames = new HashSet<>();
        try (PreparedStatement ps = getConn().prepareStatement(sql.toString())) {
            int idx = 1;
            for (String kw : keywords) {
                String pattern = "%" + kw.toLowerCase() + "%";
                ps.setString(idx++, pattern);
                ps.setString(idx++, pattern);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String nomProduit = rs.getString("nom");
                    // Skip duplicate product names
                    if (seenNames.add(nomProduit.toLowerCase())) {
                        result.add(new Produit(
                                rs.getInt("produit_id"),
                                nomProduit,
                                rs.getString("description"),
                                rs.getString("categorie"),
                                rs.getDouble("prix"),
                                rs.getInt("stock")
                        ));
                    }
                }
            }
        }
        return result;
    }
}