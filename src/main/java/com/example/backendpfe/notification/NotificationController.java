package com.example.backendpfe.notification;

import com.example.backendpfe.exception.ResourceNotFoundException;
import com.example.backendpfe.notification.DTOs.CreateNotificationRequest;
import com.example.backendpfe.notification.DTOs.NotificationResponse;
import com.example.backendpfe.user.User;
import com.example.backendpfe.user.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final UserRepository userRepository;

    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ANALYST', 'AUDITOR')")
    @PostMapping
    public ResponseEntity<NotificationResponse> sendNotification(
            @Valid @RequestBody CreateNotificationRequest request
    ) {
        return ResponseEntity.ok(notificationService.sendNotification(request));
    }

    @GetMapping("/my")
    public ResponseEntity<List<NotificationResponse>> getMyNotifications(Authentication authentication) {
        User currentUser = getCurrentUser(authentication);
        return ResponseEntity.ok(notificationService.getMyNotifications(currentUser.getIdUser()));
    }
    @GetMapping("/my/unread-count")
    public ResponseEntity<Map<String, Long>> getUnreadCount(Authentication authentication) {
        User currentUser = getCurrentUser(authentication);
        long count = notificationService.getUnreadCount(currentUser.getIdUser());
        return ResponseEntity.ok(Map.of("unreadCount", count));
    }

    @GetMapping("/my/{id}")
    public ResponseEntity<NotificationResponse> getMyNotificationById(
            @PathVariable Long id,
            Authentication authentication
    ) {
        User currentUser = getCurrentUser(authentication);
        return ResponseEntity.ok(notificationService.getMyNotificationById(id, currentUser.getIdUser()));
    }

    @PatchMapping("/{id}/seen")
    public ResponseEntity<NotificationResponse> markAsSeen(
            @PathVariable Long id,
            Authentication authentication
    ) {
        User currentUser = getCurrentUser(authentication);
        return ResponseEntity.ok(notificationService.markAsSeen(id, currentUser.getIdUser()));
    }

    @PatchMapping("/seen-all")
    public ResponseEntity<Map<String, String>> markAllAsSeen(Authentication authentication) {
        User currentUser = getCurrentUser(authentication);
        notificationService.markAllAsSeen(currentUser.getIdUser());
        return ResponseEntity.ok(Map.of("message", "Toutes les notifications ont été marquées comme lues"));
    }



    private User getCurrentUser(Authentication authentication) {
        return userRepository.findByUsernameAndIsDeletedFalse(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur connecté introuvable"));
    }
}