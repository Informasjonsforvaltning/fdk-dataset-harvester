package no.dcat.harvester.notification;

import no.dcat.harvester.crawler.notification.EmailNotificationService;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import static org.mockito.Mockito.*;

@Tag("unit")
public class EmailNotificationServiceTest {

    @Test
    public void sendMailOK() {
        JavaMailSender javaMailSender = mock(JavaMailSender.class);
        doNothing().when(javaMailSender).send((SimpleMailMessage) anyObject());

        EmailNotificationService emailService = new EmailNotificationService(javaMailSender);

        emailService.sendValidationResultNotification("fromAdress", "toaddress", "subject", "messageText");
    }

    @Test
    public void sendMailHandlesSendMailException() {
        JavaMailSender javaMailSender = mock(JavaMailSender.class);
        doThrow(new MailSendException("error")).when(javaMailSender).send((SimpleMailMessage) anyObject());

        EmailNotificationService emailService = new EmailNotificationService(javaMailSender);
        emailService.sendValidationResultNotification("fromAdress", "toaddress", "subject", "messageText");
    }
}
