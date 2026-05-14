package projet.hanouti.wejden.gui.components;

import javafx.geometry.Insets;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Sidebar navigation minimaliste — icones seules, 68px de large.
 * Tooltip au survol pour chaque module.
 * Icone active reste coloree et highlighted.
 */
public class SidebarNav extends VBox {

    private final List<SpotlightNavItem> items = new ArrayList<>();
    private Consumer<String> onNavigate;

    public SidebarNav() {
        this.setSpacing(8);
        this.setPadding(new Insets(12, 0, 12, 0));
        JfxAlign.vbox(this, "CENTER");
        this.setStyle("-fx-background-color:transparent;");

        // Modules : icone, titre (tooltip), sous-titre, moduleId
        String[][] defs = {
            {"\uD83D\uDCCA", "Dashboard",  "Ventes",    "statistiques"},
            {"\uD83D\uDCA1", "Conseils",   "Decision",  "conseils"},
            {"\uD83D\uDCE2", "Campagnes",  "Marketing", "campagnes"},
        };

        for (String[] d : defs) {
            SpotlightNavItem item = new SpotlightNavItem(d[0], d[1], d[2], d[3]);
            item.setOnActivate(() -> {
                activate(item.getModuleId());
                if (onNavigate != null) onNavigate.accept(item.getModuleId());
            });
            items.add(item);
            this.getChildren().add(item);
        }
    }

    /** Active l'item correspondant au moduleId et desactive les autres */
    public void activate(String moduleId) {
        for (SpotlightNavItem item : items) {
            item.setActive(item.getModuleId().equals(moduleId));
        }
    }

    /** Met à jour le thème de tous les items */
    public void updateTheme(boolean dark) {
        for (SpotlightNavItem item : items) {
            item.updateTheme(dark);
        }
    }

    /** Callback appele quand l'utilisateur clique sur un item */
    public void setOnNavigate(Consumer<String> handler) {
        this.onNavigate = handler;
    }
}
