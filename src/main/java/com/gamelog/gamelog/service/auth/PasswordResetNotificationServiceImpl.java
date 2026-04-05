package com.gamelog.gamelog.service.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class PasswordResetNotificationServiceImpl implements PasswordResetNotificationService {

    private static final Logger log = LoggerFactory.getLogger(PasswordResetNotificationServiceImpl.class);
    private final JavaMailSender mailSender;
    private final String mailFrom;

    public PasswordResetNotificationServiceImpl(
            JavaMailSender mailSender,
            @Value("${app.mail.from}") String mailFrom
    ) {
        this.mailSender = mailSender;
        this.mailFrom = mailFrom;
    }

    @Override
    public void sendResetLink(String email, String resetLink) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(mailFrom);
        message.setTo(email);
        message.setSubject("Redefinicao de senha - GameLog");
        message.setText(
                "Voce solicitou a redefinicao de senha.\n\n"
                        + "Clique no link abaixo para continuar:\n"
                        + resetLink
                        + "\n\n"
                        + "Se voce nao solicitou esta acao, ignore este email."
        );

        try {
            mailSender.send(message);
            log.info("Password reset email sent to {}", email);
        } catch (MailException exception) {
            log.error("Could not send reset email to {}", email, exception);
            throw new IllegalStateException("Nao foi possivel enviar o email de recuperacao");
        }
    }
}
