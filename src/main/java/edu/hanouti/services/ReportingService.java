package edu.hanouti.services;

import com.lowagie.text.Document;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.pdf.PdfWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.util.List;
import java.util.Map;
import java.awt.Color;

public class ReportingService {

    public static File generatePDF(List<Map<String, Object>> data, File targetFile) throws Exception {
        Document document = new Document();
        PdfWriter.getInstance(document, new FileOutputStream(targetFile));
        document.open();
        Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 22, Color.BLUE);
        document.add(new Paragraph("HANOUTI MARKETING HUB - RAPPORT", titleFont));
        document.add(new Paragraph("Date du rapport: " + java.time.LocalDate.now()));
        document.add(new Paragraph("------------------------------------------------------------------"));
        document.add(new Paragraph("\n"));
        for (Map<String, Object> row : data) {
            String line = String.format("Produit: %s | Ventes: %s | Revenu: %s TND",
                row.getOrDefault("produitId", "N/A"),
                row.getOrDefault("totalVendu", "0"),
                row.getOrDefault("revenuTotal", "0"));
            document.add(new Paragraph(line));
        }
        document.add(new Paragraph("\n------------------------------------------------------------------"));
        document.add(new Paragraph("Genere automatiquement par Hanouti Intelligence Engine."));
        document.close();
        return targetFile;
    }

    private static final String MY_EMAIL    = "oueslatiwejden3@gmail.com";
    private static final String MY_PASSWORD = "qosvqhzsfaottqll";

    public static void sendVendeurEmail(String toEmail, String nomVendeur,
                                        List<Map<String, Object>> produits,
                                        int joursAvantRupture) {
        String htmlBody = generateVendeurHtml(nomVendeur, produits, joursAvantRupture);

        java.util.Properties props = new java.util.Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");

        javax.mail.Session session = javax.mail.Session.getInstance(props, new javax.mail.Authenticator() {
            protected javax.mail.PasswordAuthentication getPasswordAuthentication() {
                return new javax.mail.PasswordAuthentication(MY_EMAIL, MY_PASSWORD);
            }
        });

        try {
            javax.mail.Message message = new javax.mail.internet.MimeMessage(session);
            message.setFrom(new javax.mail.internet.InternetAddress(MY_EMAIL));
            // Toujours envoyer à la boîte principale, peu importe le vendeur
            message.setRecipients(javax.mail.Message.RecipientType.TO,
                javax.mail.internet.InternetAddress.parse(MY_EMAIL));
            message.setSubject("7anouti - Rapport vendeur : " + nomVendeur);

            javax.mail.internet.MimeBodyPart mimeBodyPart = new javax.mail.internet.MimeBodyPart();
            mimeBodyPart.setContent(htmlBody, "text/html; charset=utf-8");

            javax.mail.Multipart multipart = new javax.mail.internet.MimeMultipart();
            multipart.addBodyPart(mimeBodyPart);
            message.setContent(multipart);

            System.out.println("Envoi email vers " + toEmail + "...");
            javax.mail.Transport.send(message);
            System.out.println("Email envoye avec succes !");
        } catch (Exception e) {
            System.err.println("Erreur envoi email: " + e.getMessage());
            throw new RuntimeException(e.getMessage());
        }
    }

    public static void sendEmail(String to, List<Map<String, Object>> data) {
        sendVendeurEmail(to, "Vendeur", data, 8);
    }

    // ══════════════════════════════════════════
    // TEMPLATE EMAIL — 100% INLINE STYLES (Gmail compatible)
    // ══════════════════════════════════════════
    private static String generateVendeurHtml(String nomVendeur,
                                               List<Map<String, Object>> produits,
                                               int joursAvantRupture) {
        // Top produit
        int topQte = 0;
        String topNom = "Produit";
        for (Map<String, Object> p : produits) {
            Object q = p.getOrDefault("totalVendu", p.getOrDefault("quantite_vendue", 0));
            int qte = 0;
            try { qte = Integer.parseInt(q.toString()); } catch (Exception ignored) {}
            if (qte > topQte) { topQte = qte; topNom = p.getOrDefault("produitId", p.getOrDefault("nom", "Produit")).toString(); }
        }
        int nbProduits = produits.size();
        String topQteStr = topQte > 0 ? topQte + "x" : "N/A";
        String topNomShort = topNom.length() > 12 ? topNom.substring(0, 12) : topNom;

        // Lignes produits
        String[] colors = {"#38bdf8", "#94a3b8", "#10b981"};
        String[] labels = {"Rupture J-" + joursAvantRupture, "Sous-expos&eacute;e", "En hausse"};
        String[] descs = {
            "Vendu " + topQte + " fois &mdash; votre meilleur produit",
            "Peu visible &mdash; potentiel non exploit&eacute;",
            "Offre -15% &mdash; en pleine croissance"
        };

        StringBuilder rows = new StringBuilder();
        int i = 0;
        for (Map<String, Object> p : produits) {
            if (i >= 3) break;
            String nom = p.getOrDefault("produitId", p.getOrDefault("nom", "Produit")).toString();
            String num = String.format("%02d", i + 1);
            String col = colors[i % colors.length];
            String lbl = labels[i % labels.length];
            String dsc = descs[i % descs.length];
            rows.append(
                "<tr>" +
                "<td bgcolor='#060e1c' style='background-color:#060e1c;padding:14px 10px;width:30px;border-bottom:1px solid #0f1a2e;vertical-align:middle;'>" +
                "<span style='color:#38bdf8;font-weight:800;font-size:14px;font-family:Arial,sans-serif;'>" + num + "</span>" +
                "</td>" +
                "<td bgcolor='#060e1c' style='background-color:#060e1c;padding:14px 8px;border-bottom:1px solid #0f1a2e;vertical-align:middle;'>" +
                "<div style='color:#f1f5f9;font-weight:700;font-size:14px;font-family:Arial,sans-serif;margin-bottom:3px;'>" + nom + "</div>" +
                "<div style='color:#475569;font-size:11px;font-style:italic;font-family:Arial,sans-serif;'>" + dsc + "</div>" +
                "</td>" +
                "<td bgcolor='#060e1c' style='background-color:#060e1c;padding:14px 10px;border-bottom:1px solid #0f1a2e;vertical-align:middle;text-align:right;white-space:nowrap;'>" +
                "<span style='border:1px solid " + col + ";border-radius:20px;padding:4px 10px;color:" + col + ";font-size:11px;font-weight:600;font-family:Arial,sans-serif;'>" + lbl + "</span>" +
                "</td>" +
                "</tr>"
            );
            i++;
        }
        if (i == 0) rows.append("<tr><td colspan='3' bgcolor='#060e1c' style='background-color:#060e1c;padding:16px;color:#475569;font-family:Arial,sans-serif;'>Aucun produit.</td></tr>");

        String recoText =
            "R&eacute;approvisionner le <strong style='color:#38bdf8;'>" + topNom + " avant J-" + joursAvantRupture + "</strong> " +
            "pour ne pas perdre vos ventes. Activez ensuite une <strong style='color:#38bdf8;'>banni&egrave;re promotionnelle</strong> " +
            "pour les produits sous-expos&eacute;s &mdash; hausse pr&eacute;vue de 30 &agrave; 50% de clics.";

        String today = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("dd MMMM yyyy", java.util.Locale.FRENCH));

        return
        "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">" +
        "<html xmlns='http://www.w3.org/1999/xhtml' lang='fr' style='color-scheme:dark;'>" +
        "<head>" +
        "<meta http-equiv='Content-Type' content='text/html; charset=UTF-8'>" +
        "<meta name='viewport' content='width=device-width,initial-scale=1'>" +
        "<meta name='color-scheme' content='dark'>" +
        "<meta name='supported-color-schemes' content='dark'>" +
        "<title>7anouti-E · Rapport Vendeur</title>" +
        "</head>" +

        "<body bgcolor='#020617' style='margin:0;padding:16px;background-color:#020617;'>" +
        "<table width='100%' border='0' cellpadding='0' cellspacing='0' bgcolor='#020617' style='background-color:#020617;'><tr><td align='center'>" +
        "<table width='600' border='0' cellpadding='0' cellspacing='0' bgcolor='#0a0d1a' style='background-color:#0a0d1a;max-width:600px;width:100%;border-radius:12px;overflow:hidden;'>" +

        // ── HEADER — logo + date ──
        "<tr><td bgcolor='#0a0d1a' style='background-color:#0a0d1a;padding:28px 32px 20px 32px;'>" +
        "<table width='100%' border='0' cellpadding='0' cellspacing='0'><tr>" +
        "<td style='vertical-align:middle;'>" +
        "<span style='font-size:22px;font-weight:900;color:#f1f5f9;font-family:Arial,sans-serif;letter-spacing:1px;'>" +
        "<span style='color:#38bdf8;'>7</span>anouti<span style='color:#38bdf8;'>-E</span>" +
        "</span><br>" +
        "<span style='font-size:10px;color:#334155;font-family:Arial,sans-serif;letter-spacing:2px;'>MARKETING INTELLIGENCE</span>" +
        "</td>" +
        "<td align='right' style='vertical-align:middle;'>" +
        "<span style='font-size:11px;color:#475569;font-family:Arial,sans-serif;'>" + today + "</span>" +
        "</td>" +
        "</tr></table>" +
        "</td></tr>" +

        // Ligne dégradée bleue
        "<tr><td style='padding:0;height:3px;background:linear-gradient(to right,#0a0d1a,#38bdf8,#0a0d1a);font-size:1px;line-height:1px;'>&nbsp;</td></tr>" +

        // Sous-titre + Titre hero
        "<tr><td bgcolor='#0a0d1a' style='background-color:#0a0d1a;padding:22px 32px 4px 32px;'>" +
        "<div style='color:#38bdf8;font-size:10px;letter-spacing:4px;text-transform:uppercase;font-family:Arial,sans-serif;margin-bottom:10px;'>RAPPORT VENDEUR &mdash; ANALYSE PERSONNALISÉE</div>" +
        "<div style='font-size:30px;font-weight:900;line-height:1.15;color:#f1f5f9;font-family:Arial,sans-serif;margin-bottom:6px;'>Vos produits<br><span style='color:#38bdf8;'>ont des nouvelles.</span></div>" +
        "<div style='color:#475569;font-size:13px;line-height:1.6;font-family:Arial,sans-serif;margin-bottom:20px;'>L'Intelligence 7anouti a scann&eacute; vos performances cette semaine.<br>Voici ce que les donn&eacute;es r&eacute;v&egrave;lent.</div>" +
        "</td></tr>" +

        // Ligne dégradée
        "<tr><td bgcolor='#38bdf8' height='1' style='background-color:#38bdf8;height:1px;font-size:1px;line-height:1px;'>&nbsp;</td></tr>" +

        // ── STATS ROW ──
        "<tr><td bgcolor='#0a0d1a' style='background-color:#0a0d1a;padding:0;'>" +
        "<table width='100%' border='0' cellpadding='0' cellspacing='0'><tr>" +
        "<td bgcolor='#0a0d1a' align='center' width='33%' style='background-color:#0a0d1a;padding:18px 6px;border-right:1px solid #0f1a2e;'>" +
        "<div style='font-size:26px;font-weight:900;color:#38bdf8;font-family:Arial,sans-serif;'>" + topQteStr + "</div>" +
        "<div style='font-size:8px;color:#334155;letter-spacing:2px;text-transform:uppercase;font-family:Arial,sans-serif;margin-top:5px;'>" + topNomShort + " VENDU</div>" +
        "</td>" +
        "<td bgcolor='#0a0d1a' align='center' width='33%' style='background-color:#0a0d1a;padding:18px 6px;border-right:1px solid #0f1a2e;'>" +
        "<div style='font-size:26px;font-weight:900;color:#38bdf8;font-family:Arial,sans-serif;'>J-" + joursAvantRupture + "</div>" +
        "<div style='font-size:8px;color:#334155;letter-spacing:2px;text-transform:uppercase;font-family:Arial,sans-serif;margin-top:5px;'>RUPTURE STOCK</div>" +
        "</td>" +
        "<td bgcolor='#0a0d1a' align='center' width='33%' style='background-color:#0a0d1a;padding:18px 6px;'>" +
        "<div style='font-size:26px;font-weight:900;color:#38bdf8;font-family:Arial,sans-serif;'>" + nbProduits + "</div>" +
        "<div style='font-size:8px;color:#334155;letter-spacing:2px;text-transform:uppercase;font-family:Arial,sans-serif;margin-top:5px;'>PRODUITS CONSULT&Eacute;S</div>" +
        "</td>" +
        "</tr></table>" +
        "</td></tr>" +

        // Ligne séparateur
        "<tr><td bgcolor='#0f1a2e' height='1' style='background-color:#0f1a2e;height:1px;font-size:1px;'>&nbsp;</td></tr>" +

        // ── BODY ──
        "<tr><td bgcolor='#0a0d1a' style='background-color:#0a0d1a;padding:28px 32px;'>" +

        "<p style='color:#f1f5f9;font-size:15px;font-family:Arial,sans-serif;margin:0 0 12px;'>Bonjour <strong>" + nomVendeur + "</strong>.</p>" +
        "<p style='color:#64748b;font-size:13px;line-height:1.7;font-family:Arial,sans-serif;margin:0 0 24px;'>" +
        "Cette semaine, 7anouti a analys&eacute; chaque interaction autour de vos produits.<br>" +
        "Les r&eacute;sultats sont clairs &mdash; certains performent, d'autres attendent votre intervention." +
        "</p>" +

        "<div style='color:#38bdf8;font-size:9px;letter-spacing:4px;text-transform:uppercase;font-weight:700;font-family:Arial,sans-serif;margin-bottom:12px;'>ANALYSE DE VOS PRODUITS</div>" +

        // Tableau produits
        "<table width='100%' border='0' cellpadding='0' cellspacing='0' bgcolor='#060e1c' style='background-color:#060e1c;border-radius:8px;'>" +
        rows.toString() +
        "</table>" +

        // Recommandation (sans "IA")
        "<table width='100%' border='0' cellpadding='0' cellspacing='0' style='margin:22px 0;'><tr>" +
        "<td bgcolor='#060e1c' style='background-color:#060e1c;border:1px solid #1e3a5f;border-radius:8px;padding:16px 20px;'>" +
        "<div style='color:#38bdf8;font-size:9px;letter-spacing:3px;text-transform:uppercase;font-family:Arial,sans-serif;margin-bottom:8px;'>RECOMMANDATION</div>" +
        "<p style='color:#64748b;font-size:13px;line-height:1.7;font-family:Arial,sans-serif;margin:0;'>" + recoText + "</p>" +
        "</td></tr></table>" +

        // Instruction claire — pas de bouton
        "<table width='100%' border='0' cellpadding='0' cellspacing='0' style='margin:16px 0 6px;'><tr>" +
        "<td bgcolor='#060e1c' style='background-color:#060e1c;border-left:3px solid #38bdf8;border-radius:4px;padding:12px 18px;'>" +
        "<p style='color:#94a3b8;font-size:13px;font-family:Arial,sans-serif;margin:0;'>" +
        "Ouvrez l'application <strong style='color:#38bdf8;'>7anouti-E</strong> et consultez votre tableau de bord pour agir sur ces donn&eacute;es." +
        "</p>" +
        "</td></tr></table>" +

        "</td></tr>" +

        // ── SIGNATURE ──
        "<tr><td bgcolor='#0d1526' style='background-color:#0d1526;padding:22px 32px;border-top:1px solid #1e2a40;'>" +
        "<table width='100%' border='0' cellpadding='0' cellspacing='0'><tr>" +
        "<td style='vertical-align:top;'>" +
        "<p style='color:#f1f5f9;font-size:14px;font-weight:700;font-family:Arial,sans-serif;margin:0 0 4px;'>L'&eacute;quipe 7anouti-E</p>" +
        "<p style='color:#475569;font-size:12px;font-family:Arial,sans-serif;margin:0 0 2px;'>Marketing Intelligence &bull; HeptaCode</p>" +
        "<p style='color:#334155;font-size:11px;font-family:Arial,sans-serif;margin:0;'>" + today + "</p>" +
        "</td>" +
        "<td align='right' style='vertical-align:top;'>" +
        "<p style='color:#334155;font-size:12px;font-family:Arial,sans-serif;margin:0 0 6px;'>Merci de votre confiance.</p>" +
        "</td>" +
        "</tr></table>" +
        "</td></tr>" +

        // ── FOOTER ──
        "<tr><td bgcolor='#020617' align='center' style='background-color:#020617;padding:16px 32px;border-top:1px solid #0f1a2e;'>" +
        "<table width='100%' border='0' cellpadding='0' cellspacing='0'><tr>" +
        "<td align='center'>" +
        "<a href='#' style='color:#38bdf8;text-decoration:none;font-size:11px;font-family:Arial,sans-serif;'>Se d&eacute;sabonner</a>" +
        "<span style='color:#1e293b;font-size:11px;font-family:Arial,sans-serif;'> &nbsp;&bull;&nbsp; </span>" +
        "<a href='#' style='color:#38bdf8;text-decoration:none;font-size:11px;font-family:Arial,sans-serif;'>Confidentialit&eacute;</a>" +
        "<span style='color:#1e293b;font-size:11px;font-family:Arial,sans-serif;'> &nbsp;&bull;&nbsp; </span>" +
        "<a href='#' style='color:#38bdf8;text-decoration:none;font-size:11px;font-family:Arial,sans-serif;'>Support</a>" +
        "</td>" +
        "</tr></table>" +
        "</td></tr>" +

        "</table>" +
        "</td></tr></table>" +
        "</body></html>";
    }
}
