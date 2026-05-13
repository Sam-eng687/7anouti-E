package projet.hanouti.common.utils;

import jakarta.mail.*;
import jakarta.mail.internet.*;
import java.io.UnsupportedEncodingException;
import java.util.Properties;

public class MailSender {

    private static final String FROM_EMAIL = "khuludsagar12@gmail.com";
    private static final String APP_PASSWORD = "yaee eual widb xaxa";

    private static Session createSession() {
        Properties props = new Properties();
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");

        return Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(FROM_EMAIL, APP_PASSWORD);
            }
        });
    }

    public static void sendMail(String toEmail, String subject, String messageText) throws MessagingException {
        Session session = createSession();

        Message message = new MimeMessage(session);
        message.setFrom(new InternetAddress(FROM_EMAIL));
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
        message.setSubject(subject);
        message.setText(messageText);

        Transport.send(message);
    }

    public static void sendHtmlMail(String toEmail, String subject, String htmlContent) throws MessagingException {
        Session session = createSession();

        MimeMessage message = new MimeMessage(session);
        message.setFrom(new InternetAddress(FROM_EMAIL));
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
        message.setSubject(subject);
        message.setContent(htmlContent, "text/html; charset=UTF-8");

        Transport.send(message);
    }

    public static void sendHtmlMail(String toEmail, String subject, String htmlContent,
                                    String senderName, String replyToEmail) throws MessagingException {
        Session session = createSession();

        MimeMessage message = new MimeMessage(session);
        message.setFrom(namedSender(senderName, replyToEmail));
        message.setSender(new InternetAddress(FROM_EMAIL));
        if (replyToEmail != null && !replyToEmail.isBlank()) {
            message.setReplyTo(InternetAddress.parse(replyToEmail));
        }
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
        message.setSubject(subject);
        message.setContent(htmlContent, "text/html; charset=UTF-8");

        Transport.send(message);
    }

    private static InternetAddress namedSender(String senderName, String senderEmail) throws MessagingException {
        try {
            String fromEmail = senderEmail == null || senderEmail.isBlank() ? FROM_EMAIL : senderEmail;
            if (senderName == null || senderName.isBlank()) {
                return new InternetAddress(fromEmail, "7anouti-E");
            }
            return new InternetAddress(fromEmail, senderName);
        } catch (UnsupportedEncodingException e) {
            throw new MessagingException("Nom d'expediteur invalide", e);
        }
    }

    public static void sendMailWithAttachment(String toEmail, String subject, String messageText, String filePath)
            throws MessagingException, java.io.IOException {

        Session session = createSession();

        Message message = new MimeMessage(session);
        message.setFrom(new InternetAddress(FROM_EMAIL));
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
        message.setSubject(subject);

        MimeBodyPart textPart = new MimeBodyPart();
        textPart.setText(messageText, "UTF-8");

        MimeBodyPart attachmentPart = new MimeBodyPart();
        attachmentPart.attachFile(filePath);

        Multipart multipart = new MimeMultipart();
        multipart.addBodyPart(textPart);
        multipart.addBodyPart(attachmentPart);

        message.setContent(multipart);

        Transport.send(message);
    }
}
