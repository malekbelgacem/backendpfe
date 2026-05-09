package com.example.backendpfe.notification;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByUser_IdUserOrderByCreatedAtDesc(Long userId);

    Optional<Notification> findByIdAndUser_IdUser(Long id, Long userId);

    List<Notification> findByUser_IdUserAndSeenFalse(Long userId);

    long countByUser_IdUserAndSeenFalse(Long userId);
}