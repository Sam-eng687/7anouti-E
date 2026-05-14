package projet.hanouti.produit_fournisseur.utils;

import projet.hanouti.common.utils.MailSender;
import projet.hanouti.produit_fournisseur.entities.Fournisseur;
import projet.hanouti.produit_fournisseur.entities.Produit;
import projet.hanouti.produit_fournisseur.entities.Vendeur;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;

public final class EmailUtil {
    private EmailUtil() {
    }

    public static void sendRestockAlert(Fournisseur fournisseur,
                                        Map<Produit, Integer> produits,
                                        Vendeur vendeur) {
        if (fournisseur == null || fournisseur.getEmail() == null || fournisseur.getEmail().isBlank()) {
            return;
        }
        String vendeurName = vendeur != null
                ? (value(vendeur.getPrenom()) + " " + value(vendeur.getNom())).trim()
                : "";
        if (vendeurName.isBlank()) vendeurName = "Un vendeur";
        String vendeurEmail = vendeur != null ? value(vendeur.getEmail()).trim() : "";
        String vendeurContact = vendeurEmail.isBlank()
                ? escape(vendeurName)
                : escape(vendeurName) + " &lt;" + escape(vendeurEmail) + "&gt;";

        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.FRENCH));
        StringBuilder rows = new StringBuilder();
        for (Map.Entry<Produit, Integer> entry : produits.entrySet()) {
            Produit produit = entry.getKey();
            rows.append("<tr style=\"background:#2b1717;\">")
                    .append("<td style=\"padding:16px 24px;color:#ffffff;font-weight:800;border-top:1px solid #33406f;\">")
                    .append(escape(produit.getNom())).append("</td>")
                    .append("<td style=\"padding:16px 24px;color:#cfd4df;text-transform:uppercase;border-top:1px solid #33406f;\">")
                    .append(escape(produit.getCategorie())).append("</td>")
                    .append("<td style=\"padding:16px 24px;color:#ff4d4d;font-size:22px;font-weight:900;border-top:1px solid #33406f;\">")
                    .append(entry.getValue()).append("<span style=\"font-size:14px;color:#9ca3af;font-weight:500;\"> unite(s)</span></td>")
                    .append("<td style=\"padding:16px 24px;border-top:1px solid #33406f;text-align:center;\">")
                    .append("<span style=\"display:inline-block;background:#c60000;color:#ffffff;border-radius:999px;padding:6px 16px;font-size:13px;font-weight:900;\">CRITIQUE</span>")
                    .append("</td>")
                    .append("</tr>");
        }

        String html = """
                <!doctype html>
                <html>
                <body style="margin:0;padding:0;background:#0b0f24;font-family:Segoe UI,Arial,sans-serif;color:#e6e8f5;">
                <div style="width:100%%;background:#0b0f24;padding:0;">
                  <div style="max-width:930px;margin:0 auto;background:#17182b;border:1px solid #27305f;">
                    <div style="background:linear-gradient(135deg,#0b145e,#2630d3);padding:0 60px 52px;text-align:center;color:#ffffff;">
                      <div style="width:84px;height:84px;line-height:84px;border-radius:50%%;background:rgba(255,255,255,0.14);margin:0 auto 20px;font-size:34px;font-weight:900;">7E</div>
                      <div style="font-size:38px;font-weight:900;letter-spacing:1px;">7anouti-E</div>
                      <div style="margin-top:8px;font-size:20px;letter-spacing:8px;color:#bfc7ff;text-transform:uppercase;">Demande de r&eacute;approvisionnement</div>
                    </div>

                    <div style="background:#360909;border-left:5px solid #ff5b5b;padding:22px 66px;color:#ff5b5b;font-size:21px;font-weight:900;">
                      &#9888; %d produit(s) &agrave; r&eacute;approvisionner &mdash; %s
                    </div>

                    <div style="padding:42px 60px 48px;">
                      <p style="margin:0 0 18px;font-size:24px;line-height:1.45;color:#ffffff;">Bonjour <strong style="color:#5b7cff;">%s</strong>,</p>
                      <p style="margin:0 0 42px;font-size:20px;line-height:1.6;color:#d7d9e7;">
                        Nous vous contactons de la part du vendeur <strong style="color:#ffffff;">%s</strong> sur la plateforme
                        <strong style="color:#ffffff;">7anouti-E</strong> pour vous transmettre une commande de r&eacute;approvisionnement.
                        Veuillez trouver ci-dessous le d&eacute;tail des quantit&eacute;s demand&eacute;es pour chaque produit.
                      </p>

                      <table role="presentation" cellpadding="0" cellspacing="0" style="width:100%%;border-collapse:separate;border-spacing:0;border:1px solid #30417f;border-radius:12px;overflow:hidden;">
                        <thead>
                          <tr style="background:#0f1560;">
                            <th align="left" style="padding:16px 24px;color:#bfc7ff;font-size:16px;letter-spacing:3px;text-transform:uppercase;">Produit</th>
                            <th align="left" style="padding:16px 24px;color:#bfc7ff;font-size:16px;letter-spacing:3px;text-transform:uppercase;">Cat&eacute;gorie</th>
                            <th align="left" style="padding:16px 24px;color:#bfc7ff;font-size:16px;letter-spacing:3px;text-transform:uppercase;">Qt&eacute; demand&eacute;e</th>
                            <th align="center" style="padding:16px 24px;color:#bfc7ff;font-size:16px;letter-spacing:3px;text-transform:uppercase;">Statut</th>
                          </tr>
                        </thead>
                        <tbody>%s</tbody>
                      </table>

                      <div style="margin-top:42px;background:#22264a;border-left:4px solid #3158ff;border-radius:0 10px 10px 0;padding:22px 30px;color:#ffffff;font-size:20px;line-height:1.55;">
                        <strong style="color:#5b7cff;">%s</strong>, nous comptons sur votre r&eacute;activit&eacute; habituelle. Pour toute question concernant cette commande, n'h&eacute;sitez pas &agrave; nous contacter directement.
                        <br><span style="font-size:16px;color:#cfd4df;">Contact vendeur : <strong style="color:#ffffff;">%s</strong></span>
                      </div>
                    </div>

                    <div style="border-top:1px solid #20294c;background:#101225;padding:34px 60px;text-align:center;color:#d7d9e7;">
                      <div style="font-size:18px;line-height:1.55;">Avec toute notre gratitude,<br><strong style="color:#4b7bff;">L'&eacute;quipe 7anouti-E &#128153;</strong></div>
                      <div style="margin-top:12px;font-size:14px;color:#8f96b8;">Des questions ? Ecrivez-nous &agrave; <a href="mailto:support@7anouti-e.tn" style="color:#4b7bff;text-decoration:none;">support@7anouti-e.tn</a></div>
                      <div style="margin-top:24px;font-size:12px;color:#4d536e;">&copy; 2025 7anouti-E - Tunisie<br>Ce message est g&eacute;n&eacute;r&eacute; automatiquement par le syst&egrave;me de gestion des stocks.</div>
                    </div>
                  </div>
                </div>
                </body>
                </html>
                """.formatted(
                produits.size(),
                escape(date),
                escape(fournisseur.getContactNom()),
                vendeurContact,
                rows,
                escape(fournisseur.getContactNom()),
                vendeurContact
        );

        try {
            MailSender.sendHtmlMail(
                    fournisseur.getEmail(),
                    "Demande de reapprovisionnement - " + produits.size() + " produit(s)",
                    html,
                    vendeurName,
                    vendeurEmail
            );
        } catch (Exception e) {
            System.out.println("Erreur email reapprovisionnement: " + e.getMessage());
        }
    }

    private static String escape(String value) {
        if (value == null) return "";
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    private static String value(String value) {
        return value == null ? "" : value;
    }
}
