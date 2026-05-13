package edu.hanouti.utils;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Lien navigateur « Créer un événement » (Google Calendar) sans OAuth : l’utilisateur complète / enregistre dans son compte.
 */
public final class GoogleCalendarPromoLink {

    private GoogleCalendarPromoLink() {
    }

    /**
     * Événement journée entière du {@code debut} au {@code fin} inclus (règle Google : fin exclusive = fin + 1 jour).
     */
    public static String buildPromoTemplateUrl(String produit, String type, LocalDate debut, LocalDate fin, int pct) {
        String safeProd = produit == null ? "Produit" : produit;
        String title = "Promo 7anouti : " + safeProd + " (-" + pct + "%)";
        if (type != null && !type.isBlank())
            title += " (" + type + ")";
        String details = "Promotion créée depuis 7anouti Marketing.\n"
                + "Produit : " + safeProd + "\n"
                + "Remise : " + pct + "%\n"
                + "Période : du " + debut + " au " + fin + " (inclus).";
        String dates = debut.format(DateTimeFormatter.BASIC_ISO_DATE) + "/"
                + fin.plusDays(1).format(DateTimeFormatter.BASIC_ISO_DATE);

        return "https://calendar.google.com/calendar/render?action=TEMPLATE"
                + "&text=" + enc(title)
                + "&dates=" + dates
                + "&details=" + enc(details);
    }

    private static String enc(String s) {
        return URLEncoder.encode(s == null ? "" : s, StandardCharsets.UTF_8);
    }
}
