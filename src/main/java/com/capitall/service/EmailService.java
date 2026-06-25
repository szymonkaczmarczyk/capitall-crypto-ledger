package com.capitall.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final ObjectProvider<JavaMailSender> mailSenderProvider;

    @Value("${spring.mail.username:}")
    private String configuredUsername;

    @Value("${capitall.mail.from:noreply@capitall.example}")
    private String fromAddress;

    @Value("${capitall.app.base-url:http://localhost:8080}")
    private String baseUrl;

    public EmailService(ObjectProvider<JavaMailSender> mailSenderProvider) {
        this.mailSenderProvider = mailSenderProvider;
    }

    public void sendActivationEmail(String to, String activationToken) {
        String link = baseUrl + "/activate?token=" + activationToken;
        String subject = "Capitall — aktywuj konto i ustaw 2FA";
        String body = "Witaj!\n\n" +
                "Aby dokończyć rejestrację w Capitall, kliknij w poniższy link, " +
                "aby aktywować konto i skonfigurować weryfikację dwuetapową (Microsoft Authenticator):\n\n" +
                link + "\n\n" +
                "Link jest ważny przez 24 godziny.\n\n" +
                "Jeśli to nie Ty zakładałeś konto — zignoruj tę wiadomość.\n\n" +
                "— Zespół Capitall";
        send(to, subject, body);
    }

    private void send(String to, String subject, String body) {
        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null || configuredUsername == null || configuredUsername.isBlank()) {
            log.warn("==========================================================================");
            log.warn("SMTP not configured (set MAIL_USERNAME / MAIL_PASSWORD). E-mail printed below:");
            log.warn("  To:      {}", to);
            log.warn("  Subject: {}", subject);
            for (String line : body.split("\n")) {
                log.warn("  | {}", line);
            }
            log.warn("==========================================================================");
            return;
        }
        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setFrom(fromAddress);
            msg.setTo(to);
            msg.setSubject(subject);
            msg.setText(body);
            mailSender.send(msg);
            log.info("Sent e-mail to {} subject='{}'", to, subject);
        } catch (Exception ex) {
            log.error("Failed to send e-mail to {}: {}", to, ex.getMessage());
        }
    }
}
