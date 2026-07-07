package com.tripgenie.trip.audit.repository;

import com.tripgenie.trip.audit.domain.AuditAction;
import com.tripgenie.trip.audit.domain.AuditEntityType;
import com.tripgenie.trip.audit.domain.AuditLog;
import com.tripgenie.trip.audit.domain.AuditStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.UUID;

public interface AuditLogRepository extends JpaRepository<AuditLog, UUID>, JpaSpecificationExecutor<AuditLog> {
    long countByActionType(AuditAction actionType);

    long countByActionTypeAndStatus(AuditAction actionType, AuditStatus status);

    long countByEntityTypeAndStatus(AuditEntityType entityType, AuditStatus status);

    @Query("""
            select count(a)
            from AuditLog a
            where a.actionType = com.tripgenie.trip.audit.domain.AuditAction.NOTIFICATION_PROCESSED
            """)
    long countNotificationsProcessed();
}
