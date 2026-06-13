package model.auth;

import jakarta.mail.*;
import jakarta.mail.internet.*;
import java.util.Properties;

public class EmailResetService {
    /**
     * Performs reset code.
     * @param recipientEmail the recipientEmail.
     * @param code the code.
     */
    public static void sendResetCode(String recipientEmail, String code) throws MessagingException {
        // Configuration SMTP (Gmail exemple)
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");

        Session session = Session.getInstance(props, new Authenticator() {
            /**
             * Returns the password authentication.
             * @return the PasswordAuthentication.
             */
            protected PasswordAuthentication getPasswordAuthentication() {
                // Remplacer par vos identifiants réels (ou variables d'environnement)
                return new PasswordAuthentication("votre-email@gmail.com", "votre-mdp-app");
            }
        });

        Message message = new MimeMessage(session);
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipientEmail));
        message.setSubject("Code de réinitialisation");
        message.setText("Votre code de réinitialisation est : " + code);
        Transport.send(message);
    }
}
