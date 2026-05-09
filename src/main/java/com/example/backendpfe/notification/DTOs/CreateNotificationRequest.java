package com.example.backendpfe.notification.DTOs;

import com.example.backendpfe.notification.NotificationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateNotificationRequest {

    @NotNull
    private Long userId;

    @NotBlank
    private String title;

    @NotBlank
    private String body;

    @NotNull
    private NotificationType type;
}