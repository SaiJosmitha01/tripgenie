package com.tripgenie.trip.audit.event;

import com.tripgenie.common.event.KafkaTopics;
import com.tripgenie.common.event.NotificationEvent;
import com.tripgenie.trip.audit.domain.AuditAction;
import com.tripgenie.trip.audit.domain.AuditEntityType;
import com.tripgenie.trip.audit.service.AuditLogService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@ConditionalOnProperty(prefix = "tripgenie.kafka", name = "enabled", havingValue = "true", matchIfMissing = true)
public class NotificationAuditListener {
    private final AuditLogService auditLogService;

    public NotificationAuditListener(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    @KafkaListener(topics = KafkaTopics.NOTIFICATIONS, groupId = "${tripgenie.kafka.audit-consumer-group:trip-service-audit}")
    public void onNotificationProcessed(NotificationEvent event) {
        auditLogService.recordSuccess(event.recipientUserId(), AuditAction.NOTIFICATION_PROCESSED,
                AuditEntityType.NOTIFICATION, event.eventId(), Map.of(
                        "sourceEventId", event.sourceEventId(),
                        "notificationType", event.notificationType()));
    }
}
