package com.gamelog.gamelog.service.auth;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PasswordResetNotificationServiceImplTest {

    @Mock
    private JavaMailSender mailSender;

    private PasswordResetNotificationServiceImpl notificationService;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        notificationService = new PasswordResetNotificationServiceImpl(mailSender, "noreply@gamelog.com");
    }

    @Test
    void sendResetLinkShouldSendMailWithExpectedContent() {
        notificationService.sendResetLink("user@mail.com", "http://localhost:3000/reset-password?token=abc");

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());

        SimpleMailMessage message = captor.getValue();
        assertEquals("noreply@gamelog.com", message.getFrom());
        assertEquals("user@mail.com", message.getTo()[0]);
        assertEquals("Redefinicao de senha - GameLog", message.getSubject());
    }

    @Test
    void sendResetLinkShouldThrowWhenMailSenderFails() {
        MailSendException error = new MailSendException("smtp failed");
        org.mockito.Mockito.doThrow(error).when(mailSender).send(org.mockito.Mockito.any(SimpleMailMessage.class));

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> notificationService.sendResetLink("user@mail.com", "link")
        );

        assertEquals("Nao foi possivel enviar o email de recuperacao", exception.getMessage());
    }
}
