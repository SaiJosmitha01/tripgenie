package com.tripgenie.trip.audit.dto;

import com.tripgenie.trip.audit.domain.AuditAction;
import com.tripgenie.trip.audit.domain.AuditEntityType;
import com.tripgenie.trip.audit.domain.AuditLog;
import com.tripgenie.trip.audit.domain.AuditStatus;

import java.time.Instant;
import java.util.UUID;

public record AuditLogResponse(
        UUID id,
        UUID userId,
        AuditAction actionType,
        AuditEntityType entityType,
        UUID entityId,
        Instant timestamp,
        AuditStatus status,
        String correlationId,
        String metadata
) {
    public static AuditLogResponse from(AuditLog auditLog) {
        return new AuditLogResponse(
                auditLog.getId(),
                auditLog.getUserId(),
                auditLog.getActionType(),
                auditLog.getEntityType(),
                auditLog.getEntityId(),
                auditLog.getOccurredAt(),
                auditLog.getStatus(),
                auditLog.getCorrelationId(),
                auditLog.getMetadata()
        );
    }
}
