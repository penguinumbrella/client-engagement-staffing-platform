package com.skillstorm.notification.kafka;

import com.skillstorm.notification.dto.NotificationEvent;
import com.skillstorm.notification.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class NotificationEventListener {

    private static final Logger log = LoggerFactory.getLogger(NotificationEventListener.class);

    private final NotificationService notificationService;

    public NotificationEventListener(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @KafkaListener(topics = "${notification.kafka.topic}", groupId = "notification-service")
    public void onNotificationEvent(NotificationEvent event) {
        log.info("Received notification event type={} source={}:{} recipient={}",
                event.getEventType(), event.getSourceService(), event.getSourceId(), event.getRecipientId());
        notificationService.createFromEvent(event);
    }
}
