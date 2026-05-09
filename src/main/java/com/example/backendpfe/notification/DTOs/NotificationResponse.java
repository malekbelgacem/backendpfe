package com.example.backendpfe.notification.DTOs;

import com.example.backendpfe.notification.NotificationType;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class NotificationResponse {

    private Long id;
    private String title;
    private String body;
    private NotificationType type;
    private Boolean seen;
    private Instant createdAt;
}