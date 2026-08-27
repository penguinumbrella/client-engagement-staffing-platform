package com.skillstorm.client.kafka;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class NotificationEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(NotificationEventPublisher.class);

    private final KafkaTemplate<String, NotificationEvent> kafkaTemplate;
    private final String topic;

    public NotificationEventPublisher(KafkaTemplate<String, NotificationEvent> kafkaTemplate,
                                      @Value("${notification.kafka.topic}") String topic) {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
    }

    public void publish(NotificationEvent event) {
        try {
            String key = event.getRecipientId() != null ? event.getRecipientId().toString() : null;
            kafkaTemplate.send(topic, key, event);
            log.info("Published {} for sourceId={} to topic={}", event.getEventType(), event.getSourceId(), topic);
        } catch (Exception ex) {
            log.error("Failed to publish notification event type={} sourceId={}: {}",
                    event.getEventType(), event.getSourceId(), ex.getMessage());
        }
    }
}
