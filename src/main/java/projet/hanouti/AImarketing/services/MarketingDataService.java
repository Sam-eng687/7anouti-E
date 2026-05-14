package projet.hanouti.AImarketing.services;

import projet.hanouti.AImarketing.entities.CampagneMarketing;
import projet.hanouti.AImarketing.entities.ConseilsMarketing;
import projet.hanouti.AImarketing.entities.StatistiquesVentes;
import projet.hanouti.common.utils.MyBD;
import projet.hanouti.common.utils.SessionManager;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MarketingDataService {
    public List<StatistiquesVentes> getStatistiques() {
        List<StatistiquesVentes> rows = new ArrayList<>();
        for (String sql : statsQueries()) {
            rows = readStats(sql);
            if (!rows.isEmpty()) return rows;
        }
        return fallbackStats();
    }

    public List<ConseilsMarketing> getConseils() {
        List<ConseilsMarketing> rows = new ArrayList<>();
        for (String sql : conseilQueries()) {
            rows = readConseils(sql);
            if (!rows.isEmpty()) return rows;
        }
        return fallbackConseils();
    }

    public List<CampagneMarketing> getCampagnes() {
        List<CampagneMarketing> rows = new ArrayList<>();
        for (String sql : campagneQueries()) {
            rows = readCampagnes(sql);
            if (!rows.isEmpty()) return rows;
        }
        return fallbackCampagnes();
    }

    public void setConseilState(int id, String state) {
        String normalizedState = normalizeConseilState(state);
        String[] sqls = {
                "UPDATE conseils_ia SET etat=?, date_accepte=CASE WHEN ?='ACCEPTE' THEN NOW() ELSE date_accepte END, date_ignore=CASE WHEN ?='IGNORE' THEN NOW() ELSE date_ignore END WHERE id_conseil=?",
                "UPDATE conseils_ia SET est_applique=? WHERE conseil_id=?",
                "UPDATE conseils_marketing SET applique=? WHERE conseil_id=?"
        };
        for (String sql : sqls) {
            try (PreparedStatement ps = connection().prepareStatement(sql)) {
                if (sql.contains("applique")) {
                    ps.setBoolean(1, "ACCEPTE".equalsIgnoreCase(normalizedState));
                    ps.setInt(2, id);
                } else {
                    ps.setString(1, normalizedState);
                    ps.setString(2, normalizedState);
                    ps.setString(3, normalizedState);
                    ps.setInt(4, id);
                }
                if (ps.executeUpdate() > 0) return;
            } catch (Exception ignored) {
            }
        }
    }

    public boolean addConseil(ConseilsMarketing conseil) {
        String sql = "INSERT INTO conseils_ia (id_produit, type, urgence, description, score, etat, date_genere) VALUES (?, ?, ?, ?, ?, 'EN_ATTENTE', NOW())";
        try (PreparedStatement ps = connection().prepareStatement(sql)) {
            Integer idProduit = parseProduitId(conseil.getProduitId());
            if (idProduit == null) {
                ps.setNull(1, Types.INTEGER);
            } else {
                ps.setInt(1, idProduit);
            }
            ps.setString(2, normalizeConseilType(conseil.getTypeConseil()));
            ps.setString(3, conseil.getImpactEstime() == null || conseil.getImpactEstime().isBlank() ? "NORMAL" : conseil.getImpactEstime());
            ps.setString(4, conseil.getDescription());
            ps.setInt(5, conseil.getScore());
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public void generateConseils() {
        List<StatistiquesVentes> stats = getStatistiques();
        for (StatistiquesVentes stat : stats) {
            if (stat.getTotalVendu() < 20) {
                ConseilsMarketing conseil = new ConseilsMarketing();
                conseil.setProduitId(stat.getProduitId());
                conseil.setProduitNom(stat.getProduitNom());
                conseil.setTypeConseil("PROMOTION");
                conseil.setDescription("Le produit " + stat.getProduitNom() + " a peu de ventes (" + stat.getTotalVendu() + " unités). Lance une promotion -15% cette semaine.");
                conseil.setImpactEstime("Eleve");
                conseil.setScore(85);
                addConseil(conseil);
            }
            if (stat.getTauxRetour() > 5.0) {
                ConseilsMarketing conseil = new ConseilsMarketing();
                conseil.setProduitId(stat.getProduitId());
                conseil.setProduitNom(stat.getProduitNom());
                conseil.setTypeConseil("DESTOCKAGE");
                conseil.setDescription("Taux de retour élevé (" + stat.getTauxRetour() + "%) pour " + stat.getProduitNom() + ". Lance une campagne de destockage.");
                conseil.setImpactEstime("Moyen");
                conseil.setScore(70);
                addConseil(conseil);
            }
        }
    }

    public int currentVendorId() {
        int id = SessionManager.getCurrentUserId();
        return id > 0 ? id : 1;
    }

    private List<String> statsQueries() {
        int vendorId = currentVendorId();
        return List.of(
                "SELECT * FROM statistiques_ventes WHERE id_vendeur=" + vendorId,
                "SELECT * FROM statistiques_ventes WHERE vendor_id=" + vendorId,
                "SELECT * FROM statistiques_ventes"
        );
    }

    private List<String> conseilQueries() {
        return List.of(
                "SELECT ci.*, p.nom AS produit_nom FROM conseils_ia ci LEFT JOIN produit p ON p.id_produit=ci.id_produit",
                "SELECT * FROM conseils_ia",
                "SELECT * FROM conseils_marketing"
        );
    }

    private List<String> campagneQueries() {
        int vendorId = currentVendorId();
        return List.of(
                "SELECT * FROM campagne_marketing WHERE vendor_id=" + vendorId,
                "SELECT * FROM campagne_marketing WHERE id_vendeur=" + vendorId,
                "SELECT * FROM campagne_marketing",
                "SELECT * FROM campagnes_marketing"
        );
    }

    private List<StatistiquesVentes> readStats(String sql) {
        List<StatistiquesVentes> rows = new ArrayList<>();
        try (Statement st = connection().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                StatistiquesVentes s = new StatistiquesVentes();
                s.setStatId(getInt(rs, "id", "stat_id", "id_stat"));
                s.setProduitId(getString(rs, "reference", "produit_id", "produit_ref", "produit"));
                s.setProduitNom(getString(rs, "produit_nom", "nom_produit", "nom", "libelle"));
                s.setPeriode(getString(rs, "periode", "date_periode", "date"));
                s.setTotalVendu(getInt(rs, "total_vendu", "quantite_vendue", "nb_ventes", "ventes"));
                s.setRevenuTotal(getDouble(rs, "revenu_total", "revenu", "montant"));
                s.setTauxRetour(getDouble(rs, "taux_retour", "retour", "taux_retour_moyen"));
                s.setClassement(getString(rs, "classement", "rang", "category"));
                rows.add(s);
            }
        } catch (Exception ignored) {
        }
        return rows;
    }

    private List<ConseilsMarketing> readConseils(String sql) {
        List<ConseilsMarketing> rows = new ArrayList<>();
        try (Statement st = connection().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                ConseilsMarketing c = new ConseilsMarketing();
                c.setConseilId(getInt(rs, "id", "id_conseil", "conseil_id"));
                c.setProduitId(getString(rs, "id_produit", "produit_id", "produit_ref", "reference"));
                c.setProduitNom(getString(rs, "produit_nom", "titre_acheteur", "nom", "libelle"));
                c.setTypeConseil(getString(rs, "type", "type_conseil", "categorie"));
                c.setDescription(getString(rs, "description", "contenu", "message"));
                c.setImpactEstime(getString(rs, "impact_estime", "urgence", "priorite"));
                c.setScore(getInt(rs, "score", "score_ia"));
                c.setDateGeneration(getDate(rs, "date_genere", "date_generation", "created_at"));
                String etat = getString(rs, "etat", "statut");
                c.setApplique(getBoolean(rs, "applique", "est_applique") || "APPLIQUE".equalsIgnoreCase(etat) || "ACCEPTE".equalsIgnoreCase(etat));
                c.setIgnore("IGNORE".equalsIgnoreCase(etat) || "IGNOREE".equalsIgnoreCase(etat));
                rows.add(c);
            }
        } catch (Exception ignored) {
        }
        return rows;
    }

    private List<CampagneMarketing> readCampagnes(String sql) {
        List<CampagneMarketing> rows = new ArrayList<>();
        try (Statement st = connection().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                CampagneMarketing c = new CampagneMarketing();
                c.setCampagneId(getInt(rs, "id", "campagne_id"));
                c.setNomCampagne(getString(rs, "nom", "nom_campagne", "titre"));
                c.setObjectif(getString(rs, "objectif", "type_action"));
                c.setCanal(getString(rs, "canal", "type_action", "type"));
                c.setBudget(getDouble(rs, "budget_alloue", "budget", "montant"));
                c.setDepense(getDouble(rs, "budget_depense", "depense"));
                c.setStatut(getString(rs, "statut", "etat"));
                c.setDateDebut(getDate(rs, "date_debut"));
                c.setDateFin(getDate(rs, "date_fin"));
                c.setIaScore(getDouble(rs, "score_ia", "ia_score"));
                c.setIaConseil(getString(rs, "ia_conseil", "conseil_ia"));
                rows.add(c);
            }
        } catch (Exception ignored) {
        }
        return rows;
    }

    private Connection connection() {
        return MyBD.getInstance().getConnection();
    }

    private Integer parseProduitId(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private String normalizeConseilType(String type) {
        String value = type == null ? "" : type.toLowerCase(Locale.ROOT);
        if (value.contains("bundle")) return "Bundle";
        if (value.contains("stock") || value.contains("destock")) return "Destockage";
        return "Promotion";
    }

    private String normalizeConseilState(String state) {
        if ("APPLIQUE".equalsIgnoreCase(state) || "ACCEPTE".equalsIgnoreCase(state)) return "ACCEPTE";
        if ("IGNORE".equalsIgnoreCase(state) || "IGNOREE".equalsIgnoreCase(state)) return "IGNORE";
        return "EN_ATTENTE";
    }

    private boolean hasColumn(ResultSet rs, String column) throws SQLException {
        ResultSetMetaData meta = rs.getMetaData();
        for (int i = 1; i <= meta.getColumnCount(); i++) {
            if (meta.getColumnLabel(i).equalsIgnoreCase(column)) return true;
        }
        return false;
    }

    private String getString(ResultSet rs, String... cols) {
        for (String col : cols) {
            try {
                if (hasColumn(rs, col)) {
                    String value = rs.getString(col);
                    if (value != null && !value.isBlank()) return value;
                }
            } catch (SQLException ignored) {
            }
        }
        return "";
    }

    private int getInt(ResultSet rs, String... cols) {
        for (String col : cols) {
            try {
                if (hasColumn(rs, col)) return rs.getInt(col);
            } catch (SQLException ignored) {
            }
        }
        return 0;
    }

    private double getDouble(ResultSet rs, String... cols) {
        for (String col : cols) {
            try {
                if (hasColumn(rs, col)) return rs.getDouble(col);
            } catch (SQLException ignored) {
            }
        }
        return 0;
    }

    private boolean getBoolean(ResultSet rs, String... cols) {
        for (String col : cols) {
            try {
                if (hasColumn(rs, col)) return rs.getBoolean(col);
            } catch (SQLException ignored) {
            }
        }
        return false;
    }

    private Date getDate(ResultSet rs, String... cols) {
        for (String col : cols) {
            try {
                if (hasColumn(rs, col)) return rs.getDate(col);
            } catch (SQLException ignored) {
            }
        }
        return null;
    }

    public Map<String, Double> revenueByProduct(List<StatistiquesVentes> stats) {
        Map<String, Double> data = new LinkedHashMap<>();
        stats.stream()
                .sorted((a, b) -> Double.compare(b.getRevenuTotal(), a.getRevenuTotal()))
                .limit(6)
                .forEach(s -> data.put(labelFor(s), s.getRevenuTotal()));
        return data;
    }

    public String labelFor(StatistiquesVentes stat) {
        if (stat == null) return "N/A";
        String label = stat.getProduitId();
        if (label == null || label.isBlank()) label = stat.getProduitNom();
        return label == null || label.isBlank() ? "Produit" : label;
    }

    public String typeClass(String type) {
        String value = type == null ? "" : type.toLowerCase(Locale.ROOT);
        if (value.contains("bundle")) return "type-bundle";
        if (value.contains("stock") || value.contains("destock")) return "type-stock";
        return "type-promo";
    }

    private List<StatistiquesVentes> fallbackStats() {
        List<StatistiquesVentes> rows = new ArrayList<>();
        rows.add(stat("REF-CHARGEUR", "Chargeur Rapide 65W", "Mai 2026", 120, 22350, 2.5, "Top"));
        rows.add(stat("REF-CABLE", "Cable USB-C", "Mai 2026", 70, 4200, 1.9, "Stable"));
        rows.add(stat("REF-SOURIS", "Souris sans fil", "Mai 2026", 30, 1950, 3.1, "Moyen"));
        return rows;
    }

    private StatistiquesVentes stat(String ref, String nom, String periode, int ventes, double revenu, double retour, String classement) {
        StatistiquesVentes s = new StatistiquesVentes();
        s.setProduitId(ref);
        s.setProduitNom(nom);
        s.setPeriode(periode);
        s.setTotalVendu(ventes);
        s.setRevenuTotal(revenu);
        s.setTauxRetour(retour);
        s.setClassement(classement);
        return s;
    }

    private List<ConseilsMarketing> fallbackConseils() {
        List<ConseilsMarketing> rows = new ArrayList<>();
        rows.add(conseil(1, "Masques Chirurgicaux x50", "Promotion", "Les Masques Chirurgicaux x50 sont peu cliques. Lance une campagne BANNIERE.", "Moyen", 75));
        rows.add(conseil(2, "Chargeur Rapide 65W", "Bundle", "Acheteur id=1 a consulte 23 produits. Offre groupee Chargeur Rapide 65W + Cable USB-C -15%.", "Moyen", 68));
        rows.add(conseil(3, "Paracetamol 500mg", "Promotion", "Le produit #21 a ete vu 3 fois. Lance une promo -10% cette semaine.", "Eleve", 92));
        return rows;
    }

    private ConseilsMarketing conseil(int id, String produit, String type, String description, String impact, int score) {
        ConseilsMarketing c = new ConseilsMarketing();
        c.setConseilId(id);
        c.setProduitNom(produit);
        c.setProduitId(String.valueOf(id));
        c.setTypeConseil(type);
        c.setDescription(description);
        c.setImpactEstime(impact);
        c.setScore(score);
        c.setDateGeneration(Date.valueOf(LocalDate.now()));
        return c;
    }

    private List<CampagneMarketing> fallbackCampagnes() {
        List<CampagneMarketing> rows = new ArrayList<>();
        CampagneMarketing c = new CampagneMarketing();
        c.setCampagneId(1);
        c.setNomCampagne("RAMADAN");
        c.setObjectif("FIDELISATION");
        c.setCanal("EMAIL");
        c.setBudget(77);
        c.setDepense(0);
        c.setStatut("BROUILLON");
        c.setDateDebut(Date.valueOf("2026-05-01"));
        c.setDateFin(Date.valueOf("2026-06-01"));
        c.setIaScore(8.0);
        c.setIaConseil("Brouillon - campagne non encore lancee");
        rows.add(c);
        return rows;
    }
}
