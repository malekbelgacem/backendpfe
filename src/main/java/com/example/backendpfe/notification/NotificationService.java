package com.example.backendpfe.notification;

import com.example.backendpfe.exception.ResourceNotFoundException;
import com.example.backendpfe.notification.DTOs.CreateNotificationRequest;
import com.example.backendpfe.notification.DTOs.NotificationResponse;
import com.example.backendpfe.user.User;
import com.example.backendpfe.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    public NotificationResponse createNotification(User user, NotificationType type, String title, String body) {
        Notification notification = Notification.builder()
                .user(user)
                .type(type)
                .title(title)
                .body(body)
                .seen(false)
                .build();

        notification = notificationRepository.save(notification);
        return mapToResponse(notification);
    }

    public NotificationResponse sendNotification(CreateNotificationRequest request) {
        User targetUser = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur cible introuvable"));

        Notification notification = Notification.builder()
                .user(targetUser)
                .title(request.getTitle().trim())
                .body(request.getBody().trim())
                .type(request.getType())
                .seen(false)
                .build();

        notification = notificationRepository.save(notification);

        return mapToResponse(notification);
    }

    public List<NotificationResponse> getMyNotifications(Long userId) {
        return notificationRepository.findByUser_IdUserOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    public NotificationResponse getMyNotificationById(Long notificationId, Long userId) {
        Notification notification = notificationRepository.findByIdAndUser_IdUser(notificationId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification introuvable"));

        return mapToResponse(notification);
    }

    public NotificationResponse markAsSeen(Long notificationId, Long userId) {
        Notification notification = notificationRepository.findByIdAndUser_IdUser(notificationId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification introuvable"));

        notification.setSeen(true);
        notification = notificationRepository.save(notification);

        return mapToResponse(notification);
    }

    public void markAllAsSeen(Long userId) {
        List<Notification> notifications = notificationRepository.findByUser_IdUserAndSeenFalse(userId);

        for (Notification notification : notifications) {
            notification.setSeen(true);
        }

        notificationRepository.saveAll(notifications);
    }

    public long getUnreadCount(Long userId) {
        return notificationRepository.countByUser_IdUserAndSeenFalse(userId);
    }

    private NotificationResponse mapToResponse(Notification notification) {
        return NotificationResponse.builder()
                .id(notification.getId())
                .title(notification.getTitle())
                .body(notification.getBody())
                .type(notification.getType())
                .seen(notification.getSeen())
                .createdAt(notification.getCreatedAt())
                .build();
    }
}