package projet.hanouti.GestionCommandes.services;


import projet.hanouti.GestionCommandes.entities.Commande;
import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.util.Properties;

/**
 * Service d'envoi d'emails automatiques.
 *
 * Emails envoyés automatiquement selon les événements :
 *  - Commande expédiée     → email de confirmation d'expédition
 *  - Commande refusée      → email avec motif de refus obligatoire
 *
 * Utilise l'API Jakarta Mail.
 * Dépendance Maven : com.sun.mail:jakarta.mail:2.0.1.
 *
 * Configuration SMTP dans utils/MailConfig.java ou application.properties.
 */
public class EmailService {

    // =========================================================
    // CONFIGURATION SMTP
    // Modifier ces valeurs selon votre fournisseur email.
    // Il est recommandé de les externaliser dans un fichier config.
    // =========================================================

    private static final String SMTP_HOST     = "smtp.gmail.com";
    private static final String SMTP_PORT     = "587";
    private static final String SMTP_USERNAME = "samarjandoubi45@gmail.com";   // À configurer
    private static final String SMTP_PASSWORD = "zsof otoy xzya zbms";  // À configurer
    private static final String FROM_NAME     = "7ANOUTI-E";

    // =========================================================
    // EMAILS METIER
    // =========================================================

    /**
     * Envoie un email de confirmation d'expédition à l'acheteur.
     * Déclenché quand le statut passe à EXPEDIEE.
     *
     * @param emailDestinataire adresse email de l'acheteur
     * @param commande          la commande expédiée
     */
    public void envoyerEmailExpedition(String emailDestinataire, Commande commande) {
        String sujet = "Votre commande " + commande.getNumeroCommande() + " a été expédiée !";

        String corps = construireCorpsExpedition(commande);

        envoyerEmail(emailDestinataire, sujet, corps);
    }

    /**
     * Envoie un email de refus de commande à l'acheteur avec le motif.
     * Déclenché quand le vendeur refuse une commande.
     *
     * @param emailDestinataire adresse email de l'acheteur
     * @param commande          la commande refusée
     * @param motif             motif de refus (obligatoire selon le cahier des charges)
     */
    public void envoyerEmailRefus(String emailDestinataire, Commande commande, String motif) {
        if (motif == null || motif.isBlank()) {
            throw new IllegalArgumentException("Le motif de refus est obligatoire pour l'email.");
        }

        String sujet = "Votre commande " + commande.getNumeroCommande() + " a été refusée";

        String corps = construireCorpsRefus(commande, motif);

        envoyerEmail(emailDestinataire, sujet, corps);
    }

    // =========================================================
    // METHODES PRIVEES
    // =========================================================

    /**
     * Envoie un email via SMTP avec authentification TLS.
     *
     * @param destinataire adresse email du destinataire
     * @param sujet        sujet de l'email
     * @param corps        corps HTML de l'email
     */
    private void envoyerEmail(String destinataire, String sujet, String corps) {
        Properties props = new Properties();
        props.put("mail.smtp.auth",            "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host",            SMTP_HOST);
        props.put("mail.smtp.port",            SMTP_PORT);
        props.put("mail.smtp.ssl.trust",       SMTP_HOST);
        props.put("mail.smtp.ssl.protocols", "TLSv1.2");

        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(SMTP_USERNAME, SMTP_PASSWORD);
            }
        });

        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(SMTP_USERNAME, FROM_NAME));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(destinataire));
            message.setSubject(sujet);
            message.setContent(corps, "text/html; charset=utf-8");

            Transport.send(message);
            System.out.println("[EmailService] Email envoyé à : " + destinataire);

        } catch (Exception e) {
            System.err.println("[EmailService.envoyerEmail] Erreur : " + e.getMessage());
        }
    }

    /**
     * Construit le corps HTML de l'email d'expédition.
     */
    private String construireCorpsExpedition(Commande commande) {
        return """
            <html>
            <body style="font-family: Arial, sans-serif; color: #333;">
                <div style="max-width: 600px; margin: 0 auto; padding: 20px;">

                    <h2 style="color: #2c7a2c;">📦 Votre commande est en route !</h2>

                    <p>Bonjour,</p>
                    <p>Bonne nouvelle ! Votre commande a bien été expédiée.</p>

                    <div style="background: #f5f5f5; padding: 15px; border-radius: 8px; margin: 20px 0;">
                        <strong>Numéro de commande :</strong> %s<br>
                        <strong>Montant total :</strong> %.2f TND<br>
                        <strong>Adresse de livraison :</strong> %s
                    </div>

                    <p>Vous pouvez suivre votre commande depuis votre espace <strong>Mes commandes</strong>
                    sur l'application 7ANOUTI-E.</p>

                    <p style="color: #888; font-size: 12px;">
                        Cet email a été envoyé automatiquement. Merci de ne pas y répondre.
                    </p>

                    <p>— L'équipe <strong>7ANOUTI-E</strong></p>
                </div>
            </body>
            </html>
            """.formatted(
                commande.getNumeroCommande(),
                commande.getTotal(),
                commande.getAdresseLivraison()
        );
    }

    /**
     * Construit le corps HTML de l'email de refus avec motif.
     */
    private String construireCorpsRefus(Commande commande, String motif) {
        return """
            <html>
            <body style="font-family: Arial, sans-serif; color: #333;">
                <div style="max-width: 600px; margin: 0 auto; padding: 20px;">

                    <h2 style="color: #c0392b;">❌ Votre commande a été refusée</h2>

                    <p>Bonjour,</p>
                    <p>Nous sommes désolés de vous informer que votre commande n'a pas pu être traitée.</p>

                    <div style="background: #fff3f3; border-left: 4px solid #c0392b;
                                padding: 15px; border-radius: 4px; margin: 20px 0;">
                        <strong>Numéro de commande :</strong> %s<br>
                        <strong>Montant :</strong> %.2f TND<br>
                        <strong>Motif du refus :</strong> %s
                    </div>

                    <p>Si vous avez des questions, vous pouvez contacter le support depuis votre espace client.</p>

                    <p style="color: #888; font-size: 12px;">
                        Cet email a été envoyé automatiquement. Merci de ne pas y répondre.
                    </p>

                    <p>— L'équipe <strong>7ANOUTI-E</strong></p>
                </div>
            </body>
            </html>
            """.formatted(
                commande.getNumeroCommande(),
                commande.getTotal(),
                motif
        );
    }
}
