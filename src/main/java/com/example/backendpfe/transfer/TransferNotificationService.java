package com.example.backendpfe.transfer;

import com.example.backendpfe.account.Account;
import com.example.backendpfe.notification.Notification;
import com.example.backendpfe.notification.NotificationRepository;
import com.example.backendpfe.notification.NotificationType;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class TransferNotificationService {

    private final NotificationRepository notificationRepository;

    @org.springframework.transaction.annotation.Transactional(propagation = Propagation.REQUIRES_NEW)
    public void createTransferCreatedNotification(Account sender, BigDecimal amount, String receiverAccountNumber) {
        if (sender == null || sender.getClient() == null) return;

        Notification notification = Notification.builder()
                .title("Virement créé")
                .body("Votre demande de virement de " + amount + " vers le compte " + receiverAccountNumber + " a été enregistrée.")
                .type(NotificationType.TRANSFER_CREATED)
                .user(sender.getClient())
                .seen(false)
                .build();

        notificationRepository.save(notification);
    }

    @org.springframework.transaction.annotation.Transactional(propagation = Propagation.REQUIRES_NEW)
    public void createTransferApprovedNotification(Account sender, Account receiver, BigDecimal amount) {
        if (sender != null && sender.getClient() != null) {
            Notification notifSender = Notification.builder()
                    .title("Virement approuvé")
                    .body("Votre virement de " + amount + " vers le compte " + receiver.getAccountNumber() + " a été approuvé avec succès.")
                    .type(NotificationType.TRANSFER_APPROVED)
                    .user(sender.getClient())
                    .seen(false)
                    .build();

            notificationRepository.save(notifSender);
        }

        if (receiver != null && receiver.getClient() != null) {
            Notification notifReceiver = Notification.builder()
                    .title("Virement reçu")
                    .body("Vous avez reçu un virement de " + amount + " depuis le compte " + sender.getAccountNumber() + ".")
                    .type(NotificationType.TRANSFER_RECEIVED)
                    .user(receiver.getClient())
                    .seen(false)
                    .build();

            notificationRepository.save(notifReceiver);
        }
    }

    @org.springframework.transaction.annotation.Transactional(propagation = Propagation.REQUIRES_NEW)
    public void createTransferRejectedNotification(Account sender, String title, String body) {
        if (sender == null || sender.getClient() == null) return;

        Notification notification = Notification.builder()
                .title(title)
                .body(body)
                .type(NotificationType.TRANSFER_REJECTED)
                .user(sender.getClient())
                .seen(false)
                .build();

        notificationRepository.save(notification);
    }
    @org.springframework.transaction.annotation.Transactional(propagation = Propagation.REQUIRES_NEW)
    public void createTransferBlockedNotification(Account sender, BigDecimal amount, String receiverAccountNumber) {
        if (sender == null || sender.getClient() == null) return;

        Notification notification = Notification.builder()
                .title("Virement bloqué")
                .body("Votre virement de " + amount + " vers le compte " + receiverAccountNumber +
                        " est bloqué temporairement à cause des règles de sécurité. Un analyste va examiner la transaction.")
                .type(NotificationType.SECURITY_ALERT)
                .user(sender.getClient())
                .seen(false)
                .build();

        notificationRepository.save(notification);
    }
}
