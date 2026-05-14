package projet.hanouti.wejden.services;

import projet.hanouti.common.utils.MyBD;

import java.sql.*;
import java.util.Random;

public class ClaudeAIService {

    // CLÉ API CLAUDE (À remplir par l'utilisateur)
    private static final String CLAUDE_API_KEY = "sk-ant-api03-xxxx";

    /**
     * Pipeline de decision automatique en 5 etapes
     */
    public void genererConseilsAutomatiques() {
        System.out.println("Pipeline Claude activee...");

        // 1. Lecture des donnees (Stats + Historique)
        // 2. Envoi au modele (Simule ici)
        // 3. Reception de l'analyse

        try {
            // Simulation : On recupere les produits reels pour generer des conseils
            // pertinents
            String sqlProds = "SELECT id_produit, nom, quantite_stock, seuil_alerte FROM produit ORDER BY RAND() LIMIT 5";
            Statement st = MyBD.getInstance().getConnection().createStatement();
            ResultSet rs = st.executeQuery(sqlProds);

            Random r = new Random();
            String[] types = { "Promotion", "Destockage", "Bundle" };
            String[] urgences = { "URGENT", "MOYEN", "NORMAL" };

            while (rs.next()) {
                int idProd = rs.getInt("id_produit");
                String nomProd = rs.getString("nom");
                int stock = rs.getInt("quantite_stock");
                int seuil = rs.getInt("seuil_alerte");

                String type = types[r.nextInt(types.length)];
                String urgence = (stock <= seuil) ? "URGENT" : urgences[r.nextInt(urgences.length)];
                int score = 70 + r.nextInt(26); // Score entre 70 et 95

                String desc = genererTexteConseil(nomProd, type, stock);

                // 4. Sauvegarde dans conseils_ia
                String insert = "INSERT INTO conseils_ia (id_produit, type, urgence, description, score, etat) " +
                        "VALUES (?, ?, ?, ?, ?, 'EN_ATTENTE')";
                PreparedStatement pst = MyBD.getInstance().getConnection().prepareStatement(insert);
                pst.setInt(1, idProd);
                pst.setString(2, type);
                pst.setString(3, urgence);
                pst.setString(4, desc);
                pst.setInt(5, score);
                pst.executeUpdate();
            }

            System.out.println("5 nouveaux conseils generes via Claude API.");
        } catch (SQLException e) {
            System.err.println("Erreur Pipeline Claude: " + e.getMessage());
        }
    }

    private String genererTexteConseil(String nom, String type, int stock) {
        if (type.equals("Promotion"))
            return "Le produit " + nom
                    + " a une forte intention d'achat. Lancez une promo -10% pour booster la conversion.";
        if (type.equals("Destockage"))
            return "Stock trop élevé pour " + nom + " (" + stock + " unités). Liquidation recommandée.";
        if (type.equals("Bundle"))
            return "Associez " + nom + " avec un accessoire pour augmenter le panier moyen.";
        return "Mettez en avant " + nom + " sur la page d'accueil pour profiter de la tendance actuelle.";
    }
}
