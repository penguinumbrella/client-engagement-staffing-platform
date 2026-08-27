package com.skillstorm.notification.repository;

import com.skillstorm.notification.model.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByActiveTrue();

    List<Notification> findByRecipientIdAndActiveTrue(UUID recipientId);
}
