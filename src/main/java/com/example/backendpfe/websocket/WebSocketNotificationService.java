package com.example.backendpfe.websocket;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class WebSocketNotificationService {

    private final SimpMessagingTemplate messagingTemplate;

    public void sendTransactionUpdate(Object transaction) {
        messagingTemplate.convertAndSend("/topic/transactions", transaction);
    }

    public void sendAlertUpdate(Object alert) {
        messagingTemplate.convertAndSend("/topic/alerts", alert);
    }

    public void sendDashboardUpdate(Object data) {
        messagingTemplate.convertAndSend("/topic/dashboard", data);
    }
}