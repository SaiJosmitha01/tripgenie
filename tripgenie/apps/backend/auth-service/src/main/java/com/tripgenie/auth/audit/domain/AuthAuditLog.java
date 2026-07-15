package com.tripgenie.auth.audit.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "auth_audit_logs")
public class AuthAuditLog {
    @Id
    private UUID id;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "action_type", nullable = false, length = 80)
    private String actionType;

    @Column(name = "entity_type", nullable = false, length = 80)
    private String entityType;

    @Column(name = "entity_id")
    private UUID entityId;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(nullable = false, length = 40)
    private String status;

    @Column(name = "correlation_id", length = 120)
    private String correlationId;

    @Column(columnDefinition = "TEXT")
    private String metadata;

    @PrePersist
    void prePersist() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (occurredAt == null) {
            occurredAt = Instant.now();
        }
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public void setActionType(String actionType) {
        this.actionType = actionType;
    }

    public void setEntityType(String entityType) {
        this.entityType = entityType;
    }

    public void setEntityId(UUID entityId) {
        this.entityId = entityId;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
    }

    public void setMetadata(String metadata) {
        this.metadata = metadata;
    }
}
