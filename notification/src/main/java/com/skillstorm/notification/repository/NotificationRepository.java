package com.skillstorm.notification.repository;

import com.skillstorm.notification.model.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByActiveTrue();

    List<Notification> findByRecipientIdAndActiveTrue(Long recipientId);

    List<Notification> findTop100ByOrderByCreatedAtDesc();
}
