package com.digitalbank.notification.consumer;

import com.digitalbank.transfer.domain.TransferFundsCompletedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Component
public class TransferFundsNotificationConsumer {

    private static final Logger LOG = LoggerFactory.getLogger(TransferFundsNotificationConsumer.class);

    private final JavaMailSender mailSender;

    @Value("${MAIL_FROM:noreply@digitalbank.com}")
    private String mailFrom;

    @Value("${MAIL_TO:admin@digitalbank.com}")
    private String mailTo;

    @Value("${MAIL_USERNAME:}")
    private String mailUsername;

    @Value("${MAIL_PASSWORD:}")
    private String mailPassword;

    public TransferFundsNotificationConsumer(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @KafkaListener(topics = "transfer-funds-completed", groupId = "notification-group")
    public void onTransferCompleted(TransferFundsCompletedEvent event) {
        LOG.info("method=[TransferFundsNotificationConsumer.onTransferCompleted] - Transferência concluída: transferId={}, de={}, para={}, valor={}",
            event.transferId(),
            event.sourceAccountId(),
            event.targetAccountId(),
            event.amount()
        );

        if (mailUsername.isBlank() || mailPassword.isBlank()) {
            LOG.warn("method=[TransferFundsNotificationConsumer.onTransferCompleted] - Credenciais de email não configuradas. Envio ignorado.");
            return;
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(mailFrom);
        message.setTo(mailTo);
        message.setSubject("Transferência concluída: " + event.transferId());
        message.setText(String.format(
            "Transferência realizada com sucesso.%n%nID: %s%nOrigem: %s%nDestino: %s%nValor: R$ %s",
            event.transferId(),
            event.sourceAccountId(),
            event.targetAccountId(),
            event.amount()
        ));

        try {
            mailSender.send(message);
            LOG.info("method=[TransferFundsNotificationConsumer.onTransferCompleted] - Email enviado com sucesso: transferId={}", event.transferId());
        } catch (Exception exception) {
            LOG.error("method=[TransferFundsNotificationConsumer.onTransferCompleted] - Falha ao enviar email: transferId={}", event.transferId(), exception);
        }
    }
}
