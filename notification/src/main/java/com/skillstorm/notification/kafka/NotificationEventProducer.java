package com.skillstorm.notification.kafka;

import com.skillstorm.notification.dto.NotificationEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class NotificationEventProducer {

    private static final Logger log = LoggerFactory.getLogger(NotificationEventProducer.class);

    private final KafkaTemplate<String, NotificationEvent> kafkaTemplate;
    private final String topic;

    public NotificationEventProducer(KafkaTemplate<String, NotificationEvent> kafkaTemplate,
                                     @Value("${notification.kafka.topic}") String topic) {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
    }

    public void publish(NotificationEvent event) {
        String key = event.getRecipientId() != null ? event.getRecipientId().toString() : null;
        kafkaTemplate.send(topic, key, event);
        log.info("Published notification event type={} to topic={}", event.getEventType(), topic);
    }
}
