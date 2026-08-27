package com.skillstorm.notification.service;

import com.skillstorm.notification.dto.CreateNotificationRequest;
import com.skillstorm.notification.dto.NotificationEvent;
import com.skillstorm.notification.dto.NotificationResponse;
import com.skillstorm.notification.enums.NotificationType;
import com.skillstorm.notification.model.Notification;
import com.skillstorm.notification.repository.NotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationRepository notificationRepository;

    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    public NotificationResponse createNotification(CreateNotificationRequest request) {
        NotificationType type = request.getType() != null ? request.getType() : NotificationType.GENERAL;

        Notification notification = new Notification(
                request.getRecipientId(),
                request.getTitle(),
                request.getMessage(),
                type.getLabel(),
                request.getSourceService(),
                request.getSourceId()
        );

        return NotificationResponse.from(notificationRepository.save(notification));
    }

    public NotificationResponse createFromEvent(NotificationEvent event) {
        if (event.getRecipientId() == null) {
            // Source records created before recipient tracking was added (e.g. an engagement with
            // no owner_id) have no valid recipient. Skip rather than retry forever on a poison event.
            log.warn("Skipping notification event type={} source={}:{} — no recipientId",
                    event.getEventType(), event.getSourceService(), event.getSourceId());
            return null;
        }
        if (event.getTitle() == null || event.getTitle().isBlank()) {
            throw new IllegalArgumentException("title is required on notification event");
        }
        if (event.getMessage() == null || event.getMessage().isBlank()) {
            throw new IllegalArgumentException("message is required on notification event");
        }

        NotificationType type = NotificationType.fromLabel(event.getEventType());

        Notification notification = new Notification(
                event.getRecipientId(),
                event.getTitle(),
                event.getMessage(),
                type.getLabel(),
                event.getSourceService(),
                event.getSourceId()
        );

        return NotificationResponse.from(notificationRepository.save(notification));
    }

    public List<NotificationResponse> getNotifications(UUID recipientId) {
        List<Notification> notifications = recipientId != null
                ? notificationRepository.findByRecipientIdAndActiveTrue(recipientId)
                : notificationRepository.findByActiveTrue();

        return notifications.stream()
                .map(NotificationResponse::from)
                .toList();
    }

    public NotificationResponse getNotificationById(Long id) {
        return NotificationResponse.from(findActiveOrThrow(id));
    }

    public NotificationResponse markAsRead(Long id) {
        Notification notification = findActiveOrThrow(id);
        notification.setRead(true);
        return NotificationResponse.from(notificationRepository.save(notification));
    }

    public void deleteNotification(Long id) {
        Notification notification = findActiveOrThrow(id);
        notification.setActive(false);
        notificationRepository.save(notification);
    }

    private Notification findActiveOrThrow(Long id) {
        return notificationRepository.findById(id)
                .filter(Notification::isActive)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Notification " + id + " not found"));
    }
}
