package projet.hanouti.wejden.services;

import projet.hanouti.common.utils.MyBD;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Service automatique de génération de conseils IA basé sur les données d'interaction_utilisateur.
 * Analyse les comportements utilisateurs et génère des conseils marketing intelligents.
 */
public class AutoConseilGeneratorService {

    private static AutoConseilGeneratorService instance;
    private ScheduledExecutorService scheduler;
    private boolean isRunning = false;
    
    private static final String ETAT_EN_ATTENTE = "EN_ATTENTE";

    // Configuration
    private int intervalMinutes = 30; // Génération toutes les 30 minutes par défaut
    private int minInteractionsThreshold = 5; // Minimum d'interactions pour analyser un produit
    
    private AutoConseilGeneratorService() {
        // Singleton
    }
    
    public static synchronized AutoConseilGeneratorService getInstance() {
        if (instance == null) {
            instance = new AutoConseilGeneratorService();
        }
        return instance;
    }
    
    /**
     * Démarre le générateur automatique de conseils
     */
    public void start() {
        if (isRunning) {
            System.out.println("[AutoConseilGenerator] Déjà en cours d'exécution");
            return;
        }
        
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "AutoConseilGenerator");
            t.setDaemon(true);
            return t;
        });
        
        // Génération initiale immédiate
        scheduler.execute(this::generateConseils);
        
        // Puis génération périodique
        scheduler.scheduleAtFixedRate(
            this::generateConseils,
            intervalMinutes,
            intervalMinutes,
            TimeUnit.MINUTES
        );
        
        isRunning = true;
        System.out.println("[AutoConseilGenerator] Démarré - génération toutes les " + intervalMinutes + " minutes");
    }
    
    /**
     * Arrête le générateur automatique
     */
    public void stop() {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
            isRunning = false;
            System.out.println("[AutoConseilGenerator] Arrêté");
        }
    }
    
    /**
     * Configure l'intervalle de génération
     */
    public void setIntervalMinutes(int minutes) {
        this.intervalMinutes = minutes;
        if (isRunning) {
            stop();
            start();
        }
    }
    
    /**
     * Génère les conseils basés sur l'analyse des interactions utilisateurs
     */
    public void generateConseils() {
        try {
            System.out.println("[AutoConseilGenerator] Début de l'analyse à " + LocalDateTime.now());
            
            Connection conn = MyBD.getInstance().getConnection();
            
            // 1. Analyser les produits avec beaucoup de vues mais peu d'achats
            analyzeHighViewsLowPurchases(conn);
            
            // 2. Analyser les produits avec beaucoup d'ajouts au panier mais peu d'achats
            analyzeAbandonedCarts(conn);
            
            // 3. Analyser les produits populaires pour des bundles
            analyzePopularProducts(conn);
            
            // 4. Analyser les produits avec stock faible et interactions élevées
            analyzeLowStockHighDemand(conn);
            
            // 5. Analyser les produits avec peu d'interactions
            analyzeLowEngagement(conn);
            
            System.out.println("[AutoConseilGenerator] Analyse terminée avec succès");
            
        } catch (Exception e) {
            System.err.println("[AutoConseilGenerator] Erreur lors de la génération: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Analyse 1: Produits avec beaucoup de vues mais peu d'achats → PROMOTION
     */
    private void analyzeHighViewsLowPurchases(Connection conn) throws SQLException {
        String sql = 
            "SELECT iu.id_produit, p.nom, " +
            "SUM(CASE WHEN iu.type_interaction = 'VIEW' THEN iu.nb_interaction ELSE 0 END) as total_views, " +
            "SUM(CASE WHEN iu.type_interaction = 'BOUGHT' THEN iu.nb_interaction ELSE 0 END) as total_bought, " +
            "p.quantite_stock " +
            "FROM Interaction_Utilisateur iu " +
            "JOIN produit p ON p.id_produit = iu.id_produit " +
            "WHERE iu.last_interaction >= DATE_SUB(NOW(), INTERVAL 7 DAY) " +
            "GROUP BY iu.id_produit, p.nom, p.quantite_stock " +
            "HAVING total_views >= ? AND total_bought < (total_views * 0.1)";
        
        try (PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setInt(1, minInteractionsThreshold * 2);
            ResultSet rs = pst.executeQuery();
            
            while (rs.next()) {
                int idProduit = rs.getInt("id_produit");
                String nomProduit = rs.getString("nom");
                int totalViews = rs.getInt("total_views");
                int totalBought = rs.getInt("total_bought");
                
                // Vérifier si un conseil similaire n'existe pas déjà
                if (!conseilExists(conn, idProduit, "Promotion")) {
                    double conversionRate = totalBought * 100.0 / totalViews;
                    int score = calculateScore(totalViews, totalBought, 85);
                    
                    String description = String.format(
                        "Le produit '%s' a %d vues mais seulement %d achats (taux de conversion: %.1f%%). " +
                        "Une promotion de 10-15%% pourrait booster les ventes.",
                        nomProduit, totalViews, totalBought, conversionRate
                    );
                    
                    insertConseil(conn, idProduit, "Promotion", "MOYEN", description, score);
                    System.out.println("[AutoConseilGenerator] Conseil PROMOTION créé pour: " + nomProduit);
                }
            }
        }
    }
    
    /**
     * Analyse 2: Produits avec beaucoup d'ajouts au panier mais peu d'achats → PROMOTION URGENTE
     */
    private void analyzeAbandonedCarts(Connection conn) throws SQLException {
        String sql = 
            "SELECT iu.id_produit, p.nom, " +
            "SUM(CASE WHEN iu.type_interaction = 'ADD_TO_CART' THEN iu.nb_interaction ELSE 0 END) as total_cart, " +
            "SUM(CASE WHEN iu.type_interaction = 'BOUGHT' THEN iu.nb_interaction ELSE 0 END) as total_bought " +
            "FROM Interaction_Utilisateur iu " +
            "JOIN produit p ON p.id_produit = iu.id_produit " +
            "WHERE iu.last_interaction >= DATE_SUB(NOW(), INTERVAL 7 DAY) " +
            "GROUP BY iu.id_produit, p.nom " +
            "HAVING total_cart >= ? AND total_bought < (total_cart * 0.3)";
        
        try (PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setInt(1, minInteractionsThreshold);
            ResultSet rs = pst.executeQuery();
            
            while (rs.next()) {
                int idProduit = rs.getInt("id_produit");
                String nomProduit = rs.getString("nom");
                int totalCart = rs.getInt("total_cart");
                int totalBought = rs.getInt("total_bought");
                
                if (!conseilExists(conn, idProduit, "Promotion")) {
                    double abandonRate = (totalCart - totalBought) * 100.0 / totalCart;
                    int score = calculateScore(totalCart, totalBought, 90);
                    
                    String description = String.format(
                        "Le produit '%s' a %d ajouts au panier mais seulement %d achats (taux d'abandon: %.1f%%). " +
                        "Une promotion flash ou livraison gratuite pourrait convertir ces paniers abandonnés.",
                        nomProduit, totalCart, totalBought, abandonRate
                    );
                    
                    insertConseil(conn, idProduit, "Promotion", "URGENT", description, score);
                    System.out.println("[AutoConseilGenerator] Conseil PROMOTION URGENTE créé pour: " + nomProduit);
                }
            }
        }
    }
    
    /**
     * Analyse 3: Produits populaires pour des bundles
     */
    private void analyzePopularProducts(Connection conn) throws SQLException {
        String sql = 
            "SELECT iu.id_produit, p.nom, " +
            "SUM(iu.nb_interaction) as total_interactions " +
            "FROM Interaction_Utilisateur iu " +
            "JOIN produit p ON p.id_produit = iu.id_produit " +
            "WHERE iu.last_interaction >= DATE_SUB(NOW(), INTERVAL 7 DAY) " +
            "AND iu.type_interaction IN ('VIEW', 'CLICK_PRODUCT', 'ADD_TO_CART', 'BOUGHT') " +
            "GROUP BY iu.id_produit, p.nom " +
            "HAVING total_interactions >= ? " +
            "ORDER BY total_interactions DESC " +
            "LIMIT 5";
        
        try (PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setInt(1, minInteractionsThreshold * 3);
            ResultSet rs = pst.executeQuery();
            
            while (rs.next()) {
                int idProduit = rs.getInt("id_produit");
                String nomProduit = rs.getString("nom");
                int totalInteractions = rs.getInt("total_interactions");
                
                if (!conseilExists(conn, idProduit, "Bundle")) {
                    int score = Math.min(95, 70 + (totalInteractions / 10));
                    
                    String description = String.format(
                        "Le produit '%s' est très populaire avec %d interactions. " +
                        "Créez un bundle avec des produits complémentaires pour augmenter le panier moyen de 20-30%%.",
                        nomProduit, totalInteractions
                    );
                    
                    insertConseil(conn, idProduit, "Bundle", "MOYEN", description, score);
                    System.out.println("[AutoConseilGenerator] Conseil BUNDLE créé pour: " + nomProduit);
                }
            }
        }
    }
    
    /**
     * Analyse 4: Produits avec stock faible et demande élevée → DESTOCKAGE ou REAPPRO
     */
    private void analyzeLowStockHighDemand(Connection conn) throws SQLException {
        String sql = 
            "SELECT iu.id_produit, p.nom, p.quantite_stock, p.seuil_alerte, " +
            "SUM(iu.nb_interaction) as total_interactions " +
            "FROM Interaction_Utilisateur iu " +
            "JOIN produit p ON p.id_produit = iu.id_produit " +
            "WHERE iu.last_interaction >= DATE_SUB(NOW(), INTERVAL 7 DAY) " +
            "AND p.quantite_stock <= p.seuil_alerte " +
            "GROUP BY iu.id_produit, p.nom, p.quantite_stock, p.seuil_alerte " +
            "HAVING total_interactions >= ?";
        
        try (PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setInt(1, minInteractionsThreshold);
            ResultSet rs = pst.executeQuery();
            
            while (rs.next()) {
                int idProduit = rs.getInt("id_produit");
                String nomProduit = rs.getString("nom");
                int stock = rs.getInt("quantite_stock");
                int totalInteractions = rs.getInt("total_interactions");
                
                if (!conseilExists(conn, idProduit, "Destockage")) {
                    int score = calculateScore(totalInteractions, stock, 88);
                    
                    String description = String.format(
                        "Le produit '%s' a un stock faible (%d unités) mais une forte demande (%d interactions). " +
                        "Lancez une opération de destockage rapide ou réapprovisionnez d'urgence.",
                        nomProduit, stock, totalInteractions
                    );
                    
                    insertConseil(conn, idProduit, "Destockage", "URGENT", description, score);
                    System.out.println("[AutoConseilGenerator] Conseil DESTOCKAGE créé pour: " + nomProduit);
                }
            }
        }
    }
    
    /**
     * Analyse 5: Produits avec peu d'interactions → MISE EN AVANT
     */
    private void analyzeLowEngagement(Connection conn) throws SQLException {
        String sql = 
            "SELECT p.id_produit, p.nom, p.quantite_stock, " +
            "COALESCE(SUM(iu.nb_interaction), 0) as total_interactions " +
            "FROM produit p " +
            "LEFT JOIN Interaction_Utilisateur iu ON p.id_produit = iu.id_produit " +
            "AND iu.last_interaction >= DATE_SUB(NOW(), INTERVAL 14 DAY) " +
            "WHERE p.quantite_stock > 10 AND p.statut = 'ACTIF' " +
            "GROUP BY p.id_produit, p.nom, p.quantite_stock " +
            "HAVING total_interactions < ? " +
            "ORDER BY p.quantite_stock DESC " +
            "LIMIT 3";
        
        try (PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setInt(1, minInteractionsThreshold);
            ResultSet rs = pst.executeQuery();
            
            while (rs.next()) {
                int idProduit = rs.getInt("id_produit");
                String nomProduit = rs.getString("nom");
                int stock = rs.getInt("quantite_stock");
                int totalInteractions = rs.getInt("total_interactions");
                
                if (!conseilExists(conn, idProduit, "Promotion")) {
                    int score = 70 + (stock / 10);
                    
                    String description = String.format(
                        "Le produit '%s' a peu d'interactions (%d) malgré un stock de %d unités. " +
                        "Mettez-le en avant sur la page d'accueil ou lancez une campagne email ciblée.",
                        nomProduit, totalInteractions, stock
                    );
                    
                    insertConseil(conn, idProduit, "Promotion", "NORMAL", description, score);
                    System.out.println("[AutoConseilGenerator] Conseil MISE EN AVANT créé pour: " + nomProduit);
                }
            }
        }
    }
    
    /**
     * Vérifie si un conseil similaire existe déjà (même produit, même type, créé récemment)
     */
    private boolean conseilExists(Connection conn, int idProduit, String type) throws SQLException {
        String sql = 
            "SELECT COUNT(*) FROM conseils_ia " +
            "WHERE id_produit = ? AND type = ? " +
            "AND (etat = '" + ETAT_EN_ATTENTE + "' OR date_genere >= DATE_SUB(NOW(), INTERVAL 3 DAY))";
        
        try (PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setInt(1, idProduit);
            pst.setString(2, type);
            ResultSet rs = pst.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        }
        return false;
    }
    
    /**
     * Insère un nouveau conseil dans la base de données
     */
    private void insertConseil(Connection conn, int idProduit, String type, String urgence, 
                               String description, int score) throws SQLException {
        String sql = 
            "INSERT INTO conseils_ia (id_produit, type, urgence, description, score, etat, date_genere) " +
            "VALUES (?, ?, ?, ?, ?, '" + ETAT_EN_ATTENTE + "', NOW())";
        
        try (PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setInt(1, idProduit);
            pst.setString(2, type);
            pst.setString(3, urgence);
            pst.setString(4, description);
            pst.setInt(5, score);
            pst.executeUpdate();
        }
    }
    
    /**
     * Calcule un score intelligent basé sur les métriques
     */
    private int calculateScore(int metric1, int metric2, int baseScore) {
        int ratio = metric2 > 0 ? (metric1 / metric2) : metric1;
        int bonus = Math.min(20, ratio / 2);
        return Math.min(95, baseScore + bonus);
    }
    
    /**
     * Génération manuelle (pour tests ou bouton UI)
     */
    public void generateNow() {
        generateConseils();
    }
    
    public boolean isRunning() {
        return isRunning;
    }
    
    public int getIntervalMinutes() {
        return intervalMinutes;
    }
}
