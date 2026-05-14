package projet.hanouti.module4.service;

import jakarta.mail.*;
import jakarta.mail.internet.*;
import java.util.Properties;

public class EmailService {

    private static final String EMAIL_FROM     = "zeidisamar3@gmail.com";
    private static final String EMAIL_PASSWORD = "bbxl rxqd yzyi yoqy";

    public static void sendPaymentConfirmation(
            String toEmail,
            String clientName,
            String reference,
            String methode,
            double montant
    ) {
        new Thread(() -> {
            try {
                Properties props = new Properties();
                props.put("mail.smtp.auth",            "true");
                props.put("mail.smtp.starttls.enable", "true");
                props.put("mail.smtp.host",            "smtp.gmail.com");
                props.put("mail.smtp.port",            "587");

                Session session = Session.getInstance(props, new Authenticator() {
                    @Override
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(EMAIL_FROM, EMAIL_PASSWORD);
                    }
                });

                Message message = new MimeMessage(session);
                message.setFrom(new InternetAddress(EMAIL_FROM, "7anouti-E"));
                message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
                message.setSubject("✅ Confirmation de paiement — " + reference);
                message.setContent(buildHtmlBody(clientName, reference, methode, montant), "text/html; charset=utf-8");

                Transport.send(message);
                System.out.println("[EmailService] Email envoyé à : " + toEmail);

            } catch (Exception e) {
                System.err.println("[EmailService] Erreur envoi email : " + e.getMessage());
            }
        }, "email-thread").start();
    }

    private static String buildHtmlBody(String name, String ref, String methode, double montant) {
        String methodIcon = switch (methode) {
            case "CIB"     -> "🏦";
            case "D17"     -> "📱";
            case "Espèces" -> "💵";
            default        -> "💳";
        };
        String statutLabel = "Espèces".equals(methode) ? "⏳ En attente (livraison)" : "● Validé";
        String statutColor = "Espèces".equals(methode) ? "#F59E0B" : "#4ade80";

        java.time.format.DateTimeFormatter fmt = java.time.format.DateTimeFormatter
                .ofPattern("dd MMMM yyyy, HH:mm", java.util.Locale.FRENCH);
        String date = java.time.LocalDateTime.now().format(fmt);

        return """
            <!DOCTYPE html>
            <html lang="fr">
            <head>
              <meta charset="UTF-8"/>
              <meta name="viewport" content="width=device-width,initial-scale=1.0"/>
              <link href="https://fonts.googleapis.com/css2?family=Playfair+Display:wght@400;600&family=DM+Sans:wght@300;400;500&display=swap" rel="stylesheet"/>
            </head>
            <body style="margin:0;padding:0;background:#1a1a2e;font-family:'DM Sans',Arial,sans-serif;">
              <table width="100%%" cellpadding="0" cellspacing="0">
                <tr><td align="center" style="padding:40px 16px;">

                  <table width="560" cellpadding="0" cellspacing="0"
                         style="border-radius:18px;overflow:hidden;box-shadow:0 12px 48px rgba(0,0,0,0.6);">

                    <!-- HEADER -->
                    <tr>
                      <td style="background:linear-gradient(135deg,#1a3a8f 0%%,#2563eb 60%%,#1e40af 100%%);
                                 padding:36px 28px 30px;text-align:center;">
                        <div style="width:60px;height:60px;background:rgba(255,255,255,0.15);
                                    border-radius:50%%;margin:0 auto 16px;
                                    border:2px solid rgba(255,255,255,0.28);
                                    line-height:60px;font-size:22px;font-weight:600;
                                    color:#fff;font-family:'Playfair Display',serif;">7E</div>
                        <div style="font-family:'Playfair Display',serif;color:#fff;
                                    font-size:28px;font-weight:600;letter-spacing:1px;
                                    margin-bottom:6px;">7anouti-E</div>
                        <div style="color:rgba(255,255,255,0.68);font-size:11px;
                                    letter-spacing:2.5px;text-transform:uppercase;">
                          Confirmation de paiement</div>
                      </td>
                    </tr>

                    <!-- BODY -->
                    <tr>
                      <td style="background:#1e2235;padding:34px 30px;">

                        <!-- Greeting -->
                        <p style="color:#e2e8f0;font-size:15px;line-height:1.8;margin:0 0 4px;">
                          Cher(e)
                          <span style="color:#93c5fd;font-weight:600;font-size:18px;
                                       font-family:'Playfair Display',serif;">%s</span>,
                        </p>
                        <p style="color:#94a3b8;font-size:13.5px;margin:0 0 22px;">
                          Nous sommes ravis de vous confirmer que votre paiement a bien été reçu et validé.
                        </p>

                        <!-- Status badge -->
                        <table width="100%%" cellpadding="0" cellspacing="0"
                               style="background:rgba(34,197,94,0.12);
                                      border:1px solid rgba(34,197,94,0.3);
                                      border-radius:10px;margin-bottom:22px;">
                          <tr>
                            <td style="padding:13px 18px;">
                              <span style="color:#4ade80;font-size:16px;margin-right:10px;">✓</span>
                              <span style="color:#86efac;font-size:14px;font-weight:500;">
                                Paiement confirmé avec succès</span>
                            </td>
                          </tr>
                        </table>

                        <!-- Amount banner -->
                        <table width="100%%" cellpadding="0" cellspacing="0"
                               style="background:linear-gradient(135deg,#1e3a8a,#1d4ed8);
                                      border-radius:12px;margin-bottom:22px;">
                          <tr>
                            <td style="padding:22px;text-align:center;">
                              <div style="color:rgba(255,255,255,0.6);font-size:11px;
                                          letter-spacing:2px;text-transform:uppercase;
                                          margin-bottom:7px;">Montant payé</div>
                              <div style="color:#fff;font-family:'Playfair Display',serif;
                                          font-size:34px;font-weight:600;">
                                %.2f <span style="font-size:17px;font-weight:400;opacity:0.8;">TND</span>
                              </div>
                            </td>
                          </tr>
                        </table>

                        <!-- Detail rows -->
                        <table width="100%%" cellpadding="0" cellspacing="0"
                               style="background:#252b45;border-radius:12px;
                                      margin-bottom:22px;overflow:hidden;">
                          <tr style="border-bottom:1px solid rgba(255,255,255,0.06);">
                            <td style="padding:13px 20px;color:#94a3b8;font-size:13px;">Référence</td>
                            <td style="padding:13px 20px;color:#e2e8f0;font-size:13px;
                                       font-weight:500;text-align:right;">%s</td>
                          </tr>
                          <tr style="border-bottom:1px solid rgba(255,255,255,0.06);">
                            <td style="padding:13px 20px;color:#94a3b8;font-size:13px;">Méthode</td>
                            <td style="padding:13px 20px;color:#e2e8f0;font-size:13px;
                                       font-weight:500;text-align:right;">%s %s</td>
                          </tr>
                          <tr style="border-bottom:1px solid rgba(255,255,255,0.06);">
                            <td style="padding:13px 20px;color:#94a3b8;font-size:13px;">Destinataire</td>
                            <td style="padding:13px 20px;color:#e2e8f0;font-size:13px;
                                       font-weight:500;text-align:right;">7anoutiE</td>
                          </tr>
                          <tr style="border-bottom:1px solid rgba(255,255,255,0.06);">
                            <td style="padding:13px 20px;color:#94a3b8;font-size:13px;">Date</td>
                            <td style="padding:13px 20px;color:#e2e8f0;font-size:13px;
                                       font-weight:500;text-align:right;">%s</td>
                          </tr>
                          <tr>
                            <td style="padding:13px 20px;color:#94a3b8;font-size:13px;">Statut</td>
                            <td style="padding:13px 20px;font-size:13px;font-weight:500;
                                       text-align:right;color:%s;">%s</td>
                          </tr>
                        </table>

                        <!-- Personal message -->
                        <table width="100%%" cellpadding="0" cellspacing="0"
                               style="background:rgba(37,99,235,0.1);
                                      border-left:3px solid #2563eb;
                                      border-radius:0 10px 10px 0;margin-bottom:24px;">
                          <tr>
                            <td style="padding:18px 20px;color:#cbd5e1;
                                       font-size:13.5px;line-height:1.9;">
                              <span style="color:#93c5fd;font-weight:500;">%s</span>,
                              merci infiniment pour votre confiance et votre fidélité envers
                              <span style="color:#93c5fd;font-weight:500;">7anouti-E</span> ✨<br><br>
                              Votre commande a été enregistrée avec succès sous la référence
                              <span style="color:#93c5fd;font-weight:500;">%s</span>
                              et est maintenant en cours de traitement. Notre équipe s'engage
                              à vous offrir la meilleure expérience possible.<br><br>
                              Vous recevrez une notification dès que votre commande sera prête.
                              En attendant, n'hésitez pas à nous contacter pour toute question
                              — nous sommes toujours là pour vous 🤝
                            </td>
                          </tr>
                        </table>

                        <!-- Sign off -->
                        <p style="color:#94a3b8;font-size:13px;margin:0 0 3px;">
                          Avec toute notre gratitude,</p>
                        <p style="color:#60a5fa;font-size:14px;font-weight:500;margin:0 0 20px;">
                          L'équipe 7anouti-E 💙</p>

                        <hr style="border:none;border-top:1px solid rgba(255,255,255,0.07);margin:0 0 16px;"/>

                        <p style="color:#64748b;font-size:12px;text-align:center;margin:0;">
                          Des questions ? Écrivez-nous à
                          <a href="mailto:support@7anouti-e.tn"
                             style="color:#60a5fa;text-decoration:none;">support@7anouti-e.tn</a>
                        </p>
                      </td>
                    </tr>

                    <!-- FOOTER -->
                    <tr>
                      <td style="background:#161929;padding:20px 30px;text-align:center;">
                        <p style="color:#475569;font-size:11.5px;line-height:1.7;margin:0;">
                          © 2025 7anouti-E · Tunisie<br/>
                          Ce message est un reçu officiel de votre transaction sécurisée.
                        </p>
                      </td>
                    </tr>

                  </table>
                </td></tr>
              </table>
            </body>
            </html>
            """.formatted(name, montant, ref, methodIcon, methode, date, statutColor, statutLabel, name, ref);
    }
}